package com.webtech.kamuskorea.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.webtech.kamuskorea.gamification.GamificationEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

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

    LaunchedEffect(Unit) {
        gamificationEvents
            .onEach { event ->
                when (event) {
                    is GamificationEvent.XpEarned -> {
                        // Show XP earned snackbar
                        snackbarHostState.showSnackbar(
                            message = "🎯 +${event.amount} XP dari ${event.source}!",
                            duration = SnackbarDuration.Short
                        )
                    }

                    is GamificationEvent.LevelUp -> {
                        // Show level up snackbar
                        snackbarHostState.showSnackbar(
                            message = "🎉 Level Up! Sekarang Level ${event.newLevel}!",
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
