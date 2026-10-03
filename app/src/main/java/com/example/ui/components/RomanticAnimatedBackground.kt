package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlin.math.sin

private data class FallingPetal(
    val xRatio: Float,
    val yRatio: Float,
    val speed: Float,
    val size: Float,
    val swayAmount: Float,
    val swaySpeed: Float,
    val color: Color
)

/**
    * HIGHLY OPTIMIZED 60 FPS ROMANTIC ANIMATED BACKGROUND
    * - Lightweight particle count (8 petals, 4 glow dots)
    * - Zero matrix rotations or heavy allocations for buttery smooth performance
    */
@Composable
fun RomanticAnimatedBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "romantic_bg")

    val gentleScale by infiniteTransition.animateFloat(
        initialValue = 1.01f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentle_zoom"
    )

    val timeState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_flow"
    )

    val petals = remember {
        val petalColors = listOf(
            Color(0xFFFFB7C5),
            Color(0xFFFF9EAA),
            Color(0xFFFFC0CB)
        )
        List(8) { index ->
            FallingPetal(
                xRatio = (index * 0.125f) % 1f,
                yRatio = (index * 0.14f) % 1f,
                speed = 0.0008f + (index % 3) * 0.0004f,
                size = 12f + (index % 3) * 4f,
                swayAmount = 15f + (index % 2) * 10f,
                swaySpeed = 0.05f + (index % 2) * 0.02f,
                color = petalColors[index % petalColors.size]
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.romantic_swing_bg),
            contentDescription = "Romantik bahoriy fon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(gentleScale)
        )

        val overlayBrush = if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x35100719),
                    Color(0x10180B28),
                    Color(0x40100719)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x1AFFFFFF),
                    Color(0x0AFFF5F7),
                    Color(0x25FFFFFF)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 4 glowing sky particles
            for (i in 0 until 4) {
                val px = (w * ((i * 0.25f + timeState * 0.003f) % 1f))
                val py = (h * ((i * 0.2f + sin(timeState * 0.02f + i) * 0.05f) % 1f))
                val pAlpha = (0.2f + 0.3f * sin(timeState * 0.06f + i)).coerceIn(0.1f, 0.6f)

                drawCircle(
                    color = Color.White.copy(alpha = pAlpha),
                    center = Offset(px, py),
                    radius = 4f
                )
            }

            // 8 falling petals (Zero allocation, no rotation matrix for maximum speed)
            petals.forEachIndexed { i, petal ->
                val currentYRatio = (petal.yRatio + timeState * petal.speed) % 1.1f
                val swayX = sin(timeState * petal.swaySpeed + i) * petal.swayAmount
                val currentX = (petal.xRatio * w + swayX) % w
                val currentY = currentYRatio * h

                val petalAlpha = when {
                    currentY < h * 0.1f -> currentY / (h * 0.1f)
                    currentY > h * 0.9f -> (h - currentY) / (h * 0.1f)
                    else -> 0.75f
                }.coerceIn(0f, 0.75f)

                drawOval(
                    color = petal.color.copy(alpha = petalAlpha),
                    topLeft = Offset(currentX - petal.size * 0.4f, currentY - petal.size * 0.8f),
                    size = Size(petal.size * 0.8f, petal.size * 1.5f)
                )
            }
        }
    }
}
