package com.webtech.learningkorea.ui.screens.assessment

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
import androidx.compose.ui.graphics.toArgb
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
import com.webtech.learningkorea.MainActivity
import com.webtech.learningkorea.ads.BannerAdView
import com.webtech.learningkorea.data.assessment.Question
import com.webtech.learningkorea.data.media.MediaPreloader
import com.webtech.learningkorea.ui.components.HtmlText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    assessmentType: String = "exam", // "quiz" or "exam"
    onFinish: () -> Unit,
    onExit: () -> Unit = onFinish,
    viewModel: AssessmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Get current user name from Firebase
    val userName = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
    }

    // Force landscape orientation and enable fullscreen mode
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        (activity as? MainActivity)?.enableFullscreenMode()
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            (activity as? MainActivity)?.disableFullscreenMode()
        }
    }

    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val userAnswers by viewModel.userAnswers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.assessmentResult.collectAsState()
    val error by viewModel.error.collectAsState()

    // Mengambil judul dinamis dari ViewModel
    val dynamicTitle by viewModel.currentAssessmentTitle.collectAsState()
    val finalDisplayTitle = dynamicTitle ?: assessmentTitle

    var showQuestionGrid by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var answerLanguage by remember { mutableStateOf(AnswerLanguage.PRIMARY) }
    var uiLanguage by remember { mutableStateOf(UILanguage.KOREAN) }

    // Block back button during quiz
    BackHandler(enabled = questions.isNotEmpty() && result == null) {
        showExitDialog = true
    }

    // Timer states
    var timeRemaining by remember { mutableStateOf(durationMinutes * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(assessmentId) {
        if (questions.isEmpty() && !isLoading && error == null) {
            Log.d("TakeAssessment", "🚀 Starting assessment for ID: $assessmentId")
            viewModel.startAssessment(assessmentId)
        }
    }

    LaunchedEffect(questions) {
        if (questions.isNotEmpty() && !isTimerRunning) {
            isTimerRunning = true
        }
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (timeRemaining > 0 && isActive) {
                delay(1000L)
                timeRemaining--
            }
            if (timeRemaining == 0 && result == null && !isLoading) {
                viewModel.submitAssessment(assessmentId)
                isTimerRunning = false
            }
        }
    }

    LaunchedEffect(result) {
        result?.let {
            isTimerRunning = false
            delay(500L)
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
                assessmentTitle = finalDisplayTitle,
                assessmentType = assessmentType,
                userName = userName,
                currentIndex = currentIndex,
                totalQuestions = questions.size,
                timeText = timeText,
                timerColor = timerColor,
                uiLanguage = uiLanguage,
                onShowLanguageDialog = { showLanguageDialog = true },
                onShowGrid = { showQuestionGrid = true },
                onExit = { showExitDialog = true },
                answeredCount = userAnswers.size
            )
        },
        bottomBar = {
            // Navigation Bar at bottom (Hanya 1 baris sekarang)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationButtons(
                    currentIndex = currentIndex,
                    totalQuestions = questions.size,
                    uiLanguage = uiLanguage,
                    onPrevious = { viewModel.previousQuestion() },
                    onNext = { viewModel.nextQuestion() },
                    onFinish = { showSubmitDialog = true },
                    onShowGrid = { showQuestionGrid = true }
                )
            }
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
                    // LANDSCAPE LAYOUT
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // LEFT SIDE: Question
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

                        // RIGHT SIDE: Answers
                        Card(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight(),
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
            assessmentTitle = finalDisplayTitle,
            assessmentType = assessmentType,
            userName = userName,
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
    assessmentType: String = "exam",
    userName: String,
    currentIndex: Int,
    totalQuestions: Int,
    timeText: String,
    timerColor: Color,
    uiLanguage: UILanguage = UILanguage.KOREAN,
    onShowLanguageDialog: () -> Unit = {},
    onShowGrid: () -> Unit,
    onExit: () -> Unit,
    answeredCount: Int = 0
) {
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
            // Assessment info - sejajar horizontal
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type Label (Quiz atau UBT)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (assessmentType == "quiz") "Quiz" else "UBT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Divider
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Assessment Title
                Text(
                    text = assessmentTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Divider
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // User Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "User",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Remaining Questions Indicator
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val remainingText = when (uiLanguage) {
                        UILanguage.KOREAN -> "남은"
                        UILanguage.INDONESIAN -> "Tersisa"
                        UILanguage.ENGLISH -> "Remaining"
                    }
                    val remaining = totalQuestions - answeredCount

                    Text(
                        text = "$remainingText:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Time Remaining Indicator
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val timeLabel = when (uiLanguage) {
                        UILanguage.KOREAN -> "시간"
                        UILanguage.INDONESIAN -> "Waktu"
                        UILanguage.ENGLISH -> "Time"
                    }

                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "$timeLabel:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = timerColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
    // Membersihkan teks soal dari tag HTML kosong di akhir
    val cleanedQuestionText = remember(question.questionText) {
        var text = question.questionText
        while (text.matches(Regex(".*(<br\\s*/?>|&nbsp;|\\s|<p>\\s*</p>)$", RegexOption.DOT_MATCHES_ALL))) {
            text = text.replace(Regex("(<br\\s*/?>|&nbsp;|\\s|<p>\\s*</p>)+$", RegexOption.DOT_MATCHES_ALL), "")
        }
        text.trim()
    }

    // Default posisi adalah 'top' jika null
    val boxPosition = question.boxPosition ?: "top"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // =================================================
        // POSISI 1: TOP (Di Atas Segalanya / Sebelum Soal)
        // =================================================
        if (boxPosition == "top") {
            PromptBoxComponent(question, mediaPreloader)
            Spacer(modifier = Modifier.height(4.dp)) // Kurangi spacing ke 4dp
        }

        // =================================================
        // ELEMEN A: TEKS SOAL (Selalu dirender di sini)
        // =================================================
        HtmlText(
            html = cleanedQuestionText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Normal,
                lineHeight = 26.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp)) // Tambahkan sedikit jarak setelah soal

        // =================================================
        // POSISI 2: MIDDLE (Di Bawah Soal, Di Atas Gambar)
        // =================================================
        if (boxPosition == "middle") {
            Spacer(modifier = Modifier.height(8.dp))
            PromptBoxComponent(question, mediaPreloader)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // =================================================
        // ELEMEN B: MEDIA UTAMA (Gambar/Video/Audio Soal)
        // =================================================
        val allMediaUrls = question.getAllMediaUrls()
        // Filter agar media yang dipakai di dalam kotak tidak muncul ganda di sini
        val mainMediaUrls = allMediaUrls.filter { it != question.boxMediaUrl }

        if (mainMediaUrls.isNotEmpty()) {
            mainMediaUrls.forEachIndexed { index, url ->
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
                                contentDescription = "Question Media ${index + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp),
                                contentScale = ContentScale.Fit,
                                loading = { ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(150.dp)) },
                                error = { MediaErrorPlaceholder(type = "image") }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    isAudio -> {
                        AudioPlayerCompactCached(url, mediaPreloader)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    isVideo -> {
                        VideoPlayerCompactCached(url, mediaPreloader)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // =================================================
        // POSISI 3: BOTTOM (Di Bawah Gambar/Video)
        // =================================================
        if (boxPosition == "bottom") {
            Spacer(modifier = Modifier.height(16.dp))
            PromptBoxComponent(question, mediaPreloader)
        }
    }
}

// Komponen Reusable untuk Kotak Soal
@Composable
fun PromptBoxComponent(
    question: Question,
    mediaPreloader: MediaPreloader?
) {
    // Membersihkan boxText dari tag HTML kosong di akhir (sama seperti questionText)
    val cleanedBoxText = remember(question.boxText) {
        var text = question.boxText ?: ""

        // 1. Hapus semua tag kosong di akhir secara agresif
        // Hapus <p></p>, <p>&nbsp;</p>, <p> </p>, <p><br></p> dll
        text = text.replace(Regex("<p>\\s*(&nbsp;|<br\\s*/?>)*\\s*</p>\\s*$", RegexOption.DOT_MATCHES_ALL), "")

        // 2. Hapus <br>, &nbsp;, whitespace di akhir
        while (text.matches(Regex(".*(<br\\s*/?>|&nbsp;|\\s)$", RegexOption.DOT_MATCHES_ALL))) {
            text = text.replace(Regex("(<br\\s*/?>|&nbsp;|\\s)+$", RegexOption.DOT_MATCHES_ALL), "")
        }

        // 3. Hapus tag <p> terakhir jika kosong
        text = text.replace(Regex("</p>\\s*<p>\\s*</p>\\s*$", RegexOption.DOT_MATCHES_ALL), "</p>")

        // 4. Trim whitespace di awal dan akhir
        text.trim()
    }

    // Hanya tampilkan jika ada konten (Text atau Media)
    if (cleanedBoxText.isNotEmpty() || !question.boxMediaUrl.isNullOrEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp), // Kurangi padding vertical ke 8dp
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top // Align ke atas, tidak center
            ) {
                // 1. Media dalam Kotak
                if (!question.boxMediaUrl.isNullOrEmpty()) {
                    val isAudio = question.boxMediaUrl.matches(Regex(".*\\.(mp3|wav|ogg|m4a)$", RegexOption.IGNORE_CASE))

                    if (isAudio) {
                        AudioPlayerCompactCached(question.boxMediaUrl, mediaPreloader)
                    } else {
                        SubcomposeAsyncImage(
                            model = question.boxMediaUrl,
                            contentDescription = "Box Media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            contentScale = ContentScale.Fit,
                            loading = { ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(120.dp)) }
                        )
                    }
                    // Beri jarak jika ada teks di bawahnya
                    if (cleanedBoxText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // 2. Teks dalam Kotak
                if (cleanedBoxText.isNotEmpty()) {
                    // Get color outside of AndroidView update block
                    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

                    // Custom HtmlText untuk menghilangkan padding bottom dari tag <p>
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(), // Wrap content agar tidak ada extra height
                        factory = { context ->
                            android.widget.TextView(context).apply {
                                movementMethod = android.text.method.LinkMovementMethod.getInstance()
                                // Hilangkan semua padding dan margin
                                includeFontPadding = false
                                setPadding(0, 0, 0, 0)
                                // Hilangkan line spacing extra
                                setLineSpacing(0f, 1.0f)
                            }
                        },
                        update = { textView ->
                            val spanned = androidx.core.text.HtmlCompat.fromHtml(
                                cleanedBoxText,
                                androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
                            )

                            // Trim trailing whitespace dari Spanned
                            var text = spanned
                            var len = text.length
                            while (len > 0 && Character.isWhitespace(text[len - 1])) {
                                len--
                            }
                            if (len < text.length) {
                                text = text.subSequence(0, len) as android.text.Spanned
                            }

                            textView.text = text
                            textView.textSize = 18f
                            textView.setTextColor(textColor)
                            // Set font weight
                            textView.setTypeface(null, android.graphics.Typeface.NORMAL)
                        }
                    )
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
    onClick: () -> Unit,
    optionType: String = "text",
    mediaPreloader: MediaPreloader? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp).align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
            ) {
                when (optionType) {
                    "image" -> {
                        SubcomposeAsyncImage(
                            model = text,
                            contentDescription = "Option Image",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 80.dp),
                            contentScale = ContentScale.Fit,
                            loading = { ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(60.dp)) },
                            error = { MediaErrorPlaceholder(type = "image") }
                        )
                    }
                    "audio" -> AudioPlayerCompactCached(text, mediaPreloader)
                    else -> HtmlText(
                        html = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ✅ LAYOUT NAVIGATION BARU: Tombol Penuh & Iklan di Tengah
@Composable
fun NavigationButtons(
    currentIndex: Int,
    totalQuestions: Int,
    uiLanguage: UILanguage = UILanguage.KOREAN,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onShowGrid: () -> Unit
) {
    val previousText = when (uiLanguage) {
        UILanguage.KOREAN -> "이전"
        UILanguage.INDONESIAN -> "Sebelumnya"
        UILanguage.ENGLISH -> "Prev"
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
    val allQuestionsText = when (uiLanguage) {
        UILanguage.KOREAN -> "전체"
        UILanguage.INDONESIAN -> "Semua"
        UILanguage.ENGLISH -> "All"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .height(54.dp), // Tinggi tetap agar rapi
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Jarak antar elemen
    ) {
        // 1. Previous Button (Weight 1 - Mengambil 1/3 ruang kosong)
        OutlinedButton(
            onClick = onPrevious,
            enabled = currentIndex > 0,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(2.dp))
            Text(previousText, fontSize = 11.sp, maxLines = 1)
        }

        // 2. All Questions Button (Weight 1 - Mengambil 1/3 ruang kosong)
        Button(
            onClick = onShowGrid,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.GridView, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(2.dp))
            Text(allQuestionsText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        // 3. Iklan Banner (Tengah - Ukuran Fixed/Wrap)
        // Disimpan dalam Box agar tidak melar paksa, tapi posisinya di antara All dan Next
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BannerAdView()
        }

        // 4. Next/Finish Button (Weight 1 - Mengambil 1/3 ruang kosong)
        val isLast = currentIndex == totalQuestions - 1
        Button(
            onClick = if (isLast) onFinish else onNext,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            colors = if (isLast) ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) else ButtonDefaults.buttonColors(),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLast) {
                Text(finishText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
            } else {
                Text(nextText, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ... Helper functions di bawah ini ...

@Composable
fun AudioPlayerCompact(url: String) {
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

    Row(
        modifier = Modifier
            .wrapContentWidth()
            .clickable {
                if (isPlaying) {
                    mediaPlayer.pause()
                } else {
                    mediaPlayer.start()
                }
                isPlaying = !isPlaying
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Circular Play/Pause Button
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 2.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Play/Pause Text
        Text(
            text = if (isPlaying) "Pause" else "Play",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                Text(
                    AssessmentTexts.exitTitle(uiLanguage),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

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
    assessmentTitle: String,
    assessmentType: String = "exam",
    userName: String,
    questions: List<Question>,
    currentIndex: Int,
    userAnswers: Map<Int, String>,
    uiLanguage: UILanguage = UILanguage.KOREAN,
    onQuestionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
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

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (assessmentType == "quiz") "Quiz" else "UBT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = assessmentTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(40.dp)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        AssessmentTexts.close(uiLanguage),
                        style = MaterialTheme.typography.labelLarge,
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = titleColor.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    fontSize = 14.sp
                )
                Surface(
                    shape = CircleShape,
                    color = titleColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${questions.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        fontSize = 12.sp
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick),
        shadowElevation = if (isCurrent) 3.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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

@Composable
fun AudioPlayerCompactCached(url: String, mediaPreloader: MediaPreloader?) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val audioSource = remember(url) {
        mediaPreloader?.getCachedFilePath(url, "audio") ?: url
    }

    val mediaPlayer = remember(audioSource) {
        MediaPlayer().apply {
            try {
                if (audioSource.startsWith("/")) {
                    setDataSource(audioSource)
                    prepare()
                    isLoading = false
                } else {
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
        Icon(
            Icons.Default.BrokenImage,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error
        )
    } else {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .clickable(enabled = !isLoading) {
                    if (isPlaying) {
                        mediaPlayer.pause()
                    } else {
                        mediaPlayer.start()
                    }
                    isPlaying = !isPlaying
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Circular Play/Pause Button
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isLoading)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.primary,
                shadowElevation = 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Play/Pause Text
            Text(
                text = if (isLoading) "Loading..." else if (isPlaying) "Pause" else "Play",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun VideoPlayerCompactCached(url: String, mediaPreloader: MediaPreloader?) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val videoSource = remember(url) {
        mediaPreloader?.getCachedFilePath(url, "video") ?: url
    }

    val exoPlayer = remember(videoSource) {
        try {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = if (videoSource.startsWith("/")) {
                    MediaItem.fromUri(Uri.fromFile(File(videoSource)))
                } else {
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
            }
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error initializing", e)
            hasError = true
            isLoading = false
            null
        }
    }

    DisposableEffect(videoSource) {
        onDispose {
            exoPlayer?.release()
        }
    }

    if (hasError || exoPlayer == null) {
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
            Text(
                text = language.flag,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

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