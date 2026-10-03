package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserRepository
import com.example.model.UserProfile
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.VelvetRosePrimary
import com.example.ui.theme.VelvetRoseSoft
import java.text.SimpleDateFormat
import java.util.*

/**
 * Chat Conversations Bottom Sheet:
 * Displayed when user clicks the "Chat" button in the bottom navigation bar (available after subscribing).
 * Displays all conversations with candidates, their latest messages, timestamps, and allows opening real-time chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListBottomSheet(
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onOpenChatWithCandidate: (UserProfile) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val candidates by userRepository.candidates.collectAsState()
    val allChatMessages by userRepository.chatMessages.collectAsState()
    val customContactNames by userRepository.customContactNames.collectAsState()
    val conversations = remember(allChatMessages, candidates, customContactNames) { userRepository.getChatConversations() }
    var searchQuery by remember { mutableStateOf("") }

    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val inputBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF1F5F9)

    val filteredConversations = remember(searchQuery, conversations, customContactNames) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { (candidate, message) ->
                val dispName = customContactNames[candidate.id]?.takeIf { it.isNotBlank() } ?: candidate.fullName
                dispName.contains(searchQuery, ignoreCase = true) ||
                candidate.fullName.contains(searchQuery, ignoreCase = true) ||
                candidate.displayUsername.contains(searchQuery, ignoreCase = true) ||
                message.text.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSecondary.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = VelvetRosePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Suhbatlar (Chat)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VelvetRoseSoft
                ) {
                    Text(
                        text = "${conversations.size} ta suhbat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetRosePrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Ism, saqlangan nom yoki @username...", fontSize = 13.sp, color = textSecondary) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = VelvetRosePrimary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (conversations.isEmpty()) {
                // Empty state - Suggest candidates to start chatting with
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Hozircha xabarlar yo'q",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Quyidagi nomzodlardan birini tanlab, birinchi bo'lib suhbat boshlang!",
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Candidate recommendations
                        Text(
                            text = "Tavsiya etilgan nomzodlar:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(candidates.take(5)) { candidate ->
                                val isDeleted = userRepository.isAccountDeleted(candidate.id)
                                val dispName = if (isDeleted) "Visol foydalanuvchisi" else (customContactNames[candidate.id]?.takeIf { it.isNotBlank() } ?: candidate.fullName)
                                CandidateChatListItem(
                                    candidate = candidate,
                                    displayName = dispName,
                                    photoPath = if (isDeleted) "ic_deleted_user" else candidate.photoPath,
                                    lastMessageText = "Suhbatni boshlash uchun bosing ✨",
                                    timestamp = System.currentTimeMillis(),
                                    unreadCount = 0,
                                    isDark = isDark,
                                    onClick = {
                                        onOpenChatWithCandidate(candidate)
                                    }
                                )
                                HorizontalDivider(
                                    color = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF1F5F9),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            } else {
                // Active conversations list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredConversations) { (candidate, lastMessage) ->
                        val formattedTime = lastMessage.timestamp
                        val isDeleted = userRepository.isAccountDeleted(candidate.id)
                        val dispName = if (isDeleted) "Visol foydalanuvchisi" else (customContactNames[candidate.id]?.takeIf { it.isNotBlank() } ?: candidate.fullName)
                        val unreadCount = userRepository.getUnreadCountForCandidate(candidate.id)

                        CandidateChatListItem(
                            candidate = candidate,
                            displayName = dispName,
                            photoPath = if (isDeleted) "ic_deleted_user" else candidate.photoPath,
                            lastMessageText = lastMessage.text,
                            timestamp = 0L,
                            formattedTime = formattedTime,
                            unreadCount = unreadCount,
                            isDark = isDark,
                            onClick = {
                                userRepository.markMessagesAsRead(candidate.id)
                                onOpenChatWithCandidate(candidate)
                            }
                        )
                        HorizontalDivider(
                            color = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF1F5F9),
                            thickness = 0.5.dp
                        )
                    }

                    // Also show other available candidates to message
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Boshqa nomzodlar bilan yozishish:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    val existingUserIds = conversations.map { it.first.id }.toSet()
                    val otherCandidates = candidates.filter { it.id !in existingUserIds }

                    items(otherCandidates.take(6)) { candidate ->
                        val isDeleted = userRepository.isAccountDeleted(candidate.id)
                        val dispName = if (isDeleted) "Visol foydalanuvchisi" else (customContactNames[candidate.id]?.takeIf { it.isNotBlank() } ?: candidate.fullName)
                        val unreadCount = userRepository.getUnreadCountForCandidate(candidate.id)

                        CandidateChatListItem(
                            candidate = candidate,
                            displayName = dispName,
                            photoPath = if (isDeleted) "ic_deleted_user" else candidate.photoPath,
                            lastMessageText = "Salom! Siz bilan tanishsam bo'ladimi?",
                            timestamp = System.currentTimeMillis(),
                            unreadCount = unreadCount,
                            isDark = isDark,
                            onClick = {
                                onOpenChatWithCandidate(candidate)
                            }
                        )
                        HorizontalDivider(
                            color = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF1F5F9),
                            thickness = 0.5.dp
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateChatListItem(
    candidate: UserProfile,
    displayName: String = candidate.fullName,
    photoPath: String = candidate.photoPath,
    lastMessageText: String,
    timestamp: Long,
    formattedTime: String? = null,
    unreadCount: Int = 0,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val isUnread = unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with Online dot
        Box(contentAlignment = Alignment.BottomEnd) {
            ProfileAvatar(
                photoPath = photoPath,
                gender = candidate.gender,
                size = 52.dp,
                showCameraBadge = false
            )
            // Green online indicator
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .background(Color(0xFF22C55E), CircleShape)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = displayName,
                        fontSize = 15.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isUnread) {
                        // Glowing unread message indicator dot
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(VelvetRosePrimary)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                }

                if (formattedTime != null) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = if (isUnread) VelvetRosePrimary else textSecondary,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidate.displayUsername,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = VelvetRosePrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = lastMessageText,
                    fontSize = 13.sp,
                    color = if (isUnread) textColor else textSecondary,
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isUnread) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = VelvetRosePrimary,
                        modifier = Modifier.sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$unreadCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
