package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserRepository
import com.example.model.UserPost
import com.example.model.UserProfile
import com.example.ui.theme.*

/**
 * INSTAGRAM DIRECT USLUBIDAGI REELS VIDEONI NOMZOD CHATIGA UZATISH VARAQASI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReelToChatBottomSheet(
    reel: UserPost,
    userRepository: UserRepository,
    isSubscribed: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRequireSubscription: () -> Unit,
    onOpenChatWithCandidate: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val candidates by userRepository.candidates.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val sentCandidateIds = remember { mutableStateListOf<String>() }

    val filteredCandidates = remember(candidates, searchQuery) {
        if (searchQuery.isBlank()) candidates
        else candidates.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.viloyat.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) DeepNavyMesh else Color.White,
        contentColor = if (isDark) Color(0xFFF8FAFC) else TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Sarlavha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = VelvetRosePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Direct orqali ulashish",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else TextPrimary
                        )
                        Text(
                            text = "Reels videoni nomzod chatiga yuboring",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Yopish",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Qidiruv maydoni
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Nomzodlarni qidirish...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Tozalash")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VelvetRosePrimary,
                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Nomzodlar ro'yxati
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCandidates, key = { it.id }) { candidate ->
                    val isSent = sentCandidateIds.contains(candidate.id)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(
                                photoPath = candidate.photoPath,
                                gender = candidate.gender,
                                size = 44.dp,
                                showCameraBadge = false
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = candidate.fullName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${candidate.dynamicAge} yosh • ${candidate.viloyat}",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else TextSecondary
                                )
                            }

                            if (isSent) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenChatWithCandidate(candidate)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Chatni ochish",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                val canChatWithCandidate = isSubscribed || userRepository.canChatWithCandidate(candidate.id)
                                Button(
                                    onClick = {
                                        if (!canChatWithCandidate) {
                                            onDismiss()
                                            onRequireSubscription()
                                        } else {
                                            userRepository.sendMessageToCandidate(
                                                candidateId = candidate.id,
                                                text = "Salom! Bu ajoyib videoni sizga ulashdim 🎬",
                                                sharedReel = reel
                                            )
                                            sentCandidateIds.add(candidate.id)
                                            Toast.makeText(
                                                context,
                                                "Video ${candidate.fullName} chatiga yuborildi 🚀",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Yuborish",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
