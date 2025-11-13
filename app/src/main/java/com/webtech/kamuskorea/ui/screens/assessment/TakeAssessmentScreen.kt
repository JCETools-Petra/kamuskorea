package com.webtech.kamuskorea.ui.screens.assessment

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.unit.sp
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
    onFinish: () -> Unit,
    onExit: () -> Unit = onFinish,
    viewModel: AssessmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Force landscape orientation
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val userAnswers by viewModel.userAnswers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.assessmentResult.collectAsState()
    val error by viewModel.error.collectAsState()

    var showQuestionGrid by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Timer states
    val durationMinutes = remember { mutableStateOf(10) }
    var timeRemaining by remember { mutableStateOf(durationMinutes.value * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(assessmentId) {
        // Only start assessment if questions are not already loaded
        if (questions.isEmpty() && !isLoading && error == null) {
            Log.d("TakeAssessment", "🚀 Starting assessment for ID: $assessmentId")
            viewModel.startAssessment(assessmentId)
        } else {
            Log.d("TakeAssessment", "⏭️ Skipping startAssessment - questions already loaded or loading in progress")
        }
    }

    LaunchedEffect(questions) {
        if (questions.isNotEmpty() && !isTimerRunning) {
            isTimerRunning = true
        }
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (timeRemaining > 0) {
                delay(1000L)
                timeRemaining--
            }
            if (timeRemaining == 0 && result == null) {
                Log.d("TakeAssessment", "⏰ Time's up! Auto-submitting...")
                viewModel.submitAssessment(assessmentId)
                isTimerRunning = false
            }
        }
    }

    LaunchedEffect(result) {
        result?.let {
            Log.d("TakeAssessment", "✅ Result received")
            isTimerRunning = false
            onFinish()
        }
    }

    val currentQuestion = questions.getOrNull(currentIndex)
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)
    val timerColor = when {
        timeRemaining > 120 -> Color(0xFF4CAF50)
        timeRemaining > 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Scaffold(
        topBar = {
            LandscapeTopBar(
                assessmentTitle = assessmentTitle,
                currentIndex = currentIndex,
                totalQuestions = questions.size,
                timeText = timeText,
                timerColor = timerColor,
                onShowGrid = { showQuestionGrid = true },
                onExit = { showExitDialog = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && result == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    ErrorContent(
                        error = error,
                        onRetry = { viewModel.startAssessment(assessmentId) }
                    )
                }
                currentQuestion != null -> {
                    // LANDSCAPE LAYOUT: Soal di KIRI, Jawaban di KANAN
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // LEFT SIDE: Question Content (35% width)
                        Card(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            QuestionContentLandscape(
                                question = currentQuestion,
                                currentIndex = currentIndex,
                                totalQuestions = questions.size
                            )
                        }

                        // RIGHT SIDE: Answer Options + Navigation (65% width)
                        Column(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxHeight()
                        ) {
                            // Answer Options
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    currentQuestion.getOptions().forEach { (letter, text) ->
                                        LandscapeAnswerOption(
                                            letter = letter,
                                            text = text,
                                            isSelected = userAnswers[currentQuestion.id] == letter,
                                            onClick = {
                                                viewModel.saveAnswer(currentQuestion.id, letter)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Navigation Buttons
                            NavigationButtons(
                                currentIndex = currentIndex,
                                totalQuestions = questions.size,
                                onPrevious = { viewModel.previousQuestion() },
                                onNext = { viewModel.nextQuestion() },
                                onFinish = { showSubmitDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Question Grid Dialog
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

    // Submit Dialog
    if (showSubmitDialog) {
        SubmitConfirmationDialog(
            answeredCount = userAnswers.size,
            totalQuestions = questions.size,
            timeText = timeText,
            timerColor = timerColor,
            onConfirm = {
                showSubmitDialog = false
                isTimerRunning = false
                viewModel.submitAssessment(assessmentId)
            },
            onDismiss = { showSubmitDialog = false }
        )
    }

    // Exit Dialog
    if (showExitDialog) {
        ExitConfirmationDialog(
            answeredCount = userAnswers.size,
            totalQuestions = questions.size,
            onConfirm = {
                showExitDialog = false
                isTimerRunning = false
                onExit()
            },
            onDismiss = { showExitDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandscapeTopBar(
    assessmentTitle: String,
    currentIndex: Int,
    totalQuestions: Int,
    timeText: String,
    timerColor: Color,
    onShowGrid: () -> Unit,
    onExit: () -> Unit
) {
    // Compact TopBar - hide title to save space
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Exit Button
            IconButton(
                onClick = onExit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Keluar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Progress Indicator
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${currentIndex + 1}/${totalQuestions}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Timer
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = timerColor.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                        style = MaterialTheme.typography.labelMedium,
                        color = timerColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // View All Questions Button
            OutlinedButton(
                onClick = onShowGrid,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.GridView, "Lihat Semua", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("전체", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QuestionContentLandscape(
    question: Question,
    currentIndex: Int,
    totalQuestions: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Question Number Badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Text(
                text = "Set ${currentIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Question Text
        Text(
            text = question.questionText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Media Content
        when (question.questionType) {
            "image" -> {
                question.mediaUrl?.let { url ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Question Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            "audio" -> {
                question.mediaUrl?.let { url ->
                    AudioPlayerCompact(url)
                }
            }
            "video" -> {
                question.mediaUrl?.let { url ->
                    VideoPlayerCompact(url)
                }
            }
        }
    }
}

@Composable
fun LandscapeAnswerOption(
    letter: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number Circle
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Answer Text
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )

            // Check Icon
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NavigationButtons(
    currentIndex: Int,
    totalQuestions: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Previous Button
        OutlinedButton(
            onClick = onPrevious,
            enabled = currentIndex > 0,
            modifier = Modifier
                .weight(1f)
                .height(38.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("이전", fontSize = 13.sp)
        }

        // Next or Finish Button
        if (currentIndex == totalQuestions - 1) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("마감", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("다음", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AudioPlayerCompact(url: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember(url) {
        MediaPlayer().apply {
            try {
                setDataSource(url)
                prepareAsync()
                setOnCompletionListener { isPlaying = false }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error", e)
            }
        }
    }

    DisposableEffect(url) {
        onDispose { mediaPlayer.release() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) mediaPlayer.pause()
                    else mediaPlayer.start()
                    isPlaying = !isPlaying
                },
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "오디오 문제",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "재생하려면 누르세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun VideoPlayerCompact(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
        }
    }

    DisposableEffect(url) {
        onDispose { exoPlayer.release() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply { player = exoPlayer }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
    }
}

@Composable
fun ErrorContent(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
        Button(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}

@Composable
fun SubmitConfirmationDialog(
    answeredCount: Int,
    totalQuestions: Int,
    timeText: String,
    timerColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val unansweredCount = totalQuestions - answeredCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시험을 제출하시겠습니까?") },
        text = {
            Column {
                Text("총 문제: $totalQuestions")
                Text("답변함: $answeredCount")
                if (unansweredCount > 0) {
                    Text(
                        "미답변: $unansweredCount",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "남은 시간: $timeText",
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("네, 제출")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun ExitConfirmationDialog(
    answeredCount: Int,
    totalQuestions: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    "Keluar dari Ujian?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Ujian anda akan dibatalkan.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons - Horizontal Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            "Lanjutkan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Confirm Button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            "Ya, Keluar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
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
        title = { Text("문제 탐색") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
                Text("닫기")
            }
        }
    )
}