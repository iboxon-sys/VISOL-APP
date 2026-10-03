package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.UserRepository
import com.example.ui.screens.MainScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.VisolTheme
import com.example.util.FcmManager
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val requestNotificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Once notification permission is granted, retrieve and log FCM token
                FcmManager.retrieveAndLogFcmToken(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Vaqtincha test qilish uchun skrinshot olish ruxsat etildi (FLAG_SECURE o'chirildi)
        // window.setFlags(
        //     WindowManager.LayoutParams.FLAG_SECURE,
        //     WindowManager.LayoutParams.FLAG_SECURE
        // )

        enableEdgeToEdge()

        // Coil ImageLoader optimizatsiyasi (Xotira va Disk keshini sozlash orqali laglarni bartaraf etish)
        try {
            val imageLoader = coil.ImageLoader.Builder(applicationContext)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(applicationContext.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(64L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
            coil.Coil.setImageLoader(imageLoader)
        } catch (_: Exception) {}

        com.example.service.TelegramNotificationService.initService()

        NotificationHelper.initChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                FcmManager.retrieveAndLogFcmToken(this)
            }
        } else {
            FcmManager.retrieveAndLogFcmToken(this)
        }

        val userRepository = UserRepository(applicationContext)

        setContent {
            val themeMode by userRepository.themeMode.collectAsState()
            VisolTheme(themeMode = themeMode) {
                val currentUser by userRepository.currentUser.collectAsState()
                var isSplashVisible by remember { mutableStateOf(true) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isSplashVisible) {
                        SplashScreen(
                            onSplashFinished = { isSplashVisible = false }
                        )
                    } else {
                        Crossfade(
                            targetState = currentUser != null,
                            label = "auth_crossfade"
                        ) { isRegistered ->
                            if (isRegistered) {
                                MainScreen(
                                    userRepository = userRepository,
                                    onAccountDeleted = {
                                        // User deleted account upon clicking "Chiqish"
                                    }
                                )
                            } else {
                                RegistrationScreen(
                                    userRepository = userRepository,
                                    onRegistrationSuccess = {
                                        // User registered successfully
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
