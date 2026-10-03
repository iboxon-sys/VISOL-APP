package com.example.model

import java.util.UUID

enum class ModerationStatus {
    APPROVED,
    REJECTED,
    PENDING_REVIEW
}

enum class ModerationSeverity {
    SAFE,
    LEVEL_1_CRITICAL, // O'ta qo'pol kontent / Porno -> 100% Rad etilgan, bazaga kiritilmaydi
    LEVEL_2_SUSPICIOUS // Shubhali kadrlar / Noaniq -> Admin tekshiruvi, foydalanuvchiga vaqtinchalik cheklov (is_restricted=true), Telegram botga dalil yuboriladi
}

enum class TicketStatus {
    PENDING, // Kutilmoqda
    APPROVED_CLEAN, // "Bajarildi: Ruxsat berildi" -> Cheklov yechildi (isRestricted=false)
    REJECTED_VIOLATION // "Bajarildi: Rad etildi" -> Qoidabuzarlik tasdiqlandi (isRestricted=true, isBanned=true)
}

data class ModerationTicket(
    val ticketId: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val userUsername: String,
    val userPhotoPath: String,
    val userGender: String = "Erkak",
    val userLocation: String = "",
    val mediaType: String, // "REELS", "VIDEO", "PHOTO"
    val mediaUri: String,
    val caption: String,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicAudioUrl: String? = null,
    val severity: ModerationSeverity,
    val reason: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TicketStatus = TicketStatus.PENDING,
    val telegramMessageId: Long? = null,
    val resolutionNote: String? = null,
    val resolvedAt: Long? = null
)
