package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.UserRepository
import com.example.model.TicketStatus
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Real-time Telegram Bot Polling & Interactive Inline Button Service for Visol.uz.
 * Listens to Telegram callback queries (approve, reject, ban), responds with answerCallbackQuery,
 * and updates application state and tickets in real time.
 */
object TelegramBotPollingService {

    private const val TAG = "TelegramBotPolling"
    private var pollingJob: Job? = null
    private var lastUpdateId: Long = 0L

    fun startPolling(context: Context, userRepository: UserRepository) {
        if (pollingJob?.isActive == true) return

        val appContext = context.applicationContext
        pollingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            Log.d(TAG, "Started Telegram Bot Polling for interactive buttons & admin actions...")
            while (isActive) {
                try {
                    val botToken = TelegramModerationBotService.getBotToken(appContext)
                    if (botToken.isBlank()) {
                        delay(10000)
                        continue
                    }

                    val urlString = if (lastUpdateId > 0) {
                        "https://api.telegram.org/bot$botToken/getUpdates?offset=${lastUpdateId + 1}&timeout=8"
                    } else {
                        "https://api.telegram.org/bot$botToken/getUpdates?timeout=8"
                    }

                    val url = URL(urlString)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 12000
                        readTimeout = 12000
                    }

                    val responseCode = conn.responseCode
                    if (responseCode in 200..299) {
                        val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                        val json = JSONObject(responseStr)
                        if (json.optBoolean("ok", false)) {
                            val result = json.optJSONArray("result") ?: JSONArray()
                            for (i in 0 until result.length()) {
                                val update = result.getJSONObject(i)
                                val updateId = update.optLong("update_id")
                                if (updateId > lastUpdateId) {
                                    lastUpdateId = updateId
                                }

                                // 1. Handle Inline Callback Query Button Click
                                if (update.has("callback_query")) {
                                    handleCallbackQuery(
                                        context = appContext,
                                        botToken = botToken,
                                        callback = update.getJSONObject("callback_query"),
                                        userRepository = userRepository
                                    )
                                }

                                // 2. Handle Text Commands (e.g. /start, /status)
                                if (update.has("message")) {
                                    handleTextMessage(
                                        botToken = botToken,
                                        message = update.getJSONObject("message")
                                    )
                                }
                            }
                        }
                        // Silliq va resurslarni tejovchi tanaffus (CPU va batareyani tejash)
                        delay(10000)
                    } else {
                        delay(12000)
                    }
                } catch (e: Exception) {
                    // Suppress network logs during timeout or airplane mode
                    delay(12000)
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun handleCallbackQuery(
        context: Context,
        botToken: String,
        callback: JSONObject,
        userRepository: UserRepository
    ) = withContext(Dispatchers.IO) {
        try {
            val queryId = callback.optString("id")
            val data = callback.optString("data")
            val fromUser = callback.optJSONObject("from")?.optString("first_name") ?: "Admin"
            val messageObj = callback.optJSONObject("message")
            val messageId = messageObj?.optLong("message_id") ?: 0L
            val chatId = messageObj?.optJSONObject("chat")?.optLong("id")?.toString() ?: ""

            Log.d(TAG, "Received Telegram Callback Button click: data=$data, queryId=$queryId")

            // 1. Answer Telegram Callback Query immediately so button stops spinning
            val answerUrl = URL("https://api.telegram.org/bot$botToken/answerCallbackQuery")
            val answerBody = JSONObject().apply {
                put("callback_query_id", queryId)
                put("text", "✅ Qaroringiz qabul qilindi!")
                put("show_alert", false)
            }
            try {
                val conn = (answerUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                OutputStreamWriter(conn.outputStream).use { it.write(answerBody.toString()) }
                conn.responseCode
            } catch (e: Exception) {
                Log.w(TAG, "Error answering callback query: ${e.message}")
            }

            // 2. Process Action (approve or reject)
            val tickets = userRepository.moderationTickets.value
            if (data.startsWith("approve_")) {
                val ticketId = data.removePrefix("approve_")
                val ticket = tickets.find { it.ticketId == ticketId }
                if (ticket != null) {
                    TelegramModerationBotService.executeAdminDecision(
                        context = context,
                        ticket = ticket.copy(telegramMessageId = if (messageId > 0) messageId else ticket.telegramMessageId),
                        decision = TelegramModerationBotService.AdminDecision.APPROVE_CLEAN,
                        userRepository = userRepository
                    )
                }
            } else if (data.startsWith("reject_")) {
                val ticketId = data.removePrefix("reject_")
                val ticket = tickets.find { it.ticketId == ticketId }
                if (ticket != null) {
                    TelegramModerationBotService.executeAdminDecision(
                        context = context,
                        ticket = ticket.copy(telegramMessageId = if (messageId > 0) messageId else ticket.telegramMessageId),
                        decision = TelegramModerationBotService.AdminDecision.REJECT_VIOLATION,
                        userRepository = userRepository
                    )
                }
            } else if (data.startsWith("ban_")) {
                val userId = data.removePrefix("ban_")
                userRepository.setUserBanned(userId, isBanned = true)
                userRepository.setUserRestricted(userId, isRestricted = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing callback query action: ${e.message}", e)
        }
    }

    private suspend fun handleTextMessage(botToken: String, message: JSONObject) = withContext(Dispatchers.IO) {
        try {
            val text = message.optString("text")
            val chat = message.optJSONObject("chat")
            val chatId = chat?.optLong("id")?.toString() ?: return@withContext

            if (text.startsWith("/start") || text.startsWith("/help")) {
                val replyText = """
                    👋 <b>Assalomu alaykum! Visol.uz Moderatsiya Botiga xush kelibsiz.</b>
                    
                    🛡️ Bu bot orqali ilovadagi barcha shubhali Reels, postlar va chat buzilishlari nazorat qilinadi.
                    Tugmalarni bosish orqali ruxsat berishingiz yoki foydalanuvchini bloklashingiz mumkin.
                """.trimIndent()

                val sendUrl = URL("https://api.telegram.org/bot$botToken/sendMessage")
                val body = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", replyText)
                    put("parse_mode", "HTML")
                }
                val conn = (sendUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
                conn.responseCode
            }
        } catch (_: Exception) {}
    }
}
