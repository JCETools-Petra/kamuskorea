package com.webtech.kamuskorea.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * KamusMessagingService
 *
 * Service untuk menangani Firebase Cloud Messaging (FCM)
 * dari server/Firebase Console
 *
 * Tipe notifikasi FCM yang didukung:
 * - new_content: Konten baru (PDF/Quiz) diupload
 * - announcement: Pengumuman dari admin
 * - custom: Notifikasi custom lainnya
 *
 * Format payload dari server:
 * {
 *   "notification": {
 *     "title": "Materi Baru Tersedia!",
 *     "body": "Ada 5 materi baru tentang Grammar Korea"
 *   },
 *   "data": {
 *     "type": "new_content",
 *     "content_type": "pdf",
 *     "content_id": "123"
 *   }
 * }
 */
@AndroidEntryPoint
class KamusMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var appNotificationManager: AppNotificationManager

    companion object {
        private const val TAG = "KamusMessagingService"

        // Data keys dari FCM payload
        private const val KEY_TYPE = "type"
        private const val KEY_CONTENT_TYPE = "content_type"
        private const val KEY_CONTENT_ID = "content_id"

        // Notification types
        private const val TYPE_NEW_CONTENT = "new_content"
        private const val TYPE_ANNOUNCEMENT = "announcement"
    }

    /**
     * Dipanggil ketika FCM token baru di-generate
     * Token ini digunakan untuk mengirim notifikasi ke device tertentu
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token generated: $token")

        // TODO: Kirim token ini ke backend server Anda untuk disimpan
        // Backend akan menggunakan token ini untuk mengirim targeted notifications
        // Contoh: apiService.updateFcmToken(userId, token)
    }

    /**
     * Dipanggil ketika menerima message dari FCM
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        // Cek apakah message mengandung notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Kamus Korea"
            val body = notification.body ?: ""

            Log.d(TAG, "Notification Title: $title")
            Log.d(TAG, "Notification Body: $body")

            // Cek data payload untuk menentukan tipe notifikasi
            val notificationType = remoteMessage.data[KEY_TYPE]

            when (notificationType) {
                TYPE_NEW_CONTENT -> {
                    handleNewContentNotification(title, body, remoteMessage.data)
                }
                TYPE_ANNOUNCEMENT -> {
                    handleAnnouncementNotification(title, body)
                }
                else -> {
                    // Default notification jika tipe tidak dikenali
                    handleAnnouncementNotification(title, body)
                }
            }
        }

        // Jika hanya ada data payload tanpa notification payload
        if (remoteMessage.data.isNotEmpty() && remoteMessage.notification == null) {
            Log.d(TAG, "Data payload: ${remoteMessage.data}")
            handleDataOnlyMessage(remoteMessage.data)
        }
    }

    /**
     * Handle notifikasi konten baru (PDF/Quiz)
     */
    private fun handleNewContentNotification(
        title: String,
        message: String,
        data: Map<String, String>
    ) {
        val contentType = data[KEY_CONTENT_TYPE] ?: "content"
        val contentId = data[KEY_CONTENT_ID]

        Log.d(TAG, "New content notification: type=$contentType, id=$contentId")

        appNotificationManager.showNewContentNotification(
            title = title,
            message = message,
            contentType = contentType,
            contentId = contentId
        )
    }

    /**
     * Handle notifikasi pengumuman
     */
    private fun handleAnnouncementNotification(title: String, message: String) {
        Log.d(TAG, "Announcement notification")
        appNotificationManager.showAnnouncementNotification(title, message)
    }

    /**
     * Handle data-only message (tanpa notification payload)
     * Berguna untuk silent notifications atau background data sync
     */
    private fun handleDataOnlyMessage(data: Map<String, String>) {
        val notificationType = data[KEY_TYPE]

        when (notificationType) {
            TYPE_NEW_CONTENT -> {
                val title = data["title"] ?: "Konten Baru Tersedia!"
                val message = data["message"] ?: "Ada konten baru untuk kamu"
                handleNewContentNotification(title, message, data)
            }
            TYPE_ANNOUNCEMENT -> {
                val title = data["title"] ?: "Pengumuman"
                val message = data["message"] ?: "Ada pengumuman baru"
                handleAnnouncementNotification(title, message)
            }
            else -> {
                Log.d(TAG, "Unknown data-only message type: $notificationType")
            }
        }
    }
}
