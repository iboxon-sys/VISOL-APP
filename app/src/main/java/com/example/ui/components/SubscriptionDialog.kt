package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * 7. VIP SUBSCRIPTION MODAL:
 * - Refactor to full-height, scroll-free modal.
 * - Ultra-luxury romantic design with dark navy/rose gradient, gold/rose accents.
 * - Displays 1-month (15 000 so'm), 3-month (35 000 so'm), and 1-year (99 000 so'm) plans.
 * - All content fits elegantly without vertical clipping or awkward scrolling.
 */
@Composable
fun SubscriptionDialog(
    isAlreadySubscribed: Boolean = false,
    isMandatory: Boolean = false,
    onDismiss: () -> Unit,
    onSubscribed: (plan: String) -> Unit
) {
    var selectedPlan by remember { mutableStateOf("1_month") }

    Dialog(
        onDismissRequest = {
            if (!isMandatory) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Luxury Glass Container (Fits entirely without scrolling)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1B4B),
                                Color(0xFF0F172A),
                                Color(0xFF090D16)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                WarmGold,
                                VelvetRose.copy(alpha = 0.5f),
                                GlassBorderLight
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row: Close button / Mandatory badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = VelvetRoseDark.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, VelvetRose.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = WarmGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isMandatory) "MAJBURIY VIP OBUNA" else "VISOL VIP PREMIUM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = WarmGoldLight
                                )
                            }
                        }

                        if (isMandatory) {
                            // Majburiy holatda X bosilsa to'g'ridan-to'g'ri bosh sahifaga qaytaradi (chatda qolib ketmaydi)
                            TextButton(
                                onClick = onDismiss,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Chiqish ✕",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Yopish",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Crown Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(WarmGold.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                            .border(1.5.dp, WarmGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WarmGoldLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "VIP Imkoniyatlarni Ochish",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Barcha nomzodlarning telefon raqami, Telegram va Instagram profillarini cheklovlarsiz ko'ring.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Plans List (Compact, non-scrollable)
                    PlanSelectionRow(
                        planKey = "1_month",
                        title = "1 Oylik Kirish",
                        price = "15 000 so'm",
                        badge = "Eng ommabop",
                        isSelected = selectedPlan == "1_month",
                        onSelect = { selectedPlan = "1_month" }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PlanSelectionRow(
                        planKey = "3_months",
                        title = "3 Oylik Kirish",
                        price = "35 000 so'm",
                        badge = "Tejamkor (25% off)",
                        isSelected = selectedPlan == "3_months",
                        onSelect = { selectedPlan = "3_months" }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PlanSelectionRow(
                        planKey = "1_year",
                        title = "1 Yillik VIP Status",
                        price = "99 000 so'm",
                        badge = "VIP Cheksiz",
                        isSelected = selectedPlan == "1_year",
                        onSelect = { selectedPlan = "1_year" }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PlanSelectionRow(
                        planKey = "10_ads",
                        title = "📺 10 ta Reklama Ko'rish",
                        price = "0 so'm (4 Soat VIP)",
                        badge = "BEPUL",
                        isSelected = selectedPlan == "10_ads",
                        onSelect = { selectedPlan = "10_ads" }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    var showWatchAdsModal by remember { mutableStateOf(false) }

                    // Pay / Activate Button
                    Button(
                        onClick = {
                            if (isAlreadySubscribed) {
                                onDismiss()
                            } else if (selectedPlan == "10_ads") {
                                showWatchAdsModal = true
                            } else {
                                val planName = when (selectedPlan) {
                                    "1_month" -> "1 oylik (15 000 so'm)"
                                    "3_months" -> "3 oylik (35 000 so'm)"
                                    else -> "1 yillik VIP (99 000 so'm)"
                                }
                                onSubscribed(planName)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("activate_subscription_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAlreadySubscribed) Color(0xFF10B981) else VelvetRose
                        )
                    ) {
                        Icon(
                            imageVector = if (isAlreadySubscribed) Icons.Default.CheckCircle else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAlreadySubscribed) "✓ Obuna faollashgan (VIP Faol)" else if (selectedPlan == "10_ads") "Reklamalarni Boshlash (10 ta)" else "Obunani Faollashtirish",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (showWatchAdsModal) {
                        WatchAdsDialog(
                            onDismiss = { showWatchAdsModal = false },
                            onCompleted = {
                                showWatchAdsModal = false
                                onSubscribed("Demo Reklama (4 soat)")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "🔒 Xavfsiz to'lov • Payme, Click & Uzum orqali kafolatlangan",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanSelectionRow(
    planKey: String,
    title: String,
    price: String,
    badge: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) WarmGold else Color(0x33FFFFFF),
        label = "plan_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0x33F59E0B) else Color(0x14FFFFFF),
        label = "plan_bg"
    )

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = WarmGold,
                        unselectedColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (badge.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) WarmGold else Color(0x33E11D48)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else VelvetRoseLight,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Kontaktlar va tarmoqlar ochiq",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Text(
                text = price,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) WarmGoldLight else Color.White
            )
        }
    }
}
