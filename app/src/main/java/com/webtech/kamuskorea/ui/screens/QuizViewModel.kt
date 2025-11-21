package com.webtech.kamuskorea.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.gamification.DailyQuestRepository
import com.webtech.kamuskorea.gamification.GamificationRepository
import com.webtech.kamuskorea.gamification.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val dailyQuestRepository: DailyQuestRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val TAG = "QuizViewModel"

    /**
     * Called when user completes a quiz
     * Tracks quest progress and awards XP
     */
    fun onQuizCompleted(score: Int, totalQuestions: Int) {
        viewModelScope.launch {
            try {
                // Track daily quest progress
                dailyQuestRepository.onQuizCompleted()
                Log.d(TAG, "✅ Quiz completion tracked for daily quest")

                // Award XP based on score
                val xpReward = calculateQuizXp(score, totalQuestions)
                gamificationRepository.addXp(xpReward, "quiz_completed")
                Log.d(TAG, "⭐ Awarded $xpReward XP for quiz completion (score: $score/$totalQuestions)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track quiz completion", e)
            }
        }
    }

    /**
     * Calculate XP reward based on quiz score
     */
    private fun calculateQuizXp(score: Int, totalQuestions: Int): Int {
        if (totalQuestions == 0) return 0

        val percentage = (score.toFloat() / totalQuestions) * 100
        return when {
            percentage >= 90 -> XpRewards.QUIZ_COMPLETED * 2 // Perfect/Excellent: 2x XP
            percentage >= 70 -> XpRewards.QUIZ_COMPLETED      // Good: Normal XP
            percentage >= 50 -> XpRewards.QUIZ_COMPLETED / 2  // Pass: Half XP
            else -> 10 // Failed but still get consolation XP
        }
    }
}
