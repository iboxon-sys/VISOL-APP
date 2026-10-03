package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    private const val CHANNEL_ID = "channel_profile_visitors"
    private const val CHANNEL_NAME = "Profil Kuzatuvlari"

    const val FCM_CHANNEL_ID = "high_importance_channel"
    const val FCM_CHANNEL_NAME = "Visol Push Xabarnomalari"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            val visitorChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kim profilingizni kuzatgani haqidagi bildirishnomalar"
                enableVibration(true)
            }
            manager?.createNotificationChannel(visitorChannel)

            val fcmChannel = NotificationChannel(
                FCM_CHANNEL_ID,
                FCM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "FCM orqali yuqori ustuvorlikdagi push bildirishnomalar (Heads-up banner va tovush)"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager?.createNotificationChannel(fcmChannel)
        }
    }

    fun showFcmNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        initChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, FCM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun sendProfileViewedNotification(
        context: Context,
        visitorName: String,
        timeString: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        initChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Yangi profil kuzatuvi! \uD83D\uDC40")
            .setContentText("$visitorName sizning profilingizni ko'rdi ($timeString)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$visitorName hozirgina sizning profilingizni qiziqish bilan ko'rib chiqdi. Ushbu shaxsning anketasini ochib ko'rishingiz mumkin!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted or blocked
        }
    }
}
