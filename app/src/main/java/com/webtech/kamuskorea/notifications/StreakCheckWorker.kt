package com.webtech.kamuskorea.notifications

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.webtech.kamuskorea.ui.datastore.SettingsDataStore
import com.webtech.kamuskorea.ui.datastore.dataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * StreakCheckWorker
 *
 * Worker untuk memeriksa streak user dan menampilkan Streak Saver notification
 * jika user belum belajar hari ini.
 *
 * Dijalankan setiap hari jam 22:00 untuk memberikan kesempatan terakhir
 * kepada user untuk menjaga streak mereka.
 */
@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appNotificationManager: AppNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "StreakCheckWorker"
        const val WORK_NAME = "streak_check"
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Streak Check triggered at ${System.currentTimeMillis()}")

            val dataStore = appContext.dataStore
            val preferences = dataStore.data.first()

            // Ambil last study date dan current streak dari DataStore
            val lastStudyDateString = preferences[SettingsDataStore.LAST_STUDY_DATE_KEY]
            val currentStreak = preferences[SettingsDataStore.CURRENT_STREAK_KEY] ?: 0

            // Hanya tampilkan notifikasi jika user punya streak > 0
            // (artinya pernah belajar sebelumnya)
            if (currentStreak > 0) {
                val today = LocalDate.now()
                val todayString = today.format(DATE_FORMATTER)

                // Cek apakah user sudah belajar hari ini
                val hasStudiedToday = lastStudyDateString == todayString

                if (!hasStudiedToday) {
                    // User belum belajar hari ini, tampilkan streak saver notification
                    Log.d(TAG, "User hasn't studied today. Showing Streak Saver notification. Current streak: $currentStreak")
                    appNotificationManager.showStreakSaverNotification(currentStreak)
                } else {
                    Log.d(TAG, "User already studied today. No need for Streak Saver notification.")
                }
            } else {
                Log.d(TAG, "User has no active streak. Skipping Streak Saver notification.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in Streak Check Worker", e)
            Result.failure()
        }
    }
}
