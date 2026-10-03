package com.example.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.UserProfile
import com.example.ui.theme.*

/**
 * Ultra-Modern Bento Grid Candidate Card (Warm Soft Glass)
 */
@Composable
fun CandidateBentoCard(
    candidate: UserProfile,
    isSubscribed: Boolean,
    isDark: Boolean,
    onViewDetails: () -> Unit,
    onOpenChat: () -> Unit,
    onRequireSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("candidate_card_${candidate.id}"),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = if (isDark) Color(0xFA1E122B) else Color(0xFAFFFFFF),
        borderColor = if (isDark) SoftGlassBorderDark.copy(alpha = 0.8f) else Color(0x99FF7B54),
        borderWidth = 1.2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Photo & Badges Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                if (candidate.photoPath.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(candidate.photoPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = candidate.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    if (candidate.gender == "Ayol")
                                        listOf(Color(0xFF831843), Color(0xFF2C1338))
                                    else
                                        listOf(Color(0xFF1E3A8A), Color(0xFF171B33))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(
                            photoPath = "",
                            gender = candidate.gender,
                            size = 80.dp,
                            showCameraBadge = false
                        )
                    }
                }

                // Top Subtle Vignette
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)))
                )

                // ID Tag Top-Start
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, SoftPeach.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = candidate.formattedId,
                        color = SoftPeach,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Region Pill Top-End
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = candidate.viloyat,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }

                // Online indicator Bottom-End
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .border(1.dp, Color.White, CircleShape)
                )
            }

            // Info Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Name & Verified Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = candidate.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Face ID Tasdiqlangan",
                        tint = if (candidate.isVip) WarmHoneyGold else Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(1.dp))

                // Age & District
                Text(
                    text = "${candidate.dynamicAge} yosh • ${candidate.tuman.ifBlank { candidate.viloyat }}",
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFFD4B8C7) else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftPeach
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Profil",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            if (isSubscribed) {
                                onOpenChat()
                            } else {
                                onRequireSubscription()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF331E46) else WarmPeachSoft
                        ),
                        border = BorderStroke(1.dp, SoftPeach.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Xabar",
                            tint = if (isDark) WarmPeachLight else SoftPeach,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Xabar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) WarmPeachLight else SoftPeach
                        )
                    }
                }
            }
        }
    }
}
