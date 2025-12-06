package com.webtech.learningkorea.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.learningkorea.data.QuizOption
import com.webtech.learningkorea.data.QuizQuestion
import com.webtech.learningkorea.data.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Quiz Hafalan
 */
data class QuizHafalanUiState(
    val currentQuestion: QuizQuestion? = null,
    val selectedOption: QuizOption? = null,
    val showCorrectAnswer: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val autoAdvanceTimeLeft: Int = 0 // Countdown in seconds (0-4)
)

@HiltViewModel
class QuizHafalanViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val TAG = "QuizHafalanViewModel"

    private val _uiState = MutableStateFlow(QuizHafalanUiState(isLoading = true))
    val uiState: StateFlow<QuizHafalanUiState> = _uiState.asStateFlow()

    private var autoAdvanceJob: Job? = null

    init {
        loadNextQuestion()
    }

    /**
     * Load a new random question
     */
    fun loadNextQuestion() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    selectedOption = null,
                    showCorrectAnswer = false,
                    autoAdvanceTimeLeft = 0
                )

                // Cancel any ongoing auto-advance timer
                autoAdvanceJob?.cancel()

                val question = vocabularyRepository.generateQuizQuestion()

                if (question == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Tidak ada cukup kata dalam database untuk membuat quiz"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        currentQuestion = question,
                        isLoading = false
                    )
                    Log.d(TAG, "✅ Loaded new question: ${question.question}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading question", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Gagal memuat pertanyaan: ${e.message}"
                )
            }
        }
    }

    /**
     * Handle option selection by user
     */
    fun onOptionSelected(option: QuizOption) {
        if (_uiState.value.selectedOption != null) {
            // Already answered, ignore
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedOption = option,
            showCorrectAnswer = !option.isCorrect
        )

        Log.d(TAG, "User selected: ${option.text} (${if (option.isCorrect) "CORRECT" else "WRONG"})")

        // Start 4-second countdown before auto-advancing
        startAutoAdvanceTimer()
    }

    /**
     * Start countdown timer (4 seconds) then auto-advance to next question
     */
    private fun startAutoAdvanceTimer() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            // Countdown from 4 to 1
            for (secondsLeft in 4 downTo 1) {
                _uiState.value = _uiState.value.copy(autoAdvanceTimeLeft = secondsLeft)
                delay(1000) // Wait 1 second
            }

            // After 4 seconds, load next question
            _uiState.value = _uiState.value.copy(autoAdvanceTimeLeft = 0)
            loadNextQuestion()
        }
    }

    /**
     * Skip to next question manually (optional feature)
     */
    fun skipToNext() {
        autoAdvanceJob?.cancel()
        loadNextQuestion()
    }

    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
    }
}
