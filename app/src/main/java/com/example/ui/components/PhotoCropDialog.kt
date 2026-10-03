package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SoftPeach
import com.example.ui.theme.SoftRosePink
import kotlin.math.max
import kotlin.math.min

/**
 * Modern iOS / Telegram style In-App Photo Crop & Adjustment Dialog.
 * Allows panning, pinch-to-zoom (scaling), 90-degree rotation, and center cropping.
 */
@Composable
fun PhotoCropDialog(
    sourceBitmap: Bitmap,
    isDark: Boolean = true,
    onDismiss: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val cropBoxSizeDp = 280.dp
    val cropBoxSizePx = with(density) { cropBoxSizeDp.toPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Bekor qilish",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Suratni Kesish ✂️",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Siljiting yoki yaqinlashtiring",
                            color = Color(0xFFC4A8B9),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            rotationAngle = (rotationAngle + 90f) % 360f
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Aylantirish",
                            tint = Color.White
                        )
                    }
                }

                // Center Crop Workspace with Circular Profile Guide Mask
                Box(
                    modifier = Modifier
                        .size(cropBoxSizeDp)
                        .clip(CircleShape)
                        .background(Color(0xFF181818))
                        .border(3.dp, Brush.linearGradient(listOf(SoftPeach, SoftRosePink)), CircleShape)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.7f, 4.5f)
                                val maxPan = (cropBoxSizePx * (scale - 0.7f)).coerceAtLeast(0f) / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-maxPan - 100f, maxPan + 100f)
                                offsetY = (offsetY + pan.y).coerceIn(-maxPan - 100f, maxPan + 100f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = sourceBitmap.asImageBitmap(),
                        contentDescription = "Tahrirlanayotgan surat",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                                rotationZ = rotationAngle
                            }
                    )

                    // Rule of Thirds Grid Overlay
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val gridColor = Color.White.copy(alpha = 0.25f)
                        val strokeW = 1.dp.toPx()

                        // Vertical lines
                        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = strokeW)
                        drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = strokeW)

                        // Horizontal lines
                        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = strokeW)
                        drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = strokeW)
                    }
                }

                // Controls & Confirm Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Ikki barmoq bilan yaqinlashtirish ko'rsatkichi (Lupa slayderi o'rniga zamonaviy jest indikatori)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x33FFFFFF),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "👌 Ikki barmoq bilan kattalashtiring yoki suring",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                color = SoftPeach,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Reset button
                    TextButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            rotationAngle = 0f
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SoftPeach, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dastlabki holatga qaytarish", color = SoftPeach, fontSize = 12.sp)
                    }

                    // Confirm Action Button
                    Button(
                        onClick = {
                            val cropped = cropBitmap(
                                source = sourceBitmap,
                                scale = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                                rotation = rotationAngle,
                                viewSizePx = cropBoxSizePx
                            )
                            onCropConfirmed(cropped)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftPeach),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(12.dp, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kesish va Suratni Saqlash ✓",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Perform high-precision bitmap cropping based on the visual transformation.
 */
private fun cropBitmap(
    source: Bitmap,
    scale: Float,
    translationX: Float,
    translationY: Float,
    rotation: Float,
    viewSizePx: Float
): Bitmap {
    val targetSize = 600 // Output square bitmap size
    val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val matrix = Matrix()
    
    // Fit/Crop source into viewSizePx matching ContentScale.Crop
    val baseScale = maxOf(viewSizePx / source.width.toFloat(), viewSizePx / source.height.toFloat())

    val pxRatio = targetSize.toFloat() / viewSizePx

    // Center source
    val initTx = (viewSizePx - source.width * baseScale) / 2f
    val initTy = (viewSizePx - source.height * baseScale) / 2f

    matrix.postScale(baseScale, baseScale)
    matrix.postTranslate(initTx, initTy)

    // User transformations around center of view
    val centerX = viewSizePx / 2f
    val centerY = viewSizePx / 2f
    matrix.postScale(scale, scale, centerX, centerY)
    matrix.postRotate(rotation, centerX, centerY)
    matrix.postTranslate(translationX, translationY)

    // Scale up to output resolution
    matrix.postScale(pxRatio, pxRatio)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)

    return output
}
