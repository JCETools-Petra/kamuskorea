package com.webtech.learningkorea.ui.screens.assessment

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.webtech.learningkorea.data.assessment.Question
import com.webtech.learningkorea.data.media.MediaPreloader
import com.webtech.learningkorea.ui.components.HtmlText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// Language options for UI display
enum class UILanguage(val displayName: String, val flag: String) {
    KOREAN("한국어", "🇰🇷"),
    INDONESIAN("Indonesia", "🇮🇩"),
    ENGLISH("English", "🇺🇸")
}

enum class AnswerLanguage {
    PRIMARY,    // Default language (as stored in database)
    ALTERNATIVE // Alternative language (Korean/Indonesian)
}

// Helper object for multi-language UI texts
object AssessmentTexts {
    fun submitTitle(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "시험을 제출하시겠습니까?"
        UILanguage.INDONESIAN -> "Kirim jawaban Anda?"
        UILanguage.ENGLISH -> "Submit your answers?"
    }

    fun totalQuestions(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "총 문제"
        UILanguage.INDONESIAN -> "Total soal"
        UILanguage.ENGLISH -> "Total questions"
    }

    fun answered(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "답변함"
        UILanguage.INDONESIAN -> "Dijawab"
        UILanguage.ENGLISH -> "Answered"
    }

    fun unanswered(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "미답변"
        UILanguage.INDONESIAN -> "Belum dijawab"
        UILanguage.ENGLISH -> "Unanswered"
    }

    fun timeRemaining(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "남은 시간"
        UILanguage.INDONESIAN -> "Waktu tersisa"
        UILanguage.ENGLISH -> "Time remaining"
    }

    fun confirmSubmit(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "네, 제출"
        UILanguage.INDONESIAN -> "Ya, kirim"
        UILanguage.ENGLISH -> "Yes, submit"
    }

    fun cancel(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "취소"
        UILanguage.INDONESIAN -> "Batal"
        UILanguage.ENGLISH -> "Cancel"
    }

    fun exitTitle(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "시험에서 나가시겠습니까?"
        UILanguage.INDONESIAN -> "Keluar dari ujian?"
        UILanguage.ENGLISH -> "Exit the exam?"
    }

    fun exitMessage(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "진행 상황이 저장되지 않습니다"
        UILanguage.INDONESIAN -> "Progres Anda tidak akan disimpan"
        UILanguage.ENGLISH -> "Your progress will not be saved"
    }

    fun confirmExit(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "나가기"
        UILanguage.INDONESIAN -> "Keluar"
        UILanguage.ENGLISH -> "Exit"
    }

    fun continueExam(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "계속하기"
        UILanguage.INDONESIAN -> "Lanjutkan"
        UILanguage.ENGLISH -> "Continue"
    }

    fun questionNavigation(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "문제 탐색"
        UILanguage.INDONESIAN -> "Navigasi Soal"
        UILanguage.ENGLISH -> "Question Navigation"
    }

    fun textAndImage(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "📖 읽기 문제"
        UILanguage.INDONESIAN -> "📖 Soal Reading"
        UILanguage.ENGLISH -> "📖 Reading Question"
    }

    fun videoAndAudio(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "🎧 듣기 문제"
        UILanguage.INDONESIAN -> "🎧 Soal Listening"
        UILanguage.ENGLISH -> "🎧 Listening Question"
    }

    fun close(lang: UILanguage) = when(lang) {
        UILanguage.KOREAN -> "닫기"
        UILanguage.INDONESIAN -> "Tutup"
        UILanguage.ENGLISH -> "Close"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAssessmentScreen(
    assessmentId: Int,
    assessmentTitle: String,
    durationMinutes: Int,
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var answerLanguage by remember { mutableStateOf(AnswerLanguage.PRIMARY) }
    var uiLanguage by remember { mutableStateOf(UILanguage.KOREAN) }

    // Block back button during quiz - show exit dialog instead
    BackHandler(enabled = questions.isNotEmpty() && result == null) {
        showExitDialog = true
    }

    // Timer states - using server-provided duration
    var timeRemaining by remember { mutableStateOf(durationMinutes * 60) }
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
            Log.d("TakeAssessment", "⏱️ Starting timer with duration: $durationMinutes minutes (${durationMinutes * 60} seconds)")
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
                uiLanguage = uiLanguage,
                onShowLanguageDialog = { showLanguageDialog = true },
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
                        // LEFT SIDE: Question Content (60% width)
                        Card(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            QuestionContentLandscape(
                                question = currentQuestion,
                                currentIndex = currentIndex,
                                totalQuestions = questions.size,
                                mediaPreloader = viewModel.getMediaPreloader()
                            )
                        }

                        // RIGHT SIDE: Answer Options + Navigation (40% width)
                        Column(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight()
                        ) {
                            // Answer Options - Scrollable for image/audio options
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
                                        .verticalScroll(rememberScrollState())
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val options = if (answerLanguage == AnswerLanguage.ALTERNATIVE)
                                        currentQuestion.getOptionsAlt()
                                    else
                                        currentQuestion.getOptions()

                                    options.forEachIndexed { index, (letter, text) ->
                                        LandscapeAnswerOption(
                                            letter = letter,
                                            text = text,
                                            isSelected = userAnswers[currentQuestion.id] == letter,
                                            onClick = {
                                                viewModel.saveAnswer(currentQuestion.id, letter)
                                            },
                                            optionType = currentQuestion.getOptionType(index),
                                            mediaPreloader = viewModel.getMediaPreloader()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Navigation Buttons
                            NavigationButtons(
                                currentIndex = currentIndex,
                                totalQuestions = questions.size,
                                uiLanguage = uiLanguage,
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

    // Dialogs
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiLanguage,
            onSelectLanguage = { selectedLang ->
                uiLanguage = selectedLang
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showQuestionGrid) {
        QuestionGridDialog(
            questions = questions,
            currentIndex = currentIndex,
            userAnswers = userAnswers,
            uiLanguage = uiLanguage,
            onQuestionSelected = { index ->
                viewModel.goToQuestion(index)
                showQuestionGrid = false
            },
            onDismiss = { showQuestionGrid = false }
        )
    }

    if (showSubmitDialog) {
        SubmitConfirmationDialog(
            answeredCount = userAnswers.size,
            totalQuestions = questions.size,
            timeText = timeText,
            timerColor = timerColor,
            uiLanguage = uiLanguage,
            onConfirm = {
                showSubmitDialog = false
                isTimerRunning = false
                viewModel.submitAssessment(assessmentId)
            },
            onDismiss = { showSubmitDialog = false }
        )
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            answeredCount = userAnswers.size,
            totalQuestions = questions.size,
            uiLanguage = uiLanguage,
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
    uiLanguage: UILanguage = UILanguage.KOREAN,
    onShowLanguageDialog: () -> Unit = {},
    onShowGrid: () -> Unit,
    onExit: () -> Unit
) {
    // Localized text based on UI language
    val allQuestionsText = when (uiLanguage) {
        UILanguage.KOREAN -> "전체"
        UILanguage.INDONESIAN -> "Semua"
        UILanguage.ENGLISH -> "All"
    }

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

            // Language Selector Button - Always visible
            FilledTonalButton(
                onClick = onShowLanguageDialog,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = uiLanguage.flag,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = uiLanguage.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Change Language",
                    modifier = Modifier.size(16.dp)
                )
            }

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
                Icon(Icons.Default.GridView, "View All Questions", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(allQuestionsText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QuestionContentLandscape(
    question: Question,
    currentIndex: Int,
    totalQuestions: Int,
    mediaPreloader: MediaPreloader? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()) // ✅ Enable scrolling for long questions
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

        Spacer(modifier = Modifier.height(8.dp))

        // Question Text - ✅ Optimized for long paragraph/story questions, with HTML support
        HtmlText(
            html = question.questionText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Normal, // Changed from Bold to reduce visual fatigue
                lineHeight = 26.sp // Increased from 22.sp for better paragraph spacing
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
            // No maxLines restriction - allows unlimited length
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Media Content with Loading States - Support multiple media URLs
        val allMediaUrls = question.getAllMediaUrls()
        if (allMediaUrls.isNotEmpty()) {
            allMediaUrls.forEachIndexed { index, url ->
                // Determine media type from URL extension
                val isImage = url.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)$", RegexOption.IGNORE_CASE))
                val isAudio = url.matches(Regex(".*\\.(mp3|wav|ogg|webm|m4a)$", RegexOption.IGNORE_CASE))
                val isVideo = url.matches(Regex(".*\\.(mp4|webm|ogv)$", RegexOption.IGNORE_CASE))

                when {
                    isImage -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = url,
                                contentDescription = "Question Image ${index + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                contentScale = ContentScale.Fit,
                                loading = {
                                    ShimmerPlaceholder(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                    )
                                },
                                error = {
                                    MediaErrorPlaceholder(type = "image")
                                }
                            )
                        }
                        if (index < allMediaUrls.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    isAudio -> {
                        AudioPlayerCompactCached(url, mediaPreloader)
                        if (index < allMediaUrls.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    isVideo -> {
                        VideoPlayerCompactCached(url, mediaPreloader)
                        if (index < allMediaUrls.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
// TakeAssessmentScreen - Part 2 (lanjutan dari Part 1)

@Composable
fun LandscapeAnswerOption(
    letter: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    optionType: String = "text", // "text", "image", or "audio"
    mediaPreloader: MediaPreloader? = null
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

            // Answer Content - Support for text, image, or audio
            when (optionType) {
                "image" -> {
                    SubcomposeAsyncImage(
                        model = text,
                        contentDescription = "Option Image",
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 80.dp),
                        contentScale = ContentScale.Fit,
                        loading = {
                            ShimmerPlaceholder(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            )
                        },
                        error = {
                            MediaErrorPlaceholder(type = "image")
                        }
                    )
                }
                "audio" -> {
                    Column(modifier = Modifier.weight(1f)) {
                        AudioPlayerCompactCached(text, mediaPreloader)
                    }
                }
                else -> {
                    // Text option with HTML rendering
                    HtmlText(
                        html = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 20.sp // Better line spacing for multi-line answers
                        ),
                        modifier = Modifier.weight(1f)
                        // No maxLines restriction - allows long answer text
                    )
                }
            }

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
    uiLanguage: UILanguage = UILanguage.KOREAN,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    // Localized button text
    val previousText = when (uiLanguage) {
        UILanguage.KOREAN -> "이전"
        UILanguage.INDONESIAN -> "Sebelumnya"
        UILanguage.ENGLISH -> "Previous"
    }
    val nextText = when (uiLanguage) {
        UILanguage.KOREAN -> "다음"
        UILanguage.INDONESIAN -> "Selanjutnya"
        UILanguage.ENGLISH -> "Next"
    }
    val finishText = when (uiLanguage) {
        UILanguage.KOREAN -> "마감"
        UILanguage.INDONESIAN -> "Selesai"
        UILanguage.ENGLISH -> "Finish"
    }

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
            Text(previousText, fontSize = 13.sp)
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
                Text(finishText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                Text(nextText, fontSize = 13.sp)
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
    uiLanguage: UILanguage = UILanguage.KOREAN,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val unansweredCount = totalQuestions - answeredCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AssessmentTexts.submitTitle(uiLanguage)) },
        text = {
            Column {
                Text("${AssessmentTexts.totalQuestions(uiLanguage)}: $totalQuestions")
                Text("${AssessmentTexts.answered(uiLanguage)}: $answeredCount")
                if (unansweredCount > 0) {
                    Text(
                        "${AssessmentTexts.unanswered(uiLanguage)}: $unansweredCount",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${AssessmentTexts.timeRemaining(uiLanguage)}: $timeText",
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(AssessmentTexts.confirmSubmit(uiLanguage))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AssessmentTexts.cancel(uiLanguage))
            }
        }
    )
}

@Composable
fun ExitConfirmationDialog(
    answeredCount: Int,
    totalQuestions: Int,
    uiLanguage: UILanguage = UILanguage.KOREAN,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .heightIn(max = 600.dp), // Add max height constraint
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // Make scrollable
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
                    AssessmentTexts.exitTitle(uiLanguage),
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
                        AssessmentTexts.exitMessage(uiLanguage),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            AssessmentTexts.continueExam(uiLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

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
                            AssessmentTexts.confirmExit(uiLanguage),
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
    uiLanguage: UILanguage = UILanguage.KOREAN,
    onQuestionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Group questions by type
    val textImageQuestions = questions.mapIndexedNotNull { index, question ->
        if (question.questionType == "text" || question.questionType == "image") {
            index to question
        } else null
    }

    val audioVideoQuestions = questions.mapIndexedNotNull { index, question ->
        if (question.questionType == "audio" || question.questionType == "video") {
            index to question
        } else null
    }

    // PERBAIKAN DISINI: Tambahkan properties
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false // <-- INI KUNCINYA: Mematikan batas lebar bawaan
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f) // Mengambil 95% lebar layar (Jauh lebih lebar)
                .fillMaxHeight(0.92f), // Mengambil 92% tinggi layar untuk muat 50 soal
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp) // Reduced from 35dp/28dp
            ) {
                // Title - Compact
                Text(
                    AssessmentTexts.questionNavigation(uiLanguage),
                    style = MaterialTheme.typography.titleLarge, // Reduced from headlineMedium
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp) // Reduced from 20dp
                )

                // Content - No scroll to fit 50 questions
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Reduced from 24dp
                ) {
                    // Text and Image Questions Section (Left)
                    if (textImageQuestions.isNotEmpty()) {
                        QuestionCategorySection(
                            title = AssessmentTexts.textAndImage(uiLanguage),
                            titleColor = Color(0xFF2196F3),
                            questions = textImageQuestions,
                            currentIndex = currentIndex,
                            userAnswers = userAnswers,
                            onQuestionSelected = onQuestionSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Audio and Video Questions Section (Right)
                    if (audioVideoQuestions.isNotEmpty()) {
                        QuestionCategorySection(
                            title = AssessmentTexts.videoAndAudio(uiLanguage),
                            titleColor = Color(0xFFFF5722),
                            questions = audioVideoQuestions,
                            currentIndex = currentIndex,
                            userAnswers = userAnswers,
                            onQuestionSelected = onQuestionSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Close button - Compact
                Spacer(modifier = Modifier.height(8.dp)) // Reduced from 20dp
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(40.dp) // Reduced from 52dp
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        AssessmentTexts.close(uiLanguage),
                        style = MaterialTheme.typography.labelLarge, // Smaller font
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionCategorySection(
    title: String,
    titleColor: Color,
    questions: List<Pair<Int, Question>>,
    currentIndex: Int,
    userAnswers: Map<Int, String>,
    onQuestionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp) // Reduced from 12dp
    ) {
        // Category Header - Compact
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = titleColor.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp), // Reduced from 16dp
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium, // Reduced from titleLarge
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    fontSize = 14.sp // Explicit smaller size
                )
                Surface(
                    shape = CircleShape,
                    color = titleColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${questions.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), // Reduced
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Question Grid - 10 columns for 50 questions (5 rows max per category)
        LazyVerticalGrid(
            columns = GridCells.Fixed(10), // Increased from 5 to fit more
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp), // Reduced from 600dp
            horizontalArrangement = Arrangement.spacedBy(6.dp), // Reduced from 12dp
            verticalArrangement = Arrangement.spacedBy(6.dp), // Reduced from 12dp
            userScrollEnabled = false
        ) {
            items(questions.size) { index ->
                val (originalIndex, question) = questions[index]
                val isAnswered = userAnswers.containsKey(question.id)
                val isCurrent = originalIndex == currentIndex

                QuestionNumberBubble(
                    number = originalIndex + 1,
                    isCurrent = isCurrent,
                    isAnswered = isAnswered,
                    questionType = question.questionType,
                    onClick = { onQuestionSelected(originalIndex) }
                )
            }
        }
    }
}

@Composable
fun QuestionNumberBubble(
    number: Int,
    isCurrent: Boolean,
    isAnswered: Boolean,
    questionType: String,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isAnswered -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp), // Reduced from 12dp for compact look
        color = backgroundColor,
        modifier = Modifier
            .size(38.dp) // Reduced from 56dp to fit 10 columns
            .clickable(onClick = onClick),
        shadowElevation = if (isCurrent) 3.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main content - centered
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "$number",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            // Checkmark icon for answered questions - top right corner
            if (isAnswered && !isCurrent) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Answered",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

// ========== SHIMMER AND CACHED MEDIA PLAYERS ==========

/**
 * Shimmer placeholder for loading media content
 */
@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(brush)
    ) {
        // Loading indicator in center
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Loading...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Error placeholder for failed media loading
 */
@Composable
fun MediaErrorPlaceholder(type: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                when (type) {
                    "image" -> Icons.Default.BrokenImage
                    "audio" -> Icons.Default.MusicOff
                    else -> Icons.Default.ErrorOutline
                },
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Failed to load $type",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Audio player that uses cached files when available
 */
@Composable
fun AudioPlayerCompactCached(url: String, mediaPreloader: MediaPreloader?) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Check for cached file first
    val audioSource = remember(url) {
        mediaPreloader?.getCachedFilePath(url, "audio") ?: url
    }

    val mediaPlayer = remember(audioSource) {
        MediaPlayer().apply {
            try {
                if (audioSource.startsWith("/")) {
                    // Local cached file
                    setDataSource(audioSource)
                    prepare() // Synchronous for local files
                    isLoading = false
                } else {
                    // Stream from URL
                    setDataSource(audioSource)
                    setOnPreparedListener { isLoading = false }
                    setOnErrorListener { _, _, _ ->
                        hasError = true
                        isLoading = false
                        true
                    }
                    prepareAsync()
                }
                setOnCompletionListener { isPlaying = false }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error initializing", e)
                hasError = true
                isLoading = false
            }
        }
    }

    DisposableEffect(audioSource) {
        onDispose { mediaPlayer.release() }
    }

    if (hasError) {
        MediaErrorPlaceholder(type = "audio")
    } else {
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
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                } else {
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
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "오디오 문제",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (mediaPreloader?.isCached(url) == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "Cached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        if (isLoading) "Loading..." else "재생하려면 누르세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Video player that uses cached files when available
 */
@Composable
fun VideoPlayerCompactCached(url: String, mediaPreloader: MediaPreloader?) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Check for cached file first
    val videoSource = remember(url) {
        mediaPreloader?.getCachedFilePath(url, "video") ?: url
    }

    val exoPlayer = remember(videoSource) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val mediaItem = if (videoSource.startsWith("/")) {
                    // Local cached file
                    MediaItem.fromUri(Uri.fromFile(File(videoSource)))
                } else {
                    // Stream from URL
                    MediaItem.fromUri(Uri.parse(videoSource))
                }
                setMediaItem(mediaItem)
                addListener(object : com.google.android.exoplayer2.Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            com.google.android.exoplayer2.Player.STATE_READY -> isLoading = false
                            com.google.android.exoplayer2.Player.STATE_ENDED -> {}
                        }
                    }

                    override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                        hasError = true
                        isLoading = false
                        Log.e("VideoPlayer", "Error: ${error.message}")
                    }
                })
                prepare()
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Error initializing", e)
                hasError = true
                isLoading = false
            }
        }
    }

    DisposableEffect(videoSource) {
        onDispose { exoPlayer.release() }
    }

    if (hasError) {
        MediaErrorPlaceholder(type = "video")
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply { player = exoPlayer }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                // Loading overlay
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Cache indicator
                if (mediaPreloader?.isCached(url) == true && !isLoading) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.8f)
                    ) {
                        Text(
                            "Cached",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Language selection dialog for quiz UI
 * Allows switching between Korean, Indonesian, and English
 */
@Composable
fun LanguageSelectionDialog(
    currentLanguage: UILanguage,
    onSelectLanguage: (UILanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Language",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Horizontal layout for language options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UILanguage.entries.forEach { language ->
                        LanguageOptionItemCompact(
                            language = language,
                            isSelected = language == currentLanguage,
                            onClick = { onSelectLanguage(language) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun LanguageOptionItemCompact(
    language: UILanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Flag emoji
            Text(
                text = language.flag,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Language name
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Check icon for selected
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
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

/**
 * Composable to render HTML text (supports <p>, <u>, <b>, <i>, etc.)
 * Used for questions and answers that may contain HTML formatting from rich text editor
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Start
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            android.widget.TextView(context).apply {
                // Set text style properties
                textSize = style.fontSize.value
                setTextColor(color.hashCode())
                gravity = when (textAlign) {
                    TextAlign.Center -> android.view.Gravity.CENTER
                    TextAlign.End -> android.view.Gravity.END
                    else -> android.view.Gravity.START
                }

                // Set font weight
                typeface = when {
                    style.fontWeight == FontWeight.Bold -> android.graphics.Typeface.DEFAULT_BOLD
                    else -> android.graphics.Typeface.DEFAULT
                }
            }
        },
        update = { textView ->
            // Render HTML content
            val spanned = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(html)
            }
            textView.text = spanned
        }
    )
}