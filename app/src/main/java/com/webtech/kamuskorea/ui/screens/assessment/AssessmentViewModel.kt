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
                val response = apiService.getAssessmentCategories(type)
                if (response.isSuccessful) {
                    _categories.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Gagal memuat kategori: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AssessmentViewModel", "Error fetching categories", e)
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
                val response = apiService.getAssessments(categoryId, type)
                if (response.isSuccessful) {
                    _assessments.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Gagal memuat assessment: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AssessmentViewModel", "Error fetching assessments", e)
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
                val response = apiService.getAssessmentQuestions(assessmentId)
                if (response.isSuccessful) {
                    _questions.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Gagal memuat soal: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AssessmentViewModel", "Error fetching questions", e)
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
    }

    // Next question
    fun nextQuestion() {
        if (_currentQuestionIndex.value < _questions.value.size - 1) {
            _currentQuestionIndex.value++
        }
    }

    // Previous question
    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
        }
    }

    // Go to specific question
    fun goToQuestion(index: Int) {
        if (index in _questions.value.indices) {
            _currentQuestionIndex.value = index
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
                val response = apiService.submitAssessment(assessmentId, request)
                if (response.isSuccessful) {
                    _assessmentResult.value = response.body()
                } else {
                    _error.value = "Gagal submit jawaban: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AssessmentViewModel", "Error submitting assessment", e)
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
                val response = apiService.getAssessmentResults(assessmentId)
                if (response.isSuccessful) {
                    _history.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Gagal memuat riwayat: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AssessmentViewModel", "Error fetching history", e)
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Reset result
    fun resetResult() {
        _assessmentResult.value = null
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }
}