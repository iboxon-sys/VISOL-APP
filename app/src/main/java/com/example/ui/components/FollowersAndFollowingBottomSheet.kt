package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserRepository
import com.example.model.UserProfile
import com.example.ui.theme.VelvetRoseLight
import com.example.ui.theme.VelvetRosePrimary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Full-screen resizable Glassmorphic Bottom Sheet displaying Followers ("Obunachilar") 
 * and Following ("Obuna bo'lganlar") lists with live API data, sticky search bar, optimistic follow toggles,
 * and clean empty states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersAndFollowingBottomSheet(
    userId: String,
    initialTab: Int = 0, // 0 = Obunachilar, 1 = Obuna bo'lganlar
    userRepository: UserRepository,
    isDark: Boolean = true,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }

    var followersList by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var followingList by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserState by userRepository.currentUser.collectAsState()
    val candidates by userRepository.candidates.collectAsState()
    val followingIds by userRepository.followingUserIds.collectAsState()

    val isOwner = remember(currentUserState, userId) {
        currentUserState?.id == userId
    }

    val targetUser = remember(currentUserState, candidates, userId) {
        if (isOwner) currentUserState else candidates.find { it.id == userId }
    }

    val isPrivateAccount = targetUser?.isPrivate == true

    // Fetch Followers and Following from UserRepository
    LaunchedEffect(userId, selectedTab) {
        isLoading = true
        if (selectedTab == 0) {
            userRepository.getFollowersList(userId).collectLatest { list ->
                followersList = list
                isLoading = false
            }
        } else {
            userRepository.getFollowingList(userId).collectLatest { list ->
                followingList = list
                isLoading = false
            }
        }
    }

    val currentRawList = if (selectedTab == 0) followersList else followingList
    val filteredList = remember(currentRawList, searchQuery) {
        if (searchQuery.isBlank()) {
            currentRawList
        } else {
            currentRawList.filter { u ->
                u.fullName.contains(searchQuery, ignoreCase = true) ||
                        u.viloyat.contains(searchQuery, ignoreCase = true) ||
                        u.tuman.contains(searchQuery, ignoreCase = true) ||
                        u.bio.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
            )
        },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isOwner) "Mening obunachilarim va obunalarim" else "${targetUser?.fullName ?: "Nomzod"} obunachilari",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Yopish",
                        tint = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Only allow viewing if profile is public, or viewer is the owner
            if (isPrivateAccount && !isOwner) {
                // Glassmorphic Privacy Badge for visitors on private accounts
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0x22FFFFFF) else Color(0x22000000)
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = VelvetRosePrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Ushbu hisob obunachilari maxfiy qilingan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nomzod o'z hisobining xavfsizligi va maxfiyligini ta'minlash maqsadida obunachilar ro'yxatini yashirgan.",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Tabs Selector
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = VelvetRosePrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Obunachilar (${followersList.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Obuna bo'lganlar (${followingList.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sticky Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Ism yoki tuman bo'yicha qidirish...",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = VelvetRosePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tozalash",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelvetRosePrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // List Content / Loading / Empty States
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VelvetRosePrimary)
                    }
                } else if (filteredList.isEmpty()) {
                    // Glassmorphic Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) Color(0x1F2B394A) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(20.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = if (selectedTab == 1) "Hali obunalar mavjud emas" else "Hali obunachilar mavjud emas",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Platformadagi nomzodlar bilan obuna munosabatlarini o'rnating.",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.padding(bottom = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredList, key = { it.id }) { user ->
                            val isUserFollowing = followingIds.contains(user.id)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) Color(0x15FFFFFF) else Color(0x06000000),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUser(user) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Avatar
                                        Box {
                                            ProfileAvatar(
                                                photoPath = user.photoPath,
                                                gender = user.gender,
                                                size = 46.dp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color(0xFF10B981), CircleShape)
                                                    .clip(CircleShape)
                                                    .align(Alignment.BottomEnd)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.fullName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (user.isVerifiedCamera) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Verified,
                                                        contentDescription = "Tasdiqlangan",
                                                        tint = VelvetRosePrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = "${user.dynamicAge} yosh • 📍 ${user.viloyat}",
                                                fontSize = 12.sp,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Dynamic Button based on permissions (Owner vs Visitor)
                                    if (isOwner) {
                                        if (selectedTab == 0) {
                                            // Owner managing followers -> Remove follower directly
                                            OutlinedButton(
                                                onClick = {
                                                    followersList = followersList.filter { it.id != user.id }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = Color(0xFFEF4444)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(
                                                    text = "O'chirish",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            // Owner managing following -> Unfollow directly
                                            OutlinedButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        userRepository.toggleFollowStatus(user.id).collectLatest {}
                                                        followingList = followingList.filter { it.id != user.id }
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(
                                                    text = "O'chirish",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        // Visitor viewing lists -> can Follow/Unfollow directly
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    userRepository.toggleFollowStatus(user.id).collectLatest {}
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = if (isUserFollowing) {
                                                ButtonDefaults.buttonColors(
                                                    containerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                                                )
                                            } else {
                                                ButtonDefaults.buttonColors(
                                                    containerColor = VelvetRosePrimary,
                                                    contentColor = Color.White
                                                )
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isUserFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isUserFollowing) "Obunada" else "Obuna bo'lish",
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
            }
        }
    }
}
