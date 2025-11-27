package com.webtech.learningkorea.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.webtech.learningkorea.ads.AdManager
import com.webtech.learningkorea.ads.BannerAdView
import com.webtech.learningkorea.data.local.ChapterInfo
import com.webtech.learningkorea.data.local.Vocabulary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationScreen(
    isPremium: Boolean,
    onNavigateToProfile: () -> Unit,
    adManager: AdManager? = null,
    viewModel: MemorizationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val chapters by viewModel.chapters.collectAsState()
    val vocabularyList by viewModel.vocabularyList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentChapterNumber by viewModel.currentChapterNumber.collectAsState()
    val favoriteVocabularyIds by viewModel.favoriteVocabularyIds.collectAsState()

    // State untuk menampilkan premium lock dialog
    var showPremiumDialog by remember { mutableStateOf(false) }

    // Fungsi untuk mengecek apakah bab bisa diakses
    val canAccessChapter: (Int) -> Boolean = { chapterNumber ->
        chapterNumber <= 5 || isPremium
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Main content
        Box(modifier = Modifier.weight(1f)) {
            // Show chapter list or vocabulary flashcards
            if (currentChapterNumber == null) {
                ChapterListScreen(
                    chapters = chapters,
                    isLoading = isLoading,
                    isPremium = isPremium,
                    onChapterClick = { chapterNumber ->
                        if (canAccessChapter(chapterNumber)) {
                            viewModel.loadVocabularyForChapter(chapterNumber)
                        } else {
                            showPremiumDialog = true
                        }
                    }
                )
            } else {
                FlashcardScreen(
                    vocabularyList = vocabularyList,
                    chapterNumber = currentChapterNumber!!,
                    isLoading = isLoading,
                    onBackClick = { viewModel.backToChapterList() },
                    adManager = adManager,
                    activity = activity,
                    isPremium = isPremium,
                    favoriteIds = favoriteVocabularyIds,
                    onToggleFavorite = { vocabulary -> viewModel.toggleFavoriteVocabulary(vocabulary) }
                )
            }
        }

        // Banner Ad at the bottom (for non-premium users)
        if (!isPremium) {
            BannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Premium Lock Dialog
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            icon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Bab Premium",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Bab 1-5 gratis untuk semua pengguna.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Upgrade ke Premium untuk akses ke Bab 6 dan seterusnya dengan fitur flashcard interaktif tanpa batas.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumDialog = false
                        onNavigateToProfile()
                    }
                ) {
                    Text("Upgrade Premium")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun PremiumLockContent(onNavigateToProfile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Fitur Hafalan Premium",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Tingkatkan pembelajaran Anda dengan fitur flashcard interaktif per bab.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Star, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upgrade ke Premium")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    chapters: List<ChapterInfo>,
    isLoading: Boolean,
    isPremium: Boolean,
    onChapterClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Hafalan Per Bab",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isPremium) {
                            Text(
                                "Bab 1-5 Gratis • Bab 6+ Premium",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (chapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tidak ada data hafalan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chapters) { chapter ->
                    ChapterCard(
                        chapter = chapter,
                        isPremium = isPremium,
                        isLocked = chapter.chapterNumber > 5 && !isPremium,
                        onClick = { onChapterClick(chapter.chapterNumber) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterCard(
    chapter: ChapterInfo,
    isPremium: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chapter number badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLocked)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLocked) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Terkunci",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Text(
                                text = "${chapter.chapterNumber}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Chapter info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chapter.chapterTitleKorean,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocked)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "PREMIUM",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chapter.chapterTitleIndonesian,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLocked) 0.4f else 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLocked) 0.3f else 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${chapter.wordCount} kata",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLocked) 0.3f else 0.5f)
                        )
                    }
                }

                Icon(
                    if (isLocked) Icons.Default.Lock else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isLocked) "Terkunci" else "Buka",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = if (isLocked) 0f else 180f),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLocked) 0.3f else 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    vocabularyList: List<Vocabulary>,
    chapterNumber: Int,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    adManager: AdManager? = null,
    activity: Activity? = null,
    isPremium: Boolean = false,
    favoriteIds: Set<Int> = emptySet(),
    onToggleFavorite: (Vocabulary) -> Unit = {}
) {
    val chapterTitle = vocabularyList.firstOrNull()?.chapterTitleIndonesian ?: "Bab $chapterNumber"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Bab $chapterNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            chapterTitle,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(vocabularyList) { index, vocabulary ->
                    FlashCard(
                        vocabulary = vocabulary,
                        adManager = adManager,
                        activity = activity,
                        isPremium = isPremium,
                        isFavorite = favoriteIds.contains(vocabulary.id),
                        onToggleFavorite = { onToggleFavorite(vocabulary) }
                    )
                }
            }
        }
    }
}

@Composable
fun FlashCard(
    vocabulary: Vocabulary,
    adManager: AdManager? = null,
    activity: Activity? = null,
    isPremium: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    var isFlipped by remember { mutableStateOf(false) }

    // Auto-flip back to Indonesian after 5 seconds
    LaunchedEffect(isFlipped) {
        if (isFlipped) {
            delay(5000L) // 5 seconds
            isFlipped = false
        }
    }

    // Handle flashcard click with rewarded ad (only for non-premium users)
    val onCardClick: () -> Unit = {
        if (!isPremium && adManager != null && activity != null) {
            // Show rewarded ad every 20 clicks
            adManager.showRewardedAdOnFlashcardClick(activity) {
                isFlipped = !isFlipped
            }
        } else {
            // Premium users or no adManager - just flip
            isFlipped = !isFlipped
        }
    }

    // Animation for card flip
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing), label = "rotation"
    )

    val animateFront by animateFloatAsState(
        targetValue = if (!isFlipped) 1f else 0f,
        animationSpec = tween(durationMillis = 400), label = "animateFront"
    )

    val animateBack by animateFloatAsState(
        targetValue = if (isFlipped) 1f else 0f,
        animationSpec = tween(durationMillis = 400), label = "animateBack"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFlipped)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isFlipped) {
                // Front side - Indonesian meaning (DENGAN TOMBOL FAVOURITE)
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Konten utama
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .graphicsLayer { alpha = animateFront },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = vocabulary.indonesianMeaning ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Tap untuk lihat Korea",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }

                    // Tombol Favourite di pojok kanan atas
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Back side - Korean word
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .graphicsLayer {
                            alpha = animateBack
                            rotationY = 180f
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = vocabulary.koreanWord ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = vocabulary.indonesianMeaning ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}