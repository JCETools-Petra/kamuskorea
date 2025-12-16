package com.webtech.learningkorea.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.learningkorea.gamification.Achievement
import com.webtech.learningkorea.gamification.Achievements
import com.webtech.learningkorea.gamification.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementWithStatus(
    val achievement: Achievement,
    val isUnlocked: Boolean,
    val progress: Float = 0f, // 0f to 1f
    val progressText: String = ""
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    val achievementsWithStatus: StateFlow<List<AchievementWithStatus>> =
        gamificationRepository.gamificationState.map { state ->
            Achievements.ALL.map { achievement ->
                val isUnlocked = state.hasAchievement(achievement.id)

                // Calculate progress for locked achievements
                val (progress, progressText) = if (!isUnlocked) {
                    calculateProgress(achievement.id, state.totalXp)
                } else {
                    1f to "Unlocked!"
                }

                AchievementWithStatus(
                    achievement = achievement,
                    isUnlocked = isUnlocked,
                    progress = progress,
                    progressText = progressText
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun calculateProgress(achievementId: String, totalXp: Int): Pair<Float, String> {
        // This is a simplified progress calculation
        // In real app, you'd track specific metrics (PDF count, quiz count, etc.)
        return when (achievementId) {
            "first_pdf" -> 0f to "Open your first PDF"
            "pdf_explorer" -> 0f to "Open 10 different PDFs"
            "first_quiz" -> 0f to "Complete your first quiz"
            "quiz_master_10" -> 0f to "Complete 10 quizzes"
            "quiz_master_50" -> 0f to "Complete 50 quizzes"
            "vocab_10" -> 0f to "Favorite 10 words"
            "vocab_100" -> 0f to "Favorite 100 words"
            "7_day_streak" -> 0f to "Login 7 days in a row"
            "30_day_streak" -> 0f to "Login 30 days in a row"
            "100_day_streak" -> 0f to "Login 100 days in a row"
            else -> 0f to "Locked"
        }
    }
}
