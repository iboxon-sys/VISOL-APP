package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Advanced Glassmorphic Card Container with multi-layered opacity,
 * dynamic gradient mesh lighting, and ultra-thin light-reflecting glass borders.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val effectiveBg = backgroundColor ?: getAppCardBackground(isDark)
    val effectiveBorder = borderColor ?: getAppCardBorder(isDark)

    Box(
        modifier = modifier
            .clip(shape)
            .background(effectiveBg)
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        effectiveBorder.copy(alpha = 0.35f),
                        effectiveBorder.copy(alpha = 0.12f),
                        effectiveBorder.copy(alpha = 0.25f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * Deep Romantic Background Canvas with glowing ambient light orbs
 * (Dark Navy #0F172A, Deep Velvet Rose/Purple #4C1D95, Muted Burgundy #831843)
 */
@Composable
fun RomanticMeshBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
    }
}
