package com.example.network

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.UserPost
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class TypingEvent(
    val senderId: String,
    val receiverId: String,
    val isTyping: Boolean
)

/**
 * 2-Tomonlama Izolyatsiyalangan Real-Time Chat Engine & API Service.
 * - Rest API: GET /api/messages/{user_id}/{target_id}
 * - REST API: POST /api/messages/send
 * - WebSocket Socket.io Emulation: User Rooms isolation, live message streaming, read receipts.
 */
object ChatRealtimeEngine {
    private const val TAG = "ChatRealtimeEngine"

    // Multi-user in-memory message storage indexed by conversation key: "user1_user2"
    private val conversationStore = ConcurrentHashMap<String, MutableList<ChatMessage>>()

    // Real-time Event Stream (Socket.io event emulation)
    private val _messageEventFlow = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val messageEventFlow: SharedFlow<ChatMessage> = _messageEventFlow.asSharedFlow()

    private val _typingEventFlow = MutableSharedFlow<TypingEvent>(extraBufferCapacity = 64)
    val typingEventFlow: SharedFlow<TypingEvent> = _typingEventFlow.asSharedFlow()

    private val userOnlineMap = ConcurrentHashMap<String, Boolean>()
    private val userLastSeenMap = ConcurrentHashMap<String, String>()

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun setUserOnline(userId: String, isOnline: Boolean) {
        userOnlineMap[userId] = isOnline
        if (!isOnline) {
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            userLastSeenMap[userId] = "Oxirgi marta bugun $timeStr da"
        } else {
            userLastSeenMap[userId] = "Online 🟢"
        }
    }

    fun getUserOnlineStatus(userId: String): String {
        val isOnline = userOnlineMap[userId] ?: true
        return if (isOnline) "Online 🟢" else (userLastSeenMap[userId] ?: "Oxirgi marta bugun ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())} da")
    }

    fun sendTypingStatus(senderId: String, receiverId: String, isTyping: Boolean) {
        engineScope.launch {
            _typingEventFlow.emit(TypingEvent(senderId = senderId, receiverId = receiverId, isTyping = isTyping))
        }
    }

    // Avto-javob generatori uchun namunaviy samimiy matnlar
    private val candidateReplies = listOf(
        "Assalomu alaykum! Xabaringizni oldim. Yaxshimisiz? 😊",
        "Assalomu alaykum! Profilingiz juda ma'qul keldi. Oilangiz tinchmi?",
        "Rahmat javobingiz uchun! Bo'sh vaqtingizda nima bilan mashg'ul bo'lasiz? 🌸",
        "Juda xursandman muloqotimizdan! Javobingizni kutaman ✨",
        "Mashalloh, juda samimiy gapirdingiz. Kuningiz xayrli o'tsin! 🤲",
        "Rasm va xabaringiz uchun rahmat! Omon bo'ling 💖"
    )

    private fun buildConvKey(u1: String, u2: String): String {
        return if (u1 < u2) "${u1}_$u2" else "${u2}_$u1"
    }

    /**
     * REST API Endpoint: GET /api/messages/{user_id}/{target_id}
     * Har bir foydalanuvchi faqat o'zining va maqsadli nomzodning suhbatini izolyatsiyalangan holda oladi.
     */
    fun getConversationHistory(userId: String, targetId: String): List<ChatMessage> {
        val key = buildConvKey(userId, targetId)
        val list = conversationStore[key]
        if (list == null) {
            // Dastlabki salomlashuv xabari (2-shaxs nomidan)
            val initialMsg = ChatMessage(
                id = "msg_init_${UUID.randomUUID().toString().take(8)}",
                senderId = targetId,
                receiverId = userId,
                text = "Assalomu alaykum! Profilimga qiziqish bildirganingizdan xursandman 😊",
                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                isFromMe = false,
                isRead = false
            )
            val newList = mutableListOf(initialMsg)
            conversationStore[key] = newList
            return newList
        }
        return list.map { it.copy(isFromMe = it.senderId == userId) }
    }

    /**
     * REST API Endpoint: POST /api/messages/send
     * Real-time Socket push hodisasi bilan birga xabarni jo'natadi.
     */
    fun sendMessage(
        senderId: String,
        receiverId: String,
        text: String,
        photoUri: String? = null,
        audioUri: String? = null,
        audioDurationSec: Int? = null,
        sharedReel: UserPost? = null
    ): ChatMessage {
        val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val msgId = "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"

        val message = ChatMessage(
            id = msgId,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = currentTimeStr,
            isFromMe = true,
            isRead = false, // Faqat serverga yetib borganida 1 ta galochka bo'ladi
            photoUri = photoUri,
            audioUri = audioUri,
            audioDurationSec = audioDurationSec,
            sharedReel = sharedReel
        )

        // 1. Store message in isolated conversation room
        val key = buildConvKey(senderId, receiverId)
        val list = conversationStore.getOrPut(key) { mutableListOf() }
        list.add(message)

        Log.d(TAG, "Message sent in room [$key]: ${message.text}")

        // 2. Real-time Event Emit (Socket.io event: receive_message)
        engineScope.launch {
            _messageEventFlow.emit(message)
        }

        return message
    }

    /**
     * O'qilmagan xabarlarni o'qilgan deb belgilash (is_read = true)
     */
    fun markMessagesAsRead(userId: String, candidateId: String) {
        val key = buildConvKey(userId, candidateId)
        val list = conversationStore[key] ?: return
        var updated = false
        for (i in list.indices) {
            val msg = list[i]
            if (msg.receiverId == userId && !msg.isRead) {
                list[i] = msg.copy(isRead = true)
                updated = true
            }
        }
        if (updated) {
            Log.d(TAG, "Messages marked as read for candidate: $candidateId")
        }
    }

    /**
     * Nomzod bo'yicha o'qilmagan xabarlar sonini hisoblash
     */
    fun getUnreadCount(userId: String, candidateId: String): Int {
        val key = buildConvKey(userId, candidateId)
        val list = conversationStore[key] ?: return 0
        return list.count { it.receiverId == userId && !it.isRead }
    }

    /**
     * Umumiy tumandagi o'qilmagan xabarlar soni
     */
    fun getTotalUnreadCount(userId: String): Int {
        var total = 0
        for (list in conversationStore.values) {
            total += list.count { it.receiverId == userId && !it.isRead }
        }
        return total
    }
}
