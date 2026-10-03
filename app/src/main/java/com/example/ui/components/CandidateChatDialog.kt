package com.example.ui.components

import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.util.AudioPlaybackManager
import com.example.util.AudioRecorderHelper
import com.example.util.ChatModerationService
import com.example.util.ImageModerationService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.ChatMessage
import com.example.model.ModerationSeverity
import com.example.model.ModerationTicket
import com.example.model.TicketStatus
import com.example.model.UserPost
import com.example.model.UserProfile
import com.example.ui.screens.CandidateDetailFullScreenDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LUXURY INSTAGRAM DIRECT & TELEGRAM VIP CHAT EXPERIENCE
 * - Profile Click: Tapping top bar profile opens full candidate details automatically
 * - Contact Nickname: Save / rename candidate contact name (Saxranit qilish)
 * - Borderless Reels: Shared Reels are displayed full-bleed and open directly in full player
 * - Real Voice Notes (Golos): Record with live timer, audio waveform, and play in chat bubbles
 * - Real-time animated typing status & online indicator
 * - Wallpaper customization & in-app calling simulation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidateChatDialog(
    candidate: UserProfile,
    userRepository: UserRepository,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onOpenReel: ((UserPost) -> Unit)? = null,
    onOpenCandidateProfile: ((UserProfile) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val audioRecorderHelper = remember { AudioRecorderHelper() }
    val allChatMessages by userRepository.chatMessages.collectAsState()
    val candidateWallpapers by userRepository.chatWallpapersByCandidate.collectAsState()
    val globalWallpaper by userRepository.chatWallpaperUri.collectAsState()
    val customContactNames by userRepository.customContactNames.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var onlineStatusText by remember { mutableStateOf(com.example.network.ChatRealtimeEngine.getUserOnlineStatus(candidate.id)) }
    var isCandidateTyping by remember { mutableStateOf(false) }

    // O'qilmagan xabarlarni ochilganda o'qilgan holatiga o'tkazish va xabarlar tarixini yuklash
    LaunchedEffect(candidate.id) {
        userRepository.getMessagesForCandidate(candidate.id)
        userRepository.markMessagesAsRead(candidate.id)
        com.example.network.ChatRealtimeEngine.setUserOnline(candidate.id, true)
        com.example.network.ChatRealtimeEngine.typingEventFlow.collect { event ->
            if (event.senderId == candidate.id) {
                isCandidateTyping = event.isTyping
            }
        }
    }

    // Chat ochiq turganda yangi xabar kelishi bilan uni o'qilgan deb belgilash
    LaunchedEffect(allChatMessages[candidate.id]?.size) {
        userRepository.markMessagesAsRead(candidate.id)
    }

    LaunchedEffect(inputText) {
        userRepository.sendTypingStatus(candidate.id, inputText.isNotBlank())
    }

    val isAccountDeleted = remember(candidate.id) { userRepository.isAccountDeleted(candidate.id) }
    // Saved custom nickname or candidate's real name
    val displayName = remember(customContactNames, candidate.id, candidate.fullName, isAccountDeleted) {
        if (isAccountDeleted) "Visol foydalanuvchisi"
        else customContactNames[candidate.id]?.takeIf { it.isNotBlank() } ?: candidate.fullName
    }

    // Chat orqa fon rasmi
    val wallpaperUri = remember(candidateWallpapers, globalWallpaper, candidate.id) {
        userRepository.getChatWallpaperUri(candidate.id)
    }

    val messages = allChatMessages[candidate.id] ?: emptyList()

    val listState = rememberLazyListState()

    // Real-time yangi xabar kelganda avtomatik eng pastga scroll qilish
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    val currentUserProfile = remember(userRepository.currentUser.value) {
        userRepository.currentUser.value ?: userRepository.getOrCreateActiveUser()
    }

    // Ikki bosqichli Chat Moderatsiyasi (Lokal filtr + Gemini AI tahlili & AI Hakam 3-shaxs)
    val handleSendMessage: (String) -> Unit = { rawText ->
        val textToSend = rawText.trim()
        if (textToSend.isNotBlank()) {
            inputText = ""
            coroutineScope.launch {
                val modResult = ChatModerationService.inspectChatMessage(
                    context = context,
                    senderUser = currentUserProfile,
                    receiverCandidate = candidate,
                    messageText = textToSend
                )
                if (!modResult.isApproved) {
                    // Mojaroning oldini olish (3-shaxs sxemasi):
                    // Qoidabuzar yozgan xabar suhbatdoshga (2-shaxsga) mutlaqo bormaydi.
                    // Qoidabuzarning o'ziga chat ichida 3-shaxs (AI Hakam) tomonidan maxfiy ogohlantirish chiqariladi.
                    userRepository.addAiHakamWarningMessage(
                        candidateId = candidate.id,
                        warningText = modResult.aiHakamWarning,
                        violationReason = modResult.violationReason
                    )
                    android.widget.Toast.makeText(
                        context,
                        "⚖️ AI Hakam: Xabar odob-axloq qoidalariga zidligi sababli bloklandi",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    userRepository.sendMessageToCandidate(candidate.id, textToSend)
                }
            }
        }
    }

    var selectedReactionMessageId by remember { mutableStateOf<String?>(null) }
    var showWallpaperSelector by remember { mutableStateOf(false) }
    var showStickerTray by remember { mutableStateOf(false) }
    var selectedStickerTab by remember { mutableIntStateOf(0) }
    var activeCallType by remember { mutableStateOf<String?>(null) } // "audio" or "video" or null
    var showFloatingHeart by remember { mutableStateOf(false) }

    // Modal dialogs
    var showFullCandidateProfile by remember { mutableStateOf(false) }
    var showRenameContactDialog by remember { mutableStateOf(false) }
    var renameInputText by remember(displayName) { mutableStateOf(displayName) }
    var previewingReelPost by remember { mutableStateOf<UserPost?>(null) }

    // Voice Note (Golos) Recording State
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordDurationSec by remember { mutableIntStateOf(0) }
    var liveAmplitudes by remember { mutableStateOf(listOf(8, 12, 16, 10, 14, 18, 12, 10)) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = audioRecorderHelper.startRecording(context)
            if (file != null) {
                isRecordingVoice = true
                android.widget.Toast.makeText(context, "🎙️ Ovoz yozish boshlandi...", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Mikrofonni ishga tushirib bo'lmadi", android.widget.Toast.LENGTH_SHORT).show()
                isRecordingVoice = false
            }
        } else {
            android.widget.Toast.makeText(context, "Ovoz yozish uchun mikrofonga ruxsat bering", android.widget.Toast.LENGTH_SHORT).show()
            isRecordingVoice = false
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordDurationSec = 0
            // Duration ticker
            launch {
                while (isRecordingVoice) {
                    delay(1000)
                    recordDurationSec++
                }
            }
            // Real microphone amplitude visualizer
            launch {
                while (isRecordingVoice) {
                    val amp = audioRecorderHelper.getMaxAmplitude()
                    val height = ((amp / 32767f) * 26f + 6f).toInt().coerceIn(6, 32)
                    liveAmplitudes = liveAmplitudes.drop(1) + height
                    delay(90)
                }
            }
        } else {
            liveAmplitudes = listOf(8, 12, 16, 10, 14, 18, 12, 10)
        }
    }

    // Yangi xabarlar kelganda o'qilgan deb belgilash va avtomatik pastga tushish (oxirgi xabarga)
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            userRepository.markMessagesAsRead(candidate.id)
            // Indeks messages.size chunki LazyColumn ichidagi 0-element Lock Banner hisoblanadi
            delay(100)
            listState.animateScrollToItem(messages.size)
        }
    }

    // Galereyadan chat fon rasmini tanlash
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            userRepository.setChatWallpaperForCandidate(candidate.id, uri.toString())
        }
    }

    // Galereyadan chat ichiga rasm yuborish (18+ AI Xavfsizlik & Telegram Moderatsiya bilan)
    val chatPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            coroutineScope.launch {
                val assessment = ImageModerationService.inspectMediaUri(context, uri, isVideo = false)
                if (assessment.severity != ModerationSeverity.SAFE || !assessment.isApproved) {
                    val ticket = ModerationTicket(
                        ticketId = "ticket_chat_${System.currentTimeMillis()}",
                        userId = currentUserProfile.id,
                        userName = currentUserProfile.fullName,
                        userUsername = currentUserProfile.username,
                        userPhotoPath = currentUserProfile.photoPath,
                        userGender = currentUserProfile.gender,
                        userLocation = "${currentUserProfile.viloyat}, ${currentUserProfile.tuman}",
                        mediaUri = uri.toString(),
                        mediaType = "IMAGE",
                        caption = if (inputText.isNotBlank()) inputText.trim() else "Chatda yuborilgan shubhali rasm",
                        severity = assessment.severity,
                        reason = assessment.reason,
                        confidence = assessment.confidence,
                        status = TicketStatus.PENDING,
                        timestamp = System.currentTimeMillis()
                    )
                    userRepository.createModerationTicket(ticket)
                    android.widget.Toast.makeText(
                        context,
                        "🚫 Chatda yuborilayotgan suratda noo'rin / 18+ kontent aniqlandi va rad etildi. Isbot Telegram adminga yuborildi!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    userRepository.sendPhotoToCandidate(
                        candidateId = candidate.id,
                        photoUri = uri.toString(),
                        caption = if (inputText.isNotBlank()) inputText.trim() else ""
                    )
                    inputText = ""
                    android.widget.Toast.makeText(context, "Surat xavfsiz tekshiruvdan o'tdi va yuborildi ✨", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Galereyadan chat ichiga video yuborish (18+ AI Xavfsizlik & Telegram Moderatsiya bilan)
    val chatVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            coroutineScope.launch {
                val assessment = ImageModerationService.inspectMediaUri(context, uri, isVideo = true)
                if (assessment.severity != ModerationSeverity.SAFE || !assessment.isApproved) {
                    val ticket = ModerationTicket(
                        ticketId = "ticket_chat_video_${System.currentTimeMillis()}",
                        userId = currentUserProfile.id,
                        userName = currentUserProfile.fullName,
                        userUsername = currentUserProfile.username,
                        userPhotoPath = currentUserProfile.photoPath,
                        userGender = currentUserProfile.gender,
                        userLocation = "${currentUserProfile.viloyat}, ${currentUserProfile.tuman}",
                        mediaUri = uri.toString(),
                        mediaType = "VIDEO",
                        caption = if (inputText.isNotBlank()) inputText.trim() else "Chatda yuborilgan shubhali video",
                        severity = assessment.severity,
                        reason = assessment.reason,
                        confidence = assessment.confidence,
                        status = TicketStatus.PENDING,
                        timestamp = System.currentTimeMillis()
                    )
                    userRepository.createModerationTicket(ticket)
                    android.widget.Toast.makeText(
                        context,
                        "🚫 Chatda yuborilayotgan videoda noo'rin / 18+ kontent aniqlandi va rad etildi. Isbot Telegram adminga yuborildi!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    userRepository.sendPhotoToCandidate(
                        candidateId = candidate.id,
                        photoUri = uri.toString(),
                        caption = if (inputText.isNotBlank()) inputText.trim() else "🎬 Video"
                    )
                    inputText = ""
                    android.widget.Toast.makeText(context, "Video xavfsiz tekshiruvdan o'tdi va yuborildi ✨", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val darkBgColor = WarmChocoPlumDark
    val darkSurfaceColor = Color(0xD8251A2D)
    val darkInputColor = Color(0xFF2E1F36)

    val presetWallpapers = listOf(
        "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
        "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = if (isDark) darkBgColor else PearlBackground,
            topBar = {
                // LUXURY DECORATED TOP BAR
                Surface(
                    color = if (isDark) darkSurfaceColor else Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    if (isDark) listOf(Color(0xFF33203D), Color(0xFF221529))
                                    else listOf(Color.White, Color(0xFFFFF7F5))
                                )
                            )
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Orqaga",
                                    tint = if (isDark) Color.White else TextPrimary
                                )
                            }

                            // Glowing Profile Avatar (Tapping opens Full Profile)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .padding(2.dp)
                                    .clickable {
                                        showFullCandidateProfile = true
                                        onOpenCandidateProfile?.invoke(candidate)
                                    }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(WarmGold, VelvetRosePrimary))),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    ProfileAvatar(
                                        photoPath = if (isAccountDeleted) "ic_deleted_user" else candidate.photoPath,
                                        gender = candidate.gender,
                                        size = 42.dp,
                                        showCameraBadge = false
                                    )
                                }
                                // Online Live Green Pulse
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                        .border(2.dp, if (isDark) darkSurfaceColor else Color.White, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Candidate Info Column (Tapping opens Full Profile)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showFullCandidateProfile = true
                                        onOpenCandidateProfile?.invoke(candidate)
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color.White else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (candidate.isAiHakam) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE11D48).copy(alpha = 0.2f),
                                            border = BorderStroke(0.6.dp, Color(0xFFE11D48))
                                        ) {
                                            Text(
                                                text = "⚖️ AI HAKAM",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE11D48),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else if (candidate.isOfficialAdmin) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = WarmGold.copy(alpha = 0.25f),
                                            border = BorderStroke(0.6.dp, WarmGold)
                                        ) {
                                            Text(
                                                text = "🛡️ ADMIN",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WarmGoldLight,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = WarmGold.copy(alpha = 0.2f),
                                            border = BorderStroke(0.6.dp, WarmGold)
                                        ) {
                                            Text(
                                                text = "👑 VIP",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WarmGoldLight,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (candidate.isAiHakam) "Xolis & Neytral Hakam • 24/7 Faol"
                                           else if (candidate.isOfficialAdmin) "Rasmiy Ma'muriyat • Qo'llab-quvvatlash"
                                           else if (isCandidateTyping) "✍️ Yozmoqda..."
                                           else "$onlineStatusText • ${candidate.viloyat}",
                                    fontSize = 11.sp,
                                    color = if (isCandidateTyping) VelvetRosePrimary else if (isDark) Color(0xFF94A3B8) else TextSecondary,
                                    maxLines = 1,
                                    fontWeight = if (isCandidateTyping) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // Contact Nickname / Rename Button ("Saxranit qilish")
                            IconButton(
                                onClick = {
                                    renameInputText = displayName
                                    showRenameContactDialog = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "Nomini saqlash",
                                    tint = WarmGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Action buttons: Audio Call, Video Call, Wallpaper toggle
                            IconButton(
                                onClick = { activeCallType = "audio" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Qo'ng'iroq",
                                    tint = VelvetRosePrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { activeCallType = "video" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = "Video qo'ng'iroq",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            IconButton(
                                onClick = { showWallpaperSelector = !showWallpaperSelector },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Fon",
                                    tint = if (showWallpaperSelector) VelvetRosePrimary else if (isDark) Color(0xFFCBD5E1) else TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Presets wallpaper slider
                        AnimatedVisibility(visible = showWallpaperSelector) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color(0xFF1E212E) else Color(0xFFF1F5F9))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎨 Chat orqa foni",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else TextPrimary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_PICK,
                                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                                )
                                                wallpaperLauncher.launch(intent)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("🖼️ Galereya", fontSize = 11.sp, color = VelvetRosePrimary)
                                        }
                                        if (wallpaperUri != null) {
                                            TextButton(
                                                onClick = { userRepository.setChatWallpaperForCandidate(candidate.id, null) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("O'chirish", fontSize = 11.sp, color = Color.Red)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(presetWallpapers) { preset ->
                                        Box(
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (wallpaperUri == preset) 2.dp else 0.5.dp,
                                                    color = if (wallpaperUri == preset) WarmGold else Color.Gray,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    userRepository.setChatWallpaperForCandidate(candidate.id, preset)
                                                }
                                        ) {
                                            AsyncImage(
                                                model = preset,
                                                contentDescription = "Preset wallpaper",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // LUXURY DECORATED FROSTED GLASS CHAT BAR
                Surface(
                    color = if (isDark) darkSurfaceColor else Color.White,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    if (isDark) listOf(Color(0xFF2C1C35), Color(0xFF1E1325))
                                    else listOf(Color.White, Color(0xFFFFF7F5))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(bottom = 6.dp)
                    ) {
                        // Luminous border accent
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            WarmHoneyGold.copy(alpha = 0.5f),
                                            SoftRosePink,
                                            WarmPeach.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                        )

                        // Quick Reaction Emojis & VIP Conversation Starters
                        if (!isRecordingVoice && !showStickerTray) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val quickStarters = listOf(
                                    "Assalomu alaykum 🌸",
                                    "Qayerdansiz? 📍",
                                    "Niyatlaringiz jiddiymi? 💍",
                                    "Rahmat katta 😊",
                                    "Tanishsak bo'ladimi? ✨"
                                )
                                items(quickStarters) { phrase ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDark) Color(0xFF2A2E40) else Color(0xFFF1F5F9),
                                        border = BorderStroke(0.6.dp, if (isDark) Color(0xFF3E445E) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.clickable {
                                            inputText = phrase
                                        }
                                    ) {
                                        Text(
                                            text = phrase,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color(0xFFE2E8F0) else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Live Voice Recording Banner
                        AnimatedVisibility(visible = isRecordingVoice) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .background(
                                        if (isDark) Color(0xFF2C1E26) else Color(0xFFFFF1F2),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Blinking red recording circle
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red.copy(alpha = alpha))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format("%02d:%02d", recordDurationSec / 60, recordDurationSec % 60),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    // Real-time microphone amplitude wave bars
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                        liveAmplitudes.forEach { barHeight ->
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(barHeight.dp)
                                                    .background(VelvetRosePrimary, RoundedCornerShape(2.dp))
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Cancel button
                                    IconButton(
                                        onClick = {
                                            audioRecorderHelper.cancelRecording()
                                            isRecordingVoice = false
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Bekor qilish",
                                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }

                                    // Send voice note button
                                    Surface(
                                        shape = CircleShape,
                                        color = VelvetRosePrimary,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                val recordedFile = audioRecorderHelper.stopRecording()
                                                if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 0) {
                                                    val audioUriString = Uri.fromFile(recordedFile).toString()
                                                    userRepository.sendVoiceNoteToCandidate(
                                                        candidate.id,
                                                        audioUri = audioUriString,
                                                        durationSec = maxOf(1, recordDurationSec)
                                                    )
                                                    android.widget.Toast.makeText(context, "🎙️ Ovozli xabar yuborildi", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "Ovoz yozilmadi yoki juda qisqa bo'ldi", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                isRecordingVoice = false
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Ovozni yuborish",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3D Sticker Picker Tray Modal
                        AnimatedVisibility(visible = showStickerTray && !isRecordingVoice) {
                            val categories = listOf(
                                "💕 Sovchilik & Muhabbat" to listOf("💖 Oq Ko'ngil", "💍 Oltin Uzuk", "🌹 Qizil Gul", "👰 Kelinchak", "🤵 Kuyovto'ra", "🏰 Nikoh Qasri", "🕊️ Tinchlik Kabutari", "✨ Sevgi Nuri"),
                                "🇺🇿 O'zbekona & Milliy" to listOf("🫖 Samarqand Chaynigi", "🍏 Farg'ona Olmasi", "🍞 Toshkent Noni", "👑 Buxoro Toji", "🎻 Surnay Sadosi", "🎨 Naqshinkor Kosa", "🎁 To'y Sovg'asi", "🌟 Omad Yulduzi"),
                                "😍 Emoticonlar & Reaksiyalar" to listOf("🥰 Mehr", "🔥 Olovli", "💎 Olmos", "🌺 Gulchehra", "🎉 Boshlanish", "🍫 Shirinlik", "🌸 Bahor", "🍰 Shirin Hayot")
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            if (isDark) listOf(Color(0xFF23192B), Color(0xFF181020))
                                            else listOf(Color(0xFFFFF5F5), Color(0xFFF1F5F9))
                                        )
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        brush = Brush.horizontalGradient(listOf(WarmGold, VelvetRosePrimary, WarmHoneyGold)),
                                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                // 3D Header Title
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "✨ 3D INTERFAOL STIKERLAR", fontSize = 13.sp, fontWeight = FontWeight.Black, color = WarmGoldLight)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = VelvetRosePrimary.copy(alpha = 0.2f),
                                            border = BorderStroke(0.6.dp, VelvetRosePrimary)
                                        ) {
                                            Text("3D PRO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = VelvetRosePrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = { showStickerTray = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish", tint = if (isDark) Color.White else TextPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Category tabs
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    items(categories.size) { index ->
                                        val catName = categories[index].first
                                        val isSelected = selectedStickerTab == index
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) VelvetRosePrimary else (if (isDark) Color(0xFF2D3245) else Color(0xFFE2E8F0)),
                                            border = BorderStroke(1.dp, if (isSelected) WarmGold else Color.Transparent),
                                            shadowElevation = if (isSelected) 6.dp else 1.dp,
                                            modifier = Modifier.clickable { selectedStickerTab = index }
                                        ) {
                                            Text(
                                                text = catName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else TextPrimary),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                // Stickers grid with 3D depth and radial light reflection
                                val currentStickers = categories.getOrNull(selectedStickerTab)?.second ?: emptyList()
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    items(currentStickers) { stickerItem ->
                                        val emojiPart = stickerItem.takeWhile { !it.isLetter() }.trim()
                                        val textPart = stickerItem.dropWhile { !it.isLetter() }.trim()

                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color.Transparent,
                                            border = BorderStroke(
                                                1.2.dp,
                                                Brush.linearGradient(listOf(WarmGold, WarmGoldLight, VelvetRosePrimary))
                                            ),
                                            shadowElevation = 8.dp,
                                            modifier = Modifier
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = if (isDark) listOf(Color(0xFF3B2745), Color(0xFF1F1528))
                                                        else listOf(Color.White, Color(0xFFFFF0F3))
                                                    ),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .clickable {
                                                    userRepository.sendMessageToCandidate(candidate.id, stickerItem)
                                                    showStickerTray = false
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Floating 3D Emoji Icon
                                                Text(
                                                    text = emojiPart,
                                                    fontSize = 34.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.Black.copy(alpha = 0.25f)
                                                ) {
                                                    Text(
                                                        text = textPart,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isDark) WarmGoldLight else VelvetRosePrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Main Input Bar Row
                        if (!isRecordingVoice) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 3D Sticker Tray button with 3D Pill Badge
                                Box(contentAlignment = Alignment.TopEnd) {
                                    IconButton(
                                        onClick = { showStickerTray = !showStickerTray },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (showStickerTray) VelvetRosePrimary else if (isDark) Color(0xFF382343) else Color(0xFFFFF0F3),
                                            border = BorderStroke(1.dp, WarmGold),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "🎭",
                                                    fontSize = 20.sp
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = WarmGold,
                                        modifier = Modifier.offset(x = 2.dp, y = (-2).dp)
                                    ) {
                                        Text("3D", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                }

                                // Photo attach button
                                IconButton(
                                    onClick = {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_PICK,
                                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                        )
                                        chatPhotoLauncher.launch(intent)
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Rasm yuborish",
                                        tint = VelvetRosePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Text field
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            "Xabar yozing...",
                                            fontSize = 14.sp,
                                            color = if (isDark) Color(0xFF81889C) else Color(0xFF94A3B8)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp, max = 120.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Send
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (inputText.isNotBlank()) {
                                                handleSendMessage(inputText)
                                            }
                                        }
                                    ),
                                    trailingIcon = {
                                        if (inputText.isNotBlank()) {
                                            IconButton(onClick = { inputText = "" }) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Tozalash",
                                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VelvetRosePrimary,
                                        unfocusedBorderColor = if (isDark) Color(0xFF454B62) else Color(0xFFCBD5E1),
                                        focusedContainerColor = if (isDark) darkInputColor else Color.White,
                                        unfocusedContainerColor = if (isDark) darkInputColor else Color.White,
                                        focusedTextColor = if (isDark) Color.White else TextPrimary,
                                        unfocusedTextColor = if (isDark) Color.White else TextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Dinamik Yuborish / Ovozli Xabar tugmasi (Send / Voice Note)
                                val isTyping = inputText.isNotBlank()
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    shadowElevation = if (isTyping) 6.dp else 2.dp,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isTyping) {
                                                Brush.linearGradient(listOf(VelvetRosePrimary, Color(0xFFE11D48), WarmGold))
                                            } else {
                                                Brush.linearGradient(
                                                    if (isDark) listOf(Color(0xFF3B4055), Color(0xFF2C3042))
                                                    else listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                                                )
                                            }
                                        )
                                        .clickable {
                                            if (isTyping) {
                                                handleSendMessage(inputText)
                                            } else {
                                                val permissionStatus = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context,
                                                    android.Manifest.permission.RECORD_AUDIO
                                                )
                                                if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    val file = audioRecorderHelper.startRecording(context)
                                                    if (file != null) {
                                                        isRecordingVoice = true
                                                        android.widget.Toast.makeText(context, "🎙️ Ovoz yozish boshlandi...", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Mikrofonni ishga tushirib bo'lmadi", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        }
                                        .testTag("btn_send_chat_message")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isTyping) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Yuborish",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Ovozli xabar",
                                                tint = if (isDark) Color.White else VelvetRosePrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .clickable { selectedReactionMessageId = null }
            ) {
                // Orqa fon rasmi (Wallpaper with soft overlay)
                if (!wallpaperUri.isNullOrBlank()) {
                    AsyncImage(
                        model = wallpaperUri,
                        contentDescription = "Chat Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isDark) Color.Black.copy(alpha = 0.65f)
                                else Color.Black.copy(alpha = 0.2f)
                            )
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Xavfsiz shaxsiy chat himoyalanganlik e'loni
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isDark) Color(0xFF242738).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(WarmGold.copy(alpha = 0.5f), VelvetRosePrimary.copy(alpha = 0.4f)))),
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = WarmGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Visol Direct: Shifrlangan shaxsiy suhbat 🔒",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Chat xabarlari ro'yxati
                    items(messages, key = { it.id }) { message ->
                        val currentUserId = userRepository.currentUserId
                        val isMe = message.isFromMe || message.senderId == currentUserId || 
                                   ((currentUserId == "me" || currentUserId == "user_me") && (message.senderId == "me" || message.senderId == "user_me"))
                        val isReactionMenuOpen = selectedReactionMessageId == message.id

                        ChatBubbleItem(
                            message = message,
                            isMe = isMe,
                            candidate = candidate,
                            isDark = isDark,
                            isReactionMenuOpen = isReactionMenuOpen,
                            onToggleReactionMenu = {
                                selectedReactionMessageId = if (isReactionMenuOpen) null else message.id
                            },
                            onSelectReaction = { emoji ->
                                userRepository.reactToMessage(candidate.id, message.id, emoji)
                                selectedReactionMessageId = null
                            },
                            onDoubleTapLike = {
                                userRepository.reactToMessage(candidate.id, message.id, "❤️")
                                coroutineScope.launch {
                                    showFloatingHeart = true
                                    delay(900)
                                    showFloatingHeart = false
                                }
                            },
                            onOpenReel = { reel ->
                                previewingReelPost = reel
                                onOpenReel?.invoke(reel)
                            }
                        )
                    }
                }

                // Double Tap Floating Love Heart Animation
                AnimatedVisibility(
                    visible = showFloatingHeart,
                    enter = scaleIn(initialScale = 0.3f) + fadeIn(),
                    exit = scaleOut(targetScale = 1.5f) + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = "❤️",
                        fontSize = 72.sp
                    )
                }
            }
        }

        // 1. CANDIDATE FULL PROFILE MODAL (Opened by tapping top profile)
        if (showFullCandidateProfile) {
            CandidateDetailFullScreenDialog(
                candidate = candidate,
                userRepository = userRepository,
                isSubscribed = true,
                isDark = isDark,
                onDismiss = { showFullCandidateProfile = false },
                onRequireSubscription = {}
            )
        }

        // 2. CONTACT RENAME / SAVE DIALOG ("2-shaxsni saxranit qilish")
        if (showRenameContactDialog) {
            val hasCustomName = customContactNames[candidate.id]?.isNotBlank() == true
            AlertDialog(
                onDismissRequest = { showRenameContactDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏷️ Nom qo'yish (Saxranit)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Ushbu nomzod uchun o'zingizga qulay maxsus nom yoki taxallus kiriting (Sms bildirishnoma va chatlarda shu nom ko'rinadi):",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = renameInputText,
                            onValueChange = { renameInputText = it },
                            placeholder = { Text("Masalan: Mening juftim 🌹, Jonim") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (hasCustomName) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Asl ismi: ${candidate.fullName}",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasCustomName) {
                            OutlinedButton(
                                onClick = {
                                    userRepository.setCustomContactName(candidate.id, "")
                                    renameInputText = candidate.fullName
                                    showRenameContactDialog = false
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Red
                                ),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                            ) {
                                Text("O'chirish")
                            }
                        }
                        Button(
                            onClick = {
                                userRepository.setCustomContactName(candidate.id, renameInputText)
                                showRenameContactDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary)
                        ) {
                            Text("Saqlash", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRenameContactDialog = false
                        }
                    ) {
                        Text("Bekor qilish")
                    }
                }
            )
        }

        // 3. FULL SCREEN REEL VIEWER (Directly playable when tapping shared Reel)
        if (previewingReelPost != null) {
            Dialog(
                onDismissRequest = { previewingReelPost = null },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    val reel = previewingReelPost!!
                    ReelVideoPlayerView(
                        videoUriString = reel.mediaUri,
                        isPlaying = true,
                        backgroundAudioUri = reel.musicAudioUrl,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Header Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { previewingReelPost = null },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Orqaga",
                                tint = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = "Reels Video",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Bottom Author info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "@${reel.authorName}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reel.title,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 4. SIMULATED IN-APP CALL DIALOG
        if (activeCallType != null) {
            InAppCallDialog(
                candidate = candidate,
                callType = activeCallType!!,
                isDark = isDark,
                onEndCall = { activeCallType = null }
            )
        }
    }
}

/**
 * DECORATED CHAT BUBBLE ITEM (Chat Ramkasi)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isMe: Boolean,
    candidate: UserProfile,
    isDark: Boolean,
    isReactionMenuOpen: Boolean,
    onToggleReactionMenu: () -> Unit,
    onSelectReaction: (String) -> Unit,
    onDoubleTapLike: () -> Unit,
    onOpenReel: ((UserPost) -> Unit)? = null
) {
    if (message.isAiHakamWarning) {
        // AI Hakam Maxfiy Ogohlantirish Kartasi (3-shaxs sxemasi)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF2D151E) else Color(0xFFFFF1F2),
                border = BorderStroke(1.2.dp, Color(0xFFE11D48)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(0.94f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE11D48),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "⚖️", fontSize = 13.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "⚖️ AI HAKAM — MAXFIY OGOHLANTIRISH",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFE11D48)
                            )
                            Text(
                                text = "🔒 Faqat sizga ko'rinadi (Suhbatdoshga bormadi)",
                                fontSize = 9.sp,
                                color = if (isDark) Color(0xFFFDA4AF) else Color(0xFF9F1239)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.text,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
                    if (!message.aiHakamReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🛡️ Sabab: ${message.aiHakamReason}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mojaroning oldini olish tizimi • ${formatChatTime(message.timestamp)}",
                        fontSize = 9.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        return
    }

    val align = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        // Dynamic floating 3D reaction selector above message
        AnimatedVisibility(
            visible = isReactionMenuOpen,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF2C3044) else Color.White,
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(WarmGold, VelvetRosePrimary))),
                shadowElevation = 10.dp,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val reactions = listOf("❤️", "🌹", "💍", "😍", "🤲", "✨", "🔥", "👍")
                    reactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable { onSelectReaction(emoji) }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isMe) {
                // Mini candidate avatar beside received bubble
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .padding(end = 4.dp, bottom = 2.dp)
                ) {
                    ProfileAvatar(
                        photoPath = candidate.photoPath,
                        gender = candidate.gender,
                        size = 28.dp,
                        showCameraBadge = false
                    )
                }
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    ),
                    color = Color.Transparent,
                    shadowElevation = if (isMe) 4.dp else 2.dp,
                    border = BorderStroke(
                        width = if (isMe) 1.dp else 0.8.dp,
                        brush = if (isMe) {
                            Brush.linearGradient(listOf(Color(0xFFFDA4AF), WarmGoldLight))
                        } else {
                            if (isDark) Brush.horizontalGradient(listOf(Color(0xFF3F455A), WarmGold.copy(alpha = 0.35f)))
                            else Brush.horizontalGradient(listOf(VelvetRoseSoft.copy(alpha = 0.55f), WarmGold.copy(alpha = 0.45f)))
                        }
                    ),
                    modifier = Modifier
                        .background(
                            if (isMe) {
                                Brush.linearGradient(
                                    listOf(
                                        VelvetRosePrimary,
                                        Color(0xFFE11D48),
                                        Color(0xFFBE123C)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    if (isDark) listOf(Color(0xFF262A3B), Color(0xFF1E212E))
                                    else listOf(Color.White, Color(0xFFF9FAFB))
                                )
                            },
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isMe) 18.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 18.dp
                            )
                        )
                        .combinedClickable(
                            onClick = { /* normal tap */ },
                            onLongClick = onToggleReactionMenu,
                            onDoubleClick = onDoubleTapLike
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(
                                if (message.sharedReel != null) 3.dp else 12.dp
                            )
                            .widthIn(max = 300.dp)
                    ) {
                        // 1. Agar Rasm biriktirilgan bo'lsa
                        if (!message.photoUri.isNullOrBlank()) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isMe) Color(0x44FFFFFF) else Color(0x22000000)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .padding(bottom = 6.dp)
                            ) {
                                AsyncImage(
                                    model = message.photoUri,
                                    contentDescription = "Chat Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // 2. Agar Reels video ulashilgan bo'lsa (TALL 9:16 VERTICAL REELS CARD)
                        if (message.sharedReel != null) {
                            val reel = message.sharedReel
                            Box(
                                modifier = Modifier
                                    .width(195.dp)
                                    .height(325.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenReel?.invoke(reel) }
                            ) {
                                if (reel.mediaUri.isNotBlank()) {
                                    AsyncImage(
                                        model = reel.mediaUri,
                                        contentDescription = "Reel preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(VelvetRoseDark, Color(0xFF1E1B4B))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(52.dp)
                                        )
                                    }
                                }

                                // Gradient shade overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Black.copy(alpha = 0.35f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.88f)
                                                )
                                            )
                                        )
                                )

                                // Reels Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    border = BorderStroke(0.5.dp, WarmGold),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.VideoLibrary,
                                            contentDescription = null,
                                            tint = WarmGoldLight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reels",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Center play icon
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        .align(Alignment.Center),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Ko'rish",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Bottom metadata
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "@${reel.authorName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WarmGoldLight
                                    )
                                    Text(
                                        text = reel.title,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 3. Ovozli xabar (Voice Note) Player Frame
                        if (message.text.startsWith("🎙️") || message.audioDurationSec != null || !message.audioUri.isNullOrBlank()) {
                            val context = LocalContext.current
                            val coroutineScope = rememberCoroutineScope()
                            var isPlayingVoice by remember { mutableStateOf(false) }
                            var playbackProgress by remember { mutableFloatStateOf(0f) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isMe) Color.White.copy(alpha = 0.25f) else VelvetRosePrimary,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable {
                                            if (isPlayingVoice) {
                                                AudioPlaybackManager.stopAudio()
                                                isPlayingVoice = false
                                                playbackProgress = 0f
                                            } else {
                                                isPlayingVoice = true
                                                AudioPlaybackManager.playAudio(
                                                    context = context,
                                                    audioUriString = message.audioUri,
                                                    coroutineScope = coroutineScope,
                                                    onProgress = { progress ->
                                                        playbackProgress = progress
                                                    },
                                                    onCompletion = {
                                                        isPlayingVoice = false
                                                        playbackProgress = 0f
                                                    }
                                                )
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlayingVoice) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Ovozni tinglash",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Dynamic animated waveform
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val bars = listOf(8, 14, 22, 10, 18, 26, 12, 20, 16, 24, 10, 18, 12, 6)
                                        bars.forEachIndexed { i, h ->
                                            val barFraction = i.toFloat() / bars.size
                                            val isPlayed = barFraction <= playbackProgress
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(h.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        if (isMe) {
                                                            if (isPlayed) Color.White else Color.White.copy(alpha = 0.4f)
                                                        } else {
                                                            if (isPlayed) VelvetRosePrimary else if (isDark) Color(0xFF64748B) else Color(0xFFCBD5E1)
                                                        }
                                                    )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "🎙️ Ovozli xabar",
                                            fontSize = 10.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.8f) else if (isDark) Color(0xFFCBD5E1) else TextSecondary
                                        )
                                        Text(
                                            text = "0:${String.format("%02d", message.audioDurationSec ?: 12)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMe) Color.White else if (isDark) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        } else if (message.sharedReel == null) {
                            // 4. 3D Stiker yoki Matnli xabar
                            val stickerEmojisList = listOf("💖", "💍", "🌹", "👰", "🤵", "🏰", "🕊️", "✨", "🫖", "🍏", "🍞", "👑", "🎻", "🎨", "🎁", "🌟", "🥰", "🔥", "💎", "🌺", "🎉", "🍫", "🌸", "🍰")
                            val isSticker = stickerEmojisList.any { message.text.trim().startsWith(it) } && message.text.length < 35

                            if (isSticker) {
                                Render3DStickerMessage(text = message.text, isMe = isMe, isDark = isDark)
                            } else {
                                Text(
                                    text = message.text,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    color = if (isMe) Color.White else if (isDark) Color(0xFFF1F5F9) else TextPrimary,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Timestamp va O'qilganlik nishoni (Read Receipts)
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = formatChatTime(message.timestamp),
                                fontSize = 10.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.75f) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            if (isMe) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                    contentDescription = if (message.isRead) "O'qildi" else "Yetib bordi",
                                    tint = if (message.isRead) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                // 3D Floating Reaction Badge on Message Bubble Corner
                if (!message.reaction.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E212E) else Color.White,
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.7f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(if (isMe) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(x = if (isMe) (-6).dp else 6.dp, y = 8.dp)
                    ) {
                        Text(
                            text = message.reaction,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * IN-APP SIMULATED AUDIO & VIDEO CALL MODAL
 */
@Composable
fun InAppCallDialog(
    candidate: UserProfile,
    callType: String,
    isDark: Boolean,
    onEndCall: () -> Unit
) {
    var callSeconds by remember { mutableIntStateOf(0) }
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2500) // 2.5 soniya chaqiriq (ringing)
        isConnected = true
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (callType == "video") listOf(Color(0xFF0F172A), Color(0xFF020617))
                        else listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF831843))
                    )
                )
        ) {
            // Video call camera background simulation
            if (callType == "video" && isConnected) {
                AsyncImage(
                    model = candidate.photoPath,
                    contentDescription = "Candidate Video Feed",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (callType == "video") "📹 Visol HD Video Qo'ng'iroq" else "📞 Visol Xavfsiz Audio Qo'ng'iroq",
                        color = WarmGoldLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = candidate.fullName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!isConnected) "Chaqirilmoqda (Jiringlamoqda)..." else "Ulangan • ${String.format("%02d:%02d", callSeconds / 60, callSeconds % 60)}",
                        color = if (!isConnected) Color(0xFF38BDF8) else Color(0xFF4ADE80),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Center Avatar with pulsing rings
                if (callType == "audio" || !isConnected) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(3.dp, Brush.linearGradient(listOf(WarmGold, VelvetRosePrimary))),
                            modifier = Modifier.size(130.dp)
                        ) {
                            ProfileAvatar(
                                photoPath = candidate.photoPath,
                                gender = candidate.gender,
                                size = 124.dp,
                                showCameraBadge = false
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Bottom Call Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* mute mic */ },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.MicOff, contentDescription = "Ovozni o'chirish", tint = Color.White)
                    }

                    // End Call button (Red)
                    FloatingActionButton(
                        onClick = onEndCall,
                        containerColor = Color(0xFFEF4444),
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Qo'ng'iroqni tugatish",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* speaker */ },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Karnay", tint = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Format chat message timestamps (e.g., "14:25")
 */
fun formatChatTime(timestamp: String): String {
    return timestamp.ifBlank { "Hozir" }
}

/**
 * 3D Hologram Style Animated Sticker Display
 */
@Composable
fun Render3DStickerMessage(text: String, isMe: Boolean, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "sticker3D")

    // Floating animation offsetY
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    // Glowing aura pulse
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    val emoji = text.takeWhile { !it.isLetter() }.trim().ifEmpty { text.take(2) }
    val title = text.dropWhile { !it.isLetter() }.trim()

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(185.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.radialGradient(
                    colors = if (isMe) listOf(Color(0xFFFFD700).copy(alpha = 0.35f), Color(0xFFE11D48), Color(0xFF881337))
                    else if (isDark) listOf(Color(0xFF3B82F6).copy(alpha = 0.3f), Color(0xFF1E293B), Color(0xFF0F172A))
                    else listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFF3E8FF))
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        WarmGold,
                        WarmGoldLight,
                        VelvetRosePrimary
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing 3D Aura behind Emoji
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    WarmGold.copy(alpha = auraAlpha),
                                    VelvetRosePrimary.copy(alpha = auraAlpha * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Floating 3D Emoji Icon
                Text(
                    text = emoji,
                    fontSize = 44.sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = floatOffset
                    }
                )
            }

            if (title.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(0.8.dp, WarmGold)
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
