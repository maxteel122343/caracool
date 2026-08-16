package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.audio.AudioVoiceManager

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_SOCIAL_ALERTS_ID = "cara_de_pacoca_social_alerts"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_ALERTS_ID,
                "👥 Interações da Comunidade & Voz IA",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações quando alguém curte, comenta ou usa sua cara de paçoca com voz da IA."
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(socialChannel)
        }
    }

    /**
     * Sends a notification and speaks the phrase aloud with the AI voice!
     */
    fun sendSocialNotificationWithVoice(
        context: Context,
        title: String,
        message: String,
        spokenVoicePhrase: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        try {
            createChannels(context)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL_ALERTS_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.notify(notificationId, notification)

            // Speak the phrase aloud!
            AudioVoiceManager.getInstance(context).speak(spokenVoicePhrase)
            Log.d(TAG, "Sent social notification & spoke: $spokenVoicePhrase")
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendSocialNotificationWithVoice: ${e.message}", e)
        }
    }
}
