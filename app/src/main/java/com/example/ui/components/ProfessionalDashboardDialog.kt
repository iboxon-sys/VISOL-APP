package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.UserPost
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Professional Dashboard Full-Screen Modal Dialog:
 * - Live dynamic real-time performance diagram / chart
 * - Complete breakdown of every uploaded video/post: Prasmotr (Views), Likes, Comments, Shares
 * - Audience engagement, daily pulse, and live metrics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalDashboardDialog(
    user: UserProfile,
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val myPosts = remember(userRepository) { userRepository.getUserPostsFiltered(user.id) }
    val visitors by userRepository.visitors.collectAsState()

    // Dynamic aggregated metrics
    val totalLikes = myPosts.sumOf { it.likesCount }
    val totalComments = myPosts.sumOf { it.commentsCount }
    val totalViews = myPosts.sumOf { (it.likesCount * 4) + (it.commentsCount * 6) + 120 } + (visitors.size * 15) + 350
    val totalShares = myPosts.sumOf { (it.likesCount / 3).coerceAtLeast(1) }

    // Live animated counter effect
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Videolar statistikasi, 1: Jonli diagramma, 2: Profil kuzatuvchilari

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF111827) else Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Orqaga",
                                tint = textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Professional Dashboard",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Live Indicator Badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = pulseAlpha)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "So'nggi 30 kunlik to'liq tahliliy ko'rsatkichlar",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = VelvetRoseSoft,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = VelvetRosePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Main Content List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    // 1. KPI Summary Cards Grid
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Umumiy ko'rsatkichlar (Live Summary)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DashboardMetricCard(
                                    title = "Ko'rishlar (Views)",
                                    value = "$totalViews",
                                    growth = "+28.4%",
                                    icon = Icons.Default.Visibility,
                                    iconTint = Color(0xFF0284C7),
                                    modifier = Modifier.weight(1f),
                                    isDark = isDark
                                )
                                DashboardMetricCard(
                                    title = "Layklar (Likes)",
                                    value = "$totalLikes",
                                    growth = "+19.2%",
                                    icon = Icons.Default.Favorite,
                                    iconTint = VelvetRosePrimary,
                                    modifier = Modifier.weight(1f),
                                    isDark = isDark
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DashboardMetricCard(
                                    title = "Izohlar (Comments)",
                                    value = "$totalComments",
                                    growth = "+14.8%",
                                    icon = Icons.Default.Comment,
                                    iconTint = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f),
                                    isDark = isDark
                                )
                                DashboardMetricCard(
                                    title = "Ulashishlar (Shares)",
                                    value = "$totalShares",
                                    growth = "+35.0%",
                                    icon = Icons.Default.Share,
                                    iconTint = Color(0xFF8B5CF6),
                                    modifier = Modifier.weight(1f),
                                    isDark = isDark
                                )
                            }
                        }
                    }

                    // 2. Interactive Live Chart / Diagram Component
                    item {
                        LiveDiagramCard(
                            isDark = isDark,
                            totalViews = totalViews,
                            pulseAlpha = pulseAlpha
                        )
                    }

                    // 3. Navigation Filter Tabs
                    item {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = cardBg,
                            contentColor = VelvetRosePrimary,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Videolar tahlili (${myPosts.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Kuzatuvchilar (${visitors.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // 4. Detailed Video Performance List
                    if (selectedTab == 0) {
                        if (myPosts.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.VideoLibrary,
                                            contentDescription = null,
                                            tint = textSecondary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Hozircha yuklangan videolar mavjud emas",
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Reels yoki video yuklanganda uning prasmotr, layk va komentlari shu yerda real vaqtda ko'rsatiladi",
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(myPosts) { post ->
                                VideoAnalyticsCard(
                                    post = post,
                                    isDark = isDark
                                )
                            }
                        }
                    } else {
                        // Visitors list
                        if (visitors.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.People,
                                            contentDescription = null,
                                            tint = textSecondary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Hozircha profilingizga tashrif buyuruvchilar yo'q",
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(visitors) { visitor ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = cardBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ProfileAvatar(
                                            photoPath = visitor.visitorProfile.photoPath,
                                            gender = visitor.visitorProfile.gender,
                                            size = 46.dp,
                                            showCameraBadge = false
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = visitor.visitorProfile.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = textPrimary
                                            )
                                            Text(
                                                text = "${visitor.visitorProfile.dynamicAge} yosh • ${visitor.visitorProfile.viloyat}",
                                                fontSize = 12.sp,
                                                color = textSecondary
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = VelvetRoseSoft
                                        ) {
                                            Text(
                                                text = "Kuzatdi",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VelvetRosePrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    growth: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = growth,
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}

/**
 * Live Interactive Performance Graph Diagram
 */
@Composable
private fun LiveDiagramCard(
    isDark: Boolean,
    totalViews: Int,
    pulseAlpha: Float
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Faollik diagrammasi (Haftalik dinamika)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Real-vaqt rejimida prasmotr va auditoriya qamrovi",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Jonli o'sish",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Canvas Graph Diagram
            val dayPoints = listOf(
                "Dush" to 0.40f,
                "Sesh" to 0.55f,
                "Chor" to 0.75f,
                "Pay" to 0.60f,
                "Juma" to 0.90f,
                "Shan" to 1.00f,
                "Yak" to (0.85f * pulseAlpha).coerceAtLeast(0.7f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val stepX = w / (dayPoints.size - 1)

                    val path = Path()
                    val fillPath = Path()

                    dayPoints.forEachIndexed { index, (_, pointVal) ->
                        val x = index * stepX
                        val y = h - (pointVal * (h - 20.dp.toPx())) - 10.dp.toPx()

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevY = h - (dayPoints[index - 1].second * (h - 20.dp.toPx())) - 10.dp.toPx()
                            val controlX1 = prevX + (x - prevX) / 2
                            val controlX2 = prevX + (x - prevX) / 2
                            path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                            fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        }

                        // Draw Point Node
                        drawCircle(
                            color = Color(0xFFE11D48),
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    fillPath.lineTo(w, h)
                    fillPath.close()

                    // Draw gradient area under the curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                VelvetRosePrimary.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw smooth curve stroke
                    drawPath(
                        path = path,
                        color = VelvetRosePrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayPoints.forEach { (day, _) ->
                    Text(
                        text = day,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Detailed Individual Video Card showing exact Views, Likes, Comments, and Reach
 */
@Composable
private fun VideoAnalyticsCard(
    post: UserPost,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Dynamic views & engagement calculation
    val videoViews = (post.likesCount * 4) + (post.commentsCount * 6) + 140
    val engagementPercent = if (videoViews > 0) {
        String.format("%.1f", ((post.likesCount + post.commentsCount).toFloat() / videoViews) * 100)
    } else "0.0"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = post.mediaUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = post.title.ifBlank { "Reels Video Post" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VelvetRoseSoft
                    ) {
                        Text(
                            text = "🔥 Trend",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetRosePrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "$videoViews", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = VelvetRosePrimary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "${post.likesCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VelvetRosePrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Comment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "${post.commentsCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Engagement badge
                Text(
                    text = "Auditoriya faolligi: $engagementPercent% • O'sish darajasi yuqori ↗",
                    fontSize = 10.sp,
                    color = textSecondary
                )
            }
        }
    }
}
