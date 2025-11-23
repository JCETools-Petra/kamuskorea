package com.webtech.learningkorea.ui.screens

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.learningkorea.data.local.FavoriteVocabularyDao
import com.webtech.learningkorea.data.local.FavoriteWordDao
import com.webtech.learningkorea.notifications.AppNotificationManager
import com.webtech.learningkorea.gamification.GamificationRepository
import com.webtech.learningkorea.gamification.DailyQuestRepository
import com.webtech.learningkorea.gamification.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LearningStatistics(
    val savedWordsCount: Int = 0,
    val quizCompletedCount: Int = 0,
    val streakDays: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val favoriteWordDao: FavoriteWordDao,
    private val favoriteVocabularyDao: FavoriteVocabularyDao,
    private val dataStore: DataStore<Preferences>,
    private val appNotificationManager: AppNotificationManager,
    private val gamificationRepository: GamificationRepository,
    private val dailyQuestRepository: DailyQuestRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "HomeViewModel"

    // Track previous milestone values to detect when milestones are reached
    private var previousStreak = 0
    private var previousQuizCount = 0
    private var previousFavoritesCount = 0

    companion object {
        val QUIZ_COMPLETED_COUNT_KEY = intPreferencesKey("quiz_completed_count")
        val LAST_ACTIVITY_DATE_KEY = longPreferencesKey("last_activity_date")
        val STREAK_DAYS_KEY = intPreferencesKey("streak_days")
        val LAST_DAILY_LOGIN_XP_KEY = longPreferencesKey("last_daily_login_xp")
    }

    // Combined favorites count (dictionary + hafalan)
    val totalFavoritesCount: StateFlow<Int> = combine(
        favoriteWordDao.getAllFavoriteIds(),
        favoriteVocabularyDao.getAllFavoriteIds()
    ) { wordIds, vocabIds ->
        wordIds.size + vocabIds.size
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Quiz completed count from DataStore
    val quizCompletedCount: StateFlow<Int> = dataStore.data.map { preferences ->
        preferences[QUIZ_COMPLETED_COUNT_KEY] ?: 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Streak days from DataStore
    val streakDays: StateFlow<Int> = dataStore.data.map { preferences ->
        preferences[STREAK_DAYS_KEY] ?: 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Combined statistics
    val statistics: StateFlow<LearningStatistics> = combine(
        totalFavoritesCount,
        quizCompletedCount,
        streakDays
    ) { favorites, quizzes, streak ->
        LearningStatistics(
            savedWordsCount = favorites,
            quizCompletedCount = quizzes,
            streakDays = streak
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LearningStatistics()
    )

    // Gamification state
    val gamificationState = gamificationRepository.gamificationState

    // Gamification events (XP earned, level up, achievements)
    val gamificationEvents = gamificationRepository.gamificationEvents

    // Daily quest state
    val dailyQuestState = dailyQuestRepository.dailyQuestState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.webtech.learningkorea.gamification.DailyQuestState(
            date = "",
            quests = emptyList(),
            progress = emptyMap()
        )
    )

    init {
        Log.d(TAG, "HomeViewModel initialized")
        updateStreak()
        awardDailyLoginXp()
        observeMilestones()
        resetDailyQuests()
        markDailyLogin()
    }

    /**
     * Observe statistics changes and trigger milestone notifications
     */
    private fun observeMilestones() {
        viewModelScope.launch {
            statistics.collect { stats ->
                checkAndTriggerMilestones(stats)
            }
        }
    }

    /**
     * Check if any milestone is reached and trigger notification
     */
    private fun checkAndTriggerMilestones(stats: LearningStatistics) {
        // Check streak milestones
        if (stats.streakDays != previousStreak) {
            when (stats.streakDays) {
                7 -> {
                    Log.d(TAG, "🎉 Milestone reached: 7-day streak!")
                    appNotificationManager.showSevenDayStreakMilestone()
                }
                30 -> {
                    Log.d(TAG, "🎉 Milestone reached: 30-day streak!")
                    appNotificationManager.showThirtyDayStreakMilestone()
                }
                100 -> {
                    Log.d(TAG, "🎉 Milestone reached: 100-day streak!")
                    appNotificationManager.showHundredDayStreakMilestone()
                }
            }
            previousStreak = stats.streakDays
        }

        // Check quiz completion milestones
        if (stats.quizCompletedCount != previousQuizCount && stats.quizCompletedCount == 50) {
            Log.d(TAG, "🎉 Milestone reached: 50 quizzes completed!")
            appNotificationManager.showFiftyQuizzesCompletedMilestone()
            previousQuizCount = stats.quizCompletedCount
        }

        // Check favorites milestones
        if (stats.savedWordsCount != previousFavoritesCount && stats.savedWordsCount == 100) {
            Log.d(TAG, "🎉 Milestone reached: 100 words saved!")
            appNotificationManager.showHundredWordsSavedMilestone()
            previousFavoritesCount = stats.savedWordsCount
        }
    }

    /**
     * Increment quiz completed count
     * Call this when user completes a quiz
     */
    fun incrementQuizCompleted() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val currentCount = preferences[QUIZ_COMPLETED_COUNT_KEY] ?: 0
                preferences[QUIZ_COMPLETED_COUNT_KEY] = currentCount + 1
                Log.d(TAG, "Quiz completed count incremented to: ${currentCount + 1}")
            }
            // Update streak when quiz is completed
            recordActivity()
        }
    }

    /**
     * Record user activity for streak tracking
     * Call this when user performs any learning activity
     */
    fun recordActivity() {
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())

            dataStore.edit { preferences ->
                val lastActivity = preferences[LAST_ACTIVITY_DATE_KEY] ?: 0L
                val currentStreak = preferences[STREAK_DAYS_KEY] ?: 0

                val lastActivityDay = getStartOfDay(lastActivity)
                val daysDifference = TimeUnit.MILLISECONDS.toDays(today - lastActivityDay).toInt()

                val newStreak = when {
                    lastActivity == 0L -> 1 // First activity
                    daysDifference == 0 -> currentStreak // Same day, no change
                    daysDifference == 1 -> currentStreak + 1 // Consecutive day
                    else -> 1 // Streak broken, restart
                }

                preferences[LAST_ACTIVITY_DATE_KEY] = today
                preferences[STREAK_DAYS_KEY] = newStreak

                Log.d(TAG, "Activity recorded. Streak: $newStreak days")
            }
        }
    }

    /**
     * Update streak on app open
     * Checks if streak should be reset
     */
    private fun updateStreak() {
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())

            dataStore.edit { preferences ->
                val lastActivity = preferences[LAST_ACTIVITY_DATE_KEY] ?: 0L

                if (lastActivity > 0) {
                    val lastActivityDay = getStartOfDay(lastActivity)
                    val daysDifference = TimeUnit.MILLISECONDS.toDays(today - lastActivityDay).toInt()

                    if (daysDifference > 1) {
                        // Streak broken - more than 1 day since last activity
                        preferences[STREAK_DAYS_KEY] = 0
                        Log.d(TAG, "Streak reset - no activity for $daysDifference days")
                    }
                }
            }
        }
    }

    /**
     * Get the start of the day in milliseconds
     */
    private fun getStartOfDay(timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Reset all statistics (for testing or user request)
     */
    fun resetStatistics() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[QUIZ_COMPLETED_COUNT_KEY] = 0
                preferences[STREAK_DAYS_KEY] = 0
                preferences[LAST_ACTIVITY_DATE_KEY] = 0L
            }
            Log.d(TAG, "Statistics reset")
        }
    }

    /**
     * Award daily login XP (once per day)
     * Called on app open
     */
    private fun awardDailyLoginXp() {
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())

            dataStore.data.map { preferences ->
                preferences[LAST_DAILY_LOGIN_XP_KEY] ?: 0L
            }.collect { lastLoginXpDate ->
                val lastLoginDay = if (lastLoginXpDate > 0) getStartOfDay(lastLoginXpDate) else 0L

                // Award XP if this is the first login of the day
                if (lastLoginDay < today) {
                    gamificationRepository.addXp(XpRewards.DAILY_LOGIN, "daily_login")
                    Log.d(TAG, "⭐ Awarded ${XpRewards.DAILY_LOGIN} XP for daily login")

                    // Update last login XP date
                    dataStore.edit { preferences ->
                        preferences[LAST_DAILY_LOGIN_XP_KEY] = today
                    }
                } else {
                    Log.d(TAG, "Daily login XP already awarded today")
                }
                // Only collect once
                return@collect
            }
        }
    }

    /**
     * Reset daily quests if needed (called on app open)
     */
    private fun resetDailyQuests() {
        viewModelScope.launch {
            try {
                dailyQuestRepository.resetQuestsIfNeeded()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset daily quests", e)
            }
        }
    }

    /**
     * Mark daily login for quest completion
     */
    private fun markDailyLogin() {
        viewModelScope.launch {
            try {
                dailyQuestRepository.onDailyLogin()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark daily login quest", e)
            }
        }
    }
}
