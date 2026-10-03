package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoomChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: RoomChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<RoomChatMessage>)

    @Query("SELECT * FROM chat_messages WHERE (senderId = :sessionId AND receiverId = :partnerId) OR (senderId = :partnerId AND receiverId = :sessionId) ORDER BY id ASC")
    suspend fun getMessagesForSession(sessionId: String, partnerId: String): List<RoomChatMessage>

    @Query("SELECT * FROM chat_messages WHERE senderId = :sessionId OR receiverId = :sessionId ORDER BY id ASC")
    suspend fun getAllMessagesForSession(sessionId: String): List<RoomChatMessage>

    @Query("UPDATE chat_messages SET isRead = 1 WHERE currentUserSessionId = :sessionId AND senderId = :partnerId AND receiverId = :sessionId")
    suspend fun markMessagesAsRead(sessionId: String, partnerId: String)

    @Query("UPDATE chat_messages SET reaction = :reaction WHERE id = :msgId")
    suspend fun updateMessageReaction(msgId: String, reaction: String?)

    @Query("DELETE FROM chat_messages WHERE currentUserSessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE id = :msgId")
    suspend fun deleteMessageById(msgId: String)
}
