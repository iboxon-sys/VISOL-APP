package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.ui.theme.*
import com.example.util.ImageModerationService
import kotlinx.coroutines.launch

/**
 * INSTAGRAM / TELEGRAM STYLE STORY CREATOR MODAL
 * - Pick photo from gallery or enter image/video link
 * - Add caption
 * - Add interactive link sticker (URL + Label)
 * - Save directly to 24h active user stories
 */
@Composable
fun StoryCreatorDialog(
    userRepository: UserRepository,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onStoryCreated: () -> Unit
) {
    val context = LocalContext.current
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaUrlInput by remember { mutableStateOf("") }
    var captionText by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var linkText by remember { mutableStateOf("") }
    var showLinkField by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                coroutineScope.launch {
                    val moderation = ImageModerationService.checkUriSafety(context, uri)
                    if (!moderation.isApproved) {
                        val msg = moderation.warningMessage ?: "🚫 Xavfsizlik filtri: Suratda noo'rin yoki ochiq-sochiq kontent aniqlandi!"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    } else {
                        selectedMediaUri = uri
                        mediaUrlInput = uri.toString()
                        Toast.makeText(context, "Surat xavfsizlik tekshiruvidan o'tdi ✨", Toast.LENGTH_SHORT).show()
                    }
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
                .background(if (isDark) Color(0xFF13091B) else Color(0xFFFAF7F8))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SoftPeach, SoftRosePink))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Yangi Hikoya (24 soat)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isDark) Color.White else TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = if (isDark) Color.White else TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Media Preview / Picker Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0x1F2A183A) else Color(0xFFEDE9FE))
                        .border(1.dp, SoftGlassBorderDark, RoundedCornerShape(24.dp))
                        .clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                                type = "*/*"
                                putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                            }
                            mediaPickerLauncher.launch(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedMediaUri != null || mediaUrlInput.isNotBlank()) {
                        AsyncImage(
                            model = selectedMediaUri ?: mediaUrlInput,
                            contentDescription = "Hikoya surati",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SoftPeach.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = SoftPeach,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Surat yoki video tanlang",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else TextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Galereyadan tanlash uchun bosing",
                                color = if (isDark) Color(0xFFC4A8B9) else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Floating Interactive Link sticker preview
                    if (linkUrl.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, SoftPeach),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = SoftPeach, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = linkText.ifBlank { "Havola 🔗" },
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Caption Input
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("Hikoyangiz uchun tavsif...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPeach,
                        unfocusedBorderColor = SoftGlassBorderDark,
                        focusedContainerColor = if (isDark) Color(0x1F2A183A) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0x1F2A183A) else Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Link Sticker toggle
                if (!showLinkField) {
                    TextButton(
                        onClick = { showLinkField = true },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null, tint = SoftPeach, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Interaktiv havola stikeri qo'shish (URL)", color = SoftPeach, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = linkUrl,
                            onValueChange = { linkUrl = it },
                            placeholder = { Text("URL (masalan https://...)", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = linkText,
                            onValueChange = { linkText = it },
                            placeholder = { Text("Yozuv", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        val effectiveMedia = if (selectedMediaUri != null) selectedMediaUri.toString()
                        else if (mediaUrlInput.isNotBlank()) mediaUrlInput
                        else "avatar_female_1"

                        val isVideo = if (selectedMediaUri != null) {
                            try {
                                context.contentResolver.getType(selectedMediaUri!!)?.startsWith("video/") == true
                            } catch (_: Exception) {
                                false
                            }
                        } else {
                            mediaUrlInput.endsWith(".mp4", ignoreCase = true) || mediaUrlInput.contains("video", ignoreCase = true)
                        }

                        userRepository.addUserStory(
                            mediaUri = effectiveMedia,
                            mediaType = if (isVideo) "VIDEO" else "PHOTO",
                            caption = captionText,
                            linkUrl = linkUrl.takeIf { it.isNotBlank() },
                            linkText = linkText.takeIf { it.isNotBlank() } ?: (if (linkUrl.isNotBlank()) "Havolaga o'tish 🔗" else null)
                        )
                        Toast.makeText(context, "Hikoyangiz muvaffaqiyatli joylandi! ✨", Toast.LENGTH_SHORT).show()
                        onStoryCreated()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPeach),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hikoyani Ulashish (24 soat)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
