package com.example.model

import org.json.JSONObject

/**
 * Komentga yozilgan javob modeli (Threaded reply)
 */
data class CommentReply(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorGender: String,
    val authorPhotoPath: String,
    val replyToUserName: String, // Masalan: "Madina"
    val text: String,
    val timestamp: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)

/**
 * Asosiy izoh (Komentariya) modeli
 */
data class CommentItem(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorGender: String,
    val authorPhotoPath: String,
    val text: String,
    val timestamp: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val replies: List<CommentReply> = emptyList()
)

/**
 * Visol.uz ijtimoiy post va Reels video modeli.
 * Foydalanuvchilar va nomzodlar tomonidan joylangan rasm va video postlar.
 */
data class UserPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorGender: String,
    val authorPhotoPath: String,
    val authorLocation: String,
    val title: String,
    val mediaType: String, // "PHOTO" or "REELS" / "VIDEO"
    val mediaUri: String = "",
    val timestamp: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val commentsCount: Int = 0,
    val comments: List<CommentItem> = emptyList(),
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicAudioUrl: String? = null,
    val moderationStatus: ModerationStatus = ModerationStatus.APPROVED,
    val moderationReason: String? = null,
    val moderationConfidence: Float = 0f
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("authorId", authorId)
        json.put("authorName", authorName)
        json.put("authorGender", authorGender)
        json.put("authorPhotoPath", authorPhotoPath)
        json.put("authorLocation", authorLocation)
        json.put("title", title)
        json.put("mediaType", mediaType)
        json.put("mediaUri", mediaUri)
        json.put("timestamp", timestamp)
        json.put("likesCount", likesCount)
        json.put("isLiked", isLiked)
        json.put("commentsCount", commentsCount)
        json.put("moderationStatus", moderationStatus.name)
        moderationReason?.let { json.put("moderationReason", it) }
        json.put("moderationConfidence", moderationConfidence.toDouble())
        musicTitle?.let { json.put("musicTitle", it) }
        musicArtist?.let { json.put("musicArtist", it) }
        musicAudioUrl?.let { json.put("musicAudioUrl", it) }
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): UserPost {
            val statusStr = json.optString("moderationStatus", ModerationStatus.APPROVED.name)
            val parsedStatus = try {
                ModerationStatus.valueOf(statusStr)
            } catch (_: Exception) {
                ModerationStatus.APPROVED
            }

            return UserPost(
                id = json.optString("id", "post_${System.currentTimeMillis()}"),
                authorId = json.optString("authorId", ""),
                authorName = json.optString("authorName", "Foydalanuvchi"),
                authorGender = json.optString("authorGender", "Erkak"),
                authorPhotoPath = json.optString("authorPhotoPath", ""),
                authorLocation = json.optString("authorLocation", ""),
                title = json.optString("title", ""),
                mediaType = json.optString("mediaType", "REELS"),
                mediaUri = json.optString("mediaUri", ""),
                timestamp = json.optString("timestamp", "Hozirgina"),
                likesCount = json.optInt("likesCount", 0),
                isLiked = json.optBoolean("isLiked", false),
                commentsCount = json.optInt("commentsCount", 0),
                moderationStatus = parsedStatus,
                moderationReason = if (json.has("moderationReason")) json.getString("moderationReason") else null,
                moderationConfidence = json.optDouble("moderationConfidence", 0.0).toFloat(),
                musicTitle = if (json.has("musicTitle")) json.getString("musicTitle") else null,
                musicArtist = if (json.has("musicArtist")) json.getString("musicArtist") else null,
                musicAudioUrl = if (json.has("musicAudioUrl")) json.getString("musicAudioUrl") else null
            )
        }
    }
}

object SampleReelsAndPosts {
    val initialPosts: List<UserPost> = emptyList()
}
