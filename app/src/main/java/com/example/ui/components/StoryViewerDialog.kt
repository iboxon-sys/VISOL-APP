package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.UserRepository
import com.example.model.UserStory
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * FULL-SCREEN INSTAGRAM / TELEGRAM STYLE STORY VIEWER
 * - Multi-story grouped support with segmented progress indicators (e.g. 1/3, 2/3, 3/3)
 * - Auto-advances after 5 seconds per story
 * - Tap Left: Previous story, Tap Right: Next story, Hold: Pause
 * - Interactive Link Sticker button
 * - Direct reply input field (sends direct message to author)
 */
@Composable
fun StoryViewerDialog(
    stories: List<UserStory>,
    initialIndex: Int = 0,
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onAddAnotherStory: (() -> Unit)? = null
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentStoryIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.size - 1)) }
    val currentStory = stories[currentStoryIndex]

    var replyText by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }

    // Segmented Progress Animation
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(currentStoryIndex, isPaused) {
        if (!isPaused) {
            progressAnim.snapTo(0f)
            val result = progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            if (result.endReason == androidx.compose.animation.core.AnimationEndReason.Finished) {
                if (currentStoryIndex < stories.size - 1) {
                    currentStoryIndex++
                } else {
                    onDismiss()
                }
            }
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
                .pointerInput(currentStoryIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Tap Left -> Previous
                                if (currentStoryIndex > 0) {
                                    currentStoryIndex--
                                }
                            } else {
                                // Tap Right -> Next
                                if (currentStoryIndex < stories.size - 1) {
                                    currentStoryIndex++
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // 1. Story Media Background (Photo or Video Playback)
            if (currentStory.mediaUri.isNotBlank()) {
                val isVideoStory = currentStory.mediaType.equals("VIDEO", ignoreCase = true) ||
                        currentStory.mediaUri.contains("video", ignoreCase = true) ||
                        currentStory.mediaUri.endsWith(".mp4", ignoreCase = true)
                if (isVideoStory) {
                    ReelVideoPlayerView(
                        videoUriString = currentStory.mediaUri,
                        isPlaying = !isPaused,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentStory.mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Story Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF38123D), Color(0xFF13091B), Color(0xFF1E102A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatar(
                        photoPath = currentStory.authorPhotoPath,
                        gender = currentStory.authorGender,
                        size = 130.dp,
                        showCameraBadge = false
                    )
                }
            }

            // Top Vignette Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
            )

            // Bottom Vignette Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // 2. Top Segmented Progress Bars & Author Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .statusBarsPadding()
            ) {
                // Segmented Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentStoryIndex -> 1f
                            index == currentStoryIndex -> progressAnim.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Author Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            photoPath = currentStory.authorPhotoPath,
                            gender = currentStory.authorGender,
                            size = 38.dp,
                            showCameraBadge = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentStory.authorName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• ${currentStory.remainingHours()}s qoldi",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            if (!currentStory.musicTitle.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = SoftPeach,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${currentStory.musicTitle} - ${currentStory.musicArtist ?: "Musiqa"}",
                                        color = SoftPeach,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onAddAnotherStory != null) {
                            IconButton(
                                onClick = onAddAnotherStory,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Hikoya qo'shish",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Yopish",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // 3. Middle Content (Caption & Interactive Link Sticker)
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Link Sticker
                if (!currentStory.linkUrl.isNullOrBlank()) {
                    Surface(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentStory.linkUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Havola ochilmadi", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, SoftPeach),
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = SoftPeach,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentStory.linkText ?: "Havolaga o'tish 🔗",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Story Caption
                if (currentStory.caption.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = currentStory.caption,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // 4. Bottom Reply Input Row
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = {
                        Text("Xabar yuborish...", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPeach,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            userRepository.sendChatMessage(
                                candidateId = currentStory.authorId,
                                text = "💬 [Storyga javob]: $replyText"
                            )
                            Toast.makeText(context, "Javob yuborildi! ✉️", Toast.LENGTH_SHORT).show()
                            replyText = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SoftPeach, SoftRosePink)))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Yuborish",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
