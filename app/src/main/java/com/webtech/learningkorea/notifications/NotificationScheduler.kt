package com.webtech.learningkorea.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationScheduler
 *
 * Mengelola penjadwalan semua notifikasi di aplikasi menggunakan WorkManager
 *
 * Scheduled Notifications:
 * - Daily Learning Reminder: 19:00 setiap hari
 * - Streak Saver: 22:00 setiap hari (cek apakah user sudah belajar)
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "NotificationScheduler"

        // Target times untuk notifikasi
        private val DAILY_REMINDER_TIME = LocalTime.of(19, 0) // 19:00
        private val STREAK_CHECK_TIME = LocalTime.of(22, 0)   // 22:00
    }

    /**
     * Schedule semua notifikasi
     * Dipanggil saat aplikasi pertama kali dibuka
     */
    fun scheduleAllNotifications() {
        scheduleDailyLearningReminder()
        scheduleStreakCheck()
        Log.d(TAG, "All notifications scheduled successfully")
    }

    /**
     * Schedule Daily Learning Reminder (19:00)
     */
    private fun scheduleDailyLearningReminder() {
        val initialDelay = calculateInitialDelay(DAILY_REMINDER_TIME)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // Tetap jalan meskipun battery low
            .build()

        val dailyReminderRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            NotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep jika sudah ada (jangan reschedule)
            dailyReminderRequest
        )

        Log.d(TAG, "Daily Learning Reminder scheduled for 19:00 daily (initial delay: ${initialDelay / 1000 / 60} minutes)")
    }

    /**
     * Schedule Streak Check (22:00)
     */
    private fun scheduleStreakCheck() {
        val initialDelay = calculateInitialDelay(STREAK_CHECK_TIME)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val streakCheckRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            StreakCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            streakCheckRequest
        )

        Log.d(TAG, "Streak Check scheduled for 22:00 daily (initial delay: ${initialDelay / 1000 / 60} minutes)")
    }

    /**
     * Calculate initial delay untuk menjadwalkan notifikasi pada waktu yang tepat
     *
     * Jika sekarang jam 20:00 dan target jam 19:00, maka delay = besok jam 19:00 - sekarang
     * Jika sekarang jam 18:00 dan target jam 19:00, maka delay = hari ini jam 19:00 - sekarang
     */
    private fun calculateInitialDelay(targetTime: LocalTime): Long {
        val now = LocalDateTime.now()
        var scheduledDateTime = now.toLocalDate().atTime(targetTime)

        // Jika waktu target hari ini sudah lewat, schedule untuk besok
        if (scheduledDateTime.isBefore(now) || scheduledDateTime.isEqual(now)) {
            scheduledDateTime = scheduledDateTime.plusDays(1)
        }

        val delay = Duration.between(now, scheduledDateTime)
        return delay.toMillis()
    }

    /**
     * Cancel semua scheduled notifications
     * Berguna jika user menonaktifkan notifikasi di settings
     */
    fun cancelAllNotifications() {
        workManager.cancelUniqueWork(NotificationWorker.WORK_NAME)
        workManager.cancelUniqueWork(StreakCheckWorker.WORK_NAME)
        Log.d(TAG, "All scheduled notifications cancelled")
    }

    /**
     * Reschedule semua notifikasi
     * Berguna jika user mengaktifkan kembali notifikasi di settings
     */
    fun rescheduleAllNotifications() {
        cancelAllNotifications()
        scheduleAllNotifications()
        Log.d(TAG, "All notifications rescheduled")
    }
}
