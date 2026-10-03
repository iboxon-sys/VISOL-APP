package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream

class AudioRecorderHelper {
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var isRecording = false
    private var recordingStartTime = 0L

    fun startRecording(context: Context): File? {
        try {
            cancelRecording() // Clean up any previous session

            val audioDir = File(context.cacheDir, "audio_records").apply {
                if (!exists()) mkdirs()
            }
            val file = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentAudioFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            } catch (e: Exception) {
                Log.w("AudioRecorderHelper", "MIC source failed, falling back to DEFAULT: ${e.message}")
                recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            }

            recorder.apply {
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            Log.d("AudioRecorderHelper", "Voice recording started successfully: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start recording: ${e.message}", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            return null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.maxAmplitude ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun stopRecording(): File? {
        return try {
            val recordDuration = System.currentTimeMillis() - recordingStartTime
            if (recordDuration < 600) {
                // MediaRecorder requires at least ~500ms before calling stop() without crashing
                Thread.sleep(600 - recordDuration)
            }

            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.w("AudioRecorderHelper", "Stop call failed, releasing: ${e.message}")
                    }
                    release()
                }
            }
            mediaRecorder = null
            isRecording = false

            val file = currentAudioFile
            if (file != null && file.exists() && file.length() > 0) {
                Log.d("AudioRecorderHelper", "Voice recording finished. File size: ${file.length()} bytes")
                file
            } else {
                Log.w("AudioRecorderHelper", "Recorded file is missing or empty")
                file
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to stop recording: ${e.message}", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentAudioFile
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        // ignore
                    }
                    release()
                }
            }
            currentAudioFile?.delete()
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Cancel recording error: ${e.message}")
        } finally {
            mediaRecorder = null
            isRecording = false
            currentAudioFile = null
        }
    }
}

object AudioPlaybackManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUri: String? = null
    private var progressJob: Job? = null

    fun playAudio(
        context: Context,
        audioUriString: String?,
        coroutineScope: CoroutineScope,
        onProgress: (Float) -> Unit,
        onCompletion: () -> Unit
    ) {
        stopAudio()

        if (audioUriString.isNullOrBlank()) {
            // Simulated playback if no physical audio file was recorded
            coroutineScope.launch {
                for (i in 1..100) {
                    delay(50)
                    onProgress(i / 100f)
                }
                onCompletion()
            }
            return
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setVolume(1.0f, 1.0f)
            }

            // Determine if it's a direct file path or URI
            val rawPath = if (audioUriString.startsWith("file://")) {
                Uri.parse(audioUriString).path ?: ""
            } else {
                audioUriString
            }
            val localFile = File(rawPath)

            if (localFile.exists() && localFile.canRead()) {
                val fis = FileInputStream(localFile)
                player.setDataSource(fis.fd)
                fis.close()
            } else {
                val uri = Uri.parse(audioUriString)
                player.setDataSource(context, uri)
            }

            player.prepare()
            player.start()
            currentPlayingUri = audioUriString
            mediaPlayer = player

            player.setOnCompletionListener {
                progressJob?.cancel()
                onProgress(1f)
                onCompletion()
                stopAudio()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e("AudioPlaybackManager", "MediaPlayer error: what=$what, extra=$extra")
                progressJob?.cancel()
                onCompletion()
                stopAudio()
                true
            }

            progressJob = coroutineScope.launch {
                val totalDuration = player.duration
                while (player.isPlaying && isActive) {
                    val currentPos = player.currentPosition
                    val fraction = if (totalDuration > 0) currentPos.toFloat() / totalDuration else 0f
                    onProgress(fraction.coerceIn(0f, 1f))
                    delay(50)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Audio playback failed: ${e.message}", e)
            onCompletion()
            stopAudio()
        }
    }

    fun isPlaying(audioUri: String?): Boolean {
        return mediaPlayer?.isPlaying == true && currentPlayingUri == audioUri
    }

    fun stopAudio() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Stop audio error: ${e.message}")
        } finally {
            mediaPlayer = null
            currentPlayingUri = null
        }
    }
}
