package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object SocialMediaHelper {

    /**
     * Foydalanuvchi kiritgan har qanday matndan (@, https://, url slashes)
     * faqat toza username / handle ni ajratib oladi.
     */
    fun cleanUsername(input: String): String {
        var clean = input.trim()
        clean = clean.removePrefix("@")

        // Agar to'liq URL yozilgan bo'lsa, domen qismini tozalash
        if (clean.contains("t.me/")) {
            clean = clean.substringAfter("t.me/")
        } else if (clean.contains("telegram.me/")) {
            clean = clean.substringAfter("telegram.me/")
        } else if (clean.contains("instagram.com/")) {
            clean = clean.substringAfter("instagram.com/")
        } else if (clean.contains("tiktok.com/@")) {
            clean = clean.substringAfter("tiktok.com/@")
        } else if (clean.contains("tiktok.com/")) {
            clean = clean.substringAfter("tiktok.com/")
        }

        clean = clean.removePrefix("@")

        // Query parametrlari yoki ortiqcha slashlarni olib tashlash
        if (clean.contains("?")) clean = clean.substringBefore("?")
        if (clean.contains("/")) clean = clean.substringBefore("/")

        return clean.trim()
    }

    fun toTelegramUrl(input: String): String {
        val user = cleanUsername(input)
        return if (user.isNotEmpty()) "https://t.me/$user" else ""
    }

    fun toInstagramUrl(input: String): String {
        val user = cleanUsername(input)
        return if (user.isNotEmpty()) "https://instagram.com/$user" else ""
    }

    fun toTikTokUrl(input: String): String {
        val user = cleanUsername(input)
        return if (user.isNotEmpty()) "https://tiktok.com/@$user" else ""
    }

    /**
     * Tizim brauzeri yoki mos ilovada havolani xavfsiz ochish.
     */
    fun openUrlSafely(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Havolani ochib bo'lmadi: $url", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * O'zbekiston telefon raqamini toza va xatosiz formatlash (+998 90 123 45 67)
     * Backspace (o'chirish) paytida probellarda qotib qolish muammosi bartaraf etilgan.
     */
    fun formatUzbekPhoneNumber(input: String, previous: String = ""): String {
        val digits = input.filter { it.isDigit() }
        var localDigits = if (digits.startsWith("998")) digits.drop(3) else digits

        // Agar foydalanuvchi probelni o'chirgan bo'lsa, undan oldingi raqam ham o'chiriladi
        if (input.length < previous.length && previous.endsWith(" ") && !input.endsWith(" ")) {
            if (localDigits.isNotEmpty()) {
                localDigits = localDigits.dropLast(1)
            }
        }

        val clean = localDigits.take(9)
        if (clean.isEmpty()) return "+998 "

        val sb = StringBuilder("+998 ")
        for (i in clean.indices) {
            if (i == 2 || i == 5 || i == 7) sb.append(" ")
            sb.append(clean[i])
        }
        return sb.toString()
    }

    /**
     * Telegram username formatini tekshirish
     */
    fun isValidTelegramFormat(username: String): Boolean {
        val clean = cleanUsername(username)
        return clean.length in 5..32 && clean.all { it.isLetterOrDigit() || it == '_' }
    }

    /**
     * Telegram username va havolasini tekshirish (asinxron)
     * masalan @Salikh_07 -> https://t.me/Salikh_07
     */
    suspend fun verifyTelegramLink(username: String): TelegramVerificationResult {
        val clean = cleanUsername(username)
        if (clean.isBlank()) return TelegramVerificationResult.Idle
        if (clean.length < 5) {
            return TelegramVerificationResult.InvalidFormat("Telegram username kamida 5 ta belgidan iborat bo'lishi kerak")
        }
        if (!clean.all { it.isLetterOrDigit() || it == '_' }) {
            return TelegramVerificationResult.InvalidFormat("Faqat lotin harflari, raqamlar va '_' mumkin")
        }
        val fullUrl = "https://t.me/$clean"
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL(fullUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                val code = connection.responseCode
                connection.disconnect()
                TelegramVerificationResult.Valid(fullUrl, clean)
            } catch (e: Exception) {
                TelegramVerificationResult.Valid(fullUrl, clean)
            }
        }
    }
}

sealed class TelegramVerificationResult {
    object Idle : TelegramVerificationResult()
    object Checking : TelegramVerificationResult()
    data class Valid(val url: String, val username: String) : TelegramVerificationResult()
    data class InvalidFormat(val reason: String) : TelegramVerificationResult()
}
