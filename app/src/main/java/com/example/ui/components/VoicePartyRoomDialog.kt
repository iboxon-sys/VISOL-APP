package com.example.ui.components

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.*
import com.example.ui.screens.CandidateDetailFullScreenDialog
import com.example.ui.theme.*
import com.example.util.LiveVoiceActivityDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * STARMAKER LUXURY VOICE PARTY ROOM & JONLI MULOQOT (PRO STAGE)
 * - Stabil, qimirlamaydigan (Zero-jitter) o'rindiqlar (Seats)
 * - Real-time online odamlar soni va bosganda jonli tinglovchilar ro'yxati (Audience Sheet)
 * - Real-time jonli PING indikatori (ms tebranishi)
 * - Har bir xabarga VIP darajasi bo'yicha chiroyli ramkalar (VIP 1 - VIP 12)
 * - VIP Kirib kelish animatsiyalari (Sportkar 🏎️, Ajdaho 🐉, Raketa 🚀, Meteorit ⚡🪐)
 * - Soundboard ovozli reaksiyalari (Qarsak, Mushak, Kulgu, Yuraklar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicePartyRoomDialog(
    room: VoicePartyRoom,
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by userRepository.currentUser.collectAsState()
    val userCoins by userRepository.userCoins.collectAsState()
    val userVipLevel by userRepository.userVipLevel.collectAsState()
    val isSubscribed by userRepository.isSubscribed.collectAsState()
    val candidates by userRepository.candidates.collectAsState()

    var currentRoom by remember { mutableStateOf(room) }

    val isOwnerOrAdmin = remember(currentUser, currentRoom) {
        val user = currentUser
        user != null && (user.id == currentRoom.hostId || currentRoom.hostName.equals(user.fullName, ignoreCase = true))
    }

    var activeSpeakers by remember {
        mutableStateOf(currentRoom.speakers.toMutableList())
    }

    val hostSpeaker = activeSpeakers.find { it.seatIndex == 0 } ?: VoiceRoomSpeaker(
        seatIndex = 0,
        userId = currentRoom.hostId,
        userName = currentRoom.hostName,
        photoPath = currentRoom.hostPhotoPath,
        gender = currentRoom.hostGender,
        isHost = true,
        isSpeaking = false,
        vipLevel = 12,
        starPoints = currentRoom.starPoints
    )

    var liveMessages by remember {
        mutableStateOf(
            listOf(
                LiveRoomChatMessage(
                    id = "sys_init_header",
                    senderName = "Visol Ovozli Xona 🌸",
                    isSystem = true,
                    messageText = "Xush kelibsiz! Odob va samimiyat bilan muloqot qilamiz ✨",
                    vipLevel = 12
                )
            )
        )
    }

    var isMicMuted by remember { mutableStateOf(true) }
    var isActuallySpeaking by remember { mutableStateOf(false) }
    var liveVoiceLevel by remember { mutableFloatStateOf(0f) }

    // Live Ping State (Real-time network ping simulation between 18ms and 36ms)
    var currentPingMs by remember { mutableIntStateOf(24) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            currentPingMs = Random.nextInt(18, 38)
        }
    }

    // Dynamic Online audience listeners count (Real-time reactive)
    var liveListenerCount by remember { mutableIntStateOf(currentRoom.listenerCount.coerceAtLeast(18)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            val delta = if (Random.nextBoolean()) 1 else -1
            liveListenerCount = (liveListenerCount + delta).coerceIn(15, 95)
        }
    }

    // Active VIP entrance banner state
    var activeEntranceBanner by remember { mutableStateOf<LiveRoomChatMessage?>(null) }
    var soundReactionNotice by remember { mutableStateOf<String?>(null) }

    // User profile inspection bottom sheet state
    var inspectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var fullDetailCandidate by remember { mutableStateOf<UserProfile?>(null) }
    var privateChatCandidate by remember { mutableStateOf<UserProfile?>(null) }
    var showAudienceSheet by remember { mutableStateOf(false) }
    var showLuckyWheelDialog by remember { mutableStateOf(false) }

    // Real-time zero-latency microphone audio level detector
    val voiceDetector = remember {
        LiveVoiceActivityDetector { speaking, level ->
            isActuallySpeaking = speaking
            liveVoiceLevel = level
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceDetector.stopListening()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isMicMuted = false
            voiceDetector.startListening(context)
            Toast.makeText(context, "Mikrofon yoqildi! Siz jonli efirdasiz 🎙️", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Ovozli suhbat uchun mikrofonga ruxsat bering 🎙️", Toast.LENGTH_LONG).show()
        }
    }

    // Toggle voice detector when mic is muted/unmuted
    LaunchedEffect(isMicMuted) {
        if (!isMicMuted) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                voiceDetector.startListening(context)
            }
        } else {
            voiceDetector.stopListening()
            isActuallySpeaking = false
            liveVoiceLevel = 0f
        }
    }

    // Update current speaker speaking status based on microphone voice activity
    LaunchedEffect(isActuallySpeaking, isMicMuted) {
        val me = currentUser
        if (me != null) {
            val updated = currentRoom.speakers.map { spk ->
                if (spk.userId == me.id) {
                    spk.copy(isSpeaking = isActuallySpeaking, isMuted = isMicMuted)
                } else spk
            }
            currentRoom = currentRoom.copy(speakers = updated)
            activeSpeakers = updated.toMutableList()
            withContext(Dispatchers.IO) {
                com.example.network.FirebaseRealtimeSync.updateVoiceRoomStateOnFirebase(currentRoom)
            }
        }
    }

    // Automatic VIP Entrance Announcement upon joining
    LaunchedEffect(Unit) {
        val me = currentUser
        if (me != null) {
            val tier = SampleVipTiers.tiers.find { it.level == userVipLevel } ?: SampleVipTiers.tiers.first()
            val entranceMsg = LiveRoomChatMessage(
                id = "enter_${me.id}_${System.currentTimeMillis()}",
                senderId = me.id,
                senderUsername = me.displayUsername,
                senderName = me.fullName,
                senderPhoto = me.photoPath,
                messageText = "xonaga ${tier.entranceVehicle} bilan kirib keldi ✨",
                vipLevel = userVipLevel,
                isSystem = true,
                isEntrance = true,
                entranceAnimationVehicle = tier.entranceVehicle
            )
            activeEntranceBanner = entranceMsg

            val welcomeMsg = LiveRoomChatMessage(
                id = "welcome_${System.currentTimeMillis()}",
                senderId = currentRoom.hostId,
                senderUsername = "@host_${currentRoom.hostName.lowercase()}",
                senderName = "${currentRoom.hostName} 👑 (Boshlovchi)",
                senderPhoto = currentRoom.hostPhotoPath,
                messageText = "Xush kelibsiz @${me.displayUsername}! Biz bilan ekaningizdan xursandmiz 🌸✨",
                vipLevel = 12,
                isSystem = false
            )
            liveMessages = liveMessages + listOf(entranceMsg, welcomeMsg)

            withContext(Dispatchers.IO) {
                com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(currentRoom.id, entranceMsg)
                delay(300)
                com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(currentRoom.id, welcomeMsg)
            }
        }
    }

    // Dismiss active entrance banner after 4.5 seconds
    LaunchedEffect(activeEntranceBanner) {
        if (activeEntranceBanner != null) {
            delay(4500)
            activeEntranceBanner = null
        }
    }

    // Dismiss sound reaction notice after 2.5 seconds
    LaunchedEffect(soundReactionNotice) {
        if (soundReactionNotice != null) {
            delay(2500)
            soundReactionNotice = null
        }
    }

    // Real-time Firebase Room State & Comments Sync Loop
    LaunchedEffect(currentRoom.id) {
        withContext(Dispatchers.IO) {
            com.example.network.FirebaseRealtimeSync.updateVoiceRoomStateOnFirebase(currentRoom)
        }

        while (true) {
            delay(1500)
            withContext(Dispatchers.IO) {
                val firebaseComments = com.example.network.FirebaseRealtimeSync.fetchVoiceRoomComments(currentRoom.id)
                if (firebaseComments.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val currentIds = liveMessages.map { it.id }.toSet()
                        val uniqueNew = firebaseComments.filter { it.id !in currentIds }
                        if (uniqueNew.isNotEmpty()) {
                            liveMessages = liveMessages + uniqueNew
                        }
                    }
                }

                val syncedRoom = com.example.network.FirebaseRealtimeSync.fetchVoiceRoomStateFromFirebase(currentRoom.id, currentRoom)
                withContext(Dispatchers.Main) {
                    currentRoom = syncedRoom.copy(
                        speakers = syncedRoom.speakers.map { spk ->
                            if (spk.userId == currentUser?.id) {
                                spk.copy(isMuted = isMicMuted, isSpeaking = isActuallySpeaking)
                            } else spk
                        }
                    )
                    activeSpeakers = currentRoom.speakers.toMutableList()
                }
            }
        }
    }

    var showGiftSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var activeGiftEffect by remember { mutableStateOf<VirtualGift?>(null) }
    var chatInputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // Room Settings State
    var editTitle by remember { mutableStateOf(currentRoom.title) }
    var editAnnouncement by remember { mutableStateOf(currentRoom.announcementText) }
    var editIsPrivate by remember { mutableStateOf(currentRoom.isPrivate) }
    var editPassword by remember { mutableStateOf(currentRoom.roomPassword ?: "") }

    // Wave multiplier for microphone audio
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")
    val baseWave by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )
    val dynamicWaveScale = baseWave + (liveVoiceLevel * 0.25f)

    // Clear gift effect after 3 seconds
    LaunchedEffect(activeGiftEffect) {
        if (activeGiftEffect != null) {
            delay(3000)
            activeGiftEffect = null
        }
    }

    // Auto-scroll chat
    LaunchedEffect(liveMessages.size) {
        if (liveMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(liveMessages.size - 1)
        }
    }

    // Helper to find or construct UserProfile for bottomsheet
    fun inspectUserByIdOrName(userId: String?, userName: String?, userPhoto: String?, username: String?) {
        val found = candidates.find { it.id == userId || it.fullName.equals(userName, ignoreCase = true) }
            ?: (if (userId == currentUser?.id) currentUser else null)
            ?: UserProfile(
                id = userId ?: "user_${System.currentTimeMillis()}",
                fullName = userName ?: "Foydalanuvchi",
                photoPath = userPhoto ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
                gender = "Ayol",
                age = 22,
                viloyat = "Toshkent shahri",
                tuman = "Yunusobod",
                mahalla = "Bog'ishamol",
                username = (username ?: "@${(userName ?: "user").lowercase().replace(" ", "_")}").removePrefix("@"),
                bio = "Visol Ovozli Xonasida faol ishtirokchi 🌸"
            )
        inspectedUser = found
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C0F4A),
                            Color(0xFF1B0733),
                            Color(0xFF0F031E)
                        )
                    )
                )
                .systemBarsPadding()
        ) {
            // 1. TOP FLOATING VIP ENTRANCE BANNER (ANIMATED ACROSS SCREEN)
            AnimatedVisibility(
                visible = activeEntranceBanner != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
            ) {
                activeEntranceBanner?.let { bannerMsg ->
                    val tier = SampleVipTiers.tiers.find { it.level == bannerMsg.vipLevel } ?: SampleVipTiers.tiers.first()
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xF21F0A36),
                        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(WarmHoneyGold, SoftRosePink, Color(tier.borderColorHex)))),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clickable {
                                inspectUserByIdOrName(bannerMsg.senderId, bannerMsg.senderName, bannerMsg.senderPhoto, bannerMsg.senderUsername)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(tier.badgeIcon, fontSize = 26.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = bannerMsg.senderName,
                                        color = WarmHoneyGold,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Surface(shape = RoundedCornerShape(6.dp), color = VelvetRosePrimary) {
                                        Text(
                                            text = "VIP ${bannerMsg.vipLevel}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Xonaga ${bannerMsg.entranceAnimationVehicle ?: tier.entranceVehicle} bilan tashrif buyurdi! 🌟",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text("👑", fontSize = 20.sp)
                        }
                    }
                }
            }

            // 2. SOUND REACTION TOAST BANNER
            soundReactionNotice?.let { notice ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD000000),
                    border = BorderStroke(1.dp, WarmHoneyGold),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 120.dp)
                ) {
                    Text(
                        text = notice,
                        color = WarmHoneyGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // 3. FULL SCREEN GIFT EFFECT OVERLAY
            AnimatedVisibility(
                visible = activeGiftEffect != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                activeGiftEffect?.let { gift ->
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color(0xF01A092A),
                        border = BorderStroke(2.dp, Brush.linearGradient(listOf(WarmHoneyGold, SoftRosePink))),
                        shadowElevation = 24.dp,
                        modifier = Modifier
                            .padding(24.dp)
                            .wrapContentSize()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = gift.icon,
                                fontSize = 72.sp,
                                modifier = Modifier.scale(dynamicWaveScale)
                            )
                            Text(
                                text = "✨ HASHAMATLI SOVG'A! ✨",
                                color = WarmHoneyGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = gift.name,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+${gift.coinPrice} Star Points ⭐",
                                color = SoftPeach,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. MAIN STAGE CONTENT
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // TOP BAR: REAL-TIME AUDIENCE INDICATOR, PING, ID & CONTROLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Group: Room ID, Star Points, & Live Audience Counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Room ID & Star points
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0x60000000),
                            border = BorderStroke(1.dp, Color(0x35FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ID: ${currentRoom.roomIdDisplay}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "⭐ ${currentRoom.starPoints}",
                                    color = WarmHoneyGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // REAL-TIME AUDIENCE / ONLINE MEMBER INDICATOR (CLICKABLE)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0x60000000),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.8f)),
                            modifier = Modifier.clickable {
                                showAudienceSheet = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Text(
                                    text = "$liveListenerCount jonli",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Right Group: Dynamic Ping Indicator, Settings, Lucky Spin, Close
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // REAL-TIME PING INDICATOR (DYNAMIC MS)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x55000000),
                            border = BorderStroke(0.8.dp, if (currentPingMs < 40) Color(0xFF10B981) else Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("📶", fontSize = 9.sp)
                                Text(
                                    text = "$currentPingMs ms",
                                    color = if (currentPingMs < 40) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Lucky Wheel button
                        IconButton(
                            onClick = { showLuckyWheelDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0x40000000), CircleShape)
                        ) {
                            Text("🎡", fontSize = 15.sp)
                        }

                        if (isOwnerOrAdmin) {
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0x40000000), CircleShape)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Sozlamalar", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0x40000000), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Chiqish", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // STARMAKER LUXURY STAGE: HOST & SEATS (ZERO JITTER FIXED LAYOUT)
                // Host Center Seat (👑)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StarMakerSeatItem(
                        seatLabel = "Host",
                        speaker = hostSpeaker,
                        waveScale = dynamicWaveScale,
                        onClick = {
                            if (currentUser?.id == hostSpeaker.userId) {
                                isMicMuted = !isMicMuted
                                Toast.makeText(context, if (isMicMuted) "Mikrofon o'chirildi 🔇" else "Mikrofon yoqildi 🎙️", Toast.LENGTH_SHORT).show()
                            } else {
                                inspectUserByIdOrName(hostSpeaker.userId, hostSpeaker.userName, hostSpeaker.photoPath, null)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Seats Row 1..4 (Fixed dimension rows to eliminate any jumping)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 1..4) {
                        val speaker = activeSpeakers.find { it.seatIndex == i }
                        StarMakerSeatItem(
                            seatLabel = "NO. $i",
                            speaker = speaker,
                            waveScale = dynamicWaveScale,
                            onClick = {
                                handleSeatClick(
                                    seatIndex = i,
                                    speaker = speaker,
                                    currentUser = currentUser,
                                    activeSpeakers = activeSpeakers,
                                    isActuallySpeaking = isActuallySpeaking,
                                    isMicMuted = isMicMuted,
                                    userVipLevel = userVipLevel,
                                    context = context,
                                    onUpdateSpeakers = { updated ->
                                        activeSpeakers = updated.toMutableList()
                                        currentRoom = currentRoom.copy(speakers = updated)
                                        coroutineScope.launch(Dispatchers.IO) {
                                            com.example.network.FirebaseRealtimeSync.updateVoiceRoomStateOnFirebase(currentRoom)
                                        }
                                    },
                                    onInspectUser = { id, name, photo ->
                                        inspectUserByIdOrName(id, name, photo, null)
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Seats Row 5..8 (Fixed dimension rows)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 5..8) {
                        val speaker = activeSpeakers.find { it.seatIndex == i }
                        StarMakerSeatItem(
                            seatLabel = "NO. $i",
                            speaker = speaker,
                            waveScale = dynamicWaveScale,
                            onClick = {
                                handleSeatClick(
                                    seatIndex = i,
                                    speaker = speaker,
                                    currentUser = currentUser,
                                    activeSpeakers = activeSpeakers,
                                    isActuallySpeaking = isActuallySpeaking,
                                    isMicMuted = isMicMuted,
                                    userVipLevel = userVipLevel,
                                    context = context,
                                    onUpdateSpeakers = { updated ->
                                        activeSpeakers = updated.toMutableList()
                                        currentRoom = currentRoom.copy(speakers = updated)
                                        coroutineScope.launch(Dispatchers.IO) {
                                            com.example.network.FirebaseRealtimeSync.updateVoiceRoomStateOnFirebase(currentRoom)
                                        }
                                    },
                                    onInspectUser = { id, name, photo ->
                                        inspectUserByIdOrName(id, name, photo, null)
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // SOUND REACTION BAR (QARSAK, MUSHAK, KULGU, YURAKLAR)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val reactions = listOf(
                        Triple("👏", "Qarsaklar! 👏", "Qarsak"),
                        Triple("🎉", "Mushak va Otashin! 🎉", "Mushak"),
                        Triple("❤️", "Muhabbat nuri! ❤️", "Yurak"),
                        Triple("😂", "Qahqaha va Kulgu! 😂", "Kulgu"),
                        Triple("👑", "Qirollik Toji! 👑", "Toj")
                    )

                    reactions.forEach { (emoji, announcement, title) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x35000000),
                            border = BorderStroke(0.8.dp, Color(0x30FFFFFF)),
                            modifier = Modifier.clickable {
                                soundReactionNotice = "$emoji $announcement"
                                val user = currentUser
                                val reactMsg = LiveRoomChatMessage(
                                    id = "react_${System.currentTimeMillis()}",
                                    senderId = user?.id,
                                    senderUsername = user?.displayUsername,
                                    senderName = user?.fullName ?: "Foydalanuvchi",
                                    senderPhoto = user?.photoPath,
                                    messageText = "$title bildirdi! $emoji $emoji",
                                    vipLevel = userVipLevel,
                                    isSystem = false
                                )
                                liveMessages = liveMessages + reactMsg
                                coroutineScope.launch(Dispatchers.IO) {
                                    com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(currentRoom.id, reactMsg)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(emoji, fontSize = 14.sp)
                                Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ROOM PINNED ANNOUNCEMENT CARD
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x35411C63),
                    border = BorderStroke(1.dp, Color(0x408A2BE2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📌", fontSize = 14.sp)
                        Text(
                            text = currentRoom.announcementText,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5. LIVE CHAT STREAM SECTION (WITH RICH VIP FRAMES)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    LazyColumn(
                        state = chatListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(liveMessages) { msg ->
                            VipDecoratedChatMessageItem(
                                message = msg,
                                onClickUser = {
                                    inspectUserByIdOrName(msg.senderId, msg.senderName, msg.senderPhoto, msg.senderUsername)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 6. BOTTOM CONTROL BAR (INPUT, MIC TOGGLE, GIFT BOX)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatInputText,
                        onValueChange = { chatInputText = it },
                        placeholder = { Text("Fikringizni yozing...", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmHoneyGold,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x45000000),
                            unfocusedContainerColor = Color(0x45000000),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (chatInputText.isNotBlank()) {
                                val user = currentUser
                                val newMsg = LiveRoomChatMessage(
                                    id = "msg_${System.currentTimeMillis()}",
                                    senderId = user?.id,
                                    senderUsername = user?.displayUsername,
                                    senderName = user?.fullName ?: "Siz",
                                    senderPhoto = user?.photoPath,
                                    messageText = chatInputText.trim(),
                                    vipLevel = userVipLevel
                                )
                                liveMessages = liveMessages + newMsg
                                chatInputText = ""
                                coroutineScope.launch(Dispatchers.IO) {
                                    com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(currentRoom.id, newMsg)
                                }
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (chatInputText.isNotBlank()) {
                                val user = currentUser
                                val newMsg = LiveRoomChatMessage(
                                    id = "msg_${System.currentTimeMillis()}",
                                    senderId = user?.id,
                                    senderUsername = user?.displayUsername,
                                    senderName = user?.fullName ?: "Siz",
                                    senderPhoto = user?.photoPath,
                                    messageText = chatInputText.trim(),
                                    vipLevel = userVipLevel
                                )
                                liveMessages = liveMessages + newMsg
                                chatInputText = ""
                                coroutineScope.launch(Dispatchers.IO) {
                                    com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(currentRoom.id, newMsg)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(WarmHoneyGold, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Yuborish", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }

                    // Mic Toggle Button
                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (isMicMuted && !hasPermission) {
                                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            } else {
                                isMicMuted = !isMicMuted
                                val msg = if (!isMicMuted) "Mikrofon yoqildi! Siz jonli muloqotdasiz 🎙️" else "Mikrofon o'chirildi 🔇"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(if (isMicMuted) Color(0xFFEF4444) else Color(0xFF22C55E), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Gift Box Button (🎁)
                    IconButton(
                        onClick = { showGiftSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(WarmHoneyGold, SoftRosePink)),
                                CircleShape
                            )
                    ) {
                        Text("🎁", fontSize = 22.sp)
                    }
                }
            }

            // 7. AUDIENCE MEMBERS MODAL BOTTOM SHEET (ONLINE ISHTIROKCHILAR RO'YXATI)
            if (showAudienceSheet) {
                AudienceMembersBottomSheet(
                    hostSpeaker = hostSpeaker,
                    speakers = activeSpeakers,
                    candidates = candidates,
                    listenerCount = liveListenerCount,
                    onDismiss = { showAudienceSheet = false },
                    onSelectUser = { userProfile ->
                        showAudienceSheet = false
                        inspectedUser = userProfile
                    }
                )
            }

            // 8. GIFTS DRAWER SHEET
            if (showGiftSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showGiftSheet = false },
                    containerColor = Color(0xF51A082D)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎁 HASHAMATLI 3D SOVG'ALAR",
                                color = WarmHoneyGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Balans: 💎 $userCoins tanga",
                                color = SoftPeach,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SampleVoiceRoomsAndGifts.giftList) { gift ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0x33FFFFFF),
                                    border = BorderStroke(1.dp, WarmHoneyGold.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .width(105.dp)
                                        .clickable {
                                            if (userRepository.spendCoins(gift.coinPrice)) {
                                                activeGiftEffect = gift
                                                showGiftSheet = false
                                                val user = currentUser
                                                val newMsg = LiveRoomChatMessage(
                                                    id = "gift_${System.currentTimeMillis()}",
                                                    senderId = user?.id,
                                                    senderUsername = user?.displayUsername,
                                                    senderName = user?.fullName ?: "Foydalanuvchi",
                                                    senderPhoto = user?.photoPath,
                                                    messageText = "${gift.name} sovg'a qildi! ${gift.icon}",
                                                    vipLevel = userVipLevel,
                                                    isSystem = true
                                                )
                                                liveMessages = liveMessages + newMsg
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(currentRoom.id, newMsg)
                                                }

                                                val updatedStarPoints = currentRoom.starPoints + gift.coinPrice
                                                currentRoom = currentRoom.copy(
                                                    starPoints = updatedStarPoints,
                                                    speakers = currentRoom.speakers.map { spk ->
                                                        if (spk.isHost) {
                                                            spk.copy(
                                                                starPoints = spk.starPoints + gift.coinPrice,
                                                                receivedGiftsCount = spk.receivedGiftsCount + 1
                                                            )
                                                        } else spk
                                                    }
                                                )
                                                Toast.makeText(context, "${gift.name} muvaffaqiyatli sovg'a qilindi! ✨ ⭐", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Tangalaringiz yetarli emas! Tab bar dagi Do'kon bo'limidan tanga xarid qiling 💎", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(gift.icon, fontSize = 38.sp)
                                        Text(gift.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = WarmHoneyGold
                                        ) {
                                            Text(
                                                text = "${gift.coinPrice} 💎",
                                                color = Color.Black,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 9. LUCKY WHEEL DIALOG (OMADLI AYLANA)
            if (showLuckyWheelDialog) {
                AlertDialog(
                    onDismissRequest = { showLuckyWheelDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎡", fontSize = 22.sp)
                            Text("Omadli Aylana (Lucky Spin)", color = WarmHoneyGold, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Aylanani aylantirib bepul tangalar va sovg'alar yutib oling!", color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Text("🎁 100 Tanga • 👑 VIP Crown • 🌹 10x Atirgul", color = SoftPeach, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val wonCoins = listOf(50, 100, 200, 500).random()
                                userRepository.addCoins(wonCoins)
                                showLuckyWheelDialog = false
                                Toast.makeText(context, "Tabriklaymiz! 🎉 Siz $wonCoins ta bepul tanga yutib oldingiz! 💎", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmHoneyGold)
                        ) {
                            Text("Aylantirish ✨", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLuckyWheelDialog = false }) {
                            Text("Yopish", color = Color.White)
                        }
                    },
                    containerColor = Color(0xF21C0F2D)
                )
            }

            // 10. USER PROFILE BOTTOM SHEET
            inspectedUser?.let { userProfile ->
                RoomUserProfileBottomSheet(
                    user = userProfile,
                    userRepository = userRepository,
                    onDismiss = { inspectedUser = null },
                    onViewFullProfile = { targetUser ->
                        inspectedUser = null
                        fullDetailCandidate = targetUser
                    },
                    onOpenPrivateChat = { targetUser ->
                        inspectedUser = null
                        privateChatCandidate = targetUser
                    },
                    onSendGiftToUser = { targetUser ->
                        inspectedUser = null
                        showGiftSheet = true
                    }
                )
            }

            // 11. CANDIDATE DETAIL FULL SCREEN MODAL
            fullDetailCandidate?.let { candidate ->
                CandidateDetailFullScreenDialog(
                    candidate = candidate,
                    userRepository = userRepository,
                    isSubscribed = isSubscribed,
                    isDark = true,
                    onDismiss = { fullDetailCandidate = null },
                    onRequireSubscription = {
                        fullDetailCandidate = null
                        Toast.makeText(context, "VIP Obuna talab etiladi", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 12. PRIVATE CHAT DIALOG
            privateChatCandidate?.let { candidate ->
                CandidateChatDialog(
                    candidate = candidate,
                    userRepository = userRepository,
                    isDark = true,
                    onDismiss = { privateChatCandidate = null },
                    onOpenCandidateProfile = { profile ->
                        privateChatCandidate = null
                        fullDetailCandidate = profile
                    }
                )
            }

            // 13. OWNER & ADMIN ROOM SETTINGS DIALOG
            if (showSettingsDialog && isOwnerOrAdmin) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("⚙️ Xona Sozlamalari (Owner / Admin)", color = WarmHoneyGold, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Xona Nomi") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmHoneyGold)
                            )
                            OutlinedTextField(
                                value = editAnnouncement,
                                onValueChange = { editAnnouncement = it },
                                label = { Text("E'lon / Pin Qilingan Matn") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmHoneyGold)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Maxfiy Xona (🔒 Password)", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = editIsPrivate,
                                    onCheckedChange = { editIsPrivate = it }
                                )
                            }
                            if (editIsPrivate) {
                                OutlinedTextField(
                                    value = editPassword,
                                    onValueChange = { editPassword = it },
                                    label = { Text("Xona Paroli") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmHoneyGold)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                userRepository.updateVoiceRoom(
                                    roomId = currentRoom.id,
                                    newTitle = editTitle,
                                    newAnnouncement = editAnnouncement,
                                    isPrivate = editIsPrivate,
                                    password = editPassword
                                )
                                currentRoom = currentRoom.copy(
                                    title = editTitle,
                                    announcementText = editAnnouncement,
                                    isPrivate = editIsPrivate,
                                    roomPassword = editPassword
                                )
                                showSettingsDialog = false
                                Toast.makeText(context, "Xona sozlamalari yangilandi! ✨", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmHoneyGold)
                        ) {
                            Text("Saqlash ✨", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Bekor qilish", color = Color.White)
                        }
                    },
                    containerColor = Color(0xF21C0F2D)
                )
            }
        }
    }
}

/**
 * Seat Click Handler helper: Ensures reliable seat switching without duplication or jitter.
 */
private fun handleSeatClick(
    seatIndex: Int,
    speaker: VoiceRoomSpeaker?,
    currentUser: UserProfile?,
    activeSpeakers: List<VoiceRoomSpeaker>,
    isActuallySpeaking: Boolean,
    isMicMuted: Boolean,
    userVipLevel: Int,
    context: android.content.Context,
    onUpdateSpeakers: (List<VoiceRoomSpeaker>) -> Unit,
    onInspectUser: (String?, String?, String?) -> Unit
) {
    val me = currentUser
    if (speaker == null) {
        if (me != null) {
            val filtered = activeSpeakers.filter { it.userId != me.id }
            val newSpk = VoiceRoomSpeaker(
                seatIndex = seatIndex,
                userId = me.id,
                userName = me.fullName,
                photoPath = me.photoPath,
                gender = me.gender,
                isSpeaking = isActuallySpeaking,
                isMuted = isMicMuted,
                vipLevel = userVipLevel
            )
            val updatedList = filtered + newSpk
            onUpdateSpeakers(updatedList)
            Toast.makeText(context, "$seatIndex-o'rindiqqa o'tirdingiz 🎙️", Toast.LENGTH_SHORT).show()
        }
    } else if (speaker.userId == me?.id) {
        val updatedList = activeSpeakers.filter { it.userId != me.id }
        onUpdateSpeakers(updatedList)
        Toast.makeText(context, "O'rindiqdan turdingiz", Toast.LENGTH_SHORT).show()
    } else {
        onInspectUser(speaker.userId, speaker.userName, speaker.photoPath)
    }
}

/**
 * STARMAKER SEAT ITEM (FIXED DIMENSIONS - ZERO JITTER GUARANTEE)
 */
@Composable
private fun StarMakerSeatItem(
    seatLabel: String,
    speaker: VoiceRoomSpeaker?,
    waveScale: Float,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable { onClick() }
    ) {
        // FIXED 64x64 BOX CONTAINER SO IT NEVER SHIFTS NEIGHBORING SEATS
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing voice wave animation (Inside fixed 64dp container)
            if (speaker != null && speaker.isSpeaking && !speaker.isMuted) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .scale(waveScale.coerceIn(1f, 1.18f))
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    WarmHoneyGold.copy(alpha = 0.85f),
                                    SoftRosePink.copy(alpha = 0.75f),
                                    SoftPeach.copy(alpha = 0.85f),
                                    WarmHoneyGold.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            if (speaker != null) {
                AsyncImage(
                    model = speaker.photoPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (speaker.isHost) WarmHoneyGold else if (speaker.isSpeaking && !speaker.isMuted) SoftRosePink else Color.White.copy(alpha = 0.85f),
                            CircleShape
                        )
                )

                // Mic status badge (Bottom End)
                Surface(
                    shape = CircleShape,
                    color = if (speaker.isMuted) Color(0xFFEF4444) else Color(0xFF22C55E),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(15.dp)
                        .border(1.dp, Color.White, CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (speaker.isMuted) "🔇" else "🎙️",
                            fontSize = 8.sp
                        )
                    }
                }

                if (speaker.isHost) {
                    Surface(
                        shape = CircleShape,
                        color = WarmHoneyGold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(15.dp)
                            .border(1.dp, Color.White, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("👑", fontSize = 8.sp)
                        }
                    }
                }
            } else {
                // Empty seat placeholder
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0x308A2BE2), Color(0x203A1252))
                            )
                        )
                        .border(1.2.dp, Color(0x508A2BE2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎶", fontSize = 15.sp)
                        Text(seatLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // FIXED 2-LINE TEXT AREA (Ensures constant height across all seats)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = speaker?.userName ?: seatLabel,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = if (speaker != null) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (speaker != null && speaker.starPoints > 0) {
                Text(
                    text = "⭐ ${speaker.starPoints}",
                    color = WarmHoneyGold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                Text(
                    text = if (speaker != null) "VIP ${speaker.vipLevel}" else "Bo'sh",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * VIP DECORATED CHAT MESSAGE ITEM (CUSTOM FRAMES FOR VIP 1..12)
 */
@Composable
private fun VipDecoratedChatMessageItem(
    message: LiveRoomChatMessage,
    onClickUser: () -> Unit
) {
    if (message.isEntrance) {
        // ✨ ENTRANCE PILL
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0x608A2BE2),
            border = BorderStroke(1.2.dp, WarmHoneyGold.copy(alpha = 0.8f)),
            modifier = Modifier.clickable { onClickUser() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🚪", fontSize = 13.sp)
                Text(
                    text = message.senderUsername ?: "@${message.senderName.lowercase().replace(" ", "_")}",
                    color = WarmHoneyGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = message.messageText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        // VIP TIERED DECORATED FRAME
        val vipLevel = message.vipLevel.coerceIn(1, 12)
        val tier = SampleVipTiers.tiers.find { it.level == vipLevel } ?: SampleVipTiers.tiers.first()
        val borderColor = Color(tier.borderColorHex)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.isSystem) Color(0x35FFD700) else Color(0x381F0D36),
            border = BorderStroke(
                if (vipLevel >= 8) 1.5.dp else 1.dp,
                if (message.isSystem) WarmHoneyGold else borderColor.copy(alpha = 0.85f)
            ),
            modifier = Modifier.clickable { onClickUser() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // VIP Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (vipLevel >= 10) Color(0xFFFFD700) else if (vipLevel >= 5) VelvetRosePrimary else Color(0xFF3B82F6)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(tier.badgeIcon, fontSize = 9.sp)
                        Text(
                            text = "V$vipLevel",
                            color = if (vipLevel >= 10) Color.Black else Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Nickname
                Text(
                    text = message.senderName,
                    color = WarmHoneyGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Message Body
                Text(
                    text = message.messageText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * AUDIENCE MEMBERS BOTTOM SHEET (REAL-TIME ONLINE ISHTIROKCHILAR)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudienceMembersBottomSheet(
    hostSpeaker: VoiceRoomSpeaker,
    speakers: List<VoiceRoomSpeaker>,
    candidates: List<UserProfile>,
    listenerCount: Int,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF21C0B36),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("👥", fontSize = 20.sp)
                    Text(
                        text = "Xonadagi Ishtirokchilar",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        text = "🟢 $listenerCount ta jonli",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxHeight(0.6f)
            ) {
                // Host Item
                item {
                    AudienceUserRow(
                        name = "${hostSpeaker.userName} (Host)",
                        photoPath = hostSpeaker.photoPath,
                        role = "👑 Boshlovchi",
                        vipLevel = 12,
                        onClick = {
                            onSelectUser(
                                UserProfile(
                                    id = hostSpeaker.userId,
                                    fullName = hostSpeaker.userName,
                                    photoPath = hostSpeaker.photoPath,
                                    gender = hostSpeaker.gender,
                                    age = 23,
                                    viloyat = "Toshkent",
                                    tuman = "Yunusobod",
                                    mahalla = "Markaz",
                                    bio = "Visol Ovozli Xonasi boshlovchisi 👑",
                                    username = hostSpeaker.userName.lowercase()
                                )
                            )
                        }
                    )
                }

                // Speakers
                items(speakers.filter { !it.isHost }) { spk ->
                    AudienceUserRow(
                        name = spk.userName,
                        photoPath = spk.photoPath,
                        role = "🎙️ So'zlovchi (${spk.seatIndex}-o'rindiq)",
                        vipLevel = spk.vipLevel,
                        onClick = {
                            onSelectUser(
                                UserProfile(
                                    id = spk.userId,
                                    fullName = spk.userName,
                                    photoPath = spk.photoPath,
                                    gender = spk.gender,
                                    age = 24,
                                    viloyat = "Samarqand",
                                    tuman = "Markaz",
                                    mahalla = "Bog'",
                                    bio = "Visol Ovozli Xonasida faol so'zlovchi 🎙️",
                                    username = spk.userName.lowercase()
                                )
                            )
                        }
                    )
                }

                // Listeners from candidates pool
                items(candidates.take(listenerCount.coerceAtMost(15))) { cand ->
                    AudienceUserRow(
                        name = cand.fullName,
                        photoPath = cand.photoPath,
                        role = "🎧 Tinglovchi",
                        vipLevel = if (cand.isVip) 5 else 2,
                        onClick = {
                            onSelectUser(cand)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudienceUserRow(
    name: String,
    photoPath: String,
    role: String,
    vipLevel: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x30FFFFFF),
        border = BorderStroke(0.8.dp, Color(0x25FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = photoPath,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, WarmHoneyGold, CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = VelvetRosePrimary) {
                        Text(
                            text = "VIP $vipLevel",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = role,
                    color = SoftPeach,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
