package com.webtech.kamuskorea.ui.screens.dictionary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.data.local.WordDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val wordDao: WordDao
) : ViewModel() {

    private val TAG = "DictionaryViewModel"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
}