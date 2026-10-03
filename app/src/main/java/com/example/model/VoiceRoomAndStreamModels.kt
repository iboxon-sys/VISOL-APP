package com.example.model

data class VoicePartyRoom(
    val id: String,
    val title: String,
    val hostId: String,
    val hostName: String,
    val hostPhotoPath: String,
    val hostGender: String = "Ayol",
    val hostBadge: String = "👑 Boshlovchi",
    val category: String = "Tanishuv & Gurung",
    val announcementText: String = "Bu hayotda ko'pchilik uchun jon kuydirasan. Samimiyat hech qachon urfdan qolmaydi.",
    val isPrivate: Boolean = false,
    val roomPassword: String? = null,
    val roomIdDisplay: String = "13199329",
    val expCurrent: Int = 1878,
    val expMax: Int = 5500,
    val starPoints: Int = 810915,
    val speakers: List<VoiceRoomSpeaker> = emptyList(),
    val listenerCount: Int = 28,
    val backgroundMusic: String? = null,
    val bgGradientColorsHex: List<Long> = listOf(0xFF2E1038, 0xFF120824),
    val isLiveStream: Boolean = false,
    val streamVideoUrl: String? = null
)

data class VoiceRoomSpeaker(
    val seatIndex: Int, // 0 to 7
    val userId: String,
    val userName: String,
    val photoPath: String,
    val gender: String = "Ayol",
    val isHost: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val vipLevel: Int = 5,
    val starPoints: Int = 2,
    val receivedGiftsCount: Int = 0
)

data class VirtualGift(
    val id: String,
    val name: String,
    val icon: String,
    val coinPrice: Int,
    val animationEffect: String // "ROSE", "RING", "CROWN", "CAR", "ROCKET", "CASTLE", "GALAXY"
)

data class CoinPackage(
    val coins: Int,
    val priceUzs: String,
    val bonusCoins: Int = 0,
    val isPopular: Boolean = false
)

data class VipTier(
    val level: Int, // 1 to 12
    val title: String,
    val badgeIcon: String,
    val coinPrice: Int,
    val frameTitle: String,
    val description: String,
    val perks: List<String>,
    val entranceVehicle: String, // e.g. "Sportkar 🏎️", "Ajdaho 🐉", "Raketa 🚀"
    val gradientColors: List<Long>,
    val borderColorHex: Long
)

data class VoiceRoomOnlineUser(
    val id: String,
    val name: String,
    val username: String,
    val photoPath: String,
    val vipLevel: Int,
    val isSpeaker: Boolean = false,
    val isHost: Boolean = false,
    val role: String = "Tinglovchi"
)

data class LiveRoomChatMessage(
    val id: String,
    val senderId: String? = null,
    val senderUsername: String? = null,
    val senderName: String,
    val senderPhoto: String? = null,
    val senderBadge: String? = null,
    val messageText: String,
    val giftNotice: String? = null,
    val vipLevel: Int = 1,
    val isSystem: Boolean = false,
    val isEntrance: Boolean = false,
    val entranceAnimationVehicle: String? = null,
    val timestamp: String = "Hozir"
)

object SampleCoinPackages {
    val packages = listOf(
        CoinPackage(100, "16 000 so'm"),
        CoinPackage(300, "45 000 so'm", bonusCoins = 30),
        CoinPackage(600, "85 000 so'm", bonusCoins = 80, isPopular = true),
        CoinPackage(1200, "160 000 so'm", bonusCoins = 180),
        CoinPackage(2500, "320 000 so'm", bonusCoins = 400),
        CoinPackage(5000, "620 000 so'm", bonusCoins = 900, isPopular = true),
        CoinPackage(10000, "1 190 000 so'm", bonusCoins = 2200),
        CoinPackage(25000, "2 850 000 so'm", bonusCoins = 6000),
        CoinPackage(60000, "6 500 000 so'm", bonusCoins = 18000),
        CoinPackage(150000, "15 000 000 so'm", bonusCoins = 50000, isPopular = true)
    )
}

object SampleVipTiers {
    val tiers = listOf(
        VipTier(
            level = 1,
            title = "VIP 1 • Bronza Ritsar",
            badgeIcon = "🥉",
            coinPrice = 100,
            frameTitle = "Bronza Yaltiroq Ramka",
            description = "Boshlang'ich VIP maqomi va o'ziga xos bronza chat hoshiyasi",
            perks = listOf("Bronza chat hoshiyasi", "VIP 1 nishoni", "1.1x Star bonus"),
            entranceVehicle = "Piyoda salom ✨",
            gradientColors = listOf(0xFFCD7F32, 0xFF8C531B),
            borderColorHex = 0xFFCD7F32
        ),
        VipTier(
            level = 2,
            title = "VIP 2 • Kumush Baron",
            badgeIcon = "🥈",
            coinPrice = 300,
            frameTitle = "Kumush Shisha Ramka",
            description = "Yorqin kumush tusli shaffof chat ramkasi va kirish xabarnomasi",
            perks = listOf("Kumush shisha ramka", "Maxsus kumush rang nickname", "1.15x Star bonus"),
            entranceVehicle = "Oltin skuter 🛵",
            gradientColors = listOf(0xFFC0C0C0, 0xFF808080),
            borderColorHex = 0xFFE2E8F0
        ),
        VipTier(
            level = 3,
            title = "VIP 3 • Oltin Graf",
            badgeIcon = "🥇",
            coinPrice = 600,
            frameTitle = "Oltin Nurlar Ramkasi",
            description = "Yaltiroq tillarang zarhal ramka va suhbatda ajralib turuvchi ovoz effekti",
            perks = listOf("Zarhal oltin ramka", "Oltin kirish animatsiyasi", "1.2x Star bonus"),
            entranceVehicle = "Klassik kabriolet 🚗",
            gradientColors = listOf(0xFFFFD700, 0xFFD4AF37),
            borderColorHex = 0xFFFACC15
        ),
        VipTier(
            level = 4,
            title = "VIP 4 • Zumrad Gertsog",
            badgeIcon = "❇️",
            coinPrice = 1200,
            frameTitle = "Zumrad Porlashi Ramkasi",
            description = "Yashil zumrad jilosidagi neon hoshiya va mikrofon nuri",
            perks = listOf("Zumrad neon ramka", "Mikrofon atrofida yashil aura", "1.25x Star bonus"),
            entranceVehicle = "Zumrad Gelik 🚙",
            gradientColors = listOf(0xFF10B981, 0xFF047857),
            borderColorHex = 0xFF34D399
        ),
        VipTier(
            level = 5,
            title = "VIP 5 • Qirollik Shahzodasi",
            badgeIcon = "👑",
            coinPrice = 2500,
            frameTitle = "Qirollik Toji Ramkasi",
            description = "Oltin tojli hashamatli ramka va xonaga kirganda shohona banner",
            perks = listOf("Tojli qirollik ramkasi", "Barcha tinglovchilarga salomlashuv", "1.3x Star bonus"),
            entranceVehicle = "Shohona Limuzin 🚘",
            gradientColors = listOf(0xFFF59E0B, 0xFFEC4899),
            borderColorHex = 0xFFF59E0B
        ),
        VipTier(
            level = 6,
            title = "VIP 6 • Supercar Chempioni",
            badgeIcon = "🏎️",
            coinPrice = 5000,
            frameTitle = "Sportkar Neon Ramkasi",
            description = "Sportkar kirish video animatsiyasi va qizil neon poyga ramkasi",
            perks = listOf("Sportkar poyga ramkasi", "🏎️ Ekran bo'ylab sportkar uchishi", "1.4x Star bonus"),
            entranceVehicle = "Ferrari Supercar 🏎️",
            gradientColors = listOf(0xFFEF4444, 0xFFB91C1C),
            borderColorHex = 0xFFF87171
        ),
        VipTier(
            level = 7,
            title = "VIP 7 • Yoqut Senator",
            badgeIcon = "💎",
            coinPrice = 10000,
            frameTitle = "Yoqut Kristal Ramkasi",
            description = "Yaltiroq qimmatbaho yoqut toshli ramka va audio to'lqin aurasida porlash",
            perks = listOf("Qizil yoqut ramka", "Ovoz to'lqinida yoqut auralari", "1.5x Star bonus"),
            entranceVehicle = "Yoqut Vertolyot 🚁",
            gradientColors = listOf(0xFFE11D48, 0xFF9F1239),
            borderColorHex = 0xFFFB7185
        ),
        VipTier(
            level = 8,
            title = "VIP 8 • Olovli Ajdaho",
            badgeIcon = "🐉",
            coinPrice = 20000,
            frameTitle = "Ajdaho Olovi Ramkasi",
            description = "Olovli ajdaho aylanuvchi super-ramka va ekranni yorituvchi olov effekti",
            perks = listOf("Olovli ajdaho ramkasi", "🐉 Ajdaho parvozi kirish animatsiyasi", "1.6x Star bonus"),
            entranceVehicle = "Olovli Ajdaho 🐉",
            gradientColors = listOf(0xFFFF5722, 0xFFE64A19),
            borderColorHex = 0xFFFF7043
        ),
        VipTier(
            level = 9,
            title = "VIP 9 • Kosmik Galaktika",
            badgeIcon = "🌌",
            coinPrice = 40000,
            frameTitle = "Nebula Yulduzlar Ramkasi",
            description = "Koinot tumanligi va yulduzlar charxpalagi ko'rinishidagi yuksak ramka",
            perks = listOf("Kosmik nebula ramkasi", "Yulduzli shaxsiy ID auralari", "1.7x Star bonus"),
            entranceVehicle = "Koinot Kemasi 🛸",
            gradientColors = listOf(0xFF8B5CF6, 0xFF6D28D9),
            borderColorHex = 0xFFA78BFA
        ),
        VipTier(
            level = 10,
            title = "VIP 10 • Koinot Raketasi",
            badgeIcon = "🚀",
            coinPrice = 80000,
            frameTitle = "Giper-Raketa Plazma Ramka",
            description = "Plazma nurli ramka va xonaga giper-raketa bilan kirib kelish",
            perks = listOf("Plazma raketa ramkasi", "🚀 Raketa uchirish animatsiyasi", "1.8x Star bonus"),
            entranceVehicle = "Giper Raketa 🚀",
            gradientColors = listOf(0xFF06B6D4, 0xFF0891B2),
            borderColorHex = 0xFF22D3EE
        ),
        VipTier(
            level = 11,
            title = "VIP 11 • Afsonaviy Hukmdor",
            badgeIcon = "👑🏛️",
            coinPrice = 150000,
            frameTitle = "Afsonaviy Taxt Ramkasi",
            description = "Imperator saroyi ramkasi, butun ilova bo'ylab global e'lon banneri",
            perks = listOf("Imperator taxt ramkasi", "Butun ilova bo'ylab kirish bildirishnomasi", "1.9x Star bonus"),
            entranceVehicle = "Oltin Samolyot ✈️",
            gradientColors = listOf(0xFFF59E0B, 0xFFB45309),
            borderColorHex = 0xFFFBBF24
        ),
        VipTier(
            level = 12,
            title = "VIP 12 • Qahramon Tangri (Supreme Lord)",
            badgeIcon = "⚡🪐",
            coinPrice = 300000,
            frameTitle = "Samoviy Qahramon Tangri Ramkasi",
            description = "Eng oliy daraja: Cheksiz yorqin plazma ramkasi, xona ustidagi doimiy nurlar va 2.0x ko'paytirgich",
            perks = listOf("Oliy Tangri ramkasi", "Doimiy samoviy xona nuri", "2.0x Ko'paytirgich", "Eksklyuziv ovoz effekti"),
            entranceVehicle = "Samoviy Kosmik Meteorit ⚡🪐",
            gradientColors = listOf(0xFFFF007F, 0xFFFFD700, 0xFF7928CA),
            borderColorHex = 0xFFFFD700
        )
    )
}

object SampleVoiceRoomsAndGifts {
    val giftList = listOf(
        VirtualGift("gift_rose", "Atirgul 🌸", "🌹", 10, "ROSE"),
        VirtualGift("gift_heart", "Qalb ❤️", "💖", 50, "ROSE"),
        VirtualGift("gift_ring", "Oltin Uzuk 💍", "💎", 200, "RING"),
        VirtualGift("gift_crown", "Qirolicha Toji 👑", "👑", 500, "CROWN"),
        VirtualGift("gift_car", "Sport Avto 🏎️", "🏎️", 1200, "CAR"),
        VirtualGift("gift_rocket", "Koinot Raketasi 🚀", "🚀", 2500, "ROCKET"),
        VirtualGift("gift_castle", "Qirollik Qasri 🏰", "🏰", 5000, "CASTLE"),
        VirtualGift("gift_galaxy", "Galaktika Toji 🌌", "🌌", 10000, "GALAXY")
    )

    val sampleVoiceRooms = listOf(
        VoicePartyRoom(
            id = "room_1",
            title = "RAYXON SEVDOR 💖",
            hostId = "host_sakinat",
            hostName = "SAKINAT",
            hostPhotoPath = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
            category = "Qalblar Visoli",
            announcementText = "Bu hayotda ko'pchilik uchun jon kuydirasan, dardini o'zingnikidek qabul qilasan. Lekin sening boshingga og'ir kun tushganda, holingni so'rashga ham vaqt topishmadi.",
            roomIdDisplay = "13199329",
            listenerCount = 28,
            speakers = listOf(
                VoiceRoomSpeaker(0, "host_sakinat", "SAKINAT", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500", "Ayol", isHost = true, isSpeaking = false, vipLevel = 12, starPoints = 810915, receivedGiftsCount = 810915),
                VoiceRoomSpeaker(1, "user_sevdor", "SEVDOR", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500", "Erkak", isSpeaking = false, vipLevel = 6, starPoints = 305),
                VoiceRoomSpeaker(3, "user_rayxon", "RAYXON", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500", "Ayol", isSpeaking = false, vipLevel = 4, starPoints = 0)
            )
        )
    )
}

