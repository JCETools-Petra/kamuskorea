package com.webtech.kamuskorea.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Notification Channels untuk aplikasi Kamus Korea
 *
 * Channel ID yang digunakan:
 * - DAILY_REMINDER: Untuk pengingat belajar harian (19:00)
 * - STREAK_SAVER: Untuk pengingat streak di malam hari (22:00)
 * - MILESTONE: Untuk achievement dan milestone (7 hari, 30 hari, dll)
 * - NEW_CONTENT: Untuk notifikasi konten baru dari server
 * - ANNOUNCEMENTS: Untuk pengumuman penting dari admin
 */
object NotificationChannels {

    // Channel IDs
    const val DAILY_REMINDER_CHANNEL_ID = "daily_reminder"
    const val STREAK_SAVER_CHANNEL_ID = "streak_saver"
    const val MILESTONE_CHANNEL_ID = "milestone"
    const val NEW_CONTENT_CHANNEL_ID = "new_content"
    const val ANNOUNCEMENTS_CHANNEL_ID = "announcements"

    // Notification IDs (unique untuk setiap tipe notifikasi)
    const val DAILY_REMINDER_NOTIFICATION_ID = 1001
    const val STREAK_SAVER_NOTIFICATION_ID = 1002
    const val MILESTONE_NOTIFICATION_ID = 1003
    const val NEW_CONTENT_NOTIFICATION_ID = 1004
    const val ANNOUNCEMENT_NOTIFICATION_ID = 1005

    /**
     * Membuat semua notification channels
     * Dipanggil saat aplikasi pertama kali dijalankan (di MainActivity)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Daily Reminder Channel (Priority: DEFAULT)
            val dailyReminderChannel = NotificationChannel(
                DAILY_REMINDER_CHANNEL_ID,
                "Pengingat Belajar Harian",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat untuk belajar bahasa Korea setiap hari"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Streak Saver Channel (Priority: HIGH)
            val streakSaverChannel = NotificationChannel(
                STREAK_SAVER_CHANNEL_ID,
                "Pengingat Streak",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat untuk menjaga streak belajar kamu"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Milestone Channel (Priority: HIGH)
            val milestoneChannel = NotificationChannel(
                MILESTONE_CHANNEL_ID,
                "Pencapaian & Milestone",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat kamu mencapai milestone tertentu"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 4. New Content Channel (Priority: DEFAULT)
            val newContentChannel = NotificationChannel(
                NEW_CONTENT_CHANNEL_ID,
                "Konten Baru",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi saat ada materi atau kuis baru"
                enableLights(true)
                enableVibration(false)
                setShowBadge(true)
            }

            // 5. Announcements Channel (Priority: HIGH)
            val announcementsChannel = NotificationChannel(
                ANNOUNCEMENTS_CHANNEL_ID,
                "Pengumuman",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengumuman penting dari Kamus Korea"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Register semua channels
            notificationManager.createNotificationChannels(
                listOf(
                    dailyReminderChannel,
                    streakSaverChannel,
                    milestoneChannel,
                    newContentChannel,
                    announcementsChannel
                )
            )
        }
    }

    /**
     * Mendapatkan default priority untuk channel tertentu
     * Digunakan untuk Android versi lama (< Oreo)
     */
    fun getDefaultPriority(channelId: String): Int {
        return when (channelId) {
            DAILY_REMINDER_CHANNEL_ID -> NotificationCompat.PRIORITY_DEFAULT
            STREAK_SAVER_CHANNEL_ID -> NotificationCompat.PRIORITY_HIGH
            MILESTONE_CHANNEL_ID -> NotificationCompat.PRIORITY_HIGH
            NEW_CONTENT_CHANNEL_ID -> NotificationCompat.PRIORITY_DEFAULT
            ANNOUNCEMENTS_CHANNEL_ID -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }
}
