package com.webtech.kamuskorea.ui.screens.dictionary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.data.local.WordDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch // ✅ IMPORT INI DITAMBAHKAN
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val wordDao: WordDao
) : ViewModel() {

    private val TAG = "DictionaryViewModel"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val words: StateFlow<List<Word>> = searchQuery
        .onEach { query ->
            Log.d(TAG, "========== SEARCH INPUT ==========")
            Log.d(TAG, "Query berubah: '$query'")
            Log.d(TAG, "Query length: ${query.length}")
            Log.d(TAG, "==================================")
        }
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                Log.d(TAG, "Query kosong, memuat semua kata...")
                wordDao.getAllWords()
                    .onEach { wordList ->
                        Log.d(TAG, "📚 Loaded ${wordList.size} total words")
                        if (wordList.isNotEmpty()) {
                            Log.d(TAG, "First word: ${wordList.first().koreanWord}")
                        }
                    }
            } else {
                // ✅ Tambahkan wildcard '%' di query dan trim whitespace
                val searchQueryWithWildcards = "%${query.trim()}%"

                Log.d(TAG, "========== SEARCH DEBUG ==========")
                Log.d(TAG, "Original query: '$query'")
                Log.d(TAG, "Trimmed query: '${query.trim()}'")
                Log.d(TAG, "Query dengan wildcard: '$searchQueryWithWildcards'")
                Log.d(TAG, "==================================")

                wordDao.searchWords(searchQueryWithWildcards)
                    .onEach { results ->
                        Log.d(TAG, "🔍 Search results: ${results.size} words found")
                        results.take(3).forEach { word ->
                            Log.d(TAG, "  - ${word.koreanWord} [${word.romanization}] = ${word.indonesianTranslation}")
                        }
                    }
            }
        }
        .catch { e ->
            Log.e(TAG, "❌ Error in search flow", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        Log.d(TAG, "✅ DictionaryViewModel created")

        // ✅ Test database saat init - DIPERBAIKI: gunakan launch
        viewModelScope.launch { // ✅ INI YANG KURANG!
            try {
                val allWords = wordDao.getAllWords().first() // Sekarang bisa pakai .first()
                Log.d(TAG, "========== DATABASE CHECK ==========")
                Log.d(TAG, "Total words in database: ${allWords.size}")
                if (allWords.isNotEmpty()) {
                    Log.d(TAG, "First 3 words:")
                    allWords.take(3).forEach { word ->
                        Log.d(TAG, "  - ${word.koreanWord} [${word.romanization}]")
                    }
                } else {
                    Log.e(TAG, "❌ DATABASE IS EMPTY!")
                    Log.e(TAG, "Possible causes:")
                    Log.e(TAG, "1. File kamus_korea.db tidak ada di assets/database/")
                    Log.e(TAG, "2. Database tidak ter-copy dengan benar")
                    Log.e(TAG, "3. App perlu di-uninstall dan install ulang")
                }
                Log.d(TAG, "===================================")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking database", e)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        Log.d(TAG, "onSearchQueryChange called with: '$query'")
        _searchQuery.value = query
    }
}