package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.SampleVoiceRoomsAndGifts
import com.example.model.VoicePartyRoom
import com.example.ui.components.LiveStreamViewerDialog
import com.example.ui.components.VoicePartyRoomDialog
import com.example.ui.components.VisolPullRefreshContainer
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * STARMAKER VOICE PARTY ROOMS & LIVE STREAMING HUB
 * - Custom room creation with persistence in UserRepository
 * - Filter categories & exact StarMaker visual cards
 */
@Composable
fun VoiceRoomAndStreamScreen(
    userRepository: UserRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = userRepository.themeMode.collectAsState().value != "LIGHT"
    val userCoins by userRepository.userCoins.collectAsState()
    val customRooms by userRepository.customVoiceRooms.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Barchasi") }
    var selectedVoiceRoom by remember { mutableStateOf<VoicePartyRoom?>(null) }
    var selectedStreamRoom by remember { mutableStateOf<VoicePartyRoom?>(null) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }

    // Password Prompt Dialog for private rooms
    var roomToUnlock by remember { mutableStateOf<VoicePartyRoom?>(null) }
    var enteredRoomPassword by remember { mutableStateOf("") }

    // New Room Input Fields
    var newRoomTitle by remember { mutableStateOf("") }
    var newRoomAnnouncement by remember { mutableStateOf("") }
    var newRoomIsPrivate by remember { mutableStateOf(false) }
    var newRoomPassword by remember { mutableStateOf("") }

    val allRooms = remember(customRooms) {
        customRooms + SampleVoiceRoomsAndGifts.sampleVoiceRooms
    }

    val filteredRooms = remember(selectedCategory, allRooms) {
        if (selectedCategory == "Barchasi") {
            allRooms
        } else if (selectedCategory == "Jonli Stream") {
            allRooms.filter { it.isLiveStream }
        } else {
            allRooms.filter { it.category.lowercase().contains(selectedCategory.lowercase()) || !it.isLiveStream }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F081B) else PearlBackground)
    ) {
        VisolPullRefreshContainer(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    delay(800)
                    isRefreshing = false
                    Toast.makeText(context, "Ovozli xonalar va efirlar yangilandi! ✨", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // HERO BANNER
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4A103D),
                                    Color(0xFF23082B),
                                    Color(0xFF0F0418)
                                )
                            )
                        )
                        .border(1.dp, Brush.linearGradient(listOf(WarmHoneyGold, SoftRosePink)), RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x33FFD700)
                            ) {
                                Text(
                                    text = "🎙️ STARMAKER VOICE & LIVE HUB",
                                    color = WarmHoneyGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("💎", fontSize = 12.sp)
                                Text("$userCoins", color = WarmHoneyGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Ovozli Xonalar va Jonli Efir ✨",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "Yangi alohida xona yarating yoki mavjud xonalarga kirib sovg'alar ulashing!",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { showCreateRoomDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmHoneyGold),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yangi Ovozli Xona Yarating 🎙️", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // CATEGORY FILTER CHIPS
            val categories = listOf("Barchasi", "Tanishuv & Gurung", "Musiqa & Karaoke", "Jonli Stream")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) WarmHoneyGold else if (isDark) Color(0x33FFFFFF) else Color(0x10000000),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.Black else if (isDark) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ROOMS GRID FEED
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredRooms) { roomItem ->
                    VoiceRoomCardItem(
                        room = roomItem,
                        isDark = isDark,
                        onClick = {
                            if (roomItem.isPrivate && !roomItem.roomPassword.isNullOrBlank()) {
                                roomToUnlock = roomItem
                            } else if (roomItem.isLiveStream) {
                                selectedStreamRoom = roomItem
                            } else {
                                selectedVoiceRoom = roomItem
                            }
                        }
                    )
                }
            }
        }
    }

        // VOICE ROOM DIALOG
        selectedVoiceRoom?.let { room ->
            VoicePartyRoomDialog(
                room = room,
                userRepository = userRepository,
                onDismiss = { selectedVoiceRoom = null }
            )
        }

        // STREAM VIEWER DIALOG
        selectedStreamRoom?.let { room ->
            LiveStreamViewerDialog(
                room = room,
                userRepository = userRepository,
                onDismiss = { selectedStreamRoom = null }
            )
        }

        // UNLOCK PRIVATE ROOM DIALOG
        roomToUnlock?.let { targetRoom ->
            AlertDialog(
                onDismissRequest = { roomToUnlock = null },
                title = { Text("🔒 Maxfiy Xona Parolini Kiritasiz", color = WarmHoneyGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ushbu xona maxfiy (Private). Davom etish uchun parolni kiriting:", color = Color.White, fontSize = 13.sp)
                        OutlinedTextField(
                            value = enteredRoomPassword,
                            onValueChange = { enteredRoomPassword = it },
                            label = { Text("Xona Paroli") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmHoneyGold)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredRoomPassword == targetRoom.roomPassword) {
                                selectedVoiceRoom = targetRoom
                                roomToUnlock = null
                                enteredRoomPassword = ""
                            } else {
                                Toast.makeText(context, "Xato parol kiritildi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmHoneyGold)
                    ) {
                        Text("Xonaga Kirish 🔓", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { roomToUnlock = null }) {
                        Text("Bekor qilish", color = Color.White)
                    }
                },
                containerColor = Color(0xF21C0F2D)
            )
        }

        // CREATE NEW CUSTOM ROOM DIALOG
        if (showCreateRoomDialog) {
            AlertDialog(
                onDismissRequest = { showCreateRoomDialog = false },
                title = { Text("🎙️ Yangi Alohida Ovozli Xona Yarating", color = WarmHoneyGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newRoomTitle,
                            onValueChange = { newRoomTitle = it },
                            label = { Text("Xona Nomi") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmHoneyGold)
                        )
                        OutlinedTextField(
                            value = newRoomAnnouncement,
                            onValueChange = { newRoomAnnouncement = it },
                            label = { Text("Pin Qilingan E'lon Matni") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmHoneyGold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Maxfiy Xona (🔒 Password)", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = newRoomIsPrivate,
                                onCheckedChange = { newRoomIsPrivate = it }
                            )
                        }
                        if (newRoomIsPrivate) {
                            OutlinedTextField(
                                value = newRoomPassword,
                                onValueChange = { newRoomPassword = it },
                                label = { Text("Xona Paroli") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmHoneyGold)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val createdRoom = userRepository.createCustomVoiceRoom(
                                title = newRoomTitle,
                                isPrivate = newRoomIsPrivate,
                                password = if (newRoomIsPrivate) newRoomPassword else null,
                                announcement = newRoomAnnouncement
                            )
                            showCreateRoomDialog = false
                            newRoomTitle = ""
                            newRoomAnnouncement = ""
                            newRoomPassword = ""
                            selectedVoiceRoom = createdRoom
                            Toast.makeText(context, "Yangi shaxsiy xonangiz yaratildi va saqlandi! 👑", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmHoneyGold)
                    ) {
                        Text("Xonani Yaratish va Kirish 👑", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateRoomDialog = false }) {
                        Text("Bekor qilish", color = Color.White)
                    }
                },
                containerColor = Color(0xF21C0F2D)
            )
        }
    }
}

@Composable
private fun VoiceRoomCardItem(
    room: VoicePartyRoom,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) Color(0x33FFFFFF) else Color.White,
        border = BorderStroke(1.dp, if (room.isLiveStream) Color(0xFFEF4444) else WarmHoneyGold.copy(alpha = 0.5f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = room.hostPhotoPath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(2.dp, WarmHoneyGold, CircleShape)
                        )
                        if (room.isPrivate) {
                            Text("🔒", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopStart))
                        }
                    }

                    Column {
                        Text(
                            text = room.title,
                            color = if (isDark) Color.White else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Boshlovchi: ${room.hostName}",
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (room.isLiveStream) Color(0x33EF4444) else Color(0x33FFD700)
                ) {
                    Text(
                        text = if (room.isLiveStream) "LIVE 📹" else "🎙️ PARTY",
                        color = if (room.isLiveStream) Color(0xFFEF4444) else WarmHoneyGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (!room.isLiveStream && room.speakers.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spikerlar:",
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else TextSecondary,
                        fontSize = 11.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-8).dp)
                    ) {
                        room.speakers.take(5).forEach { spk ->
                            AsyncImage(
                                model = spk.photoPath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                    Text(
                        text = "👥 ${room.listenerCount} kishi",
                        color = WarmHoneyGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
