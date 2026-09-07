package com.example.gemini

import android.util.Base64
import android.util.Log
import com.example.data.model.TranscriptSettings
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveClient(
    private val apiKey: String,
    private val settings: TranscriptSettings,
    private val onConnected: () -> Unit,
    private val onReconnecting: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onInterimText: (String) -> Unit,
    private val onTurnComplete: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "GeminiLiveClient"
        private const val BASE_WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
        
        // Priority order for real-time live audio transcription models
        const val MODEL_PRIMARY = "models/gemini-2.5-flash-native-audio-preview-12-2025"
        const val MODEL_TRANSCRIBE_LIVE = "models/gemini-3.5-transcribe-live"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSockets
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val isSetupComplete = AtomicBoolean(false)
    private val currentTurnAccumulator = StringBuilder()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    fun start() {
        if (apiKey.isBlank()) {
            onError("Gemini API key is not configured. Please add GEMINI_API_KEY in secrets or settings.")
            return
        }
        isRunning.set(true)
        isSetupComplete.set(false)
        connect()
    }

    private fun connect() {
        val url = "$BASE_WS_URL?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected, sending setup message")
                reconnectAttempts = 0
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                if (isRunning.get()) {
                    attemptReconnect()
                } else {
                    onDisconnected()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}", t)
                val errorDetails = response?.message ?: t.localizedMessage ?: "Connection error"
                if (isRunning.get()) {
                    attemptReconnect()
                } else {
                    onError("Live connection failed: $errorDetails")
                    onDisconnected()
                }
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val setupObj = JSONObject()
            val setupContent = JSONObject()

            // Preferred model for live multimodal audio
            setupContent.put("model", MODEL_PRIMARY)

            val genConfig = JSONObject()
            val responseModalities = JSONArray()
            responseModalities.put("TEXT")
            genConfig.put("responseModalities", responseModalities)
            genConfig.put("temperature", 0.1)
            setupContent.put("generationConfig", genConfig)

            // System instructions incorporating SMART transcription directives
            val systemInstruction = JSONObject()
            val parts = JSONArray()
            val part = JSONObject()

            val langInstruction = when {
                settings.isAutoLanguageDetection -> "Auto-detect language. Support multilingual and mixed-language speech (English, Bengali, Hindi, etc.)."
                settings.languageCode == "bn" -> "Transcribe primarily in Bengali (বাংলা). Support mixed English terms naturally."
                settings.languageCode == "hi" -> "Transcribe primarily in Hindi (हिन्दी). Support mixed English terms naturally."
                settings.languageCode == "en" -> "Transcribe in English."
                else -> "Language code: ${settings.languageCode}."
            }

            val vocabInstruction = if (settings.customVocabulary.isNotEmpty()) {
                "Recognize custom vocabulary and spell them accurately: ${settings.customVocabulary.joinToString(", ")}."
            } else ""

            val prompt = buildString {
                appendLine("You are a real-time live transcription and smart text formatter.")
                appendLine("Convert incoming streaming audio into clean, high quality text in near real time.")
                if (settings.isSmartTranscription) {
                    appendLine("SMART TRANSCRIPTION DIRECTIVES:")
                    if (settings.isAutoPunctuation) {
                        appendLine("- Add natural punctuation, periods, commas, and proper capitalization.")
                    }
                    if (settings.isRemoveFillerWords) {
                        appendLine("- Remove speech filler words (um, uh, er, ah, like, you know, etc.).")
                        appendLine("- Remove stuttering and unintended repeated words.")
                    }
                    appendLine("- Resolve spoken self-corrections naturally (e.g., 'meet tomorrow at three actually no Wednesday at three' -> 'meet on Wednesday at 3:00 PM').")
                    appendLine("- Format lists cleanly with bullet points or numbers when lists are spoken.")
                    appendLine("- Create natural readable paragraphs for thought transitions.")
                    appendLine("- Preserve original meaning strictly; NEVER invent information the speaker did not say.")
                }
                appendLine(langInstruction)
                if (vocabInstruction.isNotBlank()) {
                    appendLine(vocabInstruction)
                }
                appendLine("Respond ONLY with the transcribed and formatted text. Do not converse or add commentary.")
            }

            part.put("text", prompt)
            parts.put(part)
            systemInstruction.put("parts", parts)
            setupContent.put("systemInstruction", systemInstruction)

            setupObj.put("setup", setupContent)

            ws.send(setupObj.toString())
            Log.d(TAG, "Setup message sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error constructing setup message", e)
            onError("Failed to configure live session: ${e.message}")
        }
    }

    private fun handleServerMessage(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)

            if (root.has("setupComplete")) {
                Log.d(TAG, "Setup complete confirmed by server")
                isSetupComplete.set(true)
                onConnected()
                return
            }

            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        val textBuilder = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                textBuilder.append(part.getString("text"))
                            }
                        }
                        val newChunk = textBuilder.toString()
                        if (newChunk.isNotEmpty()) {
                            currentTurnAccumulator.append(newChunk)
                            onInterimText(currentTurnAccumulator.toString())
                        }
                    }
                }

                val turnComplete = serverContent.optBoolean("turnComplete", false)
                if (turnComplete) {
                    val finalizedTurn = currentTurnAccumulator.toString().trim()
                    if (finalizedTurn.isNotEmpty()) {
                        onTurnComplete(finalizedTurn)
                        currentTurnAccumulator.clear()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: ${e.message}", e)
        }
    }

    fun sendAudioChunk(pcmBytes: ByteArray) {
        if (!isRunning.get() || webSocket == null) return

        try {
            val base64Data = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)

            val root = JSONObject()
            val realtimeInput = JSONObject()
            val mediaChunks = JSONArray()

            val chunk = JSONObject()
            chunk.put("mimeType", "audio/pcm;rate=16000")
            chunk.put("data", base64Data)

            mediaChunks.put(chunk)
            realtimeInput.put("mediaChunks", mediaChunks)
            root.put("realtimeInput", realtimeInput)

            webSocket?.send(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio chunk", e)
        }
    }

    private fun attemptReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            onReconnecting()
            Log.d(TAG, "Attempting reconnect $reconnectAttempts/$maxReconnectAttempts...")
            try {
                Thread.sleep(1000L * reconnectAttempts)
            } catch (_: InterruptedException) {}
            connect()
        } else {
            onError("Connection lost. Tap microphone to restart.")
            stop()
        }
    }

    fun stop() {
        isRunning.set(false)
        isSetupComplete.set(false)

        // Flush any remaining accumulated text
        val remaining = currentTurnAccumulator.toString().trim()
        if (remaining.isNotEmpty()) {
            onTurnComplete(remaining)
            currentTurnAccumulator.clear()
        }

        try {
            webSocket?.close(1000, "User stopped recording")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebSocket", e)
        } finally {
            webSocket = null
            onDisconnected()
        }
    }
}
