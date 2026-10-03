package com.example.model

import java.util.UUID

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val gender: String, // "Erkak" or "Ayol"
    val age: Int,
    val viloyat: String,
    val tuman: String,
    val mahalla: String,
    val photoPath: String, // Local file path or drawable name
    val bio: String,
    val username: String = "", // @username takrorlanmas noyob nom
    val telegram: String = "",
    val instagram: String = "",
    val tiktok: String = "",
    val phone: String = "",
    val isVerifiedCamera: Boolean = true,
    val isCurrentUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val birthDate: String = "", // "YYYY-MM-DD" formati
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isPrivate: Boolean = false,
    val isVip: Boolean = true,
    val isMatched: Boolean = false, // Foydalanuvchi o'z juftini topganligi (nomzodlar ro'yxatida ko'rinmaydi)
    val isAdmin: Boolean = false,
    val isRestricted: Boolean = false, // Foydalanuvchi shubhali kontent tufayli vaqtinchalik tekshiruvda cheklanganligi
    val isSpamRestricted: Boolean = false, // Spam cheklovi: video yuklay olmaydi, lekin efir qila oladi va ilovadan chetlatilmaydi
    val restrictionReason: String = "",
    val restrictionTimestamp: Long = 0L,
    val isBanned: Boolean = false, // Qoidabuzarlik tufayli to'liq ban qilinganligi
    val posts: List<CandidatePost> = emptyList()
) {
    /**
     * Admin tekshiruvi: @visoladmin_009 yoki maxsus admin roli
     */
    val isAdminUser: Boolean
        get() = isAdmin || username.trim().lowercase().removePrefix("@") == "visoladmin_009" || id == "admin_009" || id == "system_admin_visol" || fullName.equals("ILOVA ADMINISTRATORI", ignoreCase = true)

    val isAiHakam: Boolean
        get() = id == "system_ai_hakam" || username.trim().lowercase().removePrefix("@") == "ai_hakam_uz"

    val isOfficialAdmin: Boolean
        get() = id == "system_admin_visol" || username.trim().lowercase().removePrefix("@") == "visoladmin_009"

    /**
     * Har bir profil uchun alohida avtomatik beriladigan noyob raqamli ID
     */
    val profileNumber: Long
        get() = (Math.abs(id.hashCode().toLong()) % 9000000L) + 1000000L

    /**
     * Foydalanuvchining to'liq username shakli (@username)
     */
    val displayUsername: String
        get() {
            if (username.isNotBlank()) {
                val clean = username.trim().removePrefix("@")
                return "@$clean"
            }
            if (instagram.isNotBlank()) {
                val clean = instagram.trim().removePrefix("@")
                return "@$clean"
            }
            val baseName = fullName.lowercase()
                .replace(" ", "_")
                .filter { it.isLetterOrDigit() || it == '_' }
            return if (baseName.isNotBlank()) "@$baseName" else "@user_${id.takeLast(4)}"
        }

    /**
     * Foydalanuvchi identifikatori (VS-XXXXXX formati)
     */
    val formattedId: String
        get() = "VS-$profileNumber"

    /**
     * Dinamik va real vaqtda hisoblangan yosh.
     * Agar birthDate mavjud bo'lsa, joriy kundan aniq hisoblab chiqaradi.
     */
    val dynamicAge: Int
        get() {
            if (birthDate.isNotBlank()) {
                val calculated = calculateAgeFromBirthDate(birthDate)
                if (calculated > 0) return calculated
            }
            return age
        }

    companion object {
        fun calculateAgeFromBirthDate(birthDateStr: String): Int {
            return try {
                val parts = birthDateStr.split("-")
                if (parts.size == 3) {
                    val birthYear = parts[0].toInt()
                    val birthMonth = parts[1].toInt()
                    val birthDay = parts[2].toInt()

                    val now = java.util.Calendar.getInstance()
                    val currentYear = now.get(java.util.Calendar.YEAR)
                    val currentMonth = now.get(java.util.Calendar.MONTH) + 1
                    val currentDay = now.get(java.util.Calendar.DAY_OF_MONTH)

                    var calculated = currentYear - birthYear
                    if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                        calculated--
                    }
                    calculated
                } else 0
            } catch (e: Exception) {
                0
            }
        }
    }
}

data class ProfileVisitor(
    val id: String,
    val visitorProfile: UserProfile,
    val visitedAt: Long,
    val formattedTime: String
)

object SampleCandidates {
    val initialList: List<UserProfile> = listOf(
        UserProfile(
            id = "admin_009",
            fullName = "ILOVA ADMINISTRATORI",
            gender = "Erkak",
            age = 28,
            viloyat = "Toshkent shahri",
            tuman = "Mirobod tumani",
            mahalla = "Markaziy Visol MFY",
            photoPath = "avatar_male_1",
            bio = "Visol.uz loyihasining rasmiy ma'muriyati. Barcha savollar, takliflar va xavfsizlik bo'yicha murojaat qilishingiz mumkin. Baxtli oilalar maskani! 👑",
            username = "visoladmin_009",
            telegram = "@visoladmin_009",
            instagram = "@visol_uz",
            tiktok = "@visol.uz",
            phone = "+998 71 200 00 09",
            isVerifiedCamera = true,
            isVip = true,
            isAdmin = true,
            followersCount = 1420
        ),
        UserProfile(
            id = "user_1",
            fullName = "Madinaxon Karimova",
            gender = "Ayol",
            age = 23,
            viloyat = "Toshkent shahri",
            tuman = "Yunusobod tumani",
            mahalla = "Bog'ishamol MFY",
            photoPath = "avatar_female_1",
            bio = "Assalomu alaykum! Oliy ma'lumotli filologman. Kitob o'qish, adabiyot va milliy qadriyatlarni hurmat qilaman. Hayotda vafodor, samimiy va jiddiy niyatli inson bilan do'stlik va oila qurish niyatidaman.",
            username = "madina_karimova",
            telegram = "@madina_uzb",
            instagram = "@madina.karimova",
            tiktok = "@madina_poetry",
            phone = "+998 90 123 45 67",
            isVerifiedCamera = true
        ),
        UserProfile(
            id = "user_2",
            fullName = "Javohirbek Rahimov",
            gender = "Erkak",
            age = 26,
            viloyat = "Samarqand viloyati",
            tuman = "Samarqand shahri",
            mahalla = "Registon MFY",
            photoPath = "avatar_male_1",
            bio = "Dasturchi muhandisman. Sport bilan shug'ullanaman, sayohat qilishni va tarixiy obidalarni yaxshi ko'raman. Milliy urf-odatlarimizni qadrlaydigan, yaxshi xulqli qiz bilan tanishmoqchiman.",
            username = "javohir_sam",
            telegram = "@javohir_dev",
            instagram = "@javohir_sam",
            tiktok = "",
            phone = "+998 93 456 78 90",
            isVerifiedCamera = true
        ),
        UserProfile(
            id = "user_3",
            fullName = "Gulnozaxon To'rayeva",
            gender = "Ayol",
            age = 24,
            viloyat = "Farg'ona viloyati",
            tuman = "Marg'ilon shahri",
            mahalla = "Atlaschilar MFY",
            photoPath = "avatar_female_2",
            bio = "Milliy ipakchilik va dizayn bo'yicha mutaxassisman. Pazandalik, milliy taomlar va shinam xonadon yaratishga qiziqaman. Samimiy do'stlik va mustahkam rishta tarafdoriman.",
            username = "gulnoza_fergana",
            telegram = "@gulnoza_atlas",
            instagram = "@gulnoza_fergana",
            tiktok = "@gulnoza_design",
            phone = "+998 97 789 01 23",
            isVerifiedCamera = true,
            isPrivate = true
        ),
        UserProfile(
            id = "user_4",
            fullName = "Sardorbek Qodirov",
            gender = "Erkak",
            age = 28,
            viloyat = "Buxoro viloyati",
            tuman = "Buxoro shahri",
            mahalla = "Labihovuz MFY",
            photoPath = "avatar_male_2",
            bio = "Iqtisodchi va tadbirkor. Doim o'z ustimda ishlayman. Maqsadim: sof niyatli, vafoli umr yo'ldoshi topish. Xushmuomala va oqila qizlar bilan muloqotga tayyorman.",
            username = "sardor_qodirov",
            telegram = "@sardor_buxoro",
            instagram = "@sardor.qodirov",
            tiktok = "",
            phone = "+998 91 234 56 78",
            isVerifiedCamera = true
        ),
        UserProfile(
            id = "user_5",
            fullName = "Nafisaxon Mirzayeva",
            gender = "Ayol",
            age = 22,
            viloyat = "Andijon viloyati",
            tuman = "Andijon shahri",
            mahalla = "Bobur MFY",
            photoPath = "avatar_female_3",
            bio = "Tibbiyot instituti talabasiman. Bolalarni yaxshi ko'raman. Mehribon, diningizni va milliy qadriyatlarni hurmat qiluvchi mard yigit bilan hayot yo'limni bog'lashni orzu qilaman.",
            username = "nafisa_andijan",
            telegram = "@nafisa_med",
            instagram = "@nafisa.andijan",
            tiktok = "",
            phone = "+998 94 321 65 47",
            isVerifiedCamera = true
        ),
        UserProfile(
            id = "user_6",
            fullName = "Boburjon Yoqubov",
            gender = "Erkak",
            age = 27,
            viloyat = "Toshkent shahri",
            tuman = "Chilonzor tumani",
            mahalla = "Navro'z MFY",
            photoPath = "avatar_male_3",
            bio = "Logistika va avtotransport sohasida ishlayman. Halollik va sadoqat eng muhim fazilat deb bilaman. Bo'sh vaqtlarda shaxmat o'ynayman va tog' sayrlariga chiqaman.",
            username = "bobur_yoqubov",
            telegram = "@bobur_toshkent",
            instagram = "@bobur_yoqubov",
            tiktok = "@bobur_travel",
            phone = "+998 90 999 88 77",
            isVerifiedCamera = true
        ),
        UserProfile(
            id = "user_7",
            fullName = "Shahnozaxon Aliyeva",
            gender = "Ayol",
            age = 21,
            viloyat = "Namangan viloyati",
            tuman = "Namangan shahri",
            mahalla = "Istiqlol MFY",
            photoPath = "avatar_female_4",
            bio = "Ingliz tili o'qituvchisiman. Madaniyatli, oilaparvar va pok niyatli insonlarni qadrlayman. Yangi bilimlarni o'rganish va bolalar tarbiyasi meni ilhomlantiradi.",
            username = "shahnoza_nam",
            telegram = "@shahnoza_nam",
            instagram = "@shahnoza.alieva",
            tiktok = "@shahnoza_teach",
            phone = "+998 93 111 22 33",
            isVerifiedCamera = true
        ),
        UserProfile(
            id = "user_8",
            fullName = "Otabek Mirzayev",
            gender = "Erkak",
            age = 29,
            viloyat = "Qashqadaryo viloyati",
            tuman = "Qarshi shahri",
            mahalla = "Nasaf MFY",
            photoPath = "avatar_male_4",
            bio = "Muhandis-energetik. Bo'sh vaqtlarda kitob mutolaa qilaman va sport bilan shug'ullanaman. Dunyoqarashi keng, odobli va samimiy umr yo'ldoshi qidirmoqdaman.",
            username = "otabek_nasaf",
            telegram = "@otabek_nasaf",
            instagram = "@otabek.mirzayev",
            tiktok = "",
            phone = "+998 97 444 55 66",
            isVerifiedCamera = true
        ),
        UserProfile(
            id = "user_9",
            fullName = "Dilnavoz Ergasheva",
            gender = "Ayol",
            age = 25,
            viloyat = "Xorazm viloyati",
            tuman = "Urganch shahri",
            mahalla = "Al-Xorazmiy MFY",
            photoPath = "avatar_female_5",
            bio = "Iqtisodchi-buxgalter. Milliy xorazm urf-odatlarini sevadigan, samimiy va ochiqko'ngil insonman. Oilani baxtli va totuv qurish tarafdoriman.",
            username = "dilnavoz_xorazm",
            telegram = "@dilnavoz_xorazm",
            instagram = "@dilnavoz.ergasheva",
            tiktok = "",
            phone = "+998 91 777 88 99",
            isVerifiedCamera = true
        ),
        UserProfile(
            id = "user_10",
            fullName = "Shohruxbek Rustamov",
            gender = "Erkak",
            age = 31,
            viloyat = "Navoiy viloyati",
            tuman = "Navoiy shahri",
            mahalla = "Zarafshon MFY",
            photoPath = "avatar_male_5",
            bio = "Moliya sohasida tahlilchi. Halol mehnat, qat'iyat va samimiyat hayotimdagi asosiy qoidalar. Dunyoqarashi keng qiz bilan do'stona va jiddiy suhbat qurish niyatidaman.",
            username = "shohrux_navoiy",
            telegram = "@shohrux_navoiy",
            instagram = "@shohrux.rustamov",
            tiktok = "@shohrux_fin",
            phone = "+998 90 333 44 55",
            isVerifiedCamera = true
        )
    )
}
