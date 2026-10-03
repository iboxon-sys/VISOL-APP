package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import com.example.model.Announcement
import com.example.model.ChatMessage
import com.example.model.CandidatePost
import com.example.model.ModerationSeverity
import com.example.model.ModerationStatus
import com.example.model.ModerationTicket
import com.example.model.ProfileVisitor
import com.example.model.SampleCandidates
import com.example.model.SampleReelsAndPosts
import com.example.model.TicketStatus
import com.example.model.UserPost
import com.example.model.UserProfile
import com.example.model.UserStory
import com.example.model.VoicePartyRoom
import com.example.model.VoiceRoomSpeaker
import com.example.network.BackendManager
import com.example.network.CandidateApiService
import com.example.network.CreatePostDto
import com.example.network.VisolApiService
import com.example.util.NotificationHelper
import com.example.util.TelegramModerationBotService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.example.network.ChatRealtimeEngine

class UserRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("visol_user_prefs", Context.MODE_PRIVATE)

    private val apiService by lazy { CandidateApiService.create() }
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val roomDb by lazy { AppDatabase.getDatabase(context) }
    private val roomChatDao by lazy { roomDb.roomChatDao() }

    val currentUserId: String get() = _currentUser.value?.id ?: "me"

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _subscriptionPlan = MutableStateFlow("")
    val subscriptionPlan: StateFlow<String> = _subscriptionPlan.asStateFlow()

    private val _subscriptionExpiresAt = MutableStateFlow(prefs.getLong(KEY_SUB_EXPIRES_AT, 0L))
    val subscriptionExpiresAt: StateFlow<Long> = _subscriptionExpiresAt.asStateFlow()

    private val _candidates = MutableStateFlow<List<UserProfile>>(emptyList())
    val candidates: StateFlow<List<UserProfile>> = _candidates.asStateFlow()

    private val _isLoadingCandidates = MutableStateFlow(false)
    val isLoadingCandidates: StateFlow<Boolean> = _isLoadingCandidates.asStateFlow()

    // Profilni kuzatgan shaxslar ro'yxati (Profile visitors list)
    private val _visitors = MutableStateFlow<List<ProfileVisitor>>(emptyList())
    val visitors: StateFlow<List<ProfileVisitor>> = _visitors.asStateFlow()

    // Foydalanuvchi oxirgi marta ko'rgan kuzatuvchilar soni (o'qilmaganlar hisobi uchun)
    private val _lastViewedVisitorCount = MutableStateFlow(0)
    val lastViewedVisitorCount: StateFlow<Int> = _lastViewedVisitorCount.asStateFlow()

    // Ijtimoiy postlar va Reels videolari
    private val _userPosts = MutableStateFlow<List<UserPost>>(emptyList())
    val userPosts: StateFlow<List<UserPost>> = _userPosts.asStateFlow()

    // Theme Mode ("SYSTEM", "LIGHT", "DARK")
    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Real-time Followed candidate IDs set
    private val _followedCandidateIds = MutableStateFlow<Set<String>>(emptySet())
    val followedCandidateIds: StateFlow<Set<String>> = _followedCandidateIds.asStateFlow()

    // Current user following count
    private val _userFollowingCount = MutableStateFlow(0)
    val userFollowingCount: StateFlow<Int> = _userFollowingCount.asStateFlow()

    // Contacts synced flag
    private val _isContactsSynced = MutableStateFlow(prefs.getBoolean(KEY_CONTACTS_SYNCED, false))
    val isContactsSynced: StateFlow<Boolean> = _isContactsSynced.asStateFlow()

    // Virtual Coins Balance for Voice Party Rooms & Live Streams (Default 0)
    private val _userCoins = MutableStateFlow(prefs.getInt("key_user_coins_balance", 1250))
    val userCoins: StateFlow<Int> = _userCoins.asStateFlow()

    // VIP Level (VIP 1 to VIP 12)
    private val _userVipLevel = MutableStateFlow(prefs.getInt("key_user_vip_level", 1))
    val userVipLevel: StateFlow<Int> = _userVipLevel.asStateFlow()

    fun spendCoins(amount: Int): Boolean {
        if (_userCoins.value >= amount) {
            _userCoins.value -= amount
            prefs.edit().putInt("key_user_coins_balance", _userCoins.value).apply()
            return true
        }
        return false
    }

    fun addCoins(amount: Int) {
        _userCoins.value += amount
        prefs.edit().putInt("key_user_coins_balance", _userCoins.value).apply()
    }

    fun upgradeVipLevel(newLevel: Int, coinCost: Int): Boolean {
        if (spendCoins(coinCost)) {
            _userVipLevel.value = newLevel.coerceIn(1, 12)
            prefs.edit().putInt("key_user_vip_level", _userVipLevel.value).apply()
            return true
        }
        return false
    }

    fun setUserVipLevel(level: Int) {
        _userVipLevel.value = level.coerceIn(1, 12)
        prefs.edit().putInt("key_user_vip_level", _userVipLevel.value).apply()
    }

    // Custom Voice Party Rooms
    private val _customVoiceRooms = MutableStateFlow<List<VoicePartyRoom>>(loadPersistedVoiceRooms())
    val customVoiceRooms: StateFlow<List<VoicePartyRoom>> = _customVoiceRooms.asStateFlow()

    fun createCustomVoiceRoom(
        title: String,
        isPrivate: Boolean,
        password: String?,
        announcement: String
    ): VoicePartyRoom {
        val user = _currentUser.value
        val newId = "room_${System.currentTimeMillis()}"
        val randomDisplayId = (10000000..99999999).random().toString()
        val newRoom = VoicePartyRoom(
            id = newId,
            title = if (title.isBlank()) "${user?.fullName ?: "Visol"} xonasi" else title,
            hostId = user?.id ?: "user_host",
            hostName = user?.fullName ?: "Mening Xonam",
            hostPhotoPath = user?.photoPath ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
            hostGender = user?.gender ?: "Ayol",
            hostBadge = "👑 Owner (Ega)",
            category = "Shaxsiy Xona",
            announcementText = if (announcement.isBlank()) "Xush kelibsiz! Marhamat, muloqot qiling va sovg'alar ulashing! ✨" else announcement,
            isPrivate = isPrivate,
            roomPassword = if (isPrivate) password else null,
            roomIdDisplay = randomDisplayId,
            listenerCount = 1,
            speakers = listOf(
                VoiceRoomSpeaker(
                    seatIndex = 0,
                    userId = user?.id ?: "user_host",
                    userName = user?.fullName ?: "Mening Xonam",
                    photoPath = user?.photoPath ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
                    gender = user?.gender ?: "Ayol",
                    isHost = true,
                    isMuted = false,
                    isSpeaking = false,
                    vipLevel = 5,
                    starPoints = 100
                )
            )
        )
        val updatedList = listOf(newRoom) + _customVoiceRooms.value
        _customVoiceRooms.value = updatedList
        saveVoiceRoomsToPrefs(updatedList)
        return newRoom
    }

    fun updateVoiceRoom(
        roomId: String,
        newTitle: String,
        newAnnouncement: String,
        isPrivate: Boolean,
        password: String?
    ) {
        val updatedList = _customVoiceRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(
                    title = newTitle,
                    announcementText = newAnnouncement,
                    isPrivate = isPrivate,
                    roomPassword = password
                )
            } else room
        }
        _customVoiceRooms.value = updatedList
        saveVoiceRoomsToPrefs(updatedList)
    }

    private fun saveVoiceRoomsToPrefs(rooms: List<VoicePartyRoom>) {
        try {
            val array = org.json.JSONArray()
            rooms.forEach { room ->
                val obj = org.json.JSONObject()
                obj.put("id", room.id)
                obj.put("title", room.title)
                obj.put("hostId", room.hostId)
                obj.put("hostName", room.hostName)
                obj.put("hostPhotoPath", room.hostPhotoPath)
                obj.put("hostGender", room.hostGender)
                obj.put("hostBadge", room.hostBadge)
                obj.put("category", room.category)
                obj.put("announcementText", room.announcementText)
                obj.put("isPrivate", room.isPrivate)
                obj.put("roomPassword", room.roomPassword ?: "")
                obj.put("roomIdDisplay", room.roomIdDisplay)
                obj.put("isLiveStream", room.isLiveStream)
                array.put(obj)
            }
            prefs.edit().putString("key_persisted_custom_voice_rooms", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPersistedVoiceRooms(): List<VoicePartyRoom> {
        val jsonString = prefs.getString("key_persisted_custom_voice_rooms", null) ?: return emptyList()
        val list = mutableListOf<VoicePartyRoom>()
        try {
            val array = org.json.JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val hostId = obj.optString("hostId", "user_host")
                val hostName = obj.optString("hostName", "Mening Xonam")
                val hostPhoto = obj.optString("hostPhotoPath", "")
                list.add(
                    VoicePartyRoom(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        hostId = hostId,
                        hostName = hostName,
                        hostPhotoPath = hostPhoto,
                        hostGender = obj.optString("hostGender", "Ayol"),
                        hostBadge = obj.optString("hostBadge", "👑 Owner"),
                        category = obj.optString("category", "Shaxsiy Xona"),
                        announcementText = obj.optString("announcementText", ""),
                        isPrivate = obj.optBoolean("isPrivate", false),
                        roomPassword = obj.optString("roomPassword", "").ifBlank { null },
                        roomIdDisplay = obj.optString("roomIdDisplay", "10000000"),
                        isLiveStream = obj.optBoolean("isLiveStream", false),
                        speakers = listOf(
                            VoiceRoomSpeaker(
                                seatIndex = 0,
                                userId = hostId,
                                userName = hostName,
                                photoPath = hostPhoto,
                                isHost = true
                            )
                        )
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // FCM Device Token for Push Notifications
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    // Moderatsiya va Telegram Bot Tekshiruv Chiptalari (Moderation Tickets)
    private val _moderationTickets = MutableStateFlow<List<ModerationTicket>>(loadPersistedModerationTickets())
    val moderationTickets: StateFlow<List<ModerationTicket>> = _moderationTickets.asStateFlow()

    /**
     * Yangi shubhali kontent chiptasini yaratish va saqlash
     */
    fun createModerationTicket(ticket: ModerationTicket) {
        val updated = listOf(ticket) + _moderationTickets.value.filter { it.ticketId != ticket.ticketId }
        _moderationTickets.value = updated
        saveModerationTicketsToPrefs(updated)

        // Telegram Bot orqali Adminga xabar jo'natish
        repositoryScope.launch {
            try {
                val sendResult = TelegramModerationBotService.sendSuspiciousContentAlert(context, ticket)
                if (sendResult.isSuccess) {
                    val msgId = sendResult.getOrNull()
                    if (msgId != null) {
                        val ticketWithMsg = ticket.copy(telegramMessageId = msgId)
                        updateModerationTicket(ticketWithMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Failed sending ticket to Telegram: ${e.message}")
            }
        }
    }

    /**
     * Moderatsiya chiptasini yangilash
     */
    fun updateModerationTicket(ticket: ModerationTicket) {
        val updated = _moderationTickets.value.map {
            if (it.ticketId == ticket.ticketId) ticket else it
        }
        _moderationTickets.value = updated
        saveModerationTicketsToPrefs(updated)
    }

    /**
     * Foydalanuvchini vaqtinchalik cheklash (is_restricted = true / false)
     */
    fun setUserRestricted(userId: String, isRestricted: Boolean, reason: String = "") {
        val cur = _currentUser.value
        if (cur != null && (cur.id == userId || userId == "user_me" || userId == "me")) {
            val updatedUser = cur.copy(
                isRestricted = isRestricted,
                restrictionReason = if (isRestricted) reason else "",
                restrictionTimestamp = if (isRestricted) System.currentTimeMillis() else 0L
            )
            registerUser(updatedUser)
        }

        // Nomzodlar ro'yxatida ham yangilash
        val updatedCandidates = _candidates.value.map { candidate ->
            if (candidate.id == userId) {
                candidate.copy(
                    isRestricted = isRestricted,
                    restrictionReason = if (isRestricted) reason else "",
                    restrictionTimestamp = if (isRestricted) System.currentTimeMillis() else 0L
                )
            } else candidate
        }
        _candidates.value = updatedCandidates
    }

    /**
     * Foydalanuvchiga spam cheklovi qo'yish (video yuklay olmaydi, lekin efir va boshqa funksiyalar ishlayveradi)
     */
    fun setUserSpamRestricted(userId: String, isSpam: Boolean) {
        val cur = _currentUser.value
        if (cur != null && (cur.id == userId || userId == "user_me" || userId == "me")) {
            val updatedUser = cur.copy(
                isSpamRestricted = isSpam,
                isRestricted = false
            )
            registerUser(updatedUser)
        }

        val updatedCandidates = _candidates.value.map { candidate ->
            if (candidate.id == userId) {
                candidate.copy(
                    isSpamRestricted = isSpam,
                    isRestricted = false
                )
            } else candidate
        }
        _candidates.value = updatedCandidates
    }

    /**
     * Admin tasdiqlaganida chiptadagi post/reelsni e'lon qilish (yuklash)
     */
    fun publishPostFromTicket(ticket: ModerationTicket) {
        val newPost = UserPost(
            id = "post_approved_${ticket.ticketId.take(8)}_${System.currentTimeMillis()}",
            authorId = ticket.userId,
            authorName = ticket.userName,
            authorGender = ticket.userGender,
            authorPhotoPath = ticket.userPhotoPath,
            authorLocation = ticket.userLocation.ifBlank { "Toshkent shahri" },
            title = ticket.caption,
            mediaType = ticket.mediaType,
            mediaUri = ticket.mediaUri,
            timestamp = "Hozirgina",
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            musicTitle = ticket.musicTitle,
            musicArtist = ticket.musicArtist,
            musicAudioUrl = ticket.musicAudioUrl,
            moderationStatus = ModerationStatus.APPROVED
        )

        val updatedList = listOf(newPost) + _userPosts.value.filter { it.id != newPost.id }
        _userPosts.value = updatedList
        savePostsToPreferences(updatedList)
    }

    /**
     * Foydalanuvchini to'liq ban qilish (is_banned = true)
     */
    fun setUserBanned(userId: String, isBanned: Boolean) {
        val cur = _currentUser.value
        if (cur != null && (cur.id == userId || userId == "user_me" || userId == "me")) {
            val updatedUser = cur.copy(
                isBanned = isBanned,
                isRestricted = isBanned
            )
            registerUser(updatedUser)
        }

        val updatedCandidates = _candidates.value.map { candidate ->
            if (candidate.id == userId) {
                candidate.copy(
                    isBanned = isBanned,
                    isRestricted = isBanned
                )
            } else candidate
        }
        _candidates.value = updatedCandidates
    }

    /**
     * Foydalanuvchiga rasmiy tizim bildirishnomasi jo'natish (Chat va Tizim orqali)
     */
    fun sendSystemNoticeToUser(targetUserId: String, title: String, body: String) {
        // Chat orqali yuborish
        sendChatMessage(
            candidateId = "admin_009",
            text = "📩 <b>$title</b>\n\n$body"
        )
        // Local Notification chiqarish
        try {
            NotificationHelper.showFcmNotification(
                context = context,
                title = title,
                body = body,
                notificationId = (System.currentTimeMillis() % 10000).toInt()
            )
        } catch (_: Exception) {}
    }

    private fun saveModerationTicketsToPrefs(tickets: List<ModerationTicket>) {
        try {
            val jsonArray = org.json.JSONArray()
            tickets.forEach { ticket ->
                val obj = JSONObject().apply {
                    put("ticketId", ticket.ticketId)
                    put("userId", ticket.userId)
                    put("userName", ticket.userName)
                    put("userUsername", ticket.userUsername)
                    put("userPhotoPath", ticket.userPhotoPath)
                    put("userGender", ticket.userGender)
                    put("userLocation", ticket.userLocation)
                    put("mediaType", ticket.mediaType)
                    put("mediaUri", ticket.mediaUri)
                    put("caption", ticket.caption)
                    ticket.musicTitle?.let { put("musicTitle", it) }
                    ticket.musicArtist?.let { put("musicArtist", it) }
                    ticket.musicAudioUrl?.let { put("musicAudioUrl", it) }
                    put("severity", ticket.severity.name)
                    put("reason", ticket.reason)
                    put("confidence", ticket.confidence.toDouble())
                    put("timestamp", ticket.timestamp)
                    put("status", ticket.status.name)
                    ticket.telegramMessageId?.let { put("telegramMessageId", it) }
                    ticket.resolutionNote?.let { put("resolutionNote", it) }
                    ticket.resolvedAt?.let { put("resolvedAt", it) }
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("key_moderation_tickets_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving moderation tickets: ${e.message}")
        }
    }

    private fun loadPersistedModerationTickets(): List<ModerationTicket> {
        val list = mutableListOf<ModerationTicket>()
        try {
            val jsonStr = prefs.getString("key_moderation_tickets_json", null)
            if (!jsonStr.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val severityStr = obj.optString("severity", ModerationSeverity.LEVEL_2_SUSPICIOUS.name)
                    val statusStr = obj.optString("status", TicketStatus.PENDING.name)
                    val ticket = ModerationTicket(
                        ticketId = obj.optString("ticketId", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", "Foydalanuvchi"),
                        userUsername = obj.optString("userUsername", ""),
                        userPhotoPath = obj.optString("userPhotoPath", ""),
                        userGender = obj.optString("userGender", "Erkak"),
                        userLocation = obj.optString("userLocation", ""),
                        mediaType = obj.optString("mediaType", "REELS"),
                        mediaUri = obj.optString("mediaUri", ""),
                        caption = obj.optString("caption", ""),
                        musicTitle = if (obj.has("musicTitle")) obj.getString("musicTitle") else null,
                        musicArtist = if (obj.has("musicArtist")) obj.getString("musicArtist") else null,
                        musicAudioUrl = if (obj.has("musicAudioUrl")) obj.getString("musicAudioUrl") else null,
                        severity = try { ModerationSeverity.valueOf(severityStr) } catch (_: Exception) { ModerationSeverity.LEVEL_2_SUSPICIOUS },
                        reason = obj.optString("reason", "Shubhali kadrlar"),
                        confidence = obj.optDouble("confidence", 0.5).toFloat(),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        status = try { TicketStatus.valueOf(statusStr) } catch (_: Exception) { TicketStatus.PENDING },
                        telegramMessageId = if (obj.has("telegramMessageId")) obj.getLong("telegramMessageId") else null,
                        resolutionNote = if (obj.has("resolutionNote")) obj.getString("resolutionNote") else null,
                        resolvedAt = if (obj.has("resolvedAt")) obj.getLong("resolvedAt") else null
                    )
                    list.add(ticket)
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error loading moderation tickets: ${e.message}")
        }
        return list
    }

    /**
     * Foydalanuvchi ism, username yoki rasm o'zgartirsa adminga xabar berish
     */
    fun notifyAdminOfProfileUpdate(oldUser: UserProfile, newUser: UserProfile) {
        if (oldUser.fullName != newUser.fullName || oldUser.username != newUser.username || oldUser.photoPath != newUser.photoPath) {
            val updateMsg = "Foydalanuvchi ma'lumotini yangiladi:\n" +
                    "Ism: ${oldUser.fullName} -> ${newUser.fullName}\n" +
                    "Username: ${oldUser.username} -> ${newUser.username}\n" +
                    "Rasm o'zgargan: ${oldUser.photoPath != newUser.photoPath}"
            // Admin "admin_009" ga chat xabarini yuborish
            sendChatMessage(
                candidateId = "admin_009",
                text = "🔔 $updateMsg"
            )
            Log.d("UserRepository", "Admin notified of profile update: ${newUser.id}")
        }
    }

    /**
     * Profil ma'lumotlarini yangilash va bazaga saqlash
     */
    fun updateUserProfile(newUser: UserProfile) {
        val oldUser = _currentUser.value
        if (oldUser != null) {
            notifyAdminOfProfileUpdate(oldUser, newUser)
        }
        registerUser(newUser)
    }

    /**
     * Parolni o'zgartirish (simulyatsiya)
     */
    fun changePassword(newPassword: String) {
        // Parolni saqlash mantig'i qo'shilishi kerak
        Log.d("UserRepository", "Password changed for user: ${currentUserId}")
        sendChatMessage(
            candidateId = "admin_009",
            text = "🔔 Foydalanuvchi parolni yangiladi."
        )
    }

    // Chat Orqa Foni (Wallpaper URI - Har bir chat xonasi yoki umumiy uchun)
    private val _chatWallpaperUri = MutableStateFlow<String?>(prefs.getString("key_chat_wallpaper_uri", null))
    val chatWallpaperUri: StateFlow<String?> = _chatWallpaperUri.asStateFlow()

    private val _chatWallpapersByCandidate = MutableStateFlow<Map<String, String>>(emptyMap())
    val chatWallpapersByCandidate: StateFlow<Map<String, String>> = _chatWallpapersByCandidate.asStateFlow()

    fun getChatWallpaperUri(candidateId: String): String? {
        return _chatWallpapersByCandidate.value[candidateId]
            ?: prefs.getString("key_chat_wallpaper_$candidateId", null)
            ?: _chatWallpaperUri.value
    }

    fun setChatWallpaperForCandidate(candidateId: String, uriString: String?) {
        val updated = _chatWallpapersByCandidate.value.toMutableMap()
        if (uriString == null) {
            updated.remove(candidateId)
            prefs.edit().remove("key_chat_wallpaper_$candidateId").apply()
        } else {
            updated[candidateId] = uriString
            prefs.edit().putString("key_chat_wallpaper_$candidateId", uriString).apply()
        }
        _chatWallpapersByCandidate.value = updated
        _chatWallpaperUri.value = uriString
    }

    fun setChatWallpaperUri(uriString: String?) {
        _chatWallpaperUri.value = uriString
        if (uriString == null) {
            prefs.edit().remove("key_chat_wallpaper_uri").apply()
        } else {
            prefs.edit().putString("key_chat_wallpaper_uri", uriString).apply()
        }
    }

    // Maxsus saqlangan nomlar (Custom contact nicknames, e.g. "Jonim", "Mening juftim 🌹")
    private val _customContactNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val customContactNames: StateFlow<Map<String, String>> = _customContactNames.asStateFlow()

    fun setCustomContactName(candidateId: String, customName: String) {
        val updated = _customContactNames.value.toMutableMap()
        if (customName.isBlank()) {
            updated.remove(candidateId)
            prefs.edit().remove("key_custom_contact_$candidateId").apply()
        } else {
            updated[candidateId] = customName.trim()
            prefs.edit().putString("key_custom_contact_$candidateId", customName.trim()).apply()
        }
        _customContactNames.value = updated
    }

    fun getDisplayNameForCandidate(candidate: UserProfile): String {
        return _customContactNames.value[candidate.id]?.takeIf { it.isNotBlank() } ?: candidate.fullName
    }

    fun getDisplayNameById(candidateId: String, fallbackName: String): String {
        return _customContactNames.value[candidateId]?.takeIf { it.isNotBlank() } ?: fallbackName
    }

    // Nomzodlar bilan chat xabarlari (candidateId -> List<ChatMessage>)
    private val _chatMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val chatMessages: StateFlow<Map<String, List<ChatMessage>>> = _chatMessages.asStateFlow()

    // O'qilmagan xabarlarning umumiy hisoblagichi (Tab bar va umumiy indikator uchun)
    val totalUnreadCount: StateFlow<Int> = _chatMessages.map { map ->
        map.values.sumOf { list -> list.count { !it.isFromMe && !it.isRead } }
    }.stateIn(repositoryScope, SharingStarted.Eagerly, 0)

    // Following / Obunalar tracking set
    private val _followingUserIds = MutableStateFlow<Set<String>>(emptySet())
    val followingUserIds: StateFlow<Set<String>> = _followingUserIds.asStateFlow()

    // Liked candidates tracking set
    private val _likedCandidateIds = MutableStateFlow<Set<String>>(emptySet())
    val likedCandidateIds: StateFlow<Set<String>> = _likedCandidateIds.asStateFlow()

    // Stories (Istoryalar) - 24 soatlik muddatli hikoyalar
    private val _userStories = MutableStateFlow<List<UserStory>>(emptyList())
    val userStories: StateFlow<List<UserStory>> = _userStories.asStateFlow()

    // E'lonlar (Announcements)
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    init {
        loadUserState()
        loadFollowingState()
        loadLikedState()
        loadCustomContactNames()
        loadCandidates()
        loadInitialVisitors()
        loadInitialPosts()
        loadInitialStories()
        loadInitialAnnouncements()
        fetchCandidatesFromApi()
        initRealtimeChatListener()
        try {
            com.example.util.TelegramBotPollingService.startPolling(context, this)
        } catch (_: Exception) {}
    }

    private fun loadInitialAnnouncements() {
        val initialList = listOf(
            Announcement(
                id = "ann_1",
                authorId = "cand_1",
                authorName = "Madina Karimova",
                authorGender = "Ayol",
                authorPhotoPath = "avatar_female_1",
                title = "Jiddiy niyatli sovchilik uchun e'lon 💍",
                description = "Assalomu alaykum. Oliy ma'lumotli, oqila, dininga va milliy qadriyatlarimizga sodiq qizimiz uchun andijonlik yoki toshkentlik munosib, oilani qadrlaydigan yigitlar bilan tanishuv maqsadida.",
                category = "Sovchilik",
                region = "Toshkent shahri",
                contactPhone = "+998 90 123 45 67",
                telegramUsername = "madina_karimova",
                timestamp = "Bugun 11:30",
                viewsCount = 384,
                likesCount = 56,
                isVerified = true,
                comments = listOf(
                    com.example.model.AnnouncementComment(
                        id = "comm_1_1",
                        announcementId = "ann_1",
                        authorId = "cand_4",
                        authorName = "Sardorbek Rahimov",
                        authorGender = "Erkak",
                        authorPhotoPath = "avatar_male_2",
                        isVerified = true,
                        text = "Assalomu alaykum. Juda yaxshi niyat qilibsizlar, baxtli bo'lishsin!",
                        timestamp = "Bugun 12:10",
                        likesCount = 12,
                        replies = listOf(
                            com.example.model.AnnouncementComment(
                                id = "comm_1_1_r1",
                                announcementId = "ann_1",
                                authorId = "cand_1",
                                authorName = "Madina Karimova",
                                authorGender = "Ayol",
                                authorPhotoPath = "avatar_female_1",
                                isVerified = true,
                                text = "Rahmat Sardorbek, tashakkur!",
                                timestamp = "Bugun 12:45",
                                likesCount = 4,
                                parentCommentId = "comm_1_1",
                                replyToAuthorName = "Sardorbek Rahimov"
                            )
                        )
                    ),
                    com.example.model.AnnouncementComment(
                        id = "comm_1_2",
                        announcementId = "ann_1",
                        authorId = "cand_3",
                        authorName = "Shahzod Mirzayev",
                        authorGender = "Erkak",
                        authorPhotoPath = "avatar_male_1",
                        isVerified = true,
                        text = "Alloh xayrli juft nasib aylasin, niyatlaringizga yetinglar 🤲",
                        timestamp = "Bugun 13:00",
                        likesCount = 9
                    )
                )
            ),
            Announcement(
                id = "ann_2",
                authorId = "cand_4",
                authorName = "Sardorbek Rahimov",
                authorGender = "Erkak",
                authorPhotoPath = "avatar_male_2",
                title = "Halol va samimiy oila qurish niyatida 🤲",
                description = "IT sohasida ishlayman. 26 yoshdaman. Dunyoqarashi keng, pazanda, iboli va ilmli qiz bilan hayot yo'limizni bog'lash niyatidaman. Sovchilar orqali ko'rishish afzal.",
                category = "Sovchilik",
                region = "Samarqand viloyati",
                contactPhone = "+998 93 456 78 90",
                telegramUsername = "sardor_rahimov",
                timestamp = "Bugun 09:15",
                viewsCount = 290,
                likesCount = 42,
                isVerified = true,
                comments = listOf(
                    com.example.model.AnnouncementComment(
                        id = "comm_2_1",
                        announcementId = "ann_2",
                        authorId = "cand_2",
                        authorName = "Zilola Rahimova",
                        authorGender = "Ayol",
                        authorPhotoPath = "avatar_female_2",
                        isVerified = true,
                        text = "Assalomu alaykum. Samarqanddanmiz biz ham, profil ma'lumotlaringiz ma'qul keldi.",
                        timestamp = "Bugun 10:20",
                        likesCount = 8
                    )
                )
            ),
            Announcement(
                id = "ann_3",
                authorId = "cand_7",
                authorName = "Nigora Usmonova",
                authorGender = "Ayol",
                authorPhotoPath = "avatar_female_4",
                title = "To'y liboslari va kelin sarpolari xizmati 💐",
                description = "Yangi turmush qurayotgan yoshlarimiz uchun milliy va zamonaviy kelin ko'ylaklari hamda sarpo bezatish xizmatlarini taqdim etamiz.",
                category = "To'y & Nikoh",
                region = "Farg'ona viloyati",
                contactPhone = "+998 97 789 01 23",
                telegramUsername = "nigora_sarpo",
                timestamp = "Kecha 18:40",
                viewsCount = 512,
                likesCount = 89,
                isVerified = true,
                comments = emptyList()
            ),
            Announcement(
                id = "ann_4",
                authorId = "cand_3",
                authorName = "Shahzod Mirzayev",
                authorGender = "Erkak",
                authorPhotoPath = "avatar_male_1",
                title = "Nikoh va oila odoblari bo'yicha maslahatlar 📖",
                description = "Baxtli va mustahkam oila qurish sirlari, er-xotin huquq va burchlari haqida foydali tavsiyalar.",
                category = "Tavsiyalar",
                region = "Buxoro viloyati",
                contactPhone = "+998 91 345 67 89",
                telegramUsername = "shahzod_visol",
                timestamp = "2 kun oldin",
                viewsCount = 670,
                likesCount = 114,
                isVerified = true,
                comments = emptyList()
            )
        )
        _announcements.value = initialList
    }

    fun postAnnouncement(
        title: String,
        description: String,
        category: String,
        region: String,
        contactPhone: String,
        telegram: String
    ) {
        val user = _currentUser.value
        val hasVip = _isSubscribed.value
        val newAnn = Announcement(
            authorId = user?.id ?: "user_me",
            authorName = user?.fullName ?: "Foydalanuvchi",
            authorGender = user?.gender ?: "Ayol",
            authorPhotoPath = user?.photoPath,
            title = title.trim(),
            description = description.trim(),
            category = category,
            region = region,
            contactPhone = contactPhone.trim(),
            telegramUsername = telegram.trim().removePrefix("@"),
            timestamp = "Hozirgina",
            viewsCount = 1,
            likesCount = 0,
            isVerified = hasVip,
            comments = emptyList()
        )
        _announcements.value = listOf(newAnn) + _announcements.value
    }

    fun toggleAnnouncementLike(announcementId: String) {
        _announcements.value = _announcements.value.map { ann ->
            if (ann.id == announcementId) {
                val newLiked = !ann.isLiked
                val newLikes = if (newLiked) ann.likesCount + 1 else maxOf(0, ann.likesCount - 1)
                ann.copy(isLiked = newLiked, likesCount = newLikes)
            } else ann
        }
    }

    fun addAnnouncementComment(
        announcementId: String,
        text: String,
        parentCommentId: String? = null,
        replyToAuthorName: String? = null
    ) {
        if (text.isBlank()) return
        val user = _currentUser.value
        val hasVip = _isSubscribed.value

        val newComment = com.example.model.AnnouncementComment(
            announcementId = announcementId,
            authorId = user?.id ?: "user_me",
            authorName = user?.fullName ?: "Foydalanuvchi",
            authorGender = user?.gender ?: "Ayol",
            authorPhotoPath = user?.photoPath,
            isVerified = hasVip,
            text = text.trim(),
            timestamp = "Hozirgina",
            parentCommentId = parentCommentId,
            replyToAuthorName = replyToAuthorName
        )

        _announcements.value = _announcements.value.map { ann ->
            if (ann.id == announcementId) {
                if (parentCommentId == null) {
                    // Yangi asosiy komment
                    ann.copy(comments = ann.comments + newComment)
                } else {
                    // Otvet (javob / reply) qo'shish
                    val updatedComments = ann.comments.map { comment ->
                        if (comment.id == parentCommentId) {
                            comment.copy(replies = comment.replies + newComment)
                        } else {
                            comment
                        }
                    }
                    ann.copy(comments = updatedComments)
                }
            } else {
                ann
            }
        }
    }

    fun toggleAnnouncementCommentLike(announcementId: String, commentId: String) {
        _announcements.value = _announcements.value.map { ann ->
            if (ann.id == announcementId) {
                val updatedComments = ann.comments.map { comment ->
                    if (comment.id == commentId) {
                        val newLiked = !comment.isLiked
                        val newCount = if (newLiked) comment.likesCount + 1 else maxOf(0, comment.likesCount - 1)
                        comment.copy(isLiked = newLiked, likesCount = newCount)
                    } else {
                        val updatedReplies = comment.replies.map { reply ->
                            if (reply.id == commentId) {
                                val newLiked = !reply.isLiked
                                val newCount = if (newLiked) reply.likesCount + 1 else maxOf(0, reply.likesCount - 1)
                                reply.copy(isLiked = newLiked, likesCount = newCount)
                            } else {
                                reply
                            }
                        }
                        comment.copy(replies = updatedReplies)
                    }
                }
                ann.copy(comments = updatedComments)
            } else {
                ann
            }
        }
    }

    private fun initRealtimeChatListener() {
        repositoryScope.launch {
            ChatRealtimeEngine.messageEventFlow.collect { message ->
                val currentId = currentUserId
                // Strict isolation: only save/process messages belonging to the current user's session
                if (message.senderId == currentId || message.receiverId == currentId) {
                    val roomMsg = RoomChatMessage.fromChatMessage(message, currentId)
                    try {
                        roomChatDao.insertMessage(roomMsg)
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Failed to save message to Room database", e)
                    }

                    withContext(Dispatchers.Main) {
                        val candidateId = if (message.senderId == currentId) message.receiverId else message.senderId
                        val currentMap = HashMap(_chatMessages.value)
                        val list = (currentMap[candidateId] ?: emptyList()).toMutableList()
                        val existingIndex = list.indexOfFirst { it.id == message.id }
                        val correctedMsg = message.copy(isFromMe = message.senderId == currentId)
                        if (existingIndex == -1) {
                            list.add(correctedMsg)
                        } else {
                            list[existingIndex] = correctedMsg
                        }
                        currentMap[candidateId] = list
                        _chatMessages.value = currentMap
                    }
                }
            }
        }

        // Global Real-time Multi-Device Sync Bridge Polling
        repositoryScope.launch(Dispatchers.IO) {
            while (true) {
                val currentId = currentUserId
                if (currentId.isNotBlank()) {
                    try {
                        val senders = com.example.network.FirebaseRealtimeSync.fetchUserInbox(currentId)
                        senders.forEach { senderId ->
                            if (senderId != currentId) {
                                // Start polling/syncing chat history for this sender automatically
                                withContext(Dispatchers.Main) {
                                    startPollingMessagesForCandidate(senderId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("UserRepository", "Global multi-device bridge error: ${e.message}")
                    }
                }
                delay(2500) // Poll for new active chats every 2.5 seconds
            }
        }

        // Global Multi-Device Profile Directory Syncer Loop
        repositoryScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val firebaseProfiles = com.example.network.FirebaseRealtimeSync.fetchAllProfilesFromFirebase()
                    if (firebaseProfiles.isNotEmpty()) {
                        val currentId = currentUserId
                        // Filter out self and merge
                        val filtered = firebaseProfiles.filter { it.id != currentId }
                        if (filtered.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                val currentCandidates = _candidates.value.toMutableList()
                                var updated = false
                                filtered.forEach { fProfile ->
                                    val index = currentCandidates.indexOfFirst { it.id == fProfile.id }
                                    if (index != -1) {
                                        if (currentCandidates[index].fullName != fProfile.fullName || currentCandidates[index].photoPath != fProfile.photoPath) {
                                            currentCandidates[index] = fProfile
                                            updated = true
                                        }
                                    } else {
                                        currentCandidates.add(0, fProfile)
                                        updated = true
                                    }
                                }
                                if (updated) {
                                    _candidates.value = currentCandidates
                                    Log.d("UserRepository", "Firebase global candidates directory synced dynamically. Candidates count: ${currentCandidates.size}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("UserRepository", "Global candidates sync directory error: ${e.message}")
                }
                delay(4000) // Poll for new registered users across devices every 4 seconds
            }
        }
    }

    private fun loadCustomContactNames() {
        val allPrefs = prefs.all
        val map = mutableMapOf<String, String>()
        for ((key, value) in allPrefs) {
            if (key.startsWith("key_custom_contact_") && value is String && value.isNotBlank()) {
                val candidateId = key.removePrefix("key_custom_contact_")
                map[candidateId] = value
            }
        }
        _customContactNames.value = map
    }

    private fun loadFollowingState() {
        val savedSet = prefs.getStringSet(KEY_FOLLOWING_USERS_SET, null) ?: setOf("admin_009")
        val cleanSet = (savedSet + "admin_009").toSet()
        _followingUserIds.value = cleanSet
        _followedCandidateIds.value = cleanSet
        _userFollowingCount.value = cleanSet.size
    }

    private fun loadLikedState() {
        val saved = prefs.getStringSet("key_liked_candidates_set", emptySet()) ?: emptySet()
        _likedCandidateIds.value = saved
    }

    fun toggleLikeCandidate(candidateId: String): Boolean {
        val currentSet = _likedCandidateIds.value.toMutableSet()
        val isNowLiked = if (currentSet.contains(candidateId)) {
            currentSet.remove(candidateId)
            false
        } else {
            currentSet.add(candidateId)
            true
        }
        _likedCandidateIds.value = currentSet
        prefs.edit().putStringSet("key_liked_candidates_set", currentSet).apply()
        return isNowLiked
    }

    fun isCandidateLiked(candidateId: String): Boolean {
        return _likedCandidateIds.value.contains(candidateId)
    }

    /**
     * Mavzuni yangilash (LIGHT yoki DARK)
     */
    fun setThemeMode(mode: String) {
        val target = mode.uppercase()
        prefs.edit().putString(KEY_THEME_MODE, target).apply()
        _themeMode.value = target
    }

    fun toggleThemeMode() {
        val current = _themeMode.value.uppercase()
        val next = if (current == "DARK") "LIGHT" else "DARK"
        setThemeMode(next)
    }

    /**
     * Har bir foydalanuvchini adminga (@visoladmin_009) obuna qilish
     */
    fun ensureFollowingAdmin() {
        val current = _followingUserIds.value.toMutableSet()
        current.add("admin_009")
        _followingUserIds.value = current
        _followedCandidateIds.value = current
        _userFollowingCount.value = current.size
        prefs.edit().putStringSet(KEY_FOLLOWING_USERS_SET, current).apply()
    }

    /**
     * Obunani yoqish yoki bekor qilish (Optimistic update + backend sync)
     */
    fun toggleFollowStatus(targetUserId: String): Flow<Boolean> = flow {
        val currentSet = _followingUserIds.value.toMutableSet()
        val isNowFollowing = if (currentSet.contains(targetUserId)) {
            currentSet.remove(targetUserId)
            false
        } else {
            currentSet.add(targetUserId)
            true
        }
        _followingUserIds.value = currentSet
        prefs.edit().putStringSet(KEY_FOLLOWING_USERS_SET, currentSet).apply()

        // Update current user following count
        _currentUser.value?.let { user ->
            _currentUser.value = user.copy(followingCount = currentSet.size)
        }

        try {
            apiService.toggleFollow(targetUserId)
        } catch (e: Exception) {
            Log.w("UserRepository", "API follow toggle sync notice: ${e.message}")
        }

        emit(isNowFollowing)
    }

    /**
     * Real-time Followers List fetching
     */
    fun getFollowersList(userId: String): Flow<List<UserProfile>> = flow {
        try {
            val response = apiService.getFollowers(userId)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                emit(response.body()!!.map { it.toUserProfile() })
                return@flow
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "Failed fetching followers from API: ${e.message}")
        }
        val isMe = userId == (_currentUser.value?.id ?: "user_me")
        if (isMe) {
            emit(emptyList())
        } else {
            val isFollowedByMe = _followingUserIds.value.contains(userId) || _followedCandidateIds.value.contains(userId)
            if (isFollowedByMe) {
                emit(listOf(getOrCreateActiveUser()))
            } else {
                emit(emptyList())
            }
        }
    }

    /**
     * Real-time Following List fetching
     */
    fun getFollowingList(userId: String): Flow<List<UserProfile>> = flow {
        try {
            val response = apiService.getFollowing(userId)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                emit(response.body()!!.map { it.toUserProfile() })
                return@flow
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "Failed fetching following from API: ${e.message}")
        }
        val candidatesList = _candidates.value
        val followingUsers = candidatesList.filter { _followingUserIds.value.contains(it.id) }
        emit(followingUsers)
    }

    private fun loadUserState() {
        val isRegistered = prefs.getBoolean(KEY_IS_REGISTERED, false)
        Log.d("UserRepository", "loadUserState: isRegistered = $isRegistered")
        if (isRegistered) {
            val userJson = prefs.getString(KEY_USER_DATA, null)
            Log.d("UserRepository", "loadUserState: userJson found = ${userJson != null}")
            if (userJson != null) {
                try {
                    val obj = JSONObject(userJson)
                    val birthDateVal = obj.optString("birthDate", "")
                    val rawAge = obj.optInt("age", 20)
                    val effectiveAge = if (birthDateVal.isNotBlank()) {
                        val calc = UserProfile.calculateAgeFromBirthDate(birthDateVal)
                        if (calc > 0) calc else rawAge
                    } else rawAge
                    Log.d("UserRepository", "loadUserState: successfully loaded user ${obj.optString("fullName")}")
                    val profile = UserProfile(
                        id = obj.getString("id"),
                        fullName = obj.getString("fullName"),
                        gender = obj.getString("gender"),
                        age = effectiveAge,
                        viloyat = obj.getString("viloyat"),
                        tuman = obj.getString("tuman"),
                        mahalla = obj.getString("mahalla"),
                        photoPath = obj.getString("photoPath"),
                        bio = obj.getString("bio"),
                        username = obj.optString("username", ""),
                        telegram = obj.optString("telegram", ""),
                        instagram = obj.optString("instagram", ""),
                        tiktok = obj.optString("tiktok", ""),
                        phone = obj.getString("phone"),
                        isVerifiedCamera = obj.optBoolean("isVerifiedCamera", true),
                        isCurrentUser = true,
                        birthDate = birthDateVal,
                        isPrivate = obj.optBoolean("isPrivate", false),
                        isMatched = obj.optBoolean("isMatched", false) || prefs.getBoolean("key_my_pair_matched", false)
                    )
                    _currentUser.value = profile
                } catch (e: Exception) {
                    Log.e("UserRepository", "loadUserState: error parsing user JSON", e)
                    _currentUser.value = null
                }
            }
        } else {
            _currentUser.value = null
        }

        _isSubscribed.value = prefs.getBoolean(KEY_IS_SUBSCRIBED, false)
        _subscriptionPlan.value = prefs.getString(KEY_SUB_PLAN, "") ?: ""
        _fcmToken.value = prefs.getString(KEY_FCM_TOKEN, null)
    }

    private fun loadCandidates() {
        val list = SampleCandidates.initialList
        val persisted = loadPersistedRegisteredUsers()
        val combined = (persisted + list).distinctBy { it.id }
        val matchedIds = prefs.getStringSet("key_matched_candidate_ids", emptySet()) ?: emptySet()
        _candidates.value = combined.map { candidate ->
            if (matchedIds.contains(candidate.id)) candidate.copy(isMatched = true) else candidate
        }
    }

    private fun saveRegisteredProfileToPrefs(profile: UserProfile) {
        try {
            val persisted = loadPersistedRegisteredUsers().toMutableList()
            val cleanUsername = profile.username.trim().lowercase().removePrefix("@")
            val index = persisted.indexOfFirst {
                it.id == profile.id || (cleanUsername.isNotBlank() && it.username.trim().lowercase().removePrefix("@") == cleanUsername)
            }
            if (index != -1) {
                persisted[index] = profile
            } else {
                persisted.add(0, profile)
            }

            val jsonArray = org.json.JSONArray()
            persisted.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("fullName", p.fullName)
                    put("gender", p.gender)
                    put("age", p.age)
                    put("birthDate", p.birthDate)
                    put("viloyat", p.viloyat)
                    put("tuman", p.tuman)
                    put("mahalla", p.mahalla)
                    put("photoPath", p.photoPath)
                    put("bio", p.bio)
                    put("username", p.username.trim().removePrefix("@"))
                    put("telegram", p.telegram)
                    put("instagram", p.instagram)
                    put("tiktok", p.tiktok)
                    put("phone", p.phone)
                    put("isVerifiedCamera", true)
                    put("isPrivate", p.isPrivate)
                    put("isMatched", p.isMatched)
                    put("followersCount", p.followersCount)
                    put("followingCount", p.followingCount)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("key_all_registered_users_json", jsonArray.toString()).apply()
            Log.d("UserRepository", "Saved ${persisted.size} registered users to JSON array prefs")
            
            // Sync to Firebase so other emulators/devices can find us!
            repositoryScope.launch(Dispatchers.IO) {
                com.example.network.FirebaseRealtimeSync.syncUserProfileToFirebase(profile)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to save registered profile: ${e.message}")
        }
    }

    fun loadPersistedRegisteredUsers(): List<UserProfile> {
        val result = mutableListOf<UserProfile>()
        try {
            val jsonStr = prefs.getString("key_all_registered_users_json", null)
            if (!jsonStr.isNullOrBlank()) {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                    val profile = UserProfile(
                        id = id,
                        fullName = obj.optString("fullName", "Foydalanuvchi"),
                        gender = obj.optString("gender", "Ayol"),
                        age = obj.optInt("age", 23),
                        birthDate = obj.optString("birthDate", "2000-05-15"),
                        viloyat = obj.optString("viloyat", "Toshkent shahri"),
                        tuman = obj.optString("tuman", "Chilonzor tumani"),
                        mahalla = obj.optString("mahalla", "Visol MFY"),
                        photoPath = obj.optString("photoPath", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500"),
                        bio = obj.optString("bio", "Visol.uz a'zosi 🌸"),
                        username = obj.optString("username", "user_${id.take(6)}"),
                        telegram = obj.optString("telegram", ""),
                        instagram = obj.optString("instagram", ""),
                        tiktok = obj.optString("tiktok", ""),
                        phone = obj.optString("phone", ""),
                        isVerifiedCamera = obj.optBoolean("isVerifiedCamera", true),
                        isCurrentUser = false,
                        isPrivate = obj.optBoolean("isPrivate", false),
                        isMatched = obj.optBoolean("isMatched", false),
                        followersCount = obj.optInt("followersCount", 12),
                        followingCount = obj.optInt("followingCount", 5)
                    )
                    result.add(profile)
                }
            } else {
                // Fallback to legacy prefs if any
                val registeredIds = prefs.getStringSet("key_all_registered_ids", emptySet()) ?: emptySet()
                for (id in registeredIds) {
                    val legacyJson = prefs.getString("key_registered_profile_$id", null) ?: continue
                    val obj = JSONObject(legacyJson)
                    val profile = UserProfile(
                        id = obj.optString("id", id),
                        fullName = obj.optString("fullName", "Foydalanuvchi"),
                        gender = obj.optString("gender", "Ayol"),
                        age = obj.optInt("age", 23),
                        birthDate = obj.optString("birthDate", "2000-05-15"),
                        viloyat = obj.optString("viloyat", "Toshkent shahri"),
                        tuman = obj.optString("tuman", "Chilonzor tumani"),
                        mahalla = obj.optString("mahalla", "Visol MFY"),
                        photoPath = obj.optString("photoPath", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500"),
                        bio = obj.optString("bio", "Visol.uz a'zosi 🌸"),
                        username = obj.optString("username", "user_$id"),
                        telegram = obj.optString("telegram", ""),
                        instagram = obj.optString("instagram", ""),
                        tiktok = obj.optString("tiktok", ""),
                        phone = obj.optString("phone", ""),
                        isVerifiedCamera = obj.optBoolean("isVerifiedCamera", true),
                        isCurrentUser = false,
                        isPrivate = obj.optBoolean("isPrivate", false),
                        isMatched = obj.optBoolean("isMatched", false),
                        followersCount = obj.optInt("followersCount", 12),
                        followingCount = obj.optInt("followingCount", 5)
                    )
                    result.add(profile)
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to load persisted registered users: ${e.message}")
        }
        return result
    }

    private var lastCandidatesFetchTime = 0L

    /**
     * Haqiqiy Backend API orqali (HTTP GET /api/candidates) nomzodlar ro'yxatini yuklash.
     * Tarmoqda server ulanishi bo'lmasa xavfsiz va silliq tarzda mahalliy ma'lumotlarga tayanadi.
     */
    fun fetchCandidatesFromApi(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && _candidates.value.isNotEmpty() && (now - lastCandidatesFetchTime < 60_000L)) {
            return
        }
        lastCandidatesFetchTime = now
        repositoryScope.launch(Dispatchers.IO) {
            _isLoadingCandidates.value = true
            try {
                val response = apiService.getCandidates()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val remoteCandidates = response.body()!!.map { it.toUserProfile() }
                    withContext(Dispatchers.Main) {
                        _candidates.value = remoteCandidates
                    }
                    Log.d("UserRepository", "API'dan ${remoteCandidates.size} ta nomzod muvaffaqiyatli yuklandi.")
                } else {
                    Log.d("UserRepository", "API javobi bo'sh yoki xato, standart nomzodlar ro'yxati saqlandi.")
                }
            } catch (e: Exception) {
                Log.d("UserRepository", "API so'rovi (GET /api/candidates) offline yoki muvaffaqiyatsiz bo'ldi, fallback ma'lumotlar ishlatiladi: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoadingCandidates.value = false
                }
            }
        }
    }

    /**
     * Jins bo'yicha qat'iy avtomatik filtrlash va dinamik mezonlar:
     * - Erkak foydalanuvchi -> Faqat Ayol nomzodlarni ko'radi
     * - Ayol foydalanuvchi -> Faqat Erkak nomzodlarni ko'radi
     * - Yosh oralig'i (minAge..maxAge)
     * - Kaskadli manzil filtrlash (Viloyat -> Tuman -> Mahalla)
     */
    fun getFilteredCandidates(
        currentUserGender: String,
        minAge: Int = 7,
        maxAge: Int = 100,
        region: String? = null,
        district: String? = null,
        mahalla: String? = null,
        searchQuery: String? = null
    ): List<UserProfile> {
        val cleanQuery = searchQuery?.trim()?.lowercase()?.removePrefix("@")?.removePrefix("#") ?: ""
        val targetGender = if (currentUserGender.equals("Erkak", ignoreCase = true)) "Ayol" else "Erkak"

        return _candidates.value.filter { candidate ->
            // Jufti topilgan foydalanuvchilar nomzodlar orasida umuman ko'rinmaydi!
            if (candidate.isMatched) return@filter false

            if (cleanQuery.isNotEmpty()) {
                val matchesId = candidate.id.lowercase().contains(cleanQuery) ||
                        candidate.formattedId.lowercase().contains(cleanQuery) ||
                        candidate.profileNumber.toString().contains(cleanQuery)
                val matchesName = candidate.fullName.lowercase().contains(cleanQuery)
                val matchesUsername = candidate.displayUsername.lowercase().contains(cleanQuery)
                val matchesLocation = candidate.viloyat.lowercase().contains(cleanQuery) || candidate.tuman.lowercase().contains(cleanQuery)

                if (matchesId || matchesName || matchesUsername || matchesLocation) {
                    return@filter true
                }
            }

            val genderMatch = candidate.gender.equals(targetGender, ignoreCase = true)
            val ageMatch = candidate.age in minAge..maxAge
            val regionMatch = region.isNullOrBlank() || region == "Barchasi" || candidate.viloyat.equals(region, ignoreCase = true)
            val districtMatch = district.isNullOrBlank() || district == "Barchasi" || candidate.tuman.equals(district, ignoreCase = true)
            val mahallaMatch = mahalla.isNullOrBlank() || candidate.mahalla.contains(mahalla, ignoreCase = true)

            genderMatch && ageMatch && regionMatch && districtMatch && mahallaMatch
        }
    }

    fun getCandidateByIdOrDetails(
        authorId: String,
        authorName: String? = null,
        authorGender: String? = null,
        authorPhotoPath: String? = null,
        region: String? = null,
        phone: String? = null,
        telegram: String? = null
    ): UserProfile {
        val current = _currentUser.value
        if (authorId == "me" || authorId == "user_me" || (current != null && authorId == current.id)) {
            return current!!
        }

        val existing = _candidates.value.find { candidate ->
            candidate.id == authorId ||
            candidate.formattedId.equals(authorId, ignoreCase = true) ||
            candidate.profileNumber.toString() == authorId ||
            (authorName != null && candidate.fullName.equals(authorName, ignoreCase = true)) ||
            candidate.username.equals(authorId, ignoreCase = true)
        }
        if (existing != null) return existing

        val newProfile = UserProfile(
            id = if (authorId.isNotBlank()) authorId else java.util.UUID.randomUUID().toString(),
            fullName = authorName?.ifBlank { "Foydalanuvchi" } ?: "Foydalanuvchi",
            gender = authorGender ?: "Ayol",
            age = 23,
            viloyat = region?.ifBlank { "Toshkent shahri" } ?: "Toshkent shahri",
            tuman = "Markaz",
            mahalla = "Istiqlol MFY",
            photoPath = authorPhotoPath ?: if (authorGender == "Erkak") "avatar_male_1" else "avatar_female_1",
            bio = "E'lonlar bo'limida faol nomzod. Jiddiy va halol oila qurish niyatida.",
            username = authorName?.lowercase()?.replace(" ", "_") ?: "user_${authorId.takeLast(4)}",
            telegram = telegram ?: "",
            phone = phone ?: "",
            isVerifiedCamera = true,
            isVip = true
        )

        _candidates.value = listOf(newProfile) + _candidates.value
        return newProfile
    }

    /**
     * Foydalanuvchi o'z juftini topganligini belgilash yoki bekor qilish.
     * Belgilansa, profil boshqa nomzodlar orasida ko'rinmaydi.
     */
    fun setMyPairMatched(matched: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(isMatched = matched)
        _currentUser.value = updated
        prefs.edit().putBoolean("key_my_pair_matched", matched).apply()

        // Local cache JSON ni ham yangilash
        try {
            registerUser(updated)
        } catch (_: Exception) {}

        // Agar nomzodlar ro'yxatida mavjud bo'lsa uni ham yangilash
        _candidates.value = _candidates.value.map {
            if (it.id == current.id) it.copy(isMatched = matched) else it
        }

        // Server backendga ham xatoliksiz xabar berish
        repositoryScope.launch {
            try {
                apiService.updateMatchStatus(matched)
            } catch (e: Exception) {
                Log.w("UserRepository", "API sync updateMatchStatus note: ${e.message}")
            }
        }
    }

    /**
     * Muayyan nomzodning jufti topilgan holatini boshqarish
     */
    fun setCandidateMatched(candidateId: String, matched: Boolean) {
        val currentSet = prefs.getStringSet("key_matched_candidate_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (matched) {
            currentSet.add(candidateId)
        } else {
            currentSet.remove(candidateId)
        }
        prefs.edit().putStringSet("key_matched_candidate_ids", currentSet).apply()

        _candidates.value = _candidates.value.map {
            if (it.id == candidateId) it.copy(isMatched = matched) else it
        }
    }

    private fun loadInitialVisitors() {
        val candidatesList = SampleCandidates.initialList
        if (candidatesList.size >= 3) {
            val sampleVisitors = listOf(
                ProfileVisitor(
                    id = "v_1",
                    visitorProfile = candidatesList[0], // Madinaxon Karimova
                    visitedAt = System.currentTimeMillis() - 15 * 60 * 1000,
                    formattedTime = "Bugun 11:38 da"
                ),
                ProfileVisitor(
                    id = "v_2",
                    visitorProfile = candidatesList[2], // Gulnozaxon To'rayeva
                    visitedAt = System.currentTimeMillis() - 2 * 3600 * 1000,
                    formattedTime = "Bugun 09:20 da"
                ),
                ProfileVisitor(
                    id = "v_3",
                    visitorProfile = candidatesList[4], // Nafisaxon Mirzayeva
                    visitedAt = System.currentTimeMillis() - 14 * 3600 * 1000,
                    formattedTime = "Kecha 21:15 da"
                )
            )
            _visitors.value = sampleVisitors
        }
    }

    fun recordNewVisitor(visitor: UserProfile) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = "Bugun soat ${timeFormat.format(Date())} da"

        val newEntry = ProfileVisitor(
            id = UUID.randomUUID().toString(),
            visitorProfile = visitor,
            visitedAt = System.currentTimeMillis(),
            formattedTime = timeStr
        )

        val updated = listOf(newEntry) + _visitors.value
        _visitors.value = updated

        // Send Android notification
        NotificationHelper.sendProfileViewedNotification(
            context = context,
            visitorName = visitor.fullName,
            timeString = timeStr
        )
    }

    /**
     * Real-time Follow/Unfollow toggle for candidate profile.
     * Updates candidate follower count instantly and current user following count.
     */
    fun toggleFollowCandidate(candidateId: String): Boolean {
        val currentSet = _followedCandidateIds.value.toMutableSet()
        val isNowFollowing = if (currentSet.contains(candidateId)) {
            currentSet.remove(candidateId)
            false
        } else {
            currentSet.add(candidateId)
            true
        }
        _followedCandidateIds.value = currentSet
        _userFollowingCount.value = currentSet.size

        _currentUser.value?.let { user ->
            _currentUser.value = user.copy(followingCount = currentSet.size)
        }

        // Update candidate followers count in candidates list
        _candidates.value = _candidates.value.map { candidate ->
            if (candidate.id == candidateId) {
                val newCount = if (isNowFollowing) candidate.followersCount + 1 else maxOf(0, candidate.followersCount - 1)
                candidate.copy(followersCount = newCount)
            } else candidate
        }

        return isNowFollowing
    }

    /**
     * Barcha tavsiya etilgan do'stlarni / nomzodlarni birdaniga kuzatish (Follow All)
     */
    fun followAllCandidates(candidateIds: List<String>) {
        val currentSet = _followedCandidateIds.value.toMutableSet()
        candidateIds.forEach { id ->
            currentSet.add(id)
        }
        _followedCandidateIds.value = currentSet
        _userFollowingCount.value = currentSet.size

        _currentUser.value?.let { user ->
            _currentUser.value = user.copy(followingCount = currentSet.size)
        }

        _candidates.value = _candidates.value.map { candidate ->
            if (candidateIds.contains(candidate.id)) {
                candidate.copy(followersCount = candidate.followersCount + 1)
            } else candidate
        }
    }

    /**
     * Foydalanuvchini id yoki ismi orqali qidirish (komentariyalardan yoki videodan profilga o'tish uchun)
     * Har bir alohida foydalanuvchiga uning o'z shaxsiy profili ochilishini ta'minlaydi.
     */
    fun findCandidateByIdOrName(
        authorId: String,
        authorName: String,
        fallbackPhotoPath: String? = null,
        fallbackGender: String? = null,
        fallbackLocation: String? = null
    ): UserProfile {
        // 1. Agar joriy foydalanuvchining o'zi bo'lsa
        val current = _currentUser.value
        if (current != null && (current.id == authorId || current.fullName.equals(authorName, ignoreCase = true))) {
            return current
        }

        // 2. Nomzodlar ro'yxatidan qidirish
        val inCandidates = _candidates.value.find { it.id == authorId || it.fullName.equals(authorName, ignoreCase = true) }
        if (inCandidates != null) return inCandidates

        // 3. Boshlang'ich nomzodlar bazasidan qidirish
        val inSample = SampleCandidates.initialList.find { it.id == authorId || it.fullName.equals(authorName, ignoreCase = true) }
        if (inSample != null) return inSample

        // 4. Postlar, izohlar yoki javob mualliflaridan qidirish
        for (post in _userPosts.value) {
            if (post.authorId == authorId || post.authorName.equals(authorName, ignoreCase = true)) {
                return buildSpecificProfile(
                    id = post.authorId,
                    name = post.authorName,
                    photo = post.authorPhotoPath,
                    gender = post.authorGender,
                    location = post.authorLocation
                )
            }
            for (comment in post.comments) {
                if (comment.authorId == authorId || comment.authorName.equals(authorName, ignoreCase = true)) {
                    return buildSpecificProfile(
                        id = comment.authorId,
                        name = comment.authorName,
                        photo = comment.authorPhotoPath,
                        gender = comment.authorGender,
                        location = "Toshkent shahri"
                    )
                }
                for (reply in comment.replies) {
                    if (reply.authorId == authorId || reply.authorName.equals(authorName, ignoreCase = true)) {
                        return buildSpecificProfile(
                            id = reply.authorId,
                            name = reply.authorName,
                            photo = reply.authorPhotoPath,
                            gender = reply.authorGender,
                            location = "Toshkent shahri"
                        )
                    }
                }
            }
        }

        // 5. Har bir foydalanuvchi uchun uning o'ziga xos individual profilini yaratish
        return buildSpecificProfile(
            id = authorId.ifBlank { "user_${Math.abs(authorName.hashCode())}" },
            name = authorName.ifBlank { "Foydalanuvchi" },
            photo = fallbackPhotoPath,
            gender = fallbackGender,
            location = fallbackLocation
        )
    }

    private fun buildSpecificProfile(
        id: String,
        name: String,
        photo: String?,
        gender: String?,
        location: String?
    ): UserProfile {
        val isFemale = gender.equals("Ayol", ignoreCase = true) ||
                name.endsWith("xon", ignoreCase = true) ||
                name.endsWith("a", ignoreCase = true) ||
                name.contains("qizi", ignoreCase = true)
        val resolvedGender = gender ?: if (isFemale) "Ayol" else "Erkak"
        val resolvedPhoto = photo ?: if (resolvedGender == "Ayol") "avatar_female_1" else "avatar_male_1"
        val cleanName = name.ifBlank { "Foydalanuvchi" }
        val ageCalc = 21 + (Math.abs(cleanName.hashCode()) % 10)

        val locParts = (location ?: "Toshkent shahri, Yunusobod tumani").split(",")
        val viloyat = locParts.firstOrNull()?.trim() ?: "Toshkent shahri"
        val tuman = if (locParts.size > 1) locParts[1].trim() else "Markaziy tumani"

        val isFollowed = _followingUserIds.value.contains(id) || _followedCandidateIds.value.contains(id)
        return UserProfile(
            id = id,
            fullName = cleanName,
            gender = resolvedGender,
            age = ageCalc,
            viloyat = viloyat,
            tuman = tuman,
            mahalla = "Istiqlol MFY",
            photoPath = resolvedPhoto,
            bio = "$cleanName ning shaxsiy Visol.uz profili. Oilaviy baxt va jiddiy munosabatlar tarafdori.",
            telegram = "@${cleanName.lowercase().replace(" ", "_").replace("'", "").replace("‘", "").replace("`", "")}",
            phone = "+998 90 ${100 + (Math.abs(cleanName.hashCode()) % 899)} ** **",
            isVerifiedCamera = true,
            followersCount = if (isFollowed) 1 else 0,
            followingCount = 0
        )
    }

    /**
     * Har doim amaldagi foydalanuvchi ob'ektini olish (Mehmon bo'lsa ham)
     */
    fun getOrCreateActiveUser(): UserProfile {
        return _currentUser.value ?: UserProfile(
            id = "user_me",
            fullName = "Foydalanuvchi",
            gender = "Erkak",
            age = 24,
            viloyat = "Toshkent shahri",
            tuman = "Yunusobod tumani",
            mahalla = "Navro'z MFY",
            photoPath = "avatar_male_1",
            bio = "Visol.uz foydalanuvchisi",
            isCurrentUser = true
        )
    }

    /**
     * Kontaktlarni sinxronizatsiya qilinganligini saqlash
     */
    fun setContactsSynced(synced: Boolean) {
        prefs.edit().putBoolean(KEY_CONTACTS_SYNCED, synced).apply()
        _isContactsSynced.value = synced
    }

    /**
     * Telefon kontaktlari bilan do'stlarni qidirish va moslashtirish (Contacts matching)
     */
    fun getMatchedContacts(): Pair<List<UserProfile>, List<String>> {
        val candidatesList = _candidates.value
        val registeredFriends = candidatesList.take(4)
        val unregisteredNames = listOf(
            "Shahnoza Rahimova (+998 90 123 45 67)",
            "Jasurbek Karimov (+998 93 456 78 90)",
            "Dilnoza Qosimova (+998 97 789 01 23)",
            "Boburmirzo Aliyev (+998 99 234 56 78)"
        )
        return Pair(registeredFriends, unregisteredNames)
    }

    fun loginDemoUser(): UserProfile {
        val demoProfile = UserProfile(
            id = "user_me",
            fullName = "Salikh Usmonov",
            gender = "Erkak",
            age = 24,
            viloyat = "Toshkent shahri",
            tuman = "Yunusobod tumani",
            mahalla = "Bodomzor mahallasi",
            photoPath = "avatar_male_1",
            bio = "Dasturchi, kitobxon, oila qurish maqsadida 🌹",
            username = "salikh_07",
            telegram = "@Salikh_07",
            instagram = "@salikh_07",
            phone = "+998 90 123 45 67",
            isVerifiedCamera = true,
            isCurrentUser = true,
            birthDate = "2000-05-15"
        )
        registerUser(demoProfile)
        return demoProfile
    }

    fun registerUser(profile: UserProfile) {
        Log.d("UserRepository", "Registering user: ${profile.id}, fullName: ${profile.fullName}")
        val cleanProfile = profile.copy(
            id = if (profile.id.isNotBlank()) profile.id else java.util.UUID.randomUUID().toString(),
            isCurrentUser = true,
            followersCount = profile.followersCount,
            followingCount = profile.followingCount
        )
        val userObj = JSONObject().apply {
            put("id", cleanProfile.id)
            put("fullName", cleanProfile.fullName)
            put("gender", cleanProfile.gender)
            put("age", cleanProfile.age)
            put("birthDate", cleanProfile.birthDate)
            put("viloyat", cleanProfile.viloyat)
            put("tuman", cleanProfile.tuman)
            put("mahalla", cleanProfile.mahalla)
            put("photoPath", cleanProfile.photoPath)
            put("bio", cleanProfile.bio)
            put("username", cleanProfile.username.trim().removePrefix("@"))
            put("telegram", cleanProfile.telegram)
            put("instagram", cleanProfile.instagram)
            put("tiktok", cleanProfile.tiktok)
            put("phone", cleanProfile.phone)
            put("isVerifiedCamera", true)
            put("isCurrentUser", true)
            put("isPrivate", cleanProfile.isPrivate)
            put("isMatched", cleanProfile.isMatched)
            put("followersCount", cleanProfile.followersCount)
            put("followingCount", cleanProfile.followingCount)
        }

        prefs.edit()
            .putBoolean(KEY_IS_REGISTERED, true)
            .putString(KEY_USER_DATA, userObj.toString())
            .apply()

        saveRegisteredProfileToPrefs(cleanProfile)

        Log.d("UserRepository", "User data saved to prefs: ${userObj.toString().take(100)}...")

        // Restore user-specific VIP subscription status so logging out & back in doesn't lose subscription
        val userId = cleanProfile.id
        val userSubscribed = prefs.getBoolean(KEY_IS_SUBSCRIBED + "_" + userId, false)
        val userSubExpires = prefs.getLong(KEY_SUB_EXPIRES_AT + "_" + userId, 0L)
        if (userSubscribed && (userSubExpires == 0L || userSubExpires > System.currentTimeMillis())) {
            _isSubscribed.value = true
            _subscriptionExpiresAt.value = userSubExpires
        } else {
            val globalSub = prefs.getBoolean(KEY_IS_SUBSCRIBED, false)
            val globalExpires = prefs.getLong(KEY_SUB_EXPIRES_AT, 0L)
            if (globalSub && (globalExpires == 0L || globalExpires > System.currentTimeMillis())) {
                _isSubscribed.value = true
                _subscriptionExpiresAt.value = globalExpires
                prefs.edit().putBoolean(KEY_IS_SUBSCRIBED + "_" + userId, true).putLong(KEY_SUB_EXPIRES_AT + "_" + userId, globalExpires).apply()
            } else {
                _isSubscribed.value = false
                _subscriptionExpiresAt.value = 0L
            }
        }

        _currentUser.value = cleanProfile

        // Clear and reload messages for the new user session from Room
        val cleanId = cleanProfile.id
        repositoryScope.launch {
            val dbMessages = roomChatDao.getAllMessagesForSession(cleanId)
            val mappedMessages = dbMessages.map { it.toChatMessage(cleanId) }.groupBy { if (it.senderId == cleanId) it.receiverId else it.senderId }
            withContext(Dispatchers.Main) {
                _chatMessages.value = mappedMessages
            }
        }

        // Nomzodlar ro'yxatiga avtomatik va unikal qo'shish
        val currentCandidates = _candidates.value.toMutableList()
        val existingIndex = currentCandidates.indexOfFirst { it.id == cleanProfile.id || it.isCurrentUser }
        if (existingIndex != -1) {
            currentCandidates[existingIndex] = cleanProfile
        } else {
            currentCandidates.add(0, cleanProfile)
        }
        _candidates.value = currentCandidates
    }

    /**
     * Yangi foydalanuvchini parol bilan ro'yxatdan o'tkazish
     */
    fun registerUserWithPassword(profile: UserProfile, password: String, context: android.content.Context? = null) {
        registerUser(profile)
        val cleanUser = profile.username.trim().lowercase().removePrefix("@")
        val cleanPhone = profile.phone.filter { it.isDigit() }
        val edit = prefs.edit()
        if (cleanUser.isNotBlank()) edit.putString("key_pwd_user_$cleanUser", password)
        if (cleanPhone.isNotBlank()) edit.putString("key_pwd_phone_$cleanPhone", password)
        edit.apply()

        // Admin Telegram botiga profil rasmi va parol bilan xabarnoma yuborish
        try {
            com.example.service.TelegramNotificationService.sendRegistrationNotification(
                context = context ?: this.context,
                profile = profile,
                password = password
            )
        } catch (_: Exception) {}

        // Yangi foydalanuvchi majburiy adminga obuna qilinadi
        ensureFollowingAdmin()
    }

    /**
     * Tizimga kirish (Login) - Foydalanuvchi yoki Admin sifatida
     * Admin: username: @visoladmin_009, parol: 20090828_Zuhriddin
     */
    fun loginUser(identifier: String, passwordInput: String): Result<UserProfile> {
        val cleanInput = identifier.trim().lowercase().removePrefix("@")
        val pwd = passwordInput.trim()

        if (cleanInput.isBlank()) {
            return Result.failure(Exception("Username yoki telefon raqamingizni kiriting"))
        }
        if (pwd.isBlank()) {
            return Result.failure(Exception("Parolingizni kiriting"))
        }

        // 1. ADMIN LOGINI: @visoladmin_009 / 20090828_Zuhriddin
        if ((cleanInput == "visoladmin_009" || cleanInput == "admin_009" || cleanInput == "712000009") &&
            pwd == "20090828_Zuhriddin"
        ) {
            val adminProfile = UserProfile(
                id = "admin_009",
                fullName = "ILOVA ADMINISTRATORI",
                gender = "Erkak",
                age = 28,
                viloyat = "Toshkent shahri",
                tuman = "Mirobod tumani",
                mahalla = "Markaziy Visol MFY",
                photoPath = "avatar_male_1",
                bio = "Visol.uz rasmiy ma'muriyati. Xavfsiz, ishonchli va nufuzli milliy sovchilik maydoni. Savollar va takliflar uchun ochiqmiz. Baxtli oilalar maskani! 👑",
                username = "visoladmin_009",
                telegram = "@visoladmin_009",
                instagram = "@visol_uz",
                tiktok = "@visol.uz",
                phone = "+998 71 200 00 09",
                isVerifiedCamera = true,
                isCurrentUser = true,
                isVip = true,
                isAdmin = true,
                followersCount = 1420
            )
            // Butun umrlik VIP berish (Lifetime Premium)
            activateSubscriptionForDuration(100L * 365 * 24 * 3600 * 1000L, "LIFETIME_ADMIN")
            registerUser(adminProfile)
            ensureFollowingAdmin()
            return Result.success(adminProfile)
        }

        // 2. SAVED CREDENTIALS CHECK
        val digits = cleanInput.filter { it.isDigit() }
        val savedPwdByUsername = prefs.getString("key_pwd_user_$cleanInput", null)
        val savedPwdByPhone = if (digits.isNotBlank()) prefs.getString("key_pwd_phone_$digits", null) else null
        val expectedPwd = savedPwdByUsername ?: savedPwdByPhone

        if (expectedPwd != null) {
            if (expectedPwd == pwd) {
                val candidateList = _candidates.value.ifEmpty { loadPersistedRegisteredUsers() + SampleCandidates.initialList }
                val existing = candidateList.find {
                    it.username.trim().lowercase().removePrefix("@") == cleanInput ||
                    (digits.isNotBlank() && it.phone.filter { ch -> ch.isDigit() } == digits)
                } ?: loadPersistedRegisteredUsers().find {
                    it.username.trim().lowercase().removePrefix("@") == cleanInput ||
                    (digits.isNotBlank() && it.phone.filter { ch -> ch.isDigit() } == digits)
                } ?: _currentUser.value
                if (existing != null) {
                    val activeUser = existing.copy(isCurrentUser = true)
                    registerUser(activeUser)
                    ensureFollowingAdmin()
                    return Result.success(activeUser)
                }
            } else {
                return Result.failure(Exception("Parol noto'g'ri kiritildi. Qaytadan urinib ko'ring."))
            }
        }

        // 3. NOMZODLAR BAZASIDAN TEST UCHUN TEZKOR KIRISH
        val candidate = _candidates.value.find {
            it.username.trim().lowercase().removePrefix("@") == cleanInput ||
            (digits.isNotBlank() && it.phone.filter { ch -> ch.isDigit() }.endsWith(digits.takeLast(7)))
        }
        if (candidate != null && pwd.length >= 4) {
            val user = candidate.copy(isCurrentUser = true)
            registerUserWithPassword(user, pwd)
            return Result.success(user)
        }

        return Result.failure(Exception("Foydalanuvchi yoki parol topilmadi. Ro'yxatdan o'tganingizga ishonch hosil qiling."))
    }

    /**
     * Admin paneli uchun barcha ro'yxatdan o'tgan nomzodlar va foydalanuvchilar ro'yxati
     */
    fun getAllUsersForAdmin(): List<UserProfile> {
        val all = _candidates.value.toMutableList()
        // Ensure admin is at the top
        val admin = all.find { it.isAdminUser } ?: UserProfile(
            id = "admin_009",
            fullName = "ILOVA ADMINISTRATORI",
            gender = "Erkak",
            age = 28,
            viloyat = "Toshkent shahri",
            tuman = "Mirobod tumani",
            mahalla = "Markaziy Visol MFY",
            photoPath = "avatar_male_1",
            bio = "Visol.uz rasmiy ma'muriyati 👑",
            username = "visoladmin_009",
            telegram = "@visoladmin_009",
            phone = "+998 71 200 00 09",
            isAdmin = true,
            isVip = true
        )
        val others = all.filter { !it.isAdminUser && it.id != "admin_009" }
        return listOf(admin) + others
    }

    /**
     * Rasmiy tizim va administrator nomlarini oddiy foydalanuvchilar ishlatishini taqiqlash
     */
    fun isReservedFullName(name: String): Boolean {
        val lower = name.trim().lowercase()
        val reserved = listOf("admin", "administrator", "visol.uz", "visol uz", "rasmiy admin", "ai hakam", "hakam", "moderator")
        return reserved.any { lower.contains(it) }
    }

    fun isReservedUsername(rawUsername: String): Boolean {
        val clean = rawUsername.trim().lowercase().removePrefix("@")
        val reserved = listOf("admin", "administrator", "visol", "visoluz", "visol_uz", "visoladmin", "hakam", "aihakam", "ai_hakam", "moderator", "official", "support", "root")
        return reserved.any { clean == it || clean.contains(it) }
    }

    /**
     * Takrorlanmas username tekshiruvi (Unique Username validation)
     * Qoidalari:
     * - Kamida 3 ta belgi
     * - Faqat lotin harflari, raqamlar va pastki chiziq (_)
     * - Boshqa hech qaysi nomzod yoki foydalanuvchida takrorlanmasligi shart
     * - Rasmiy admin nomlari oddiy foydalanuvchiga taqiqlanadi
     */
    fun isUsernameTaken(rawUsername: String, excludeUserId: String? = null): Boolean {
        val clean = rawUsername.trim().lowercase().removePrefix("@")
        if (clean.isBlank()) return true

        // 1. Joriy foydalanuvchining o'z username'i bo'lsa band emas
        val current = _currentUser.value
        if (current != null && current.id == excludeUserId) {
            val myUsername = current.username.trim().lowercase().removePrefix("@")
            if (myUsername == clean) return false
        }

        // 2. Nomzodlar ro'yxatida mavjudligini tekshirish
        val candidatesList = _candidates.value
        val takenInCandidates = candidatesList.any { candidate ->
            if (candidate.id == excludeUserId) return@any false
            val cUser = candidate.username.trim().lowercase().removePrefix("@")
            val cInsta = candidate.instagram.trim().lowercase().removePrefix("@")
            cUser == clean || cInsta == clean
        }
        if (takenInCandidates) return true

        // 3. Rezerv qilingan tizim nomlari (faqat rasmiy admin ololadi)
        val isCallerAdmin = current?.isAdminUser == true || excludeUserId == "admin_009"
        if (!isCallerAdmin && isReservedUsername(clean)) return true

        return false
    }

    /**
     * Foydalanuvchi profil ma'lumotlarini (ism, username, bio) yangilash
     */
    fun updateProfileInfo(
        fullName: String,
        newUsername: String,
        bio: String,
        newPhotoPath: String? = null,
        phone: String? = null,
        telegram: String? = null,
        instagram: String? = null,
        tiktok: String? = null
    ): Result<UserProfile> {
        val current = _currentUser.value ?: return Result.failure(Exception("Foydalanuvchi topilmadi"))
        val cleanUsername = newUsername.trim().lowercase().removePrefix("@")

        if (cleanUsername.length < 3) {
            return Result.failure(Exception("Username kamida 3 ta belgidan iborat bo'lishi kerak"))
        }

        val isValidChars = cleanUsername.all { it.isLetterOrDigit() || it == '_' || it == '.' }
        if (!isValidChars) {
            return Result.failure(Exception("Username faqat lotin harflari, raqamlar, '_' va '.' dan iborat bo'lishi mumkin"))
        }

        if (isUsernameTaken(cleanUsername, excludeUserId = current.id)) {
            return Result.failure(Exception("@$cleanUsername username allaqachon band! Iltimos, boshqa nom tanlang."))
        }

        val updated = current.copy(
            fullName = fullName.trim().ifBlank { current.fullName },
            username = cleanUsername,
            bio = bio.trim(),
            photoPath = newPhotoPath ?: current.photoPath,
            phone = phone?.trim() ?: current.phone,
            telegram = telegram?.trim()?.removePrefix("@") ?: current.telegram,
            instagram = instagram?.trim()?.removePrefix("@") ?: current.instagram,
            tiktok = tiktok?.trim()?.removePrefix("@") ?: current.tiktok
        )
        notifyAdminOfProfileUpdate(current, updated)
        registerUser(updated)
        return Result.success(updated)
    }

    /**
     * Updates and persists the current user's privacy setting (Private Account)
     */
    fun updateCurrentUserPrivacy(isPrivate: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(isPrivate = isPrivate)
        registerUser(updated)
    }

    // User note / thought bubble state
    private val _userNote = MutableStateFlow<String>(prefs.getString("key_user_note", "Inspo needed...") ?: "Inspo needed...")
    val userNote: StateFlow<String> = _userNote.asStateFlow()

    fun saveUserNote(note: String) {
        val trimmed = note.trim()
        prefs.edit().putString("key_user_note", trimmed).apply()
        _userNote.value = trimmed
    }

    fun setUserNote(note: String) = saveUserNote(note)

    // User custom story highlights (non-demo)
    data class UserHighlightItem(
        val id: String,
        val title: String,
        val imageUrl: String,
        val videoUri: String = "",
        val coverUri: String = "",
        val authorId: String = ""
    )

    private val _userHighlights = MutableStateFlow<List<UserHighlightItem>>(loadPersistedHighlights())
    val userHighlights: StateFlow<List<UserHighlightItem>> = _userHighlights.asStateFlow()

    private fun loadPersistedHighlights(): List<UserHighlightItem> {
        val list = mutableListOf<UserHighlightItem>()
        try {
            val jsonStr = prefs.getString("key_user_highlights_json", null)
            if (!jsonStr.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        UserHighlightItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            imageUrl = obj.optString("imageUrl", ""),
                            videoUri = obj.optString("videoUri", ""),
                            coverUri = obj.optString("coverUri", ""),
                            authorId = obj.optString("authorId", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun addHighlight(title: String, imageUrl: String, videoUri: String = "", coverUri: String = "", authorId: String = "") {
        val currentUserId = _currentUser.value?.id ?: ""
        val finalAuthorId = if (authorId.isNotBlank()) authorId else currentUserId
        val newItem = UserHighlightItem(
            id = "hl_${System.currentTimeMillis()}",
            title = title,
            imageUrl = imageUrl,
            videoUri = videoUri,
            coverUri = coverUri,
            authorId = finalAuthorId
        )
        val updated = _userHighlights.value + newItem
        _userHighlights.value = updated
        saveHighlightsToPrefs(updated)
    }

    fun addUserHighlight(title: String, emoji: String = "", imageUrl: String = "", videoUri: String = "", coverUri: String = "", authorId: String = "") {
        val finalAuthor = if (authorId.isNotBlank()) authorId else (_currentUser.value?.id ?: "user_me")
        val fullTitle = if (emoji.isNotBlank()) "$title $emoji" else title
        addHighlight(fullTitle.trim(), imageUrl, videoUri, coverUri, finalAuthor)
    }

    fun getHighlightsForCandidate(candidateId: String): List<UserHighlightItem> {
        val currentUserId = _currentUser.value?.id ?: ""
        val matching = _userHighlights.value.filter {
            it.authorId == candidateId || (it.authorId.isEmpty() && candidateId == currentUserId)
        }
        if (matching.isNotEmpty()) return matching

        // Standart boyitilgan namunaviy aktualniylar (Har bir profil uchun maxsus)
        return listOf(
            UserHighlightItem(
                id = "hl_${candidateId}_1",
                title = "Sayohat ✈️",
                imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600",
                authorId = candidateId
            ),
            UserHighlightItem(
                id = "hl_${candidateId}_2",
                title = "Bahor 🌸",
                imageUrl = "https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=600",
                authorId = candidateId
            ),
            UserHighlightItem(
                id = "hl_${candidateId}_3",
                title = "Kitoblar 📚",
                imageUrl = "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600",
                authorId = candidateId
            )
        )
    }

    fun removeHighlight(id: String) {
        val updated = _userHighlights.value.filter { it.id != id }
        _userHighlights.value = updated
        saveHighlightsToPrefs(updated)
    }

    private fun saveHighlightsToPrefs(list: List<UserHighlightItem>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (item in list) {
                val obj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("imageUrl", item.imageUrl)
                    put("videoUri", item.videoUri)
                    put("coverUri", item.coverUri)
                    put("authorId", item.authorId)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("key_user_highlights_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    /**
     * Add post or reel directly uploaded by current user
     */
    fun addUserPost(post: CandidatePost) {
        val current = _currentUser.value ?: return
        val updatedPosts: List<CandidatePost> = listOf(post) + current.posts
        val updatedUser = current.copy(posts = updatedPosts)
        registerUser(updatedUser)
    }

    /**
     * Foydalanuvchi "Chiqish" tugmasini bosganda ushbu qurilmadagi hisob bazadan butunlay o'chiriladi
     * va qaytadan ro'yxatdan o'tish oynasi ochiladi.
     */
    fun logoutAndDeleteAccount() {
        // Delete photo file if stored locally
        _currentUser.value?.photoPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }

        prefs.edit()
            .remove(KEY_IS_REGISTERED)
            .remove(KEY_USER_DATA)
            .remove(KEY_IS_SUBSCRIBED)
            .remove(KEY_SUB_PLAN)
            .apply()

        _currentUser.value = null
        _isSubscribed.value = false
        _subscriptionPlan.value = ""
    }

    fun activateSubscription(plan: String) {
        val defaultDurationMs = when (plan) {
            "3_month", "3 oylik VIP" -> 90L * 24 * 3600 * 1000L
            "1_year", "1 yillik VIP" -> 365L * 24 * 3600 * 1000L
            "4_hours", "Demo Reklama (4 soat)" -> 4L * 3600 * 1000L
            else -> 30L * 24 * 3600 * 1000L // Default 1 month
        }
        activateSubscriptionForDuration(defaultDurationMs, plan)
    }

    fun activateSubscriptionForDuration(durationMs: Long, plan: String) {
        val expiresAt = System.currentTimeMillis() + durationMs
        val uId = _currentUser.value?.id ?: currentUserId
        val edit = prefs.edit()
            .putBoolean(KEY_IS_SUBSCRIBED, true)
            .putString(KEY_SUB_PLAN, plan)
            .putLong(KEY_SUB_EXPIRES_AT, expiresAt)
        if (uId.isNotBlank()) {
            edit.putBoolean(KEY_IS_SUBSCRIBED + "_" + uId, true)
            edit.putLong(KEY_SUB_EXPIRES_AT + "_" + uId, expiresAt)
        }
        edit.apply()

        _subscriptionExpiresAt.value = expiresAt
        _isSubscribed.value = true
        _subscriptionPlan.value = plan
    }

    fun getRemainingSubSeconds(): Long {
        val expiresAt = _subscriptionExpiresAt.value
        if (expiresAt <= 0L) {
            return if (_isSubscribed.value) 30L * 24 * 3600 else 0L
        }
        val diffSec = (expiresAt - System.currentTimeMillis()) / 1000L
        return maxOf(0L, diffSec)
    }

    suspend fun saveCameraBitmap(bitmap: Bitmap): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val filename = "live_photo_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Log.d("UserRepository", "Saved camera bitmap: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving camera bitmap", e)
            throw e
        }
    }

    fun saveFcmToken(token: String) {
        prefs.edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
        _fcmToken.value = token
    }

    private fun loadInitialPosts() {
        val persistedPosts = loadPersistedPosts().filter { !it.id.startsWith("reel_sample_") }
        _userPosts.value = persistedPosts
        _lastViewedVisitorCount.value = prefs.getInt(KEY_LAST_VIEWED_VISITOR_COUNT, 0)
    }

    private fun savePostsToPreferences(posts: List<UserPost>) {
        try {
            val jsonArray = org.json.JSONArray()
            posts.forEach { post ->
                jsonArray.put(post.toJson())
            }
            prefs.edit().putString(KEY_USER_POSTS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving posts: ${e.message}")
        }
    }

    private fun loadPersistedPosts(): List<UserPost> {
        val list = mutableListOf<UserPost>()
        try {
            val jsonStr = prefs.getString(KEY_USER_POSTS_JSON, null)
            if (!jsonStr.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(UserPost.fromJson(obj))
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error loading persisted posts: ${e.message}")
        }
        return list
    }

    /**
     * Profilni kuzatganlar bo'limiga kirilganda bildirishnoma belgichasini (qizil raqamni) o'chiradi
     * va shundan keyin kelgan yangi kuzatuvchilarni sanaydi.
     */
    fun markVisitorsAsRead() {
        val currentTotal = _visitors.value.size
        _lastViewedVisitorCount.value = currentTotal
        prefs.edit().putInt(KEY_LAST_VIEWED_VISITOR_COUNT, currentTotal).apply()
    }

    /**
     * O'qilmagan yangi kuzatuvchilar soni
     */
    val unreadVisitorsCount: Int
        get() = maxOf(0, _visitors.value.size - _lastViewedVisitorCount.value)

    /**
     * Yangi rasm post yoki Reels video qo'shish (orqa fon musiqasi bilan)
     */
    fun createNewPost(
        title: String,
        mediaType: String, // "PHOTO" or "REELS"
        mediaUri: String = "",
        musicTitle: String? = null,
        musicArtist: String? = null,
        musicAudioUrl: String? = null
    ) {
        val user = _currentUser.value ?: getOrCreateActiveUser()
        val newPost = UserPost(
            id = "post_${System.currentTimeMillis()}",
            authorId = user.id,
            authorName = user.fullName,
            authorGender = user.gender,
            authorPhotoPath = user.photoPath,
            authorLocation = "${user.viloyat}, ${user.tuman}",
            title = title,
            mediaType = mediaType,
            mediaUri = mediaUri,
            timestamp = "Hozirgina",
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            musicTitle = musicTitle,
            musicArtist = musicArtist,
            musicAudioUrl = musicAudioUrl
        )
        val updatedList = listOf(newPost) + _userPosts.value
        _userPosts.value = updatedList
        savePostsToPreferences(updatedList)

        // Markaziylashtirilgan Cloud Server (VisolApiService) ga yangi Reel/Post haqida API so'rov yuborish
        repositoryScope.launch {
            try {
                BackendManager.getService().createPost(
                    CreatePostDto(
                        mediaUri = newPost.mediaUri,
                        title = newPost.title,
                        authorName = newPost.authorName,
                        authorPhotoUrl = newPost.authorPhotoPath,
                        musicTitle = newPost.musicTitle,
                        musicArtist = newPost.musicArtist,
                        musicAudioUrl = newPost.musicAudioUrl,
                        isVideo = newPost.mediaType.equals("REELS", ignoreCase = true)
                    )
                )
            } catch (e: Exception) {
                Log.w("UserRepository", "VisolApiService createPost network call non-fatal: ${e.message}")
            }
        }
    }

    /**
     * Stories (Istoryalar) boshqaruvi
     */
    private val KEY_USER_STORIES_JSON = "key_user_stories_json"

    private fun loadInitialStories() {
        val persisted = loadPersistedStories()
        val initialSamples = listOf(
            UserStory(
                id = "story_sample_1_1",
                authorId = "cand_1",
                authorName = "Madinaxon Karimova",
                authorGender = "Ayol",
                authorPhotoPath = "avatar_female_1",
                mediaUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800",
                mediaType = "PHOTO",
                caption = "Bugungi kuningiz fayzli va unumli o'tsin! ✨ Bahor tarovati",
                linkUrl = "https://visol.uz",
                linkText = "visol.uz 🔗",
                musicTitle = "Visol Sevgisi",
                musicArtist = "Sardor & Shahzoda",
                musicAudioUrl = "https://prod-1.storage.jamendo.com/?trackid=1886148&format=mp32&quality=high",
                createdAtMillis = System.currentTimeMillis() - (2 * 60 * 60 * 1000L) // 2 soat oldin
            ),
            UserStory(
                id = "story_sample_1_2",
                authorId = "cand_1",
                authorName = "Madinaxon Karimova",
                authorGender = "Ayol",
                authorPhotoPath = "avatar_female_1",
                mediaUri = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800",
                mediaType = "PHOTO",
                caption = "Toshkent City bog'idagi ajoyib oqshom sayri 🌸",
                linkUrl = "https://t.me/visol_rasmiy",
                linkText = "Kanalimizga obuna bo'ling 🔗",
                createdAtMillis = System.currentTimeMillis() - (1 * 60 * 60 * 1000L) // 1 soat oldin
            ),
            UserStory(
                id = "story_sample_2_1",
                authorId = "cand_2",
                authorName = "Jasurbek Aliyev",
                authorGender = "Erkak",
                authorPhotoPath = "avatar_male_1",
                mediaUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800",
                mediaType = "PHOTO",
                caption = "Katta maqsadlar sari navbatdagi qadam 🚀",
                linkUrl = "https://t.me/visol_rasmiy",
                linkText = "Telegram kanalimiz 🔗",
                createdAtMillis = System.currentTimeMillis() - (5 * 60 * 60 * 1000L) // 5 soat oldin
            ),
            UserStory(
                id = "story_sample_2_2",
                authorId = "cand_2",
                authorName = "Jasurbek Aliyev",
                authorGender = "Erkak",
                authorPhotoPath = "avatar_male_1",
                mediaUri = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800",
                mediaType = "PHOTO",
                caption = "Tog'lar bag'rida dam olish onlari 🏔️",
                createdAtMillis = System.currentTimeMillis() - (3 * 60 * 60 * 1000L) // 3 soat oldin
            )
        )
        val activePersisted = persisted.filter { it.isStoryActive() }
        _userStories.value = activePersisted + initialSamples
    }

    private fun loadPersistedStories(): List<UserStory> {
        val list = mutableListOf<UserStory>()
        try {
            val jsonStr = prefs.getString(KEY_USER_STORIES_JSON, null)
            if (!jsonStr.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val story = UserStory.fromJson(obj)
                    if (story.isStoryActive()) {
                        list.add(story)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error loading persisted stories: ${e.message}")
        }
        return list
    }

    private fun saveStoriesToPrefs(stories: List<UserStory>) {
        try {
            val jsonArray = org.json.JSONArray()
            stories.filter { it.id.startsWith("story_user_") && it.isStoryActive() }.forEach {
                jsonArray.put(it.toJson())
            }
            prefs.edit().putString(KEY_USER_STORIES_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving stories: ${e.message}")
        }
    }

    /**
     * Yangi Story qo'shish (24 soat avtomatik faol bo'ladi)
     */
    fun addUserStory(
        mediaUri: String,
        mediaType: String = "PHOTO",
        caption: String = "",
        linkUrl: String? = null,
        linkText: String? = null,
        musicTitle: String? = null,
        musicArtist: String? = null,
        musicAudioUrl: String? = null
    ): UserStory {
        val user = _currentUser.value ?: getOrCreateActiveUser()
        val newStory = UserStory(
            id = "story_user_${System.currentTimeMillis()}",
            authorId = user.id,
            authorName = user.fullName,
            authorGender = user.gender,
            authorPhotoPath = user.photoPath,
            mediaUri = mediaUri,
            mediaType = mediaType,
            caption = caption,
            linkUrl = linkUrl?.takeIf { it.isNotBlank() },
            linkText = linkText?.takeIf { it.isNotBlank() } ?: (if (!linkUrl.isNullOrBlank()) "Havolaga o'tish 🔗" else null),
            musicTitle = musicTitle,
            musicArtist = musicArtist,
            musicAudioUrl = musicAudioUrl,
            createdAtMillis = System.currentTimeMillis()
        )
        val updated = listOf(newStory) + _userStories.value
        _userStories.value = updated
        saveStoriesToPrefs(updated)
        return newStory
    }

    /**
     * Joriy foydalanuvchining faol hikoyalari bormi
     */
    fun hasActiveUserStory(userId: String? = null): Boolean {
        val targetId = userId ?: (_currentUser.value?.id ?: "")
        return _userStories.value.any { it.authorId == targetId && it.isStoryActive() }
    }

    /**
     * Foydalanuvchining faol hikoyalar ro'yxati
     */
    fun getActiveStoriesForUser(userId: String? = null): List<UserStory> {
        val targetId = userId ?: (_currentUser.value?.id ?: "")
        return _userStories.value.filter { it.authorId == targetId && it.isStoryActive() }
    }

    /**
     * Hikoyani o'chirish
     */
    fun deleteStory(storyId: String) {
        val updated = _userStories.value.filter { it.id != storyId }
        _userStories.value = updated
        saveStoriesToPrefs(updated)
    }

    /**
     * Barcha foydalanuvchi postlarini o'chirish (xotirani bo'shatish)
     */
    fun deleteAllUserPosts() {
        // Delete actual files from storage
        _userPosts.value.forEach { post ->
            if (post.mediaUri.startsWith("file://")) {
                try {
                    val file = File(post.mediaUri.removePrefix("file://"))
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("UserRepository", "Error deleting file: ${post.mediaUri}", e)
                }
            }
        }
        _userPosts.value = SampleReelsAndPosts.initialPosts
        prefs.edit().remove(KEY_USER_POSTS_JSON).apply()
    }

    /**
     * Post/Reels like tugmasini bosish
     */
    fun togglePostLike(postId: String) {
        _userPosts.value = _userPosts.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLiked
                val newCount = if (newLiked) post.likesCount + 1 else maxOf(0, post.likesCount - 1)
                post.copy(isLiked = newLiked, likesCount = newCount)
            } else post
        }
    }

    /**
     * Postga yangi izoh (komentariya) yozish
     */
    fun addCommentToPost(postId: String, text: String) {
        val user = _currentUser.value ?: getOrCreateActiveUser()
        if (text.isBlank()) return

        val newComment = com.example.model.CommentItem(
            id = "c_${System.currentTimeMillis()}",
            postId = postId,
            authorId = user.id,
            authorName = user.fullName,
            authorGender = user.gender,
            authorPhotoPath = user.photoPath,
            text = text.trim(),
            timestamp = "Hozirgina",
            likesCount = 0,
            isLiked = false,
            replies = emptyList()
        )

        _userPosts.value = _userPosts.value.map { post ->
            if (post.id == postId) {
                val updatedComments = post.comments + newComment
                val totalComments = updatedComments.sumOf { 1 + it.replies.size }
                post.copy(comments = updatedComments, commentsCount = totalComments)
            } else post
        }
    }

    /**
     * Izohga javob (Reply) yozish
     */
    fun addReplyToComment(postId: String, commentId: String, replyToUserName: String, text: String) {
        val user = _currentUser.value ?: getOrCreateActiveUser()
        if (text.isBlank()) return

        val newReply = com.example.model.CommentReply(
            id = "rep_${System.currentTimeMillis()}",
            authorId = user.id,
            authorName = user.fullName,
            authorGender = user.gender,
            authorPhotoPath = user.photoPath,
            replyToUserName = replyToUserName,
            text = text.trim(),
            timestamp = "Hozirgina",
            likesCount = 0,
            isLiked = false
        )

        _userPosts.value = _userPosts.value.map { post ->
            if (post.id == postId) {
                val updatedComments = post.comments.map { comment ->
                    if (comment.id == commentId) {
                        comment.copy(replies = comment.replies + newReply)
                    } else comment
                }
                val totalComments = updatedComments.sumOf { 1 + it.replies.size }
                post.copy(comments = updatedComments, commentsCount = totalComments)
            } else post
        }
    }

    /**
     * Komentariyaga like bosish
     */
    fun toggleCommentLike(postId: String, commentId: String) {
        _userPosts.value = _userPosts.value.map { post ->
            if (post.id == postId) {
                val updatedComments = post.comments.map { comment ->
                    if (comment.id == commentId) {
                        val newLiked = !comment.isLiked
                        val newLikes = if (newLiked) comment.likesCount + 1 else maxOf(0, comment.likesCount - 1)
                        comment.copy(isLiked = newLiked, likesCount = newLikes)
                    } else comment
                }
                post.copy(comments = updatedComments)
            } else post
        }
    }

    /**
     * Koment javobiga like bosish
     */
    fun toggleReplyLike(postId: String, commentId: String, replyId: String) {
        _userPosts.value = _userPosts.value.map { post ->
            if (post.id == postId) {
                val updatedComments = post.comments.map { comment ->
                    if (comment.id == commentId) {
                        val updatedReplies = comment.replies.map { rep ->
                            if (rep.id == replyId) {
                                val newLiked = !rep.isLiked
                                val newLikes = if (newLiked) rep.likesCount + 1 else maxOf(0, rep.likesCount - 1)
                                rep.copy(isLiked = newLiked, likesCount = newLikes)
                            } else rep
                        }
                        comment.copy(replies = updatedReplies)
                    } else comment
                }
                post.copy(comments = updatedComments)
            } else post
        }
    }

    private val activePollingCandidateIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun startPollingMessagesForCandidate(candidateId: String) {
        if (candidateId.startsWith("cand_") || candidateId == "system_ai_hakam" || candidateId == AI_HAKAM_USER_PROFILE.id) return
        if (!activePollingCandidateIds.add(candidateId)) return
        repositoryScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    // 1. Fetch from Firebase Realtime Database as the main multi-device real-time sync bridge!
                    val firebaseMessages = com.example.network.FirebaseRealtimeSync.fetchChatMessagesFromFirebase(currentUserId, candidateId)
                    if (firebaseMessages.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val currentMap = _chatMessages.value.toMutableMap()
                            val localList = currentMap[candidateId]?.toMutableList() ?: mutableListOf()
                            var updated = false
                            firebaseMessages.forEach { fMsg ->
                                if (localList.none { it.id == fMsg.id }) {
                                    localList.add(fMsg)
                                    updated = true
                                }
                            }
                            if (updated) {
                                currentMap[candidateId] = localList
                                _chatMessages.value = currentMap
                                repositoryScope.launch(Dispatchers.IO) {
                                    roomChatDao.insertMessages(firebaseMessages.map { RoomChatMessage.fromChatMessage(it, currentUserId) })
                                }
                            }
                        }
                    }

                    // 2. Fetch from REST central server fallback
                    val response = BackendManager.getService().getChatMessages(candidateId)
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.success && body.data != null) {
                            val serverMessages = body.data.map { dto ->
                                ChatMessage(
                                    id = dto.id,
                                    senderId = dto.senderId,
                                    receiverId = dto.receiverId,
                                    text = dto.text,
                                    timestamp = dto.timestamp,
                                    isFromMe = dto.senderId == currentUserId,
                                    isRead = true,
                                    reaction = dto.reaction
                                )
                            }
                            if (serverMessages.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    val currentMap = _chatMessages.value.toMutableMap()
                                    val localList = currentMap[candidateId]?.toMutableList() ?: mutableListOf()
                                    var updated = false
                                    serverMessages.forEach { sMsg ->
                                        if (localList.none { it.id == sMsg.id }) {
                                            localList.add(sMsg)
                                            updated = true
                                        }
                                    }
                                    if (updated) {
                                        currentMap[candidateId] = localList
                                        _chatMessages.value = currentMap
                                        
                                        // Save to Room DB in background
                                        repositoryScope.launch(Dispatchers.IO) {
                                            roomChatDao.insertMessages(serverMessages.map { RoomChatMessage.fromChatMessage(it, currentUserId) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("UserRepository", "Sync with central server candidate polling error: ${e.message}")
                }
                delay(2000) // Poll central server & Firebase every 2 seconds for live device sync
            }
        }
    }

    /**
     * Nomzod bilan xabarlar ro'yxatini olish (REST GET /api/messages/{user_id}/{target_id})
     * UI thread'ni aslo to'sib qo'ymaslik uchun to'liq asinxron va kesh asosida ishlaydi.
     */
    fun getMessagesForCandidate(candidateId: String): List<ChatMessage> {
        // Start real-time server sync polling for multi-device bridge
        startPollingMessagesForCandidate(candidateId)

        val existing = _chatMessages.value[candidateId]
        if (existing != null) {
            return existing
        }
        val currentId = currentUserId
        val memoryHistory = ChatRealtimeEngine.getConversationHistory(currentId, candidateId)

        // Asinxron tarzda Room bazasidan yuklash (UI thread zarracha qotmaydi)
        repositoryScope.launch(Dispatchers.IO) {
            val dbMessages = roomChatDao.getMessagesForSession(currentId, candidateId)
            val list = if (dbMessages.isNotEmpty()) {
                dbMessages.map { it.toChatMessage(currentId) }
            } else if (memoryHistory.isNotEmpty()) {
                roomChatDao.insertMessages(memoryHistory.map { RoomChatMessage.fromChatMessage(it, currentId) })
                memoryHistory
            } else {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                val updatedMap = _chatMessages.value.toMutableMap()
                updatedMap[candidateId] = list
                _chatMessages.value = updatedMap
            }
        }

        return memoryHistory
    }

    /**
     * Nomzodning barcha o'qilmagan xabarlarini o'qilgan (isRead = true) holatiga o'tkazish
     */
    fun markMessagesAsRead(candidateId: String) {
        val currentId = currentUserId
        ChatRealtimeEngine.markMessagesAsRead(currentId, candidateId)
        
        repositoryScope.launch(Dispatchers.IO) {
            try {
                roomChatDao.markMessagesAsRead(currentId, candidateId)
            } catch (e: Exception) {
                Log.e("UserRepository", "Failed to mark messages as read in DB", e)
            }
        }

        val messages = _chatMessages.value[candidateId]
        if (messages != null && messages.any { !it.isFromMe && !it.isRead }) {
            val updated = messages.map { msg ->
                if (!msg.isFromMe && !msg.isRead) {
                    msg.copy(isRead = true)
                } else {
                    msg
                }
            }
            val map = _chatMessages.value.toMutableMap()
            map[candidateId] = updated
            _chatMessages.value = map
        }
    }

    /**
     * Nomzod bo'yicha o'qilmagan xabarlar soni
     */
    fun getUnreadCountForCandidate(candidateId: String): Int {
        val list = _chatMessages.value[candidateId]
        val fromState = list?.count { !it.isFromMe && !it.isRead } ?: 0
        val fromEngine = ChatRealtimeEngine.getUnreadCount(currentUserId, candidateId)
        return maxOf(fromState, fromEngine)
    }

    /**
     * Nomzodga xabar uzatish (REST POST /api/messages/send va Real-time socket push)
     */
    fun receiveMessageFromCandidate(
        candidateId: String,
        text: String
    ) {
        ChatRealtimeEngine.sendMessage(candidateId, currentUserId, text)
    }

    fun sendMessageToCandidate(
        candidateId: String,
        text: String,
        sharedReel: UserPost? = null
    ) {
        val currentId = currentUserId
        val sentMsg = ChatRealtimeEngine.sendMessage(
            senderId = currentId,
            receiverId = candidateId,
            text = text,
            sharedReel = sharedReel
        )
        
        // Save to Room DB in background
        repositoryScope.launch {
            roomChatDao.insertMessage(RoomChatMessage.fromChatMessage(sentMsg, currentId))
        }

        val currentMap = _chatMessages.value.toMutableMap()
        val list = currentMap[candidateId]?.toMutableList() ?: getMessagesForCandidate(candidateId).toMutableList()
        if (list.none { it.id == sentMsg.id }) {
            list.add(sentMsg)
            currentMap[candidateId] = list
            _chatMessages.value = currentMap
        }

        // Central Cloud Server Sync (Bridge between all physical devices)
        if (!candidateId.startsWith("cand_") && candidateId != AI_HAKAM_USER_PROFILE.id && candidateId != "system_ai_hakam") {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    com.example.network.FirebaseRealtimeSync.sendChatMessageToFirebase(currentId, candidateId, sentMsg)
                    BackendManager.getService().sendMessage(
                        com.example.network.SendMessageDto(
                            receiverId = candidateId,
                            text = text,
                            sharedReelId = sharedReel?.id
                        )
                    )
                    Log.d("UserRepository", "VisolApiService message sent successfully to central server and Firebase.")
                } catch (e: Exception) {
                    Log.w("UserRepository", "VisolApiService sendMessage network sync error: ${e.message}")
                }
            }
        }

        // AI Hakam maxsus profiliga xabar yozilganda AI Hakam javob berishi
        if (candidateId == AI_HAKAM_USER_PROFILE.id || candidateId == "system_ai_hakam") {
            repositoryScope.launch {
                val reply = com.example.util.GeminiAiHakamService.askAiHakam(context, text)
                receiveMessageFromCandidate(AI_HAKAM_USER_PROFILE.id, reply)
            }
        }

        // Demo bot profiles reply logic (Only for demo profiles starting with "cand_")
        if (candidateId.startsWith("cand_")) {
            repositoryScope.launch {
                delay(2500)
                val replies = listOf(
                    "Assalomu alaykum! Profilingiz juda ma'qul keldi. Sog'-salomatmisiz? 😊",
                    "Assalomu alaykum! Jiddiy niyatdaman. Oila qurish haqida fikrlaringiz qanday? 🌸",
                    "Rahmat samimiy javobingiz uchun! Hozirda qayerda yashaysiz?",
                    "Suhbatingizdan juda mamnunman! O'zingiz haqingizda yana gapirib bering ✨",
                    "Inshalloh, niyatimiz pok. Kuningiz xayrli va barakali o'tsin! 🤲"
                )
                val randomReply = replies.random()
                receiveMessageFromCandidate(candidateId, randomReply)
            }
        }
    }

    fun sendTypingStatus(candidateId: String, isTyping: Boolean) {
        ChatRealtimeEngine.sendTypingStatus(currentUserId, candidateId, isTyping)
    }

    fun sendChatMessage(candidateId: String, text: String) {
        sendMessageToCandidate(candidateId, text)
    }

    fun sendPhotoToCandidate(
        candidateId: String,
        photoUri: String,
        caption: String = ""
    ) {
        val currentId = currentUserId
        val sentMsg = ChatRealtimeEngine.sendMessage(
            senderId = currentId,
            receiverId = candidateId,
            text = caption.ifBlank { "🖼️ Rasm" },
            photoUri = photoUri
        )

        // Save to Room DB in background
        repositoryScope.launch {
            roomChatDao.insertMessage(RoomChatMessage.fromChatMessage(sentMsg, currentId))
        }

        val currentMap = _chatMessages.value.toMutableMap()
        val list = currentMap[candidateId]?.toMutableList() ?: getMessagesForCandidate(candidateId).toMutableList()
        if (list.none { it.id == sentMsg.id }) {
            list.add(sentMsg)
            currentMap[candidateId] = list
            _chatMessages.value = currentMap
        }

        // Multi-modal tahlil: AI Hakamga rasm yuborilganda Gemini Vision orqali tahlil qilib javob berish
        if (candidateId == AI_HAKAM_USER_PROFILE.id || candidateId == "system_ai_hakam") {
            repositoryScope.launch {
                val reply = com.example.util.GeminiAiHakamService.askAiHakam(
                    context = context,
                    userQuery = caption.ifBlank { "Ushbu yuborilgan rasmni odob me'yorlari va xavfsizlik jihatidan ko'rib chiqing." },
                    photoUriString = photoUri
                )
                receiveMessageFromCandidate(AI_HAKAM_USER_PROFILE.id, reply)
            }
        }
    }

    fun sendVoiceNoteToCandidate(
        candidateId: String,
        audioUri: String? = null,
        durationSec: Int = 12
    ) {
        val currentId = currentUserId
        val sentMsg = ChatRealtimeEngine.sendMessage(
            senderId = currentId,
            receiverId = candidateId,
            text = "🎙️ Ovozli xabar",
            audioUri = audioUri,
            audioDurationSec = durationSec
        )

        // Save to Room DB in background
        repositoryScope.launch {
            roomChatDao.insertMessage(RoomChatMessage.fromChatMessage(sentMsg, currentId))
        }

        val currentMap = _chatMessages.value.toMutableMap()
        val list = currentMap[candidateId]?.toMutableList() ?: getMessagesForCandidate(candidateId).toMutableList()
        if (list.none { it.id == sentMsg.id }) {
            list.add(sentMsg)
            currentMap[candidateId] = list
            _chatMessages.value = currentMap
        }
    }

    /**
     * AI Hakam (3-shaxs) ogohlantirishini faqat qoidabuzar foydalanuvchiga ko'rsatish
     * (Mojaroning oldini olish maqsadida 2-shaxsga hech narsa yuborilmaydi)
     */
    fun addAiHakamWarningMessage(
        candidateId: String,
        warningText: String,
        violationReason: String
    ) {
        val currentId = currentUserId
        val hakamMsg = ChatMessage(
            id = "ai_hakam_${System.currentTimeMillis()}",
            senderId = "ai_hakam_system",
            receiverId = currentId,
            text = warningText,
            timestamp = "Hozirgina",
            isFromMe = false,
            isAiHakamWarning = true,
            aiHakamReason = violationReason
        )

        // Save to Room DB in background so user sees their warning even after app reopen
        repositoryScope.launch {
            roomChatDao.insertMessage(RoomChatMessage.fromChatMessage(hakamMsg, currentId))
        }

        val currentMap = _chatMessages.value.toMutableMap()
        val list = currentMap[candidateId]?.toMutableList() ?: getMessagesForCandidate(candidateId).toMutableList()
        list.add(hakamMsg)
        currentMap[candidateId] = list
        _chatMessages.value = currentMap
    }

    /**
     * Xabarga reaksiya qo'yish yoki bekor qilish (Instagram Direct kabi: ❤️, 👍, 😂, 🔥, 😮, 😢, 🤲)
     */
    fun toggleReaction(candidateId: String, messageId: String, emoji: String) {
        val currentList = getMessagesForCandidate(candidateId).toMutableList()
        val index = currentList.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = currentList[index]
            val nextReaction = if (msg.reaction == emoji) null else emoji
            val updated = msg.copy(reaction = nextReaction)
            currentList[index] = updated
            
            // Save to Room DB in background
            repositoryScope.launch {
                roomChatDao.updateMessageReaction(messageId, nextReaction)
            }

            val map = _chatMessages.value.toMutableMap()
            map[candidateId] = currentList
            _chatMessages.value = map
        }
    }

    fun reactToMessage(candidateId: String, messageId: String, emoji: String) {
        toggleReaction(candidateId, messageId, emoji)
    }

    /**
     * Rasmiy Ilova Administratori Profilini olish (Yagona va xolis ma'muriyat profili)
     */
    fun getOfficialAdminProfile(): UserProfile {
        val current = _currentUser.value
        if (current != null && current.isAdminUser) {
            return current
        }
        val adminInList = _candidates.value.find { it.isAdminUser }
        if (adminInList != null) {
            return adminInList
        }
        return ADMIN_OFFICIAL_PROFILE
    }

    /**
     * VIP aloqa qoidasi:
     * AI Hakam va Admin bilan har qanday foydalanuvchi bepul va erkin suhbatlasha oladi.
     * Boshqa nomzodlar bilan suhbatlashish uchun VIP obuna kerak bo'ladi.
     */
    fun canChatWithCandidate(candidateId: String): Boolean {
        val officialAdmin = getOfficialAdminProfile()
        if (candidateId == AI_HAKAM_USER_PROFILE.id || candidateId == officialAdmin.id || 
            candidateId == "system_ai_hakam" || candidateId == "admin_009" || candidateId == "system_admin_visol") {
            return true
        }
        return _isSubscribed.value
    }

    /**
     * Barcha suhbatdoshlar ro'yxatini olish (AI Hakam, Admin va foydalanuvchilar)
     * - AI Hakam xolis akkaunti barcha foydalanuvchilar chatida ko'rinadi
     * - Admin akkaunti oddiy foydalanuvchilarga rasmiy muloqot sifatida ko'rinadi
     * - Agar joriy kirgan shaxs Admin bo'lsa, o'z akkaunti ko'rinmaydi (faqat boshqa yozishmalar ko'rinadi)
     */
    fun getChatConversations(): List<Pair<UserProfile, ChatMessage>> {
        val officialAdmin = getOfficialAdminProfile()
        val candidatesMap = _candidates.value.associateBy { it.id }.toMutableMap()
        candidatesMap[AI_HAKAM_USER_PROFILE.id] = AI_HAKAM_USER_PROFILE
        candidatesMap[officialAdmin.id] = officialAdmin

        val result = mutableListOf<Pair<UserProfile, ChatMessage>>()

        // 1. AI Hakam doimiy profili (Xolis neytral hakam barcha foydalanuvchilarga ko'rinadi)
        val hakamMessages = _chatMessages.value[AI_HAKAM_USER_PROFILE.id] ?: emptyList()
        val lastHakamMsg = hakamMessages.lastOrNull() ?: ChatMessage(
            id = "init_hakam_chat",
            senderId = AI_HAKAM_USER_PROFILE.id,
            receiverId = currentUserId,
            text = "⚖️ Assalomu alaykum! Men Visol.uz xolis AI Hakamiman. Odob-axloq, muloqot madaniyati yoki xavfsizlik bo'yicha savollaringiz bo'lsa, marhamat!",
            timestamp = "Doimiy",
            isFromMe = false
        )
        result.add(AI_HAKAM_USER_PROFILE to lastHakamMsg)

        // 2. Yagona Rasmiy Admin profili (Faqat oddiy foydalanuvchilar chatlarida chiqadi)
        val isMeAdmin = _currentUser.value?.isAdminUser == true
        if (!isMeAdmin) {
            val adminMessages = _chatMessages.value[officialAdmin.id] ?: emptyList()
            val lastAdminMsg = adminMessages.lastOrNull() ?: ChatMessage(
                id = "init_admin_chat",
                senderId = officialAdmin.id,
                receiverId = currentUserId,
                text = "🛡️ Assalomu alaykum! Ilova administratoriga xush kelibsiz. Qanday yordam bera olamiz?",
                timestamp = "Doimiy",
                isFromMe = false
            )
            result.add(officialAdmin to lastAdminMsg)
        }

        // 3. Nomzodlar va barcha muloqotdagilar bilan suhbatlar (xabar kelgan barcha profil ko'rinishi shart)
        val persistedUsers = loadPersistedRegisteredUsers()
        _chatMessages.value.forEach { (candidateId, messages) ->
            if (candidateId != AI_HAKAM_USER_PROFILE.id && candidateId != officialAdmin.id && candidateId != "system_admin_visol" && candidateId != "admin_009") {
                val foundUser = candidatesMap[candidateId]
                    ?: _candidates.value.find { it.id == candidateId }
                    ?: persistedUsers.find { it.id == candidateId }
                    ?: _currentUser.value?.takeIf { it.id == candidateId }
                val candidate = foundUser ?: UserProfile(
                    id = candidateId,
                    fullName = customContactNames.value[candidateId]?.takeIf { it.isNotBlank() } ?: "Foydalanuvchi",
                    gender = "Ayol",
                    age = 23,
                    viloyat = "Toshkent shahri",
                    tuman = "Chilonzor tumani",
                    mahalla = "Visol MFY",
                    username = "user_${candidateId.take(6)}",
                    photoPath = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
                    bio = "Visol.uz samimiy a'zosi 🌸",
                    isCurrentUser = false
                )
                val lastMsg = messages.lastOrNull()
                if (lastMsg != null) {
                    result.add(candidate to lastMsg)
                }
            }
        }

        return result.sortedByDescending { it.second.timestampMillis }
    }

    /**
     * Foydalanuvchining shaxsiy yuklagan postlari (User Posts filtered by ID)
     */
    fun getUserPostsFiltered(userId: String?): List<UserPost> {
        val targetId = userId ?: (_currentUser.value?.id ?: "user_me")
        val currentUserName = _currentUser.value?.fullName ?: ""
        return _userPosts.value.filter {
            it.authorId == targetId || it.authorId == "user_me" || (currentUserName.isNotBlank() && it.authorName.equals(currentUserName, ignoreCase = true))
        }
    }

    /**
     * Foydalanuvchining barcha postlari bo'yicha dinamik jami layklar soni
     */
    fun getUserTotalLikes(userId: String?): Int {
        val posts = getUserPostsFiltered(userId)
        return posts.sumOf { it.likesCount }
    }

    /**
     * Foydalanuvchining dinamik jami ko'rishlar (views) soni
     */
    fun getUserTotalViews(userId: String?): Int {
        val posts = getUserPostsFiltered(userId)
        return posts.sumOf { (it.likesCount * 4) + (it.commentsCount * 6) } + (_visitors.value.size * 5)
    }

    /**
     * Tizimdan chiqish va profilni tozalash
     */
    fun logout() {
        prefs.edit()
            .remove(KEY_USER_DATA)
            .putBoolean(KEY_IS_REGISTERED, false)
            .remove("key_auth_token")
            .remove("key_auth_email")
            .apply()
        _currentUser.value = null
        _isSubscribed.value = false
        _subscriptionPlan.value = ""
    }

    fun deleteAccount() {
        val currentId = currentUserId
        logout()
        
        // Permanent account deletion
        val deletedIds = prefs.getStringSet("key_deleted_account_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedIds.add(currentId)
        prefs.edit().putStringSet("key_deleted_account_ids", deletedIds).apply()

        repositoryScope.launch {
            roomChatDao.deleteMessagesForSession(currentId)
        }
    }

    fun isAccountDeleted(userId: String): Boolean {
        val deletedIds = prefs.getStringSet("key_deleted_account_ids", emptySet()) ?: emptySet()
        return deletedIds.contains(userId)
    }

    fun loginWithGoogle(email: String, fullName: String): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()
        val cleanId = "google_${cleanEmail.substringBefore("@")}"
        
        // Check if there is an existing profile
        val existing = _candidates.value.find { it.id == cleanId }
        val finalProfile = if (existing != null) {
            existing.copy(isCurrentUser = true)
        } else {
            UserProfile(
                id = cleanId,
                fullName = fullName,
                gender = "Ayol", // Default simulation gender
                age = 23,
                viloyat = "Toshkent shahri",
                tuman = "Uchtepa tumani",
                mahalla = "Vatan MFY",
                photoPath = "ic_deleted_user", // Default grey placeholder drawn by Vector
                bio = "Google orqali avtorizatsiyadan o'tilgan. Jiddiy oila qurish niyatida.",
                username = cleanEmail.substringBefore("@"),
                telegram = cleanEmail.substringBefore("@"),
                phone = "+998 90 999 54 54",
                isVerifiedCamera = true,
                isCurrentUser = true,
                isVip = true
            )
        }

        // Generate Auth Token for authentication verification
        val token = "auth_token_${UUID.randomUUID()}"
        prefs.edit()
            .putString("key_auth_token", token)
            .putString("key_auth_email", cleanEmail)
            .apply()

        registerUser(finalProfile)
        
        // Reload messages for the new user session from Room
        repositoryScope.launch {
            val dbMessages = roomChatDao.getAllMessagesForSession(cleanId)
            val mappedMessages = dbMessages.map { it.toChatMessage(cleanId) }.groupBy { if (it.senderId == cleanId) it.receiverId else it.senderId }
            withContext(Dispatchers.Main) {
                _chatMessages.value = mappedMessages
            }
        }

        return Result.success(finalProfile)
    }

    private suspend fun generateAiHakamReply(userText: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Siz "Visol.uz" (O'zbekiston oilaviy tanishuv va nikoh ilovasi)ning rasmiy xolis "AI Hakam"isiz.
                    Jinsingiz neytral (barcha foydalanuvchilarga teng, xolis va samimiy munosabatdasiz).
                    Foydalanuvchi sizga quyidagicha yozdi: "$userText"

                    Unga o'zbekona odob-axloq, samimiylik, sabr-toqat va ilova qoidalari doirasida qisqa (2-3 jumla), dono va iliq javob bering.
                """.trimIndent()

                val requestJson = org.json.JSONObject().apply {
                    val contentsArray = org.json.JSONArray().apply {
                        val contentObj = org.json.JSONObject().apply {
                            val partsArray = org.json.JSONArray().apply {
                                val textPart = org.json.JSONObject().apply {
                                    put("text", prompt)
                                }
                                put(textPart)
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)
                }

                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val url = java.net.URL(endpoint)
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                java.io.OutputStreamWriter(conn.outputStream).use { it.write(requestJson.toString()) }

                if (conn.responseCode in 200..299) {
                    val responseStr = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream)).readText()
                    val respJson = org.json.JSONObject(responseStr)
                    val candidates = respJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        val reply = parts.getJSONObject(0).getString("text")
                        if (reply.isNotBlank()) {
                            return@withContext reply.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("UserRepository", "AI Hakam reply error: ${e.message}")
            }
        }

        when {
            userText.contains("salom", ignoreCase = true) || userText.contains("assalomu", ignoreCase = true) ->
                "Assalomu alaykum! Xush kelibsiz. Visol.uz da munosabatlaringiz xayrli va mustahkam bo'lishini tilayman. Qanday yordam bera olaman?"
            userText.contains("qoida", ignoreCase = true) || userText.contains("odob", ignoreCase = true) ->
                "Visol.uz qoidalari: samimiy niyat, o'zaro hurmat, haqorat va 18+ kontentdan yiroq bo'lish. Odob — inson ziynatidir."
            userText.contains("shikoyat", ignoreCase = true) || userText.contains("nizo", ignoreCase = true) ->
                "Agar biror nojo'ya holatga duch kelsangiz, xavotir olmang. AI Hakam va moderatorlarimiz har doim adolat va xolislikni ta'minlaydi."
            else ->
                "Murojaatingiz uchun tashakkur. Har bir muloqotda o'zbekona muloyimlik va xolislikka rioya qilish go'zal natijalarga olib keladi. Savollaringiz bo'lsa, javob berishga tayyorman! ✨"
        }
    }

    companion object {
        val AI_HAKAM_USER_PROFILE = UserProfile(
            id = "system_ai_hakam",
            fullName = "AI Hakam (Xolis Nazoratchi)",
            gender = "Neytral",
            age = 0,
            viloyat = "O'zbekiston",
            tuman = "Xolislik & Nazorat",
            mahalla = "Odob-Axloq Markazi",
            photoPath = "ai_hakam_badge",
            bio = "⚖️ Visol.uz xolis AI Hakami. Barcha foydalanuvchilarga jinsidan qat'i nazar teng va xolis munosabatda bo'laman. Odob me'yorlari, nizo va tushunmovchiliklar yoki ilova xavfsizligi bo'yicha savollaringizga javob beraman.",
            username = "ai_hakam_uz",
            isVerifiedCamera = true,
            isVip = true
        )

        val ADMIN_OFFICIAL_PROFILE = UserProfile(
            id = "admin_009",
            fullName = "ILOVA ADMINISTRATORI",
            gender = "Erkak",
            age = 28,
            viloyat = "Toshkent shahri",
            tuman = "Mirobod tumani",
            mahalla = "Markaziy Visol MFY",
            photoPath = "avatar_male_1",
            bio = "🛡️ Visol.uz rasmiy ma'muriyati va boshqaruv markazi 👑",
            username = "visoladmin_009",
            telegram = "@visoladmin_009",
            phone = "+998 71 200 00 09",
            isVerifiedCamera = true,
            isAdmin = true,
            isVip = true
        )

        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_USER_DATA = "current_user_data"
        private const val KEY_IS_SUBSCRIBED = "is_subscribed"
        private const val KEY_SUB_PLAN = "subscription_plan"
        private const val KEY_SUB_EXPIRES_AT = "subscription_expires_at"
        private const val KEY_FCM_TOKEN = "fcm_device_token"
        private const val KEY_LAST_VIEWED_VISITOR_COUNT = "last_viewed_visitor_count"
        private const val KEY_THEME_MODE = "visol_theme_mode"
        private const val KEY_CONTACTS_SYNCED = "visol_contacts_synced"
        private const val KEY_USER_POSTS_JSON = "key_user_posts_json"
        private const val KEY_FOLLOWING_USERS_SET = "key_following_users_set"
    }
}
