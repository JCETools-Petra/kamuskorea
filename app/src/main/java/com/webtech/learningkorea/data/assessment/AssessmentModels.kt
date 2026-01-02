package com.webtech.learningkorea.data.assessment

import com.google.gson.annotations.SerializedName

/**
 * Kategori untuk quiz/ujian
 */
data class AssessmentCategory(
    val id: Int,
    val name: String,
    val type: String, // "quiz" atau "exam"
    val description: String?,
    @SerializedName("order_index") val orderIndex: Int
)

/**
 * Quiz atau Ujian
 */
data class Assessment(
    val id: Int,
    val title: String,
    val description: String?,
    val type: String, // "quiz" atau "exam"
    @SerializedName("duration_minutes") val durationMinutes: Int,
    @SerializedName("passing_score") val passingScore: Int,
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("category_name") val categoryName: String?,
    @SerializedName("question_count") val questionCount: Int,
    val locked: Boolean = false
)

/**
 * Pertanyaan
 */
data class Question(
    val id: Int,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("question_type") val questionType: String, // "text", "image", "audio", "video"
    @SerializedName("media_url") val mediaUrl: String?,
    @SerializedName("media_url_2") val mediaUrl2: String? = null,
    @SerializedName("media_url_3") val mediaUrl3: String? = null,

    // --- TAMBAHAN BARU UNTUK KOTAK SOAL (PROMPT BOX) ---
    @SerializedName("box_text") val boxText: String? = null,
    @SerializedName("box_media_url") val boxMediaUrl: String? = null,
    @SerializedName("box_position") val boxPosition: String? = "top",
    // ---------------------------------------------------

    @SerializedName("option_a") val optionA: String,
    @SerializedName("option_a_type") val optionAType: String? = "text", // "text", "image", "audio"
    @SerializedName("option_b") val optionB: String,
    @SerializedName("option_b_type") val optionBType: String? = "text",
    @SerializedName("option_c") val optionC: String?,
    @SerializedName("option_c_type") val optionCType: String? = "text",
    @SerializedName("option_d") val optionD: String?,
    @SerializedName("option_d_type") val optionDType: String? = "text",
    @SerializedName("option_a_alt") val optionAAlt: String? = null, // Alternative language (Korean/Indonesian)
    @SerializedName("option_b_alt") val optionBAlt: String? = null,
    @SerializedName("option_c_alt") val optionCAlt: String? = null,
    @SerializedName("option_d_alt") val optionDAlt: String? = null,
    @SerializedName("order_index") val orderIndex: Int
) {
    fun getOptions(): List<Pair<String, String>> {
        val options = mutableListOf<Pair<String, String>>()
        options.add("1" to optionA)
        options.add("2" to optionB)
        optionC?.let { options.add("3" to it) }
        optionD?.let { options.add("4" to it) }
        return options
    }

    fun getOptionsAlt(): List<Pair<String, String>> {
        val options = mutableListOf<Pair<String, String>>()
        options.add("1" to (optionAAlt ?: optionA))
        options.add("2" to (optionBAlt ?: optionB))
        optionC?.let { options.add("3" to (optionCAlt ?: it)) }
        optionD?.let { options.add("4" to (optionDAlt ?: it)) }
        return options
    }

    fun hasAlternativeOptions(): Boolean {
        return optionAAlt != null || optionBAlt != null
    }

    fun getOptionType(index: Int): String {
        return when (index) {
            0 -> optionAType ?: "text"
            1 -> optionBType ?: "text"
            2 -> optionCType ?: "text"
            3 -> optionDType ?: "text"
            else -> "text"
        }
    }

    fun getAllMediaUrls(): List<String> {
        // Menggabungkan media soal utama dengan media dari prompt box jika ada
        val list = mutableListOf<String>()
        if (!mediaUrl.isNullOrEmpty()) list.add(mediaUrl)
        if (!mediaUrl2.isNullOrEmpty()) list.add(mediaUrl2)
        if (!mediaUrl3.isNullOrEmpty()) list.add(mediaUrl3)
        if (!boxMediaUrl.isNullOrEmpty()) list.add(boxMediaUrl)
        return list
    }
}

/**
 * Jawaban user untuk satu pertanyaan
 */
data class UserAnswer(
    @SerializedName("question_id") val questionId: Int,
    val answer: String // "A", "B", "C", atau "D"
)

/**
 * Request body untuk submit assessment
 */
data class SubmitAssessmentRequest(
    val answers: List<UserAnswer>,
    @SerializedName("time_taken_seconds") val timeTakenSeconds: Int
)

/**
 * Detail hasil satu pertanyaan
 */
data class QuestionResult(
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("user_answer") val userAnswer: String,
    @SerializedName("correct_answer") val correctAnswer: String,
    @SerializedName("is_correct") val isCorrect: Boolean,
    val explanation: String?
)

/**
 * Response setelah submit assessment
 */
data class AssessmentResult(
    val score: Int,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("correct_answers") val correctAnswers: Int,
    val passed: Boolean,
    val details: List<QuestionResult>
)

/**
 * Riwayat hasil assessment
 */
data class AssessmentHistory(
    val id: Int,
    @SerializedName("assessment_id") val assessmentId: Int,
    @SerializedName("assessment_title") val assessmentTitle: String,
    val type: String,
    val score: Int,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("correct_answers") val correctAnswers: Int,
    @SerializedName("time_taken_seconds") val timeTakenSeconds: Int?,
    @SerializedName("completed_at") val completedAt: String
)

/**
 * Entry in the assessment leaderboard
 * NOTE: Email removed for privacy protection
 */
data class AssessmentLeaderboardEntry(
    @SerializedName("userName") val userName: String,
    val score: Int,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("completedAt") val completedAt: String? = null
)