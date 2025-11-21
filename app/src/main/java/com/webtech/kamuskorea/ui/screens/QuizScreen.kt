package com.webtech.kamuskorea.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// 1. Definisikan struktur data untuk setiap pertanyaan kuis
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

// Contoh data kuis (bisa diganti dengan data dari database/API nanti)
val sampleQuestions = listOf(
    QuizQuestion(
        question = "Apa arti dari '안녕하세요' (Annyeonghaseyo)?",
        options = listOf("Terima kasih", "Halo", "Maaf", "Selamat tinggal"),
        correctAnswerIndex = 1
    ),
    QuizQuestion(
        question = "Bagaimana cara mengucapkan 'Terima kasih' dalam bahasa Korea?",
        options = listOf("미안합니다 (Mianhamnida)", "주세요 (Juseyo)", "감사합니다 (Kamsahamnida)", "네 (Ne)"),
        correctAnswerIndex = 2
    ),
    QuizQuestion(
        question = "Kata '사랑해' (Saranghae) memiliki arti...",
        options = listOf("Aku benci kamu", "Aku lapar", "Siapa namamu?", "Aku cinta kamu"),
        correctAnswerIndex = 3
    )
)

@Composable
fun QuizScreen(
    isPremium: Boolean,
    onNavigateToProfile: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isPremium) {
            // Jika pengguna premium, tampilkan konten kuis yang fungsional
            QuizContent(viewModel = viewModel)
        } else {
            // Jika bukan, tampilkan pesan untuk upgrade
            Text(
                "Fitur Latihan Soal hanya untuk anggota premium.",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = onNavigateToProfile) {
                Text("Upgrade ke Premium")
            }
        }
    }
}

@Composable
fun QuizContent(viewModel: QuizViewModel) {
    // 2. State untuk mengelola logika kuis
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerChecked by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }

    val currentQuestion = sampleQuestions[currentQuestionIndex]

    if (isQuizFinished) {
        // Tampilan jika kuis sudah selesai
        QuizResultScreen(
            score = score,
            totalQuestions = sampleQuestions.size,
            onRestart = {
                // Reset semua state untuk memulai kuis dari awal
                currentQuestionIndex = 0
                selectedAnswerIndex = null
                isAnswerChecked = false
                score = 0
                isQuizFinished = false
            }
        )
    } else {
        // Tampilan selama kuis berlangsung
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Soal ${currentQuestionIndex + 1} dari ${sampleQuestions.size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = currentQuestion.question,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // 3. Tampilkan Pilihan Jawaban
            currentQuestion.options.forEachIndexed { index, option ->
                val isCorrect = index == currentQuestion.correctAnswerIndex
                val isSelected = index == selectedAnswerIndex

                // Tentukan warna berdasarkan status jawaban
                val backgroundColor = when {
                    isAnswerChecked && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                    isAnswerChecked && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = !isAnswerChecked) {
                            selectedAnswerIndex = index
                        },
                    colors = CardDefaults.cardColors(containerColor = backgroundColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { if (!isAnswerChecked) selectedAnswerIndex = index }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. Tombol Aksi (Periksa Jawaban / Lanjut)
            Button(
                onClick = {
                    if (isAnswerChecked) {
                        // Jika sudah diperiksa, lanjut ke soal berikutnya atau selesaikan kuis
                        if (currentQuestionIndex < sampleQuestions.size - 1) {
                            currentQuestionIndex++
                            selectedAnswerIndex = null
                            isAnswerChecked = false
                        } else {
                            // Quiz selesai - track quest dan award XP
                            isQuizFinished = true
                            viewModel.onQuizCompleted(score, sampleQuestions.size)
                        }
                    } else {
                        // Jika belum, periksa jawaban yang dipilih
                        if (selectedAnswerIndex != null) {
                            if (selectedAnswerIndex == currentQuestion.correctAnswerIndex) {
                                score++
                            }
                            isAnswerChecked = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAnswerIndex != null // Tombol aktif hanya jika ada jawaban yang dipilih
            ) {
                Text(if (isAnswerChecked) "Lanjut" else "Periksa Jawaban")
            }
        }
    }
}

@Composable
fun QuizResultScreen(score: Int, totalQuestions: Int, onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kuis Selesai!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Skor Anda: $score / $totalQuestions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRestart) {
            Text("Ulangi Kuis")
        }
    }
}