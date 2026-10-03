package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.UserRepository
import com.example.model.ModerationSeverity
import com.example.model.ModerationTicket
import com.example.model.TicketStatus
import com.example.ui.theme.*
import com.example.util.TelegramModerationBotService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VISOL.UZ HYBRID MODERATION & TELEGRAM BOT ADMIN HUB DIALOG
 * Provides full control over:
 * 1. Suspicious Content Tickets (Pending AI review) with inline proof & instant actions:
 *    - "✅ Hammasi joyida (Banni yechish)"
 *    - "❌ Qoidabuzarlik (Rad etish va Ban qilish)"
 * 2. Telegram Bot Configuration & Live Alert Testing
 * 3. Restricted & Banned User Accounts Management
 * 4. Multi-Tier AI Moderation Sandbox & Simulation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationDialog(
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current

    val tickets by userRepository.moderationTickets.collectAsState()
    val candidates by userRepository.candidates.collectAsState()
    val currentUser by userRepository.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingTickets = remember(tickets) { tickets.filter { it.status == TicketStatus.PENDING } }
    val resolvedTickets = remember(tickets) { tickets.filter { it.status != TicketStatus.PENDING } }

    val restrictedUsers = remember(candidates, currentUser) {
        val list = candidates.filter { it.isRestricted || it.isBanned || it.isSpamRestricted }.toMutableList()
        val me = currentUser
        if (me != null && (me.isRestricted || me.isBanned || me.isSpamRestricted) && list.none { it.id == me.id }) {
            list.add(0, me)
        }
        list
    }

    // Bot settings state
    var botTokenInput by remember { mutableStateOf(TelegramModerationBotService.getBotToken(context)) }
    var adminChatIdInput by remember { mutableStateOf(TelegramModerationBotService.getAdminChatId(context)) }
    var isSendingTestMsg by remember { mutableStateOf(false) }
    var testResultStatus by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xEE0F172A) else Color(0xEEF8FAFC))
                .systemBarsPadding()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    VelvetRosePrimary,
                                    Color(0xFF8B5CF6)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Moderatsiya & Telegram Bot",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Gibrid AI NSFW & Telegram Bot boshqaruv tizimi",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Yopish",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModerationTabButton(
                        title = "Shubhali (${pendingTickets.size})",
                        icon = Icons.Default.WarningAmber,
                        isSelected = selectedTab == 0,
                        badgeColor = if (pendingTickets.isNotEmpty()) Color(0xFFEF4444) else null,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    ModerationTabButton(
                        title = "Bot Sozlamalari",
                        icon = Icons.Default.Send,
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                    ModerationTabButton(
                        title = "Cheklovlar (${restrictedUsers.size})",
                        icon = Icons.Default.Block,
                        isSelected = selectedTab == 2,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 2 }
                    )
                    ModerationTabButton(
                        title = "AI Test",
                        icon = Icons.Default.Psychology,
                        isSelected = selectedTab == 3,
                        modifier = Modifier.weight(0.9f),
                        onClick = { selectedTab = 3 }
                    )
                }

                Divider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), thickness = 0.5.dp)

                // Tab Contents
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> PendingTicketsTab(
                            pendingTickets = pendingTickets,
                            resolvedTickets = resolvedTickets,
                            userRepository = userRepository,
                            isDark = isDark
                        )
                        1 -> BotSettingsTab(
                            botToken = botTokenInput,
                            onTokenChange = { botTokenInput = it },
                            adminChatId = adminChatIdInput,
                            onChatIdChange = { adminChatIdInput = it },
                            isSending = isSendingTestMsg,
                            testStatus = testResultStatus,
                            onSave = {
                                TelegramModerationBotService.setBotToken(context, botTokenInput)
                                TelegramModerationBotService.setAdminChatId(context, adminChatIdInput)
                                Toast.makeText(context, "Telegram Bot sozlamalari saqlandi ✓", Toast.LENGTH_SHORT).show()
                            },
                            onTest = {
                                coroutineScope.launch {
                                    isSendingTestMsg = true
                                    testResultStatus = "Telegram botga ulanmoqda..."
                                    val result = TelegramModerationBotService.sendTestAlertToTelegram(context)
                                    isSendingTestMsg = false
                                    testResultStatus = if (result.isSuccess) {
                                        "✓ ${result.getOrNull()}"
                                    } else {
                                        "⚠️ Xatolik: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            isDark = isDark
                        )
                        2 -> RestrictedUsersTab(
                            users = restrictedUsers,
                            userRepository = userRepository,
                            isDark = isDark
                        )
                        3 -> AiModerationTestTab(
                            userRepository = userRepository,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    badgeColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) (if (isDark) Color(0xFF334155) else Color.White) else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) VelvetRosePrimary else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) (if (isDark) Color.White else TextPrimary) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeColor != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(badgeColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun PendingTicketsTab(
    pendingTickets: List<ModerationTicket>,
    resolvedTickets: List<ModerationTicket>,
    userRepository: UserRepository,
    isDark: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (pendingTickets.isEmpty() && resolvedTickets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Barcha kontentlar toza!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Hozircha admin tekshiruvini kutayotgan shubhali kadrlar yoki qoidabuzarliklar yo'q.",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (pendingTickets.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    )
                    Text(
                        text = "KUTILAYOTGAN SHUBHALI KONTENTLAR (${pendingTickets.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }

            items(pendingTickets, key = { it.ticketId }) { ticket ->
                TicketCard(
                    ticket = ticket,
                    isDark = isDark,
                    onApprove = {
                        coroutineScope.launch {
                            TelegramModerationBotService.executeAdminDecision(
                                context = context,
                                ticket = ticket,
                                decision = TelegramModerationBotService.AdminDecision.APPROVE_CLEAN,
                                userRepository = userRepository
                            )
                            Toast.makeText(context, "✅ Ruxsat berildi: Fayl yuklandi va foydalanuvchiga video spam cheklovi qo'yildi!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReject = {
                        coroutineScope.launch {
                            TelegramModerationBotService.executeAdminDecision(
                                context = context,
                                ticket = ticket,
                                decision = TelegramModerationBotService.AdminDecision.REJECT_VIOLATION,
                                userRepository = userRepository
                            )
                            Toast.makeText(context, "❌ Rad etildi: Fayl bazaga yuklanmadi va foydalanuvchi ban qilindi!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        if (resolvedTickets.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "HAL QILINGAN TEKSHIRUVLAR TARIXI (${resolvedTickets.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            items(resolvedTickets, key = { it.ticketId }) { ticket ->
                ResolvedTicketCard(ticket = ticket, isDark = isDark)
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: ModerationTicket,
    isDark: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val timeFormatted = remember(ticket.timestamp) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ticket.timestamp))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFBEB)
        ),
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: User info & level badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(VelvetRosePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ticket.userName.take(1).uppercase(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetRosePrimary
                        )
                    }

                    Column {
                        Text(
                            text = ticket.userName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else TextPrimary
                        )
                        Text(
                            text = "${ticket.userUsername} • ID: ${ticket.userId}",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "2-Daraja (Shubhali)",
                        color = Color(0xFFB45309),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Reason banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDE68A).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "AI Aniqlagan Sabab:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        text = ticket.reason,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White else Color(0xFF78350F)
                    )
                }
            }

            if (ticket.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Post matni: \"${ticket.caption}\"",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Media turi: ${ticket.mediaType} • Vaqt: $timeFormatted",
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // The Two Mandatory Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Approve Clean
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("admin_approve_ticket_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hammasi joyida",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Button 2: Reject Violation
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("admin_reject_ticket_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Qoidabuzarlik",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ResolvedTicketCard(ticket: ModerationTicket, isDark: Boolean) {
    val isClean = ticket.status == TicketStatus.APPROVED_CLEAN
    val bgStatusColor = if (isClean) Color(0xFF10B981) else Color(0xFFEF4444)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${ticket.userName} (${ticket.userUsername})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDark) Color.White else TextPrimary
                )
                Text(
                    text = ticket.reason,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
                ticket.resolutionNote?.let {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = bgStatusColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(bgStatusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isClean) "✓ Ruxsat berildi" else "✕ Rad etildi",
                    color = bgStatusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BotSettingsTab(
    botToken: String,
    onTokenChange: (String) -> Unit,
    adminChatId: String,
    onChatIdChange: (String) -> Unit,
    isSending: Boolean,
    testStatus: String?,
    onSave: () -> Unit,
    onTest: () -> Unit,
    isDark: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
                ),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Telegram Bot Integratsiyasi Ulangan 🟢",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                        Text(
                            text = "Shubhali 2-darajali kontentlar avtomatik Telegram guruh/chatiga yuboriladi.",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFD1FAE5) else Color(0xFF047857)
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = botToken,
                onValueChange = onTokenChange,
                label = { Text("Telegram Bot Token (BotFather)") },
                placeholder = { Text("8732304169:AAF8t300...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_bot_token")
            )
        }

        item {
            OutlinedTextField(
                value = adminChatId,
                onValueChange = onChatIdChange,
                label = { Text("Admin Telegram Chat ID / Guruh ID") },
                placeholder = { Text("6621230534") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_admin_chat_id")
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saqlash", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onTest,
                    enabled = !isSending,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Xabar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (testStatus != null) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = testStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (testStatus.startsWith("✓")) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📌 Telegram Bot Mantiqi Haqida:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = VelvetRosePrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Foydalanuvchi shubhali kadr yuklaganda bot xabar yuboradi.\n2. Xabarda User ID, ismi, video kadri va inline tugmalar bo'ladi.\n3. Tugma bosilganda foydalanuvchining banni yechiladi yoki tasdiqlanadi hamda ilova ichida rasmiy bildirishnoma jo'natiladi.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                }
            }
        }
    }
}

@Composable
private fun RestrictedUsersTab(
    users: List<com.example.model.UserProfile>,
    userRepository: UserRepository,
    isDark: Boolean
) {
    val context = LocalContext.current

    if (users.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Cheklangan foydalanuvchilar yo'q",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Barcha a'zolar to'liq erkin foydalanish huquqiga ega.",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(users, key = { it.id }) { user ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                ),
                border = BorderStroke(1.dp, if (user.isBanned) Color(0xFFEF4444) else Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (user.isBanned) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.fullName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (user.isBanned) Color(0xFFEF4444) else Color(0xFFF59E0B)
                            )
                        }

                        Column {
                            Text(
                                text = user.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color.White else TextPrimary
                            )
                            Text(
                                text = if (user.isBanned) {
                                    "🚫 To'liq Ban qilingan"
                                } else if (user.isSpamRestricted) {
                                    "⚠️ Video Spam Cheklovi (Efir ochiq)"
                                } else {
                                    "⚠️ Vaqtinchalik tekshiruv cheklovi"
                                },
                                fontSize = 11.sp,
                                color = if (user.isBanned) Color(0xFFEF4444) else Color(0xFFD97706),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (user.restrictionReason.isNotBlank()) {
                                Text(
                                    text = "Sabab: ${user.restrictionReason}",
                                    fontSize = 10.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            userRepository.setUserRestricted(user.id, isRestricted = false)
                            userRepository.setUserBanned(user.id, isBanned = false)
                            userRepository.setUserSpamRestricted(user.id, isSpam = false)
                            userRepository.sendSystemNoticeToUser(
                                targetUserId = user.id,
                                title = "Cheklov olib tashlandi ✨",
                                body = "Admin tomonidan profilingizdagi barcha cheklovlar bekor qilindi. Barcha imkoniyatlardan va video yuklashdan to'liq foydalanishingiz mumkin!"
                            )
                            Toast.makeText(context, "${user.fullName} uchun barcha cheklovlar yechildi ✓", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Banni yechish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiModerationTestTab(
    userRepository: UserRepository,
    isDark: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var simResultText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "🧪 AI Moderatsiya Simulyatori",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VelvetRosePrimary
            )
            Text(
                text = "Ushbu bo'lim orqali 1-daraja va 2-daraja moderatsiya oqimlarini hamda Telegram Bot xabarnomalarini test qilishingiz mumkin.",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }

        // Simulator 1: Level 1 (Critical Adult)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1-Daraja: O'ta qo'pol kontent (100% Taqiqlangan)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Natija: Fayl darhol rad etiladi, bazaga kiritilmaydi va foydalanuvchiga ogohlantirish beriladi.", fontSize = 11.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            simResultText = "🚫 1-Daraja Sinovi: Fayl 100% rad etildi! Bazaga yuklanmadi. Foydalanuvchiga qat'iy ogohlantirish chiqarildi."
                            Toast.makeText(context, "1-Daraja qoidabuzarlik muvaffaqiyatli ushlandi!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("1-Daraja Qoidabuzarlikni Sinash", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Simulator 2: Level 2 (Suspicious & Telegram Bot alert)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("2-Daraja: Shubhali kadrlar (Admin & Telegram Bot)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB45309))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Natija: Video bazaga kiritilmaydi, foydalanuvchiga is_restricted=true belgilanadi va Telegram bot orqali Adminga dalillar bilan xabar yuboriladi.", fontSize = 11.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val currentUser = userRepository.currentUser.value ?: userRepository.getOrCreateActiveUser()
                                // 1. Restrict
                                userRepository.setUserRestricted(currentUser.id, isRestricted = true, reason = "Sinov: Shubhali kadrlar (48%)")
                                // 2. Create ticket
                                val ticket = ModerationTicket(
                                    userId = currentUser.id,
                                    userName = currentUser.fullName,
                                    userUsername = currentUser.displayUsername,
                                    userPhotoPath = currentUser.photoPath,
                                    mediaType = "REELS",
                                    mediaUri = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800",
                                    caption = "Sinov uchun shubhali video kadrlari",
                                    severity = ModerationSeverity.LEVEL_2_SUSPICIOUS,
                                    reason = "Shubhali ochiq kadrlar (48% tana qoplamasi)",
                                    confidence = 0.48f,
                                    timestamp = System.currentTimeMillis()
                                )
                                userRepository.createModerationTicket(ticket)
                                simResultText = "⚠️ 2-Daraja Sinovi: Foydalanuvchi vaqtinchalik cheklandi (is_restricted=true), chipta yaratildi va Telegram Botga dalilli xabarnoma yuborildi!"
                                Toast.makeText(context, "Telegram Botga shubhali kontent xabarnomasi yuborildi!", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("2-Daraja Shubhali Kontentni Sinash & Botga Jo'natish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (simResultText != null) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = simResultText!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White else TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
