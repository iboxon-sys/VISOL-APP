package com.example.ui.screens

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.PhotoCropDialog
import com.example.ui.components.PrivacyPolicyDialog
import com.example.util.ImageModerationService
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.UserRepository
import com.example.model.DistrictInfo
import com.example.model.RegionInfo
import com.example.model.UserProfile
import com.example.model.UzbekistanData
import com.example.ui.components.BiometricFaceVerificationCamera
import com.example.ui.components.RomanticAnimatedBackground
import com.example.ui.components.SearchableMahallaPickerBottomSheet
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.SocialMediaHelper
import com.example.util.TelegramVerificationResult
import com.example.util.PhoneNumberVisualTransformation
import java.util.Calendar
import java.util.UUID
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =========================================================================
// ULTRA-PREMIUM WARM GLASS RANG PALITRASI (iOS 18 & Telegram Mini App)
// =========================================================================
private val VelvetNightBg = Color(0xFF100719)
private val DeepVioletCardBg = Color(0x95200D34)
private val GlassSurfaceBg = Color(0x95230F3B) // Frosted semi-transparent glass
private val GlassSurfaceActive = Color(0xB52F144D)
private val PeachGradientStart = Color(0xFFFF7B54)
private val PinkGradientEnd = Color(0xFFFF4D79)
private val GoldAccent = Color(0xFFFBBF24)
private val GlassBorderSoft = Color(0x77FF8C61) // Distinct border
private val GlassBorderGlow = Color(0xDDFF8C61)
private val TextPureWhite = Color(0xFFFAF5FF)
private val TextLavenderMuted = Color(0xFFE2C4DE)
private val SuccessMint = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    userRepository: UserRepository,
    onRegistrationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current
    val themeMode by userRepository.themeMode.collectAsState()
    val isEffectiveDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isDark
    }

    // Ekran rejimi: "REGISTER" (Ro'yxatdan o'tish) yoki "LOGIN" (Kirish)
    var authMode by remember { mutableStateOf("REGISTER") }

    // Kirish (Login) maydonlari
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoginPasswordVisible by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }

    // Bosqich holati (1: Shaxsiyat & Parol, 2: Manzil & Rasm, 3: Aloqa & Niyat)
    var currentStep by remember { mutableIntStateOf(1) }

    // 1-Bosqich maydonlari
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf("Ayol") }
    var birthDate by remember { mutableStateOf("") }
    var calculatedAge by remember { mutableIntStateOf(0) }

    // Validatsiya belgilari
    var step1Submitted by remember { mutableStateOf(false) }
    var step2Submitted by remember { mutableStateOf(false) }
    var step3Submitted by remember { mutableStateOf(false) }

    // 2-Bosqich: Manzil & Face ID / Galereya Rasmi
    var selectedRegion by remember { mutableStateOf<RegionInfo?>(null) }
    var selectedDistrict by remember { mutableStateOf<DistrictInfo?>(null) }
    var selectedMahalla by remember { mutableStateOf("") }
    var customMahalla by remember { mutableStateOf("") }

    var showRegionSheet by remember { mutableStateOf(false) }
    var showDistrictSheet by remember { mutableStateOf(false) }
    var showMahallaSheet by remember { mutableStateOf(false) }
    var regionSearchQuery by remember { mutableStateOf("") }
    var districtSearchQuery by remember { mutableStateOf("") }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFaceVerified by remember { mutableStateOf(false) }
    var showBiometricCamera by remember { mutableStateOf(false) }
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var pendingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var pendingPassword by remember { mutableStateOf<String?>(null) }

    // 3-Bosqich: Aloqa va Hayotiy Niyatlar
    var phone by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var tiktok by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var telegramCheckState by remember { mutableStateOf<TelegramVerificationResult>(TelegramVerificationResult.Idle) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Telegram tekshirish
    LaunchedEffect(telegram) {
        val clean = SocialMediaHelper.cleanUsername(telegram)
        if (clean.isBlank()) {
            telegramCheckState = TelegramVerificationResult.Idle
            return@LaunchedEffect
        }
        if (clean.length < 5) {
            telegramCheckState = TelegramVerificationResult.InvalidFormat("Username kamida 5 ta belgi bo'lishi kerak")
            return@LaunchedEffect
        }
        telegramCheckState = TelegramVerificationResult.Checking
        delay(350)
        telegramCheckState = SocialMediaHelper.verifyTelegramLink(clean)
    }

    // Kamera ruxsati
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showBiometricCamera = true
        } else {
            Toast.makeText(context, "Face ID biometriyasi uchun kamera ruxsati kerak!", Toast.LENGTH_LONG).show()
        }
    }

    // Galereyadan rasm tanlash (Face ID shart bo'lmagan qulay usul)
    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            try {
                // Safely decode with inSampleSize to prevent OOM / render crashes
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                val reqSize = 1080
                var sampleSize = 1
                while ((options.outWidth / sampleSize) > reqSize || (options.outHeight / sampleSize) > reqSize) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                if (decodedBitmap != null && !decodedBitmap.isRecycled) {
                    pendingCropBitmap = decodedBitmap
                    showCropDialog = true
                } else {
                    Toast.makeText(context, "Rasm yuklashda xatolik: Rasm yaroqsiz", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Rasmni yuklashda xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Validatsiyalar
    val isReservedName = userRepository.isReservedFullName(fullName)
    val isFullNameValid = fullName.trim().length >= 3 && !isReservedName
    val hasSpacesInUsername = username.any { it.isWhitespace() }
    val cleanUsername = username.filter { !it.isWhitespace() }.lowercase().removePrefix("@")
    val isReservedUser = userRepository.isReservedUsername(cleanUsername)
    val isUsernameFormatValid = cleanUsername.length in 3..30 && 
        cleanUsername.all { (it in 'a'..'z') || (it in '0'..'9') || it == '_' } &&
        !hasSpacesInUsername && !isReservedUser
    val isUsernameTaken = isUsernameFormatValid && userRepository.isUsernameTaken(cleanUsername)
    val isUsernameValid = isUsernameFormatValid && !isUsernameTaken
    val isPasswordValid = password.trim().length >= 4
    val isAgeValid = calculatedAge in 18..100
    val isUnderage = calculatedAge in 1..17
    val isStep1Valid = isFullNameValid && isUsernameValid && isPasswordValid && isAgeValid && birthDate.isNotBlank()

    val isRegionValid = selectedRegion != null
    val isDistrictValid = selectedDistrict != null
    val isMahallaValid = selectedMahalla.isNotBlank() || customMahalla.isNotBlank()
    val isFaceValid = isFaceVerified && capturedBitmap != null
    val isStep2Valid = isRegionValid && isDistrictValid && isMahallaValid && isFaceValid

    val hasSocialContact = telegram.trim().isNotBlank() || instagram.trim().isNotBlank() || tiktok.trim().isNotBlank()
    val cleanPhoneDigits = phone.filter { it.isDigit() }.let { if (it.startsWith("998")) it.drop(3) else it }.take(9)
    val isPhoneValid = cleanPhoneDigits.length == 9
    val isBioValid = bio.trim().length >= 5
    val isStep3Valid = isPhoneValid && hasSocialContact && isBioValid

    // DatePicker (Kamida 18 yosh chegarasi)
    val openDatePicker = {
        val calendar = Calendar.getInstance()
        val defaultYear = calendar.get(Calendar.YEAR) - 22
        val defaultMonth = calendar.get(Calendar.MONTH)
        val defaultDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val monthStr = (selectedMonth + 1).toString().padStart(2, '0')
                val dayStr = selectedDay.toString().padStart(2, '0')
                val formattedDate = "$selectedYear-$monthStr-$dayStr"
                birthDate = formattedDate
                val age = UserProfile.calculateAgeFromBirthDate(formattedDate)
                calculatedAge = age
                if (age < 18) {
                    errorMessage = "⚠️ Yosh chegarasi: Visol ilovasidan faqat 18 yoshga to'lgan fuqarolar foydalanishi mumkin! 18 yoshdan kichik shaxslarning kirishi cheklangan."
                } else {
                    errorMessage = null
                }
            },
            defaultYear,
            defaultMonth,
            defaultDay
        )
        // Maksimal ruxsat etilgan sana: aynan bugundan 18 yil oldin
        val maxCalendar = Calendar.getInstance().apply {
            add(Calendar.YEAR, -18)
        }
        datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis
        datePickerDialog.show()
    }

    // Tezkor Demo To'ldirish (Tez va qulay test qilish uchun)
    val autofillDemoData = {
        fullName = if (gender == "Ayol") "Zuhra Karimova" else "Farrux Aliyev"
        username = if (gender == "Ayol") "zuhra_k" else "farrux_a"
        password = "password123"
        birthDate = "2001-06-15"
        calculatedAge = 24
        val reg = UzbekistanData.regions.first()
        selectedRegion = reg
        val dist = reg.districts.first()
        selectedDistrict = dist
        selectedMahalla = dist.mahallas.firstOrNull() ?: "Istiqlol MFY"
        isFaceVerified = true
        phone = "901234567"
        telegram = "@visol_baxti"
        instagram = "@visol_uz"
        bio = "Oliy ma'lumotli, milliy qadriyatlarga sodiq, baxtli oila qurish niyatidaman 🌸"
        Toast.makeText(context, "Namunaviy ma'lumotlar to'ldirildi ✨", Toast.LENGTH_SHORT).show()
    }

    // Tezkor Ro'yxatdan O'tish Funksiyasi (1-Tegishda)
    val performQuickRegistration: () -> Unit = {
        if (fullName.trim().length < 2) {
            errorMessage = "Ism va familiyangizni kiriting (masalan: Malika Raximova)"
        } else if (password.trim().isBlank()) {
            errorMessage = "Tizimga kirish uchun parol belgilang"
        } else if (!isSubmitting) {
            errorMessage = null
            isSubmitting = true
            coroutineScope.launch {
                try {
                    val finalFullName = fullName.trim()
                    val genUser = if (cleanUsername.isNotBlank()) cleanUsername
                    else "${finalFullName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }}_${(100..999).random()}"

                    val photoPath = capturedBitmap?.let { bitmap ->
                        Log.d("RegistrationScreen", "Saving photo, width: ${bitmap.width}, height: ${bitmap.height}")
                        userRepository.saveCameraBitmap(bitmap)
                    } ?: (if (gender == "Ayol") "avatar_female_1" else "avatar_male_1")
                    Log.d("RegistrationScreen", "Photo path: $photoPath")

                    val finalPhone = if (cleanPhoneDigits.isNotBlank()) "+998 $cleanPhoneDigits" else ""
                    val finalRegion = selectedRegion?.name ?: "Toshkent shahri"
                    val finalDistrict = selectedDistrict?.name ?: (selectedRegion?.districts?.firstOrNull()?.name ?: "Yakkasaroy tumani")
                    val finalMahalla = if (customMahalla.isNotBlank()) customMahalla.trim()
                    else (selectedMahalla.ifBlank { selectedDistrict?.mahallas?.firstOrNull() ?: "Tinchlik MFY" })

                    val cleanTg = SocialMediaHelper.cleanUsername(telegram)
                    val cleanInsta = SocialMediaHelper.cleanUsername(instagram)
                    val finalBio = if (bio.isNotBlank()) bio.trim() else "Visol loyihasining samimiy a'zosi 🌸"
                    val finalBirthDate = if (birthDate.isNotBlank()) birthDate else "2002-05-15"
                    val finalAge = if (calculatedAge in 16..90) calculatedAge else 23

                    val newProfile = UserProfile(
                        id = UUID.randomUUID().toString(),
                        fullName = finalFullName,
                        gender = gender,
                        age = finalAge,
                        viloyat = finalRegion,
                        tuman = finalDistrict,
                        mahalla = finalMahalla,
                        photoPath = photoPath,
                        bio = finalBio,
                        username = genUser,
                        telegram = if (cleanTg.isNotEmpty()) "@$cleanTg" else "",
                        instagram = if (cleanInsta.isNotEmpty()) "@$cleanInsta" else "",
                        tiktok = "",
                        phone = finalPhone,
                        isVerifiedCamera = true,
                        isCurrentUser = true,
                        birthDate = finalBirthDate
                    )

                    pendingProfile = newProfile
                    pendingPassword = password
                    showPrivacyPolicyDialog = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSubmitting = false
                }
            }
        }
    }

    // =========================================================================
    // ASOSIY UI EKRANI: MOTION GLASS VA AMBIENT FON
    // =========================================================================
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Romantik Animatsiyali Fon (Sevishganlar arg'imchoqda, musaffo osmon va bahoriy gullar)
        RomanticAnimatedBackground(isDarkTheme = isEffectiveDark)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                RegistrationTopHeader(
                    currentStep = currentStep,
                    isEffectiveDark = isEffectiveDark,
                    onToggleTheme = { userRepository.toggleThemeMode() },
                    onStepClick = { currentStep = it },
                    onAutofillClick = {
                        autofillDemoData()
                        performQuickRegistration()
                    }
                )
            },
            bottomBar = {
                if (authMode == "REGISTER") {
                    RegistrationBottomBar(
                        currentStep = currentStep,
                        isSubmitting = isSubmitting,
                        onPrevClick = { if (currentStep > 1) currentStep-- },
                        onNextClick = {
                            when (currentStep) {
                                1 -> {
                                    step1Submitted = true
                                    if (birthDate.isBlank()) {
                                        errorMessage = "Iltimos, tug'ilgan sanangizni tanlang"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    } else if (calculatedAge < 18) {
                                        errorMessage = "⚠️ Visol ilovasi faqat 18 yoshga to'lgan fuqarolar uchun! 18 yoshdan kichiklarning kirishi bloklangan."
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    } else if (!isFullNameValid) {
                                        errorMessage = "Ism va familiyangizni to'liq kiriting"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    } else if (!isUsernameValid) {
                                        errorMessage = if (isUsernameTaken) "@$cleanUsername allaqachon band" else "Username kamida 3 ta belgidan iborat bo'lsin"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    } else if (!isPasswordValid) {
                                        errorMessage = "Parol kamida 4 ta belgidan iborat bo'lsin"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = null
                                        currentStep = 2
                                    }
                                }
                                2 -> {
                                    step2Submitted = true
                                    if (!isRegionValid) {
                                        errorMessage = "Iltimos, viloyatingizni tanlang"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    } else if (!isDistrictValid) {
                                        errorMessage = "Iltimos, tumaningizni tanlang"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    } else if (!isFaceValid) {
                                        errorMessage = "Iltimos, profil suratingizni yuklang yoki Face ID tasdiqlang"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = null
                                        currentStep = 3
                                    }
                                }
                                3 -> {
                                    step3Submitted = true
                                    performQuickRegistration()
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Auth rejimi almashtirgichi (Ro'yxatdan o'tish | Tizimga kirish)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isEffectiveDark) Color(0x22FFFFFF) else Color(0x12000000),
                    border = BorderStroke(1.dp, if (isEffectiveDark) GlassBorderSoft else Color(0x22FF7B54)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (authMode == "REGISTER") Brush.horizontalGradient(listOf(PeachGradientStart, PinkGradientEnd)) else SolidColor(Color.Transparent))
                                .clickable { authMode = "REGISTER"; errorMessage = null }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ro'yxatdan o'tish",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (authMode == "REGISTER") Color.White else (if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (authMode == "LOGIN") Brush.horizontalGradient(listOf(PeachGradientStart, PinkGradientEnd)) else SolidColor(Color.Transparent))
                                .clickable { authMode = "LOGIN"; errorMessage = null }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tizimga kirish",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (authMode == "LOGIN") Color.White else (if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B))
                            )
                        }
                    }
                }

                // Xatolik xabari (Animated Glass Pill)
                AnimatedVisibility(
                    visible = errorMessage != null || loginErrorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val msg = errorMessage ?: loginErrorMessage
                    if (msg != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x3DFF4D79),
                            border = BorderStroke(1.dp, PinkGradientEnd.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = PinkGradientEnd,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isEffectiveDark) TextPureWhite else Color(0xFF881337),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                // Frosted Glassmorphic Elevated Card Surface for Auth/Registration Form
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isEffectiveDark) Color(0x801A0B2E) else Color(0xB8FFFFFF),
                    border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(PeachGradientStart.copy(alpha = 0.85f), PinkGradientEnd.copy(alpha = 0.75f), GoldAccent.copy(alpha = 0.85f)))),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        if (authMode == "LOGIN") {
                            // =========================================================
                            // TIZIMGA KIRISH (LOGIN) KARTASI
                            // =========================================================
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Profilga Kirish 🔑",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isEffectiveDark) TextPureWhite else Color(0xFF1E1022)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Oldin ro'yxatdan o'tgan bo'lsangiz, @username yoki telefoningiz hamda parolingiz bilan kiring.",
                                    fontSize = 12.sp,
                                    color = if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B),
                                    lineHeight = 17.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Administrator uchun tezkor kirish tugmasi
                                Surface(
                                    onClick = {
                                        loginIdentifier = "@visoladmin_009"
                                        loginPassword = "20090828_Zuhriddin"
                                        isLoggingIn = true
                                        loginErrorMessage = null
                                        coroutineScope.launch {
                                            val res = userRepository.loginUser("@visoladmin_009", "20090828_Zuhriddin")
                                            isLoggingIn = false
                                            if (res.isSuccess) {
                                                val u = res.getOrThrow()
                                                Toast.makeText(context, "Hush kelibsiz, Administrator! 👑", Toast.LENGTH_LONG).show()
                                                onRegistrationSuccess()
                                            } else {
                                                loginErrorMessage = res.exceptionOrNull()?.message
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0x35FBBF24),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "👑", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Administrator sifatida tezkor kirish",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                            Text(
                                                text = "@visoladmin_009 • Butun umrlik VIP & Barcha ma'lumotlar",
                                                fontSize = 10.sp,
                                                color = if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Username yoki Telefon raqami
                                OutlinedTextField(
                                    value = loginIdentifier,
                                    onValueChange = { loginIdentifier = it; loginErrorMessage = null },
                                    label = { Text("Username (@user) yoki Telefon raqam") },
                                    placeholder = { Text("@visoladmin_009 yoki +998901234567") },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PeachGradientStart)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = warmOutlinedColors(isEffectiveDark),
                                    modifier = Modifier.fillMaxWidth().testTag("input_login_identifier")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Parol
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = { loginPassword = it; loginErrorMessage = null },
                                    label = { Text("Parol") },
                                    placeholder = { Text("••••••••") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = PeachGradientStart)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isLoginPasswordVisible = !isLoginPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isLoginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = PeachGradientStart
                                            )
                                        }
                                    },
                                    visualTransformation = if (isLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = warmOutlinedColors(isEffectiveDark),
                                    modifier = Modifier.fillMaxWidth().testTag("input_login_password")
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Kirish tugmasi
                                Button(
                                    onClick = {
                                        if (loginIdentifier.trim().isBlank()) {
                                            loginErrorMessage = "Username yoki telefoningizni kiriting"
                                            return@Button
                                        }
                                        if (loginPassword.trim().isBlank()) {
                                            loginErrorMessage = "Parolingizni kiriting"
                                            return@Button
                                        }
                                        isLoggingIn = true
                                        loginErrorMessage = null
                                        coroutineScope.launch {
                                            val res = userRepository.loginUser(loginIdentifier, loginPassword)
                                            isLoggingIn = false
                                            if (res.isSuccess) {
                                                val loggedUser = res.getOrThrow()
                                                Toast.makeText(context, "Hush kelibsiz, ${loggedUser.fullName}! ✨", Toast.LENGTH_SHORT).show()
                                                onRegistrationSuccess()
                                            } else {
                                                loginErrorMessage = res.exceptionOrNull()?.message ?: "Foydalanuvchi yoki parol noto'g'ri"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("btn_do_login")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.horizontalGradient(listOf(PeachGradientStart, PinkGradientEnd))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoggingIn) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Tizimga kirish", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Google Sign-In Option
                                var showGoogleSignInDialog by remember { mutableStateOf(false) }
                                var googleEmail by remember { mutableStateOf("") }
                                var googleName by remember { mutableStateOf("") }
                                var googleError by remember { mutableStateOf<String?>(null) }

                                if (showGoogleSignInDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showGoogleSignInDialog = false; googleError = null },
                                        title = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountCircle,
                                                    contentDescription = null,
                                                    tint = PeachGradientStart,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Google orqali kirish", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text(
                                                    text = "Google akkauntingiz orqali xavfsiz tizimga kirish uchun quyidagi ma'lumotlarni kiriting:",
                                                    fontSize = 13.sp,
                                                    color = if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B)
                                                )
                                                
                                                OutlinedTextField(
                                                    value = googleName,
                                                    onValueChange = { googleName = it; googleError = null },
                                                    label = { Text("To'liq ism-familyangiz") },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                OutlinedTextField(
                                                    value = googleEmail,
                                                    onValueChange = { googleEmail = it; googleError = null },
                                                    label = { Text("Google E-mail (Gmail)") },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                if (googleError != null) {
                                                    Text(googleError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    val emailTrim = googleEmail.trim()
                                                    val nameTrim = googleName.trim()
                                                    if (emailTrim.isBlank() || !emailTrim.contains("@")) {
                                                        googleError = "Iltimos, haqiqiy Gmail pochtangizni kiriting"
                                                    } else if (nameTrim.isBlank()) {
                                                        googleError = "Iltimos, ism-familyangizni kiriting"
                                                     } else {
                                                         showGoogleSignInDialog = false
                                                         googleError = null
                                                         // Call UserRepository's loginWithGoogle
                                                         val res = userRepository.loginWithGoogle(emailTrim, nameTrim)
                                                         if (res.isSuccess) {
                                                             onRegistrationSuccess()
                                                         } else {
                                                             loginErrorMessage = res.exceptionOrNull()?.message ?: "Tizimga kirishda xatolik yuz berdi"
                                                         }
                                                     }
                                                 },
                                                 colors = ButtonDefaults.buttonColors(containerColor = PeachGradientStart)
                                             ) {
                                                 Text("Kirish", color = Color.White)
                                             }
                                         },
                                         dismissButton = {
                                             TextButton(onClick = { showGoogleSignInDialog = false; googleError = null }) {
                                                 Text("Bekor qilish")
                                             }
                                         }
                                     )
                                 }

                                 // Elegant Google Sign-In Button
                                 OutlinedButton(
                                     onClick = { showGoogleSignInDialog = true },
                                     shape = RoundedCornerShape(16.dp),
                                     border = BorderStroke(1.dp, if (isEffectiveDark) Color(0x3DFFFFFF) else Color(0x1F000000)),
                                     colors = ButtonDefaults.outlinedButtonColors(
                                         containerColor = if (isEffectiveDark) Color(0x1AFFFFFF) else Color(0x0A000000)
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
                                             imageVector = Icons.Default.AccountCircle,
                                             contentDescription = "Google Icon",
                                             tint = PeachGradientStart,
                                             modifier = Modifier.size(20.dp)
                                         )
                                         Spacer(modifier = Modifier.width(10.dp))
                                         Text(
                                             text = "Google orqali kirish",
                                             color = if (isEffectiveDark) Color.White else Color(0xFF334155),
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 14.sp
                                         )
                                     }
                                 }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Ro'yxatdan o'tishga havola
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Profilingiz yo'qmi? ",
                                        fontSize = 13.sp,
                                        color = if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Ro'yxatdan o'tish",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PeachGradientStart,
                                        modifier = Modifier.clickable { authMode = "REGISTER" }
                                    )
                                }
                            }
                        } else {
                            // =========================================================
                            // 3 TА KATEGORIYALI WIZARD RO'YXATDAN O'TISH (STEP 1, 2, 3)
                            // =========================================================
                            AnimatedContent(
                                targetState = currentStep,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                                    } else {
                                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                                    }
                                },
                                label = "step_wizard_transition"
                            ) { step ->
                                when (step) {
                                    1 -> StepOnePersonalContent(
                                        fullName = fullName,
                                        onFullNameChange = { fullName = it; errorMessage = null },
                                        username = username,
                                        onUsernameChange = { input ->
                                            val noSpaces = input.filter { !it.isWhitespace() }
                                            if (input.any { it.isWhitespace() }) {
                                                Toast.makeText(context, "Username'da bo'sh joy (space) taqiqlanadi!", Toast.LENGTH_SHORT).show()
                                            }
                                            username = noSpaces
                                            errorMessage = null
                                        },
                                        password = password,
                                        onPasswordChange = { password = it; errorMessage = null },
                                        isPasswordValid = isPasswordValid,
                                        isPasswordVisible = isPasswordVisible,
                                        onTogglePasswordVisible = { isPasswordVisible = !isPasswordVisible },
                                        gender = gender,
                                        onGenderChange = { gender = it; errorMessage = null },
                                        birthDate = birthDate,
                                        calculatedAge = calculatedAge,
                                        onSelectBirthDate = openDatePicker,
                                        showErrors = step1Submitted,
                                        isFullNameValid = isFullNameValid,
                                        isUsernameValid = isUsernameValid,
                                        isUsernameTaken = isUsernameTaken,
                                        cleanUsername = cleanUsername,
                                        isEffectiveDark = isEffectiveDark
                                    )
                                    2 -> StepTwoLocationAndFaceContent(
                                        selectedRegion = selectedRegion,
                                        selectedDistrict = selectedDistrict,
                                        selectedMahalla = selectedMahalla,
                                        customMahalla = customMahalla,
                                        onOpenRegionSheet = { showRegionSheet = true },
                                        onOpenDistrictSheet = { showDistrictSheet = true },
                                        onOpenMahallaSheet = { showMahallaSheet = true },
                                        capturedBitmap = capturedBitmap,
                                        isFaceVerified = isFaceVerified,
                                        onTriggerCamera = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                        onTriggerGallery = {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_PICK,
                                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                            )
                                            galleryPhotoLauncher.launch(intent)
                                        },
                                        showErrors = step2Submitted
                                    )
                                    3 -> StepThreeContactAndBioContent(
                                        isEffectiveDark = isEffectiveDark,
                                        phone = phone,
                                        onPhoneChange = { phone = it },
                                        telegram = telegram,
                                        onTelegramChange = { telegram = it },
                                        telegramCheckState = telegramCheckState,
                                        instagram = instagram,
                                        onInstagramChange = { instagram = it },
                                        tiktok = tiktok,
                                        onTiktokChange = { tiktok = it },
                                        bio = bio,
                                        onBioChange = { bio = it },
                                        onAddBioTag = { tag -> bio = if (bio.isBlank()) tag else "$bio • $tag" },
                                        showErrors = step3Submitted,
                                        isPhoneValid = isPhoneValid,
                                        isBioValid = isBioValid
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // =====================================================================
        // MODAL SHEETS VA CAMERA DIALOG
        // =====================================================================

        // Viloyat Tanlash Sheet
        if (showRegionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRegionSheet = false },
                containerColor = Color(0xFF1B1124),
                contentColor = TextPureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Hududni tanlang 📍",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPureWhite
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = regionSearchQuery,
                        onValueChange = { regionSearchQuery = it },
                        placeholder = { Text("Viloyat qidirish...", color = TextLavenderMuted.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PeachGradientStart) },
                        shape = RoundedCornerShape(14.dp),
                        colors = warmOutlinedColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val filteredRegions = UzbekistanData.regions.filter {
                        it.name.contains(regionSearchQuery, ignoreCase = true)
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(filteredRegions) { region ->
                            Surface(
                                onClick = {
                                    selectedRegion = region
                                    selectedDistrict = null
                                    selectedMahalla = ""
                                    customMahalla = ""
                                    showRegionSheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedRegion?.name == region.name) Color(0x35FF4D79) else Color(0x0EFFFFFF),
                                border = BorderStroke(1.dp, if (selectedRegion?.name == region.name) PeachGradientStart else GlassBorderSoft),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text(
                                    text = region.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedRegion?.name == region.name) FontWeight.Bold else FontWeight.Medium,
                                    color = TextPureWhite,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tuman Tanlash Sheet
        if (showDistrictSheet && selectedRegion != null) {
            val allDistricts = selectedRegion!!.districts
            val filteredDistricts = allDistricts.filter { it.name.contains(districtSearchQuery, ignoreCase = true) }

            ModalBottomSheet(
                onDismissRequest = { showDistrictSheet = false },
                containerColor = Color(0xFF1B1124),
                contentColor = TextPureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "${selectedRegion!!.name} tumanlari",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPureWhite
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = districtSearchQuery,
                        onValueChange = { districtSearchQuery = it },
                        placeholder = { Text("Tuman qidirish...", color = TextLavenderMuted.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PeachGradientStart) },
                        shape = RoundedCornerShape(14.dp),
                        colors = warmOutlinedColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(filteredDistricts) { district ->
                            Surface(
                                onClick = {
                                    selectedDistrict = district
                                    selectedMahalla = ""
                                    customMahalla = ""
                                    showDistrictSheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedDistrict?.name == district.name) Color(0x35FF4D79) else Color(0x0EFFFFFF),
                                border = BorderStroke(1.dp, if (selectedDistrict?.name == district.name) PeachGradientStart else GlassBorderSoft),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text(
                                    text = district.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedDistrict?.name == district.name) FontWeight.Bold else FontWeight.Medium,
                                    color = TextPureWhite,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mahalla Tanlash Sheet
        if (showMahallaSheet && selectedDistrict != null) {
            SearchableMahallaPickerBottomSheet(
                regionName = selectedRegion?.name,
                districtName = selectedDistrict?.name,
                selectedMahalla = selectedMahalla,
                onMahallaSelected = { mahallaName ->
                    selectedMahalla = mahallaName
                    customMahalla = ""
                    showMahallaSheet = false
                },
                onDismiss = { showMahallaSheet = false }
            )
        }

        // Biometrik Face ID Kamera Dialog
        if (showBiometricCamera) {
            Dialog(
                onDismissRequest = { showBiometricCamera = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center
                ) {
                    BiometricFaceVerificationCamera(
                        onVerificationSuccess = { squareBitmap ->
                            pendingCropBitmap = squareBitmap
                            showBiometricCamera = false
                            showCropDialog = true
                        },
                        onCancel = { showBiometricCamera = false }
                    )
                }
            }
        }

        // Rasmni kesish va tahrirlash dialogi (Photo Crop Dialog)
        if (showCropDialog && pendingCropBitmap != null) {
            PhotoCropDialog(
                sourceBitmap = pendingCropBitmap!!,
                isDark = isEffectiveDark,
                onDismiss = {
                    showCropDialog = false
                    pendingCropBitmap = null
                },
                onCropConfirmed = { croppedBmp: Bitmap ->
                    coroutineScope.launch {
                        val moderation = ImageModerationService.checkBitmapSafety(context, croppedBmp)
                        if (!moderation.isApproved) {
                            val msg = moderation.warningMessage ?: "🚫 Xavfsizlik filtri: Suratda noo'rin yoki ochiq-sochiq kontent aniqlandi!"
                            errorMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            showCropDialog = false
                            pendingCropBitmap = null
                            capturedBitmap = null
                            isFaceVerified = false
                        } else {
                            capturedBitmap = croppedBmp
                            isFaceVerified = true
                            showCropDialog = false
                            pendingCropBitmap = null
                            errorMessage = null
                            Toast.makeText(context, "Surat xavfsizlik tekshiruvidan o'tdi va saqlandi ✨", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // Maxfiylik Siyosati va Foydalanish Qoidalari (Scroll lock to accept)
        if (showPrivacyPolicyDialog) {
            PrivacyPolicyDialog(
                isDark = isEffectiveDark,
                onAccept = {
                    showPrivacyPolicyDialog = false
                    val profile = pendingProfile
                    val pwd = pendingPassword
                    if (profile != null && pwd != null) {
                        try {
                            userRepository.registerUserWithPassword(profile, pwd, context)
                            Toast.makeText(context, "Hush kelibsiz, ${profile.fullName}! ✨", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ro'yxatdan o'tishda xatolik: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    onRegistrationSuccess()
                }
            )
        }
    }
}

// =========================================================================
// 1-BOSQICH: SHAXSIYAT VA KELIB CHIQISH
// =========================================================================
@Composable
private fun StepOnePersonalContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordValid: Boolean,
    isPasswordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    birthDate: String,
    calculatedAge: Int,
    onSelectBirthDate: () -> Unit,
    showErrors: Boolean,
    isFullNameValid: Boolean,
    isUsernameValid: Boolean,
    isUsernameTaken: Boolean,
    cleanUsername: String,
    isEffectiveDark: Boolean = true
) {
    val headingColor = if (isEffectiveDark) TextPureWhite else Color(0xFF1E1022)
    val mutedColor = if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Hero Heading
        Text(
            text = "Siz bilan tanishamiz 🌸",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = headingColor,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "O'zingiz haqingizda to'g'ri ma'lumot berish — mustahkam oilaning ilk kalitidir.",
            fontSize = 12.sp,
            color = mutedColor,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Katta, Nufuzli O'zbekona Jins Tanlash Kursor Kartochkalari (🧕 Kelin & 🤵 Kuyov)
        Text(
            text = "Jinsingizni tanlang *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = headingColor
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LuxuryGenderCard(
                title = "Ayol",
                roleTitle = "Kelin nomzodi",
                emoji = "🧕",
                icon = Icons.Default.Female,
                isSelected = gender == "Ayol",
                onClick = { onGenderChange("Ayol") },
                modifier = Modifier.weight(1f)
            )
            LuxuryGenderCard(
                title = "Erkak",
                roleTitle = "Kuyov nomzodi",
                emoji = "🤵",
                icon = Icons.Default.Male,
                isSelected = gender == "Erkak",
                onClick = { onGenderChange("Erkak") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ism va Familiya
        val nameHasError = showErrors && !isFullNameValid
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Ism va Familiyangiz *") },
            placeholder = { Text("Masalan: Jasur Aliyev", color = mutedColor.copy(alpha = 0.5f)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (nameHasError) PinkGradientEnd else PeachGradientStart
                )
            },
            trailingIcon = {
                if (isFullNameValid) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessMint, modifier = Modifier.size(20.dp))
                }
            },
            isError = nameHasError,
            supportingText = if (nameHasError) {
                {
                    val isReserved = fullName.lowercase().let { it.contains("admin") || it.contains("visol") || it.contains("hakam") }
                    if (isReserved) {
                        Text("Rasmiy tizim nomlaridan foydalanish taqiqlangan!", color = PinkGradientEnd, fontSize = 11.sp)
                    } else {
                        Text("Ismingiz kamida 3 ta harfdan iborat bo'lsin", color = PinkGradientEnd, fontSize = 11.sp)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = warmOutlinedColors(isEffectiveDark),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_fullname")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // @Username (Noyob Identifikator)
        val userHasError = showErrors && !isUsernameValid
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Noyob identifikator (@username) *") },
            prefix = { Text("@", fontWeight = FontWeight.Bold, color = PeachGradientStart) },
            placeholder = { Text("jasur_aliyev", color = mutedColor.copy(alpha = 0.5f)) },
            leadingIcon = {
                Icon(
                    Icons.Default.AlternateEmail,
                    contentDescription = null,
                    tint = if (userHasError) PinkGradientEnd else PeachGradientStart
                )
            },
            trailingIcon = {
                if (isUsernameValid && cleanUsername.isNotBlank()) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Mos", tint = SuccessMint, modifier = Modifier.size(20.dp))
                }
            },
            isError = userHasError,
            supportingText = {
                val hasSpaces = username.any { it.isWhitespace() }
                val isReserved = cleanUsername.let { it.contains("admin") || it.contains("visol") || it.contains("hakam") }
                val hasInvalidChars = cleanUsername.isNotBlank() && !cleanUsername.all { (it in 'a'..'z') || (it in '0'..'9') || it == '_' }
                when {
                    hasSpaces -> Text("Username'da bo'sh joy (space) taqiqlanadi!", color = PinkGradientEnd, fontSize = 11.sp)
                    isReserved -> Text("Ushbu username rasmiy ma'muriyat tomonidan band!", color = PinkGradientEnd, fontSize = 11.sp)
                    hasInvalidChars -> Text("Faqat lotin harflari, raqamlar va '_' mumkin", color = PinkGradientEnd, fontSize = 11.sp)
                    userHasError && cleanUsername.length < 3 -> Text("Kamida 3 ta lotin belgisi bo'lsin", color = PinkGradientEnd, fontSize = 11.sp)
                    isUsernameTaken -> Text("Ushbu username band! Boshqasini tanlang.", color = PinkGradientEnd, fontSize = 11.sp)
                    isUsernameValid && cleanUsername.isNotBlank() -> Text("✓ Ushbu nom sizga mos keladi", color = SuccessMint, fontSize = 11.sp)
                    else -> null
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = warmOutlinedColors(isEffectiveDark),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_username")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Parol (Keyingi kirish uchun kalit)
        val pwdHasError = showErrors && !isPasswordValid
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Parol (Kamida 4 ta belgi) *") },
            placeholder = { Text("••••••••", color = mutedColor.copy(alpha = 0.5f)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (pwdHasError) PinkGradientEnd else PeachGradientStart
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Yashirish" else "Ko'rsatish",
                        tint = if (pwdHasError) PinkGradientEnd else PeachGradientStart
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = pwdHasError,
            supportingText = if (pwdHasError) {
                { Text("Parol kamida 4 ta belgidan iborat bo'lishi shart", color = PinkGradientEnd, fontSize = 11.sp) }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = warmOutlinedColors(isEffectiveDark),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_password")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tug'ilgan Sana & Yosh
        val ageHasError = showErrors && (birthDate.isBlank() || calculatedAge !in 7..100)
        Surface(
            onClick = onSelectBirthDate,
            shape = RoundedCornerShape(16.dp),
            color = GlassSurfaceBg,
            border = BorderStroke(1.dp, if (ageHasError) PinkGradientEnd else if (birthDate.isNotBlank()) PeachGradientStart else GlassBorderSoft),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_select_birth_date")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (birthDate.isNotBlank()) Color(0x35FF7B54) else Color(0x14FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (ageHasError) PinkGradientEnd else PeachGradientStart,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tug'ilgan sana va yosh *",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (ageHasError) PinkGradientEnd else TextLavenderMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (birthDate.isNotBlank()) "$birthDate • $calculatedAge yoshda" else "Sanani tanlash uchun bosing",
                        fontSize = 14.sp,
                        fontWeight = if (birthDate.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                        color = if (birthDate.isNotBlank()) TextPureWhite else TextLavenderMuted.copy(alpha = 0.6f)
                    )
                }
                if (calculatedAge in 18..100) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x3010B981)
                    ) {
                        Text(
                            text = "18+ Tasdiqlandi ✓",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessMint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (calculatedAge in 1..17) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x35EF4444)
                    ) {
                        Text(
                            text = "< 18 yosh taqiqlangan ❌",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PeachGradientStart)
                }
            }
        }

        if (calculatedAge in 1..17) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x35EF4444),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "⚠️ Visol ilovasi faqat 18 yoshga to'lgan fuqarolar uchun! 18 yoshga to'lmagan shaxslarning ro'yxatdan o'tishi va ilovadan foydalanishi qat'iyan taqiqlanadi.",
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// 2-BOSQICH: MANZIL VA BIOMETRIK FACE ID XAVFSIZLIGI
// =========================================================================
@Composable
private fun StepTwoLocationAndFaceContent(
    selectedRegion: RegionInfo?,
    selectedDistrict: DistrictInfo?,
    selectedMahalla: String,
    customMahalla: String,
    onOpenRegionSheet: () -> Unit,
    onOpenDistrictSheet: () -> Unit,
    onOpenMahallaSheet: () -> Unit,
    capturedBitmap: Bitmap?,
    isFaceVerified: Boolean,
    onTriggerCamera: () -> Unit,
    onTriggerGallery: () -> Unit,
    showErrors: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Manzil va Xavfsizlik 🛡️",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPureWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Visol.uz da faqat haqiqiy profillar jamlangan. Suratni kameradan yoki galereyangizdan yuklashingiz mumkin.",
            fontSize = 12.sp,
            color = TextLavenderMuted,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Viloyat
        LuxurySelectorField(
            title = "Viloyat / Hudud *",
            value = selectedRegion?.name ?: "Hududingizni tanlang",
            isSelected = selectedRegion != null,
            onClick = onOpenRegionSheet,
            icon = Icons.Default.LocationCity,
            hasError = showErrors && selectedRegion == null
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tuman
        LuxurySelectorField(
            title = "Tuman yoki Shahar *",
            value = selectedDistrict?.name ?: (if (selectedRegion == null) "Avval viloyatni tanlang" else "Tumanni tanlang"),
            isSelected = selectedDistrict != null,
            onClick = onOpenDistrictSheet,
            icon = Icons.Default.Place,
            isEnabled = selectedRegion != null,
            hasError = showErrors && selectedDistrict == null
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Mahalla
        val displayMahalla = if (customMahalla.isNotBlank()) customMahalla else selectedMahalla
        LuxurySelectorField(
            title = "Mahalla / MFY *",
            value = displayMahalla.ifBlank { if (selectedDistrict == null) "Avval tumanni tanlang" else "Mahallani tanlang" },
            isSelected = displayMahalla.isNotBlank(),
            onClick = onOpenMahallaSheet,
            icon = Icons.Default.HomeWork,
            isEnabled = selectedDistrict != null,
            hasError = showErrors && displayMahalla.isBlank()
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Profil Surati va Tasdiqlov
        Text(
            text = "Profil Surati & Tasdiqlov *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPureWhite
        )
        Spacer(modifier = Modifier.height(8.dp))

        val faceHasError = showErrors && !isFaceVerified
        Surface(
            onClick = onTriggerCamera,
            shape = RoundedCornerShape(22.dp),
            color = if (isFaceVerified) Color(0x2E10B981) else GlassSurfaceBg,
            border = BorderStroke(
                1.5.dp,
                if (faceHasError) PinkGradientEnd else if (isFaceVerified) SuccessMint else GlassBorderSoft
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isFaceVerified) SuccessMint else PeachGradientStart, CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = "Tasdiqlangan yuz",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = if (faceHasError) PinkGradientEnd else PeachGradientStart,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isFaceVerified) "Surat Tasdiqlandi ✓" else "Jonli Kamera (Face ID)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFaceVerified) SuccessMint else TextPureWhite
                        )
                        if (isFaceVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Verified, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFaceVerified) "Surat muvaffaqiyatli saqlandi. Profilga moviy belgi beriladi."
                        else "Kameraga qarab ko'zni bir marta yumib-oching.",
                        fontSize = 11.sp,
                        color = TextLavenderMuted,
                        lineHeight = 15.sp
                    )
                }

                Icon(
                    imageVector = if (isFaceVerified) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = if (isFaceVerified) SuccessMint else PeachGradientStart,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Yoki Galereyadan Rasm Tanlash (Face ID shart bo'lmagan usul)
        Surface(
            onClick = onTriggerGallery,
            shape = RoundedCornerShape(18.dp),
            color = Color(0x18FFFFFF),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("btn_select_gallery_photo")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0x30FBBF24)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Yoki Galereyadan Rasm Qo'yish 🖼️",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )
                    Text(
                        text = "Face ID shart emas, galereyangizdan ham qo'yaverasiz.",
                        fontSize = 11.sp,
                        color = TextLavenderMuted
                    )
                }
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// =========================================================================
// 3-BOSQICH: ALOQA VA HAYOTIY NIYATLAR
// =========================================================================
@Composable
private fun StepThreeContactAndBioContent(
    isEffectiveDark: Boolean,
    phone: String,
    onPhoneChange: (String) -> Unit,
    telegram: String,
    onTelegramChange: (String) -> Unit,
    telegramCheckState: TelegramVerificationResult,
    instagram: String,
    onInstagramChange: (String) -> Unit,
    tiktok: String,
    onTiktokChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    onAddBioTag: (String) -> Unit,
    showErrors: Boolean,
    isPhoneValid: Boolean,
    isBioValid: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Bog'lanish va Hayotiy Niyatlar 💍",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPureWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Suhbatdosh bilan ishonchli bog'lanish va oilaviy orzularingizni ochiq bayon eting.",
            fontSize = 12.sp,
            color = TextLavenderMuted,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Telefon raqami (+998)
        val phoneHasError = showErrors && !isPhoneValid
        OutlinedTextField(
            value = phone,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                val clean = if (digits.startsWith("998")) digits.drop(3) else digits
                val limited = clean.take(9)
                onPhoneChange(limited)
            },
            visualTransformation = PhoneNumberVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            label = { Text("Telefon raqamingiz *") },
            placeholder = { Text("+998 90 123 45 67", color = TextLavenderMuted.copy(alpha = 0.5f)) },
            leadingIcon = {
                Text(
                    text = "🇺🇿",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            },
            trailingIcon = {
                if (isPhoneValid) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessMint, modifier = Modifier.size(20.dp))
                }
            },
            isError = phoneHasError,
            supportingText = if (phoneHasError) {
                { Text("9 ta raqamdan iborat telefon raqamini kiriting", color = PinkGradientEnd, fontSize = 11.sp) }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = warmOutlinedColors(isEffectiveDark),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Telegram Username
        OutlinedTextField(
            value = telegram,
            onValueChange = onTelegramChange,
            label = { Text("Telegram manzili (@username) *") },
            prefix = { Text("@", color = PeachGradientStart, fontWeight = FontWeight.Bold) },
            placeholder = { Text("username", color = TextLavenderMuted.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = PeachGradientStart) },
            trailingIcon = {
                when (telegramCheckState) {
                    is TelegramVerificationResult.Valid -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessMint)
                    is TelegramVerificationResult.Checking -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PeachGradientStart)
                    else -> null
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = warmOutlinedColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Instagram & TikTok bir qatorda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = instagram,
                onValueChange = onInstagramChange,
                label = { Text("Instagram", fontSize = 12.sp) },
                prefix = { Text("@", color = PeachGradientStart) },
                placeholder = { Text("profil", color = TextLavenderMuted.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = warmOutlinedColors(),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = tiktok,
                onValueChange = onTiktokChange,
                label = { Text("TikTok", fontSize = 12.sp) },
                prefix = { Text("@", color = PeachGradientStart) },
                placeholder = { Text("profil", color = TextLavenderMuted.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = warmOutlinedColors(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bio & Oilaviy Niyat
        val bioHasError = showErrors && !isBioValid
        Text(
            text = "O'zingiz va oilaviy orzularingiz haqida *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPureWhite
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            placeholder = {
                Text(
                    "Kasbingiz, qiziqishlaringiz va qanday inson bilan oila qurishni orzu qilishingizni yozing...",
                    color = TextLavenderMuted.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            },
            isError = bioHasError,
            supportingText = if (bioHasError) {
                { Text("Kamida 5 ta harfdan iborat samimiy fikr bildiring", color = PinkGradientEnd, fontSize = 11.sp) }
            } else null,
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = warmOutlinedColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tezkor Ilhomlantiruvchi Teglar
        Text(
            text = "Tezkor biriktirish uchun bosing:",
            fontSize = 11.sp,
            color = TextLavenderMuted
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Oila qadriyatlari 👨‍👩‍👧",
                "Oliy ma'lumotli 🎓",
                "Kitobxon 📚",
                "Halol kasb 💼",
                "Sportchi 🏃",
                "Pazanda 🍲"
            ).forEach { tag ->
                Surface(
                    onClick = { onAddBioTag(tag) },
                    shape = RoundedCornerShape(12.dp),
                    color = GlassSurfaceBg,
                    border = BorderStroke(0.8.dp, GlassBorderSoft)
                ) {
                    Text(
                        text = "+ $tag",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PeachGradientStart,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// YORDAMCHI KOMPONENTLAR: HEADER, BOTTOM BAR, GENDER CARD VA SELECTOR
// =========================================================================

@Composable
private fun RegistrationTopHeader(
    currentStep: Int,
    isEffectiveDark: Boolean,
    onToggleTheme: () -> Unit,
    onStepClick: (Int) -> Unit,
    onAutofillClick: () -> Unit
) {
    val headerBg = if (isEffectiveDark) Color(0x50100719) else Color(0x60FFFFFF)
    val textPrimary = if (isEffectiveDark) TextPureWhite else Color(0xFF1E1022)
    val textMuted = if (isEffectiveDark) TextLavenderMuted else Color(0xFF475569)

    Surface(
        color = headerBg,
        border = BorderStroke(
            0.5.dp,
            Brush.verticalGradient(listOf(GlassBorderSoft.copy(alpha = 0.5f), Color.Transparent))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Yuqori Qator: Logo, Rasmiy Brand, Mavzu almashtirish va Tezkor To'ldirish
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PeachGradientStart, PinkGradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🕊️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Visol.uz",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x35FBBF24),
                                border = BorderStroke(0.6.dp, GoldAccent.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "BETA",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Nufuzli O'zbekona Sovchilik Maydoni",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = textMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mavzu almashtirish tugmasi (Dark / Light)
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isEffectiveDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Mavzuni o'zgartirish",
                            tint = if (isEffectiveDark) GoldAccent else Color(0xFF6B4E71),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // ⚡ Tezkor to'ldirish tugmasi (Test qilish uchun super qulay)
                    Surface(
                        onClick = onAutofillClick,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x28FBBF24),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚡", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Namunaviy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Yupqa va nafis 3-bosqichli progress bar
            val animatedProgress by animateFloatAsState(
                targetValue = currentStep / 3f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "step_prog"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x18FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(PeachGradientStart, PinkGradientEnd, GoldAccent)))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bosqich Navigatsiya Chipslari
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LuxuryStepChip(
                    number = 1,
                    title = "Shaxsiyat",
                    isActive = currentStep == 1,
                    isDone = currentStep > 1,
                    onClick = { onStepClick(1) }
                )
                LuxuryStepChip(
                    number = 2,
                    title = "Manzil & Face",
                    isActive = currentStep == 2,
                    isDone = currentStep > 2,
                    onClick = { onStepClick(2) }
                )
                LuxuryStepChip(
                    number = 3,
                    title = "Aloqa & Niyat",
                    isActive = currentStep == 3,
                    isDone = false,
                    onClick = { onStepClick(3) }
                )
            }
        }
    }
}

@Composable
private fun RegistrationBottomBar(
    currentStep: Int,
    isSubmitting: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Surface(
        color = Color(0x66100719),
        border = BorderStroke(
            0.5.dp,
            Brush.horizontalGradient(listOf(GlassBorderSoft.copy(alpha = 0.5f), GlassBorderGlow.copy(alpha = 0.6f), GlassBorderSoft.copy(alpha = 0.5f)))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = onPrevClick,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GlassBorderSoft),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PeachGradientStart),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("wizard_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Ortga",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ortga", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Katta, Shimmer Nurlanuvchi "Keyingisi" Tugmasi
            Button(
                onClick = onNextClick,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0x60FFA07A)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = PeachGradientStart, ambientColor = PinkGradientEnd)
                    .testTag("wizard_next_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PeachGradientStart, PinkGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Hisob ochilmoqda...",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = if (currentStep == 3) "Ro'yxatdan o'tish ✨" else "Keyingi bosqich",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (currentStep == 3) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LuxuryGenderCard(
    title: String,
    roleTitle: String,
    emoji: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "gender_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) Color(0x35FF4D79) else GlassSurfaceBg,
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            brush = if (isSelected) Brush.linearGradient(listOf(PeachGradientStart, PinkGradientEnd))
            else Brush.linearGradient(listOf(GlassBorderSoft, GlassBorderSoft))
        ),
        modifier = modifier
            .scale(scaleAnim)
            .height(84.dp)
            .shadow(
                elevation = if (isSelected) 8.dp else 0.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = PeachGradientStart
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Brush.linearGradient(listOf(PeachGradientStart, PinkGradientEnd))
                        else Brush.linearGradient(listOf(Color(0x1EFFFFFF), Color(0x0CFFFFFF)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = TextPureWhite
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PeachGradientStart, modifier = Modifier.size(14.dp))
                    }
                }
                Text(
                    text = roleTitle,
                    fontSize = 10.sp,
                    color = if (isSelected) GoldAccent else TextLavenderMuted
                )
            }
        }
    }
}

@Composable
private fun LuxuryStepChip(
    number: Int,
    title: String,
    isActive: Boolean,
    isDone: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = when {
            isActive -> Color(0x38FF4D79)
            isDone -> Color(0x2E10B981)
            else -> Color(0x0EFFFFFF)
        },
        border = BorderStroke(
            1.dp,
            when {
                isActive -> PeachGradientStart
                isDone -> SuccessMint
                else -> GlassBorderSoft
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> PeachGradientStart
                            isDone -> SuccessMint
                            else -> Color(0x28FFFFFF)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                } else {
                    Text(text = "$number", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) TextPureWhite else TextLavenderMuted
            )
        }
    }
}

@Composable
private fun LuxurySelectorField(
    title: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean = true,
    hasError: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled) GlassSurfaceBg else Color(0x06FFFFFF),
        border = BorderStroke(
            1.dp,
            if (hasError) PinkGradientEnd else if (isSelected) PeachGradientStart else GlassBorderSoft
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0x35FF7B54) else Color(0x14FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (hasError) PinkGradientEnd else if (isSelected) PeachGradientStart else TextLavenderMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = if (hasError) PinkGradientEnd else TextLavenderMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isEnabled) TextPureWhite else Color(0xFF6B5867),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (isEnabled) PeachGradientStart else Color(0xFF6B5867)
            )
        }
    }
}

@Composable
private fun AnimatedAuroraBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val orbX1 = w * 0.25f + (sin(waveOffset) * 60f)
        val orbY1 = h * 0.2f + (sin(waveOffset * 1.2f) * 40f)

        val orbX2 = w * 0.8f + (sin(waveOffset * 0.8f + 1f) * 70f)
        val orbY2 = h * 0.5f + (sin(waveOffset * 1.5f + 2f) * 50f)

        val orbX3 = w * 0.4f + (sin(waveOffset * 1.1f + 3f) * 60f)
        val orbY3 = h * 0.85f + (sin(waveOffset * 0.9f) * 40f)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(PeachGradientStart.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(orbX1, orbY1),
                radius = w * 0.65f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(PinkGradientEnd.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(orbX2, orbY2),
                radius = w * 0.70f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(GoldAccent.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(orbX3, orbY3),
                radius = w * 0.60f
            )
        )
    }
}

@Composable
private fun warmOutlinedColors(isEffectiveDark: Boolean = true) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = if (isEffectiveDark) Color(0xA5260F3F) else Color(0xF5FFFFFF),
    unfocusedContainerColor = if (isEffectiveDark) Color(0x75200C35) else Color(0xE8F8FAFC),
    disabledContainerColor = if (isEffectiveDark) Color(0x44180B28) else Color(0xFFF1F5F9),
    errorContainerColor = if (isEffectiveDark) Color(0x3DFF4D79) else Color(0xFFFFF1F2),
    focusedBorderColor = PinkGradientEnd,
    unfocusedBorderColor = if (isEffectiveDark) GlassBorderSoft else Color(0x77FF7B54),
    errorBorderColor = PinkGradientEnd,
    focusedTextColor = if (isEffectiveDark) TextPureWhite else Color(0xFF1E1022),
    unfocusedTextColor = if (isEffectiveDark) TextPureWhite else Color(0xFF1E1022),
    disabledTextColor = Color(0xFF94A3B8),
    errorTextColor = if (isEffectiveDark) TextPureWhite else Color(0xFFE11D48),
    focusedLabelColor = PeachGradientStart,
    unfocusedLabelColor = if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B),
    focusedLeadingIconColor = PeachGradientStart,
    unfocusedLeadingIconColor = PeachGradientStart,
    focusedTrailingIconColor = PeachGradientStart,
    unfocusedTrailingIconColor = if (isEffectiveDark) TextLavenderMuted else Color(0xFF64748B),
    focusedPrefixColor = PeachGradientStart,
    unfocusedPrefixColor = PeachGradientStart,
    cursorColor = PinkGradientEnd
)
