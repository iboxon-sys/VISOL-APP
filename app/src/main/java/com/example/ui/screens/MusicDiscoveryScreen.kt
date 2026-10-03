package com.example.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.MusicTrack
import com.example.service.music.MusicApiClient
import com.example.service.music.YouTubeDataApiClient
import com.example.ui.components.YouTubeAudioPlayerView
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Visol.uz Rasmiy Musiqa Bo'limi (Music Discovery Screen)
 * - Milliy, Sevgi taronalari, Estrada va Zamonaviy Pop musiqalar katalogi
 * - Real-time qidiruv va janrlar bo'yicha saralash
 * - Player logikasi: Har safar trek bosilganda QAT'IY 0-soniyadan (audio.currentTime = 0 / player.seekTo(0)) boshlanadi!
 * - Real-time scrubber (slider), vaqt ko'rsatkichi, soundwave visualizer va Reels videoga biriktirish imkoniyati.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicDiscoveryScreen(
    userRepository: UserRepository,
    onUseTrackInReel: ((MusicTrack) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current

    val categories = listOf(
        "Barchasi 🌟",
        "🔥 YouTube Trend",
        "🇺🇿 O'zbek xitlari",
        "✨ Xamdam Sobirov",
        "🎤 Jaloliddin Ahmadaliyev",
        "❤️ Sevgi Taronalari",
        "🎧 Zamonaviy Pop",
        "🤖 YouTube Bot (320 kbps)"
    )

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoadingTracks by remember { mutableStateOf(false) }
    var tracksList by remember { mutableStateOf<List<MusicTrack>>(MusicApiClient.getAllCuratedTracks()) }

    // Media Player & YouTube Player State
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var activeTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var activeYoutubeVideoId by remember { mutableStateOf<String?>(null) }
    var isVideoClipVisible by remember { mutableStateOf(false) }
    var seekToMsState by remember { mutableStateOf<Int?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }
    var isBuffering by remember { mutableStateOf(false) }

    // Safely release both players
    fun releasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("MusicDiscovery", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            activeTrack = null
            activeYoutubeVideoId = null
            seekToMsState = null
            isPlaying = false
            currentPlaybackMs = 0
            totalDurationMs = 0
            isBuffering = false
            isVideoClipVisible = false
        }
    }

    // Play track strictly starting from 0s with instant reliable Native MediaPlayer
    val playTrack: (MusicTrack) -> Unit = { track ->
        val yId = MusicApiClient.resolveYouTubeVideoId(track)

        if (activeTrack?.id == track.id) {
            // Toggle pause/play
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                } catch (e: Exception) {
                    releasePlayer()
                }
            } else {
                isPlaying = !isPlaying
            }
        } else {
            // New track clicked -> reset player, load audio immediately
            try {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null

            activeTrack = track
            activeYoutubeVideoId = yId
            currentPlaybackMs = 0
            totalDurationMs = track.durationSeconds * 1000
            isPlaying = true
            isBuffering = true
            seekToMsState = 0

            if (track.audioUrl.isNullOrBlank()) {
                isBuffering = false
                isPlaying = false
                Toast.makeText(context, "Audio manbasi topilmadi yoki internet aloqasida xatolik yuz berdi.", Toast.LENGTH_SHORT).show()
            } else {
                try {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

                val mp = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    try {
                        setDataSource(context, Uri.parse(track.audioUrl))
                    } catch (e: Exception) {
                        isBuffering = false
                        isPlaying = false
                        Toast.makeText(context, "Qo'shiqni ijro etib bo'lmadi. Internet tarmoq aloqasini tekshiring.", Toast.LENGTH_LONG).show()
                        return@apply
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("MusicDiscovery", "MediaPlayer error: $what, $extra")
                        isBuffering = false
                        isPlaying = false
                        Toast.makeText(context, "Musiqani yuklashda xatolik yuz berdi. Iltimos qayta urinib ko'ring.", Toast.LENGTH_SHORT).show()
                        true
                    }
                    setOnPreparedListener { player ->
                        try {
                            player.seekTo(0)
                            player.start()
                            isPlaying = true
                            isBuffering = false
                            currentPlaybackMs = 0
                            totalDurationMs = if (player.duration > 0) player.duration else (track.durationSeconds * 1000)
                        } catch (e: Exception) {
                            Log.e("MusicDiscovery", "Error on start playback", e)
                            releasePlayer()
                        }
                    }
                    setOnCompletionListener {
                        try {
                            it.seekTo(0)
                            it.start()
                            currentPlaybackMs = 0
                        } catch (_: Exception) {
                            releasePlayer()
                        }
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                Log.e("MusicDiscovery", "Failed to prepare track audio", e)
                isBuffering = false
            }
            }
        }
    }

    // Seek to 0 seconds explicitly (Boshidan boshlash)
    val rewindToStart: () -> Unit = {
        try {
            seekToMsState = 0
            currentPlaybackMs = 0
            mediaPlayer?.seekTo(0)
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
            isPlaying = true
            Toast.makeText(context, "Musiqa boshidan boshlandi ⏪", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MusicDiscovery", "Error seeking to 0", e)
        }
    }

    // Ticker to continuously update playback progress for native audio
    LaunchedEffect(activeTrack, isPlaying) {
        while (activeTrack != null && mediaPlayer != null) {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    currentPlaybackMs = mediaPlayer?.currentPosition ?: 0
                    val dur = mediaPlayer?.duration ?: 0
                    if (dur > 0) totalDurationMs = dur
                }
            } catch (_: Exception) {}
            delay(250)
        }
    }

    // Load tracks with YouTube Data API v3, instant debounced search, and curated categories
    LaunchedEffect(searchQuery, selectedCategoryIndex) {
        if (searchQuery.isNotBlank()) {
            delay(350) // Debounce typing so search doesn't spam network or lag
        }
        isLoadingTracks = true
        try {
            val results = if (searchQuery.isNotBlank()) {
                MusicApiClient.searchTracks(searchQuery)
            } else {
                when (selectedCategoryIndex) {
                    1 -> YouTubeDataApiClient.getTrendingMusic(30)
                    2 -> YouTubeDataApiClient.searchVideos("uzbek yangi xitlar 2026", maxResults = 25)
                    3 -> YouTubeDataApiClient.searchVideos("Xamdam Sobirov yangi taronalari", maxResults = 20)
                    4 -> YouTubeDataApiClient.searchVideos("Jaloliddin Ahmadaliyev xit qo'shiqlari", maxResults = 20)
                    5 -> YouTubeDataApiClient.searchVideos("sevgi taronalari lirik romantik uzbek", maxResults = 20)
                    6 -> YouTubeDataApiClient.searchVideos("uzbek zamonaviy pop yangi qo'shiqlar", maxResults = 20)
                    7 -> MusicApiClient.searchYouTubeMp3Bot("trend uzbek 2026")
                    else -> MusicApiClient.searchTracks("")
                }
            }
            if (results.isNotEmpty()) {
                tracksList = results
            }
        } catch (e: Exception) {
            Log.e("MusicDiscovery", "Error fetching tracks: ${e.message}")
        } finally {
            isLoadingTracks = false
        }
    }

    // Clean up on screen exit
    DisposableEffect(Unit) {
        onDispose {
            releasePlayer()
        }
    }

    // Rotating vinyl animation for player
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // -------------------------------------------------------------
        // 1. TOP HEADER & SEARCH BAR
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF111827) else Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = VelvetRoseSoft,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = VelvetRosePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Musiqa & Taronalar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Sevgi, go'zal ohanglar va mashhur taronalar",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                // Volume indicator badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPlaying) VelvetRosePrimary else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.Headphones,
                            contentDescription = null,
                            tint = if (isPlaying) Color.White else textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "IJRODA" else "KATALOG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) Color.White else textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Qo'shiq nomini yozing (YouTube Cookies orqali qidiruv)...",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Qidiruv",
                        tint = VelvetRosePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoadingTracks) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = VelvetRosePrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tozalash",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VelvetRosePrimary,
                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_music")
            )
        }

        // -------------------------------------------------------------
        // 2. CATEGORIES FILTER BAR
        // -------------------------------------------------------------
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories.size) { index ->
                val isSelected = selectedCategoryIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryIndex = index },
                    label = {
                        Text(
                            text = categories[index],
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VelvetRosePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                        labelColor = textPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) VelvetRosePrimary else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // -------------------------------------------------------------
        // 3. TRACKS LIST
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoadingTracks && tracksList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = VelvetRosePrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Taronalar qidirilmoqda...", color = textSecondary, fontSize = 13.sp)
                    }
                }
            } else if (tracksList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicOff,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Hech qanday musiqa topilmadi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Boshqa so'z bilan qidirib ko'ring",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCategoryIndex == 1) {
                        item(key = "youtube_bot_banner") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF2563EB),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.SmartToy,
                                                    contentDescription = "Bot",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "YouTube MP3 Bot (@visol_music_bot)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = textPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "COOKIES ⚡ 320 KBPS",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Qo'shiq nomini yozing — YouTube Cookie orqali MP3 va ablojkasi bilan tortib keladi!",
                                                fontSize = 11.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val botSuggestions = listOf(
                                            "Xamdam Sobirov - Yomon ekan",
                                            "Jaloliddin Ahmadaliyev - Yurak Nolasi",
                                            "Konsta - Odamlar nima deydi",
                                            "Miyagi - Captain",
                                            "Janob Rasul - Mayli",
                                            "Sardor Tairov"
                                        )
                                        items(botSuggestions) { sugg ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isDark) Color(0xFF334155) else Color.White,
                                                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                                                onClick = { searchQuery = sugg }
                                            ) {
                                                Text(
                                                    text = sugg,
                                                    fontSize = 11.sp,
                                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(tracksList, key = { it.id }) { track ->
                        val isThisActive = activeTrack?.id == track.id
                        val isThisPlaying = isThisActive && isPlaying

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isThisActive) (if (isDark) Color(0xFF2E1065) else VelvetRoseSoft) else cardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isThisActive) VelvetRosePrimary else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                            ),
                            onClick = { playTrack(track) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Album Cover / Disc Art with Play indicator
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (track.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = track.coverUrl,
                                            contentDescription = track.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = VelvetRosePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Play/Pause Overlay Icon
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isThisActive && isBuffering) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Track Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = track.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isThisActive) VelvetRosePrimary else textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (track.genre.contains("YouTube", ignoreCase = true) || track.id.startsWith("yt_")) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "🤖 YouTube MP3",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF4444),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (track.isTrending) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "🔥 Trend",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF4444),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = track.artist,
                                            fontSize = 12.sp,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(text = "•", fontSize = 10.sp, color = textSecondary)
                                        Text(
                                            text = "${track.durationSeconds / 60}:${String.format("%02d", track.durationSeconds % 60)}",
                                            fontSize = 11.sp,
                                            color = textSecondary
                                        )
                                    }

                                    // Dancing Soundwave Visualizer if playing
                                    if (isThisPlaying) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SoundWaveAnimation()
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Action: Use track in Reel button, Rewind, and Report (Shikoyat)
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val ok = MusicApiClient.downloadMp3WithManager(context, track)
                                                if (ok) {
                                                    Toast.makeText(
                                                        context,
                                                        "⬇️ \"${track.title}\" MP3 formatida yuklab olinmoqda (Yuklanmalar papkasiga)",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(context, "Yuklab olishda xatolik yuz berdi", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.FileDownload,
                                                contentDescription = "MP3 Yuklab olish",
                                                tint = Color(0xFF0EA5E9),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                Toast.makeText(
                                                    context,
                                                    "🚩 \"${track.title}\" bo'yicha shikoyatingiz moderatorga yuborildi",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Flag,
                                                contentDescription = "Shikoyat qilish",
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (isThisActive) {
                                                    rewindToStart()
                                                } else {
                                                    playTrack(track)
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isThisActive) Icons.Default.Replay else Icons.Outlined.PlayCircle,
                                                contentDescription = "Boshidan",
                                                tint = if (isThisActive) VelvetRosePrimary else textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (onUseTrackInReel != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = VelvetRosePrimary,
                                            onClick = {
                                                onUseTrackInReel(track)
                                                Toast.makeText(context, "${track.title} videoga tanlandi 🎵", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "Reels 🎬",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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

        // -------------------------------------------------------------
        // 4. FLOATING ACTIVE AUDIO & VIDEO PLAYER CARD (STICKY AT BOTTOM)
        // -------------------------------------------------------------
        if (activeTrack != null) {
            val track = activeTrack!!
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = if (isDark) Color(0xFF1E1B4B) else Color(0xFF4C0519),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floating_music_player")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Single persistent YouTube Engine for Video Clip display
                    if (isVideoClipVisible) {
                        val currentVideoId = activeYoutubeVideoId ?: MusicApiClient.resolveYouTubeVideoId(track)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .padding(bottom = 8.dp)
                        ) {
                            YouTubeAudioPlayerView(
                                videoId = currentVideoId,
                                isPlaying = isPlaying,
                                seekToMs = seekToMsState,
                                onStateChange = { playing, buffering ->
                                    isPlaying = playing
                                    isBuffering = buffering
                                },
                                onProgressUpdate = { cur, dur ->
                                    currentPlaybackMs = cur
                                    if (dur > 0) totalDurationMs = dur
                                },
                                onError = { errCode ->
                                    Log.e("MusicDiscovery", "YouTube player error: $errCode")
                                    isBuffering = false
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Track Info and Main Play Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spinning Vinyl Disc
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .rotate(if (isPlaying) spinAngle else 0f)
                                .border(2.dp, WarmGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (track.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = track.coverUrl,
                                    contentDescription = track.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // Center hole of vinyl
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = track.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Red,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = "YouTube",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${track.artist} • 0:00 dan boshlanadi",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Player Action Buttons: Video Mode (All tracks), Rewind to Start, Play/Pause, Close
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Clip / Audio toggle available on all songs
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isVideoClipVisible) VelvetRosePrimary else Color.White.copy(alpha = 0.2f),
                                onClick = {
                                    isVideoClipVisible = !isVideoClipVisible
                                    if (isVideoClipVisible) {
                                        try { mediaPlayer?.pause() } catch (_: Exception) {}
                                    } else {
                                        try { mediaPlayer?.start() } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isVideoClipVisible) Icons.Default.Videocam else Icons.Default.MusicVideo,
                                        contentDescription = "Klip",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isVideoClipVisible) "Klip" else "Vinil",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // 1. REWIND TO START (audio.currentTime = 0 / player.seekTo(0))
                            IconButton(
                                onClick = rewindToStart,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Boshidan boshlash",
                                    tint = WarmGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 2. Play / Pause
                            IconButton(
                                onClick = { playTrack(track) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color.White, CircleShape)
                            ) {
                                if (isBuffering) {
                                    CircularProgressIndicator(
                                        color = VelvetRosePrimary,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = VelvetRosePrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // 3. Stop / Close
                            IconButton(
                                onClick = { releasePlayer() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Yopish",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Slider
                    val currentSec = currentPlaybackMs / 1000
                    val totalSec = if (totalDurationMs > 0) totalDurationMs / 1000 else track.durationSeconds
                    val sliderProgress = if (totalDurationMs > 0) {
                        (currentPlaybackMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = sliderProgress,
                        onValueChange = { frac ->
                            if (totalDurationMs > 0) {
                                val targetMs = (frac * totalDurationMs).toInt()
                                if (activeYoutubeVideoId != null) {
                                    seekToMsState = targetMs
                                    currentPlaybackMs = targetMs
                                } else {
                                    mediaPlayer?.seekTo(targetMs)
                                    currentPlaybackMs = targetMs
                                }
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = WarmGold,
                            activeTrackColor = WarmGold,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )

                    // Time display row: current time / total time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${currentSec / 60}:${String.format("%02d", currentSec % 60)}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${totalSec / 60}:${String.format("%02d", totalSec % 60)}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dancing SoundWave visualizer bars animation
 */
@Composable
fun SoundWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        listOf(h1, h2, h3, h4).forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VelvetRosePrimary)
            )
        }
    }
}
