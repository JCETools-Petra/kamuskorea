package com.webtech.kamuskorea.gamification

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.webtech.kamuskorea.ui.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing daily quests
 */
@Singleton
class DailyQuestRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gamificationRepository: GamificationRepository
) {

    private val dataStore: DataStore<Preferences> = context.dataStore

    companion object {
        private const val TAG = "DailyQuestRepository"
        private val DAILY_QUEST_DATE_KEY = stringPreferencesKey("daily_quest_date")

        // Quest progress keys (stored as JSON or separate keys per quest)
        private fun questProgressKey(questId: String) = intPreferencesKey("quest_progress_$questId")
        private fun questCompletedKey(questId: String) = stringPreferencesKey("quest_completed_$questId")
        private fun questCompletedAtKey(questId: String) = longPreferencesKey("quest_completed_at_$questId")
    }

    /**
     * Get today's date string
     */
    private fun getTodayDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Get current day of week (1=Monday, 7=Sunday)
     */
    private fun getDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert Sunday=1 to Sunday=7, Monday=2 to Monday=1, etc.
        return if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
    }

    /**
     * Observe daily quest state
     */
    val dailyQuestState: Flow<DailyQuestState> = dataStore.data.map { preferences ->
        val savedDate = preferences[DAILY_QUEST_DATE_KEY] ?: ""
        val todayDate = getTodayDate()

        // Get quests for today
        val quests = DailyQuests.getDailyQuests(getDayOfWeek())

        // Build progress map
        val progressMap = quests.associate { quest ->
            val progress = if (savedDate == todayDate) {
                preferences[questProgressKey(quest.id)] ?: 0
            } else {
                0 // Reset if date changed
            }

            val isCompleted = if (savedDate == todayDate) {
                preferences[questCompletedKey(quest.id)] == "true"
            } else {
                false
            }

            val completedAt = if (savedDate == todayDate) {
                preferences[questCompletedAtKey(quest.id)]
            } else {
                null
            }

            quest.id to DailyQuestProgress(
                questId = quest.id,
                currentProgress = progress,
                isCompleted = isCompleted,
                completedAt = completedAt
            )
        }

        val allCompleted = progressMap.values.all { it.isCompleted }

        DailyQuestState(
            date = todayDate,
            quests = quests,
            progress = progressMap,
            allCompleted = allCompleted
        )
    }

    /**
     * Update quest progress
     */
    suspend fun updateQuestProgress(questId: String, increment: Int = 1) {
        val state = dailyQuestState.first()
        val quest = state.quests.find { it.id == questId } ?: return
        val currentProgress = state.getQuestProgress(questId)

        if (currentProgress.isCompleted) {
            Log.d(TAG, "Quest $questId already completed")
            return
        }

        val newProgress = (currentProgress.currentProgress + increment).coerceAtMost(quest.targetProgress)
        val isCompleted = newProgress >= quest.targetProgress

        // Track if quest just completed (to award XP after edit block)
        var questJustCompleted = false

        dataStore.edit { preferences ->
            preferences[DAILY_QUEST_DATE_KEY] = getTodayDate()
            preferences[questProgressKey(questId)] = newProgress
            preferences[questCompletedKey(questId)] = isCompleted.toString()

            if (isCompleted && !currentProgress.isCompleted) {
                preferences[questCompletedAtKey(questId)] = System.currentTimeMillis()
                questJustCompleted = true
                Log.d(TAG, "✅ Quest completed: ${quest.title} (+${quest.xpReward} XP)")
            }
        }

        // Award XP AFTER edit block to avoid nested DataStore update
        if (questJustCompleted) {
            gamificationRepository.addXp(quest.xpReward, "Quest: ${quest.title}")
        }
    }

    /**
     * Reset daily quests (called at midnight or app start)
     */
    suspend fun resetQuestsIfNeeded() {
        val state = dailyQuestState.first()
        val todayDate = getTodayDate()

        if (state.date != todayDate) {
            Log.d(TAG, "🔄 Resetting daily quests for $todayDate")

            dataStore.edit { preferences ->
                preferences[DAILY_QUEST_DATE_KEY] = todayDate

                // Clear all quest progress
                state.quests.forEach { quest ->
                    preferences.remove(questProgressKey(quest.id))
                    preferences.remove(questCompletedKey(quest.id))
                    preferences.remove(questCompletedAtKey(quest.id))
                }
            }
        }
    }

    /**
     * Track word search for quest progress
     */
    suspend fun onWordSearched() {
        updateQuestProgress(DailyQuests.SEARCH_3_WORDS.id, 1)
    }

    /**
     * Track quiz completion for quest progress
     */
    suspend fun onQuizCompleted() {
        updateQuestProgress(DailyQuests.COMPLETE_1_QUIZ.id, 1)
    }

    /**
     * Track favorite saved for quest progress
     */
    suspend fun onFavoriteSaved() {
        updateQuestProgress(DailyQuests.SAVE_2_FAVORITES.id, 1)
    }

    /**
     * Track daily login (maintain streak)
     */
    suspend fun onDailyLogin() {
        updateQuestProgress(DailyQuests.MAINTAIN_STREAK.id, 1)
    }
}
