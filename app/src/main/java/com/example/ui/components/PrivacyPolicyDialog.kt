package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetRoseDark
import com.example.ui.theme.VelvetRosePrimary
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.launch

/**
 * Visol.uz Maxfiylik Siyosati va Foydalanish Qoidalari Modali.
 * Ilovadagi barcha tizimlarni (Face ID, Geomanzil, Reels/Post moderatsiyasi, AI Hakam, Telegram Admin, VIP obuna)
 * to'liq skaner qilib shakllantirilgan.
 * 
 * SHART: Foydalanuvchi hujjatni oxirigacha scroll qilib o'qib chiqmaguncha "Rozi bo'lish" tugmasi ochilmaydi.
 */
@Composable
fun PrivacyPolicyDialog(
    isDark: Boolean,
    onAccept: () -> Unit
) {
    val scrollState = rememberScrollState()

    val coroutineScope = rememberCoroutineScope()

    // Foydalanuvchi hujjat oxirigacha yetib borganligini aniqlash (Compose canScrollForward va maxValue)
    val hasReachedBottom by remember {
        derivedStateOf {
            if (scrollState.maxValue <= 0) true
            else scrollState.value >= (scrollState.maxValue - 50) || !scrollState.canScrollForward
        }
    }

    Dialog(
        onDismissRequest = { /* Majburiy qabul qilish, tashqariga bosganda yopilmaydi */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF0F0B17) else Color(0xFFFAF7F5)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header (Sarlavha)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF1C142A) else Color.White,
                    border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VelvetRosePrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, VelvetRosePrimary),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "📜", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Visol.uz Maxfiylik Siyosati",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else TextPrimary
                            )
                            Text(
                                text = "Foydalanish Qoidalari va Xavfsizlik Shartnomasi",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else TextSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE11D48).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFFE11D48))
                        ) {
                            Text(
                                text = "Majburiy",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Policy Content Box
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF161122) else Color(0xFFF1F5F9).copy(alpha = 0.85f),
                    border = BorderStroke(0.8.dp, if (isDark) Color(0xFF2C243B) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(18.dp)
                    ) {
                        PolicySection(
                            icon = "🌸",
                            title = "1. Kirish va Ilovaning Maqsadi",
                            content = "«Visol.uz» platformasi O'zbekiston Respublikasi milliy qadriyatlari, odob-axloq normalari va oilaviy an'analariga asoslangan jiddiy oila qurish, nikoh va tanishuv xizmatidir. Ilovadan faqat jiddiy niyatli, voyaga yetgan (18 yoshdan oshgan) shaxslar foydalanishi shart."
                        )

                        PolicySection(
                            icon = "👤",
                            title = "2. Biometrik Face ID va Jonli Rasm Verifikatsiyasi",
                            content = "• Har bir foydalanuvchi hisobining haqiqiyligini ta'minlash uchun jonli kamera orqali selfi (Face ID) tasdiqlashdan o'tadi.\n• Begona shaxslarning, internetdan olingan yoki yuz qismi ko'rinmagan suratlardan foydalanish taqiqlanadi.\n• Biometrik tekshiruv soxta profillarning (feyk) oldini olish va jamiyat xavfsizligini kafolatlash uchun xizmat qiladi."
                        )

                        PolicySection(
                            icon = "📍",
                            title = "3. Manzil va Geolokatsiya Ma'lumotlari Maxfiyligi",
                            content = "• Viloyat, Tuman va Mahalla ma'lumotlari faqat mos nomzodlarni hududiy yaqinlik bo'yicha to'g'ri saralash uchun ishlatiladi.\n• Foydalanuvchining aniq uy manzili, geolokatsiya koordinatalari yoki shaxsiy hujjatlari hech qachon 3-shaxslarga taqdim etilmaydi."
                        )

                        PolicySection(
                            icon = "🎬",
                            title = "4. Reels, Postlar va Media Moderatsiyasi (18+ Taqiqlangan)",
                            content = "• Ilovada har qanday 18+ (kattalar uchun), ochiq-sochiq, intim, behayo, qurol, zo'ravonlik yoki giyohvandlikni targ'ib qiluvchi kontent qat'iyan taqiqlanadi.\n• Barcha yuklanadigan videolar va suratlar Google Gemini Multi-Modal AI va kompyuter ko'rish tizimi orqali avtomatlashtirilgan 2 bosqichli tekshiruvdan o'tkaziladi.\n• Qoidabuzarlik aniqlangan taqdirda, fayl tizimga yuklanmaydi, foydalanuvchi darhol cheklanadi (isRestricted=true) va dalillar Telegram Bot orqali Adminga yuboriladi."
                        )

                        PolicySection(
                            icon = "⚖️",
                            title = "5. Chatlar va O'zbekona Odob-Axloq Qoidalari (AI Hakam)",
                            content = "• Suhbatlarda onadan, oiladan so'kish, shaxsiyatga tegish, tahdid, psixologik bosim (bulling) va nojo'ya intim suratlar talab qilish qat'iy man etiladi.\n• Mojaroning oldini olish tizimi (3-shaxs sxemasi): Haqoratli yoki nojo'ya xabar yozilganda, u suhbatdoshga (2-shaxsga) mutlaqo bormaydi (zanjir uziladi). Qoidabuzarning o'ziga chat ichida maxfiy «AI Hakam» ogohlantirishi chiqariladi va ma'muriyatga bildiriladi."
                        )

                        PolicySection(
                            icon = "🛡️",
                            title = "6. Admin va Telegram Bot Moderatsiya Nazorati",
                            content = "• Shubhali kontent va xavfsizlik holatlari rasmiy Telegram Bot orqali administratorlar nazoratiga tushadi.\n• Adminlar xolis tekshiruv o'tkazib, qoidabuzarlarni butunlay chetlatish (Ban) yoki cheklovni bekor qilish bo'yicha yakuniy qaror qabul qilish huquqiga ega."
                        )

                        PolicySection(
                            icon = "👑",
                            title = "7. VIP Obuna, Tangalar va Aloqa Imtiyozlari",
                            content = "• VIP a'zolar to'liq profillarni ko'rish, 1-ga-1 shaxsiy audio/video qo'ng'iroqlardan foydalanish va to'g'ridan-to'g'ri xabarlashuv imkoniyatiga ega bo'ladilar.\n• Xizmatlar uchun to'lovlar ixtiyoriy bo'lib, platforma faoliyatini rivojlantirish va sifatli xizmat ko'rsatishga yo'naltiriladi."
                        )

                        PolicySection(
                            icon = "🗑️",
                            title = "8. Foydalanuvchi Huquqlari va Profilni O'chirish",
                            content = "• Har bir foydalanuvchi istalgan vaqtda o'z profilini, barcha xabarlarini, yuklagan postlari va fotosuratlarini ilovadan to'liq va qaytarib bo'lmas tarzda o'chirib yuborish (Hisobni o'chirish) huquqiga ega."
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scroll Progress Indicator
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasReachedBottom) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (hasReachedBottom) Color(0xFF10B981) else Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hasReachedBottom) "✅" else "⬇️",
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasReachedBottom) "Hujjat oxirigacha o'qib chiqildi. Rozilik bildirishingiz mumkin." else "Iltimos, shartnomani to'liq o'qib chiqish uchun pastgacha scroll qiling.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (hasReachedBottom) Color(0xFF10B981) else (if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Bar (Accept Button)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF1C142A) else Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, if (hasReachedBottom) WarmGold else Color(0x33888888)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!hasReachedBottom) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                },
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = VelvetRosePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Hujjat oxiriga tushish ⬇️",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VelvetRosePrimary
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                if (hasReachedBottom) {
                                    onAccept()
                                }
                            },
                            enabled = hasReachedBottom,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VelvetRosePrimary,
                                disabledContainerColor = if (isDark) Color(0xFF2C243B) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (hasReachedBottom) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (hasReachedBottom) Color.White else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasReachedBottom) "Qoidalarga to'liq roziman va Ilovaga kirish ✨" else "Hujjatni oxirigacha o'qing ⬇️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (hasReachedBottom) Color.White else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySection(
    icon: String,
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = VelvetRoseDark
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = Color(0xFF475569)
        )
    }
}
