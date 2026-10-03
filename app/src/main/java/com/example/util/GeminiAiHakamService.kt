package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Direct Gemini API Service for Visol.uz AI Hakam (Official Neutral AI Judge).
 * Connects directly to Google Generative Language API (gemini-2.5-flash / gemini-flash-latest).
 * Handles multi-modal inputs: Text queries, photos/images, and context-aware Uzbek ethics moderation.
 */
object GeminiAiHakamService {

    private const val TAG = "GeminiAiHakamService"

    private const val SYSTEM_PROMPT = """
Siz "Visol.uz" (O'zbekiston oilaviy tanishuv, jiddiy niyatli muloqot va qonuniy nikoh ilovasi)ning rasmiy xolis "AI Hakam"isiz.
Jinsingiz: Neytral (belgilanmagan — barcha foydalanuvchilarga, xoh yigit, xoh qiz bo'lsin, teng, adolatli va xolis yondashasiz).
Vazifangiz:
1. Foydalanuvchilar o'rtasida tushunmovchilik, nizo yoki xafagarchilik bo'lsa, ularga dono, o'zbekona muloyim va sabrli maslahat berish.
2. Ilova qoidalari (18+ kontent taqiqlanganligi, so'kinish va haqoratlarga yo'l qo'yilmasligi, oila muqaddasligi)ni tushuntirish.
3. Foydalanuvchi biror rasm yuborsa, undagi holatni yoki xavfsizlik jihatini xolis baholash.
4. Javoblaringizni har doim o'zbek tilida (lotin alifbosida), samimiy, dono, odobli va qisqa (2-4 jumla) ko'rinishida bering.
Qat'iyan taqiqlanadi: noxolis bo'lish, bir jinsni kamsitish, so'kish va behayo so'zlarga javobda o'xshash so'zlar ishlatish.
"""

    /**
     * Retrieves Gemini API key from BuildConfig or environment.
     */
    fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String ?: ""
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key
            else System.getenv("GEMINI_API_KEY") ?: ""
        } catch (_: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }
    }

    /**
     * Ask AI Hakam with text and optional photo (Multimodal support).
     */
    suspend fun askAiHakam(
        context: Context,
        userQuery: String,
        photoUriString: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured, using fallback intelligent reply.")
            return@withContext generateFallbackAiReply(userQuery)
        }

        try {
            val partsArray = JSONArray()

            // 1. Agar rasm bo'lsa, Base64 ko'rinishida inlineData qilib qo'shamiz (Multi-modal)
            if (!photoUriString.isNullOrBlank()) {
                val imageBase64 = encodeImageToBase64(context, photoUriString)
                if (!imageBase64.isNullOrBlank()) {
                    val inlineDataObj = JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", imageBase64)
                    }
                    val imagePart = JSONObject().apply {
                        put("inline_data", inlineDataObj)
                    }
                    partsArray.put(imagePart)
                }
            }

            // 2. Matn qismini qo'shish
            val textPart = JSONObject().apply {
                put("text", userQuery)
            }
            partsArray.put(textPart)

            val contentObj = JSONObject().apply {
                put("role", "user")
                put("parts", partsArray)
            }

            // System Instruction
            val systemInstructionObj = JSONObject().apply {
                val sysParts = JSONArray().apply {
                    put(JSONObject().apply { put("text", SYSTEM_PROMPT) })
                }
                put("parts", sysParts)
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply { put(contentObj) })
                put("system_instruction", systemInstructionObj)
                val genConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 300)
                }
                put("generationConfig", genConfig)
            }

            // Modern standard model recommended in gemini-api skill
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            OutputStreamWriter(conn.outputStream).use { it.write(requestJson.toString()) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val respJson = JSONObject(responseStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val reply = parts.getJSONObject(0).getString("text")
                    if (reply.isNotBlank()) {
                        Log.d(TAG, "Gemini AI Hakam response received: ${reply.take(50)}...")
                        return@withContext reply.trim()
                    }
                }
            } else {
                val errorStream = conn.errorStream?.let { BufferedReader(InputStreamReader(it)).readText() }
                Log.w(TAG, "Gemini API returned $responseCode: $errorStream")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI API call failed: ${e.message}", e)
        }

        return@withContext generateFallbackAiReply(userQuery)
    }

    private fun encodeImageToBase64(context: Context, uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val stream = ByteArrayOutputStream()
            // Resize if too big for fast network transfer
            val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                val ratio = 1024f / Math.max(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else {
                bitmap
            }
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "Image encoding error: ${e.message}")
            null
        }
    }

    /**
     * Contextual intelligent fallback if network or quota is unavailable.
     */
    private fun generateFallbackAiReply(userText: String): String {
        val lower = userText.lowercase()
        return when {
            lower.contains("salom") || lower.contains("assalomu") ->
                "Assalomu alaykum! Men Visol.uz xolis AI Hakamiman. Qanday savol yoki vaziyat bo'yicha yordam bera olaman? Xush kelibsiz! ✨"
            lower.contains("qoida") || lower.contains("odob") || lower.contains("nizom") ->
                "Visol.uz qoidalari: barcha foydalanuvchilar bir-biriga hurmat bilan munosabatda bo'lishi, haqorat, 18+ va nojo'ya xatti-harakatlardan tiyilishi shart. Odob — jamiyat ko'zgusidir."
            lower.contains("haqorat") || lower.contains("so'k") || lower.contains("nizo") || lower.contains("shikoyat") ->
                "Agar suhbatdoshingiz nojo'ya so'z ishlatgan bo'lsa yoki qoidani buzgan bo'lsa, xavotir olmang. AI Hakam va moderatorlar barcha holatlarni nazorat qiladi. Shikoyat tugmasi orqali xabar bering, adolat ta'minlanadi."
            lower.contains("surat") || lower.contains("rasm") || lower.contains("foto") ->
                "Yuborilgan rasmlar sun'iy intellekt tomonidan ko'zdan kechiriladi. Ilovada faqat odob doirasidagi, shaxsiy va xavfsiz fotosuratlar qabul qilinadi."
            else ->
                "Murojaatingiz uchun tashakkur! Har bir muloqotda o'zbekona muloyimlik, sabr va xolislikka rioya qilish eng go'zal natijalarga olib keladi. Savollaringiz bo'lsa, bemalol yozing! ⚖️"
        }
    }
}
