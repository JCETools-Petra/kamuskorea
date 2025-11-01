package com.webtech.kamuskorea.ui.screens.assessment

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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

    var showQuestionGrid by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(assessmentId) {
        viewModel.startAssessment(assessmentId)
    }

    // Handle hasil submit
    LaunchedEffect(result) {
        result?.let {
            onFinish(it.score, it.passed)
        }
    }

    val currentQuestion = questions.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(assessmentTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Soal ${currentIndex + 1} dari ${questions.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (currentQuestion != null) {
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