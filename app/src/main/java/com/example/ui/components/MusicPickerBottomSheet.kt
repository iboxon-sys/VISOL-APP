package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MusicTrack
import com.example.service.music.MusicApiClient
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Global Music Selection Library & Audio Overlay Engine
 * Powered by Senior Mobile Audio/Video Editing & MediaPipeline Engine.
 * Supports:
 * - Real-time track search & filter tags
 * - Automatic duration lock syncing with videoDuration
 * - Draggable glassmorphic waveform audio cropper
 * - Real-time looping audio preview of trimmed range
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPickerBottomSheet(
    onDismiss: () -> Unit,
    onTrackSelected: (MusicTrack) -> Unit,
    initialTrack: MusicTrack? = null,
    videoDuration: Float? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ommabop, 1: Original Sound, 2: Janrlar, 3: Saqlangan
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loadedTracks by remember { mutableStateOf<List<MusicTrack>>(MusicApiClient.getAllCuratedTracks()) }

    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var activePlayingTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var currentPlaybackMs by remember { mutableIntStateOf(0) }
    var totalPlaybackMs by remember { mutableIntStateOf(0) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekSliderValue by remember { mutableFloatStateOf(0f) }

    // Segment Cutter State - faqat foydalanuvchi qirqish tugmasini bosgandagina ochiladi
    var trackForSegmentCutter by remember { mutableStateOf<MusicTrack?>(null) }
    
    // Automatic Duration Lock: Sync segment length with video duration if provided, otherwise default to 15s
    val lockedLengthSec = remember(videoDuration) {
        videoDuration ?: 15f
    }
    
    var segmentStartSec by remember { mutableFloatStateOf(initialTrack?.trimStartSec ?: 0f) }
    var segmentLengthSec by remember { mutableFloatStateOf(initialTrack?.trimLengthSec?.toFloat() ?: lockedLengthSec) }

    // Update segment length if videoDuration changes
    LaunchedEffect(videoDuration) {
        if (videoDuration != null) {
            segmentLengthSec = videoDuration
        }
    }

    // Live search & tab change effect with debounce
    LaunchedEffect(searchQuery, selectedTab) {
        isLoading = true
        delay(300) // Debounce search
        try {
            val tracks = when (selectedTab) {
                1 -> MusicApiClient.searchYouTubeMp3Bot(searchQuery.ifBlank { "trend uzbek" })
                2 -> MusicApiClient.getOriginalSounds().filter {
                    searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
                }
                3 -> MusicApiClient.searchTracks(searchQuery.ifBlank { "Pop" }, genreTag = "Pop")
                4 -> MusicApiClient.getOriginalSounds()
                else -> MusicApiClient.searchTracks(searchQuery)
            }
            loadedTracks = tracks
        } catch (e: Exception) {
            loadedTracks = emptyList()
        } finally {
            isLoading = false
        }
    }

    // Full Music Playback: Musiqalarni boshidan oxirigacha to'liq ijro etish
    // Faqat qirqish oynasi (trackForSegmentCutter != null) ochiq bo'lsagina belgilangan oraliqda loop qilinadi
    LaunchedEffect(currentlyPlayingId, trackForSegmentCutter, segmentStartSec, segmentLengthSec) {
        val isTrimming = trackForSegmentCutter != null
        if (currentlyPlayingId != null && mediaPlayer != null && isTrimming) {
            while (trackForSegmentCutter != null && currentlyPlayingId != null) {
                try {
                    val currentPos = mediaPlayer?.currentPosition ?: 0
                    val targetEnd = ((segmentStartSec + segmentLengthSec) * 1000).toInt()
                    val targetStart = (segmentStartSec * 1000).toInt()
                    
                    if (currentPos >= targetEnd || currentPos < targetStart - 300) {
                        mediaPlayer?.seekTo(targetStart)
                    }
                } catch (_: Exception) {}
                delay(100)
            }
        }
    }

    // Centralized MediaPlayer release function
    val releaseMediaPlayer = {
        try {
            mediaPlayer?.let { mp ->
                try {
                    mp.setOnCompletionListener(null)
                    mp.setOnErrorListener(null)
                    mp.setOnPreparedListener(null)
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                } catch (_: Exception) {}
                try {
                    mp.reset()
                } catch (_: Exception) {}
                try {
                    mp.release()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("MusicPicker", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            currentlyPlayingId = null
            activePlayingTrack = null
            isAudioPlaying = false
            currentPlaybackMs = 0
            totalPlaybackMs = 0
        }
    }

    val playPreview: (MusicTrack, Float) -> Unit = { track, startFromSec ->
        if (currentlyPlayingId == track.id && mediaPlayer != null) {
            // Toggle pause / play if same track
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    isAudioPlaying = false
                } else {
                    mediaPlayer?.start()
                    isAudioPlaying = true
                }
            } catch (e: Exception) {
                releaseMediaPlayer()
            }
        } else {
            releaseMediaPlayer()
            activePlayingTrack = track
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

                val mp = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, Uri.parse(track.audioUrl))
                    setOnErrorListener { _, what, extra ->
                        Log.e("MusicPicker", "MediaPlayer error: what=$what, extra=$extra")
                        releaseMediaPlayer()
                        true
                    }
                    setOnPreparedListener { player ->
                        try {
                            val startMs = (startFromSec * 1000).toInt()
                            if (startMs > 0) {
                                player.seekTo(startMs)
                            } else {
                                player.seekTo(0) // ALWAYS start from 0s (boshidan boshlanishi shart)
                            }
                            player.start()
                            currentlyPlayingId = track.id
                            isAudioPlaying = true
                            currentPlaybackMs = startMs
                            totalPlaybackMs = if (player.duration > 0) player.duration else (track.durationSeconds * 1000).toInt()
                        } catch (e: Exception) {
                            Log.e("MusicPicker", "Error starting playback", e)
                            releaseMediaPlayer()
                        }
                    }
                    setOnCompletionListener {
                        releaseMediaPlayer()
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                Log.e("MusicPicker", "Failed to start preview", e)
                releaseMediaPlayer()
            }
        }
    }

    // Continuous ticker for slider & time display
    LaunchedEffect(currentlyPlayingId, isAudioPlaying) {
        while (currentlyPlayingId != null && mediaPlayer != null) {
            try {
                if (!isUserSeeking && mediaPlayer?.isPlaying == true) {
                    currentPlaybackMs = mediaPlayer?.currentPosition ?: 0
                    val dur = mediaPlayer?.duration ?: 0
                    if (dur > 0) {
                        totalPlaybackMs = dur
                    }
                }
            } catch (_: Exception) {}
            delay(200)
        }
    }

    val selectTrackFull: (MusicTrack) -> Unit = { track ->
        releaseMediaPlayer()
        onTrackSelected(
            track.copy(
                trimStartSec = 0f,
                trimLengthSec = track.durationSeconds
            )
        )
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseMediaPlayer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = {
                releaseMediaPlayer()
                onDismiss()
            }),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = DeepNavyMesh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(VelvetRosePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Musiqa va Audio Kutubxona",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = if (videoDuration != null) "Sinxronlangan video: ${"%.1f".format(videoDuration)}s • To'liq tanlanadi" else "Boshidan oxirigacha to'liq tanlash",
                                fontSize = 11.sp,
                                color = VelvetRoseLight
                            )
                        }
                    }
                    IconButton(onClick = {
                        releaseMediaPlayer()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = Color.White)
                    }
                }

            // Waveform Audio Trimmer State View: Takes full screen space so lists are never squished!
            if (trackForSegmentCutter != null) {
                val cutTrack = trackForSegmentCutter!!
                val amplitudes = remember(cutTrack.id) {
                    cutTrack.waveformAmplitudes.ifEmpty {
                        MusicApiClient.generateWaveformForTrack(cutTrack.id, 50)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, VelvetRosePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.ContentCut, contentDescription = null, tint = VelvetRosePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Audio Qirqish & Sinxronlash",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${cutTrack.title} • ${cutTrack.artist}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = {
                                releaseMediaPlayer()
                                trackForSegmentCutter = null
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Audio Player Preview control inside Trimmer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x11FFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    playPreview(cutTrack, segmentStartSec)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(VelvetRosePrimary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (currentlyPlayingId == cutTrack.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Preview",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (currentlyPlayingId == cutTrack.id) "Jonli ijro eshitilmoqda 🎧" else "Tinglash uchun bosing",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Tanlangan oraliq avtomatik aylanadi (loop)",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Label info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Interaktiv To'lqinli Qirquvchi (Waveform)",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VelvetRosePrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Uzunlik: ${"%.1f".format(segmentLengthSec)}s",
                                    color = VelvetRoseLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // INTERACTIVE WAVEFORM DRAGGER / CROPPING WINDOW ENGINE
                        var boxWidth by remember { mutableIntStateOf(1) }
                        val density = LocalDensity.current
                        val windowWidthPercent = (segmentLengthSec / cutTrack.durationSeconds.toFloat()).coerceIn(0.1f, 1.0f)

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F172A))
                                .onSizeChanged { boxWidth = it.width }
                        ) {
                            val maxWidthPx = boxWidth.toFloat()
                            val windowWidthPx = maxWidthPx * windowWidthPercent
                            val maxStartPx = (maxWidthPx - windowWidthPx).coerceAtLeast(0f)

                            val initialOffsetPx = (segmentStartSec / cutTrack.durationSeconds.toFloat()) * maxWidthPx
                            var dragOffsetPx by remember(segmentStartSec, boxWidth) { mutableFloatStateOf(initialOffsetPx.coerceIn(0f, maxStartPx)) }

                            // Render full Waveform Bars behind window
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val totalPoints = amplitudes.size.coerceAtLeast(1)
                                amplitudes.forEachIndexed { idx, amp ->
                                    val pointPercent = idx.toFloat() / totalPoints.toFloat()
                                    val startPercent = dragOffsetPx / maxWidthPx
                                    val endPercent = (dragOffsetPx + windowWidthPx) / maxWidthPx
                                    val inRange = pointPercent in startPercent..endPercent
                                    val barHeight = (amp * 60).dp

                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (inRange) VelvetRosePrimary else Color(0xFF334155))
                                    )
                                }
                            }

                            // Sliding Glassmorphic Selection Window Overlay (Drag to crop)
                            val dragOffsetDp = with(density) { dragOffsetPx.toDp() }
                            val windowWidthDp = with(density) { windowWidthPx.toDp() }

                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = dragOffsetDp)
                                    .width(windowWidthDp)
                                    .fillMaxHeight()
                                    .background(Color(0x33F43F5E))
                                    .border(2.dp, VelvetRosePrimary, RoundedCornerShape(10.dp))
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newOffset = (dragOffsetPx + dragAmount.x).coerceIn(0f, maxStartPx)
                                                dragOffsetPx = newOffset
                                                
                                                val newStartSec = (newOffset / maxWidthPx) * cutTrack.durationSeconds
                                                segmentStartSec = newStartSec.coerceIn(0f, cutTrack.durationSeconds.toFloat() - segmentLengthSec)
                                                
                                                if (currentlyPlayingId == cutTrack.id && mediaPlayer != null) {
                                                    try {
                                                        mediaPlayer?.seekTo((segmentStartSec * 1000).toInt())
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        )
                                    }
                            ) {
                                // Glassmorphic Slider handles
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 2.dp)
                                        .width(5.dp)
                                        .fillMaxHeight(0.6f)
                                        .background(Color.White, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 2.dp)
                                        .width(5.dp)
                                        .fillMaxHeight(0.6f)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Waveform time details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val startMin = segmentStartSec.toInt() / 60
                            val startSec = segmentStartSec.toInt() % 60
                            val endMin = (segmentStartSec + segmentLengthSec).toInt() / 60
                            val endSec = (segmentStartSec + segmentLengthSec).toInt() % 60
                            val totalMin = cutTrack.durationSeconds / 60
                            val totalSec = cutTrack.durationSeconds % 60

                            Text(
                                text = "Boshlanishi: %d:%02d".format(startMin, startSec),
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tugashi: %d:%02d / %d:%02d".format(endMin, endSec, totalMin, totalSec),
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Full song selection & Trimmed segment selection buttons
                        Button(
                            onClick = {
                                selectTrackFull(cutTrack)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Boshidan oxirigacha to'liq tanlash ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                releaseMediaPlayer()
                                onTrackSelected(
                                    cutTrack.copy(
                                        trimStartSec = segmentStartSec,
                                        trimLengthSec = segmentLengthSec.toInt(),
                                        title = if (cutTrack.isOriginalSound) cutTrack.title else "${cutTrack.title} (${segmentStartSec.toInt()}s - ${(segmentStartSec + segmentLengthSec).toInt()}s)"
                                    )
                                )
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, VelvetRoseLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = VelvetRoseLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Faqat kesilgan oraliqni biriktirish (${segmentStartSec.toInt()}s - ${(segmentStartSec + segmentLengthSec).toInt()}s)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = VelvetRoseLight)
                        }
                    }
                }
            } else {
                // Live Search Input (Normal mode)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Qoshiq, xonanda yoki @username...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VelvetRoseLight) },
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = VelvetRosePrimary,
                                strokeWidth = 2.dp
                            )
                        } else if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x331E293B),
                        unfocusedContainerColor = Color(0x221E293B),
                        focusedBorderColor = VelvetRosePrimary,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabButton(title = "Ommabop 🔥", isSelected = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f))
                    TabButton(title = "YouTube 🤖", isSelected = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1.1f))
                    TabButton(title = "Original 🎙️", isSelected = selectedTab == 2, onClick = { selectedTab = 2 }, modifier = Modifier.weight(1f))
                    TabButton(title = "Janrlar 🎵", isSelected = selectedTab == 3, onClick = { selectedTab = 3 }, modifier = Modifier.weight(1f))
                    TabButton(title = "Saqlangan ⭐", isSelected = selectedTab == 4, onClick = { selectedTab = 4 }, modifier = Modifier.weight(1f))
                }

                // Spacious LazyColumn layout that NEVER qisilib qoladi (Never squishes)
                if (isLoading && loadedTracks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = VelvetRosePrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Live Music API catalogi yuklanmoqda...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                } else if (loadedTracks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicOff,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Hech qanday musiqa topilmadi",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Boshqa musiqa nomi yoki ijrochi ismini qidirib ko'ring.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(loadedTracks, key = { it.id }) { track ->
                            val isPlaying = currentlyPlayingId == track.id
                            MusicTrackItem(
                                track = track,
                                isPlaying = isPlaying,
                                onPlayPreview = {
                                    playPreview(track, 0f)
                                },
                                onCutSegment = {
                                    trackForSegmentCutter = track
                                    segmentStartSec = 0f
                                    segmentLengthSec = lockedLengthSec
                                },
                                onSelect = {
                                    selectTrackFull(track)
                                }
                            )
                        }
                    }
                }

                // Persistent Bottom Music Player Bar with Draggable Scrub Slider & Song Name
                if (activePlayingTrack != null) {
                    val track = activePlayingTrack!!
                    val effectivePosition = if (isUserSeeking) seekSliderValue else currentPlaybackMs.toFloat()
                    val effectiveMax = (totalPlaybackMs.coerceAtLeast(1000)).toFloat()

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, VelvetRosePrimary.copy(alpha = 0.7f)),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini Cover
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = track.coverUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Track Title & Artist
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Rewind -10s
                                IconButton(
                                    onClick = {
                                        val newPos = (currentPlaybackMs - 10000).coerceAtLeast(0)
                                        try {
                                            mediaPlayer?.seekTo(newPos)
                                            currentPlaybackMs = newPos
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "-10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Play / Pause Button
                                IconButton(
                                    onClick = {
                                        playPreview(track, 0f)
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(VelvetRosePrimary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Forward +10s
                                IconButton(
                                    onClick = {
                                        val newPos = (currentPlaybackMs + 10000).coerceAtMost(totalPlaybackMs)
                                        try {
                                            mediaPlayer?.seekTo(newPos)
                                            currentPlaybackMs = newPos
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "+10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Select / Tanlash Button
                                Surface(
                                    onClick = { selectTrackFull(track) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tanlash",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Draggable Scrub Slider (Qo'lda surish uchun)
                            Slider(
                                value = effectivePosition.coerceIn(0f, effectiveMax),
                                onValueChange = { newVal ->
                                    isUserSeeking = true
                                    seekSliderValue = newVal
                                },
                                onValueChangeFinished = {
                                    isUserSeeking = false
                                    try {
                                        mediaPlayer?.seekTo(seekSliderValue.toInt())
                                        currentPlaybackMs = seekSliderValue.toInt()
                                    } catch (_: Exception) {}
                                },
                                valueRange = 0f..effectiveMax,
                                colors = SliderDefaults.colors(
                                    thumbColor = VelvetRoseLight,
                                    activeTrackColor = VelvetRosePrimary,
                                    inactiveTrackColor = Color(0xFF475569)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp)
                            )

                            // Time Labels (current / total)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatPlaybackTime(effectivePosition.toInt()),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = formatPlaybackTime(effectiveMax.toInt()),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
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
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) VelvetRosePrimary else Color(0xFF1E293B),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun MusicTrackItem(
    track: MusicTrack,
    isPlaying: Boolean,
    onPlayPreview: () -> Unit,
    onCutSegment: () -> Unit,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x1AFFFFFF),
        border = BorderStroke(1.dp, if (isPlaying) VelvetRosePrimary else Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover & Play Preview button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onPlayPreview,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPlayPreview() }
            ) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.isOriginalSound) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = VelvetRosePrimary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "Original",
                                fontSize = 9.sp,
                                color = VelvetRoseLight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "${track.artist} • ${track.genre}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }

            // Cutter & Select Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCutSegment,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF334155), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ContentCut,
                        contentDescription = "Qirqish (Waveform)",
                        tint = VelvetRoseLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onSelect,
                    shape = RoundedCornerShape(10.dp),
                    color = VelvetRosePrimary
                ) {
                    Text(
                        text = "Tanlash",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun formatPlaybackTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

