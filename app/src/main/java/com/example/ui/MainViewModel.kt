package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioRecorder
import com.example.data.local.AppDatabase
import com.example.data.model.LiveConnectionState
import com.example.data.model.MicState
import com.example.data.model.TranscriptEntity
import com.example.data.model.TranscriptSettings
import com.example.data.model.TranscriptionUiState
import com.example.data.repository.TranscriptRepository
import com.example.gemini.GeminiLiveClient
import com.example.gemini.SmartPostProcessor
import com.example.ui.editor.UndoRedoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val repository: TranscriptRepository
    val savedTranscripts: StateFlow<List<TranscriptEntity>>

    private val _uiState = MutableStateFlow(TranscriptionUiState())
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    private val undoRedoManager = UndoRedoManager()
    private var audioRecorder: AudioRecorder? = null
    private var geminiLiveClient: GeminiLiveClient? = null
    private var timerJob: Job? = null

    val hasBuildConfigApiKey: Boolean = try {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    } catch (_: Throwable) {
        false
    }

    init {
        val database = AppDatabase.getInstance(application)
        repository = TranscriptRepository(database.transcriptDao())
        savedTranscripts = repository.allTranscripts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        undoRedoManager.initialize("")
    }

    private fun getActiveApiKey(): String {
        val customKey = _uiState.value.settings.customApiKey.trim()
        if (customKey.isNotEmpty()) return customKey

        return try {
            if (BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") BuildConfig.GEMINI_API_KEY else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun toggleRecording() {
        if (_uiState.value.micState == MicState.RECORDING) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Gemini API key is required. Please configure it in Settings or AI Studio secrets.",
                    connectionState = LiveConnectionState.Error("API Key required")
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                micState = MicState.RECORDING,
                connectionState = LiveConnectionState.Connecting,
                recordingDurationSeconds = 0L,
                errorMessage = null,
                statusMessage = "Listening..."
            )
        }

        startTimer()

        // 1. Initialize Gemini Live Client
        geminiLiveClient = GeminiLiveClient(
            apiKey = apiKey,
            settings = _uiState.value.settings,
            onConnected = {
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            connectionState = LiveConnectionState.Connected,
                            statusMessage = "Connected. Speak naturally..."
                        )
                    }
                }
            },
            onReconnecting = {
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            connectionState = LiveConnectionState.Reconnecting,
                            statusMessage = "Reconnecting session..."
                        )
                    }
                }
            },
            onDisconnected = {
                viewModelScope.launch(Dispatchers.Main) {
                    if (_uiState.value.micState != MicState.RECORDING) {
                        _uiState.update { it.copy(connectionState = LiveConnectionState.Disconnected) }
                    }
                }
            },
            onInterimText = { interim ->
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { it.copy(interimText = interim) }
                }
            },
            onTurnComplete = { finalizedTurn ->
                viewModelScope.launch(Dispatchers.Main) {
                    appendFinalizedTurn(finalizedTurn)
                }
            },
            onError = { error ->
                viewModelScope.launch(Dispatchers.Main) {
                    Log.e(TAG, "GeminiLiveClient error: $error")
                    _uiState.update {
                        it.copy(
                            errorMessage = error,
                            connectionState = LiveConnectionState.Error(error)
                        )
                    }
                }
            }
        )

        geminiLiveClient?.start()

        // 2. Initialize AudioRecorder
        audioRecorder = AudioRecorder(
            onAudioChunk = { pcmChunk ->
                geminiLiveClient?.sendAudioChunk(pcmChunk)
            },
            onAmplitudeChanged = { amp ->
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { it.copy(audioLevel = amp) }
                }
            },
            onError = { error ->
                viewModelScope.launch(Dispatchers.Main) {
                    Log.e(TAG, "AudioRecorder error: $error")
                    _uiState.update {
                        it.copy(
                            errorMessage = error,
                            micState = MicState.IDLE,
                            statusMessage = "Mic error: $error"
                        )
                    }
                    stopRecording()
                }
            }
        )

        val success = audioRecorder?.start(viewModelScope) ?: false
        if (!success) {
            stopRecording()
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        timerJob = null

        audioRecorder?.stop()
        audioRecorder = null

        geminiLiveClient?.stop()
        geminiLiveClient = null

        _uiState.update {
            it.copy(
                micState = MicState.IDLE,
                connectionState = LiveConnectionState.Disconnected,
                audioLevel = 0f,
                statusMessage = "Recording paused."
            )
        }

        // Trigger Smart Post-Processing if enabled and we have text
        if (_uiState.value.settings.isSmartTranscription && _uiState.value.finalizedText.isNotBlank()) {
            runSmartPolish()
        } else {
            autoSaveCurrentTranscript()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(recordingDurationSeconds = it.recordingDurationSeconds + 1) }
            }
        }
    }

    private fun appendFinalizedTurn(turn: String) {
        val cleanTurn = turn.trim()
        if (cleanTurn.isEmpty()) return

        val current = _uiState.value.finalizedText
        val separator = if (current.isEmpty()) "" else if (current.endsWith("\n\n") || current.endsWith(". ") || current.endsWith("? ") || current.endsWith("! ")) " " else " "
        val updated = if (current.isEmpty()) cleanTurn else "$current$separator$cleanTurn"

        undoRedoManager.pushState(updated)

        val words = countWords(updated)
        _uiState.update {
            it.copy(
                finalizedText = updated,
                interimText = "",
                wordCount = words,
                characterCount = updated.length
            )
        }

        autoSaveCurrentTranscript()
    }

    fun runSmartPolish() {
        val textToPolish = _uiState.value.finalizedText.trim()
        if (textToPolish.isEmpty()) return

        val apiKey = getActiveApiKey()
        if (apiKey.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSmartPolishing = true, statusMessage = "Gemini SMART formatting in progress...") }

            val polisher = SmartPostProcessor(apiKey)
            val result = polisher.polishTranscript(textToPolish, _uiState.value.settings)

            result.onSuccess { polished ->
                undoRedoManager.pushState(polished)
                val words = countWords(polished)
                _uiState.update {
                    it.copy(
                        finalizedText = polished,
                        isSmartPolishing = false,
                        wordCount = words,
                        characterCount = polished.length,
                        statusMessage = "Smart formatted successfully."
                    )
                }
                autoSaveCurrentTranscript()
            }.onFailure { error ->
                Log.w(TAG, "Smart Polish error: ${error.message}")
                _uiState.update {
                    it.copy(
                        isSmartPolishing = false,
                        statusMessage = "Ready."
                    )
                }
            }
        }
    }

    fun onTextEdited(newText: String) {
        undoRedoManager.pushState(newText)
        val words = countWords(newText)
        _uiState.update {
            it.copy(
                finalizedText = newText,
                wordCount = words,
                characterCount = newText.length
            )
        }
        autoSaveCurrentTranscript()
    }

    fun undo() {
        val prev = undoRedoManager.undo() ?: return
        val words = countWords(prev)
        _uiState.update {
            it.copy(
                finalizedText = prev,
                wordCount = words,
                characterCount = prev.length
            )
        }
        autoSaveCurrentTranscript()
    }

    fun redo() {
        val next = undoRedoManager.redo() ?: return
        val words = countWords(next)
        _uiState.update {
            it.copy(
                finalizedText = next,
                wordCount = words,
                characterCount = next.length
            )
        }
        autoSaveCurrentTranscript()
    }

    val canUndo: Boolean get() = undoRedoManager.canUndo
    val canRedo: Boolean get() = undoRedoManager.canRedo

    fun clearTranscript() {
        if (_uiState.value.micState == MicState.RECORDING) {
            stopRecording()
        }
        undoRedoManager.pushState("")
        _uiState.update {
            it.copy(
                finalizedText = "",
                interimText = "",
                wordCount = 0,
                characterCount = 0,
                currentTranscriptId = null,
                documentTitle = "Untitled Transcript"
            )
        }
    }

    fun createNewDocument() {
        clearTranscript()
        _uiState.update {
            it.copy(
                documentTitle = "New Transcript",
                currentTranscriptId = null
            )
        }
    }

    fun loadTranscript(entity: TranscriptEntity) {
        if (_uiState.value.micState == MicState.RECORDING) {
            stopRecording()
        }
        undoRedoManager.initialize(entity.content)
        _uiState.update {
            it.copy(
                currentTranscriptId = entity.id,
                documentTitle = entity.title,
                finalizedText = entity.content,
                interimText = "",
                wordCount = entity.wordCount,
                characterCount = entity.content.length
            )
        }
    }

    fun deleteTranscript(entity: TranscriptEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entity)
            if (_uiState.value.currentTranscriptId == entity.id) {
                createNewDocument()
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(documentTitle = newTitle) }
        autoSaveCurrentTranscript()
    }

    fun updateSettings(newSettings: TranscriptSettings) {
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun autoSaveCurrentTranscript() {
        val state = _uiState.value
        val content = state.finalizedText.trim()
        if (content.isEmpty()) return

        val title = if (state.documentTitle.isBlank() || state.documentTitle == "Untitled Transcript" || state.documentTitle == "New Transcript") {
            content.take(36).replace("\n", " ").trim()
        } else {
            state.documentTitle
        }

        viewModelScope.launch(Dispatchers.IO) {
            val entity = TranscriptEntity(
                id = state.currentTranscriptId ?: 0,
                title = title,
                content = content,
                language = state.settings.languageCode,
                wordCount = state.wordCount,
                durationSeconds = state.recordingDurationSeconds,
                updatedAt = System.currentTimeMillis()
            )
            val id = repository.save(entity)
            if (state.currentTranscriptId == null) {
                _uiState.update { it.copy(currentTranscriptId = id, documentTitle = title) }
            }
        }
    }

    private fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\s+")).size
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder?.stop()
        geminiLiveClient?.stop()
        timerJob?.cancel()
    }
}
