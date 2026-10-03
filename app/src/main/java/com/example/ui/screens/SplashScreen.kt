package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 1. CUSTOM SPLASH / LOADING SCREEN:
 * - Sleek, dark-themed loading screen
 * - Centered Animated/Polished Text: "ZUHRIDDIN SYS PRODUCT TAQDIM ETADI"
 * - Smooth luxury progress ring & glowing heart animation
 * - Smooth transition to the main screen after 2.5 seconds.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // 2.5 second auto timer
    LaunchedEffect(Unit) {
        delay(2500L)
        onSplashFinished()
    }

    // Shimmer and pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090D16),
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF090D16)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background light orb (modernized)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WarmPeach.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(w / 2f, h * 0.44f),
                    radius = w * 0.8f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Updated Elegant Logo/Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                WarmPeach.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating ring (thinner, more elegant)
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                WarmPeach,
                                WarmGold,
                                WarmPeachLight,
                                Color.Transparent
                            )
                        ),
                        startAngle = ringRotation,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner Brand Icon
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = WarmPeach,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Updated Elegant Title
            Text(
                text = "VISOL",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Updated Presentation Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha)
            ) {
                Text(
                    text = "ZUHRIDDIN SYS PRODUCT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .width(40.dp)
                        .background(WarmPeach)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TAQDIM ETADI",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = WarmPeach
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Improved Loading Indicator
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = WarmPeach,
                strokeWidth = 2.dp,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            )
        }

        // Bottom security guarantee
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "🔒 Biometrik Face ID & Maxfiy Himoyalangan",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
        }
    }
}
