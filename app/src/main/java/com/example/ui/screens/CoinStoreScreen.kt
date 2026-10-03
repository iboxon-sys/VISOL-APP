package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserRepository
import com.example.model.CoinPackage
import com.example.model.SampleCoinPackages
import com.example.model.SampleVipTiers
import com.example.model.VipTier
import com.example.ui.theme.*

/**
 * LUXURY COIN & VIP TIER STORE (2 TA RUYXATLI BO'LIM)
 * - 1-taraf: Monetalar xaridi (Click, Payme, Uzum, Bank kartalari)
 * - 2-taraf: VIP 1 dan VIP 12 gacha darajalarni monetaga sotib olish / ayirboshlash
 */
@Composable
fun CoinStoreScreen(
    userRepository: UserRepository
) {
    val context = LocalContext.current
    val isDark = userRepository.themeMode.collectAsState().value != "LIGHT"
    val userCoins by userRepository.userCoins.collectAsState()
    val userVipLevel by userRepository.userVipLevel.collectAsState()

    var selectedStoreTab by remember { mutableIntStateOf(0) } // 0: Monetalar Xaridi, 1: VIP 1..12 Ayirboshlash
    var selectedPaymentMethod by remember { mutableStateOf("Click") }
    var selectedPackageToBuy by remember { mutableStateOf<CoinPackage?>(null) }
    var selectedVipTierToBuy by remember { mutableStateOf<VipTier?>(null) }

    val packages = remember { SampleCoinPackages.packages }
    val vipTiers = remember { SampleVipTiers.tiers }

    val infiniteTransition = rememberInfiniteTransition(label = "store_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val currentTier = remember(userVipLevel) {
        vipTiers.find { it.level == userVipLevel } ?: vipTiers.first()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0C0617) else PearlBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. TOP HEADER & BALANCE CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(16.dp, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF350E4C),
                                    Color(0xFF1D072D),
                                    Color(0xFF0F0318)
                                )
                            )
                        )
                        .border(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(
                                    WarmHoneyGold.copy(alpha = glowAlpha),
                                    SoftRosePink.copy(alpha = 0.7f),
                                    SoftPeach.copy(alpha = 0.85f)
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WarmHoneyGold.copy(alpha = 0.2f),
                                    border = BorderStroke(0.8.dp, WarmHoneyGold.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "💎 MONETA BALANSI",
                                        color = WarmHoneyGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SoftRosePink.copy(alpha = 0.25f),
                                    border = BorderStroke(0.8.dp, SoftRosePink)
                                ) {
                                    Text(
                                        text = "${currentTier.badgeIcon} VIP $userVipLevel",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("💎", fontSize = 28.sp)
                                Text(
                                    text = "$userCoins",
                                    color = WarmHoneyGold,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "tanga",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Right mini booster pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x35FFFFFF),
                            border = BorderStroke(1.dp, WarmHoneyGold.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                selectedStoreTab = 1
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(currentTier.badgeIcon, fontSize = 20.sp)
                                Text(
                                    text = "VIP $userVipLevel",
                                    color = WarmHoneyGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "O'zgartirish ➔",
                                    color = SoftPeach,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. SEGMENTED DUAL TAB SELECTOR (1: Monetalar, 2: VIP 1..12)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(
                        if (isDark) Color(0xFF1E1032) else Color(0xFFE2E8F0),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 1: Monetalar Olish
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedStoreTab == 0) WarmHoneyGold else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedStoreTab = 0 }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💎", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1. Monetalar Olish",
                            color = if (selectedStoreTab == 0) Color.Black else (if (isDark) Color.White else TextPrimary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Tab 2: VIP 1 dan VIP 12 gacha
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedStoreTab == 1) VelvetRosePrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedStoreTab = 1 }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👑", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2. VIP 1..12 Ayirboshlash",
                            color = if (selectedStoreTab == 1) Color.White else (if (isDark) Color.White else TextPrimary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 3. TAB 1: MONETALAR SOTIB OLISH (COINS)
            if (selectedStoreTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Payment method picker
                    item(span = { GridItemSpan(2) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "💳 TO'LOV TIZIMINI TANLANG:",
                                color = if (isDark) WarmHoneyGold else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val methods = listOf(
                                    Triple("Click", Color(0xFF0073FF), "🔵"),
                                    Triple("Payme", Color(0xFF00CCCC), "💠"),
                                    Triple("Uzum Bank", Color(0xFF7000FF), "🍇"),
                                    Triple("Anor / Karta", Color(0xFFE11D48), "💳")
                                )
                                methods.forEach { (method, brandColor, icon) ->
                                    val isSelected = selectedPaymentMethod == method
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) brandColor.copy(alpha = 0.25f) else if (isDark) Color(0x22FFFFFF) else Color(0x10000000),
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) brandColor else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedPaymentMethod = method }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(icon, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = method,
                                                color = if (isSelected) (if (isDark) Color.White else brandColor) else if (isDark) Color.White.copy(alpha = 0.8f) else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🎁 RASMIY TANGA PAKETLARI:",
                                color = if (isDark) WarmHoneyGold else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Tezkor to'lov ⚡",
                                color = SoftRosePink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(packages) { pkg ->
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isDark) Color(0x28FFFFFF) else Color.White,
                            border = BorderStroke(
                                if (pkg.isPopular) 1.5.dp else 1.dp,
                                if (pkg.isPopular) WarmHoneyGold else if (isDark) Color(0x20FFFFFF) else Color(0x15000000)
                            ),
                            shadowElevation = if (pkg.isPopular) 10.dp else 3.dp,
                            modifier = Modifier.clickable {
                                selectedPackageToBuy = pkg
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (pkg.isPopular) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SoftRosePink
                                    ) {
                                        Text(
                                            text = "🔥 ENG OMMABOP",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("💎", fontSize = 22.sp)
                                    Text(
                                        text = "${pkg.coins}",
                                        color = if (isDark) Color.White else TextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                if (pkg.bonusCoins > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = WarmHoneyGold.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "+${pkg.bonusCoins} Bonus! 🎁",
                                            color = WarmHoneyGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { selectedPackageToBuy = pkg },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (pkg.isPopular) WarmHoneyGold else Color(0x40FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = pkg.priceUzs,
                                        color = if (pkg.isPopular) Color.Black else if (isDark) Color.White else Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 4. TAB 2: VIP 1 DAN VIP 12 GACHA (MONETAGA AYIRBOSHLASH)
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x356A1B9A),
                            border = BorderStroke(1.dp, WarmHoneyGold.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "👑 VIP 1 DAN VIP 12 GACHA IMTIYOZLAR",
                                    color = WarmHoneyGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Monetalaringizni sarflab VIP darajangizni oshiring. Har bir daraja o'ziga xos hashamatli chat ramkasi, kirish avtomashinasi/animatsiyasi va Star ko'paytirgichiga ega!",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    items(vipTiers) { tier ->
                        val isOwned = userVipLevel >= tier.level
                        val isCurrent = userVipLevel == tier.level
                        val tierBorderColor = Color(tier.borderColorHex)

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isDark) Color(0x331C0B36) else Color.White,
                            border = BorderStroke(
                                if (isCurrent) 2.dp else 1.2.dp,
                                if (isCurrent) WarmHoneyGold else tierBorderColor.copy(alpha = 0.8f)
                            ),
                            shadowElevation = if (isCurrent) 8.dp else 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Tier Title & Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(tier.badgeIcon, fontSize = 24.sp)
                                        Column {
                                            Text(
                                                text = tier.title,
                                                color = if (isDark) Color.White else TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = tier.frameTitle,
                                                color = tierBorderColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = WarmHoneyGold
                                        ) {
                                            Text(
                                                text = "FAOL DARAJA ✓",
                                                color = Color.Black,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else if (isOwned) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF10B981)
                                        ) {
                                            Text(
                                                text = "OCHILGAN 🔓",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = WarmHoneyGold.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, WarmHoneyGold)
                                        ) {
                                            Text(
                                                text = "💎 ${tier.coinPrice} tanga",
                                                color = WarmHoneyGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Live Frame Preview Box
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x35000000),
                                    border = BorderStroke(1.5.dp, tierBorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(tier.badgeIcon, fontSize = 16.sp)
                                        Text(
                                            text = "Chat Ramkasi:",
                                            color = WarmHoneyGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "\"Salom xona ahlari! ✨\"",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "Kirish: ${tier.entranceVehicle}",
                                            color = SoftPeach,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Perks bullets
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    tier.perks.forEach { perk ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("✨", fontSize = 10.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = perk,
                                                color = if (isDark) Color.White.copy(alpha = 0.8f) else TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // Action Button
                                Button(
                                    onClick = {
                                        if (isCurrent) {
                                            Toast.makeText(context, "Siz hozirda ${tier.title} darajasidasiz ✨", Toast.LENGTH_SHORT).show()
                                        } else if (userCoins >= tier.coinPrice) {
                                            selectedVipTierToBuy = tier
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Tangalaringiz yetarli emas! Sizda $userCoins tanga bor. Monetalar bo'limidan hisobni to'ldiring 💎",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrent) Color(0x40FFFFFF) else if (userCoins >= tier.coinPrice) VelvetRosePrimary else Color(0x35E11D48)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isCurrent) "Hozirgi Darajangiz ✓" else if (isOwned) "Faollashtirish (VIP ${tier.level})" else "💎 ${tier.coinPrice} Tangaga Sotib Olish",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. CONFIRM COIN PURCHASE DIALOG
        selectedPackageToBuy?.let { pkg ->
            AlertDialog(
                onDismissRequest = { selectedPackageToBuy = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💎", fontSize = 20.sp)
                        Text("Moneta Xarid Qilish", color = WarmHoneyGold, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Siz ${pkg.coins} (+${pkg.bonusCoins} bonus) tanga xarid qilmoqchisiz.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33FFD700),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("To'lanadigan summa:", color = Color.White, fontSize = 13.sp)
                                Text(pkg.priceUzs, color = WarmHoneyGold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(
                            "To'lov tizimi: $selectedPaymentMethod ⚡",
                            color = SoftPeach,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val total = pkg.coins + pkg.bonusCoins
                            userRepository.addCoins(total)
                            selectedPackageToBuy = null
                            Toast.makeText(context, "$selectedPaymentMethod orqali $total tanga hisobingizga muvaffaqiyatli qo'shildi! 💎✨", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmHoneyGold),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("To'lash va To'ldirish ✨", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedPackageToBuy = null }) {
                        Text("Bekor qilish", color = Color.White)
                    }
                },
                containerColor = Color(0xF21C0F2D)
            )
        }

        // 6. CONFIRM VIP PURCHASE / UPGRADE DIALOG
        selectedVipTierToBuy?.let { tier ->
            AlertDialog(
                onDismissRequest = { selectedVipTierToBuy = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tier.badgeIcon, fontSize = 22.sp)
                        Text(tier.title, color = WarmHoneyGold, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Hisobingizdagi ${tier.coinPrice} tangani sarflab VIP ${tier.level} darajasiga o'tmoqchimisiz?",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33FFD700),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sarflanadigan tanga:", color = Color.White, fontSize = 12.sp)
                                Text("💎 ${tier.coinPrice}", color = WarmHoneyGold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(
                            text = "✅ Olinadigan imtiyoz: ${tier.frameTitle} va ${tier.entranceVehicle} kirish animatsiyasi!",
                            color = SoftPeach,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (userRepository.upgradeVipLevel(tier.level, tier.coinPrice)) {
                                selectedVipTierToBuy = null
                                Toast.makeText(
                                    context,
                                    "Tabriklaymiz! 🎉 Siz muvaffaqiyatli ${tier.title} darajasiga erishdingiz! 👑✨",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Tangalaringiz yetarli emas!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Monetaga Sotib Olish ✨", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedVipTierToBuy = null }) {
                        Text("Bekor qilish", color = Color.White)
                    }
                },
                containerColor = Color(0xF21C0F2D)
            )
        }
    }
}
