package com.webtech.kamuskorea.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.VocabularyRepository
import com.webtech.kamuskorea.data.local.ChapterInfo
import com.webtech.kamuskorea.data.local.Vocabulary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val _chapters = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chapters: StateFlow<List<ChapterInfo>> = _chapters.asStateFlow()

    private val _vocabularyList = MutableStateFlow<List<Vocabulary>>(emptyList())
    val vocabularyList: StateFlow<List<Vocabulary>> = _vocabularyList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentChapterNumber = MutableStateFlow<Int?>(null)
    val currentChapterNumber: StateFlow<Int?> = _currentChapterNumber.asStateFlow()

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
}
