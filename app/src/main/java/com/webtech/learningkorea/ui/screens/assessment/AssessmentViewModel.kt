package com.webtech.learningkorea.ui.screens.assessment

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.learningkorea.data.assessment.*
import com.webtech.learningkorea.data.media.MediaPreloader
import com.webtech.learningkorea.data.network.ApiService
import com.webtech.learningkorea.analytics.AnalyticsTracker
import com.webtech.learningkorea.gamification.GamificationRepository
import com.webtech.learningkorea.gamification.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStore: DataStore<Preferences>,
    private val mediaPreloader: MediaPreloader,
    private val analyticsTracker: AnalyticsTracker,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    companion object {
        val QUIZ_COMPLETED_COUNT_KEY = intPreferencesKey("quiz_completed_count")
        val LAST_ACTIVITY_DATE_KEY = longPreferencesKey("last_activity_date")
        val STREAK_DAYS_KEY = intPreferencesKey("streak_days")
    }

    private val _categories = MutableStateFlow<List<AssessmentCategory>>(emptyList())
    val categories: StateFlow<List<AssessmentCategory>> = _categories.asStateFlow()

    private val _assessments = MutableStateFlow<List<Assessment>>(emptyList())
    val assessments: StateFlow<List<Assessment>> = _assessments.asStateFlow()

    // Variable baru untuk menyimpan Judul Assessment yang benar dari database
    private val _currentAssessmentTitle = MutableStateFlow<String?>(null)
    val currentAssessmentTitle: StateFlow<String?> = _currentAssessmentTitle.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, String>> = _userAnswers.asStateFlow()

    private val _assessmentResult = MutableStateFlow<AssessmentResult?>(null)
    val assessmentResult: StateFlow<AssessmentResult?> = _assessmentResult.asStateFlow()

    private val _history = MutableStateFlow<List<AssessmentHistory>>(emptyList())
    val history: StateFlow<List<AssessmentHistory>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<AssessmentLeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<AssessmentLeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _isLoadingLeaderboard = MutableStateFlow(false)
    val isLoadingLeaderboard: StateFlow<Boolean> = _isLoadingLeaderboard.asStateFlow()

    private val _startTime = MutableStateFlow(0L)

    // Expose media preloader for UI
    val mediaLoadingStates = mediaPreloader.loadingStates

    /**
     * Get the MediaPreloader instance
     */
    fun getMediaPreloader(): MediaPreloader = mediaPreloader

    // Fetch kategori
    fun fetchCategories(type: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("AssessmentVM", "📡 Fetching categories, type=$type")
                val response = apiService.getAssessmentCategories(type)
                if (response.isSuccessful) {
                    _categories.value = response.body() ?: emptyList()
                    Log.d("AssessmentVM", "✅ Categories loaded: ${_categories.value.size}")
                } else {
                    val errorMsg = "Gagal memuat kategori: ${response.code()}"
                    Log.e("AssessmentVM", errorMsg)
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e("AssessmentVM", "❌ Error fetching categories", e)
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch daftar assessment
    fun fetchAssessments(categoryId: Int? = null, type: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("AssessmentVM", "📡 Fetching assessments, categoryId=$categoryId, type=$type")
                val response = apiService.getAssessments(categoryId, type)
                if (response.isSuccessful) {
                    _assessments.value = response.body() ?: emptyList()
                    Log.d("AssessmentVM", "✅ Assessments loaded: ${_assessments.value.size}")
                } else {
                    val errorMsg = "Gagal memuat assessment: ${response.code()}"
                    Log.e("AssessmentVM", errorMsg)
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e("AssessmentVM", "❌ Error fetching assessments", e)
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Mulai assessment - fetch questions
    fun startAssessment(assessmentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentQuestionIndex.value = 0
            _userAnswers.value = emptyMap()
            _assessmentResult.value = null
            _startTime.value = System.currentTimeMillis()

            // LOGIKA BARU: Cari judul assessment dari list yang sudah ada berdasarkan ID
            // Ini memperbaiki masalah judul yang selalu "Exam"
            val foundAssessment = _assessments.value.find { it.id == assessmentId }
            if (foundAssessment != null) {
                _currentAssessmentTitle.value = foundAssessment.title
                Log.d("AssessmentVM", "📌 Title set from cache: ${foundAssessment.title}")
            }

            try {
                Log.d("AssessmentVM", "🚀 Starting assessment ID: $assessmentId")
                val response = apiService.getAssessmentQuestions(assessmentId)

                Log.d("AssessmentVM", "📨 Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val questionsList = response.body() ?: emptyList()
                    Log.d("AssessmentVM", "✅ Questions received: ${questionsList.size}")

                    // LOG DETAIL SETIAP PERTANYAAN - PENTING UNTUK DEBUG ENCODING
                    questionsList.forEachIndexed { index, q ->
                        Log.d("AssessmentVM", "═══ Question ${index + 1} ═══")
                        Log.d("AssessmentVM", "ID: ${q.id}")
                        Log.d("AssessmentVM", "Text: ${q.questionText}")
                        Log.d("AssessmentVM", "Type: ${q.questionType}")
                        Log.d("AssessmentVM", "Option A: ${q.optionA}")
                        Log.d("AssessmentVM", "Option B: ${q.optionB}")
                        Log.d("AssessmentVM", "Option C: ${q.optionC}")
                        Log.d("AssessmentVM", "Option D: ${q.optionD}")

                        // Check for encoding issues
                        if (q.questionText.contains("?")) {
                            Log.w("AssessmentVM", "⚠️ Possible encoding issue in question text!")
                        }
                    }

                    _questions.value = questionsList

                    // Start preloading media for questions
                    if (questionsList.isNotEmpty()) {
                        Log.d("AssessmentVM", "📦 Starting media preload for ${questionsList.size} questions")
                        mediaPreloader.preloadQuestions(questionsList, 0)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    val errorMsg = "Gagal memuat soal: ${response.code()} - $errorBody"
                    Log.e("AssessmentVM", errorMsg)
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e("AssessmentVM", "❌ Exception fetching questions", e)
                Log.e("AssessmentVM", "Exception type: ${e.javaClass.simpleName}")
                Log.e("AssessmentVM", "Exception message: ${e.message}")
                e.printStackTrace()
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Simpan jawaban user
    fun saveAnswer(questionId: Int, answer: String) {
        val currentAnswers = _userAnswers.value.toMutableMap()
        currentAnswers[questionId] = answer
        _userAnswers.value = currentAnswers
        Log.d("AssessmentVM", "💾 Answer saved: Q$questionId = $answer (Total answered: ${currentAnswers.size})")
    }

    // Next question
    fun nextQuestion() {
        if (_currentQuestionIndex.value < _questions.value.size - 1) {
            _currentQuestionIndex.value++
            Log.d("AssessmentVM", "➡️ Next question: ${_currentQuestionIndex.value + 1}")
            // Preload upcoming questions
            mediaPreloader.preloadQuestions(_questions.value, _currentQuestionIndex.value)
        }
    }

    // Previous question
    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
            Log.d("AssessmentVM", "⬅️ Previous question: ${_currentQuestionIndex.value + 1}")
            // Preload upcoming questions from new position
            mediaPreloader.preloadQuestions(_questions.value, _currentQuestionIndex.value)
        }
    }

    // Go to specific question
    fun goToQuestion(index: Int) {
        if (index in _questions.value.indices) {
            _currentQuestionIndex.value = index
            Log.d("AssessmentVM", "🎯 Jump to question: ${index + 1}")
            // Preload upcoming questions from jumped position
            mediaPreloader.preloadQuestions(_questions.value, index)
        }
    }

    // Submit assessment
    fun submitAssessment(assessmentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val timeTaken = ((System.currentTimeMillis() - _startTime.value) / 1000).toInt()
            val answers = _userAnswers.value.map { (questionId, answer) ->
                UserAnswer(questionId, answer)
            }

            val request = SubmitAssessmentRequest(answers, timeTaken)

            try {
                Log.d("AssessmentVM", "📤 Submitting assessment ID: $assessmentId")
                Log.d("AssessmentVM", "Total answers: ${answers.size}")
                Log.d("AssessmentVM", "Time taken: ${timeTaken}s")

                answers.forEach { answer ->
                    Log.d("AssessmentVM", "  Q${answer.questionId} -> ${answer.answer}")
                }

                val response = apiService.submitAssessment(assessmentId, request)

                Log.d("AssessmentVM", "📨 Submit response code: ${response.code()}")

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("AssessmentVM", "✅ Assessment submitted successfully!")
                    Log.d("AssessmentVM", "Score: ${result?.score}")
                    Log.d("AssessmentVM", "Passed: ${result?.passed}")
                    Log.d("AssessmentVM", "Correct: ${result?.correctAnswers}/${result?.totalQuestions}")

                    _assessmentResult.value = result

                    // Update learning statistics
                    updateStatisticsOnQuizComplete()

                    // Award XP for quiz completion
                    gamificationRepository.addXp(XpRewards.QUIZ_COMPLETED, "quiz_completed")
                    Log.d("AssessmentVM", "⭐ Awarded ${XpRewards.QUIZ_COMPLETED} XP for quiz completion")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    val errorMsg = "Gagal submit jawaban: ${response.code()} - $errorBody"
                    Log.e("AssessmentVM", errorMsg)
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e("AssessmentVM", "❌ Exception submitting assessment", e)
                Log.e("AssessmentVM", "Exception type: ${e.javaClass.simpleName}")
                Log.e("AssessmentVM", "Exception message: ${e.message}")
                e.printStackTrace()
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch riwayat
    fun fetchHistory(assessmentId: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("AssessmentVM", "📡 Fetching history, assessmentId=$assessmentId")
                val response = apiService.getAssessmentResults(assessmentId)
                if (response.isSuccessful) {
                    _history.value = response.body() ?: emptyList()
                    Log.d("AssessmentVM", "✅ History loaded: ${_history.value.size} records")
                } else {
                    val errorMsg = "Gagal memuat riwayat: ${response.code()}"
                    Log.e("AssessmentVM", errorMsg)
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e("AssessmentVM", "❌ Error fetching history", e)
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch leaderboard
    fun fetchLeaderboard(type: String, assessmentId: Int? = null) {
        viewModelScope.launch {
            _isLoadingLeaderboard.value = true
            try {
                Log.d("AssessmentVM", "📡 Fetching leaderboard, type=$type, assessmentId=$assessmentId")
                val response = apiService.getAssessmentLeaderboard(type, assessmentId)
                if (response.isSuccessful) {
                    _leaderboard.value = response.body() ?: emptyList()
                    Log.d("AssessmentVM", "✅ Leaderboard loaded: ${_leaderboard.value.size} entries")
                } else {
                    val errorMsg = "Gagal memuat leaderboard: ${response.code()}"
                    Log.e("AssessmentVM", errorMsg)
                    // Don't set global error, just log it
                }
            } catch (e: Exception) {
                Log.e("AssessmentVM", "❌ Error fetching leaderboard", e)
                // Don't set global error, just log it
            } finally {
                _isLoadingLeaderboard.value = false
            }
        }
    }

    // Reset result
    fun resetResult() {
        _assessmentResult.value = null
        Log.d("AssessmentVM", "🔄 Result reset")
    }

    // Clear error
    fun clearError() {
        _error.value = null
        Log.d("AssessmentVM", "🧹 Error cleared")
    }

    // Reset assessment state (untuk cancel/keluar dari ujian)
    fun resetAssessment() {
        _questions.value = emptyList()
        _currentQuestionIndex.value = 0
        _userAnswers.value = emptyMap()
        _assessmentResult.value = null
        _error.value = null
        _currentAssessmentTitle.value = null // Reset title
        _startTime.value = 0L
        // Cancel any pending preload operations
        mediaPreloader.cancelAll()
        Log.d("AssessmentVM", "🔄 Assessment state reset")
    }

    // Update statistics when quiz is completed
    private fun updateStatisticsOnQuizComplete() {
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())

            dataStore.edit { preferences ->
                // Increment quiz completed count
                val currentCount = preferences[QUIZ_COMPLETED_COUNT_KEY] ?: 0
                preferences[QUIZ_COMPLETED_COUNT_KEY] = currentCount + 1
                Log.d("AssessmentVM", "📊 Quiz completed count updated: ${currentCount + 1}")

                // Update streak
                val lastActivity = preferences[LAST_ACTIVITY_DATE_KEY] ?: 0L
                val currentStreak = preferences[STREAK_DAYS_KEY] ?: 0

                val lastActivityDay = getStartOfDay(lastActivity)
                val daysDifference = TimeUnit.MILLISECONDS.toDays(today - lastActivityDay).toInt()

                val newStreak = when {
                    lastActivity == 0L -> 1 // First activity
                    daysDifference == 0 -> currentStreak // Same day, no change
                    daysDifference == 1 -> currentStreak + 1 // Consecutive day
                    else -> 1 // Streak broken, restart
                }

                preferences[LAST_ACTIVITY_DATE_KEY] = today
                preferences[STREAK_DAYS_KEY] = newStreak

                Log.d("AssessmentVM", "🔥 Streak updated: $newStreak days")
            }
        }
    }

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
}