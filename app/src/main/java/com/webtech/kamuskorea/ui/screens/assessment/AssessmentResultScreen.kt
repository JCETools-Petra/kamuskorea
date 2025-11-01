package com.webtech.kamuskorea.ui.screens.assessment

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webtech.kamuskorea.data.assessment.AssessmentResult
import com.webtech.kamuskorea.data.assessment.QuestionResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentResultScreen(
    result: AssessmentResult,
    onBackToList: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hasil") },
                navigationIcon = {
                    IconButton(onClick = onBackToList) {
                        Icon(Icons.Default.Close, "Tutup")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Score card
            item {
                ScoreCard(result)
            }

            // Summary
            item {
                SummaryCard(result)
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToList,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ke Daftar")
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Coba Lagi")
                    }
                }
            }

            // Detailed results
            item {
                Text(
                    "Pembahasan Soal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(result.details) { index, detail ->
                QuestionResultCard(index + 1, detail)
            }
        }
    }
}

@Composable
fun ScoreCard(result: AssessmentResult) {
    val animatedScore by animateFloatAsState(
        targetValue = result.score.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.passed)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = if (result.passed) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (result.passed) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status
            Text(
                text = if (result.passed) "Lulus!" else "Belum Lulus",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (result.passed)
                    "Selamat! Anda berhasil menyelesaikan ujian ini."
                else
                    "Jangan menyerah! Coba lagi untuk hasil yang lebih baik.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SummaryCard(result: AssessmentResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Ringkasan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            SummaryRow(
                icon = Icons.Default.HelpOutline,
                label = "Total Soal",
                value = "${result.totalQuestions}"
            )
            SummaryRow(
                icon = Icons.Default.CheckCircle,
                label = "Benar",
                value = "${result.correctAnswers}",
                valueColor = Color(0xFF4CAF50)
            )
            SummaryRow(
                icon = Icons.Default.Cancel,
                label = "Salah",
                value = "${result.totalQuestions - result.correctAnswers}",
                valueColor = Color(0xFFF44336)
            )
            SummaryRow(
                icon = Icons.Default.Grade,
                label = "Nilai",
                value = "${result.score}",
                valueColor = if (result.passed) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun QuestionResultCard(number: Int, result: QuestionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isCorrect)
                Color(0xFF4CAF50).copy(alpha = 0.05f)
            else
                Color(0xFFF44336).copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (result.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (result.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$number",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Soal $number",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (result.isCorrect)
                        Color(0xFF4CAF50).copy(alpha = 0.2f)
                    else
                        Color(0xFFF44336).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (result.isCorrect) "BENAR" else "SALAH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Jawaban user
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Jawaban Anda:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    result.userAnswer,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (result.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            // Jawaban benar (jika salah)
            if (!result.isCorrect) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Jawaban Benar:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        result.correctAnswer,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            // Penjelasan
            result.explanation?.let { explanation ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFFFA000)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pembahasan",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            explanation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}