package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.UserRepository
import com.example.model.CommentItem
import com.example.model.MusicTrack
import com.example.model.UserPost
import com.example.model.UserProfile
import com.example.ui.components.PhotoCropDialog
import com.example.util.ImageModerationService
import kotlinx.coroutines.launch
import java.io.File
import com.example.model.UserStory
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.SocialMediaHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    user: UserProfile,
    userRepository: UserRepository,
    onLogoutClick: () -> Unit,
    onCandidateClick: ((UserProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val themeMode by userRepository.themeMode.collectAsState()
    val liveFollowingCount by userRepository.userFollowingCount.collectAsState()
    val visitors by userRepository.visitors.collectAsState()
    val isSubscribed by userRepository.isSubscribed.collectAsState()
    val userHighlights by userRepository.userHighlights.collectAsState()
    val userNote by userRepository.userNote.collectAsState()
    val allRepositoryPosts by userRepository.userPosts.collectAsState()
    val allStories by userRepository.userStories.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDashboardModal by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var showCreateHighlightDialog by remember { mutableStateOf(false) }
    var showFindFriendsSheet by remember { mutableStateOf(false) }
    var showFollowersSheet by remember { mutableStateOf(false) }
    var showVisitorsSheet by remember { mutableStateOf(false) }
    var showAdminModerationDialog by remember { mutableStateOf(false) }
    var showFirebaseHubDialog by remember { mutableStateOf(false) }
    var selectedFollowersTab by remember { mutableIntStateOf(0) }
    var selectedPostForDetail by remember { mutableStateOf<UserPost?>(null) }
    var selectedStoryForViewer by remember { mutableStateOf<UserStory?>(null) }

    val currentNote by remember(userNote) { mutableStateOf(userNote.ifBlank { "Inspo needed..." }) }
    var selectedProfileTab by remember { mutableIntStateOf(0) }

    val bgColor = if (isDark) Color(0xFF1E2028) else Color(0xFFFFFFFF)
    val textPrimaryColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val buttonBgColor = if (isDark) Color(0xFF262936) else Color(0xFFF1F5F9)
    val dividerColor = if (isDark) Color(0xFF303444) else Color(0xFFE2E8F0)

    val myUserPosts = remember(allRepositoryPosts, user.id, user.fullName) {
        allRepositoryPosts.filter {
            it.authorId == user.id || it.authorId == "user_me"
        }
    }

    val dynamicViewsCount = remember(myUserPosts.size, visitors.size, user.followersCount) {
        userRepository.getUserTotalViews(user.id)
    }
    val dynamicLikesCount = remember(myUserPosts) {
        userRepository.getUserTotalLikes(user.id)
    }

    val userActiveStories = remember(allStories, user.id) {
        allStories.filter { (it.authorId == user.id || it.authorId == "user_me") && it.isStoryActive() }
    }
    val hasActiveStory = userActiveStories.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { showCreatePostDialog = true },
                modifier = Modifier.size(28.dp).testTag("btn_create_post_top")
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddBox,
                    contentDescription = "Create post",
                    tint = textPrimaryColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showEditProfileDialog = true }
            ) {
                Text(
                    text = user.displayUsername.removePrefix("@"),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
                if (isSubscribed) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "VIP Tasdiqlangan",
                        tint = WarmGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = textPrimaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFE11D48), CircleShape)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = {
                        val nextMode = if (isDark) "LIGHT" else "DARK"
                        userRepository.setThemeMode(nextMode)
                    },
                    modifier = Modifier.size(28.dp).testTag("btn_theme_toggle_profile")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDark) "Yorug rejim" else "Qorongu rejim",
                        tint = if (isDark) Color(0xFFFACC15) else textPrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Threads profili", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AlternateEmail,
                        contentDescription = "Threads",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-2).dp)
                            .size(14.dp)
                            .background(Color(0xFFEF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (user.isAdminUser) {
                    IconButton(
                        onClick = { showAdminModerationDialog = true },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_admin_moderation_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Admin Moderatsiya",
                            tint = VelvetRosePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier.size(28.dp).testTag("btn_profile_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Foydalanuvchi cheklanganligi haqida ogohlantirish banneri
        if (user.isRestricted || user.isBanned) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (user.isBanned) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
                ),
                border = BorderStroke(1.dp, if (user.isBanned) Color(0xFFEF4444) else Color(0xFFF59E0B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (user.isBanned) Icons.Default.Block else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (user.isBanned) Color(0xFFEF4444) else Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (user.isBanned) "🚫 Hisobingiz butunlay bloklangan" else "⚠️ Vaqtinchalik cheklov o'rnatilgan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user.isBanned) Color(0xFF991B1B) else Color(0xFF92400E)
                        )
                        Text(
                            text = if (user.isBanned) {
                                "Qoidabuzarlik tasdiqlangan. Ilovadan to'liq foydalana olmaysiz."
                            } else {
                                "Siz yuklagan kontent xavfsizlik qoidalariga shubhali deb topildi va admin tekshiruviga yuborildi. Tekshiruv tugaguncha video yuklay olmaysiz."
                            },
                            fontSize = 11.sp,
                            color = if (user.isBanned) Color(0xFFB91C1C) else Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. AVATAR + STATS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = buttonBgColor,
                    onClick = { showNoteDialog = true },
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentNote,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .then(
                                if (hasActiveStory) {
                                    Modifier.border(
                                        BorderStroke(
                                            2.5.dp,
                                            Brush.linearGradient(
                                                listOf(SoftRosePink, WarmPeach, WarmHoneyGold)
                                            )
                                        ),
                                        CircleShape
                                    ).padding(3.dp)
                                } else Modifier
                            )
                            .clickable {
                                if (hasActiveStory) {
                                    selectedStoryForViewer = userActiveStories.first()
                                } else {
                                    showCreateStoryDialog = true
                                }
                            }
                    ) {
                        ProfileAvatar(
                            photoPath = user.photoPath,
                            gender = user.gender,
                            size = if (hasActiveStory) 78.dp else 84.dp,
                            showCameraBadge = false
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF0284C7), CircleShape)
                            .border(2.dp, bgColor, CircleShape)
                            .clickable { showCreateStoryDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${myUserPosts.size}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                    Text(text = "posts", fontSize = 13.sp, color = textPrimaryColor)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        selectedFollowersTab = 0
                        showFollowersSheet = true
                    }
                ) {
                    Text(
                        text = "${user.followersCount}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                    Text(text = "followers", fontSize = 13.sp, color = textPrimaryColor)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        selectedFollowersTab = 1
                        showFollowersSheet = true
                    }
                ) {
                    Text(
                        text = "$liveFollowingCount",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                    Text(text = "following", fontSize = 13.sp, color = textPrimaryColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. USER BIO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val clipboardManager = LocalClipboardManager.current

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.fullName.ifBlank { "Visol foydalanuvchisi" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
                if (isSubscribed) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "VIP Tasdiqlangan",
                        tint = WarmGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ID Badge with copy functionality
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = VelvetRosePrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, VelvetRosePrimary.copy(alpha = 0.3f)),
                onClick = {
                    clipboardManager.setText(AnnotatedString(user.formattedId))
                    Toast.makeText(context, "ID nusxalandi: ${user.formattedId}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("btn_copy_user_id")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${user.formattedId}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetRosePrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Nusxalash",
                        tint = VelvetRosePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = buttonBgColor,
                    onClick = { showEditProfileDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AlternateEmail,
                            contentDescription = null,
                            tint = textPrimaryColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = user.displayUsername.removePrefix("@"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = buttonBgColor,
                    onClick = { showEditProfileDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ Add",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (user.bio.isNotBlank()) user.bio else "Photography, lifestyle & reels content creator ✨\nVisol ilovasida oz juftimni qidirmoqdaman ❤️",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = textPrimaryColor
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "📍 ${user.viloyat}, ${user.tuman}",
                fontSize = 12.sp,
                color = textSecondaryColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. DYNAMIC DASHBOARD
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = buttonBgColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showDashboardModal = true }
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Professional dashboard",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                    Text(
                        text = "Batafsil ↗",
                        fontSize = 11.sp,
                        color = VelvetRosePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "↗ $dynamicViewsCount views in last 30 days",
                        fontSize = 12.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "•", fontSize = 10.sp, color = textSecondaryColor)
                    Text(
                        text = "❤️ $dynamicLikesCount layklar",
                        fontSize = 12.sp,
                        color = VelvetRosePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. ACTION BUTTONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showEditProfileDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBgColor,
                    contentColor = textPrimaryColor
                ),
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_edit_profile")
            ) {
                Text(text = "Edit profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    Toast.makeText(context, "Profil havolasi nusxalandi: visol.app/${user.displayUsername.removePrefix("@")}", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBgColor,
                    contentColor = textPrimaryColor
                ),
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text(text = "Share profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = buttonBgColor,
                onClick = { showFindFriendsSheet = true },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_find_friends_icon")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Find friends",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5.5 JUFTIM TOPILDI (MATCH STATUS TOGGLE)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (user.isMatched) Color(0xFF10B981).copy(alpha = 0.12f) else VelvetRosePrimary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, if (user.isMatched) Color(0xFF10B981) else VelvetRosePrimary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (user.isMatched) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (user.isMatched) Color(0xFF10B981) else VelvetRosePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (user.isMatched) "❤️ Juftim topildi!" else "Juftingizni topdingizmi?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor
                        )
                        Text(
                            text = if (user.isMatched) "Profil nomzodlar ro'yxatidan yashirilgan" else "Juftim topildi belgilanganda profil nomzodlar orasida ko'rinmaydi",
                            fontSize = 11.sp,
                            color = textSecondaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val newMatched = !user.isMatched
                        userRepository.setMyPairMatched(newMatched)
                        Toast.makeText(
                            context,
                            if (newMatched) "Tabriklaymiz! ❤️ Juftingiz topilgani belgilandi. Profil nomzodlardan yashirildi." else "Status yangilandi: Qidiruvda",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.isMatched) Color(0xFF10B981) else VelvetRosePrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (user.isMatched) "Bekor qilish" else "Juftim topildi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. STORY HIGHLIGHTS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { showCreateHighlightDialog = true }
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .border(1.dp, dividerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "New", fontSize = 11.sp, color = textPrimaryColor)
            }

            userHighlights.forEach { highlight ->
                HighlightItem(
                    title = highlight.title,
                    imageUrl = highlight.imageUrl,
                    coverUri = highlight.coverUri,
                    textPrimaryColor = textPrimaryColor,
                    onClick = {
                        Toast.makeText(context, "${highlight.title} ochildi ✨", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. NAVIGATION TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(0.5.dp, dividerColor)),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ProfileTabButton(
                icon = Icons.Default.GridOn,
                isSelected = selectedProfileTab == 0,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onClick = { selectedProfileTab = 0 },
                modifier = Modifier.weight(1f)
            )

            ProfileTabButton(
                icon = Icons.Default.VideoLibrary,
                isSelected = selectedProfileTab == 1,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onClick = { selectedProfileTab = 1 },
                modifier = Modifier.weight(1f)
            )

            ProfileTabButton(
                icon = Icons.Default.Repeat,
                isSelected = selectedProfileTab == 2,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onClick = { selectedProfileTab = 2 },
                modifier = Modifier.weight(1f)
            )

            ProfileTabButton(
                icon = Icons.Default.AssignmentInd,
                isSelected = selectedProfileTab == 3,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onClick = { selectedProfileTab = 3 },
                modifier = Modifier.weight(1f)
            )
        }

        // 8. 3-COLUMN GRID
        val displayMedia = when (selectedProfileTab) {
            1 -> myUserPosts.filter { it.mediaType == "REELS" || it.mediaType == "VIDEO" }
            2 -> myUserPosts.take(3)
            3 -> myUserPosts.take(2)
            else -> myUserPosts
        }

        if (displayMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = if (selectedProfileTab == 1) Icons.Outlined.VideoLibrary else Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        tint = textSecondaryColor,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedProfileTab == 1) "Hozircha videolar yuklanmagan" else "Hozircha postlar yoq",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ozingiz yuklagan media kontentlar shu yerda paydo boladi.",
                        fontSize = 12.sp,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showCreatePostDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Video yoki rasm yuklash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val chunkedPosts = displayMedia.chunked(3)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp)
            ) {
                chunkedPosts.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        for (i in 0 until 3) {
                            if (i < rowItems.size) {
                                val post = rowItems[i]
                                val isVideo = post.mediaType == "REELS" || post.mediaType == "VIDEO"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(buttonBgColor)
                                        .clickable { selectedPostForDetail = post }
                                ) {
                                    if (post.mediaUri.isNotBlank()) {
                                        AsyncImage(
                                            model = post.mediaUri,
                                            contentDescription = post.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        ProfileAvatar(
                                            photoPath = user.photoPath,
                                            gender = user.gender,
                                            size = 120.dp,
                                            showCameraBadge = false
                                        )
                                    }

                                    if (isVideo || selectedProfileTab == 1) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Reel",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(18.dp)
                                        )
                                    }

                                    if (!post.musicTitle.isNullOrBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = "Music",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                                .size(14.dp)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showDashboardModal) {
        com.example.ui.components.ProfessionalDashboardDialog(
            user = user,
            userRepository = userRepository,
            onDismiss = { showDashboardModal = false }
        )
    }

    if (showEditProfileDialog) {
        EditProfileModalDialog(
            user = user,
            userRepository = userRepository,
            onDismiss = { showEditProfileDialog = false },
            onSaved = {
                showEditProfileDialog = false
                Toast.makeText(context, "Profil muvaffaqiyatli yangilandi ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showNoteDialog) {
        var tempNote by remember { mutableStateOf(currentNote) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Eslatma (Note) qoldirish", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempNote,
                    onValueChange = { if (it.length <= 60) tempNote = it },
                    label = { Text("Fikringiz...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        userRepository.setUserNote(tempNote)
                        showNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary)
                ) {
                    Text("Saqlash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    if (showCreatePostDialog) {
        CreatePostModalDialog(
            user = user,
            userRepository = userRepository,
            onDismiss = { showCreatePostDialog = false },
            onPostCreated = {
                showCreatePostDialog = false
                Toast.makeText(context, "Post muvaffaqiyatli yuklandi! ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCreateStoryDialog) {
        CreateStoryModalDialog(
            userRepository = userRepository,
            onDismiss = { showCreateStoryDialog = false },
            onStoryCreated = {
                showCreateStoryDialog = false
                Toast.makeText(context, "Istorya (Story) muvaffaqiyatli joylandi! ✨", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCreateHighlightDialog) {
        var highlightTitle by remember { mutableStateOf("") }
        var selectedEmoji by remember { mutableStateOf("💖") }
        var selectedVideoUri by remember { mutableStateOf<String?>(null) }
        var selectedCoverUri by remember { mutableStateOf<String?>(null) }
        val emojis = listOf("💖", "✈️", "☕", "🌸", "🏔️", "✨", "🎵", "🏆")

        val videoPickerLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedVideoUri = uri.toString()
            }
        }

        val coverPickerLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedCoverUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateHighlightDialog = false },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            title = {
                Text(
                    text = "Yangi Aktualniy (Highlight)",
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = highlightTitle,
                        onValueChange = { highlightTitle = it },
                        label = { Text("Aktualniy nomi (masalan: Sayohat ✈️)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Media (Video / Foto) yuklash tugmasi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Video yoki Foto:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimaryColor)
                            Text(
                                text = if (selectedVideoUri != null) "✅ Media biriktirildi" else "Tanlanmagan",
                                fontSize = 11.sp,
                                color = if (selectedVideoUri != null) Color(0xFF10B981) else textSecondaryColor
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tanlash")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ablojka (Muqova rasmi) yuklash tugmasi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Muqova rasmi (Ablojka):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimaryColor)
                            Text(
                                text = if (selectedCoverUri != null) "✅ Ablojka biriktirildi" else "Tanlanmagan",
                                fontSize = 11.sp,
                                color = if (selectedCoverUri != null) Color(0xFF10B981) else textSecondaryColor
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                coverPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ablojka")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Yoki Muqova emojisi:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        emojis.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (selectedEmoji == emoji) VelvetRosePrimary else buttonBgColor,
                                        CircleShape
                                    )
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 15.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = highlightTitle.ifBlank { "Aktualniy $selectedEmoji" }
                        userRepository.addUserHighlight(
                            title = title,
                            emoji = selectedEmoji,
                            imageUrl = selectedCoverUri ?: selectedVideoUri ?: "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=300",
                            videoUri = selectedVideoUri ?: "",
                            coverUri = selectedCoverUri ?: ""
                        )
                        showCreateHighlightDialog = false
                        Toast.makeText(context, "Aktualniy muvaffaqiyatli saqlandi va yuklandi! ✨", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary)
                ) {
                    Text("Saqlash va Yuklash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateHighlightDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    if (selectedPostForDetail != null) {
        PostDetailModalDialog(
            post = selectedPostForDetail!!,
            userRepository = userRepository,
            onDismiss = { selectedPostForDetail = null }
        )
    }

    if (selectedStoryForViewer != null) {
        StoryViewerModalDialog(
            stories = userActiveStories,
            initialIndex = userActiveStories.indexOfFirst { it.id == selectedStoryForViewer?.id }.coerceAtLeast(0),
            onDismiss = { selectedStoryForViewer = null }
        )
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = if (isDark) Color(0xFF18181B) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Sozlamalar va Maxfiylik",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Mavzu rejimi (Theme):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimaryColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        label = "Tizim ⚙️",
                        isSelected = themeMode == "SYSTEM",
                        onClick = { userRepository.setThemeMode("SYSTEM") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        label = "Yorug ☀️",
                        isSelected = themeMode == "LIGHT",
                        onClick = { userRepository.setThemeMode("LIGHT") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        label = "Qorongu 🌙",
                        isSelected = themeMode == "DARK",
                        onClick = { userRepository.setThemeMode("DARK") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = dividerColor)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yopiq hisob (Private)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor
                        )
                        Text(
                            text = "Faqat siz ruxsat bergan foydalanuvchilar postlaringizni koradi",
                            fontSize = 11.sp,
                            color = textSecondaryColor
                        )
                    }
                    Switch(
                        checked = user.isPrivate,
                        onCheckedChange = { isChecked ->
                            userRepository.updateCurrentUserPrivacy(isChecked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VelvetRosePrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    onClick = {
                        showSettingsSheet = false
                        showAdminModerationDialog = true
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = VelvetRosePrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Moderatsiya va Telegram Bot Boshqaruvi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = textSecondaryColor, modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    onClick = {
                        showSettingsSheet = false
                        showFindFriendsSheet = true
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, tint = VelvetRosePrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Kontaktlardan dostlarni topish",
                            fontSize = 14.sp,
                            color = textPrimaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = textSecondaryColor, modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    onClick = {
                        userRepository.deleteAllUserPosts()
                        Toast.makeText(context, "Barcha videolar va postlar ochirildi, xotira boshatildi ✓", Toast.LENGTH_SHORT).show()
                        showSettingsSheet = false
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Barcha yuklangan videolarni ochirish (Xotira tozalash)",
                            fontSize = 14.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = { Text("Akkauntni o'chirish", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
                        text = { Text("Haqiqatdan ham profilingizni va barcha xabarlaringizni butunlay o'chirib tashlamoqchimisiz? Ushbu amalni ortga qaytarib bo'lmaydi!", fontSize = 14.sp) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    showSettingsSheet = false
                                    userRepository.deleteAccount()
                                    Toast.makeText(context, "Akkauntingiz butunlay o'chirildi", Toast.LENGTH_LONG).show()
                                    onLogoutClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Text("Butunlay o'chirish", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("Bekor qilish")
                            }
                        }
                    )
                }

                Surface(
                    onClick = {
                        showDeleteConfirmDialog = true
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Profilni butunlay o'chirish (Account Deletion)",
                            fontSize = 14.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Surface(
                    onClick = {
                        showSettingsSheet = false
                        onLogoutClick()
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Hisobdan chiqish",
                            fontSize = 14.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showFindFriendsSheet) {
        FindFriendsBottomSheet(
            userRepository = userRepository,
            onDismiss = { showFindFriendsSheet = false },
            onCandidateClick = { candidate ->
                showFindFriendsSheet = false
                onCandidateClick?.invoke(candidate)
            }
        )
    }

    if (showFollowersSheet) {
        FollowersAndFollowingBottomSheet(
            userId = user.id,
            initialTab = selectedFollowersTab,
            userRepository = userRepository,
            isDark = isDark,
            onDismiss = { showFollowersSheet = false },
            onSelectUser = { candidate ->
                showFollowersSheet = false
                onCandidateClick?.invoke(candidate)
            }
        )
    }

    if (showVisitorsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVisitorsSheet = false },
            containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = VelvetRosePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profilimni korganlar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textPrimaryColor
                        )
                    }
                    IconButton(onClick = { showVisitorsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = textSecondaryColor)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (visitors.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hozircha hech kim profilingizni kormagan",
                            color = textSecondaryColor,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visitors) { visitor ->
                            Surface(
                                onClick = {
                                    showVisitorsSheet = false
                                    onCandidateClick?.invoke(visitor.visitorProfile)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = buttonBgColor
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileAvatar(
                                        photoPath = visitor.visitorProfile.photoPath,
                                        gender = visitor.visitorProfile.gender,
                                        size = 48.dp,
                                        showCameraBadge = false
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = visitor.visitorProfile.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = textPrimaryColor
                                        )
                                        Text(
                                            text = "🕒 ${visitor.formattedTime}",
                                            fontSize = 12.sp,
                                            color = textSecondaryColor
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = textSecondaryColor
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAdminModerationDialog) {
        AdminModerationDialog(
            userRepository = userRepository,
            onDismiss = { showAdminModerationDialog = false }
        )
    }

    if (showFirebaseHubDialog) {
        Dialog(
            onDismissRequest = { showFirebaseHubDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                color = if (isDark) Color(0xFF13151A) else Color(0xFFF8FAFC)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // TOP HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 Firebase Ecosystem Hub",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor
                        )
                        IconButton(onClick = { showFirebaseHubDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textPrimaryColor)
                        }
                    }

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 12.dp))

                    val coroutineScope = rememberCoroutineScope()
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. MODULE: AUTHENTICATE A NEW USER
                        FirebaseHubCard(
                            title = "1. Authenticate a new user (Firebase Auth)",
                            description = "Monitors live user authentication state and session changes.",
                            codeSnippet = "Auth.auth().addStateDidChangeListener { (auth, user) in\n    if let user = user {\n        let email = user.email\n    }\n}\n\nAuth.auth().signIn(withEmail: email, password: password)",
                            isDark = isDark
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Auth Status: Active 🟢",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = VelvetRosePrimary
                                )
                                Text(
                                    text = "Current User: ${user.fullName}\nID: ${user.id}\nEmail: ${user.fullName.replace(" ", "").lowercase()}@visol.app",
                                    fontSize = 12.sp,
                                    color = textSecondaryColor
                                )
                                Button(
                                    onClick = { Toast.makeText(context, "FirebaseAuth: Seans faol saqlanmoqda!", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary)
                                ) {
                                    Text("Monitor Auth Listener", color = Color.White)
                                }
                            }
                        }

                        // 2. MODULE: READ / WRITE IN REALTIME
                        FirebaseHubCard(
                            title = "2. Read / write in realtime (Firestore & RTDB)",
                            description = "Real-time chat and voice room data synchronizer.",
                            codeSnippet = "let db = Firestore.firestore()\n// Write\ndb.collection(\"users\").document(\"alovelace\").setData([\n    \"firstname\": \"Ada\",\n    \"lastname\": \"Lovelace\"\n])\n// Read / listen\ndb.collection(\"users\").document(\"alovelace\")\n   .addSnapshotListener { ... }",
                            isDark = isDark
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Multiplayer sync bridge: Active 🟢",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Your emulators are connected via: https://visol-uz-realtime-default-rtdb.firebaseio.com/",
                                    fontSize = 12.sp,
                                    color = textSecondaryColor
                                )
                                Button(
                                    onClick = { Toast.makeText(context, "Barcha chat va ovozli xonalar Firebase Realtime Database sinxronizatorida! ⚡", Toast.LENGTH_LONG).show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("Test Real-time Database Trigger", color = Color.White)
                                }
                            }
                        }

                        // 3. MODULE: SUBSCRIBE TO A NOTIFICATION TOPIC
                        var fcmTopic by remember { mutableStateOf("news") }
                        FirebaseHubCard(
                            title = "3. Subscribe user to topic (Firebase Messaging)",
                            description = "Subscribes the device to an FCM topic channel to listen to live notifications.",
                            codeSnippet = "Messaging.messaging().subscribe(toTopic: \"/topics/news\")",
                            isDark = isDark
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = fcmTopic,
                                    onValueChange = { fcmTopic = it },
                                    label = { Text("FCM Topic") },
                                    prefix = { Text("/topics/") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        com.example.network.FirebaseController.subscribeToTopic(context, fcmTopic)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                ) {
                                    Text("Subscribe to Topic 🔔", color = Color.White)
                                }
                            }
                        }

                        // 4. MODULE: LOG A CUSTOM ANALYTICS EVENT
                        FirebaseHubCard(
                            title = "4. Log custom Analytics event (Firebase Analytics)",
                            description = "Saves custom analytic logs and captures user flow metrics.",
                            codeSnippet = "Analytics.logEvent(\"share_image\", parameters: [\"id\" : \"image123\"])",
                            isDark = isDark
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            com.example.network.FirebaseController.logAnalyticsEvent(
                                                context,
                                                "share_image",
                                                mapOf("id" to "image_visol_123", "type" to "avatar")
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                    ) {
                                        Text("Log share_image 📊", color = Color.White, fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            com.example.network.FirebaseController.logAnalyticsEvent(
                                                context,
                                                "custom_user_action",
                                                mapOf("user" to user.fullName, "timestamp" to System.currentTimeMillis())
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280))
                                    ) {
                                        Text("Log user_action 📈", color = Color.White, fontSize = 11.sp)
                                    }
                                }

                                Text(
                                    text = "Logged Events Live View:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = textPrimaryColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                val analyticsLogs = com.example.network.FirebaseController.getLoggedEvents()
                                if (analyticsLogs.isEmpty()) {
                                    Text("No events logged yet. Click above to log!", fontSize = 11.sp, color = textSecondaryColor)
                                } else {
                                    analyticsLogs.takeLast(3).reversed().forEach { logStr ->
                                        Text(
                                            text = logStr,
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                            modifier = Modifier
                                                .background(if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5), RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                                .fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // 5. MODULE: SAVE AN IMAGE TO CLOUD STORAGE
                        var uploadedCloudUrl by remember { mutableStateOf("") }
                        val imagePickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            if (uri != null) {
                                com.example.network.FirebaseController.uploadImageToCloudStorage(context, uri.toString()) { downloadUrl ->
                                    uploadedCloudUrl = downloadUrl
                                }
                            }
                        }

                        FirebaseHubCard(
                            title = "5. Save an image to Cloud Storage (Firebase Storage)",
                            description = "Uploads photos or media files to the secure Firebase cloud store.",
                            codeSnippet = "let storageRef = Storage.storage().reference()\nlet riversRef = storageRef.child(\"image/rivers.jpg\")\nlet uploadTask = riversRef.putData(data, metadata: nil) { ... }",
                            isDark = isDark
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pick & Upload Image ☁️", color = Color.White)
                                }

                                if (uploadedCloudUrl.isNotEmpty()) {
                                    Text(
                                        text = "Cloud Download URL:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = textPrimaryColor
                                    )
                                    Text(
                                        text = uploadedCloudUrl,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color(0xFF0EA5E9),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .background(if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                            .fillMaxWidth()
                                    )
                                    AsyncImage(
                                        model = uploadedCloudUrl,
                                        contentDescription = "Uploaded",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, dividerColor, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
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

@Composable
private fun FirebaseHubCard(
    title: String,
    description: String,
    codeSnippet: String,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2028) else Color(0xFFF1F5F9)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF303444) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (isDark) Color.White else Color.Black)
            Text(description, fontSize = 12.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            
            // Code block matching screenshot style (Swift/Kotlin/JS)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF0D0E12) else Color(0xFFE2E8F0),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF232530) else Color(0xFFCBD5E1))
            ) {
                Text(
                    text = codeSnippet,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (isDark) Color(0xFFF87171) else Color(0xFF991B1B),
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            HorizontalDivider(color = if (isDark) Color(0xFF303444) else Color(0xFFE2E8F0))
            
            content()
        }
    }
}

@Composable
private fun HighlightItem(
    title: String,
    imageUrl: String,
    coverUri: String = "",
    textPrimaryColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val displayData = coverUri.ifBlank { imageUrl.ifBlank { "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=300" } }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFF59E0B), Color(0xFF8B5CF6)))
                    ),
                    CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(displayData)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = textPrimaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileTabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) textPrimaryColor else textSecondaryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(2.dp)
                        .background(textPrimaryColor)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostModalDialog(
    user: UserProfile,
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit
) {
    var postTitle by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf("REELS") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMusicTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var showMusicPicker by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        title = {
            Text(
                text = "Yangi Post yoki Video joylash",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isDark) Color.White else TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMediaType == "REELS",
                        onClick = { selectedMediaType = "REELS" },
                        label = { Text("🎬 Reels Video") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMediaType == "PHOTO",
                        onClick = { selectedMediaType = "PHOTO" },
                        label = { Text("📸 Rasm Post") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedMediaUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                    ) {
                        if (selectedMediaType == "PHOTO") {
                            AsyncImage(
                                model = selectedMediaUri,
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { selectedMediaUri = null },
                            modifier = Modifier
                                .padding(6.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Ochirish", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Surface(
                        onClick = {
                            if (selectedMediaType == "REELS") {
                                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            } else {
                                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.5.dp, VelvetRosePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (selectedMediaType == "REELS") Icons.Default.VideoLibrary else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = VelvetRosePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedMediaType == "REELS") "Video faylni tanlang (MP4, MOV)" else "Rasm faylini tanlang (JPG, PNG)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color.White else TextPrimary
                            )
                            Text(
                                text = "Galereyadan fayl biriktirish",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = postTitle,
                    onValueChange = { postTitle = it },
                    placeholder = { Text("Tavsif, fikr yoki tabrik yozing...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = { showMusicPicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedMusicTrack != null) VelvetRoseSoft else (if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, if (selectedMusicTrack != null) VelvetRosePrimary else Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = VelvetRosePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedMusicTrack != null) "🎵 ${selectedMusicTrack!!.title} - ${selectedMusicTrack!!.artist}" else "Fon musiqasi tanlash (ixtiyoriy)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (selectedMusicTrack != null) "Almashtirish" else "Tanlash",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetRosePrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = postTitle.ifBlank {
                        if (selectedMediaType == "REELS") "Mening yangi videom 🎬" else "Mening yangi suratim 📸"
                    }
                    val mediaUriString = selectedMediaUri?.toString() ?: (
                        if (selectedMediaType == "REELS") "https://assets.mixkit.co/videos/preview/mixkit-girl-in-neon-light-1232-large.mp4"
                        else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800"
                    )

                    userRepository.createNewPost(
                        title = finalTitle,
                        mediaType = selectedMediaType,
                        mediaUri = mediaUriString,
                        musicTitle = selectedMusicTrack?.title,
                        musicArtist = selectedMusicTrack?.artist,
                        musicAudioUrl = selectedMusicTrack?.audioUrl
                    )
                    onPostCreated()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary)
            ) {
                Text("Joylash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )

    if (showMusicPicker) {
        MusicPickerBottomSheet(
            onDismiss = { showMusicPicker = false },
            onTrackSelected = { track ->
                selectedMusicTrack = track
                showMusicPicker = false
                Toast.makeText(context, "${track.title} tanlandi 🎵", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateStoryModalDialog(
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onStoryCreated: () -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    val selectedMediaType by remember { mutableStateOf("PHOTO") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMusicTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var showMusicPicker by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        title = {
            Text(
                text = "Yangi Istorya (Story) joylash",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isDark) Color.White else TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Story 24 soat davomida profilingizda faol bolib, rangdor doira effekti bilan korinadi ✨",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedMediaUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                    ) {
                        AsyncImage(
                            model = selectedMediaUri,
                            contentDescription = "Story preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedMediaUri = null },
                            modifier = Modifier
                                .padding(6.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Ochirish", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Surface(
                        onClick = {
                            mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.5.dp, VelvetRosePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = VelvetRosePrimary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Galereyadan rasm yoki video tanlash",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color.White else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Hikoyangiz uchun matn...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    placeholder = { Text("Havola (URL link): masalan visol.uz") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = { showMusicPicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedMusicTrack != null) VelvetRoseSoft else (if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, if (selectedMusicTrack != null) VelvetRosePrimary else Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = VelvetRosePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedMusicTrack != null) "🎵 ${selectedMusicTrack!!.title}" else "Musiqa qoshish (ixtiyoriy)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (selectedMusicTrack != null) "Ozgartirish" else "Tanlash",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetRosePrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mediaUriString = selectedMediaUri?.toString() ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800"
                    userRepository.addUserStory(
                        mediaUri = mediaUriString,
                        mediaType = selectedMediaType,
                        caption = caption,
                        linkUrl = linkUrl.takeIf { it.isNotBlank() },
                        linkText = if (linkUrl.isNotBlank()) "visol.uz 🔗" else null,
                        musicTitle = selectedMusicTrack?.title,
                        musicArtist = selectedMusicTrack?.artist,
                        musicAudioUrl = selectedMusicTrack?.audioUrl
                    )
                    onStoryCreated()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary)
            ) {
                Text("Ulashish ✨")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )

    if (showMusicPicker) {
        MusicPickerBottomSheet(
            onDismiss = { showMusicPicker = false },
            onTrackSelected = { track ->
                selectedMusicTrack = track
                showMusicPicker = false
                Toast.makeText(context, "${track.title} tanlandi 🎵", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun StoryViewerModalDialog(
    stories: List<UserStory>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.size - 1)) }
    val currentStory = stories.getOrNull(currentIndex) ?: stories.first()
    val context = LocalContext.current

    var storyMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(currentStory.id, currentStory.musicAudioUrl) {
        if (!currentStory.musicAudioUrl.isNullOrBlank()) {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

                val mp = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, Uri.parse(currentStory.musicAudioUrl))
                    isLooping = true
                    setOnErrorListener { _, what, extra ->
                        Log.w("StoryViewer", "MediaPlayer error: $what, $extra")
                        true
                    }
                    setOnPreparedListener { player ->
                        player.seekTo(0)
                        player.start()
                    }
                    prepareAsync()
                }
                storyMediaPlayer = mp
            } catch (e: Exception) {
                Log.e("StoryViewer", "Music error", e)
            }
        }
        onDispose {
            try {
                storyMediaPlayer?.stop()
                storyMediaPlayer?.release()
            } catch (_: Exception) {}
            storyMediaPlayer = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = currentStory.mediaUri.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800" },
                contentDescription = currentStory.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentIndex > 0) {
                                currentIndex--
                            } else {
                                onDismiss()
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentIndex < stories.size - 1) {
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .background(
                                    if (idx <= currentIndex) Color.White else Color.White.copy(alpha = 0.35f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        Brush.linearGradient(
                                            listOf(Color(0xFFE11D48), Color(0xFFF59E0B), Color(0xFF8B5CF6))
                                        )
                                    ),
                                    CircleShape
                                )
                                .padding(2.dp)
                        ) {
                            ProfileAvatar(
                                photoPath = currentStory.authorPhotoPath,
                                gender = currentStory.authorGender,
                                size = 32.dp,
                                showCameraBadge = false
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = currentStory.authorName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val remaining = currentStory.remainingHours()
                            Text(
                                text = "24 soatlik hikoya • ${remaining}s qoldi",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Yopish",
                            tint = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!currentStory.linkUrl.isNullOrBlank()) {
                    Surface(
                        onClick = {
                            Toast.makeText(context, "Havola ochilmoqda: ${currentStory.linkUrl}", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Link",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentStory.linkText ?: "visol.uz 🔗",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                if (!currentStory.musicTitle.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = VelvetRosePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎵 ${currentStory.musicTitle} - ${currentStory.musicArtist ?: ""}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (currentStory.caption.isNotBlank()) {
                    Text(
                        text = currentStory.caption,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PostDetailModalDialog(
    post: UserPost,
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    var isLiked by remember(post.id) { mutableStateOf(post.isLiked) }
    var likesCount by remember(post.id) { mutableIntStateOf(post.likesCount) }
    var commentText by remember { mutableStateOf("") }
    var commentsList by remember(post.id) { mutableStateOf(post.comments) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isMusicPlaying by remember { mutableStateOf(false) }

    DisposableEffect(post.musicAudioUrl) {
        if (!post.musicAudioUrl.isNullOrBlank()) {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

                val mp = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, Uri.parse(post.musicAudioUrl))
                    isLooping = true
                    setOnErrorListener { _, what, extra ->
                        Log.w("PostDetail", "MediaPlayer error: $what, $extra")
                        isMusicPlaying = false
                        true
                    }
                    setOnPreparedListener { player ->
                        player.seekTo(0)
                        player.start()
                        isMusicPlaying = true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                Log.e("PostDetail", "Music error", e)
            }
        }
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDark) Color(0xFF0F172A) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            photoPath = post.authorPhotoPath,
                            gender = post.authorGender,
                            size = 38.dp,
                            showCameraBadge = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = post.authorName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else TextPrimary
                            )
                            Text(
                                text = post.authorLocation.ifBlank { "Toshkent" },
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Yopish",
                            tint = if (isDark) Color.White else TextPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.mediaUri.isNotBlank()) {
                        AsyncImage(
                            model = post.mediaUri,
                            contentDescription = post.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        ProfileAvatar(
                            photoPath = post.authorPhotoPath,
                            gender = post.authorGender,
                            size = 180.dp,
                            showCameraBadge = false
                        )
                    }

                    if (post.mediaType == "REELS" || post.mediaType == "VIDEO") {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                if (!post.musicTitle.isNullOrBlank()) {
                    Surface(
                        color = if (isDark) Color(0xFF1E1B4B) else VelvetRoseSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = VelvetRosePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🎵 ${post.musicTitle} - ${post.musicArtist ?: ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = {
                                    if (mediaPlayer?.isPlaying == true) {
                                        mediaPlayer?.pause()
                                        isMusicPlaying = false
                                    } else {
                                        mediaPlayer?.start()
                                        isMusicPlaying = true
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMusicPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Mute/Unmute",
                                    tint = VelvetRosePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            likesCount = if (isLiked) likesCount + 1 else maxOf(0, likesCount - 1)
                            userRepository.togglePostLike(post.id)
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) VelvetRosePrimary else (if (isDark) Color.White else TextPrimary),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "$likesCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else TextPrimary
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Izohlar",
                        tint = if (isDark) Color.White else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${commentsList.size}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else TextPrimary
                    )
                }

                if (post.title.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${post.authorName} ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) Color.White else TextPrimary
                        )
                        Text(
                            text = post.title,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else TextPrimary
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (commentsList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Birinchi bolib izoh qoldiring! 💬",
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(commentsList) { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                ProfileAvatar(
                                    photoPath = comment.authorPhotoPath,
                                    gender = comment.authorGender,
                                    size = 32.dp,
                                    showCameraBadge = false
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comment.authorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.White else TextPrimary
                                    )
                                    Text(
                                        text = comment.text,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Izoh yozing...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                userRepository.addCommentToPost(post.id, commentText)
                                val newC = CommentItem(
                                    id = "c_${System.currentTimeMillis()}",
                                    postId = post.id,
                                    authorId = "user_me",
                                    authorName = "Men",
                                    authorGender = "Erkak",
                                    authorPhotoPath = "avatar_male_1",
                                    text = commentText.trim(),
                                    timestamp = "Hozirgina",
                                    likesCount = 0,
                                    isLiked = false,
                                    replies = emptyList()
                                )
                                commentsList = commentsList + newC
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(VelvetRosePrimary, CircleShape)
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
}

@Composable
private fun EditProfileModalDialog(
    user: UserProfile,
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf(user.fullName) }
    var username by remember { mutableStateOf(user.username.ifBlank { user.displayUsername.removePrefix("@") }) }
    var phone by remember { mutableStateOf(user.phone) }
    var telegram by remember { mutableStateOf(user.telegram.removePrefix("@")) }
    var instagram by remember { mutableStateOf(user.instagram.removePrefix("@")) }
    var tiktok by remember { mutableStateOf(user.tiktok.removePrefix("@")) }
    var bio by remember { mutableStateOf(user.bio) }
    var currentPhotoPath by remember { mutableStateOf(user.photoPath) }
    
    // Password change state
    var showPasswordChange by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Photo crop state
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cleanUser = username.filter { !it.isWhitespace() }.lowercase().removePrefix("@")
    val isReservedUser = !user.isAdminUser && userRepository.isReservedUsername(cleanUser)
    val isReservedName = !user.isAdminUser && userRepository.isReservedFullName(fullName)
    val isFormatValid = cleanUser.length in 3..30 && 
        cleanUser.all { (it in 'a'..'z') || (it in '0'..'9') || it == '_' } && 
        !username.any { it.isWhitespace() } && !isReservedUser
    val isTaken = isFormatValid && userRepository.isUsernameTaken(cleanUser, excludeUserId = user.id)

    // Gallery launcher with safe downsampling to avoid OOM / crash
    val editAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                val reqSize = 1080
                var sampleSize = 1
                while ((options.outWidth / sampleSize) > reqSize || (options.outHeight / sampleSize) > reqSize) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                if (decodedBitmap != null && !decodedBitmap.isRecycled) {
                    pendingCropBitmap = decodedBitmap
                    showCropDialog = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Rasm yuklashda xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profilni tahrirlash", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar preview & change button
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    ProfileAvatar(
                        photoPath = currentPhotoPath,
                        gender = user.gender,
                        size = 86.dp,
                        showCameraBadge = false
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(VelvetRosePrimary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable {
                                val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                    type = "image/*"
                                }
                                editAvatarLauncher.launch(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Rasmni almashtirish",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                            type = "image/*"
                        }
                        editAvatarLauncher.launch(intent)
                    }
                ) {
                    Text("Rasmni o'zgartirish (Kesish bilan)", fontSize = 12.sp, color = VelvetRosePrimary)
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("To'liq ism") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { input ->
                        val noSpaces = input.filter { !it.isWhitespace() }
                        username = noSpaces.lowercase().removePrefix("@")
                        errorMessage = null
                    },
                    label = { Text("Foydalanuvchi nomi (@username)") },
                    prefix = { Text("@", fontWeight = FontWeight.Bold, color = VelvetRosePrimary) },
                    trailingIcon = {
                        if (isFormatValid && !isTaken) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Available",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    isError = isTaken || (!isFormatValid && cleanUser.isNotBlank()),
                    supportingText = {
                        when {
                            isTaken -> Text("Ushbu @$cleanUser band! Boshqa nom tanlang.", color = MaterialTheme.colorScheme.error)
                            !isFormatValid && cleanUser.isNotBlank() -> Text("Kamida 3 belgi, faqat lotin harflari va _", color = MaterialTheme.colorScheme.error)
                            isFormatValid && !isTaken -> Text("Ushbu username mos keladi ✓", color = Color(0xFF16A34A))
                            else -> null
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_username_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        if (newValue.length < phone.length) {
                            if (newValue.length <= 5) {
                                phone = "+998 "
                            } else {
                                val digits = newValue.filter { it.isDigit() }
                                val local = if (digits.startsWith("998")) digits.drop(3) else digits
                                phone = if (local.isEmpty()) "+998 " else SocialMediaHelper.formatUzbekPhoneNumber(local)
                            }
                        } else {
                            phone = SocialMediaHelper.formatUzbekPhoneNumber(newValue)
                        }
                    },
                    label = { Text("Telefon raqami") },
                    placeholder = { Text("+998 90 123 45 67") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = VelvetRosePrimary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = telegram,
                    onValueChange = { telegram = SocialMediaHelper.cleanUsername(it) },
                    label = { Text("Telegram username") },
                    placeholder = { Text("username") },
                    prefix = { Text("@", fontWeight = FontWeight.Bold, color = VelvetRosePrimary) },
                    supportingText = {
                        if (telegram.isNotBlank()) {
                            Text("Havola: https://t.me/${SocialMediaHelper.cleanUsername(telegram)}", color = VelvetRosePrimary, fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = instagram,
                    onValueChange = { instagram = it.trim().removePrefix("@") },
                    label = { Text("Instagram username") },
                    prefix = { Text("@", fontWeight = FontWeight.Bold, color = VelvetRosePrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tiktok,
                    onValueChange = { tiktok = it.trim().removePrefix("@") },
                    label = { Text("TikTok username") },
                    prefix = { Text("@", fontWeight = FontWeight.Bold, color = VelvetRosePrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio (Ma'lumot)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Parolni o'zgartirish tugmasi va bo'limi
                Surface(
                    onClick = { showPasswordChange = !showPasswordChange },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = VelvetRosePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Parolni almashtirish", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Icon(
                            if (showPasswordChange) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }

                if (showPasswordChange) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Yangi parol") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Parolni tasdiqlang") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.trim().length < 3) {
                        errorMessage = "Ism kamida 3 ta belgidan iborat bo'lishi kerak"
                        return@Button
                    }
                    if (isReservedName) {
                        errorMessage = "Rasmiy tizim nomlaridan foydalanish taqiqlangan!"
                        return@Button
                    }
                    if (isReservedUser) {
                        errorMessage = "Ushbu username rasmiy ma'muriyat tomonidan band qilingan!"
                        return@Button
                    }
                    if (!isFormatValid) {
                        errorMessage = "Username noto'g'ri shaklda yoki probel kiritilgan"
                        return@Button
                    }
                    if (isTaken) {
                        errorMessage = "@$cleanUser username allaqachon band!"
                        return@Button
                    }

                    if (showPasswordChange && newPassword.isNotBlank()) {
                        if (newPassword.length < 4) {
                            errorMessage = "Parol kamida 4 ta belgidan iborat bo'lishi kerak"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            errorMessage = "Kiritilgan parollar bir-biriga mos kelmadi"
                            return@Button
                        }
                        userRepository.changePassword(newPassword)
                        Toast.makeText(context, "Parol muvaffaqiyatli almashtirildi 🔒", Toast.LENGTH_SHORT).show()
                    }

                    val result = userRepository.updateProfileInfo(
                        fullName = fullName,
                        newUsername = cleanUser,
                        bio = bio,
                        newPhotoPath = currentPhotoPath,
                        phone = phone,
                        telegram = telegram,
                        instagram = instagram,
                        tiktok = tiktok
                    )

                    if (result.isSuccess) {
                        Toast.makeText(context, "Profil ma'lumotlari yangilandi ✨", Toast.LENGTH_SHORT).show()
                        onSaved()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Xatolik yuz berdi"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                modifier = Modifier.testTag("btn_save_profile")
            ) {
                Text("Saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )

    if (showCropDialog && pendingCropBitmap != null) {
        PhotoCropDialog(
            sourceBitmap = pendingCropBitmap!!,
            isDark = true,
            onDismiss = {
                showCropDialog = false
                pendingCropBitmap = null
            },
            onCropConfirmed = { croppedBmp ->
                coroutineScope.launch {
                    val moderation = ImageModerationService.checkBitmapSafety(context, croppedBmp)
                    if (!moderation.isApproved) {
                        val msg = moderation.warningMessage ?: "🚫 Xavfsizlik filtri: Suratda noo'rin yoki ochiq-sochiq kontent aniqlandi!"
                        errorMessage = msg
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        showCropDialog = false
                        pendingCropBitmap = null
                    } else {
                        try {
                            val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { out ->
                                croppedBmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            currentPhotoPath = file.absolutePath
                            showCropDialog = false
                            pendingCropBitmap = null
                            errorMessage = null
                            Toast.makeText(context, "Surat xavfsizlik tekshiruvidan o'tdi va tayyorlandi ✨", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Suratni saqlashda xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            VelvetRosePrimary
        } else {
            if (isDark) Color(0xFF262626) else Color(0xFFF1F5F9)
        },
        border = if (isSelected) null else BorderStroke(1.dp, if (isDark) Color(0xFF404040) else Color(0xFFE2E8F0)),
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else TextPrimary)
            )
        }
    }
}
