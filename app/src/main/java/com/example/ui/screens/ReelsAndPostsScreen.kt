package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.CommentItem
import com.example.model.CommentReply
import com.example.model.ModerationSeverity
import com.example.model.ModerationTicket
import com.example.model.MusicTrack
import com.example.model.UserPost
import com.example.model.UserProfile
import com.example.service.media.MediaMuxingPipeline
import com.example.service.media.MediaUploadServiceClient
import com.example.service.music.MusicApiClient
import com.example.ui.components.AdminModerationDialog
import com.example.ui.components.CandidateChatDialog
import com.example.ui.components.MusicPickerBottomSheet
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ReelVideoPlayerView
import com.example.ui.components.ShareReelToChatBottomSheet
import com.example.ui.components.VideoMontageEditorModal
import com.example.ui.theme.*
import com.example.util.ImageModerationService
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Visol.uz ijtimoiy lentalari:
 * 1. 🎬 Reels: Instagram/TikTok uslubidagi vertikal to'liq oyna (Vertical Pager)
 * 2. 📸 Postlar: Rasm va matnli lentalar
 * 3. 💬 Real-vaqtli nested komentariya va javoblar tizimi
 * 4. OOM-xavfsiz video siqish va progress barli yuklagich
 * 5. Avatarlar va ismlar orqali profilga to'g'ridan-to'g'ri o'tish
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsAndReelsFeedScreen(
    userRepository: UserRepository,
    currentUser: UserProfile?,
    modifier: Modifier = Modifier,
    onCandidateClick: ((UserProfile) -> Unit)? = null,
    onSubscribeClick: (() -> Unit)? = null,
    onMyProfileClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val posts by userRepository.userPosts.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Reels, 1: Postlar
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showMusicPickerModal by remember { mutableStateOf(false) }
    var showRestrictedWarningDialog by remember { mutableStateOf(false) }
    var showAdminModerationDialog by remember { mutableStateOf(false) }

    // Komentariya oynasi (BottomSheet) holati
    var activePostForComments by remember { mutableStateOf<UserPost?>(null) }
    var sharingReelForChat by remember { mutableStateOf<UserPost?>(null) }
    var chatTargetCandidate by remember { mutableStateOf<UserProfile?>(null) }
    val isSubscribed by userRepository.isSubscribed.collectAsState()
    val followedCandidateIds by userRepository.followedCandidateIds.collectAsState()
    val context = LocalContext.current

    val activeUser = currentUser ?: userRepository.getOrCreateActiveUser()
    val isRestrictedUser = activeUser.isRestricted || activeUser.isBanned
    val isSpamUser = activeUser.isSpamRestricted
    var showSpamWarningDialog by remember { mutableStateOf(false) }

    fun attemptOpenCreatePost() {
        if (isRestrictedUser) {
            showRestrictedWarningDialog = true
        } else if (isSpamUser) {
            showSpamWarningDialog = true
        } else {
            showCreatePostDialog = true
        }
    }
    val reelsList = remember(posts, activeUser) {
        val rawReels = posts.filter { it.mediaType == "REELS" }
        com.example.service.recs.ReelsRecommendationEngine.getRecommendedReels(rawReels, activeUser.id)
    }
    val photoPostsList = remember(posts) { posts.filter { it.mediaType == "PHOTO" } }

    fun navigateToAuthorProfile(
        authorId: String,
        authorName: String,
        photo: String? = null,
        gender: String? = null,
        location: String? = null
    ) {
        if (authorId == activeUser.id) {
            // 1-Person Routing: Route self user to their own profile tab
            onMyProfileClick?.invoke()
        } else {
            // 2-Person Routing: Route to candidate/visitor profile
            val found = userRepository.findCandidateByIdOrName(
                authorId = authorId,
                authorName = authorName,
                fallbackPhotoPath = photo,
                fallbackGender = gender,
                fallbackLocation = location
            )
            if (found != null) {
                onCandidateClick?.invoke(found)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (selectedTab == 0) Color.Black else if (isDark) DeepNavyMesh else PearlBackground)
    ) {
        if (selectedTab == 0) {
            // 1. REELS: Vertikal to'liq oynada video lavhalar (Instagram / TikTok uslubida)
            if (reelsList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = VelvetRosePrimary,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Hozircha Reels videolari mavjud emas",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { attemptOpenCreatePost() },
                            colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary)
                        ) {
                            Text("Birinchi Reelsni joylash 🎬")
                        }
                    }
                }
            } else {
                ReelsVerticalPagerView(
                    reels = reelsList,
                    followedCandidateIds = followedCandidateIds,
                    onLikeClick = { postId -> userRepository.togglePostLike(postId) },
                    onCommentClick = { post -> activePostForComments = post },
                    onShareClick = { post -> sharingReelForChat = post },
                    onAuthorClick = { authorId, authorName, photo, gender, location ->
                        navigateToAuthorProfile(authorId, authorName, photo, gender, location)
                    },
                    onFollowAuthorClick = { authorId -> userRepository.toggleFollowCandidate(authorId) },
                    onChatClick = { authorId ->
                        val found = userRepository.findCandidateByIdOrName(
                            authorId = authorId,
                            authorName = "",
                            fallbackPhotoPath = null,
                            fallbackGender = null,
                            fallbackLocation = null
                        )
                        if (found != null) {
                            chatTargetCandidate = found
                        }
                    },
                    onMusicClick = {
                        showMusicPickerModal = true
                    }
                )
            }
        } else {
            // 2. POSTLAR: Rasm va matnli lentalar (Feed)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp)
            ) {
                if (photoPostsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = VelvetRosePrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Hozircha foto postlar mavjud emas",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFCBD5E1) else TextPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(photoPostsList, key = { it.id }) { post ->
                            PostFeedItem(
                                post = post,
                                isDark = isDark,
                                onLikeClick = { userRepository.togglePostLike(post.id) },
                                onCommentClick = { activePostForComments = post },
                                onAuthorClick = {
                                    navigateToAuthorProfile(
                                        post.authorId,
                                        post.authorName,
                                        post.authorPhotoPath,
                                        post.authorGender,
                                        post.authorLocation
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Yuqori minimalist Tab & Tugmalar
        if (selectedTab == 0) {
            // REELS UCHUN: Shaffof, nozik, zamonaviy Instagram uslubidagi floating panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chap tomonda: Nozik matnli Tablar (Reels va Postlar)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Reels",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(VelvetRosePrimary, CircleShape)
                            )
                        }

                        Text(
                            text = "Postlar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 4.dp)
                        )
                    }

                    // O'ng tomonda: Admin paneli (agar admin bo'lsa) va yangi post/reels qo'shish tugmasi (+)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeUser.isAdminUser) {
                            Surface(
                                onClick = { showAdminModerationDialog = true },
                                shape = CircleShape,
                                color = Color(0xFF0F172A).copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, VelvetRosePrimary),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("btn_admin_moderation_reels")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Moderatsiya paneli",
                                        tint = VelvetRosePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = { attemptOpenCreatePost() },
                            shape = CircleShape,
                            color = VelvetRosePrimary,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_create_post")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Reels joylash",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // POSTLAR (LENTA) UCHUN: Chiroyli, zamonaviy sarlavha paneli
            Surface(
                color = if (isDark) DeepNavyMesh.copy(alpha = 0.95f) else Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Reels",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 4.dp)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Postlar",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(VelvetRosePrimary, CircleShape)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeUser.isAdminUser) {
                            IconButton(
                                onClick = { showAdminModerationDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(VelvetRosePrimary.copy(alpha = 0.12f), CircleShape)
                                    .testTag("btn_admin_moderation_posts")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Admin paneli",
                                    tint = VelvetRosePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { attemptOpenCreatePost() },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_create_post")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Joylash", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Komentariyalar (Izohlar) pastki oynasi (BottomSheet)
        if (activePostForComments != null) {
            val livePost = posts.find { it.id == activePostForComments!!.id } ?: activePostForComments!!
            CommentsBottomSheet(
                post = livePost,
                currentUser = currentUser,
                isDark = isDark,
                onDismiss = { activePostForComments = null },
                onAddComment = { text ->
                    userRepository.addCommentToPost(livePost.id, text)
                },
                onAddReply = { commentId, replyToUserName, text ->
                    userRepository.addReplyToComment(livePost.id, commentId, replyToUserName, text)
                },
                onLikeComment = { commentId ->
                    userRepository.toggleCommentLike(livePost.id, commentId)
                },
                onLikeReply = { commentId, replyId ->
                    userRepository.toggleReplyLike(livePost.id, commentId, replyId)
                },
                onUserClick = { authorId, authorName, photo, gender, location ->
                    activePostForComments = null
                    navigateToAuthorProfile(authorId, authorName, photo, gender, location)
                }
            )
        }

        // Post / Reels yaratish dialogi (Katta OOM-xavfsiz yuklagich bilan)
        if (showCreatePostDialog) {
            val userToUse = currentUser ?: userRepository.getOrCreateActiveUser()
            CreatePostDialog(
                currentUser = userToUse,
                userRepository = userRepository,
                isDark = isDark,
                onDismiss = { showCreatePostDialog = false },
                onSubmit = { title, mediaType, mediaUri, musicTitle, musicArtist, musicAudioUrl ->
                    userRepository.createNewPost(
                        title = title,
                        mediaType = mediaType,
                        mediaUri = mediaUri,
                        musicTitle = musicTitle,
                        musicArtist = musicArtist,
                        musicAudioUrl = musicAudioUrl
                    )
                    showCreatePostDialog = false
                }
            )
        }

        // Cheklangan foydalanuvchi ogohlantirish dialogi
        if (showRestrictedWarningDialog) {
            AlertDialog(
                onDismissRequest = { showRestrictedWarningDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Vaqtinchalik Cheklov!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Siz yuklagan kontent xavfsizlik qoidalariga shubhali deb topildi va admin tekshiruviga yuborildi. Tekshiruv tugaguncha yangi video yoki post yuklay olmaysiz.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showRestrictedWarningDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tushundim", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Spam cheklovi ogohlantirish dialogi (Video yuklay olmaydi, efir qila oladi)
        if (showSpamWarningDialog) {
            AlertDialog(
                onDismissRequest = { showSpamWarningDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Video Yuklash Cheklangan (Spam)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Profilingizga yangi video yuklash bo'yicha cheklov o'rnatilgan. Siz ilovadan chetlatilmadingiz — efir qilish (Live), suhbatlashish va boshqa barcha imkoniyatlardan to'liq foydalanishingiz mumkin!",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showSpamWarningDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tushundim", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Admin Moderatsiya va Telegram Bot boshqaruv markazi dialogi
        if (showAdminModerationDialog) {
            AdminModerationDialog(
                userRepository = userRepository,
                onDismiss = { showAdminModerationDialog = false }
            )
        }

        // Musiqa tanlash oynasi (Global Music Library)
        if (showMusicPickerModal) {
            MusicPickerBottomSheet(
                onDismiss = { showMusicPickerModal = false },
                onTrackSelected = { track ->
                    showMusicPickerModal = false
                }
            )
        }

        // Reels videoni nomzodlar chatiga ulashish (Instagram Direct share)
        if (sharingReelForChat != null) {
            ShareReelToChatBottomSheet(
                reel = sharingReelForChat!!,
                userRepository = userRepository,
                isSubscribed = isSubscribed,
                isDark = isDark,
                onDismiss = { sharingReelForChat = null },
                onRequireSubscription = {
                    sharingReelForChat = null
                    Toast.makeText(context, "VIP obuna talab qilinadi! 👑", Toast.LENGTH_SHORT).show()
                },
                onOpenChatWithCandidate = { candidate ->
                    sharingReelForChat = null
                    chatTargetCandidate = candidate
                }
            )
        }

        // Nomzod bilan to'g'ridan-to'g'ri VIP Chat dialogi
        if (chatTargetCandidate != null) {
            CandidateChatDialog(
                candidate = chatTargetCandidate!!,
                userRepository = userRepository,
                isDark = isDark,
                onDismiss = { chatTargetCandidate = null }
            )
        }
    }
}

/**
 * REELS: Instagram/TikTok kabi to'liq vertikal ekranli Pager pleyer
 */
@Composable
fun ReelsVerticalPagerView(
    reels: List<UserPost>,
    followedCandidateIds: Set<String>,
    onLikeClick: (String) -> Unit,
    onCommentClick: (UserPost) -> Unit,
    onShareClick: (UserPost) -> Unit,
    onAuthorClick: (authorId: String, authorName: String, photo: String?, gender: String?, location: String?) -> Unit,
    onFollowAuthorClick: (String) -> Unit,
    onChatClick: (authorId: String) -> Unit,
    onMusicClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { reels.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val reel = reels[page]
        val isAuthorFollowed = followedCandidateIds.contains(reel.authorId)
        
        SingleReelPlayerItem(
            reel = reel,
            isAuthorFollowed = isAuthorFollowed,
            onLikeClick = { onLikeClick(reel.id) },
            onCommentClick = { onCommentClick(reel) },
            onShareClick = { onShareClick(reel) },
            onAuthorClick = {
                onAuthorClick(
                    reel.authorId,
                    reel.authorName,
                    reel.authorPhotoPath,
                    reel.authorGender,
                    reel.authorLocation
                )
            },
            onFollowAuthorClick = { onFollowAuthorClick(reel.authorId) },
            onChatClick = { onChatClick(reel.authorId) },
            onMusicClick = onMusicClick
        )
    }
}

/**
 * Bitta to'liq ekranli Reels elementi
 */
@Composable
fun SingleReelPlayerItem(
    reel: UserPost,
    isAuthorFollowed: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onFollowAuthorClick: () -> Unit,
    onChatClick: () -> Unit,
    onMusicClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var is2xSpeed by remember { mutableStateOf(false) }
    var currentProgressSec by remember { mutableFloatStateOf(0f) }
    var totalDurationSec by remember { mutableFloatStateOf(30f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgressSec by remember { mutableFloatStateOf(0f) }
    var seekTargetSec by remember { mutableStateOf<Float?>(null) }
    val context = LocalContext.current

    val startTime = remember { System.currentTimeMillis() }
    DisposableEffect(reel.id) {
        onDispose {
            val endTime = System.currentTimeMillis()
            val watchDurationMs = endTime - startTime
            val totalDurationMs = (totalDurationSec * 1000).toLong()
            
            com.example.service.recs.ReelsRecommendationEngine.trackEngagement(
                com.example.service.recs.UserEngagement(
                    userId = "active_user",
                    contentId = reel.id,
                    watchDurationMs = watchDurationMs,
                    totalDurationMs = totalDurationMs,
                    isLiked = reel.isLiked,
                    isShared = false,
                    isSkipped = watchDurationMs < 3000L
                )
            )
        }
    }

    // Heart burst animation
    val heartScale = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Musiqa diski aylanuvchi animatsiyasi
    val infiniteTransition = rememberInfiniteTransition(label = "VinylRotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!reel.isLiked) {
                            onLikeClick()
                        }
                        showHeartBurst = true
                        coroutineScope.launch {
                            heartScale.snapTo(0f)
                            heartScale.animateTo(
                                targetValue = 1.3f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                            delay(200)
                            heartScale.animateTo(0f, tween(200))
                            showHeartBurst = false
                        }
                    },
                    onTap = {
                        isPlaying = !isPlaying
                    }
                )
            }
    ) {
        // 1. Asosiy Haqiqiy Video / Media Fon (Vertikal to'liq ekran)
        if (reel.mediaUri.isNotBlank()) {
            ReelVideoPlayerView(
                videoUriString = reel.mediaUri,
                isPlaying = isPlaying,
                is2xSpeed = is2xSpeed,
                seekToSec = seekTargetSec,
                backgroundAudioUri = reel.musicAudioUrl,
                onDurationKnown = { dur ->
                    if (dur > 0) totalDurationSec = dur
                },
                onProgressUpdate = { prog ->
                    if (!isScrubbing) {
                        currentProgressSec = prog
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Zamonaviy romantik videolavha gradient foni
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                VelvetRoseDark,
                                Color(0xFF2E0854),
                                Color(0xFF0F172A),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = VelvetRosePrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Visol.uz Sevgi & Oila 🌸",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To'liq vertikal Reels lavhasi",
                        color = WarmGoldLight,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Qoraytirilgan gradient (Pastki matnlar o'qilishi uchun)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Ijro / Pauza indikatori
        if (!isPlaying) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(70.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Ijro",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }

        // Heart Burst Animation on Double Tap
        if (showHeartBurst) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = VelvetRosePrimary,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(heartScale.value)
                )
            }
        }

        // 2. O'NG TOMONDAGI HARAKATLAR USTUNI (Kichraytirilgan, video kengroq ko'rinishi uchun)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Muallif avatari (+ Profilga o'tish)
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.clickable { onAuthorClick() }
            ) {
                ProfileAvatar(
                    photoPath = reel.authorPhotoPath,
                    gender = reel.authorGender,
                    size = 40.dp,
                    showCameraBadge = false
                )
                Surface(
                    shape = CircleShape,
                    color = VelvetRosePrimary,
                    modifier = Modifier
                        .offset(y = 4.dp)
                        .size(15.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Follow",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Like tugmasi
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        onLikeClick()
                        if (!reel.isLiked) {
                            showHeartBurst = true
                            coroutineScope.launch {
                                heartScale.snapTo(0f)
                                heartScale.animateTo(
                                    targetValue = 1.2f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                                delay(180)
                                heartScale.animateTo(0f, tween(150))
                                showHeartBurst = false
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (reel.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (reel.isLiked) VelvetRosePrimary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${reel.likesCount}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // Izohlar tugmasi
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Izohlar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "${reel.commentsCount}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // Ulashish (Instagram Direct kabi nomzod chatiga yuborish)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Chatga yuborish",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Ulashish",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Aylanuvchi musiqa vinil diski
            // Musiqa aylanuvchi diski (bosilsa musiqa oynasi ochiladi)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .rotate(rotationAngle)
                    .clickable { onMusicClick() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = VelvetRosePrimary,
                    modifier = Modifier.size(14.dp)
                ) {}
            }
        }

        // 3. PASTKI CHAP TOMON: Muallif ma'lumoti, tavsif va musiqa
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 14.dp, bottom = 24.dp)
        ) {
            // Muallif ismi va manzili (Bosilsa profil ochiladi)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAuthorClick() }
            ) {
                Text(
                    text = reel.authorName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "📍 ${reel.authorLocation}",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                // Obuna tugmasi yoki tasdiq belgisi
                if (isAuthorFollowed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Obuna bo'lgansiz",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Obuna bo'lgansiz",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Surface(
                        onClick = onFollowAuthorClick,
                        shape = RoundedCornerShape(12.dp),
                        color = VelvetRosePrimary
                    ) {
                        Text(
                            text = "Obuna bo'lish",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sarlavha / Caption
            Text(
                text = reel.title,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Musiqa nomi (bosilsa musiqa kutubxonasi ochiladi)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onMusicClick() }
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = WarmGoldLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (!reel.musicTitle.isNullOrBlank()) "${reel.musicTitle}${if (!reel.musicArtist.isNullOrBlank()) " • ${reel.musicArtist}" else ""} 🎵" else "Visol.uz • Milliy Sevgi Ohanglari 🎵",
                    color = WarmGoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 4. O'NG TEPADAGI 2X TEZLIK GESTURE DETECTOR (Touch & Hold for 2x speed)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(0.35f)
                .align(Alignment.TopEnd)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            is2xSpeed = true
                            tryAwaitRelease()
                            is2xSpeed = false
                        }
                    )
                }
        )

        // 2X Tezlik Glassmorphic suzuvchi indikator
        AnimatedVisibility(
            visible = is2xSpeed,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.5.dp, VelvetRosePrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = VelvetRosePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "2x Tezlik ⏩",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 5. PASTKI NOZIK SEEKBAR VA HARAKATLANUVCHI REAL-TIME VIDEO PREVIEW OYNACHASI
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        ) {
            val totalWidth = maxWidth
            val progressFraction = if (totalDurationSec > 0f) {
                ((if (isScrubbing) scrubProgressSec else currentProgressSec) / totalDurationSec).coerceIn(0f, 1f)
            } else 0f
            val previewBoxWidth = 88.dp
            val previewOffset = ((totalWidth - previewBoxWidth) * progressFraction).coerceIn(4.dp, (totalWidth - previewBoxWidth - 4.dp).coerceAtLeast(4.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                // Ushlab surilganda barmoq bilan birga gorizontal harakatlanuvchi kichik real-time video ekrancha
                if (isScrubbing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.92f),
                            border = BorderStroke(1.5.dp, VelvetRosePrimary),
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .offset(x = previewOffset)
                                .width(previewBoxWidth)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (reel.mediaUri.isNotBlank()) {
                                        ReelVideoPlayerView(
                                            videoUriString = reel.mediaUri,
                                            isPlaying = false,
                                            is2xSpeed = false,
                                            isMuted = true,
                                            seekToSec = scrubProgressSec,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                val displayTime = scrubProgressSec.toInt()
                                val totalTime = totalDurationSec.toInt()
                                Text(
                                    text = String.format("%02d:%02d / %02d:%02d", displayTime / 60, displayTime % 60, totalTime / 60, totalTime % 60),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Nozik, ixcham Video SeekBar Slider
                Slider(
                    value = if (isScrubbing) scrubProgressSec else currentProgressSec,
                    onValueChange = {
                        isScrubbing = true
                        scrubProgressSec = it
                        seekTargetSec = it // Real-time kadr o'zgarishi
                    },
                    onValueChangeFinished = {
                        currentProgressSec = scrubProgressSec
                        seekTargetSec = scrubProgressSec
                        isScrubbing = false
                    },
                    valueRange = 0f..totalDurationSec.coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = VelvetRosePrimary,
                        activeTrackColor = VelvetRosePrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                )
            }
        }
    }
}

/**
 * Oddiy Rasm Posti (Lenta ko'rinishida)
 */
@Composable
fun PostFeedItem(
    post: UserPost,
    isDark: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Yuqori qism: Muallif avatari, ismi, manzili
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAuthorClick() }
                ) {
                    ProfileAvatar(
                        photoPath = post.authorPhotoPath,
                        gender = post.authorGender,
                        size = 42.dp,
                        showCameraBadge = false
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                        )
                        Text(
                            text = "${post.authorLocation} • ${post.timestamp}",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else TextSecondary
                        )
                    }
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Batafsil",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            // Post matni
            if (post.title.isNotBlank()) {
                Text(
                    text = post.title,
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFE2E8F0) else TextPrimary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Post Rasmi yoki Videosi
            val isVideoPost = post.mediaType.equals("REELS", ignoreCase = true) || post.mediaType.equals("VIDEO", ignoreCase = true)
            if (isVideoPost && post.mediaUri.isNotBlank()) {
                var isFeedVideoPlaying by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFeedVideoPlaying) {
                        ReelVideoPlayerView(
                            videoUriString = post.mediaUri,
                            isPlaying = true,
                            is2xSpeed = false,
                            seekToSec = null,
                            backgroundAudioUri = post.musicAudioUrl,
                            onDurationKnown = {},
                            onProgressUpdate = {},
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { isFeedVideoPlaying = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                                Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White, modifier = Modifier.padding(4.dp))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                                    )
                                )
                                .clickable { isFeedVideoPlaying = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = VelvetRosePrimary.copy(alpha = 0.92f),
                                    modifier = Modifier.size(60.dp),
                                    shadowElevation = 8.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Ijro",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Videoni tomosha qilish 🎬",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else if (post.mediaUri.isNotBlank()) {
                AsyncImage(
                    model = post.mediaUri,
                    contentDescription = "Post Media",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isDark) Color(0xFF0F172A) else Color(0xFFFFF1F2),
                                    if (isDark) Color(0xFF334155) else Color(0xFFFFE4E6)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = VelvetRosePrimary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Visol.uz Baxt & Do'stlik 🌸",
                            color = VelvetRoseDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Pastki amallar: Like, Izohlar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onLikeClick,
                        shape = RoundedCornerShape(12.dp),
                        color = if (post.isLiked) Color(0xFFFFE4E6) else if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (post.isLiked) VelvetRosePrimary else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${post.likesCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (post.isLiked) VelvetRosePrimary else if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        onClick = onCommentClick,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Izohlar",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${post.commentsCount} izoh",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Text(
                    text = "Visol.uz",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelvetRosePrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 💬 REAL-TIME NESTED KOMENTARIYALAR VA BIR KOMENTGA JAVOB YOZISH OYNASI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: UserPost,
    currentUser: UserProfile?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onAddReply: (commentId: String, replyToUserName: String, text: String) -> Unit,
    onLikeComment: (String) -> Unit,
    onLikeReply: (commentId: String, replyId: String) -> Unit,
    onUserClick: (authorId: String, authorName: String, photo: String?, gender: String?, location: String?) -> Unit
) {
    var commentInputText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<CommentItem?>(null) }
    var replyingToTargetUser by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) DeepNavyMesh else Color.White,
        contentColor = if (isDark) Color(0xFFF8FAFC) else TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Sarlavha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Izohlar (${post.commentsCount})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Yopish",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))

            // Izohlar va javoblar ro'yxati
            if (post.comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hozircha hech qanday izoh yo'q",
                            color = if (isDark) Color(0xFF94A3B8) else TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Birinchi bo'lib fikringizni bildiring!",
                            color = VelvetRosePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(post.comments, key = { it.id }) { comment ->
                        CommentItemRow(
                            comment = comment,
                            isDark = isDark,
                            onReplyClick = {
                                replyingToComment = comment
                                replyingToTargetUser = comment.authorName
                            },
                            onReplyToReplyClick = { targetUser ->
                                replyingToComment = comment
                                replyingToTargetUser = targetUser
                            },
                            onLikeClick = { onLikeComment(comment.id) },
                            onLikeReplyClick = { replyId -> onLikeReply(comment.id, replyId) },
                            onUserClick = { authorId, authorName, photo, gender, location ->
                                onUserClick(authorId, authorName, photo, gender, location)
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))

            // Agar biror komentga javob yozilayotgan bo'lsa indikator
            if (replyingToComment != null) {
                Surface(
                    color = if (isDark) Color(0x33E11D48) else VelvetRoseSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = VelvetRosePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${replyingToTargetUser}ga javob yozilmoqda...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFCA5A5) else VelvetRoseDark
                            )
                        }
                        IconButton(
                            onClick = {
                                replyingToComment = null
                                replyingToTargetUser = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Bekor qilish", tint = VelvetRoseDark)
                        }
                    }
                }
            }

            // Izoh kiritish maydoni (Pastda)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    photoPath = currentUser?.photoPath ?: "",
                    gender = currentUser?.gender ?: "Erkak",
                    size = 36.dp,
                    showCameraBadge = false
                )
                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = commentInputText,
                    onValueChange = { commentInputText = it },
                    placeholder = {
                        Text(
                            text = if (replyingToComment != null) "${replyingToTargetUser}ga javob..." else "Izoh yozing...",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentInputText.isNotBlank()) {
                                if (replyingToComment != null) {
                                    onAddReply(
                                        replyingToComment!!.id,
                                        replyingToTargetUser,
                                        commentInputText.trim()
                                    )
                                    replyingToComment = null
                                    replyingToTargetUser = ""
                                } else {
                                    onAddComment(commentInputText.trim())
                                }
                                commentInputText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelvetRosePrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.Transparent,
                        unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.Transparent,
                        focusedTextColor = if (isDark) Color(0xFFF8FAFC) else TextPrimary,
                        unfocusedTextColor = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (commentInputText.isNotBlank()) {
                            if (replyingToComment != null) {
                                onAddReply(
                                    replyingToComment!!.id,
                                    replyingToTargetUser,
                                    commentInputText.trim()
                                )
                                replyingToComment = null
                                replyingToTargetUser = ""
                            } else {
                                onAddComment(commentInputText.trim())
                            }
                            commentInputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(VelvetRosePrimary, CircleShape)
                        .testTag("send_comment_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Yuborish",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bitta Izoh va uning ostidagi javoblar (Threaded replies) komponenti
 */
@Composable
fun CommentItemRow(
    comment: CommentItem,
    isDark: Boolean,
    onReplyClick: () -> Unit,
    onReplyToReplyClick: (String) -> Unit,
    onLikeClick: () -> Unit,
    onLikeReplyClick: (String) -> Unit,
    onUserClick: (authorId: String, authorName: String, photo: String?, gender: String?, location: String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Asosiy izoh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.clickable {
                        onUserClick(
                            comment.authorId,
                            comment.authorName,
                            comment.authorPhotoPath,
                            comment.authorGender,
                            "Toshkent shahri"
                        )
                    }
                ) {
                    ProfileAvatar(
                        photoPath = comment.authorPhotoPath,
                        gender = comment.authorGender,
                        size = 36.dp,
                        showCameraBadge = false
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            onUserClick(
                                comment.authorId,
                                comment.authorName,
                                comment.authorPhotoPath,
                                comment.authorGender,
                                "Toshkent shahri"
                            )
                        }
                    ) {
                        Text(
                            text = comment.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = comment.timestamp,
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF94A3B8) else TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = comment.text,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else TextPrimary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Javob yozish tugmasi
                    Text(
                        text = "Javob yozish ↩",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetRosePrimary,
                        modifier = Modifier
                            .clickable { onReplyClick() }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            // Izoh Like tugmasi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (comment.isLiked) VelvetRosePrimary else if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (comment.likesCount > 0) {
                    Text(
                        text = "${comment.likesCount}",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }

        // Agar ushbu izohga javoblar (replies) bo'lsa, ichkariga kaskad (indent) bilan chiqariladi
        if (comment.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comment.replies.forEach { reply ->
                    ReplyItemRow(
                        reply = reply,
                        isDark = isDark,
                        onReplyClick = { onReplyToReplyClick(reply.authorName) },
                        onLikeClick = { onLikeReplyClick(reply.id) },
                        onUserClick = { authorId, authorName, photo, gender, location ->
                            onUserClick(authorId, authorName, photo, gender, location)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Izohga qaytarilgan javob (Reply) qatori
 */
@Composable
fun ReplyItemRow(
    reply: CommentReply,
    isDark: Boolean,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit,
    onUserClick: (authorId: String, authorName: String, photo: String?, gender: String?, location: String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.clickable {
                    onUserClick(
                        reply.authorId,
                        reply.authorName,
                        reply.authorPhotoPath,
                        reply.authorGender,
                        "Toshkent shahri"
                    )
                }
            ) {
                ProfileAvatar(
                    photoPath = reply.authorPhotoPath,
                    gender = reply.authorGender,
                    size = 28.dp,
                    showCameraBadge = false
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        onUserClick(
                            reply.authorId,
                            reply.authorName,
                            reply.authorPhotoPath,
                            reply.authorGender,
                            "Toshkent shahri"
                        )
                    }
                ) {
                    Text(
                        text = reply.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reply.timestamp,
                        fontSize = 9.sp,
                        color = if (isDark) Color(0xFF94A3B8) else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                // @Kimga javob ekani
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${reply.replyToUserName} ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = VelvetRosePrimary,
                        modifier = Modifier.clickable {
                            onUserClick("", reply.replyToUserName, null, null, null)
                        }
                    )
                    Text(
                        text = reply.text,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else TextPrimary,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Javob yozish ↩",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelvetRosePrimary,
                    modifier = Modifier
                        .clickable { onReplyClick() }
                        .padding(vertical = 2.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 6.dp)
        ) {
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (reply.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (reply.isLiked) VelvetRosePrimary else if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }
            if (reply.likesCount > 0) {
                Text(
                    text = "${reply.likesCount}",
                    fontSize = 9.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) VelvetRosePrimary else Color.Transparent
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Yangi Post yoki Reels video qo'shish Dialog oynasi
 * OOM xavfsiz video siqish va visual yuklash progress paneli bilan boyitilgan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    currentUser: UserProfile,
    userRepository: UserRepository,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, mediaType: String, mediaUri: String, musicTitle: String?, musicArtist: String?, musicAudioUrl: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var postText by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf("REELS") } // "REELS" or "PHOTO"
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var showMontageModal by remember { mutableStateOf(false) }
    var showMusicPickerForPost by remember { mutableStateOf(false) }
    var selectedMusicTrack by remember { mutableStateOf<MusicTrack?>(null) }

    // Dual-file Pipeline State variables
    var rawInputVideoUri by remember { mutableStateOf<Uri?>(null) }
    var editedVideoOutputFile by remember { mutableStateOf<File?>(null) }
    var detectedVideoDurationSec by remember { mutableFloatStateOf(15f) }

    // Dynamic Video Duration Listener
    LaunchedEffect(rawInputVideoUri) {
        if (rawInputVideoUri != null) {
            try {
                val mp = MediaPlayer.create(context, rawInputVideoUri)
                if (mp != null) {
                    val durMs = mp.duration
                    if (durMs > 0) {
                        detectedVideoDurationSec = durMs / 1000f
                    }
                    mp.release()
                }
            } catch (e: Exception) {
                android.util.Log.e("ReelsAndPostsScreen", "Error getting video duration: ${e.message}")
            }
        }
    }

    // Upload state & progress
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStageText by remember { mutableStateOf("") }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    rawInputVideoUri = uri
                    selectedMediaUri = uri
                    editedVideoOutputFile = null // Reset output on new pick
                    Toast.makeText(context, "Fayl muvaffaqiyatli tanlandi ✓", Toast.LENGTH_SHORT).show()
                    if (selectedMediaType == "REELS") {
                        showMontageModal = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Faylni yuklashda xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isUploading) {
                if (showMontageModal) {
                    showMontageModal = false
                } else if (showMusicPickerForPost) {
                    showMusicPickerForPost = false
                } else {
                    onDismiss()
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (showMontageModal) {
                VideoMontageEditorModal(
                    videoUri = rawInputVideoUri,
                    initialTitle = postText,
                    isDark = isDark,
                    onDismiss = { showMontageModal = false },
                    onMontageComplete = { finalTitle, finalUri ->
                        showMontageModal = false
                        postText = finalTitle
                        val parsedUri = Uri.parse(finalUri)
                        selectedMediaUri = parsedUri
                        if (parsedUri.scheme == "file") {
                            editedVideoOutputFile = File(parsedUri.path ?: "")
                        }
                        Toast.makeText(context, "Montaj muvaffaqiyatli yakunlandi! ✨", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DeepNavyMesh else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 20.dp)
                ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Sarlavha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0x33E11D48) else VelvetRoseSoft,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = VelvetRosePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Yangi post joylash",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                            )
                            Text(
                                text = "Reels video yoki rasm ulashing",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else TextSecondary
                            )
                        }
                    }

                    if (!isUploading) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Yopish",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Post turi tanlash (Reels vs Rasm)
                Text(
                    text = "Post turi:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { if (!isUploading) selectedMediaType = "REELS" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMediaType == "REELS") {
                            if (isDark) Color(0x33E11D48) else VelvetRoseSoft
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        },
                        border = if (selectedMediaType == "REELS") BorderStroke(1.5.dp, VelvetRosePrimary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VideoCall,
                                contentDescription = null,
                                tint = if (selectedMediaType == "REELS") VelvetRosePrimary else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎬 Reels Video",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMediaType == "REELS") VelvetRosePrimary else if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        onClick = { if (!isUploading) selectedMediaType = "PHOTO" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMediaType == "PHOTO") {
                            if (isDark) Color(0x33E11D48) else VelvetRoseSoft
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        },
                        border = if (selectedMediaType == "PHOTO") BorderStroke(1.5.dp, VelvetRosePrimary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (selectedMediaType == "PHOTO") VelvetRosePrimary else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📸 Rasm Post",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMediaType == "PHOTO") VelvetRosePrimary else if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Matn kiritish maydoni
                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = {
                        Text(
                            "Fikringiz, tabrik yoki tavsifingizni yozing...",
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    },
                    enabled = !isUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelvetRosePrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.Transparent,
                        unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.Transparent,
                        focusedTextColor = if (isDark) Color(0xFFF8FAFC) else TextPrimary,
                        unfocusedTextColor = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Media Picker & Preview Area (Gallery or Link URL)
                var videoLinkInputText by remember { mutableStateOf("") }
                var showLinkInputMode by remember { mutableStateOf(false) }

                if (selectedMediaUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFF0F172A))
                    ) {
                        if (selectedMediaType == "PHOTO") {
                            AsyncImage(
                                model = selectedMediaUri,
                                contentDescription = "Tanlangan rasm",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.BottomStart)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedMediaType == "REELS") "Video tayyor ✓" else "Rasm tayyor ✓",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Montaj qilish tugmasi (Reels bo'lsa)
                        if (selectedMediaType == "REELS") {
                            Surface(
                                onClick = { showMontageModal = true },
                                shape = RoundedCornerShape(8.dp),
                                color = VelvetRosePrimary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ContentCut,
                                        contentDescription = "Montaj",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Montaj ✂️",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // O'chirish / Almashtirish tugmasi
                        IconButton(
                            onClick = { selectedMediaUri = null },
                            modifier = Modifier
                                .padding(6.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Faylni o'chirish",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (!isUploading) {
                                    val intent = if (selectedMediaType == "REELS") {
                                        android.content.Intent(
                                            android.content.Intent.ACTION_PICK,
                                            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                        )
                                    } else {
                                        android.content.Intent(
                                            android.content.Intent.ACTION_PICK,
                                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                        )
                                    }
                                    mediaPickerLauncher.launch(intent)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.5.dp, VelvetRosePrimary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = VelvetRoseSoft,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (selectedMediaType == "REELS") Icons.Default.VideoLibrary else Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = VelvetRosePrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (selectedMediaType == "REELS") "Galereyadan video tanlash" else "Galereyadan rasm tanlash",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else TextPrimary
                                )
                            }
                        }

                        // Tashqi havola (URL / Link) orqali yuklash opsiyasi
                        if (selectedMediaType == "REELS") {
                            if (!showLinkInputMode) {
                                TextButton(
                                    onClick = { showLinkInputMode = true },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = VelvetRosePrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tashqi havola (Instagram / TikTok / YouTube / URL) 🔗", color = VelvetRosePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔗 Video havolasi orqali yuklash",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else TextPrimary
                                        )
                                        IconButton(
                                            onClick = { showLinkInputMode = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = videoLinkInputText,
                                            onValueChange = { videoLinkInputText = it },
                                            placeholder = { Text("https://... yoki video linki", fontSize = 11.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = VelvetRosePrimary,
                                                unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                            )
                                        )
                                        Button(
                                            onClick = {
                                                if (videoLinkInputText.isNotBlank()) {
                                                    val cleanLink = videoLinkInputText.trim()
                                                    if (postText.isBlank()) {
                                                        val handleMatch = Regex("@([a-zA-Z0-9._]+)").find(cleanLink)?.value
                                                        postText = if (handleMatch != null) {
                                                            "Lavha: $handleMatch orqali 🎬 #visol"
                                                        } else {
                                                            "Yangi video lavha ✨ #reels"
                                                        }
                                                    }
                                                    val parsedUri = Uri.parse(cleanLink)
                                                    rawInputVideoUri = parsedUri
                                                    selectedMediaUri = parsedUri
                                                    Toast.makeText(context, "Video havolasi ulandi ✓", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary)
                                        ) {
                                            Text("Ulash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fon Musiqasi tanlash tugmasi (Ixcham va estetik)
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    onClick = { if (!isUploading) showMusicPickerForPost = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedMusicTrack != null) VelvetRoseSoft else if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (selectedMusicTrack != null) VelvetRosePrimary else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = VelvetRosePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedMusicTrack != null) "🎵 ${selectedMusicTrack!!.title} - ${selectedMusicTrack!!.artist}" else "Fon musiqasi qo'shish (ixtiyoriy)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (selectedMusicTrack != null) "${selectedMusicTrack!!.durationSeconds} soniya • Biriktirildi" else "Reels musiqiy to'plamidan tanlash",
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Text(
                            text = if (selectedMusicTrack != null) "O'zgartirish" else "Tanlash",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetRosePrimary
                        )
                    }
                }

                // Upload Progress Bar with percentage and background compression indicator
                if (isUploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uploadStageText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelvetRosePrimary
                            )
                            Text(
                                text = "${(uploadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VelvetRosePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = VelvetRosePrimary,
                            trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )
                    }
                }

                if (isUploading) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xDD0F172A),
                        border = BorderStroke(1.dp, VelvetRosePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uploadStageText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${(uploadProgress * 100).toInt()}%",
                                    color = VelvetRoseLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = VelvetRosePrimary,
                                trackColor = Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Joylash tugmasi
                Button(
                    onClick = {
                        if (postText.trim().isBlank() && selectedMediaUri == null) {
                            Toast.makeText(context, "Iltimos, post matnini yozing yoki fayl tanlang", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            isUploading = true
                            var finalMediaCdnUrl = ""
                            var uploadSuccess = false

                            // 1. AI & NSFW Xavfsizlik Moderatsiya Tekshiruvi (2-Bosqichli)
                            val targetInspectionUri = rawInputVideoUri ?: selectedMediaUri
                            if (targetInspectionUri != null) {
                                uploadStageText = "Xavfsizlik va AI moderatsiya tekshiruvi..."
                                uploadProgress = 0.05f

                                val assessment = ImageModerationService.inspectMediaUri(
                                    context = context,
                                    uri = targetInspectionUri,
                                    isVideo = (selectedMediaType == "REELS")
                                )

                                if (assessment.severity != ModerationSeverity.SAFE) {
                                    // Barcha darajadagi taqiqlangan/shubhali kontentlar:
                                    // 1. Bazaga avtomatik yuklanmaydi (rad etiladi)
                                    // 2. Foydalanuvchiga vaqtinchalik cheklov o'rnatiladi
                                    userRepository.setUserRestricted(
                                        userId = currentUser.id,
                                        isRestricted = true,
                                        reason = assessment.reason
                                    )

                                    // 3. Adminga Telegram Bot orqali dalil va 2 ta inline tugma bilan yuboriladi
                                    val ticket = ModerationTicket(
                                        userId = currentUser.id,
                                        userName = currentUser.fullName,
                                        userUsername = currentUser.displayUsername,
                                        userPhotoPath = currentUser.photoPath,
                                        userGender = currentUser.gender,
                                        userLocation = "${currentUser.viloyat}, ${currentUser.tuman}",
                                        mediaType = selectedMediaType,
                                        mediaUri = targetInspectionUri.toString(),
                                        caption = postText.trim(),
                                        musicTitle = selectedMusicTrack?.title,
                                        musicArtist = selectedMusicTrack?.artist,
                                        musicAudioUrl = selectedMusicTrack?.audioUrl,
                                        severity = assessment.severity,
                                        reason = assessment.reason,
                                        confidence = assessment.confidence,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    userRepository.createModerationTicket(ticket)

                                    // 4. Foydalanuvchiga tushuntirish beriladi
                                    Toast.makeText(
                                        context,
                                        "⚠️ Siz yuklagan kontent xavfsizlik qoidalariga shubhali/noo'rin deb topildi va admin tekshiruviga yuborildi. Tekshiruv tugaguncha video yuklay olmaysiz.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    isUploading = false
                                    onDismiss()
                                    return@launch
                                }
                            }

                            if (selectedMediaUri != null) {
                                if (selectedMediaType == "REELS") {
                                    var fileToUpload = editedVideoOutputFile
                                    
                                    // MANDATORY EXPORT / RENDER CHECK BEFORE UPLOAD:
                                    if (fileToUpload == null || !fileToUpload.exists() || fileToUpload.length() == 0L) {
                                        uploadStageText = "Video tayyorlanmoqda..."
                                        uploadProgress = 0.15f
                                        
                                        val renderedFile = MediaMuxingPipeline.muxAudioVideo(
                                            context = context,
                                            videoUri = rawInputVideoUri ?: selectedMediaUri!!,
                                            audioTrack = selectedMusicTrack,
                                            videoVolume = 1f,
                                            musicVolume = 0.8f,
                                            trimStartSec = 0f,
                                            trimEndSec = detectedVideoDurationSec
                                        )
                                        
                                        fileToUpload = renderedFile
                                        editedVideoOutputFile = renderedFile
                                    }
                                    
                                    if (fileToUpload == null || !fileToUpload.exists() || fileToUpload.length() == 0L) {
                                        Toast.makeText(context, "Video tayyorlashda xatolik yuz berdi", Toast.LENGTH_SHORT).show()
                                        isUploading = false
                                        return@launch
                                    }
                                    
                                    // Perform upload with processed file URI
                                    val processedUri = Uri.fromFile(fileToUpload)
                                    MediaUploadServiceClient.uploadMediaWithProgress(
                                        context = context,
                                        mediaUri = processedUri,
                                        mediaType = selectedMediaType
                                    ).collect { progressState ->
                                        uploadProgress = progressState.progress
                                        uploadStageText = progressState.stageText

                                        if (progressState.error != null) {
                                            Toast.makeText(context, progressState.error, Toast.LENGTH_SHORT).show()
                                            return@collect
                                        }

                                        if (progressState.isCompleted) {
                                            finalMediaCdnUrl = progressState.cdnUrl ?: Uri.fromFile(fileToUpload).toString()
                                            uploadSuccess = true
                                        }
                                    }
                                } else {
                                    // Photo upload
                                    MediaUploadServiceClient.uploadMediaWithProgress(
                                        context = context,
                                        mediaUri = selectedMediaUri!!,
                                        mediaType = selectedMediaType
                                    ).collect { progressState ->
                                        uploadProgress = progressState.progress
                                        uploadStageText = progressState.stageText

                                        if (progressState.error != null) {
                                            Toast.makeText(context, progressState.error, Toast.LENGTH_SHORT).show()
                                            return@collect
                                        }

                                        if (progressState.isCompleted) {
                                            finalMediaCdnUrl = progressState.cdnUrl ?: ""
                                            uploadSuccess = true
                                        }
                                    }
                                }
                            } else {
                                // Text-only posts or default fallback
                                uploadSuccess = true
                            }

                            if (!uploadSuccess) {
                                isUploading = false
                                return@launch
                            }

                            if (selectedMediaType == "REELS" && selectedMusicTrack == null) {
                                val cleanUser = currentUser.fullName.replace(" ", "").lowercase()
                                MusicApiClient.registerOriginalSound(
                                    username = cleanUser,
                                    soundTitle = "Original Sound - @$cleanUser",
                                    audioUrl = finalMediaCdnUrl.ifBlank { "https://prod-1.storage.jamendo.com/?trackid=1886148&format=mp32&quality=high" }
                                )
                            }

                            onSubmit(
                                postText.trim(),
                                selectedMediaType,
                                finalMediaCdnUrl,
                                selectedMusicTrack?.title,
                                selectedMusicTrack?.artist,
                                selectedMusicTrack?.audioUrl
                            )
                            isUploading = false
                            Toast.makeText(context, "Post bulutli serverga joylandi va tarqatildi! ✨", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isUploading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_new_post_btn")
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yuklanmoqda...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Postni e'lon qilish",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

            // In-dialog Music picker overlay
            if (showMusicPickerForPost && !showMontageModal) {
                MusicPickerBottomSheet(
                    onDismiss = { showMusicPickerForPost = false },
                    videoDuration = detectedVideoDurationSec,
                    onTrackSelected = { track ->
                        selectedMusicTrack = track.copy(
                            trimStartSec = 0f,
                            trimLengthSec = track.durationSeconds
                        )
                        showMusicPickerForPost = false
                        Toast.makeText(context, "Musiqa tanlandi: ${track.title} 🎵", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
