package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.UserProfile
import com.example.ui.theme.*

/**
 * Voice Room ichidagi foydalanuvchining qulay yarim oyna (Half-screen Modal BottomSheet) profili:
 * - Fotosurat ustiga bosganda to'liq profilga olib boradi
 * - Obuna bo'lish (Follow/Unfollow) tugmasi
 * - Shaxsiy xabar yozish va sovg'a ulashish
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomUserProfileBottomSheet(
    user: UserProfile,
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onViewFullProfile: (UserProfile) -> Unit,
    onOpenPrivateChat: (UserProfile) -> Unit,
    onSendGiftToUser: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val followedIds by userRepository.followedCandidateIds.collectAsState()
    val isFollowing = remember(followedIds, user.id) { followedIds.contains(user.id) }
    val isDark = userRepository.themeMode.collectAsState().value != "LIGHT"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xF21C0E2D) else Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = if (isDark) Color(0x66FFFFFF) else Color(0x66000000)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. AVATAR WITH VIP CROWN (Clickable to view full profile)
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(96.dp)
                    .clickable {
                        onDismiss()
                        onViewFullProfile(user)
                    }
            ) {
                AsyncImage(
                    model = user.photoPath,
                    contentDescription = user.fullName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(
                            2.5.dp,
                            Brush.linearGradient(listOf(WarmHoneyGold, SoftRosePink, SoftPeach)),
                            CircleShape
                        )
                )

                Surface(
                    shape = CircleShape,
                    color = WarmHoneyGold,
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, Color.White, CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("👑", fontSize = 11.sp)
                    }
                }
            }

            // 2. NAME, USERNAME & BADGES
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.fullName,
                        color = if (isDark) Color.White else TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = SoftRosePink) {
                        Text(
                            text = "VIP 5",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = user.displayUsername,
                    color = WarmHoneyGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (user.viloyat.isNotBlank()) {
                    Text(
                        text = "📍 ${user.viloyat}${if (user.tuman.isNotBlank()) ", " + user.tuman else ""}",
                        color = if (isDark) Color(0xFFC4A8B9) else TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${user.bio}\"",
                        color = if (isDark) Color.White.copy(alpha = 0.85f) else TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // 3. ACTION BUTTONS (Obuna bo'lish, Shaxsiy Chat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Obuna bo'lish tugmasi
                Button(
                    onClick = {
                        userRepository.toggleFollowCandidate(user.id)
                        Toast.makeText(
                            context,
                            if (isFollowing) "${user.fullName} obunadan chiqarildi" else "${user.fullName}ga obuna bo'lindi! 💖",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0x33FFFFFF) else SoftRosePink
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isFollowing) "Obunadasiz" else "Obuna bo'lish",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Shaxsiy Chat tugmasi
                Button(
                    onClick = {
                        onDismiss()
                        onOpenPrivateChat(user)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmHoneyGold),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Xabar yozish",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Profilni to'liq ko'rish havolasi
            Surface(
                onClick = {
                    onDismiss()
                    onViewFullProfile(user)
                },
                shape = RoundedCornerShape(16.dp),
                color = Color(0x22FFFFFF),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "To'liq profil ma'lumotlarini ko'rish 👤 ➔",
                        color = WarmHoneyGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
