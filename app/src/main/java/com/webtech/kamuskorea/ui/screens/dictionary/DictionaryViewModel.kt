package com.webtech.kamuskorea.ui.screens.dictionary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.data.local.WordDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val wordDao: WordDao
) : ViewModel() {

    private val TAG = "SearchDebug"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val words: StateFlow<List<Word>> = searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                wordDao.getAllWords()
            } else {
                // DIPERBARUI:
                // Kita tidak perlu .lowercase() lagi karena DAO pakai COLLATE NOCASE.
                // Kita tambahkan wildcard '%' di sini.
                val searchQueryWithWildcards = "%${query}%"

                Log.d(TAG, "Mencari di DAO (v1, NOCASE) dengan query: '$searchQueryWithWildcards'")

                wordDao.searchWords(searchQueryWithWildcards)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}