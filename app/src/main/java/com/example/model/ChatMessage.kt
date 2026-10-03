package com.example.model

/**
 * Nomzod va foydalanuvchi o'rtasidagi shaxsiy chat xabari modeli.
 * Instagram Direct uslubida matn, vaqt, jo'natuvchi va Reels videoni ulashish (share) xususiyatlarini qo'llab-quvvatlaydi.
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val sharedReel: UserPost? = null,
    val isRead: Boolean = false,
    val reaction: String? = null,
    val photoUri: String? = null,
    val audioDurationSec: Int? = null,
    val audioUri: String? = null,
    val isAiHakamWarning: Boolean = false,
    val aiHakamReason: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)
