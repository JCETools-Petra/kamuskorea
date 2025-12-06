package com.webtech.learningkorea.gamification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * XpSyncWorker
 *
 * Background worker yang sync XP, level, dan achievements ke server MySQL
 * Dijalankan setiap 15 menit menggunakan WorkManager
 */
@HiltWorker
class XpSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val gamificationRepository: GamificationRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "XpSyncWorker"
        const val WORK_NAME = "xp_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🔄 XP Sync Worker triggered")

            // Check if sync is needed (15 minutes since last sync)
            if (!gamificationRepository.needsSync()) {
                Log.d(TAG, "⏭️ Sync not needed yet, skipping")
                return Result.success()
            }

            // Perform sync
            val result = gamificationRepository.syncToServer()

            if (result.isSuccess) {
                val rank = result.getOrNull() ?: 0
                Log.d(TAG, "✅ XP synced successfully. Rank: $rank")
                Result.success()
            } else {
                Log.e(TAG, "❌ XP sync failed: ${result.exceptionOrNull()?.message}")
                // Retry on failure
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ XP sync error", e)
            Result.retry()
        }
    }
}
