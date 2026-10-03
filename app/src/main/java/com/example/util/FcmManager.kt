package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

object FcmManager {
    private const val TAG = "VisolFCM"

    /**
     * Checks if Firebase is initialized or can be safely auto-initialized with a valid
     * google-services.json configuration. If no valid Google Services config exists,
     * returns false and avoids calling Firebase APIs to prevent Firebase Installations crashes.
     */
    fun isFirebaseConfigured(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                true
            } else {
                // Check if Google Services generated string resource exists in the APK
                val resId = context.resources.getIdentifier(
                    "google_app_id",
                    "string",
                    context.packageName
                )
                if (resId != 0 && context.getString(resId).isNotBlank()) {
                    val app = FirebaseApp.initializeApp(context)
                    app != null
                } else {
                    Log.d(TAG, "No google-services.json configuration found. Skipping Firebase SDK initialization.")
                    false
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase initialization check note: ${e.message}")
            false
        }
    }

    /**
     * Initializes FCM or device token.
     * If valid Firebase credentials exist, fetches FCM token.
     * If not, safely generates and saves an active dev device token so the app operates smoothly.
     */
    fun retrieveAndLogFcmToken(
        context: Context,
        onTokenReceived: ((String) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        val userRepository = UserRepository(appContext)

        // Ensure an active token is immediately available for UI and offline mode
        if (userRepository.fcmToken.value.isNullOrBlank()) {
            fallbackToken(userRepository, onTokenReceived)
        }

        if (isFirebaseConfigured(appContext)) {
            try {
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful && !task.result.isNullOrBlank()) {
                            val token = task.result
                            Log.d(TAG, "==================================================")
                            Log.d(TAG, "🔥 FCM UNIQUE DEVICE TOKEN:")
                            Log.d(TAG, token)
                            Log.d(TAG, "==================================================")

                            userRepository.saveFcmToken(token)
                            onTokenReceived?.invoke(token)
                        } else {
                            Log.w(TAG, "Fetching FCM registration token: ${task.exception?.message}")
                            fallbackToken(userRepository, onTokenReceived)
                        }
                    }
            } catch (t: Throwable) {
                Log.w(TAG, "FirebaseMessaging exception: ${t.message}. Fallback token maintained.")
                fallbackToken(userRepository, onTokenReceived)
            }
        } else {
            // Firebase is not configured with a real google-services.json.
            // Using active local/dev token for communication.
            val currentToken = userRepository.fcmToken.value
            if (currentToken.isNullOrBlank()) {
                fallbackToken(userRepository, onTokenReceived)
            } else {
                onTokenReceived?.invoke(currentToken)
            }
        }
    }

    private fun fallbackToken(
        userRepository: UserRepository,
        onTokenReceived: ((String) -> Unit)?
    ) {
        val existingToken = userRepository.fcmToken.value
        val token = if (!existingToken.isNullOrBlank()) {
            existingToken
        } else {
            "fcm_visol_dev_${UUID.randomUUID().toString().replace("-", "").take(24)}"
        }
        Log.d(TAG, "🔥 Active Device Token: $token")
        userRepository.saveFcmToken(token)
        onTokenReceived?.invoke(token)
    }
}

