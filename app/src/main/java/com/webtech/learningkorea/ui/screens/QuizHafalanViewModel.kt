package com.webtech.learningkorea.ui.screens

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
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
    val autoAdvanceTimeLeft: Int = 0, // Countdown in seconds (0-4)
    val dailyQuota: Int = 100, // Total daily quota
    val quotaRemaining: Int = 100, // Remaining quota for today
    val quotaExhausted: Boolean = false, // True when no quota left
    val shouldShowInterstitialAd: Boolean = false, // True every 20 answers
    val totalAnsweredToday: Int = 0 // Total answered today
)

@HiltViewModel
class QuizHafalanViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val TAG = "QuizHafalanViewModel"

    companion object {
        private val QUIZ_HAFALAN_COUNT_KEY = intPreferencesKey("quiz_hafalan_daily_count")
        private val QUIZ_HAFALAN_DATE_KEY = longPreferencesKey("quiz_hafalan_last_date")
        private const val DAILY_QUOTA = 100
        private const val AD_FREQUENCY = 20 // Show ad every 20 answers
    }

    private val _uiState = MutableStateFlow(QuizHafalanUiState(isLoading = true))
    val uiState: StateFlow<QuizHafalanUiState> = _uiState.asStateFlow()

    private var autoAdvanceJob: Job? = null

    init {
        viewModelScope.launch {
            checkAndResetDailyQuota()
            loadNextQuestion()
        }
    }

    /**
     * Check and reset daily quota if it's a new day
     */
    private suspend fun checkAndResetDailyQuota() {
        val today = getStartOfDay(System.currentTimeMillis())
        val preferences = dataStore.data.first()
        val lastDate = preferences[QUIZ_HAFALAN_DATE_KEY] ?: 0L
        val lastDateDay = getStartOfDay(lastDate)

        val count = if (lastDateDay < today) {
            // New day - reset count
            Log.d(TAG, "📅 New day detected - resetting quota to $DAILY_QUOTA")
            dataStore.edit { prefs ->
                prefs[QUIZ_HAFALAN_COUNT_KEY] = 0
                prefs[QUIZ_HAFALAN_DATE_KEY] = today
            }
            0
        } else {
            preferences[QUIZ_HAFALAN_COUNT_KEY] ?: 0
        }

        val remaining = DAILY_QUOTA - count
        _uiState.value = _uiState.value.copy(
            quotaRemaining = remaining.coerceAtLeast(0),
            totalAnsweredToday = count,
            quotaExhausted = remaining <= 0
        )

        Log.d(TAG, "📊 Daily quota: $count/$DAILY_QUOTA (${remaining} remaining)")
    }

    /**
     * Load a new random question
     */
    fun loadNextQuestion() {
        viewModelScope.launch {
            try {
                // Check quota first
                checkAndResetDailyQuota()

                if (_uiState.value.quotaExhausted) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Kuota harian habis! Anda telah menjawab 100 soal hari ini. Silakan coba lagi besok.",
                        currentQuestion = null
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    selectedOption = null,
                    showCorrectAnswer = false,
                    autoAdvanceTimeLeft = 0,
                    shouldShowInterstitialAd = false
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

        viewModelScope.launch {
            // Increment daily count
            dataStore.edit { prefs ->
                val currentCount = prefs[QUIZ_HAFALAN_COUNT_KEY] ?: 0
                val newCount = currentCount + 1
                prefs[QUIZ_HAFALAN_COUNT_KEY] = newCount
                prefs[QUIZ_HAFALAN_DATE_KEY] = getStartOfDay(System.currentTimeMillis())

                val remaining = DAILY_QUOTA - newCount

                // Check if should show interstitial ad (every 20 answers)
                val shouldShowAd = (newCount % AD_FREQUENCY == 0)

                _uiState.value = _uiState.value.copy(
                    selectedOption = option,
                    showCorrectAnswer = !option.isCorrect,
                    quotaRemaining = remaining.coerceAtLeast(0),
                    totalAnsweredToday = newCount,
                    quotaExhausted = remaining <= 0,
                    shouldShowInterstitialAd = shouldShowAd
                )

                Log.d(TAG, "User selected: ${option.text} (${if (option.isCorrect) "CORRECT" else "WRONG"})")
                Log.d(TAG, "📊 Progress: $newCount/$DAILY_QUOTA ($remaining remaining)")
                if (shouldShowAd) {
                    Log.d(TAG, "📺 Triggering interstitial ad (answered $newCount questions)")
                }
            }

            // Start 4-second countdown before auto-advancing
            startAutoAdvanceTimer()
        }
    }

    /**
     * Mark interstitial ad as shown (called from UI after ad is displayed)
     */
    fun onInterstitialAdShown() {
        _uiState.value = _uiState.value.copy(shouldShowInterstitialAd = false)
        Log.d(TAG, "✅ Interstitial ad marked as shown")
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

    /**
     * Get start of day timestamp (00:00:00) in milliseconds
     */
    private fun getStartOfDay(timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
    }
}
