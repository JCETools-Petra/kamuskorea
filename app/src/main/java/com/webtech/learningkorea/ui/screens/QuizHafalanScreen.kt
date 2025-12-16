package com.webtech.learningkorea.ui.screens

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.webtech.learningkorea.ads.BannerAdView
import com.webtech.learningkorea.data.QuizOption
import com.webtech.learningkorea.ui.localization.LocalStrings

// --- KOMPONEN PEMUTAR AUDIO MINIMALIS ---
@Composable
fun MinimalAudioPlayer(url: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    // Inisialisasi MediaPlayer
    val mediaPlayer = remember(url) {
        MediaPlayer().apply {
            try {
                setDataSource(url)
                prepareAsync()
                setOnCompletionListener { isPlaying = false }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error loading audio: $url", e)
            }
        }
    }

    // Membersihkan resource saat composable dihancurkan
    DisposableEffect(url) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Tampilan Tombol Saja (Bulat)
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        IconButton(
            onClick = {
                try {
                    if (isPlaying) {
                        mediaPlayer.pause()
                    } else {
                        mediaPlayer.start()
                    }
                    isPlaying = !isPlaying
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play Audio",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// --- SCREEN UTAMA ---
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
        // TopBar dihapus agar Full Screen
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    // KONDISI 1: LOADING
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // KONDISI 2: ERROR
                    uiState.error != null -> {
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

                    // KONDISI 3: MENAMPILKAN SOAL
                    uiState.currentQuestion != null -> {
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

            // Iklan Banner di bawah (untuk user non-premium)
            if (!isPremium) {
                BannerAdView(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- KONTEN QUIZ ---
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
        // Indikator Waktu Mundur (Timer)
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

        // KARTU PERTANYAAN
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
                // LOGIKA DETEKSI AUDIO
                val isSoundFile = question.question.contains("sound", ignoreCase = true) ||
                        question.question.endsWith(".mp3", ignoreCase = true) ||
                        question.question.endsWith(".wav", ignoreCase = true) ||
                        question.question.endsWith(".m4a", ignoreCase = true)

                if (isSoundFile) {
                    // JIKA AUDIO: Tampilkan Tombol Play (Tanpa Teks Nama File)
                    MinimalAudioPlayer(url = question.question)
                } else {
                    // JIKA TEKS BIASA: Tampilkan Teks Pertanyaan
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PILIHAN JAWABAN
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

// --- KARTU OPSI JAWABAN ---
@Composable
fun AnswerOptionCard(
    option: QuizOption,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showCorrectAnswer: Boolean,
    onClick: () -> Unit
) {
    // Menentukan warna background berdasarkan status jawaban
    val backgroundColor = when {
        isSelected && isCorrect -> Color(0xFF4CAF50) // Hijau jika benar
        isSelected && !isCorrect -> Color(0xFFE57373) // Merah jika salah
        showCorrectAnswer && isCorrect -> Color(0xFF81C784) // Hijau muda untuk menunjukkan jawaban benar
        else -> MaterialTheme.colorScheme.surfaceVariant
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
                // Label Opsi (1, 2, 3, 4) - SUDAH DIUBAH DARI A, B, C, D
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = contentColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(), // <--- DI SINI PERUBAHANNYA (Menjadi Angka)
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Teks Opsi Jawaban
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Ikon Centang (Jika Benar)
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

            // Ikon Silang (Jika Salah)
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