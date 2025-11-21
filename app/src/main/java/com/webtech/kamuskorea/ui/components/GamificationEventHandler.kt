package com.webtech.kamuskorea.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.webtech.kamuskorea.gamification.GamificationEvent
import com.webtech.kamuskorea.ui.localization.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Composable that handles gamification events and shows appropriate UI feedback
 * (Snackbars, dialogs, animations, etc.)
 */
@Composable
fun GamificationEventHandler(
    gamificationEvents: SharedFlow<GamificationEvent>,
    snackbarHostState: SnackbarHostState,
    onLevelUp: ((Int) -> Unit)? = null,
    onAchievementUnlocked: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        gamificationEvents
            .onEach { event ->
                when (event) {
                    is GamificationEvent.XpEarned -> {
                        // Show XP earned snackbar for 3 seconds
                        launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "🎯 +${event.amount} XP ${strings.xpFrom} ${getSourceDisplay(event.source, strings)}!",
                                duration = SnackbarDuration.Short,
                                withDismissAction = false
                            )
                            // Auto dismiss after showing (Short = ~4s, but we'll rely on the built-in timing)
                        }
                    }

                    is GamificationEvent.LevelUp -> {
                        // Show level up snackbar
                        snackbarHostState.showSnackbar(
                            message = "🎉 ${strings.levelUp}! ${strings.nowLevel} ${event.newLevel}!",
                            duration = SnackbarDuration.Long
                        )
                        // Trigger level up callback
                        onLevelUp?.invoke(event.newLevel)
                    }

                    is GamificationEvent.AchievementUnlocked -> {
                        // Show achievement unlocked snackbar
                        snackbarHostState.showSnackbar(
                            message = "🏆 ${event.achievement.title} (+${event.achievement.xpReward} XP)",
                            duration = SnackbarDuration.Long
                        )
                        // Trigger achievement unlocked callback
                        onAchievementUnlocked?.invoke(event.achievement.id)
                    }
                }
            }
            .collect()
    }
}

/**
 * Get display text for XP source
 */
private fun getSourceDisplay(source: String, strings: com.webtech.kamuskorea.ui.localization.AppStrings): String {
    return when {
        source.contains("word_favorited") || source.contains("vocabulary_favorited") -> strings.saveFavorite
        source.contains("quiz") -> strings.completeQuiz
        source.contains("daily_login") -> strings.dailyLogin
        source.contains("Quest") -> strings.questComplete
        else -> source
    }
}
