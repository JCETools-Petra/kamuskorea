package com.webtech.kamuskorea.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.webtech.kamuskorea.gamification.DailyQuest
import com.webtech.kamuskorea.gamification.DailyQuestProgress
import com.webtech.kamuskorea.gamification.DailyQuestState
import com.webtech.kamuskorea.gamification.QuestType
import com.webtech.kamuskorea.ui.localization.LocalStrings

/**
 * Card displaying daily quests
 */
@Composable
fun DailyQuestCard(
    questState: DailyQuestState,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var selectedQuest by remember { mutableStateOf<DailyQuest?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.dailyQuest,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Completion badge
                if (questState.allCompleted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                if (strings.language == "Bahasa Interface") "Selesai!" else "Complete!",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        "${questState.getTotalQuestsCompleted()}/${questState.quests.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quest items
            questState.quests.forEach { quest ->
                val progress = questState.getQuestProgress(quest.id)
                val localizedQuest = getLocalizedQuest(quest, strings)
                QuestItem(
                    quest = localizedQuest,
                    progress = progress,
                    onClick = { selectedQuest = localizedQuest }
                )
            }

            // Total XP earned today
            if (questState.getTotalXpEarned() > 0) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (strings.language == "Bahasa Interface") "XP Hari Ini:" else "Today's XP:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "+${questState.getTotalXpEarned()} XP",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Show quest guide dialog
    selectedQuest?.let { quest ->
        QuestGuideDialog(
            quest = quest,
            strings = strings,
            onDismiss = { selectedQuest = null }
        )
    }
}

@Composable
private fun QuestItem(
    quest: DailyQuest,
    progress: DailyQuestProgress,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (progress.isCompleted) {
                Color(0xFF4CAF50).copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            },
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (progress.isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        painterResource(quest.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quest info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (progress.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Quest info",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress bar
                LinearProgressIndicator(
                    progress = { progress.currentProgress.toFloat() / quest.targetProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = if (progress.isCompleted) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Progress text
                Text(
                    "${progress.currentProgress}/${quest.targetProgress}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // XP reward
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (progress.isCompleted) {
                Color(0xFF4CAF50).copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            }
        ) {
            Text(
                "+${quest.xpReward}",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (progress.isCompleted) {
                    Color(0xFF4CAF50)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun QuestGuideDialog(
    quest: DailyQuest,
    strings: com.webtech.kamuskorea.ui.localization.AppStrings,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        quest.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Quest description
                Text(
                    quest.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // How to complete section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        strings.howToComplete,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        quest.guide,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Reward section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.questReward,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "+${quest.xpReward} XP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // OK button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(strings.ok)
                }
            }
        }
    }
}

/**
 * Get localized quest data based on quest type
 */
private fun getLocalizedQuest(
    quest: DailyQuest,
    strings: com.webtech.kamuskorea.ui.localization.AppStrings
): DailyQuest {
    return when (quest.type) {
        QuestType.SEARCH_WORDS -> quest.copy(
            title = strings.questExploreDict,
            description = strings.questExploreDesc,
            guide = strings.questExploreGuide
        )
        QuestType.COMPLETE_QUIZ -> quest.copy(
            title = strings.questCompleteQuiz,
            description = strings.questCompleteQuizDesc,
            guide = strings.questCompleteQuizGuide
        )
        QuestType.SAVE_FAVORITES -> quest.copy(
            title = strings.questSaveFavorite,
            description = strings.questSaveFavoriteDesc,
            guide = strings.questSaveFavoriteGuide
        )
        QuestType.STUDY_STREAK -> quest.copy(
            title = strings.questDailyLogin,
            description = strings.questDailyLoginDesc,
            guide = strings.questDailyLoginGuide
        )
        else -> quest
    }
}
