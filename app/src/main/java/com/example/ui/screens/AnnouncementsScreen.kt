package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserRepository
import com.example.model.Announcement
import com.example.model.AnnouncementComment
import com.example.model.UzbekistanData
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.TelegramBrandIcon
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    userRepository: UserRepository,
    isDark: Boolean,
    onOpenCandidateProfile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val announcements by userRepository.announcements.collectAsState()
    val currentUser by userRepository.currentUser.collectAsState()
    val isSubscribed by userRepository.isSubscribed.collectAsState()

    var selectedCategory by remember { mutableStateOf("Barchasi") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Comment Modal Sheet / Bottom Drawer State
    var activeCommentAnnouncementId by remember { mutableStateOf<String?>(null) }
    var replyingToComment by remember { mutableStateOf<AnnouncementComment?>(null) }
    var commentInputText by remember { mutableStateOf("") }

    val categories = listOf("Barchasi", "Sovchilik", "To'y & Nikoh", "Tavsiyalar", "Xizmatlar")

    val filteredList = remember(announcements, selectedCategory, searchQuery) {
        announcements.filter { ann ->
            val matchCat = selectedCategory == "Barchasi" || ann.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    ann.title.contains(searchQuery, ignoreCase = true) ||
                    ann.description.contains(searchQuery, ignoreCase = true) ||
                    ann.region.contains(searchQuery, ignoreCase = true) ||
                    ann.authorName.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    val bgColor = if (isDark) Color(0xFF0F111A) else Color(0xFFF9FAFC)
    val cardBgColor = if (isDark) Color(0xFF191C2B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = VelvetRosePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier
                    .testTag("btn_create_announcement_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "E'lon berish")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "E'lon berish",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Surface(
                color = if (isDark) Color(0xFF151824) else Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "E'lonlar Maydoni",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VelvetRosePrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, VelvetRosePrimary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "HALOL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = VelvetRosePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Sovchilik, to'y xizmatlari va foydali e'lonlar",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }

                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VelvetRosePrimary.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = "Yangi e'lon",
                                tint = VelvetRosePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Qidiruv maydoni
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("E'lonlar, viloyatlar yoki ismlar bo'yicha qidiruv...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = VelvetRosePrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Tozalash")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VelvetRosePrimary,
                            unfocusedBorderColor = if (isDark) Color(0xFF2E334D) else Color(0xFFE2E8F0),
                            focusedContainerColor = if (isDark) Color(0xFF10121C) else Color(0xFFF8FAFC),
                            unfocusedContainerColor = if (isDark) Color(0xFF10121C) else Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("input_search_announcements")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Kategoriyalar qatori
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) VelvetRosePrimary else if (isDark) Color(0xFF22273B) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) VelvetRosePrimary else if (isDark) Color(0xFF2E334D) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .clickable { selectedCategory = cat }
                                    .testTag("filter_cat_$cat")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    val icon = when (cat) {
                                        "Sovchilik" -> "💍"
                                        "To'y & Nikoh" -> "💐"
                                        "Tavsiyalar" -> "📖"
                                        "Xizmatlar" -> "✨"
                                        else -> "🌟"
                                    }
                                    Text(text = icon, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // E'lonlar Ro'yxati
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VelvetRosePrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = VelvetRosePrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Hozircha bu toifada e'lonlar yo'q",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Birinchi bo'lib o'z e'loningizni joylashtiring!",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("E'lon berish +", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredList, key = { it.id }) { ann ->
                        AnnouncementItemCard(
                            announcement = ann,
                            isDark = isDark,
                            cardBgColor = cardBgColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onLike = { userRepository.toggleAnnouncementLike(ann.id) },
                            onOpenAuthor = {
                                val cand = userRepository.getCandidateByIdOrDetails(
                                    authorId = ann.authorId,
                                    authorName = ann.authorName,
                                    authorGender = ann.authorGender,
                                    authorPhotoPath = ann.authorPhotoPath,
                                    region = ann.region,
                                    phone = ann.contactPhone,
                                    telegram = ann.telegramUsername
                                )
                                onOpenCandidateProfile?.invoke(cand.id)
                            },
                            onOpenCommentSheet = {
                                activeCommentAnnouncementId = ann.id
                                replyingToComment = null
                            },
                            onCall = { phone ->
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Qo'ng'iroq qilish imkonsiz", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onTelegram = { username ->
                                try {
                                    val clean = username.removePrefix("@")
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$clean"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Telegram ochilmadi", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Comments
    activeCommentAnnouncementId?.let { annId ->
        val currentAnnouncement = announcements.find { it.id == annId }
        if (currentAnnouncement != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    activeCommentAnnouncementId = null
                    replyingToComment = null
                    commentInputText = ""
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = if (isDark) Color(0xFF161926) else Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Fikr va Izohlar",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = VelvetRosePrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${currentAnnouncement.comments.size + currentAnnouncement.comments.sumOf { it.replies.size }}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VelvetRosePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                activeCommentAnnouncementId = null
                                replyingToComment = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Yopish", tint = textSecondary)
                        }
                    }

                    HorizontalDivider(color = if (isDark) Color(0xFF24283B) else Color(0xFFE2E8F0))

                    // Comments List
                    if (currentAnnouncement.comments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Hozircha izohlar yo'q",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSecondary
                                )
                                Text(
                                    text = "Birinchi bo'lib fikringizni bildiring!",
                                    fontSize = 12.sp,
                                    color = textSecondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(currentAnnouncement.comments, key = { it.id }) { comment ->
                                CommentItemRow(
                                    comment = comment,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onLike = {
                                        userRepository.toggleAnnouncementCommentLike(annId, comment.id)
                                    },
                                    onReply = {
                                        replyingToComment = comment
                                    },
                                    onOpenAuthor = {
                                        activeCommentAnnouncementId = null
                                        val cand = userRepository.getCandidateByIdOrDetails(comment.authorId, comment.authorName, comment.authorGender, comment.authorPhotoPath)
                                        onOpenCandidateProfile?.invoke(cand.id)
                                    },
                                    onOpenReplyAuthor = { replyAuthorId ->
                                        activeCommentAnnouncementId = null
                                        val cand = userRepository.getCandidateByIdOrDetails(replyAuthorId)
                                        onOpenCandidateProfile?.invoke(cand.id)
                                    },
                                    onLikeReply = { replyId ->
                                        userRepository.toggleAnnouncementCommentLike(annId, replyId)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Replying Indicator Banner
                    AnimatedVisibility(visible = replyingToComment != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = VelvetRosePrimary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, VelvetRosePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Reply,
                                        contentDescription = null,
                                        tint = VelvetRosePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${replyingToComment?.authorName} ga javob berilyapti...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VelvetRosePrimary
                                    )
                                }
                                IconButton(
                                    onClick = { replyingToComment = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Bekor qilish", tint = VelvetRosePrimary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Comment Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentInputText,
                            onValueChange = { commentInputText = it },
                            placeholder = {
                                Text(
                                    if (replyingToComment != null) "Javobingizni yozing..." else "Fikr yoki izoh qoldiring...",
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelvetRosePrimary,
                                unfocusedBorderColor = if (isDark) Color(0xFF2E334D) else Color(0xFFE2E8F0),
                                focusedContainerColor = if (isDark) Color(0xFF10121C) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF10121C) else Color(0xFFF8FAFC)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (commentInputText.isNotBlank()) {
                                        userRepository.addAnnouncementComment(
                                            announcementId = annId,
                                            text = commentInputText,
                                            parentCommentId = replyingToComment?.id,
                                            replyToAuthorName = replyingToComment?.authorName
                                        )
                                        commentInputText = ""
                                        replyingToComment = null
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_announcement_comment")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (commentInputText.isNotBlank()) {
                                    userRepository.addAnnouncementComment(
                                        announcementId = annId,
                                        text = commentInputText,
                                        parentCommentId = replyingToComment?.id,
                                        replyToAuthorName = replyingToComment?.authorName
                                    )
                                    commentInputText = ""
                                    replyingToComment = null
                                }
                            },
                            enabled = commentInputText.isNotBlank(),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (commentInputText.isNotBlank()) VelvetRosePrimary else Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Yuborish",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet: Yangi E'lon Berish Dialogi
    if (showCreateDialog) {
        CreateAnnouncementModalSheet(
            userRepository = userRepository,
            isDark = isDark,
            onDismiss = { showCreateDialog = false },
            onPosted = {
                showCreateDialog = false
                Toast.makeText(context, "E'loningiz muvaffaqiyatli joylandi! ✨", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AnnouncementItemCard(
    announcement: Announcement,
    isDark: Boolean,
    cardBgColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onLike: () -> Unit,
    onOpenAuthor: () -> Unit,
    onOpenCommentSheet: () -> Unit,
    onCall: (String) -> Unit,
    onTelegram: (String) -> Unit
) {
    val categoryGradient = when (announcement.category) {
        "Sovchilik" -> listOf(Color(0xFFBE123C), Color(0xFFFB7185))
        "To'y & Nikoh" -> listOf(Color(0xFF854D0E), Color(0xFFFBBF24))
        "Tavsiyalar" -> listOf(Color(0xFF065F46), Color(0xFF34D399))
        else -> listOf(Color(0xFF1E40AF), Color(0xFF60A5FA))
    }

    val totalCommentsCount = announcement.comments.size + announcement.comments.sumOf { it.replies.size }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) WarmChocoGlassDark else Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(
            1.dp,
            if (isDark) Brush.linearGradient(listOf(SoftRosePink.copy(alpha = 0.38f), WarmPeach.copy(alpha = 0.32f), WarmHoneyGold.copy(alpha = 0.22f)))
            else Brush.linearGradient(listOf(Color(0x28F43F5E), Color(0x20FF8A65)))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("announcement_item_${announcement.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Muallif qatori (Tapping opens Full Profile)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onOpenAuthor() }
                        .testTag("author_avatar_${announcement.id}")
                ) {
                    ProfileAvatar(
                        photoPath = announcement.authorPhotoPath ?: "",
                        gender = announcement.authorGender,
                        size = 46.dp,
                        showCameraBadge = false
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenAuthor() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = announcement.authorName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = textPrimary
                        )
                        // Verified badge ONLY if subscription/VIP is active!
                        if (announcement.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "VIP Tasdiqlangan",
                                tint = WarmGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = announcement.region,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${announcement.timestamp}",
                            fontSize = 11.sp,
                            color = textSecondary.copy(alpha = 0.8f)
                        )
                    }
                }

                // Toifa belgisi (Category Pill)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = categoryGradient[0].copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, categoryGradient[0].copy(alpha = 0.3f))
                ) {
                    Text(
                        text = announcement.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryGradient[0],
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sarlavha
            Text(
                text = announcement.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tavsif matni
            Text(
                text = announcement.description,
                fontSize = 14.sp,
                color = textSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Aloqa tugmalari
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (announcement.contactPhone.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCall(announcement.contactPhone) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Qo'ng'iroq",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }

                if (announcement.telegramUsername.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTelegram(announcement.telegramUsername) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TelegramBrandIcon(size = 16.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Telegram",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = if (isDark) Color(0xFF24283B) else Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Pastki Harakatlar qatori (Like, Comment, Profil ochish)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Like tugmasi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLike() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (announcement.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (announcement.isLiked) VelvetRosePrimary else textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${announcement.likesCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (announcement.isLiked) VelvetRosePrimary else textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Comment tugmasi (Opens comments bottom sheet)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenCommentSheet() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Kommentlar",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (totalCommentsCount > 0) "$totalCommentsCount ta izoh" else "Fikr bildirish",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )
                    }
                }

                // Profilni ko'rish tugmasi
                TextButton(
                    onClick = onOpenAuthor,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Profil 👤 ↗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetRosePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItemRow(
    comment: AnnouncementComment,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onOpenAuthor: () -> Unit,
    onOpenReplyAuthor: (String) -> Unit,
    onLikeReply: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Asosiy komment
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.clickable { onOpenAuthor() }) {
                ProfileAvatar(
                    photoPath = comment.authorPhotoPath ?: "",
                    gender = comment.authorGender,
                    size = 36.dp,
                    showCameraBadge = false
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1E2132) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenAuthor() }
                        ) {
                            Text(
                                text = comment.authorName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            if (comment.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Tasdiqlangan",
                                    tint = WarmGold,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = comment.text,
                            fontSize = 13.sp,
                            color = textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Comment Actions (Vaqt, Like, Otvet)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                ) {
                    Text(
                        text = comment.timestamp,
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (comment.likesCount > 0) "Yoqdi (${comment.likesCount})" else "Yoqdi",
                        fontSize = 11.sp,
                        fontWeight = if (comment.isLiked) FontWeight.Bold else FontWeight.Medium,
                        color = if (comment.isLiked) VelvetRosePrimary else textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onLike() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Javob berish",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetRosePrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onReply() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Ichki javoblar (Replies / Otvetlar)
        if (comment.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                comment.replies.forEach { reply ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.clickable { onOpenReplyAuthor(reply.authorId) }) {
                            ProfileAvatar(
                                photoPath = reply.authorPhotoPath ?: "",
                                gender = reply.authorGender,
                                size = 28.dp,
                                showCameraBadge = false
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF24283D) else Color(0xFFE2E8F0).copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { onOpenReplyAuthor(reply.authorId) }
                                    ) {
                                        Text(
                                            text = reply.authorName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        if (reply.isVerified) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = WarmGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        if (reply.replyToAuthorName != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "▶ @${reply.replyToAuthorName}",
                                                fontSize = 10.sp,
                                                color = VelvetRosePrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = reply.text,
                                        fontSize = 12.sp,
                                        color = textPrimary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            ) {
                                Text(
                                    text = reply.timestamp,
                                    fontSize = 10.sp,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (reply.likesCount > 0) "Yoqdi (${reply.likesCount})" else "Yoqdi",
                                    fontSize = 10.sp,
                                    fontWeight = if (reply.isLiked) FontWeight.Bold else FontWeight.Medium,
                                    color = if (reply.isLiked) VelvetRosePrimary else textSecondary,
                                    modifier = Modifier
                                        .clickable { onLikeReply(reply.id) }
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnouncementModalSheet(
    userRepository: UserRepository,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPosted: () -> Unit
) {
    val currentUser by userRepository.currentUser.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Sovchilik") }
    var selectedRegion by remember { mutableStateOf(currentUser?.viloyat ?: "Toshkent shahri") }
    var phone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var telegram by remember { mutableStateOf(currentUser?.telegram ?: "") }

    val categories = listOf("Sovchilik", "To'y & Nikoh", "Tavsiyalar", "Xizmatlar")
    val regions = UzbekistanData.regions.map { it.name }

    val isValid = title.isNotBlank() && description.isNotBlank() && (phone.isNotBlank() || telegram.isNotBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF171A27) else Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
        ) {
            Text(
                text = "Yangi E'lon Berish ✨",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = "E'loningiz barcha foydalanuvchilarga ko'rinadi",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Toifa tanlash
            Text(
                text = "Toifani tanlang *",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) VelvetRosePrimary else if (isDark) Color(0xFF23273A) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) VelvetRosePrimary else Color.Transparent),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sarlavha
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("E'lon sarlavhasi *") },
                placeholder = { Text("Masalan: Jiddiy niyatda tanishuv") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_ann_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Batafsil matn
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Batafsil ma'lumot *") },
                placeholder = { Text("O'zingiz va talablaringiz haqida yozing...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_ann_desc")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Telefon va Telegram
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon raqami") },
                    placeholder = { Text("+998 90 123 45 67") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_new_ann_phone")
                )

                OutlinedTextField(
                    value = telegram,
                    onValueChange = { telegram = it },
                    label = { Text("Telegram username") },
                    placeholder = { Text("@username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_new_ann_tg")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isValid) {
                        userRepository.postAnnouncement(
                            title = title,
                            description = description,
                            category = selectedCategory,
                            region = selectedRegion,
                            contactPhone = phone,
                            telegram = telegram
                        )
                        onPosted()
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_submit_new_announcement")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("E'lonni Joylashtirish", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
