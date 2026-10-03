package com.example.model

import org.json.JSONObject

/**
 * Visol.uz Instagram Story (Istorya) modeli
 * 24 soatlik faol hikoya formati, interaktiv "Link" (Havola) stikeri va musiqa qo'llab-quvvatlashi bilan.
 */
data class UserStory(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorGender: String,
    val authorPhotoPath: String,
    val mediaUri: String,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val caption: String = "",
    val linkUrl: String? = null, // Interaktiv havola URLi (masalan: "https://visol.uz/xizmatlar")
    val linkText: String? = null, // Stiker matni (masalan: "Batafsil ko'rish 🔗" yoki "visol.uz")
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicAudioUrl: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val viewsCount: Int = 0
) {
    /**
     * Hikoya 24 soat davomida faol bo'ladi
     */
    fun isStoryActive(): Boolean {
        val now = System.currentTimeMillis()
        val duration24Hours = 24 * 60 * 60 * 1000L
        return (now - createdAtMillis) in 0..duration24Hours
    }

    /**
     * Qolgan soatlarni hisoblash
     */
    fun remainingHours(): Int {
        val duration24Hours = 24 * 60 * 60 * 1000L
        val remainingMs = (createdAtMillis + duration24Hours) - System.currentTimeMillis()
        return (remainingMs / (60 * 60 * 1000L)).toInt().coerceIn(0, 24)
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("authorId", authorId)
        json.put("authorName", authorName)
        json.put("authorGender", authorGender)
        json.put("authorPhotoPath", authorPhotoPath)
        json.put("mediaUri", mediaUri)
        json.put("mediaType", mediaType)
        json.put("caption", caption)
        linkUrl?.let { json.put("linkUrl", it) }
        linkText?.let { json.put("linkText", it) }
        musicTitle?.let { json.put("musicTitle", it) }
        musicArtist?.let { json.put("musicArtist", it) }
        musicAudioUrl?.let { json.put("musicAudioUrl", it) }
        json.put("createdAtMillis", createdAtMillis)
        json.put("viewsCount", viewsCount)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): UserStory {
            return UserStory(
                id = json.optString("id", "story_${System.currentTimeMillis()}"),
                authorId = json.optString("authorId", ""),
                authorName = json.optString("authorName", "Foydalanuvchi"),
                authorGender = json.optString("authorGender", "Erkak"),
                authorPhotoPath = json.optString("authorPhotoPath", ""),
                mediaUri = json.optString("mediaUri", ""),
                mediaType = json.optString("mediaType", "PHOTO"),
                caption = json.optString("caption", ""),
                linkUrl = if (json.has("linkUrl")) json.getString("linkUrl") else null,
                linkText = if (json.has("linkText")) json.getString("linkText") else null,
                musicTitle = if (json.has("musicTitle")) json.getString("musicTitle") else null,
                musicArtist = if (json.has("musicArtist")) json.getString("musicArtist") else null,
                musicAudioUrl = if (json.has("musicAudioUrl")) json.getString("musicAudioUrl") else null,
                createdAtMillis = json.optLong("createdAtMillis", System.currentTimeMillis()),
                viewsCount = json.optInt("viewsCount", 0)
            )
        }
    }
}
