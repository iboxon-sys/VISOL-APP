package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SoftPeach
import com.example.ui.theme.SoftRosePink
import com.example.ui.theme.WarmHoneyGold
import com.example.ui.theme.WarmPeachSoft
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class NavigationTabItem(
    val index: Int,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val badgeCount: Int = 0
)

/**
 * iOS 18 Glassmorphism Floating Draggable Tab Bar.
 *
 * Supports:
 * - Direct click navigation on any tab.
 * - Continuous horizontal slide/drag gesture: user can touch down on any tab and slide their finger
 *   across all tabs with real-time responsive physical indicator tracking.
 * - On finger release, automatically snaps to the nearest tab and activates that screen.
 */
@Composable
fun IOSGlassScrollableTabBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    isDark: Boolean,
    unreadCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val tabs = remember(unreadCount) {
        listOf(
            NavigationTabItem(
                index = 0,
                title = "Tanishuv",
                selectedIcon = Icons.Default.Favorite,
                unselectedIcon = Icons.Outlined.FavoriteBorder,
                testTag = "tab_candidates_button"
            ),
            NavigationTabItem(
                index = 1,
                title = "Reels",
                selectedIcon = Icons.Default.VideoLibrary,
                unselectedIcon = Icons.Outlined.VideoLibrary,
                testTag = "tab_reels_button"
            ),
            NavigationTabItem(
                index = 2,
                title = "Voice & Efir",
                selectedIcon = Icons.Default.Mic,
                unselectedIcon = Icons.Outlined.Mic,
                testTag = "tab_voice_stream_button"
            ),
            NavigationTabItem(
                index = 3,
                title = "Do'kon",
                selectedIcon = Icons.Default.ShoppingBag,
                unselectedIcon = Icons.Outlined.ShoppingBag,
                testTag = "tab_coin_store_button"
            ),
            NavigationTabItem(
                index = 4,
                title = "Suhbat",
                selectedIcon = Icons.Default.ChatBubble,
                unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                testTag = "tab_chat_button",
                badgeCount = unreadCount
            ),
            NavigationTabItem(
                index = 5,
                title = "Profilim",
                selectedIcon = Icons.Default.Person,
                unselectedIcon = Icons.Outlined.Person,
                testTag = "tab_my_profile_button"
            )
        )
    }

    var totalBarWidthPx by remember { mutableFloatStateOf(0f) }
    val tabCount = tabs.size
    val tabWidthPx = if (tabCount > 0 && totalBarWidthPx > 0f) totalBarWidthPx / tabCount else 0f

    // Animated indicator position offset
    val indicatorOffsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Sync indicator with selectedIndex when not dragging
    LaunchedEffect(selectedIndex, tabWidthPx) {
        if (!isDragging && tabWidthPx > 0f) {
            val targetOffset = selectedIndex * tabWidthPx
            indicatorOffsetX.animateTo(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = if (isDark) Color(0xD8150B27) else Color(0xEBFFFFFF),
        shadowElevation = 24.dp,
        border = BorderStroke(
            1.2.dp,
            Brush.verticalGradient(
                listOf(
                    if (isDark) Color(0x80FFFFFF) else Color(0xC0FFFFFF),
                    if (isDark) Color(0x30FFFFFF) else Color(0x60FFFFFF),
                    if (isDark) Color(0x50E06D85) else Color(0x60FCE2D6)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .navigationBarsPadding()
            .onGloballyPositioned { coords ->
                totalBarWidthPx = coords.size.width.toFloat()
            }
            .pointerInput(totalBarWidthPx) {
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        isDragging = true
                        if (tabWidthPx > 0f) {
                            val startTab = (startOffset.x / tabWidthPx).toInt().coerceIn(0, tabCount - 1)
                            coroutineScope.launch {
                                indicatorOffsetX.snapTo(startOffset.x - (tabWidthPx / 2f))
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        if (tabWidthPx > 0f) {
                            val currentPos = indicatorOffsetX.value + (tabWidthPx / 2f)
                            val nearestTab = (currentPos / tabWidthPx).roundToInt().coerceIn(0, tabCount - 1)
                            onTabSelected(nearestTab)
                            coroutineScope.launch {
                                indicatorOffsetX.animateTo(
                                    targetValue = nearestTab * tabWidthPx,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        coroutineScope.launch {
                            indicatorOffsetX.animateTo(
                                targetValue = selectedIndex * tabWidthPx,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (indicatorOffsetX.value + dragAmount).coerceIn(
                            -10f,
                            (totalBarWidthPx - tabWidthPx + 10f).coerceAtLeast(0f)
                        )
                        coroutineScope.launch {
                            indicatorOffsetX.snapTo(newOffset)
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 1. Sliding iOS Glass Capsule Active Indicator
            if (tabWidthPx > 0f) {
                val indicatorWidthDp = with(density) { (tabWidthPx - 8f).toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffsetX.value.roundToInt() + 4, 0) }
                        .width(indicatorWidthDp)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                if (isDark) listOf(
                                    Color(0x708A2BE2),
                                    Color(0x60E06D85),
                                    Color(0x403A1252)
                                )
                                else listOf(
                                    Color(0x80FFFFFF),
                                    Color(0x60FCE2D6),
                                    Color(0x70FFB6C1)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    if (isDark) Color(0x90FFFFFF) else Color(0xD0FFFFFF),
                                    SoftRosePink.copy(alpha = 0.6f),
                                    WarmHoneyGold.copy(alpha = 0.4f)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                )
            }

            // 2. Interactive Navigation Items Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedIndex == tab.index
                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 0.95f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tab_scale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(tab.index) {
                                detectTapGestures(
                                    onTap = {
                                        onTabSelected(tab.index)
                                    }
                                )
                            }
                            .scale(scaleAnim)
                            .testTag(tab.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (tab.badgeCount > 0) {
                                    Badge(
                                        containerColor = SoftRosePink,
                                        modifier = Modifier.offset(x = 4.dp, y = (-2).dp)
                                    ) {
                                        Text(
                                            text = "${tab.badgeCount}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) {
                                    if (tab.index == 1) SoftRosePink else SoftPeach
                                } else {
                                    if (isDark) Color(0xFFC4A8B9) else Color(0xFF8A717E)
                                },
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (tab.index == 1) SoftRosePink else SoftPeach
                            } else {
                                if (isDark) Color(0xFFC4A8B9) else Color(0xFF8A717E)
                            }
                        )
                    }
                }
            }
        }
    }
}
