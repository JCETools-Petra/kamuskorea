package com.webtech.learningkorea.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.webtech.learningkorea.ads.BannerAdView
import com.webtech.learningkorea.data.QuizOption
import com.webtech.learningkorea.ui.localization.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHafalanScreen(
    isPremium: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: QuizHafalanViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Hafalan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading -> {
                        // Loading state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.error != null -> {
                        // Error state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = uiState.error ?: "Terjadi kesalahan",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadNextQuestion() }) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                    uiState.currentQuestion != null -> {
                        // Quiz content
                        QuizContent(
                            question = uiState.currentQuestion!!,
                            selectedOption = uiState.selectedOption,
                            showCorrectAnswer = uiState.showCorrectAnswer,
                            autoAdvanceTimeLeft = uiState.autoAdvanceTimeLeft,
                            onOptionSelected = { option -> viewModel.onOptionSelected(option) }
                        )
                    }
                }
            }

            // Banner Ad at the bottom (for non-premium users)
            if (!isPremium) {
                BannerAdView(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun QuizContent(
    question: com.webtech.learningkorea.data.QuizQuestion,
    selectedOption: QuizOption?,
    showCorrectAnswer: Boolean,
    autoAdvanceTimeLeft: Int,
    onOptionSelected: (QuizOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress indicator (countdown timer)
        if (autoAdvanceTimeLeft > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pertanyaan berikutnya dalam: $autoAdvanceTimeLeft detik",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Question card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Answer options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            question.options.forEachIndexed { index, option ->
                AnswerOptionCard(
                    option = option,
                    index = index,
                    isSelected = selectedOption == option,
                    isCorrect = option.isCorrect,
                    showCorrectAnswer = showCorrectAnswer,
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}

@Composable
fun AnswerOptionCard(
    option: QuizOption,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showCorrectAnswer: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected && isCorrect -> Color(0xFF4CAF50) // Green for correct
        isSelected && !isCorrect -> Color(0xFFE57373) // Red for wrong
        showCorrectAnswer && isCorrect -> Color(0xFF81C784) // Light green to show correct answer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        isSelected -> Color.Transparent
        else -> MaterialTheme.colorScheme.outline
    }

    val contentColor = when {
        isSelected || (showCorrectAnswer && isCorrect) -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSelected && !showCorrectAnswer) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Option label (A, B, C, D)
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = contentColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = ('A' + index).toString(),
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Option text
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Checkmark for correct answer
            AnimatedVisibility(
                visible = (isSelected && isCorrect) || (showCorrectAnswer && isCorrect),
                enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Correct",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // X mark for wrong answer
            AnimatedVisibility(
                visible = isSelected && !isCorrect,
                enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Wrong",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
