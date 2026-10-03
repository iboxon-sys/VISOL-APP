package com.example.model

/**
 * Real-time post created by a candidate (photos/short video clips with captions)
 */
data class CandidatePost(
    val id: String,
    val candidateId: String,
    val title: String,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val mediaUri: String = "",
    val timestamp: String,
    val likesCount: Int = 24
)
