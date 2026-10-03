package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ChatMessage
import com.example.model.UserPost

@Entity(tableName = "chat_messages")
data class RoomChatMessage(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: String,
    val photoUri: String? = null,
    val audioUri: String? = null,
    val audioDurationSec: Int? = null,
    val isRead: Boolean = false,
    val reaction: String? = null,
    val isAiHakamWarning: Boolean = false,
    val aiHakamReason: String? = null,
    val currentUserSessionId: String // For strict individual session isolation
) {
    fun toChatMessage(activeUserId: String? = null): ChatMessage {
        val viewer = activeUserId ?: currentUserSessionId
        val isMe = senderId == viewer || 
                   ((viewer == "me" || viewer == "user_me") && (senderId == "me" || senderId == "user_me"))
        return ChatMessage(
            id = id,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = timestamp,
            photoUri = photoUri,
            audioUri = audioUri,
            audioDurationSec = audioDurationSec,
            isFromMe = isMe,
            isRead = isRead,
            reaction = reaction,
            isAiHakamWarning = isAiHakamWarning,
            aiHakamReason = aiHakamReason
        )
    }

    companion object {
        fun fromChatMessage(msg: ChatMessage, sessionUserId: String): RoomChatMessage {
            return RoomChatMessage(
                id = msg.id,
                senderId = msg.senderId,
                receiverId = msg.receiverId,
                text = msg.text,
                timestamp = msg.timestamp,
                photoUri = msg.photoUri,
                audioUri = msg.audioUri,
                audioDurationSec = msg.audioDurationSec,
                isRead = msg.isRead,
                reaction = msg.reaction,
                isAiHakamWarning = msg.isAiHakamWarning,
                aiHakamReason = msg.aiHakamReason,
                currentUserSessionId = sessionUserId
            )
        }
    }
}
