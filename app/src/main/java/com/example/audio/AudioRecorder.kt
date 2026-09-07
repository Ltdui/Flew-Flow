package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AudioRecorder(
    private val onAudioChunk: (ByteArray) -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // 100ms chunk = 1600 samples = 3200 bytes
        const val CHUNK_SIZE_BYTES = 3200
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope): Boolean {
        if (isRecording.get()) return true

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onError("AudioRecord min buffer size configuration error")
            return false
        }

        val bufferSize = max(minBufferSize, CHUNK_SIZE_BYTES * 2)

        try {
            // Try VOICE_RECOGNITION first for optimal speech tuning, fallback to MIC
            var record: AudioRecord? = null
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            } catch (e: Exception) {
                Log.w(TAG, "VOICE_RECOGNITION failed, trying MIC source: ${e.message}")
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                onError("AudioRecord failed to initialize. Please check mic permissions.")
                record.release()
                return false
            }

            audioRecord = record
            record.startRecording()
            isRecording.set(true)

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(CHUNK_SIZE_BYTES)
                while (isActive && isRecording.get()) {
                    val readBytes = record.read(buffer, 0, buffer.size)
                    if (readBytes > 0) {
                        val chunkCopy = buffer.copyOf(readBytes)
                        onAudioChunk(chunkCopy)

                        // Calculate normalized RMS amplitude for visualization
                        val amplitude = calculateNormalizedRms(chunkCopy, readBytes)
                        onAmplitudeChanged(amplitude)
                    } else if (readBytes < 0) {
                        Log.e(TAG, "Error reading audio: $readBytes")
                    }
                }
            }

            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Microphone permission missing", e)
            onError("Microphone permission denied: ${e.message}")
            stop()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting audio recording", e)
            onError("Failed to start recording: ${e.message}")
            stop()
            return false
        }
    }

    fun stop() {
        if (!isRecording.getAndSet(false)) return

        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
            onAmplitudeChanged(0.0f)
        }
    }

    private fun calculateNormalizedRms(pcmBytes: ByteArray, length: Int): Float {
        var sumSquares = 0.0
        val sampleCount = length / 2
        if (sampleCount == 0) return 0f

        for (i in 0 until length - 1 step 2) {
            // Little endian 16-bit PCM
            val sample = ((pcmBytes[i + 1].toInt() shl 8) or (pcmBytes[i].toInt() and 0xFF)).toShort()
            sumSquares += (sample.toDouble() * sample.toDouble())
        }

        val rms = sqrt(sumSquares / sampleCount)
        // Normalize RMS against maximum short value (~32767) with amplification for voice sensitivity
        val normalized = (rms / 8000.0).toFloat()
        return min(1.0f, max(0.02f, normalized))
    }
}
