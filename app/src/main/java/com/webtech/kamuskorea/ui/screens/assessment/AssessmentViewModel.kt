package com.webtech.kamuskorea.ui.screens.assessment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.assessment.*
import com.webtech.kamuskorea.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _categories = MutableStateFlow<List<AssessmentCategory>>(emptyList())
    val categories: StateFlow<List<AssessmentCategory>> = _categories.asStateFlow()

    private val _assessments = MutableStateFlow<List<Assessment>>(emptyList())
    val assessments: StateFlow<List<Assessment>> = _assessments.asStateFlow()

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

    private val _startTime = MutableStateFlow(0L)

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
        }
    }

    // Previous question
    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
            Log.d("AssessmentVM", "⬅️ Previous question: ${_currentQuestionIndex.value + 1}")
        }
    }

    // Go to specific question
    fun goToQuestion(index: Int) {
        if (index in _questions.value.indices) {
            _currentQuestionIndex.value = index
            Log.d("AssessmentVM", "🎯 Jump to question: ${index + 1}")
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
}