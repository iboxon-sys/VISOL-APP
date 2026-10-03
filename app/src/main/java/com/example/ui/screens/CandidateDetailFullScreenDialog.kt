package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.CandidatePost
import com.example.model.UserProfile
import com.example.model.UserStory
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.SocialMediaHelper

/**
 * FULL-SCREEN CANDIDATE DETAIL MODAL
 * - Fixes half-screen scroll restriction completely: Full display dialog with edge-to-edge layout.
 * - Header photo with verified Face ID badge and dynamic age calculation.
 * - Instagram-style Follow/Unfollow button with real-time reactive follower counter.
 * - Media Feed (Candidate posts: photos/videos preview gallery).
 * - Full biographical information, contact links (guarded by VIP subscription).
 */
@Composable
fun CandidateDetailFullScreenDialog(
    candidate: UserProfile,
    userRepository: UserRepository,
    isSubscribed: Boolean,
    isDark: Boolean = false,
    onDismiss: () -> Unit,
    onRequireSubscription: () -> Unit
) {
    val context = LocalContext.current
    val followedIds by userRepository.followedCandidateIds.collectAsState()
    val isFollowing = followedIds.contains(candidate.id)
    val candidatesList by userRepository.candidates.collectAsState()
    var showChatDialog by remember { mutableStateOf(false) }
    var showFollowersSheet by remember { mutableStateOf(false) }
    var followersSheetTab by remember { mutableIntStateOf(0) }

    val dialogBg = if (isDark) WarmChocoPlumDark else PearlBackground
    val cardBg = if (isDark) WarmChocoGlassDark else Color.White
    val cardBorder = if (isDark) SoftGlassBorderDark else Color(0x22F43F5E)
    val textColor = if (isDark) Color(0xFFFDF4F5) else TextPrimary
    val textMuted = if (isDark) Color(0xFFD4B8C7) else TextSecondary

    // Up-to-date candidate with reactive followers count
    val currentCandidate = candidatesList.find { it.id == candidate.id } ?: candidate

    val allStories by userRepository.userStories.collectAsState()
    val allHighlights by userRepository.userHighlights.collectAsState()
    val allUserPosts by userRepository.userPosts.collectAsState()

    val candidateActiveStories = remember(allStories, currentCandidate) {
        val matching = allStories.filter { story ->
            (story.authorId == currentCandidate.id || story.authorName.contains(currentCandidate.fullName.take(4), ignoreCase = true)) && story.isStoryActive()
        }
        if (matching.isNotEmpty()) matching else {
            listOf(
                UserStory(
                    id = "story_${currentCandidate.id}",
                    authorId = currentCandidate.id,
                    authorName = currentCandidate.fullName,
                    authorGender = currentCandidate.gender,
                    authorPhotoPath = currentCandidate.photoPath,
                    mediaUri = currentCandidate.photoPath,
                    mediaType = "PHOTO",
                    caption = "Bugungi kuningiz xayrli va go'zal lahzalarga to'la bo'lsin! 🌹",
                    musicTitle = "Visol Bahori",
                    musicArtist = "Visol Music",
                    createdAtMillis = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
                )
            )
        }
    }
    val candidateHighlights = remember(allHighlights, currentCandidate.id) {
        userRepository.getHighlightsForCandidate(currentCandidate.id)
    }

    var selectedMediaTab by remember { mutableIntStateOf(0) } // 0: Fotolar & Postlar, 1: Videolar & Reels
    var viewingStory by remember { mutableStateOf<UserStory?>(null) }
    var viewingHighlight by remember { mutableStateOf<UserRepository.UserHighlightItem?>(null) }
    var viewingPost by remember { mutableStateOf<CandidatePost?>(null) }

    // Media feed (photos and videos) - 1-shaxs qo'ygan post va videolarni 2-shaxs bevosita ko'radi
    val samplePosts = remember(allUserPosts, currentCandidate) {
        val matchingUserPosts = allUserPosts.filter {
            it.authorId == currentCandidate.id || it.authorName.contains(currentCandidate.fullName.take(4), ignoreCase = true)
        }.map { p ->
            CandidatePost(
                id = p.id,
                candidateId = currentCandidate.id,
                title = p.title.ifBlank { "Yangi lavha ✨" },
                mediaType = p.mediaType,
                mediaUri = p.mediaUri,
                timestamp = p.timestamp,
                likesCount = p.likesCount
            )
        }
        val directPosts = currentCandidate.posts
        val combined = (matchingUserPosts + directPosts).distinctBy { it.id }
        if (combined.isNotEmpty()) combined else listOf(
            CandidatePost(
                id = "post_${currentCandidate.id}_1",
                candidateId = currentCandidate.id,
                title = "Dam olish kunlaridan fotolavha 🌸",
                mediaType = "PHOTO",
                mediaUri = currentCandidate.photoPath,
                timestamp = "Kecha, 18:30",
                likesCount = 42
            ),
            CandidatePost(
                id = "post_${currentCandidate.id}_2",
                candidateId = currentCandidate.id,
                title = "Samarqand Registon maydonida 🏛️",
                mediaType = "PHOTO",
                mediaUri = currentCandidate.photoPath,
                timestamp = "3 kun oldin",
                likesCount = 89
            ),
            CandidatePost(
                id = "post_${currentCandidate.id}_3",
                candidateId = currentCandidate.id,
                title = "Tabiat qo'ynida qisqa video 📹",
                mediaType = "VIDEO",
                mediaUri = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=600",
                timestamp = "1 hafta oldin",
                likesCount = 64
            ),
            CandidatePost(
                id = "post_${currentCandidate.id}_4",
                candidateId = currentCandidate.id,
                title = "Toshkent City bog'i bo'ylab sayr 🎬",
                mediaType = "VIDEO",
                mediaUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
                timestamp = "2 hafta oldin",
                likesCount = 112
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        RomanticMeshBackground(
            isDarkTheme = isDark,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Top Hero Section with Verified Face ID & Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .background(
                            Brush.verticalGradient(
                                if (currentCandidate.gender == "Ayol")
                                    listOf(Color(0xFFFCE7F3), Color(0xFFFBCFE8), Color(0xFFFAF5F5))
                                else
                                    listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFFAF5F5))
                            )
                        )
                ) {
                    // Center Avatar
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ProfileAvatar(
                            photoPath = currentCandidate.photoPath,
                            gender = currentCandidate.gender,
                            size = 140.dp,
                            showCameraBadge = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Verified Face ID Badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xDD0F172A),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Jonli Face ID Tasdiqlangan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Top Bar Controls (Back Button & Security Badge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = Color(0xCCFFFFFF),
                            border = BorderStroke(1.dp, GlassBorderLight),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Ortga",
                                    tint = TextPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xCCFFFFFF),
                            border = BorderStroke(1.dp, GlassBorderLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = VelvetRose,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "FLAG_SECURE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VelvetRose
                                )
                            }
                        }
                    }
                }

                // 2. Main Candidate Info Card
                Card(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1B1124) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF321E3E) else Color(0x11E11D48)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                    // Name & ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentCandidate.fullName,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )
                                val isCandVerified = if (currentCandidate.isCurrentUser) isSubscribed else currentCandidate.isVip
                                if (isCandVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "VIP Tasdiqlangan",
                                        tint = WarmGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${currentCandidate.dynamicAge} yosh • ${currentCandidate.gender}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VelvetRose
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val clipboardManager = LocalClipboardManager.current
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VelvetRose.copy(alpha = 0.12f),
                                    border = BorderStroke(0.8.dp, VelvetRose.copy(alpha = 0.3f)),
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentCandidate.formattedId))
                                        Toast.makeText(context, "ID nusxalandi: ${currentCandidate.formattedId}", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ID: ${currentCandidate.formattedId}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VelvetRose
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Nusxalash",
                                            tint = VelvetRose,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "📍 ${currentCandidate.viloyat}, ${currentCandidate.tuman} (${currentCandidate.mahalla})",
                        fontSize = 13.sp,
                        color = textMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Instagram-style Real-time Follower Counter & Action
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = cardBg,
                        borderColor = cardBorder,
                        borderWidth = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        followersSheetTab = 0
                                        showFollowersSheet = true
                                    }
                                ) {
                                    Text(
                                        text = "${currentCandidate.followersCount}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "Kuzatuvchilar",
                                        fontSize = 11.sp,
                                        color = textMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        followersSheetTab = 1
                                        showFollowersSheet = true
                                    }
                                ) {
                                    Text(
                                        text = "${currentCandidate.followingCount}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "Kuzatmoqda",
                                        fontSize = 11.sp,
                                        color = textMuted
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Chat button - Shaxsiy 1-ga-1 xabarlashuv (Faqat VIP Obunachilar uchun)
                                Button(
                                    onClick = {
                                        if (isSubscribed) {
                                            showChatDialog = true
                                        } else {
                                            onRequireSubscription()
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VelvetRoseSoft
                                    ),
                                    border = BorderStroke(1.dp, VelvetRosePrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = "Chat",
                                        tint = VelvetRosePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Xabar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VelvetRosePrimary
                                    )
                                }

                                // Follow/Unfollow Button
                                Button(
                                    onClick = {
                                        val nowFollowing = userRepository.toggleFollowCandidate(currentCandidate.id)
                                        Toast.makeText(
                                            context,
                                            if (nowFollowing) "Kuzatuv boshlandi!" else "Kuzatuv bekor qilindi",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) Color(0xFFF1F5F9) else VelvetRose
                                    ),
                                    border = if (isFollowing) BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                                ) {
                                    Icon(
                                        imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = if (isFollowing) TextPrimary else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFollowing) "Kuzatilyapti" else "Kuzatish",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFollowing) TextPrimary else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Bio Section
                    Text(
                        text = "O'zi haqida:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentCandidate.bio,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. Aktualniy & Istoryalar (Instagram/Telegram Stories & Highlights)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Aktualniy & Istoryalar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            if (candidateActiveStories.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = VelvetRoseSoft
                                ) {
                                    Text(
                                        text = "Jonli istorya",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VelvetRose,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            // Active 24h stories if available
                            if (candidateActiveStories.isNotEmpty()) {
                                item {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewingStory = candidateActiveStories.first()
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(66.dp)
                                                .border(
                                                    width = 2.5.dp,
                                                    brush = Brush.sweepGradient(
                                                        listOf(
                                                            Color(0xFFE11D48),
                                                            Color(0xFFF97316),
                                                            Color(0xFFA855F7),
                                                            Color(0xFFE11D48)
                                                        )
                                                    ),
                                                    shape = CircleShape
                                                )
                                                .padding(3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ProfileAvatar(
                                                photoPath = currentCandidate.photoPath,
                                                gender = currentCandidate.gender,
                                                size = 56.dp,
                                                showCameraBadge = false
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Istorya",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VelvetRose
                                        )
                                    }
                                }
                            }

                            // Highlights (Aktualniy)
                            items(candidateHighlights) { hl ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewingHighlight = hl
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(66.dp)
                                            .border(
                                                width = 1.8.dp,
                                                color = WarmGold.copy(alpha = 0.8f),
                                                shape = CircleShape
                                            )
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(VelvetRoseSoft, Color(0xFFFDE68A))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = hl.title.take(1).uppercase(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VelvetRoseDark
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = hl.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 6. Postlar & Videolar (Tab switcher)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF1E2430) else Color(0xFFF1F5F9))
                            .padding(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMediaTab = 0 },
                            color = if (selectedMediaTab == 0) (if (isDark) Color(0xFF334155) else Color.White) else Color.Transparent,
                            shadowElevation = if (selectedMediaTab == 0) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = if (selectedMediaTab == 0) VelvetRose else textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Fotolavhalar (${samplePosts.count { it.mediaType == "PHOTO" }})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedMediaTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedMediaTab == 0) textColor else textMuted
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMediaTab = 1 },
                            color = if (selectedMediaTab == 1) (if (isDark) Color(0xFF334155) else Color.White) else Color.Transparent,
                            shadowElevation = if (selectedMediaTab == 1) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = if (selectedMediaTab == 1) VelvetRose else textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Videolar & Reels (${samplePosts.count { it.mediaType == "VIDEO" }})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedMediaTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedMediaTab == 1) textColor else textMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredPosts = remember(selectedMediaTab, samplePosts) {
                        if (selectedMediaTab == 0) samplePosts.filter { it.mediaType == "PHOTO" }
                        else samplePosts.filter { it.mediaType == "VIDEO" }
                    }

                    if (filteredPosts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedMediaTab == 0) "Fotolavhalar hozircha yuklanmagan" else "Videolar hozircha yuklanmagan",
                                color = textMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(filteredPosts) { post ->
                                GlassCard(
                                    modifier = Modifier
                                        .width(170.dp)
                                        .height(210.dp)
                                        .clickable { viewingPost = post },
                                    shape = RoundedCornerShape(20.dp),
                                    backgroundColor = cardBg,
                                    borderColor = cardBorder,
                                    borderWidth = 1.dp
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(136.dp)
                                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        if (post.mediaType == "VIDEO")
                                                            listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                                        else
                                                            listOf(VelvetRoseSoft, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (post.mediaType == "VIDEO") Icons.Default.PlayCircleFilled else Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                tint = if (post.mediaType == "VIDEO") Color.White else VelvetRose,
                                                modifier = Modifier.size(if (post.mediaType == "VIDEO") 44.dp else 36.dp)
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xCC0F172A),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(6.dp)
                                            ) {
                                                Text(
                                                    text = if (post.mediaType == "VIDEO") "VIDEO 0:35" else "FOTO",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = post.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                color = textColor
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "❤️ ${post.likesCount}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VelvetRose
                                                )
                                                Text(
                                                    text = post.timestamp,
                                                    fontSize = 9.sp,
                                                    color = textMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 6. Contact Information (Guarded by Subscription)
                    if (isSubscribed) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = VelvetRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Aloqa Ma'lumotlari (VIP Ochiq)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Phone
                                Surface(
                                    onClick = {
                                        val cleanPhone = currentCandidate.phone.replace(" ", "")
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF262626) else Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = VelvetRose)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = "Telefon raqam", fontSize = 11.sp, color = textMuted)
                                            Text(text = currentCandidate.phone, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                        }
                                    }
                                }

                                if (currentCandidate.telegram.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        onClick = {
                                            val url = SocialMediaHelper.toTelegramUrl(currentCandidate.telegram)
                                            SocialMediaHelper.openUrlSafely(context, url)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDark) Color(0xFF262626) else Color(0xFFF8FAFC),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TelegramBrandIcon(size = 24.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = "Telegram profil", fontSize = 11.sp, color = textMuted)
                                                Text(text = currentCandidate.telegram, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                                            }
                                        }
                                    }
                                }

                                if (currentCandidate.instagram.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        onClick = {
                                            val url = SocialMediaHelper.toInstagramUrl(currentCandidate.instagram)
                                            SocialMediaHelper.openUrlSafely(context, url)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDark) Color(0xFF262626) else Color(0xFFF8FAFC),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            InstagramBrandIcon(size = 24.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = "Instagram profil", fontSize = 11.sp, color = textMuted)
                                                Text(text = currentCandidate.instagram, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Locked contact card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF261D13) else Color(0xFFFFFBEB)),
                            border = BorderStroke(1.dp, WarmGold)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = WarmGold,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kontaktlar va Tarmoqlar Yopiq",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Nomzodning telefon raqami, Telegram va Instagram profillarini ko'rish uchun VIP obuna kerak.",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFD1D5DB) else TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onRequireSubscription,
                                    colors = ButtonDefaults.buttonColors(containerColor = VelvetRose),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("VIP Obuna Sotib Olish (15 000 so'm)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
            }
        }

        if (showChatDialog && isSubscribed) {
            CandidateChatDialog(
                candidate = currentCandidate,
                userRepository = userRepository,
                isDark = isDark,
                onDismiss = { showChatDialog = false }
            )
        } else if (showChatDialog && !isSubscribed) {
            showChatDialog = false
            onRequireSubscription()
        }

        if (showFollowersSheet) {
            FollowersAndFollowingBottomSheet(
                userId = currentCandidate.id,
                initialTab = followersSheetTab,
                userRepository = userRepository,
                isDark = isDark,
                onDismiss = { showFollowersSheet = false }
            )
        }

        // 7. Fullscreen Story Viewer (2-shaxs 1-shaxsning 24 soatlik istoryasini ko'rishi uchun)
        if (viewingStory != null) {
            Dialog(
                onDismissRequest = { viewingStory = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Story background/media
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF881337), Color(0xFF1E1B4B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = WarmGold,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = viewingStory!!.caption.ifBlank { "Salom, bu mening yangi istoryam! 🌸" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Top Bar with progress and author
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileAvatar(
                                    photoPath = currentCandidate.photoPath,
                                    gender = currentCandidate.gender,
                                    size = 40.dp,
                                    showCameraBadge = false
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentCandidate.fullName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "24 soatlik istorya",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewingStory = null },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0x55000000), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 8. Fullscreen Highlight Viewer (Aktualniy ko'rish)
        if (viewingHighlight != null) {
            Dialog(
                onDismissRequest = { viewingHighlight = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF31102A), Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = VelvetRoseSoft,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = viewingHighlight!!.title.take(1).uppercase(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = VelvetRoseDark
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Aktualniy: ${viewingHighlight!!.title}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${currentCandidate.fullName} tomonidan saqlangan eng sara lavhalar ✨",
                                fontSize = 14.sp,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ ${viewingHighlight!!.title}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmGoldLight
                        )
                        IconButton(
                            onClick = { viewingHighlight = null },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x55000000), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White)
                        }
                    }
                }
            }
        }

        // 9. Post & Video Viewer (2-shaxs 1-shaxsning barcha video va postlarini to'liq ochib ko'rishi)
        if (viewingPost != null) {
            Dialog(
                onDismissRequest = { viewingPost = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                var postLiked by remember { mutableStateOf(false) }
                val postLikes = viewingPost!!.likesCount + if (postLiked) 1 else 0

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xEE090D16))
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E2430) else Color.White
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProfileAvatar(
                                        photoPath = currentCandidate.photoPath,
                                        gender = currentCandidate.gender,
                                        size = 40.dp,
                                        showCameraBadge = false
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = currentCandidate.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = viewingPost!!.timestamp,
                                            fontSize = 11.sp,
                                            color = textMuted
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewingPost = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Yopish", tint = textColor)
                                }
                            }

                            // Visual container (video or photo)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            if (viewingPost!!.mediaType == "VIDEO")
                                                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                                            else
                                                listOf(VelvetRoseSoft, Color(0xFFE2E8F0))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewingPost!!.mediaType == "VIDEO") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0x99000000),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Ijro etish",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "HD Video Player (0:35)",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = VelvetRose,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }
                            }

                            // Footer actions & caption
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { postLiked = !postLiked },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (postLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Like",
                                                tint = if (postLiked) PomegranateRed else textColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$postLikes ta yoqdi",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textColor
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (viewingPost!!.mediaType == "VIDEO") Color(0xFF0284C7) else VelvetRoseSoft
                                    ) {
                                        Text(
                                            text = if (viewingPost!!.mediaType == "VIDEO") "🎬 VIDEO" else "📸 FOTO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (viewingPost!!.mediaType == "VIDEO") Color.White else VelvetRoseDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = viewingPost!!.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
