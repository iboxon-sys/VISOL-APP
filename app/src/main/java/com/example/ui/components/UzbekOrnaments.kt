package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.*
import java.io.File

/**
 * Rasmiy Telegram ilovasi ikonkasining aniq vektori
 */
@Composable
fun TelegramBrandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2AABEE)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.58f)) {
            val w = this.size.width
            val h = this.size.height

            val planePath = Path().apply {
                moveTo(w * 0.15f, h * 0.52f)
                lineTo(w * 0.90f, h * 0.15f)
                lineTo(w * 0.72f, h * 0.88f)
                lineTo(w * 0.48f, h * 0.65f)
                lineTo(w * 0.38f, h * 0.78f)
                lineTo(w * 0.36f, h * 0.58f)
                lineTo(w * 0.75f, h * 0.30f)
                lineTo(w * 0.32f, h * 0.54f)
                close()
            }
            drawPath(planePath, color = Color.White)
        }
    }
}

/**
 * Rasmiy Instagram ilovasi ikonkasining gradientli vektori
 */
@Composable
fun InstagramBrandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val igBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF833AB4), // Purple
            Color(0xFFFD1D1D), // Red
            Color(0xFFFCB045)  // Orange/Yellow
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(igBrush),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = w * 0.12f

            // Outer camera rounded rect
            drawRoundRect(
                color = Color.White,
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.28f, h * 0.28f),
                style = Stroke(width = strokeW)
            )

            // Center camera lens circle
            drawCircle(
                color = Color.White,
                radius = w * 0.25f,
                center = Offset(w / 2, h / 2),
                style = Stroke(width = strokeW)
            )

            // Small flash dot
            drawCircle(
                color = Color.White,
                radius = w * 0.06f,
                center = Offset(w * 0.78f, h * 0.22f)
            )
        }
    }
}

/**
 * Rasmiy TikTok ilovasi ikonkasining vektori
 */
@Composable
fun TikTokBrandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val w = this.size.width
            val h = this.size.height

            // Cyan shadow of musical note
            val notePathCyan = Path().apply {
                moveTo(w * 0.45f, h * 0.20f)
                lineTo(w * 0.58f, h * 0.20f)
                cubicTo(w * 0.58f, h * 0.35f, w * 0.70f, h * 0.45f, w * 0.85f, h * 0.45f)
                lineTo(w * 0.85f, h * 0.60f)
                cubicTo(w * 0.72f, h * 0.60f, w * 0.60f, h * 0.52f, w * 0.58f, h * 0.48f)
                lineTo(w * 0.58f, h * 0.72f)
                cubicTo(w * 0.58f, h * 0.85f, w * 0.46f, h * 0.95f, w * 0.32f, h * 0.95f)
                cubicTo(w * 0.18f, h * 0.95f, w * 0.08f, h * 0.85f, w * 0.08f, h * 0.72f)
                cubicTo(w * 0.08f, h * 0.58f, w * 0.20f, h * 0.48f, w * 0.35f, h * 0.48f)
                lineTo(w * 0.35f, h * 0.62f)
                cubicTo(w * 0.28f, h * 0.62f, w * 0.22f, h * 0.66f, w * 0.22f, h * 0.72f)
                cubicTo(w * 0.22f, h * 0.78f, w * 0.26f, h * 0.82f, w * 0.32f, h * 0.82f)
                cubicTo(w * 0.40f, h * 0.82f, w * 0.45f, h * 0.75f, w * 0.45f, h * 0.68f)
                close()
            }
            drawPath(notePathCyan, color = Color(0xFF25F4EE).copy(alpha = 0.85f))

            // White main note
            val notePathWhite = Path().apply {
                moveTo(w * 0.50f, h * 0.15f)
                lineTo(w * 0.63f, h * 0.15f)
                cubicTo(w * 0.63f, h * 0.30f, w * 0.75f, h * 0.40f, w * 0.90f, h * 0.40f)
                lineTo(w * 0.90f, h * 0.55f)
                cubicTo(w * 0.77f, h * 0.55f, w * 0.65f, h * 0.47f, w * 0.63f, h * 0.43f)
                lineTo(w * 0.63f, h * 0.68f)
                cubicTo(w * 0.63f, h * 0.82f, w * 0.50f, h * 0.92f, w * 0.36f, h * 0.92f)
                cubicTo(w * 0.22f, h * 0.92f, w * 0.12f, h * 0.82f, w * 0.12f, h * 0.68f)
                cubicTo(w * 0.12f, h * 0.55f, w * 0.24f, h * 0.45f, w * 0.38f, h * 0.45f)
                lineTo(w * 0.38f, h * 0.58f)
                cubicTo(w * 0.32f, h * 0.58f, w * 0.26f, h * 0.62f, w * 0.26f, h * 0.68f)
                cubicTo(w * 0.26f, h * 0.74f, w * 0.30f, h * 0.78f, w * 0.36f, h * 0.78f)
                cubicTo(w * 0.44f, h * 0.78f, w * 0.50f, h * 0.72f, w * 0.50f, h * 0.64f)
                close()
            }
            drawPath(notePathWhite, color = Color.White)
        }
    }
}

/**
 * iOS uslubidagi Face ID biometrik skanerlovchi ramka
 */
@Composable
fun FaceIdScanningOverlay(
    isScanning: Boolean,
    isVerified: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "face_id_anim")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_scan"
    )

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cornerLen = w * 0.16f
            val cornerRadius = 14f
            val strokeWidth = 5f
            val bracketColor = if (isVerified) Color(0xFF10B981) else Color(0xFF0284C7)

            // 4 burchakdagi Face ID qavslari (Top-Left, Top-Right, Bottom-Left, Bottom-Right)
            // Top-Left
            drawLine(bracketColor, Offset(16f, 16f + cornerLen), Offset(16f, 16f), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, Offset(16f, 16f), Offset(16f + cornerLen, 16f), strokeWidth, StrokeCap.Round)

            // Top-Right
            drawLine(bracketColor, Offset(w - 16f - cornerLen, 16f), Offset(w - 16f, 16f), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, Offset(w - 16f, 16f), Offset(w - 16f, 16f + cornerLen), strokeWidth, StrokeCap.Round)

            // Bottom-Left
            drawLine(bracketColor, Offset(16f, h - 16f - cornerLen), Offset(16f, h - 16f), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, Offset(16f, h - 16f), Offset(16f + cornerLen, h - 16f), strokeWidth, StrokeCap.Round)

            // Bottom-Right
            drawLine(bracketColor, Offset(w - 16f - cornerLen, h - 16f), Offset(w - 16f, h - 16f), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, Offset(w - 16f, h - 16f - cornerLen), Offset(w - 16f, h - 16f), strokeWidth, StrokeCap.Round)

            // Face ID laser nur chizig'i
            if (isScanning && !isVerified) {
                val currentY = h * laserY
                drawLine(
                    color = Color(0xFF00FFCC),
                    start = Offset(24f, currentY),
                    end = Offset(w - 24f, currentY),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun UzbekOrnamentDivider(
    modifier: Modifier = Modifier,
    color: Color = GoldAccent
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val midY = size.height / 2
        val midX = size.width / 2

        drawLine(
            color = color.copy(alpha = 0.35f),
            start = Offset(20f, midY),
            end = Offset(midX - 35f, midY),
            strokeWidth = 2f
        )
        drawLine(
            color = color.copy(alpha = 0.35f),
            start = Offset(midX + 35f, midY),
            end = Offset(size.width - 20f, midY),
            strokeWidth = 2f
        )

        val diamondPath = Path().apply {
            moveTo(midX, midY - 7f)
            lineTo(midX + 10f, midY)
            lineTo(midX, midY + 7f)
            lineTo(midX - 10f, midY)
            close()
        }
        drawPath(diamondPath, color = color)
        drawCircle(color = color, radius = 3f, center = Offset(midX - 18f, midY))
        drawCircle(color = color, radius = 3f, center = Offset(midX + 18f, midY))
    }
}

@Composable
fun UzbekHeaderCard(
    modifier: Modifier = Modifier,
    title: String = "Visol",
    subtitle: String = "Milliy Sevgi va Do'stlik Rishtalari"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(EmeraldDark, EmeraldPrimary, Color(0xFF0F5132))
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(GoldLight, GoldAccent))
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                UzbekOrnamentDivider(
                    modifier = Modifier.width(160.dp),
                    color = GoldLight
                )
            }
        }
    }
}

@Composable
fun SecurityScreenshotBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFEF2F2),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PomegranateRed,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Xavfsizlik & Maxfiylik Kafolati",
                    color = PomegranateRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Skrinshot olish va rasmlarni saqlash taqiqlangan (FLAG_SECURE faol).",
                    color = Color(0xFF7F1D1D),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun ProfileAvatar(
    photoPath: String,
    gender: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showCameraBadge: Boolean = true
) {
    val bitmap = remember(photoPath) {
        if (photoPath.isNotEmpty() && !photoPath.startsWith("avatar_")) {
            val file = File(photoPath)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    Box(
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.5.dp, GoldAccent, CircleShape)
                .background(
                    if (gender == "Ayol") Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (photoPath == "ic_deleted_user") {
                Image(
                    painter = painterResource(id = R.drawable.ic_deleted_user),
                    contentDescription = "O'chirilgan foydalanuvchi",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Profil surati",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = if (gender == "Ayol") "\uD83E\uDDD5" else "\uD83E\uDDD4",
                    fontSize = (size.value * 0.48f).sp
                )
            }
        }

        if (showCameraBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.32f)
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Face ID jonli rasm",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.18f)
                )
            }
        }
    }
}
