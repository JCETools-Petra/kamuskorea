package com.webtech.kamuskorea.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.VocabularyRepository
import com.webtech.kamuskorea.data.local.ChapterInfo
import com.webtech.kamuskorea.data.local.Vocabulary
import com.webtech.kamuskorea.data.local.FavoriteVocabulary
import com.webtech.kamuskorea.data.local.FavoriteVocabularyDao
import com.webtech.kamuskorea.analytics.AnalyticsTracker
import com.webtech.kamuskorea.gamification.GamificationRepository
import com.webtech.kamuskorea.gamification.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val favoriteVocabularyDao: FavoriteVocabularyDao,
    private val analyticsTracker: AnalyticsTracker,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val TAG = "MemorizationViewModel"

    private val _chapters = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chapters: StateFlow<List<ChapterInfo>> = _chapters.asStateFlow()

    private val _vocabularyList = MutableStateFlow<List<Vocabulary>>(emptyList())
    val vocabularyList: StateFlow<List<Vocabulary>> = _vocabularyList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentChapterNumber = MutableStateFlow<Int?>(null)
    val currentChapterNumber: StateFlow<Int?> = _currentChapterNumber.asStateFlow()

    // Track favorite vocabulary IDs
    val favoriteVocabularyIds: StateFlow<Set<Int>> = favoriteVocabularyDao.getAllFavoriteIds()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // Get all favorite vocabularies
    val favoriteVocabularies: StateFlow<List<FavoriteVocabulary>> = favoriteVocabularyDao.getAllFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadChapters()
    }

    /**
     * Load all chapters
     */
    private fun loadChapters() {
        viewModelScope.launch {
            _isLoading.value = true
            vocabularyRepository.getAllChapters().collect { chapterList ->
                _chapters.value = chapterList
                _isLoading.value = false
            }
        }
    }

    /**
     * Load vocabulary for a specific chapter
     */
    fun loadVocabularyForChapter(chapterNumber: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentChapterNumber.value = chapterNumber
            vocabularyRepository.getVocabularyByChapter(chapterNumber).collect { vocabList ->
                _vocabularyList.value = vocabList
                _isLoading.value = false
            }
        }
    }

    /**
     * Go back to chapter list
     */
    fun backToChapterList() {
        _currentChapterNumber.value = null
        _vocabularyList.value = emptyList()
    }

    /**
     * Toggle favorite status for a vocabulary item
     */
    fun toggleFavoriteVocabulary(vocabulary: Vocabulary) {
        viewModelScope.launch {
            val vocabId = vocabulary.id ?: return@launch
            val isFav = favoriteVocabularyIds.value.contains(vocabId)
            if (isFav) {
                favoriteVocabularyDao.removeFavorite(vocabId)
                Log.d(TAG, "Removed from favorites: ${vocabulary.koreanWord}")
            } else {
                val favoriteVocabulary = FavoriteVocabulary(
                    vocabularyId = vocabId,
                    koreanWord = vocabulary.koreanWord ?: "",
                    indonesianMeaning = vocabulary.indonesianMeaning ?: "",
                    chapterNumber = vocabulary.chapterNumber ?: 0,
                    chapterTitleIndonesian = vocabulary.chapterTitleIndonesian ?: ""
                )
                favoriteVocabularyDao.addFavorite(favoriteVocabulary)
                Log.d(TAG, "Added to favorites: ${vocabulary.koreanWord}")

                // Award XP for favoriting vocabulary
                gamificationRepository.addXp(XpRewards.WORD_FAVORITED, "vocabulary_favorited")
                Log.d(TAG, "⭐ Awarded ${XpRewards.WORD_FAVORITED} XP for favoriting vocabulary")
            }
        }
    }

    /**
     * Remove favorite vocabulary by ID
     */
    fun removeFavoriteVocabularyById(vocabularyId: Int) {
        viewModelScope.launch {
            favoriteVocabularyDao.removeFavorite(vocabularyId)
            Log.d(TAG, "Removed favorite vocabulary by ID: $vocabularyId")
        }
    }

    /**
     * Track when a flashcard is flipped and award XP
     * Call this from UI when user flips a flashcard
     */
    fun onFlashcardFlipped(chapterNumber: Int) {
        viewModelScope.launch {
            // Track analytics
            analyticsTracker.logFlashcardFlipped(chapterNumber)

            // Award XP
            gamificationRepository.addXp(XpRewards.FLASHCARD_FLIPPED, "flashcard_flipped")
            Log.d(TAG, "⭐ Awarded ${XpRewards.FLASHCARD_FLIPPED} XP for flipping flashcard")
        }
    }
}
