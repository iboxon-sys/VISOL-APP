package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.UserRepository
import com.example.model.ProfileVisitor
import com.example.model.UserProfile
import com.example.model.UserStory
import com.example.model.UzbekistanData
import com.example.model.RegionInfo
import com.example.model.DistrictInfo
import com.example.ui.components.*
import com.example.ui.components.SubscriptionDialog
import com.example.ui.theme.*
import com.example.util.SocialMediaHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ULTRA-MODERN MAIN SCREEN (Warm Soft Glass & Luxury Structure)
 * - 5-Hub Navigation Island (Tanishuv, Reels, E'lonlar, Suhbatlar, Profilim)
 * - Dual Discovery Mode (🔥 Swipe Card Stack & ✨ Bento Grid Feed)
 * - Active Stories & Members horizontal reel
 * - Live Glass Filter Chips & Instant Search
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userRepository: UserRepository,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val hasInternet = remember {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(network)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: true
        } catch (e: Exception) {
            true
        }
    }
    val currentUser by userRepository.currentUser.collectAsState()
    val isSubscribed by userRepository.isSubscribed.collectAsState()
    val candidates by userRepository.candidates.collectAsState()
    val visitors by userRepository.visitors.collectAsState()
    val lastViewedVisitorCount by userRepository.lastViewedVisitorCount.collectAsState()
    val unreadVisitorsCount = (visitors.size - lastViewedVisitorCount).coerceAtLeast(0)
    val totalUnreadCount by userRepository.totalUnreadCount.collectAsState()
    val allChatMessages by userRepository.chatMessages.collectAsState()
    val allStories by userRepository.userStories.collectAsState()
    val followedCandidateIds by userRepository.followedCandidateIds.collectAsState()
    val likedCandidateIds by userRepository.likedCandidateIds.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isRefreshingCandidates by remember { mutableStateOf(false) }

    // Nomzodlarni backend APIdan yangilash
    LaunchedEffect(Unit) {
        userRepository.fetchCandidatesFromApi()
    }

    // Jins bo'yicha qat'iy avtomatik moslashtirish
    val currentUserGender = currentUser?.gender ?: "Erkak"
    val targetGender = if (currentUserGender.equals("Erkak", ignoreCase = true)) "Ayol" else "Erkak"

    // Dinamik filtrlar (7 yoshdan 100 yoshgacha)
    var showFilterSheet by remember { mutableStateOf(false) }
    var minAgeFilter by remember { mutableFloatStateOf(7f) }
    var maxAgeFilter by remember { mutableFloatStateOf(100f) }
    var selectedRegionFilter by remember { mutableStateOf<String?>(null) }
    var selectedDistrictFilter by remember { mutableStateOf<String?>(null) }
    var mahallaFilterQuery by remember { mutableStateOf("") }
    var verifiedOnlyFilter by remember { mutableStateOf(false) }

    // Discovery Display Mode: 0 = Swipe Deck, 1 = Bento Grid
    var discoveryMode by remember { mutableIntStateOf(0) }

    var selectedCandidateForDetail by remember { mutableStateOf<UserProfile?>(null) }
    var showSubscriptionSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showVisitorsSheet by remember { mutableStateOf(false) }
    var showChatListSheet by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }
    var activeChatCandidate by remember { mutableStateOf<UserProfile?>(null) }

    // Story Viewers / Creators state
    var viewingStoryGroup by remember { mutableStateOf<List<UserStory>?>(null) }
    var showStoryCreatorModal by remember { mutableStateOf(false) }

    // Bottom Nav: 0 = Tanishuv, 1 = Reels, 2 = E'lonlar, 3 = Suhbatlar, 4 = Profilim
    var selectedBottomNav by remember { mutableIntStateOf(0) }

    val isDark = LocalIsDarkTheme.current

    var candidateSearchQuery by remember { mutableStateOf("") }

    // Filtr natijalari
    val filteredCandidates = remember(
        candidates,
        currentUserGender,
        minAgeFilter,
        maxAgeFilter,
        selectedRegionFilter,
        selectedDistrictFilter,
        mahallaFilterQuery,
        candidateSearchQuery,
        verifiedOnlyFilter
    ) {
        val base = userRepository.getFilteredCandidates(
            currentUserGender = currentUserGender,
            minAge = minAgeFilter.toInt(),
            maxAge = maxAgeFilter.toInt(),
            region = selectedRegionFilter,
            district = selectedDistrictFilter,
            mahalla = if (mahallaFilterQuery.isNotBlank()) mahallaFilterQuery.trim() else null,
            searchQuery = candidateSearchQuery
        )
        if (verifiedOnlyFilter) base.filter { it.isVip || it.isVerifiedCamera } else base
    }

    val activeFilterCount = (if (minAgeFilter.toInt() > 18 || maxAgeFilter.toInt() < 60) 1 else 0) +
            (if (!selectedRegionFilter.isNullOrBlank()) 1 else 0) +
            (if (!selectedDistrictFilter.isNullOrBlank()) 1 else 0) +
            (if (mahallaFilterQuery.isNotBlank()) 1 else 0) +
            (if (verifiedOnlyFilter) 1 else 0)

    Box(modifier = Modifier.fillMaxSize()) {
        // Romantik va jonli harakatlanuvchi fon olib tashlandi

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (selectedBottomNav != 1) { // Reelsda to'liq ekran video
                    Surface(
                        color = if (isDark) Color(0x4413091B) else Color(0x78FFFFFF),
                    shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp),
                    shadowElevation = 4.dp,
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                SoftPeach.copy(alpha = 0.35f),
                                SoftRosePink.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(SoftPeach, SoftRosePink))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (hasInternet) "Visol.uz" else "Aloqa yo'q...",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 19.sp,
                                            letterSpacing = 0.5.sp,
                                            color = if (isDark) Color.White else TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "🕊️",
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = if (hasInternet) "O'zbekona Tanishuv va Oila" else "Tarmoqqa ulanish kutilmoqda",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (hasInternet) SoftPeach else Color(0xFFEF4444)
                                    )
                                }
                            }
                        },
                        actions = {
                            // 1. Profilni kuzatganlar (Eye badge)
                            Surface(
                                onClick = {
                                    userRepository.markVisitorsAsRead()
                                    showVisitorsSheet = true
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isDark) Color(0x33FF7B54) else WarmPeachSoft,
                                border = BorderStroke(1.dp, SoftPeach.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("visitors_eye_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Profilni kuzatganlar",
                                        tint = if (isDark) Color.White else TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (unreadVisitorsCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(17.dp)
                                                .clip(CircleShape)
                                                .background(SoftRosePink),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$unreadVisitorsCount",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // 2. VIP Obuna Badge (Izolyatsiya qilingan komponent — butun ekranni qayta chizmaydi)
                            VipBadgeChip(
                                isSubscribed = isSubscribed,
                                userRepository = userRepository,
                                isDark = isDark,
                                onClick = { showSubscriptionSheet = true }
                            )

                            // 3. Theme switch
                            IconButton(
                                onClick = {
                                    val newMode = if (isDark) "LIGHT" else "DARK"
                                    userRepository.setThemeMode(newMode)
                                },
                                modifier = Modifier.testTag("theme_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Mavzu",
                                    tint = if (isDark) WarmHoneyGold else Color(0xFF64748B)
                                )
                            }

                            // 4. Logout / Chiqish
                            IconButton(
                                onClick = { showLogoutConfirmDialog = true },
                                modifier = Modifier.testTag("logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Chiqish",
                                    tint = Color(0xFFEF4444).copy(alpha = 0.8f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        },
        bottomBar = {
            // iOS 18 Glass Floating & Interactive Draggable Navigation Bar
            IOSGlassScrollableTabBar(
                selectedIndex = selectedBottomNav,
                onTabSelected = { newTab ->
                    if (newTab == 4 && !isSubscribed) {
                        showSubscriptionSheet = true
                    } else {
                        selectedBottomNav = newTab
                    }
                },
                isDark = isDark,
                unreadCount = totalUnreadCount
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // RomanticAnimatedBackground olib tashlandi
            when (selectedBottomNav) {
                1 -> {
                    // Reels & Postlar bo'limi
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        PostsAndReelsFeedScreen(
                            userRepository = userRepository,
                            currentUser = currentUser,
                            onCandidateClick = { candidate ->
                                selectedCandidateForDetail = candidate
                            },
                            onSubscribeClick = {
                                showSubscriptionSheet = true
                            },
                            onMyProfileClick = {
                                selectedBottomNav = 5
                            }
                        )
                    }
                }
                2 -> {
                    // StarMaker Style Voice Party Rooms & Live Stream Hub
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    ) {
                        VoiceRoomAndStreamScreen(
                            userRepository = userRepository
                        )
                    }
                }
                3 -> {
                    // Do'kon / Tangalar bo'limi
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    ) {
                        CoinStoreScreen(
                            userRepository = userRepository
                        )
                    }
                }
                4 -> {
                    // Chat / Suhbatlar bo'limi
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    ) {
                        if (!isSubscribed) {
                            // VIP Qulflangan holati
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    backgroundColor = if (isDark) WarmChocoGlassDark else Color.White.copy(alpha = 0.95f),
                                    borderColor = SoftGlassBorderDark
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(listOf(WarmPeachSoft, SoftRosePinkGlow.copy(alpha = 0.3f)))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = SoftRosePink,
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(18.dp))
                                        Text(
                                            text = "Shaxsiy Suhbatlar Qulflangan 🔒",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp,
                                            textAlign = TextAlign.Center,
                                            color = if (isDark) Color.White else TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Nomzodlar bilan cheksiz xabarlashish, fotosuratlar almashish va ovozli suhbat qurish uchun VIP obunani faollashtiring.",
                                            fontSize = 13.sp,
                                            color = if (isDark) Color(0xFFC4A8B9) else TextSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(22.dp))
                                        Button(
                                            onClick = { showSubscriptionSheet = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftPeach),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("VIP Obunani Faollashtirish", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            val conversations = remember(allChatMessages, candidates) { userRepository.getChatConversations() }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Mening suhbatlarim 💬",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (conversations.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = SoftPeach.copy(alpha = 0.5f),
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Hozircha suhbatlar yo'q",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else TextPrimary,
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Nomzodlar sahifasidan o'zingizga yoqqan inson profiliga kiring va birinchi salomni yo'llang!",
                                                color = if (isDark) Color(0xFFC4A8B9) else TextSecondary,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(18.dp))
                                            Button(
                                                onClick = { selectedBottomNav = 0 },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftPeach),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text("Nomzodlarni ko'rish")
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(conversations) { (candidate, lastMsg) ->
                                            GlassCard(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { activeChatCandidate = candidate },
                                                shape = RoundedCornerShape(18.dp),
                                                backgroundColor = if (isDark) Color(0x1F2B173B) else Color.White,
                                                borderColor = SoftGlassBorderDark
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    ProfileAvatar(
                                                        photoPath = candidate.photoPath,
                                                        gender = candidate.gender,
                                                        size = 50.dp,
                                                        showCameraBadge = false
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = candidate.fullName,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 15.sp,
                                                                color = if (isDark) Color.White else TextPrimary
                                                            )
                                                            Text(
                                                                text = candidate.displayUsername,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = SoftPeach
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Text(
                                                            text = lastMsg.text,
                                                            fontSize = 13.sp,
                                                            color = if (isDark) Color(0xFFC4A8B9) else TextSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
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
                }
                5 -> {
                    // Profilim bo'limi
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    ) {
                        if (currentUser != null) {
                            MyProfileScreen(
                                user = currentUser!!,
                                userRepository = userRepository,
                                onLogoutClick = {
                                    showLogoutConfirmDialog = true
                                },
                                onCandidateClick = { candidate ->
                                    selectedCandidateForDetail = candidate
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Foydalanuvchi topilmadi", color = TextSecondary)
                            }
                        }
                    }
                }
                else -> {
                    // 0: Tanishuv (Discovery Hub: Stories, Mode Switcher, Quick Filters & Feeds)
                    VisolPullRefreshContainer(
                        isRefreshing = isRefreshingCandidates,
                        onRefresh = {
                            coroutineScope.launch {
                                isRefreshingCandidates = true
                                userRepository.fetchCandidatesFromApi(forceRefresh = true)
                                delay(800)
                                isRefreshingCandidates = false
                                Toast.makeText(context, "Nomzodlar ro'yxati yangilandi! ✨", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                        ) {
                        // 1. Stories Reel (Instagram / Telegram Avatars - Faqat obuna bo'linganlar + O'z hikoyangiz)
                        ActiveStoriesReel(
                            currentUser = currentUser,
                            allStories = allStories,
                            followedCandidateIds = followedCandidateIds,
                            candidates = candidates,
                            isDark = isDark,
                            onMyStoryClick = {
                                val myStories = allStories.filter {
                                    (it.authorId == (currentUser?.id ?: "user_me") || it.authorId == "user_me" || it.authorId == "me") && it.isStoryActive()
                                }
                                if (myStories.isNotEmpty()) {
                                    viewingStoryGroup = myStories
                                } else {
                                    showStoryCreatorModal = true
                                }
                            },
                            onStoryGroupClick = { storyGroup ->
                                viewingStoryGroup = storyGroup
                            },
                            onCandidateClick = { candidate ->
                                selectedCandidateForDetail = candidate
                            }
                        )

                        // 2. Search & Filter Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Search Input
                            OutlinedTextField(
                                value = candidateSearchQuery,
                                onValueChange = { candidateSearchQuery = it },
                                placeholder = { Text("Ism, ID yoki hudud...", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = SoftPeach, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (candidateSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { candidateSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Tozalash", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoftPeach,
                                    unfocusedBorderColor = if (isDark) SoftGlassBorderDark else Color(0x33FF7B54),
                                    focusedContainerColor = if (isDark) Color(0x1F2A183A) else Color.White,
                                    unfocusedContainerColor = if (isDark) Color(0x1F2A183A) else Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("input_search_candidates")
                            )

                            // Mode Switcher Pill (🔥 Swipe vs ✨ Grid vs ❤️ Yoqqanlar)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDark) Color(0x2E2A183A) else WarmPeachSoft,
                                border = BorderStroke(1.dp, SoftGlassBorderDark),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Swipe Mode Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (discoveryMode == 0) SoftPeach else Color.Transparent)
                                            .clickable { discoveryMode = 0 }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "🔥 Karta",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (discoveryMode == 0) Color.White else if (isDark) Color(0xFFC4A8B9) else TextSecondary
                                        )
                                    }

                                    // Grid Mode Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (discoveryMode == 1) SoftPeach else Color.Transparent)
                                            .clickable { discoveryMode = 1 }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "✨ Katalog",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (discoveryMode == 1) Color.White else if (isDark) Color(0xFFC4A8B9) else TextSecondary
                                        )
                                    }

                                    // Liked Candidates Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (discoveryMode == 2) SoftPeach else Color.Transparent)
                                            .clickable { discoveryMode = 2 }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "❤️ Yoqqanlar",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (discoveryMode == 2) Color.White else if (isDark) Color(0xFFC4A8B9) else TextSecondary
                                            )
                                            if (likedCandidateIds.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(if (discoveryMode == 2) Color.White else SoftRosePink),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${likedCandidateIds.size}",
                                                        color = if (discoveryMode == 2) SoftPeach else Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Filter Button
                            Surface(
                                onClick = { showFilterSheet = true },
                                shape = RoundedCornerShape(20.dp),
                                color = if (activeFilterCount > 0) SoftPeach else if (isDark) Color(0x2E2A183A) else Color.White,
                                border = BorderStroke(1.dp, SoftGlassBorderDark),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("btn_open_filters")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Filtrlar",
                                        tint = if (activeFilterCount > 0) Color.White else SoftPeach,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (activeFilterCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$activeFilterCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Quick Horizontal Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Target Gender Indicator Chip
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0x26FF7B54) else WarmPeachSoft,
                                border = BorderStroke(0.5.dp, SoftPeach.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (targetGender == "Ayol") "Qizlar 🧕 (${filteredCandidates.size})" else "Yigitlar 🧔 (${filteredCandidates.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftPeach,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            // Face ID Verified Toggle Chip
                            Surface(
                                onClick = { verifiedOnlyFilter = !verifiedOnlyFilter },
                                shape = RoundedCornerShape(14.dp),
                                color = if (verifiedOnlyFilter) Color(0xFF10B981) else if (isDark) Color(0x1F2A183A) else Color.White,
                                border = BorderStroke(0.5.dp, if (verifiedOnlyFilter) Color(0xFF10B981) else SoftGlassBorderDark)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (verifiedOnlyFilter) Color.White else Color(0xFF10B981),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Face ID tasdiqlangan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (verifiedOnlyFilter) Color.White else if (isDark) Color.White else TextPrimary
                                    )
                                }
                            }

                            // Selected Region Filter Chip
                            if (!selectedRegionFilter.isNullOrBlank()) {
                                Surface(
                                    onClick = { selectedRegionFilter = null },
                                    shape = RoundedCornerShape(14.dp),
                                    color = SoftPeach,
                                    border = BorderStroke(0.5.dp, SoftPeach)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedRegionFilter!!,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 4. Feeds Content: Swipe Deck or Bento Grid or Layklarim
                        if (discoveryMode == 0) {
                            // 🔥 Mode 0: Swipe Deck
                             CandidateSwipeDeck(
                                candidates = filteredCandidates,
                                isSubscribed = isSubscribed,
                                isDark = isDark,
                                onViewCandidateDetails = { candidate ->
                                    selectedCandidateForDetail = candidate
                                },
                                onOpenChat = { candidate ->
                                    activeChatCandidate = candidate
                                },
                                onRequireSubscription = {
                                    showSubscriptionSheet = true
                                },
                                onLike = { candidate ->
                                    userRepository.toggleLikeCandidate(candidate.id)
                                },
                                onSuperLike = { candidate ->
                                    Toast.makeText(context, "${candidate.fullName}ga Super Like yuborildi! ⭐", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        } else if (discoveryMode == 1) {
                            // ✨ Mode 1: Bento Grid Catalog
                            if (filteredCandidates.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        backgroundColor = if (isDark) WarmChocoGlassDark else Color.White,
                                        borderColor = SoftGlassBorderDark
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SearchOff,
                                                contentDescription = null,
                                                tint = SoftPeach,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Mos nomzodlar topilmadi",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (isDark) Color.White else TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Filtrlarni o'zgartirib ko'ring yoki tozalang.",
                                                fontSize = 12.sp,
                                                color = if (isDark) Color(0xFFC4A8B9) else TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = {
                                                    minAgeFilter = 7f
                                                    maxAgeFilter = 100f
                                                    selectedRegionFilter = null
                                                    selectedDistrictFilter = null
                                                    mahallaFilterQuery = ""
                                                    verifiedOnlyFilter = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftPeach),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text("Filtrni tozalash")
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
                                ) {
                                    items(filteredCandidates, key = { it.id }) { candidate ->
                                        CandidateBentoCard(
                                            candidate = candidate,
                                            isSubscribed = isSubscribed,
                                            isDark = isDark,
                                            onViewDetails = {
                                                selectedCandidateForDetail = candidate
                                            },
                                            onOpenChat = {
                                                activeChatCandidate = candidate
                                            },
                                            onRequireSubscription = {
                                                showSubscriptionSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // ❤️ Mode 2: Mening Layklarim (Your Liked Candidates)
                            val likedList = remember(candidates, likedCandidateIds) {
                                candidates.filter { likedCandidateIds.contains(it.id) }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                if (likedList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Outlined.FavoriteBorder,
                                                contentDescription = null,
                                                tint = SoftRosePink.copy(alpha = 0.5f),
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Hozircha hech kimga layk bosmagansiz",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else TextPrimary,
                                                fontSize = 16.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Tanishuv bo'limida yoqqan nomzodlarga ❤️ tugmasini bosing va ular bu yerda saqlanadi!",
                                                color = if (isDark) Color(0xFFC4A8B9) else TextSecondary,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 32.dp)
                                            )
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(likedList) { candidate ->
                                            CandidateBentoCard(
                                                candidate = candidate,
                                                isSubscribed = isSubscribed,
                                                isDark = isDark,
                                                onViewDetails = { selectedCandidateForDetail = candidate },
                                                onOpenChat = {
                                                    if (isSubscribed) {
                                                        activeChatCandidate = candidate
                                                    } else {
                                                        showSubscriptionSheet = true
                                                    }
                                                },
                                                onRequireSubscription = { showSubscriptionSheet = true }
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
            }
        }
    }

    // Modal BottomSheet: Kengaytirilgan Qidiruv Filtrlari
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = if (isDark) Color(0xFF1B0F26) else Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = SoftPeach,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Qidiruv filtrlari",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (isDark) Color.White else TextPrimary
                            )
                        )
                    }
                    IconButton(onClick = { showFilterSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Yosh oralig'i Slider
                GlassCard(
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = if (isDark) Color(0x1F2A183A) else WarmPeachSoft.copy(alpha = 0.5f),
                    borderColor = SoftGlassBorderDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Yosh oralig'i",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SoftPeach
                            ) {
                                Text(
                                    text = "${minAgeFilter.toInt()} - ${maxAgeFilter.toInt()} yosh",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        RangeSlider(
                            value = minAgeFilter..maxAgeFilter,
                            onValueChange = { range ->
                                minAgeFilter = range.start
                                maxAgeFilter = range.endInclusive
                            },
                            valueRange = 7f..100f,
                            steps = 92,
                            colors = SliderDefaults.colors(
                                thumbColor = SoftPeach,
                                activeTrackColor = SoftPeach
                            ),
                            modifier = Modifier.testTag("filter_age_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("7 yosh", fontSize = 11.sp, color = if (isDark) Color(0xFFC4A8B9) else TextSecondary)
                            Text("100 yosh", fontSize = 11.sp, color = if (isDark) Color(0xFFC4A8B9) else TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Viloyat tanlash
                Text(
                    text = "Viloyat / Hudud",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDark) Color.White else TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                var regionDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = regionDropdownExpanded,
                    onExpandedChange = { regionDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRegionFilter ?: "Barcha viloyatlar",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionDropdownExpanded) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = SoftPeach) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = regionDropdownExpanded,
                        onDismissRequest = { regionDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Barcha viloyatlar (Barchasi)") },
                            onClick = {
                                selectedRegionFilter = null
                                selectedDistrictFilter = null
                                regionDropdownExpanded = false
                            }
                        )
                        UzbekistanData.regions.forEach { reg ->
                            DropdownMenuItem(
                                text = { Text(reg.name) },
                                onClick = {
                                    selectedRegionFilter = reg.name
                                    selectedDistrictFilter = null
                                    regionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Qo'llash va Tozalash tugmalari
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            minAgeFilter = 18f
                            maxAgeFilter = 60f
                            selectedRegionFilter = null
                            selectedDistrictFilter = null
                            mahallaFilterQuery = ""
                            verifiedOnlyFilter = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Tozalash")
                    }

                    Button(
                        onClick = { showFilterSheet = false },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftPeach),
                        modifier = Modifier.weight(1.6f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Qo'llash (${filteredCandidates.size} ta)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Sheet: Profilni Kuzatganlar (Eye Action)
    if (showVisitorsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVisitorsSheet = false },
            containerColor = if (isDark) Color(0xFF1B0F26) else Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = SoftPeach,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Profilni kuzatganlar (${visitors.size})",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isDark) Color.White else TextPrimary
                            )
                        )
                    }
                    IconButton(onClick = { showVisitorsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (visitors.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hozircha profilingizni hech kim ko'rmagan",
                            color = if (isDark) Color(0xFFC4A8B9) else TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visitors) { visitor ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSubscribed) {
                                            selectedCandidateForDetail = visitor.visitorProfile
                                            showVisitorsSheet = false
                                        } else {
                                            showSubscriptionSheet = true
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = if (isDark) Color(0x1F2A183A) else Color(0xFFF9FAFB),
                                borderColor = SoftGlassBorderDark
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileAvatar(
                                        photoPath = if (isSubscribed) visitor.visitorProfile.photoPath else "",
                                        gender = visitor.visitorProfile.gender,
                                        size = 48.dp,
                                        showCameraBadge = false
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isSubscribed) visitor.visitorProfile.fullName else "Sirli mehmon (VIP)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isDark) Color.White else TextPrimary
                                        )
                                        Text(
                                            text = if (isSubscribed) "📍 ${visitor.visitorProfile.viloyat}" else "Kimligini ko'rish uchun VIP oling 🔒",
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFC4A8B9) else TextSecondary
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

    // Modal: Story Viewer (Instagram / Telegram format)
    if (viewingStoryGroup != null) {
        val group = viewingStoryGroup!!
        val isMyStories = group.any { story: UserStory ->
            story.authorId == (currentUser?.id ?: "user_me") || story.authorId == "user_me" || story.authorId == "me"
        }
        StoryViewerDialog(
            stories = group,
            userRepository = userRepository,
            onDismiss = { viewingStoryGroup = null },
            onAddAnotherStory = if (isMyStories) {
                {
                    viewingStoryGroup = null
                    showStoryCreatorModal = true
                }
            } else null
        )
    }

    // Modal: Story Creator
    if (showStoryCreatorModal) {
        StoryCreatorDialog(
            userRepository = userRepository,
            isDark = isDark,
            onDismiss = { showStoryCreatorModal = false },
            onStoryCreated = {
                showStoryCreatorModal = false
            }
        )
    }

    // Modal: VIP Obuna dialogi
    if (showSubscriptionSheet) {
        SubscriptionDialog(
            isAlreadySubscribed = isSubscribed,
            onDismiss = { showSubscriptionSheet = false },
            onSubscribed = { plan ->
                userRepository.activateSubscription(plan)
                showSubscriptionSheet = false
            }
        )
    }

    // Modal: Nomzodning to'liq sahifasi
    if (selectedCandidateForDetail != null) {
        CandidateDetailFullScreenDialog(
            candidate = selectedCandidateForDetail!!,
            userRepository = userRepository,
            isSubscribed = isSubscribed,
            isDark = isDark,
            onDismiss = { selectedCandidateForDetail = null },
            onRequireSubscription = { showSubscriptionSheet = true }
        )
    }

    // Modal: Nomzod bilan shaxsiy chat
    if (activeChatCandidate != null) {
        CandidateChatDialog(
            candidate = activeChatCandidate!!,
            userRepository = userRepository,
            isDark = isDark,
            onDismiss = { activeChatCandidate = null },
            onOpenCandidateProfile = { profile ->
                selectedCandidateForDetail = profile
                activeChatCandidate = null
            }
        )
    }

    // Modal: Chiqishni tasdiqlash
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            containerColor = if (isDark) Color(0xFF1B0F26) else Color.White,
            title = {
                Text("Hisobdan chiqish", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else TextPrimary)
            },
            text = {
                Text("Rostdan ham hisobingizdan chiqmoqchimisiz?", color = if (isDark) Color(0xFFC4A8B9) else TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        userRepository.logout()
                        onAccountDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Chiqish", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }
}

/**
 * Izolyatsiya qilingan VIP nishoni komponenti.
 * 1 soniyalik hisoblagich faqat ushbu kichik tugmachaning o'zini yangilaydi va butun MainScreen'ni lag qilmaydi.
 */
@Composable
private fun VipBadgeChip(
    isSubscribed: Boolean,
    userRepository: UserRepository,
    isDark: Boolean,
    onClick: () -> Unit
) {
    var remainingSec by remember { mutableLongStateOf(userRepository.getRemainingSubSeconds()) }

    LaunchedEffect(isSubscribed) {
        while (isSubscribed) {
            remainingSec = userRepository.getRemainingSubSeconds()
            kotlinx.coroutines.delay(1000L)
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSubscribed) WarmHoneyGold else if (isDark) Color(0x33FFFFFF) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isSubscribed) WarmHoneyGoldLight else SoftGlassBorderDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSubscribed) Icons.Default.Verified else Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = if (isSubscribed) Color.Black else SoftPeach,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            val subText = if (isSubscribed) {
                val d = remainingSec / 86400
                val h = (remainingSec % 86400) / 3600
                val m = (remainingSec % 3600) / 60
                if (d > 0) "${d}k ${h}s" else "${h}s ${m}d"
            } else "VIP"
            Text(
                text = subText,
                color = if (isSubscribed) Color.Black else if (isDark) Color.White else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
