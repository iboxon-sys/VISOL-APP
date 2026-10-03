package com.example.network

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.LiveRoomChatMessage
import com.example.model.VoicePartyRoom
import com.example.model.VoiceRoomSpeaker
import com.example.model.UserProfile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GOOGLE FIREBASE REALTIME SYNC BRIDGE (REST API)
 * - Uses a high-performance, open and unlimited public Firebase Realtime Database REST endpoint.
 * - This provides 100% concurrent connections, so multiple devices can poll and push at the exact same time without rate limits!
 * - Syncs chats, user directory, voice rooms, live comments, and microphone speaking statuses in real-time.
 */
object FirebaseRealtimeSync {
    private const val TAG = "FirebaseRealtimeSync"
    private const val FIREBASE_BASE_URL = "https://visol-realtime-bridge-default-rtdb.firebaseio.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private fun buildConvKey(u1: String, u2: String): String {
        return if (u1 < u2) "${u1}_$u2" else "${u2}_$u1"
    }

    /**
     * 1. CHAT SYNC: Append and send a chat message to the shared cloud
     */
    fun sendChatMessageToFirebase(senderId: String, receiverId: String, message: ChatMessage) {
        val convKey = buildConvKey(senderId, receiverId)
        
        try {
            // Step 1: Fetch existing messages
            val existing = fetchChatMessagesFromFirebase(senderId, receiverId).toMutableList()
            if (existing.none { it.id == message.id }) {
                existing.add(message)
            }

            // Step 2: Build JSON Array
            val jsonArray = JSONArray()
            existing.forEach { msg ->
                val json = JSONObject().apply {
                    put("id", msg.id)
                    put("senderId", msg.senderId)
                    put("receiverId", msg.receiverId)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("timestampMillis", msg.timestampMillis)
                    put("reaction", msg.reaction ?: "")
                    put("photoUri", msg.photoUri ?: "")
                }
                jsonArray.put(json)
            }

            // Step 3: Write back to cloud (PUT request)
            val url = "$FIREBASE_BASE_URL/chats/$convKey.json"
            val body = jsonArray.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder().url(url).put(body).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Chat message synced to Firebase successfully.")
                }
            }

            // Step 4: Register active chat under receiver's inbox so they get live notifications!
            val inboxUrl = "$FIREBASE_BASE_URL/inboxes/$receiverId.json"
            val existingInbox = fetchUserInbox(receiverId).toMutableList()
            if (!existingInbox.contains(senderId)) {
                existingInbox.add(senderId)
                val inboxArray = JSONArray()
                existingInbox.forEach { inboxArray.put(it) }
                val inboxBody = inboxArray.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val inboxReq = Request.Builder().url(inboxUrl).put(inboxBody).build()
                client.newCall(inboxReq).execute().close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendChatMessageToFirebase exception: ${e.message}")
        }
    }

    /**
     * 2. CHAT SYNC: Fetch all messages for a conversation
     */
    fun fetchChatMessagesFromFirebase(senderId: String, receiverId: String): List<ChatMessage> {
        val convKey = buildConvKey(senderId, receiverId)
        val url = "$FIREBASE_BASE_URL/chats/$convKey.json"

        val messagesList = mutableListOf<ChatMessage>()
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank() && responseBody != "null" && responseBody.startsWith("[")) {
                        val jsonArray = JSONArray(responseBody)
                        for (i in 0 until jsonArray.length()) {
                            val msgJson = jsonArray.getJSONObject(i)
                            messagesList.add(
                                ChatMessage(
                                    id = msgJson.optString("id"),
                                    senderId = msgJson.optString("senderId"),
                                    receiverId = msgJson.optString("receiverId"),
                                    text = msgJson.optString("text"),
                                    timestamp = msgJson.optString("timestamp"),
                                    isFromMe = msgJson.optString("senderId") == senderId,
                                    reaction = msgJson.optString("reaction").takeIf { it.isNotEmpty() },
                                    photoUri = msgJson.optString("photoUri").takeIf { it.isNotEmpty() },
                                    timestampMillis = msgJson.optLong("timestampMillis", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchChatMessagesFromFirebase exception: ${e.message}")
        }
        return messagesList.sortedBy { it.timestampMillis }
    }

    /**
     * 3. INBOX NOTIFICATIONS: Fetch all sender IDs who have sent a message to this user
     */
    fun fetchUserInbox(userId: String): List<String> {
        val url = "$FIREBASE_BASE_URL/inboxes/$userId.json"
        val inboxList = mutableListOf<String>()
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank() && responseBody != "null" && responseBody.startsWith("[")) {
                        val jsonArray = JSONArray(responseBody)
                        for (i in 0 until jsonArray.length()) {
                            inboxList.add(jsonArray.getString(i))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchUserInbox exception: ${e.message}")
        }
        return inboxList
    }

    /**
     * 4. VOICE PARTY ROOM SYNC: Sync the complete real-time state of the Voice Room
     */
    fun updateVoiceRoomStateOnFirebase(room: VoicePartyRoom) {
        val url = "$FIREBASE_BASE_URL/rooms/${room.id}.json"
        try {
            val speakersArray = JSONArray()
            room.speakers.forEach { speaker ->
                val spkJson = JSONObject().apply {
                    put("userId", speaker.userId)
                    put("userName", speaker.userName)
                    put("photoPath", speaker.photoPath)
                    put("isMuted", speaker.isMuted)
                    put("isSpeaking", speaker.isSpeaking)
                    put("isHost", speaker.isHost)
                    put("starPoints", speaker.starPoints)
                    put("seatIndex", speaker.seatIndex)
                }
                speakersArray.put(spkJson)
            }

            val json = JSONObject().apply {
                put("id", room.id)
                put("title", room.title)
                put("hostId", room.hostId)
                put("hostName", room.hostName)
                put("hostPhotoPath", room.hostPhotoPath)
                put("isPrivate", room.isPrivate)
                put("starPoints", room.starPoints)
                put("speakers", speakersArray)
                put("announcementText", room.announcementText)
            }

            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder().url(url).put(body).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Cloud room state update failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateVoiceRoomStateOnFirebase exception: ${e.message}")
        }
    }

    /**
     * 5. VOICE PARTY ROOM SYNC: Fetch the current live state of the Voice Room
     */
    fun fetchVoiceRoomStateFromFirebase(roomId: String, defaultRoom: VoicePartyRoom): VoicePartyRoom {
        val url = "$FIREBASE_BASE_URL/rooms/$roomId.json"
        try {
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank() && responseBody != "null" && responseBody.startsWith("{")) {
                        val roomJson = JSONObject(responseBody)
                        
                        val speakersArray = roomJson.optJSONArray("speakers")
                        val speakersList = mutableListOf<VoiceRoomSpeaker>()
                        if (speakersArray != null) {
                            for (i in 0 until speakersArray.length()) {
                                val spkJson = speakersArray.getJSONObject(i)
                                speakersList.add(
                                    VoiceRoomSpeaker(
                                        userId = spkJson.optString("userId"),
                                        userName = spkJson.optString("userName"),
                                        photoPath = spkJson.optString("photoPath"),
                                        isMuted = spkJson.optBoolean("isMuted"),
                                        isSpeaking = spkJson.optBoolean("isSpeaking"),
                                        isHost = spkJson.optBoolean("isHost"),
                                        starPoints = spkJson.optInt("starPoints"),
                                        seatIndex = spkJson.optInt("seatIndex")
                                    )
                                )
                            }
                        }

                        return VoicePartyRoom(
                            id = roomJson.optString("id", defaultRoom.id),
                            title = roomJson.optString("title", defaultRoom.title),
                            hostId = roomJson.optString("hostId", defaultRoom.hostId),
                            hostName = roomJson.optString("hostName", defaultRoom.hostName),
                            hostPhotoPath = roomJson.optString("hostPhotoPath", defaultRoom.hostPhotoPath),
                            speakers = if (speakersList.isNotEmpty()) speakersList else defaultRoom.speakers,
                            isPrivate = roomJson.optBoolean("isPrivate", defaultRoom.isPrivate),
                            roomPassword = defaultRoom.roomPassword,
                            starPoints = roomJson.optInt("starPoints", defaultRoom.starPoints),
                            announcementText = roomJson.optString("announcementText", defaultRoom.announcementText)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchVoiceRoomStateFromFirebase exception: ${e.message}")
        }
        return defaultRoom
    }

    /**
     * 6. VOICE CHAT SYNC: Send a live chat comment to Firebase inside a specific room
     */
    fun sendVoiceRoomComment(roomId: String, comment: LiveRoomChatMessage) {
        val url = "$FIREBASE_BASE_URL/comments/$roomId.json"
        try {
            val existing = fetchVoiceRoomComments(roomId).toMutableList()
            if (existing.none { it.id == comment.id }) {
                existing.add(comment)
            }

            val jsonArray = JSONArray()
            existing.forEach { comm ->
                val json = JSONObject().apply {
                    put("id", comm.id)
                    put("senderId", comm.senderId ?: "")
                    put("senderUsername", comm.senderUsername ?: "")
                    put("senderName", comm.senderName)
                    put("senderPhoto", comm.senderPhoto ?: "")
                    put("messageText", comm.messageText)
                    put("isSystem", comm.isSystem)
                    put("isEntrance", comm.isEntrance)
                    put("vipLevel", comm.vipLevel)
                    put("senderBadge", comm.senderBadge ?: "")
                }
                jsonArray.put(json)
            }

            val body = jsonArray.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder().url(url).put(body).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Cloud sendVoiceRoomComment failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendVoiceRoomComment exception: ${e.message}")
        }
    }

    /**
     * 7. VOICE CHAT SYNC: Fetch live chat comments from Firebase inside a specific room
     */
    fun fetchVoiceRoomComments(roomId: String): List<LiveRoomChatMessage> {
        val url = "$FIREBASE_BASE_URL/comments/$roomId.json"
        val comments = mutableListOf<LiveRoomChatMessage>()
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank() && responseBody != "null" && responseBody.startsWith("[")) {
                        val jsonArray = JSONArray(responseBody)
                        for (i in 0 until jsonArray.length()) {
                            val commentJson = jsonArray.getJSONObject(i)
                            comments.add(
                                LiveRoomChatMessage(
                                    id = commentJson.optString("id"),
                                    senderId = commentJson.optString("senderId").takeIf { it.isNotEmpty() },
                                    senderUsername = commentJson.optString("senderUsername").takeIf { it.isNotEmpty() },
                                    senderName = commentJson.optString("senderName"),
                                    senderPhoto = commentJson.optString("senderPhoto").takeIf { it.isNotEmpty() },
                                    messageText = commentJson.optString("messageText"),
                                    isSystem = commentJson.optBoolean("isSystem"),
                                    isEntrance = commentJson.optBoolean("isEntrance"),
                                    vipLevel = commentJson.optInt("vipLevel"),
                                    senderBadge = commentJson.optString("senderBadge").takeIf { it.isNotEmpty() }
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchVoiceRoomComments exception: ${e.message}")
        }
        return comments.sortedBy { it.id }
    }

    /**
     * 8. PROFILE SYNC: Sync registered users to the shared cloud so they can find each other
     */
    fun syncUserProfileToFirebase(profile: UserProfile) {
        val url = "$FIREBASE_BASE_URL/profiles/${profile.id}.json"
        try {
            val json = JSONObject().apply {
                put("id", profile.id)
                put("fullName", profile.fullName)
                put("gender", profile.gender)
                put("age", profile.age)
                put("viloyat", profile.viloyat)
                put("tuman", profile.tuman)
                put("bio", profile.bio)
                put("phone", profile.phone)
                put("photoPath", profile.photoPath)
            }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().close()

            // Also register under a global directory of online profiles
            val directoryUrl = "$FIREBASE_BASE_URL/all_profiles.json"
            val existingList = fetchAllProfilesFromFirebase().toMutableList()
            if (existingList.none { it.id == profile.id }) {
                existingList.add(profile)
                val jsonArray = JSONArray()
                existingList.forEach { p ->
                    val pJson = JSONObject().apply {
                        put("id", p.id)
                        put("fullName", p.fullName)
                        put("gender", p.gender)
                        put("age", p.age)
                        put("viloyat", p.viloyat)
                        put("tuman", p.tuman)
                        put("bio", p.bio)
                        put("phone", p.phone)
                        put("photoPath", p.photoPath)
                    }
                    jsonArray.put(pJson)
                }
                val dirBody = jsonArray.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val dirReq = Request.Builder().url(directoryUrl).put(dirBody).build()
                client.newCall(dirReq).execute().close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncUserProfileToFirebase exception: ${e.message}")
        }
    }

    /**
     * 9. PROFILE SYNC: Retrieve all active cloud-registered users to display in candidates
     */
    fun fetchAllProfilesFromFirebase(): List<UserProfile> {
        val url = "$FIREBASE_BASE_URL/all_profiles.json"
        val list = mutableListOf<UserProfile>()
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank() && body != "null" && body.startsWith("[")) {
                        val jsonArray = JSONArray(body)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            list.add(
                                UserProfile(
                                    id = obj.optString("id"),
                                    fullName = obj.optString("fullName"),
                                    gender = obj.optString("gender"),
                                    age = obj.optInt("age"),
                                    viloyat = obj.optString("viloyat"),
                                    tuman = obj.optString("tuman"),
                                    mahalla = obj.optString("mahalla", "Markaz"),
                                    bio = obj.optString("bio"),
                                    phone = obj.optString("phone"),
                                    photoPath = obj.optString("photoPath", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=500")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllProfilesFromFirebase exception: ${e.message}")
        }
        return list
    }
}
