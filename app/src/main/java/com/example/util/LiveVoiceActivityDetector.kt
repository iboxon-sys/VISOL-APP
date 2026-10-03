package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sqrt

/**
 * High-performance, zero-latency Voice Activity Detector (VAD) for Voice Party Rooms.
 * Reads real-time microphone PCM audio stream to detect genuine speaking vs silence.
 */
class LiveVoiceActivityDetector(
    private val onVoiceLevelChanged: (isSpeaking: Boolean, level: Float) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var detectorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1024)

    @SuppressLint("MissingPermission")
    fun startListening(context: Context) {
        if (isListening) return
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.w("LiveVoiceDetector", "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isListening = true

            detectorJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                var silenceFrames = 0
                val silenceThresholdFrames = 3 // ~300ms of silence before turning off indicator

                while (isActive && isListening) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        var sumSquared = 0.0
                        for (i in 0 until readCount) {
                            val sample = buffer[i].toDouble()
                            sumSquared += sample * sample
                        }
                        val rms = sqrt(sumSquared / readCount)
                        
                        // Noise gate threshold: speech is typically > 600-1000 RMS in PCM 16-bit
                        val threshold = 750.0
                        if (rms > threshold) {
                            silenceFrames = 0
                            val normalizedLevel = ((rms - threshold) / 5000.0).toFloat().coerceIn(0.1f, 1.0f)
                            withContext(Dispatchers.Main) {
                                onVoiceLevelChanged(true, normalizedLevel)
                            }
                        } else {
                            silenceFrames++
                            if (silenceFrames >= silenceThresholdFrames) {
                                withContext(Dispatchers.Main) {
                                    onVoiceLevelChanged(false, 0f)
                                }
                            }
                        }
                    }
                    delay(80)
                }
            }
        } catch (e: Exception) {
            Log.e("LiveVoiceDetector", "Error in startListening: ${e.message}")
            stopListening()
        }
    }

    fun stopListening() {
        isListening = false
        detectorJob?.cancel()
        detectorJob = null
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w("LiveVoiceDetector", "Error releasing AudioRecord: ${e.message}")
        }
        audioRecord = null
        scope.launch(Dispatchers.Main) {
            onVoiceLevelChanged(false, 0f)
        }
    }
}
