package com.webtech.learningkorea.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.webtech.learningkorea.MainActivity
import com.webtech.learningkorea.R
import com.webtech.learningkorea.analytics.AnalyticsTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppNotificationManager
 *
 * Mengelola semua notifikasi di aplikasi Kamus Korea
 * Handles:
 * - Daily learning reminder
 * - Streak saver notification
 * - Milestone celebrations
 * - New content alerts
 * - Admin announcements
 */
@Singleton
class AppNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsTracker: AnalyticsTracker
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Menampilkan notifikasi pengingat belajar harian (19:00)
     */
    fun showDailyLearningReminder() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "daily_reminder")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationChannels.DAILY_REMINDER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.DAILY_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Akan kita buat icon ini
            .setContentTitle("Waktunya Belajar Korea! 📚")
            .setContentText("Yuk lanjutkan belajar bahasa Korea kamu hari ini!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Jangan sampai streak kamu putus! Belajar 5 menit saja sudah cukup untuk hari ini.")
            )
            .setPriority(NotificationChannels.getDefaultPriority(NotificationChannels.DAILY_REMINDER_CHANNEL_ID))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationChannels.DAILY_REMINDER_NOTIFICATION_ID, notification)

        // Track analytics
        analyticsTracker.logNotificationShown("daily_reminder", "scheduled")
    }

    /**
     * Menampilkan notifikasi streak saver (22:00 jika belum belajar hari ini)
     */
    fun showStreakSaverNotification(currentStreak: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "streak_saver")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationChannels.STREAK_SAVER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.STREAK_SAVER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Streak Kamu Hampir Putus! 🔥")
            .setContentText("Kamu belum belajar hari ini. Streak $currentStreak hari terancam hilang!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Streak $currentStreak hari kamu sangat berharga! Cukup buka 1 materi atau kerjakan 1 kuis untuk menjaga streak kamu.")
            )
            .setPriority(NotificationChannels.getDefaultPriority(NotificationChannels.STREAK_SAVER_CHANNEL_ID))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationChannels.STREAK_SAVER_NOTIFICATION_ID, notification)

        // Track analytics
        analyticsTracker.logNotificationShown("streak_saver", "scheduled")
    }

    /**
     * Menampilkan notifikasi milestone: Streak 7 hari
     */
    fun showSevenDayStreakMilestone() {
        showMilestoneNotification(
            title = "Streak 7 Hari! 🎉",
            message = "Luar biasa! Kamu sudah konsisten belajar 7 hari berturut-turut!",
            bigText = "Konsistensi adalah kunci sukses belajar bahasa. Teruskan streak kamu dan raih milestone berikutnya!",
            milestoneType = "streak_7_days"
        )
    }

    /**
     * Menampilkan notifikasi milestone: Streak 30 hari
     */
    fun showThirtyDayStreakMilestone() {
        showMilestoneNotification(
            title = "Streak 30 Hari! 🏆",
            message = "Incredible! Kamu adalah master konsistensi! 30 hari berturut-turut!",
            bigText = "1 bulan penuh tanpa putus! Kamu membuktikan bahwa dedikasi kamu luar biasa. Terus tingkatkan!",
            milestoneType = "streak_30_days"
        )
    }

    /**
     * Menampilkan notifikasi milestone: Streak 100 hari
     */
    fun showHundredDayStreakMilestone() {
        showMilestoneNotification(
            title = "Streak 100 Hari! 🌟",
            message = "LEGENDARY! Kamu sudah mencapai streak 100 hari!",
            bigText = "Kamu termasuk dalam 1% pengguna paling konsisten! Prestasi ini sangat luar biasa!",
            milestoneType = "streak_100_days"
        )
    }

    /**
     * Menampilkan notifikasi milestone: 100 kata disimpan
     */
    fun showHundredWordsSavedMilestone() {
        showMilestoneNotification(
            title = "100 Kata Tersimpan! 📝",
            message = "Wow! Kamu sudah menyimpan 100 kata favorit!",
            bigText = "Vocabulary kamu bertambah pesat! Terus tambah koleksi kata-kata favorit kamu.",
            milestoneType = "words_saved_100"
        )
    }

    /**
     * Menampilkan notifikasi milestone: 50 kuis diselesaikan
     */
    fun showFiftyQuizzesCompletedMilestone() {
        showMilestoneNotification(
            title = "50 Kuis Diselesaikan! ✅",
            message = "Keren! Kamu sudah menyelesaikan 50 kuis!",
            bigText = "Practice makes perfect! Kemampuan kamu pasti sudah meningkat drastis!",
            milestoneType = "quizzes_completed_50"
        )
    }

    /**
     * Helper function untuk menampilkan milestone notification
     */
    private fun showMilestoneNotification(
        title: String,
        message: String,
        bigText: String,
        milestoneType: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "milestone")
            putExtra("milestone_type", milestoneType)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationChannels.MILESTONE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.MILESTONE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationChannels.getDefaultPriority(NotificationChannels.MILESTONE_CHANNEL_ID))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationChannels.MILESTONE_NOTIFICATION_ID, notification)

        // Track analytics
        analyticsTracker.logNotificationShown("milestone", milestoneType)
    }

    /**
     * Menampilkan notifikasi konten baru dari FCM
     */
    fun showNewContentNotification(
        title: String,
        message: String,
        contentType: String, // "pdf", "quiz", "chapter"
        contentId: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "new_content")
            putExtra("content_type", contentType)
            contentId?.let { putExtra("content_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationChannels.NEW_CONTENT_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.NEW_CONTENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationChannels.getDefaultPriority(NotificationChannels.NEW_CONTENT_CHANNEL_ID))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationChannels.NEW_CONTENT_NOTIFICATION_ID, notification)

        // Track analytics
        analyticsTracker.logNotificationShown("new_content", contentType)
    }

    /**
     * Menampilkan notifikasi pengumuman dari FCM
     */
    fun showAnnouncementNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "announcement")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationChannels.ANNOUNCEMENT_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.ANNOUNCEMENTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationChannels.getDefaultPriority(NotificationChannels.ANNOUNCEMENTS_CHANNEL_ID))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationChannels.ANNOUNCEMENT_NOTIFICATION_ID, notification)

        // Track analytics
        analyticsTracker.logNotificationShown("announcement", "fcm")
    }

    /**
     * Cancel notifikasi tertentu
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel semua notifikasi
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
