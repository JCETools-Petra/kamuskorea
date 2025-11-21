package com.webtech.kamuskorea.ui.screens.dictionary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.data.local.WordDao
import com.webtech.kamuskorea.data.local.FavoriteWord
import com.webtech.kamuskorea.data.local.FavoriteWordDao
import com.webtech.kamuskorea.analytics.AnalyticsTracker
import com.webtech.kamuskorea.gamification.GamificationRepository
import com.webtech.kamuskorea.gamification.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DisplayLanguage {
    KOREAN,     // Show Korean word first
    INDONESIAN  // Show Indonesian word first
}

@OptIn(FlowPreview::class)
@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val wordDao: WordDao,
    private val favoriteWordDao: FavoriteWordDao,
    private val analyticsTracker: AnalyticsTracker,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val TAG = "DictionaryViewModel"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _displayLanguage = MutableStateFlow(DisplayLanguage.KOREAN)
    val displayLanguage: StateFlow<DisplayLanguage> = _displayLanguage.asStateFlow()

    // Track favorite IDs
    val favoriteIds: StateFlow<Set<Int>> = favoriteWordDao.getAllFavoriteIds()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // Get all favorites
    val favorites: StateFlow<List<FavoriteWord>> = favoriteWordDao.getAllFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val words: StateFlow<List<Word>> = searchQuery
        .debounce(300L)
        .onEach {
            _isLoading.value = true
            Log.d(TAG, "Search query changed: '$it'")
        }
        .flatMapLatest { query ->
            if (query.isBlank()) {
                wordDao.getAllWords()
            } else {
                val searchPattern = "%${query.trim()}%"
                wordDao.searchWords(searchPattern)
            }
        }
        .onEach {
            _isLoading.value = false
            Log.d(TAG, "Words loaded: ${it.size}")
        }
        .catch { e ->
            Log.e(TAG, "Error loading words", e)
            _isLoading.value = false
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        Log.d(TAG, "DictionaryViewModel initialized")
        checkDatabaseContent()
    }

    private fun checkDatabaseContent() {
        viewModelScope.launch {
            try {
                val allWords = wordDao.getAllWords().first()
                Log.d(TAG, "========== DATABASE CHECK ==========")
                Log.d(TAG, "Total words: ${allWords.size}")
                if (allWords.isNotEmpty()) {
                    Log.d(TAG, "First 3 words:")
                    allWords.take(3).forEach { word ->
                        Log.d(TAG, "  - ${word.koreanWord} [${word.romanization}] = ${word.indonesianTranslation}")
                    }
                } else {
                    Log.e(TAG, "❌ Database is EMPTY!")
                }
                Log.d(TAG, "===================================")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking database", e)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        Log.d(TAG, "onSearchQueryChange: '$query'")
        _searchQuery.value = query
    }

    fun toggleDisplayLanguage() {
        _displayLanguage.value = when (_displayLanguage.value) {
            DisplayLanguage.KOREAN -> DisplayLanguage.INDONESIAN
            DisplayLanguage.INDONESIAN -> DisplayLanguage.KOREAN
        }
        Log.d(TAG, "Display language toggled to: ${_displayLanguage.value}")
    }

    fun toggleFavorite(word: Word) {
        viewModelScope.launch {
            val isFav = favoriteIds.value.contains(word.id)
            if (isFav) {
                favoriteWordDao.removeFavorite(word.id)
                Log.d(TAG, "Removed from favorites: ${word.koreanWord}")

                // Track unfavorite
                analyticsTracker.logWordUnfavorited(word.koreanWord)
            } else {
                val favoriteWord = FavoriteWord(
                    wordId = word.id,
                    koreanWord = word.koreanWord,
                    romanization = word.romanization,
                    indonesianTranslation = word.indonesianTranslation
                )
                favoriteWordDao.addFavorite(favoriteWord)
                Log.d(TAG, "Added to favorites: ${word.koreanWord}")

                // Track favorite and award XP
                analyticsTracker.logWordFavorited(word.koreanWord, word.indonesianTranslation)
                gamificationRepository.addXp(XpRewards.WORD_FAVORITED, "word_favorited")
                Log.d(TAG, "⭐ Awarded ${XpRewards.WORD_FAVORITED} XP for favoriting word")
            }
        }
    }

    fun removeFavoriteById(wordId: Int) {
        viewModelScope.launch {
            favoriteWordDao.removeFavorite(wordId)
            Log.d(TAG, "Removed favorite by ID: $wordId")
        }
    }
}
