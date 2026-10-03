package com.example.model

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val coverUrl: String,
    val audioUrl: String,
    val genre: String,
    val isOriginalSound: Boolean = false,
    val playCount: Long = 0L,
    val trimStartSec: Float = 0f,
    val trimLengthSec: Int = 15,
    val waveformAmplitudes: List<Float> = emptyList(),
    val originalAuthorUsername: String? = null,
    val isTrending: Boolean = false,
    val lyrics: String = "",
    val keywords: List<String> = emptyList(),
    val youtubeVideoId: String? = null
)
