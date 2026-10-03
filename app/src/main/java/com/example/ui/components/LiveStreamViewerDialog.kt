package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.*
import com.example.ui.theme.SoftPeach
import com.example.ui.theme.SoftRosePink
import com.example.ui.theme.WarmHoneyGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * JONLI EFIR (LIVE STREAM) OYNASI VA REAL-TIME CHAT:
 * - Jonli video stream player (ExoPlayer)
 * - Firebase orqali butun dunyodagi tomoshabinlar bilan real-vaqtli jonli chat (Real-time live comments)
 * - Tezkor emodzi va sovg'alar (Gifts) yuborish
 * - Interaktiv layklar va tomoshabinlar hisoblagichi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamViewerDialog(
    room: VoicePartyRoom,
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by userRepository.currentUser.collectAsState()
    val userCoins by userRepository.userCoins.collectAsState()

    var heartCount by remember { mutableIntStateOf(142) }
    var showGiftSheet by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    var liveComments by remember {
        mutableStateOf(
            listOf(
                LiveRoomChatMessage(
                    id = "sys_init",
                    senderName = "Visol Jonli Efir 📹",
                    isSystem = true,
                    messageText = "${room.hostName} jonli efirni boshladi! Hush kelibsiz ✨"
                ),
                LiveRoomChatMessage(
                    id = "msg_init_1",
                    senderName = "Muxlis_99",
                    vipLevel = 3,
                    messageText = "Assalomu alaykum! Omad efirga 🔥"
                )
            )
        )
    }

    // Real-time Firebase Comments Sync Loop for Live Stream
    LaunchedEffect(room.id) {
        while (true) {
            delay(1500)
            withContext(Dispatchers.IO) {
                val remoteComments = com.example.network.FirebaseRealtimeSync.fetchVoiceRoomComments(room.id)
                if (remoteComments.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val existingIds = liveComments.map { it.id }.toSet()
                        val newComments = remoteComments.filter { it.id !in existingIds }
                        if (newComments.isNotEmpty()) {
                            liveComments = liveComments + newComments
                        }
                    }
                }
            }
        }
    }

    // Auto-scroll on new comments
    LaunchedEffect(liveComments.size) {
        if (liveComments.isNotEmpty()) {
            chatListState.animateScrollToItem(liveComments.size - 1)
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = room.streamVideoUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
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
                .background(Color.Black)
        ) {
            // Live Video Stream Layer
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dark Gradient Overlays for perfect legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB0000000),
                                Color.Transparent,
                                Color(0xE0000000)
                            )
                        )
                    )
            )

            // TOP HEADER: Host info, VIP star points, Live badge & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = room.hostPhotoPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, WarmHoneyGold, CircleShape)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(room.hostName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Surface(shape = CircleShape, color = WarmHoneyGold) {
                                Text("👑", fontSize = 8.sp, modifier = Modifier.padding(2.dp))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEF4444)) {
                                Text("JONLI EFIR 📹", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text("👥 ${room.listenerCount} tomoshabin", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0x66000000), CircleShape)
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White)
                }
            }

            // FLOATING HEART LIKES BUTTON (Right edge)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        heartCount++
                        val me = currentUser
                        val newMsg = LiveRoomChatMessage(
                            id = "like_${System.currentTimeMillis()}",
                            senderName = me?.fullName ?: "Muxlis",
                            messageText = "Layk bosdi! ❤️🔥",
                            vipLevel = 3
                        )
                        liveComments = liveComments + newMsg
                        coroutineScope.launch(Dispatchers.IO) {
                            com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(room.id, newMsg)
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFFFF1493), Color(0xFFC71585))),
                            CircleShape
                        )
                        .shadow(12.dp, CircleShape)
                ) {
                    Text("❤️", fontSize = 26.sp)
                }
                Text("$heartCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }

            // BOTTOM CHAT OVERLAY & INPUT SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. LIVE CHAT STREAM MESSAGES (Frosted Glass bubbles)
                LazyColumn(
                    state = chatListState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(180.dp)
                ) {
                    items(liveComments) { msg ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (msg.isSystem) Color(0x66FFD700) else Color(0x70180B2B),
                            border = BorderStroke(
                                1.dp,
                                if (msg.isSystem) WarmHoneyGold else Color(0x30FFFFFF)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (msg.vipLevel > 0) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = SoftRosePink) {
                                        Text("VIP ${msg.vipLevel}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                    }
                                }
                                Text(
                                    text = msg.senderName,
                                    color = if (msg.isSystem) Color.Black else WarmHoneyGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = msg.messageText,
                                    color = if (msg.isSystem) Color.Black else Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 2. QUICK REACTION EMOJIS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val emojis = listOf("👏", "💖", "🔥", "🌹", "🎁", "💎")
                    emojis.forEach { emo ->
                        Surface(
                            shape = CircleShape,
                            color = Color(0x55000000),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier.clickable {
                                val me = currentUser
                                val newMsg = LiveRoomChatMessage(
                                    id = "emo_${System.currentTimeMillis()}",
                                    senderName = me?.fullName ?: "Siz",
                                    messageText = emo,
                                    vipLevel = 3
                                )
                                liveComments = liveComments + newMsg
                                coroutineScope.launch(Dispatchers.IO) {
                                    com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(room.id, newMsg)
                                }
                            }
                        ) {
                            Text(text = emo, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                // 3. CHAT INPUT BAR + SEND & GIFT BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatInputText,
                        onValueChange = { chatInputText = it },
                        placeholder = { Text("Fikringizni yozing...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftPeach,
                            unfocusedBorderColor = Color(0x44FFFFFF),
                            focusedContainerColor = Color(0x66000000),
                            unfocusedContainerColor = Color(0x66000000),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (chatInputText.isNotBlank()) {
                                val me = currentUser
                                val newMsg = LiveRoomChatMessage(
                                    id = "msg_${System.currentTimeMillis()}",
                                    senderName = me?.fullName ?: "Siz",
                                    messageText = chatInputText.trim(),
                                    vipLevel = 5
                                )
                                liveComments = liveComments + newMsg
                                chatInputText = ""
                                coroutineScope.launch(Dispatchers.IO) {
                                    com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(room.id, newMsg)
                                }
                            }
                        }),
                        trailingIcon = {
                            if (chatInputText.isNotBlank()) {
                                IconButton(onClick = {
                                    val me = currentUser
                                    val newMsg = LiveRoomChatMessage(
                                        id = "msg_${System.currentTimeMillis()}",
                                        senderName = me?.fullName ?: "Siz",
                                        messageText = chatInputText.trim(),
                                        vipLevel = 5
                                    )
                                    liveComments = liveComments + newMsg
                                    chatInputText = ""
                                    coroutineScope.launch(Dispatchers.IO) {
                                        com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(room.id, newMsg)
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Yuborish", tint = WarmHoneyGold)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    Button(
                        onClick = { showGiftSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmHoneyGold),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🎁", fontSize = 16.sp)
                            Text("Sovg'a", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 4. GIFT PICKER BOTTOM SHEET
            if (showGiftSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showGiftSheet = false },
                    containerColor = Color(0xF51C0F2D)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("🎁 JONLI EFIRGA SOVG'ALAR YUBORISH", color = WarmHoneyGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(SampleVoiceRoomsAndGifts.giftList) { gift ->
                                Button(
                                    onClick = {
                                        if (userRepository.spendCoins(gift.coinPrice)) {
                                            showGiftSheet = false
                                            val me = currentUser
                                            val giftMsg = LiveRoomChatMessage(
                                                id = "gift_${System.currentTimeMillis()}",
                                                senderName = me?.fullName ?: "Siz",
                                                messageText = "${gift.icon} ${gift.name} sovg'asini yubordi! (${gift.coinPrice} 💎)",
                                                vipLevel = 5,
                                                isSystem = true
                                            )
                                            liveComments = liveComments + giftMsg
                                            coroutineScope.launch(Dispatchers.IO) {
                                                com.example.network.FirebaseRealtimeSync.sendVoiceRoomComment(room.id, giftMsg)
                                            }
                                            Toast.makeText(context, "${gift.name} efirchiga yuborildi! 💖", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Tangalar balansi yetarli emas! Do'kondan to'ldiring 💎", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(gift.icon, fontSize = 28.sp)
                                        Text(gift.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("${gift.coinPrice} 💎", color = WarmHoneyGold, fontSize = 11.sp)
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
