package com.webtech.kamuskorea.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * NotificationWorker
 *
 * Worker untuk menampilkan Daily Learning Reminder setiap hari jam 19:00
 * Dijalankan oleh WorkManager sebagai PeriodicWorkRequest
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appNotificationManager: AppNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
        const val WORK_NAME = "daily_learning_reminder"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Daily Learning Reminder triggered at ${System.currentTimeMillis()}")

            // Tampilkan notifikasi daily learning reminder
            appNotificationManager.showDailyLearningReminder()

            Log.d(TAG, "Daily Learning Reminder shown successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Daily Learning Reminder", e)
            Result.failure()
        }
    }
}
