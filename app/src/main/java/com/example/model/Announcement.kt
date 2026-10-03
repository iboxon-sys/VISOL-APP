package com.example.model

import java.util.UUID

data class AnnouncementComment(
    val id: String = UUID.randomUUID().toString(),
    val announcementId: String,
    val authorId: String,
    val authorName: String,
    val authorGender: String = "Ayol",
    val authorPhotoPath: String? = null,
    val isVerified: Boolean = false,
    val text: String,
    val timestamp: String = "Hozirgina",
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val parentCommentId: String? = null,
    val replyToAuthorName: String? = null,
    val replies: List<AnnouncementComment> = emptyList()
)

data class Announcement(
    val id: String = UUID.randomUUID().toString(),
    val authorId: String,
    val authorName: String,
    val authorGender: String = "Ayol",
    val authorPhotoPath: String? = null,
    val title: String,
    val description: String,
    val category: String, // "Sovchilik", "To'y & Nikoh", "Xizmatlar", "Tavsiyalar"
    val region: String,
    val contactPhone: String = "",
    val telegramUsername: String = "",
    val timestamp: String = "Bugun",
    val viewsCount: Int = 142,
    val likesCount: Int = 28,
    val isLiked: Boolean = false,
    val isVerified: Boolean = true,
    val comments: List<AnnouncementComment> = emptyList()
)
