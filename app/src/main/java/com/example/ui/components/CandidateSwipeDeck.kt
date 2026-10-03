package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Modern Dating Swipe Card Deck (Tinder / Bumble / Hinge style)
 * with animated gestures, Voice Bio preview, Compatibility Badge, and action buttons.
 */
@Composable
fun CandidateSwipeDeck(
    candidates: List<UserProfile>,
    isSubscribed: Boolean,
    isDark: Boolean,
    onViewCandidateDetails: (UserProfile) -> Unit,
    onOpenChat: (UserProfile) -> Unit,
    onRequireSubscription: () -> Unit,
    onLike: (UserProfile) -> Unit = {},
    onSuperLike: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentIndex by remember(candidates) { mutableIntStateOf(0) }
    var historyIndex by remember { mutableStateOf<Int?>(null) }
    var lastActionText by remember { mutableStateOf<String?>(null) }

    // Swipe drag offset state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val currentCandidate = candidates.getOrNull(currentIndex)
    val nextCandidate = candidates.getOrNull(currentIndex + 1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (currentCandidate == null) {
            // All candidates swiped out state
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(28.dp),
                backgroundColor = if (isDark) WarmChocoGlassDark else Color.White.copy(alpha = 0.9f),
                borderColor = SoftGlassBorderDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(WarmPeachSoft, SoftRosePinkGlow.copy(alpha = 0.3f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = SoftRosePink,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Bugungi barcha nomzodlar ko'rib chiqildi! 🎉",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color.White else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tez orada yangi qizlar va yigitlar qo'shiladi yoki filtrlarni kengaytirib ko'ring.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color(0xFFC4A8B9) else TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { currentIndex = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftPeach
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Boshidan ko'rish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Background Next Card preview (for layered 3D depth)
            if (nextCandidate != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .graphicsLayer {
                            scaleX = 0.93f
                            scaleY = 0.93f
                            translationY = 24.dp.toPx()
                            alpha = 0.75f
                        }
                ) {
                    SwipeCandidateCardView(
                        candidate = nextCandidate,
                        isSubscribed = isSubscribed,
                        isDark = isDark,
                        onCardClick = {}
                    )
                }
            }

            // Foreground Active Swipable Card
            val rotationDegree = (offsetX / 25f).coerceIn(-22f, 22f)
            val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "card_x")
            val animatedOffsetY by animateFloatAsState(targetValue = offsetY, label = "card_y")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                    .graphicsLayer {
                        rotationZ = rotationDegree
                    }
                    .pointerInput(currentCandidate.id) {
                        detectDragGestures(
                            onDragEnd = {
                                if (offsetX > 260f) {
                                    // Swiped Right -> LIKE
                                    onLike(currentCandidate)
                                    historyIndex = currentIndex
                                    lastActionText = "YOQDI ❤️"
                                    currentIndex++
                                    offsetX = 0f
                                    offsetY = 0f
                                } else if (offsetX < -260f) {
                                    // Swiped Left -> PASS
                                    historyIndex = currentIndex
                                    lastActionText = "O'TKAZILDI ✖️"
                                    currentIndex++
                                    offsetX = 0f
                                    offsetY = 0f
                                } else if (offsetY < -280f) {
                                    // Swiped Up -> SUPER LIKE
                                    historyIndex = currentIndex
                                    onSuperLike(currentCandidate)
                                    currentIndex++
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    // Snap back
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        )
                    }
            ) {
                SwipeCandidateCardView(
                    candidate = currentCandidate,
                    isSubscribed = isSubscribed,
                    isDark = isDark,
                    onCardClick = { onViewCandidateDetails(currentCandidate) }
                )

                // LIKE Stamp overlay on right drag
                if (offsetX > 60f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(24.dp)
                            .graphicsLayer { rotationZ = -15f }
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE610B981))
                            .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "YOQDI ❤️",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                }

                // NOPE Stamp overlay on left drag
                if (offsetX < -60f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .graphicsLayer { rotationZ = 15f }
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE6EF4444))
                            .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "KEYINGISI ❌",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // Bottom Floating Action Control Pill
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (isDark) Color(0xD820122B) else Color(0xF2FFFFFF))
                    .border(BorderStroke(1.dp, SoftGlassBorderDark), RoundedCornerShape(32.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Revert button (Qaytarish)
                IconButton(
                    onClick = {
                        if (historyIndex != null && historyIndex!! >= 0) {
                            currentIndex = historyIndex!!
                            historyIndex = null
                        }
                    },
                    enabled = historyIndex != null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (historyIndex != null) Color(0xFFFBBF24).copy(alpha = 0.2f) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Qaytarish",
                        tint = if (historyIndex != null) WarmGold else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Pass button (❌)
                IconButton(
                    onClick = {
                        historyIndex = currentIndex
                        currentIndex++
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .border(BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Keyingisi",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Super Like (⭐)
                IconButton(
                    onClick = {
                        if (currentCandidate != null) {
                            onSuperLike(currentCandidate)
                            historyIndex = currentIndex
                            currentIndex++
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                        .border(BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Super Like",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Like Button (❤️)
                IconButton(
                    onClick = {
                        if (currentCandidate != null) {
                            onLike(currentCandidate)
                            historyIndex = currentIndex
                            currentIndex++
                        }
                    },
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(SoftPeach, SoftRosePink))
                        )
                        .shadow(8.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Yoqdi",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Direct Chat Launcher (💬)
                IconButton(
                    onClick = {
                        if (currentCandidate != null) {
                            if (isSubscribed) {
                                onOpenChat(currentCandidate)
                            } else {
                                onRequireSubscription()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(WarmPeachSoft)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Xabar yozish",
                        tint = SoftPeach,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual High-Fidelity Swipable Dating Card
 */
@Composable
fun SwipeCandidateCardView(
    candidate: UserProfile,
    isSubscribed: Boolean,
    isDark: Boolean,
    onCardClick: () -> Unit
) {
    val context = LocalContext.current
    var isAudioPlaying by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(32.dp),
        backgroundColor = if (isDark) Color(0x991E102A) else Color.White.copy(alpha = 0.65f),
        borderColor = SoftGlassBorderDark,
        borderWidth = 1.5.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Hero Photo / Avatar Background
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
                                    listOf(Color(0xFF831843), Color(0xFF1E102A))
                                else
                                    listOf(Color(0xFF1E3A8A), Color(0xFF1E102A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatar(
                        photoPath = "",
                        gender = candidate.gender,
                        size = 140.dp,
                        showCameraBadge = false
                    )
                }
            }

            // Top Ambient Soft Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
            )

            // Bottom Deep Gradient Scrim for readable texts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            // Top Bar Badges (ID, Verified Face ID, Status)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ID Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, WarmPeach.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = candidate.formattedId,
                            color = WarmPeachLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Face ID & VIP badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xCC10B981),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Face ID", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (candidate.isVip) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xCCF59E0B),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("VIP", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom Profile Information Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .padding(bottom = 72.dp) // space for bottom pills
            ) {
                // Name, Age & Online dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${candidate.fullName}, ${candidate.dynamicAge} yosh",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Location Pill + Profession / Status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = WarmPeachLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${candidate.viloyat}, ${candidate.tuman.ifBlank { "Markaz" }}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (candidate.mahalla.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = WarmGoldLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = candidate.mahalla,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Short Bio / Purpose statement
                if (candidate.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "“${candidate.bio}”",
                        color = Color(0xFFF1E6EE),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Voice Bio / Music waveform preview bar
                Surface(
                    onClick = {
                        isAudioPlaying = !isAudioPlaying
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isAudioPlaying) SoftPeach.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isAudioPlaying) Color.White else Color.White.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Ovozli salomlashuv",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAudioPlaying) "Ovozli salomlashuv yangramoqda..." else "🎙️ Ovozli salomlashuvni tinglash",
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
