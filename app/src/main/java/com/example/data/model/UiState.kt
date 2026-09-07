package com.example.data.model

sealed interface LiveConnectionState {
    data object Disconnected : LiveConnectionState
    data object Connecting : LiveConnectionState
    data object Connected : LiveConnectionState
    data object Reconnecting : LiveConnectionState
    data class Error(val message: String) : LiveConnectionState
}

enum class MicState {
    IDLE,
    RECORDING,
    PROCESSING
}

data class TranscriptionUiState(
    val currentTranscriptId: Long? = null,
    val documentTitle: String = "Untitled Transcript",
    val finalizedText: String = "",
    val interimText: String = "",
    val micState: MicState = MicState.IDLE,
    val connectionState: LiveConnectionState = LiveConnectionState.Disconnected,
    val isSmartPolishing: Boolean = false,
    val recordingDurationSeconds: Long = 0L,
    val audioLevel: Float = 0.0f,
    val waveformSamples: List<Float> = emptyList(),
    val settings: TranscriptSettings = TranscriptSettings(),
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val statusMessage: String = "Ready to record",
    val errorMessage: String? = null
)
