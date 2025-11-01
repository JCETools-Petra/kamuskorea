package com.webtech.kamuskorea.ui.screens.assessment

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.webtech.kamuskorea.data.assessment.Question
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAssessmentScreen(
    assessmentId: Int,
    assessmentTitle: String,
    onFinish: (score: Int, passed: Boolean) -> Unit,
    viewModel: AssessmentViewModel = hiltViewModel()
) {
    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val userAnswers by viewModel.userAnswers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.assessmentResult.collectAsState()
    val error by viewModel.error.collectAsState()

    var showQuestionGrid by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    // ===== TIMER STATES =====
    val durationMinutes = remember { mutableStateOf(10) } // Default 10 minutes, will be updated from assessment
    var timeRemaining by remember { mutableStateOf(durationMinutes.value * 60) } // in seconds
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(assessmentId) {
        viewModel.startAssessment(assessmentId)
    }

    // Start timer when questions are loaded
    LaunchedEffect(questions) {
        if (questions.isNotEmpty() && !isTimerRunning) {
            isTimerRunning = true
            // You can get duration from assessment details if available
            // For now using default 10 minutes
        }
    }

    // ===== COUNTDOWN TIMER =====
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (timeRemaining > 0) {
                delay(1000L)
                timeRemaining--
            }
            // Time's up - auto submit
            if (timeRemaining == 0) {
                Log.d("TakeAssessment", "⏰ Time's up! Auto-submitting...")
                viewModel.submitAssessment(assessmentId)
                isTimerRunning = false
            }
        }
    }

    // Handle hasil submit
    LaunchedEffect(result) {
        result?.let {
            Log.d("TakeAssessment", "✅ Result received: score=${it.score}, passed=${it.passed}")
            isTimerRunning = false
            onFinish(it.score, it.passed)
        }
    }

    // Debug log untuk questions - PENTING UNTUK DEBUGGING ENCODING
    LaunchedEffect(questions) {
        if (questions.isNotEmpty()) {
            Log.d("TakeAssessment", "📝 Questions loaded: ${questions.size}")
            questions.forEachIndexed { index, q ->
                Log.d("TakeAssessment", "Q${index + 1}: ${q.questionText}")
                Log.d("TakeAssessment", "  A: ${q.optionA}")
                Log.d("TakeAssessment", "  B: ${q.optionB}")
            }
        }
    }

    // Handle error
    LaunchedEffect(error) {
        error?.let {
            Log.e("TakeAssessment", "❌ Error: $it")
        }
    }

    val currentQuestion = questions.getOrNull(currentIndex)

    // Format time as MM:SS
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    // Color based on time remaining
    val timerColor = when {
        timeRemaining > 120 -> Color(0xFF4CAF50) // Green
        timeRemaining > 60 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(assessmentTitle, style = MaterialTheme.typography.titleMedium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Soal ${currentIndex + 1} dari ${questions.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            // TIMER DISPLAY
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = timerColor.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = "Timer",
                                        tint = timerColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        timeText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = timerColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showQuestionGrid = true }) {
                        Icon(Icons.Default.GridView, "Lihat Semua Soal")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Memuat soal...")
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            error ?: "Terjadi kesalahan",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.startAssessment(assessmentId) }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                currentQuestion != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Progress bar
                        val progress = (currentIndex + 1).toFloat() / questions.size
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Question content with scroll
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            QuestionContent(
                                question = currentQuestion,
                                selectedAnswer = userAnswers[currentQuestion.id],
                                onAnswerSelected = { answer ->
                                    viewModel.saveAnswer(currentQuestion.id, answer)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Navigation buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.previousQuestion() },
                                enabled = currentIndex > 0
                            ) {
                                Icon(Icons.Default.ChevronLeft, null)
                                Text("Sebelumnya")
                            }

                            if (currentIndex == questions.size - 1) {
                                Button(onClick = { showSubmitDialog = true }) {
                                    Text("Selesai")
                                    Icon(Icons.Default.Check, null)
                                }
                            } else {
                                Button(onClick = { viewModel.nextQuestion() }) {
                                    Text("Selanjutnya")
                                    Icon(Icons.Default.ChevronRight, null)
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        "Tidak ada soal tersedia",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Dialog grid navigasi soal
    if (showQuestionGrid) {
        QuestionGridDialog(
            questions = questions,
            currentIndex = currentIndex,
            userAnswers = userAnswers,
            onQuestionSelected = { index ->
                viewModel.goToQuestion(index)
                showQuestionGrid = false
            },
            onDismiss = { showQuestionGrid = false }
        )
    }

    // Dialog konfirmasi submit
    if (showSubmitDialog) {
        val answeredCount = userAnswers.size
        val totalQuestions = questions.size
        val unansweredCount = totalQuestions - answeredCount

        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Selesaikan Ujian?") },
            text = {
                Column {
                    Text("Total Soal: $totalQuestions")
                    Text("Terjawab: $answeredCount")
                    if (unansweredCount > 0) {
                        Text(
                            "Belum Dijawab: $unansweredCount",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Waktu tersisa: $timeText",
                        fontWeight = FontWeight.Bold,
                        color = timerColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (unansweredCount > 0)
                            "Masih ada soal yang belum dijawab. Yakin ingin menyelesaikan?"
                        else
                            "Yakin ingin menyelesaikan ujian?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitDialog = false
                    isTimerRunning = false
                    Log.d("TakeAssessment", "📤 Submitting assessment...")
                    viewModel.submitAssessment(assessmentId)
                }) {
                    Text("Ya, Selesaikan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun QuestionContent(
    question: Question,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit
) {
    Column {
        // Media content
        when (question.questionType) {
            "image" -> {
                question.mediaUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Question Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            "audio" -> {
                question.mediaUrl?.let { url ->
                    AudioPlayer(url)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            "video" -> {
                question.mediaUrl?.let { url ->
                    VideoPlayer(url)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Question text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options
        question.getOptions().forEach { (letter, text) ->
            AnswerOption(
                letter = letter,
                text = text,
                isSelected = selectedAnswer == letter,
                onClick = { onAnswerSelected(letter) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun AnswerOption(
    letter: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AudioPlayer(url: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember {
        MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer.pause()
                    } else {
                        mediaPlayer.start()
                    }
                    isPlaying = !isPlaying
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Audio Soal", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
fun QuestionGridDialog(
    questions: List<Question>,
    currentIndex: Int,
    userAnswers: Map<Int, String>,
    onQuestionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Navigasi Soal") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(questions) { index, question ->
                    val isAnswered = userAnswers.containsKey(question.id)
                    val isCurrent = index == currentIndex

                    Surface(
                        shape = CircleShape,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isAnswered -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onQuestionSelected(index) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}