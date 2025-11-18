package com.webtech.kamuskorea.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.webtech.kamuskorea.MainActivity
import com.webtech.kamuskorea.R

class KamusKoreaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        private const val CHANNEL_ID = "kamus_korea_notifications"
        private const val CHANNEL_NAME = "Kamus Korea Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifikasi dari Kamus Korea"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM Token: $token")
        // TODO: Jika Anda ingin menyimpan token ke server, lakukan di sini
        // sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "📩 Message received from: ${remoteMessage.from}")

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "📬 Notification Title: ${notification.title}")
            Log.d(TAG, "📬 Notification Body: ${notification.body}")

            sendNotification(
                title = notification.title ?: "Kamus Korea",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }

        // Handle data payload (jika hanya mengirim data tanpa notification)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "📦 Data Payload: ${remoteMessage.data}")

            // Jika tidak ada notification payload, buat dari data
            if (remoteMessage.notification == null) {
                val title = remoteMessage.data["title"] ?: "Kamus Korea"
                val body = remoteMessage.data["body"] ?: ""
                sendNotification(title, body, remoteMessage.data)
            }
        }
    }

    private fun sendNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        // Intent untuk buka MainActivity saat notification di-click
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Tambahkan data extras jika ada action khusus
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo) // Gunakan icon yang ada
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel untuk Android O ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Show notification dengan ID unik berdasarkan timestamp
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "✅ Notification displayed: $title")
    }
}
