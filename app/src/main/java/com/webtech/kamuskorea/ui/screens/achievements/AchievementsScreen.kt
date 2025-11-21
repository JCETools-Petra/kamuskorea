package com.webtech.kamuskorea.ui.screens.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.webtech.kamuskorea.gamification.AchievementCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val achievementsWithStatus by viewModel.achievementsWithStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pencapaian") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary card
            AchievementsSummaryCard(
                totalAchievements = achievementsWithStatus.size,
                unlockedCount = achievementsWithStatus.count { it.isUnlocked },
                modifier = Modifier.padding(16.dp)
            )

            // Achievement grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievementsWithStatus, key = { it.achievement.id }) { item ->
                    AchievementCard(item = item)
                }
            }
        }
    }
}

@Composable
fun AchievementsSummaryCard(
    totalAchievements: Int,
    unlockedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Pencapaian Terbuka",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$unlockedCount / $totalAchievements",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Progress circle
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = if (totalAchievements > 0) unlockedCount.toFloat() / totalAchievements else 0f,
                    modifier = Modifier.size(60.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Text(
                    "${if (totalAchievements > 0) (unlockedCount * 100 / totalAchievements) else 0}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AchievementCard(item: AchievementWithStatus) {
    val categoryColor = when (item.achievement.category) {
        AchievementCategory.LEARNING -> Color(0xFF4CAF50)
        AchievementCategory.STREAK -> Color(0xFFFF9800)
        AchievementCategory.QUIZ -> Color(0xFF2196F3)
        AchievementCategory.VOCABULARY -> Color(0xFFE91E63)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .alpha(if (item.isUnlocked) 1f else 0.6f),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isUnlocked) {
                categoryColor.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon with lock overlay
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (item.isUnlocked) categoryColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(item.achievement.iconRes),
                            contentDescription = null,
                            tint = if (item.isUnlocked) categoryColor else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Lock overlay for locked achievements
                if (!item.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                item.achievement.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (item.isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
            )

            // Description
            Text(
                item.achievement.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (item.isUnlocked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color.Gray.copy(alpha = 0.7f)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // XP reward
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (item.isUnlocked) categoryColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)
            ) {
                Text(
                    "+${item.achievement.xpReward} XP",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isUnlocked) categoryColor else Color.Gray
                )
            }

            // Status
            if (item.isUnlocked) {
                Text(
                    "✓ Terbuka",
                    style = MaterialTheme.typography.labelSmall,
                    color = categoryColor,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    item.progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
