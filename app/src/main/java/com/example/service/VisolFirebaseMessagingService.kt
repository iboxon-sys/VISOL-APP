package com.example.service

import android.util.Log
import com.example.data.UserRepository
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * VisolFirebaseMessagingService handles Firebase Cloud Messaging (FCM) callbacks:
 * - onNewToken: Called whenever a new FCM registration token is generated or refreshed.
 * - onMessageReceived: Called when an incoming push notification is delivered.
 *
 * Foreground vs. Background vs. Terminated:
 * - Foreground: onMessageReceived is triggered directly; we display a heads-up notification using NotificationHelper.
 * - Background & Terminated:
 *     - If payload contains "notification", the Android OS automatically displays the notification in the tray.
 *     - If payload contains "data", onMessageReceived is invoked in background and custom handling proceeds.
 */
class VisolFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "==================================================")
        Log.d(TAG, "FCM onNewToken: $token")
        Log.d(TAG, "==================================================")

        // Persist token in UserRepository SharedPreferences
        val userRepository = UserRepository(applicationContext)
        userRepository.saveFcmToken(token)

        // In production, send token to your Node.js backend:
        // sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // Check data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "FCM Message data payload: ${remoteMessage.data}")
        }

        // Check notification payload
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Visol Bildirishnomasi"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Sizga yangi xabar yoki profil kuzatuvi keldi!"

        // Display local heads-up notification using channel with High Importance
        NotificationHelper.showFcmNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = remoteMessage.data
        )
    }

    companion object {
        private const val TAG = "VisolFCMService"
    }
}
