package com.webtech.kamuskorea.gamification

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.analytics.AnalyticsTracker
import com.webtech.kamuskorea.data.network.ApiService
import com.webtech.kamuskorea.notifications.AppNotificationManager
import com.webtech.kamuskorea.ui.datastore.SettingsDataStore
import com.webtech.kamuskorea.ui.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GamificationRepository
 *
 * Manages all gamification features:
 * - XP tracking (local + cloud sync)
 * - Level calculation
 * - Achievement unlocking
 * - Leaderboard
 */
@Singleton
class GamificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val firebaseAuth: FirebaseAuth,
    private val analyticsTracker: AnalyticsTracker,
    private val notificationManager: AppNotificationManager
) {

    private val dataStore: DataStore<Preferences> = context.dataStore

    companion object {
        private const val TAG = "GamificationRepository"
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
    }

    // ========== STATE FLOWS ==========

    /**
     * Observe current gamification state
     */
    val gamificationState: Flow<GamificationState> = dataStore.data.map { preferences ->
        GamificationState(
            totalXp = preferences[SettingsDataStore.USER_XP_KEY] ?: 0,
            currentLevel = preferences[SettingsDataStore.USER_LEVEL_KEY] ?: 1,
            achievementsUnlocked = preferences[SettingsDataStore.ACHIEVEMENTS_UNLOCKED_KEY] ?: emptySet<String>(),
            leaderboardRank = preferences[SettingsDataStore.LEADERBOARD_RANK_KEY] ?: 0,
            lastSyncTimestamp = preferences[SettingsDataStore.LAST_XP_SYNC_KEY] ?: 0L
        )
    }

    /**
     * Observe total XP
     */
    val totalXp: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.USER_XP_KEY] ?: 0
    }

    /**
     * Observe current level
     */
    val currentLevel: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.USER_LEVEL_KEY] ?: 1
    }

    /**
     * Observe achievements unlocked
     */
    val achievementsUnlocked: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.ACHIEVEMENTS_UNLOCKED_KEY] ?: emptySet<String>()
    }

    // ========== XP MANAGEMENT ==========

    /**
     * Add XP to user (local only, instant)
     * Call this from ViewModels when user performs actions
     */
    suspend fun addXp(amount: Int, source: String) {
        if (amount <= 0) return

        dataStore.edit { preferences ->
            val currentXp = preferences[SettingsDataStore.USER_XP_KEY] ?: 0
            val newTotalXp = currentXp + amount

            val oldLevel = preferences[SettingsDataStore.USER_LEVEL_KEY] ?: 1
            val newLevel = LevelSystem.calculateLevel(newTotalXp)

            preferences[SettingsDataStore.USER_XP_KEY] = newTotalXp
            preferences[SettingsDataStore.USER_LEVEL_KEY] = newLevel

            Log.d(TAG, "✅ Added $amount XP from $source. Total: $newTotalXp, Level: $newLevel")

            // Track analytics
            analyticsTracker.logXpEarned(amount, source)

            // Check for level up
            if (newLevel > oldLevel) {
                Log.d(TAG, "🎉 Level Up! $oldLevel → $newLevel")
                analyticsTracker.logLevelUp(newLevel)
                // TODO: Show level up notification/dialog
            }
        }

        // Check for achievement unlocks
        checkAchievementUnlocks()
    }

    /**
     * Check and unlock achievements based on current stats
     */
    private suspend fun checkAchievementUnlocks() {
        val state = gamificationState.first()
        val currentXp = state.totalXp

        // Check each achievement
        Achievements.ALL.forEach { achievement ->
            if (!state.hasAchievement(achievement.id)) {
                val shouldUnlock = when (achievement.id) {
                    "first_pdf" -> checkFirstPdfAchievement()
                    "pdf_explorer" -> checkPdfExplorerAchievement()
                    "7_day_streak" -> checkStreakAchievement(7)
                    "30_day_streak" -> checkStreakAchievement(30)
                    "100_day_streak" -> checkStreakAchievement(100)
                    "first_quiz" -> checkFirstQuizAchievement()
                    "quiz_master_10" -> checkQuizMasterAchievement(10)
                    "quiz_master_50" -> checkQuizMasterAchievement(50)
                    "vocab_10" -> checkVocabAchievement(10)
                    "vocab_100" -> checkVocabAchievement(100)
                    else -> false
                }

                if (shouldUnlock) {
                    unlockAchievement(achievement.id)
                }
            }
        }
    }

    /**
     * Unlock an achievement
     */
    private suspend fun unlockAchievement(achievementId: String) {
        val achievement = Achievements.getById(achievementId) ?: return

        dataStore.edit { preferences ->
            val current: Set<String> = preferences[SettingsDataStore.ACHIEVEMENTS_UNLOCKED_KEY] ?: emptySet()
            preferences[SettingsDataStore.ACHIEVEMENTS_UNLOCKED_KEY] = current.plus(achievementId)

            Log.d(TAG, "🏆 Achievement Unlocked: ${achievement.title} (+${achievement.xpReward} XP)")

            // Add XP reward
            val currentXp = preferences[SettingsDataStore.USER_XP_KEY] ?: 0
            val newTotalXp = currentXp + achievement.xpReward
            val newLevel = LevelSystem.calculateLevel(newTotalXp)

            preferences[SettingsDataStore.USER_XP_KEY] = newTotalXp
            preferences[SettingsDataStore.USER_LEVEL_KEY] = newLevel
        }

        // Track analytics
        analyticsTracker.logAchievementUnlocked(achievementId, achievement.title)

        // Show notification
        // notificationManager.showAchievementUnlocked(achievement.title, achievement.description)
    }

    // ========== ACHIEVEMENT CHECK HELPERS ==========

    private suspend fun checkFirstPdfAchievement(): Boolean {
        // Check if user has opened at least 1 PDF
        // TODO: Implement based on your PDF tracking logic
        return false
    }

    private suspend fun checkPdfExplorerAchievement(): Boolean {
        // Check if user has opened 10 different PDFs
        // TODO: Implement based on your PDF tracking logic
        return false
    }

    private suspend fun checkStreakAchievement(days: Int): Boolean {
        val preferences = dataStore.data.first()
        val currentStreak = preferences[SettingsDataStore.CURRENT_STREAK_KEY] ?: 0
        return currentStreak >= days
    }

    private suspend fun checkFirstQuizAchievement(): Boolean {
        val preferences = dataStore.data.first()
        val quizCount = preferences[com.webtech.kamuskorea.ui.screens.HomeViewModel.QUIZ_COMPLETED_COUNT_KEY] ?: 0
        return quizCount >= 1
    }

    private suspend fun checkQuizMasterAchievement(count: Int): Boolean {
        val preferences = dataStore.data.first()
        val quizCount = preferences[com.webtech.kamuskorea.ui.screens.HomeViewModel.QUIZ_COMPLETED_COUNT_KEY] ?: 0
        return quizCount >= count
    }

    private suspend fun checkVocabAchievement(count: Int): Boolean {
        // TODO: Implement based on favorites count
        return false
    }

    // ========== CLOUD SYNC ==========

    /**
     * Sync XP, level, and achievements to server
     * Called by XpSyncWorker every 15 minutes
     */
    suspend fun syncToServer(): Result<Int> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                Log.e(TAG, "Cannot sync: User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }

            val state = gamificationState.first()

            val request = SyncXpRequest(
                totalXp = state.totalXp,
                currentLevel = state.currentLevel,
                achievementsUnlocked = state.achievementsUnlocked.toList()
            )

            val response = apiService.syncXp(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val rank = response.body()?.leaderboardRank ?: 0

                // Update last sync timestamp and rank
                dataStore.edit { preferences ->
                    preferences[SettingsDataStore.LAST_XP_SYNC_KEY] = System.currentTimeMillis()
                    preferences[SettingsDataStore.LEADERBOARD_RANK_KEY] = rank
                }

                Log.d(TAG, "✅ Synced to server. Rank: $rank")
                Result.success(rank)
            } else {
                Log.e(TAG, "❌ Sync failed: ${response.errorBody()?.string()}")
                Result.failure(Exception("Sync failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Check if sync is needed (15 minutes since last sync)
     */
    suspend fun needsSync(): Boolean {
        val lastSync = dataStore.data.first()[SettingsDataStore.LAST_XP_SYNC_KEY] ?: 0L
        val now = System.currentTimeMillis()
        return (now - lastSync) >= SYNC_INTERVAL_MS
    }

    // ========== LEADERBOARD ==========

    /**
     * Fetch leaderboard from server
     */
    suspend fun getLeaderboard(limit: Int = 100): Result<List<LeaderboardEntry>> {
        return try {
            val response = apiService.getLeaderboard(limit)

            if (response.isSuccessful && response.body()?.success == true) {
                val leaderboard = response.body()?.data ?: emptyList()
                Log.d(TAG, "✅ Fetched leaderboard: ${leaderboard.size} users")
                Result.success(leaderboard)
            } else {
                Log.e(TAG, "❌ Failed to fetch leaderboard")
                Result.failure(Exception("Failed to fetch leaderboard"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Leaderboard error", e)
            Result.failure(e)
        }
    }

    /**
     * Get current user's rank
     */
    suspend fun getUserRank(): Result<UserRankResponse> {
        return try {
            val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val response = apiService.getUserRank(uid)

            if (response.isSuccessful && response.body()?.success == true) {
                val rankData = response.body()!!
                Log.d(TAG, "✅ User rank: ${rankData.rank}/${rankData.totalUsers}")
                Result.success(rankData)
            } else {
                Result.failure(Exception("Failed to fetch user rank"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ User rank error", e)
            Result.failure(e)
        }
    }
}
