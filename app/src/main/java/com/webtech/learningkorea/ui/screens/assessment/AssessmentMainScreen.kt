package com.webtech.learningkorea.ui.screens.assessment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.webtech.learningkorea.ads.LargeBannerAdView
import com.webtech.learningkorea.R

/**
 * Screen utama untuk memilih jenis assessment (Quiz atau Ujian)
 */
@Composable
fun AssessmentMainScreen(
    onNavigateToQuiz: () -> Unit,
    onNavigateToExam: () -> Unit,
    onNavigateToHistory: () -> Unit,
    isPremium: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            "Latihan & Ujian",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Uji kemampuan bahasa Korea Anda",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quiz Card
        AssessmentTypeCard(
            title = "Quiz",
            description = "Latihan singkat dengan soal pilihan ganda",
            iconResId = R.drawable.ic_quiz_baru,
            gradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
            onClick = onNavigateToQuiz
        )

        // Exam Card (UBT)
        AssessmentTypeCard(
            title = "UBT",
            description = "Ujian lengkap dengan berbagai jenis soal",
            iconResId = R.drawable.ic_ubt_baru,
            gradient = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
            onClick = onNavigateToExam
        )

        // History Card
        AssessmentTypeCard(
            title = "Riwayat",
            description = "Lihat hasil ujian dan quiz sebelumnya",
            iconResId = R.drawable.ic_default_profile,
            gradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
            onClick = onNavigateToHistory
        )

        Spacer(modifier = Modifier.weight(1f))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Tips",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Latihan teratur dengan quiz untuk hasil ujian yang lebih baik!",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Large Banner Ad di bagian bawah
        if (!isPremium) {
            Spacer(modifier = Modifier.height(8.dp))
            LargeBannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AssessmentTypeCard(
    title: String,
    description: String,
    iconResId: Int,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Gradient background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(gradient)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon/Image Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Content
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}