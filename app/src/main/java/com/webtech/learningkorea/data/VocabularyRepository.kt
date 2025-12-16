package com.webtech.learningkorea.data

import com.webtech.learningkorea.data.local.ChapterInfo
import com.webtech.learningkorea.data.local.Vocabulary
import com.webtech.learningkorea.data.local.VocabularyDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyRepository @Inject constructor(
    private val vocabularyDao: VocabularyDao
) {
    /**
     * Get all chapters with word count
     */
    fun getAllChapters(): Flow<List<ChapterInfo>> {
        return vocabularyDao.getAllChapters()
    }

    /**
     * Get vocabulary words for a specific chapter
     */
    fun getVocabularyByChapter(chapterNumber: Int): Flow<List<Vocabulary>> {
        return vocabularyDao.getVocabularyByChapter(chapterNumber)
    }

    /**
     * Get total word count across all chapters
     */
    suspend fun getTotalWordCount(): Int {
        return vocabularyDao.getTotalWordCount()
    }

    /**
     * Get word count for a specific chapter
     */
    suspend fun getChapterWordCount(chapterNumber: Int): Int {
        return vocabularyDao.getChapterWordCount(chapterNumber)
    }

    /**
     * Generate a quiz question with 1 correct answer and 3 wrong answers
     * Returns QuizQuestion with korean_word as question and indonesian_meaning as answers
     */
    suspend fun generateQuizQuestion(): QuizQuestion? {
        // Get random word for the question
        val correctWord = vocabularyDao.getRandomWord() ?: return null

        // Get 3 random wrong answers (different from correct answer)
        val wrongAnswers = vocabularyDao.getRandomWordsExcluding(
            excludeIds = listOf(correctWord.id ?: 0),
            limit = 3
        )

        if (wrongAnswers.size < 3) {
            // Not enough words in database for quiz
            return null
        }

        // Combine correct and wrong answers, then shuffle
        val allOptions = mutableListOf<QuizOption>().apply {
            add(QuizOption(correctWord.indonesianMeaning ?: "", isCorrect = true))
            wrongAnswers.forEach { word ->
                add(QuizOption(word.indonesianMeaning ?: "", isCorrect = false))
            }
        }.shuffled()

        return QuizQuestion(
            question = correctWord.koreanWord ?: "",
            options = allOptions,
            correctAnswer = correctWord.indonesianMeaning ?: ""
        )
    }
}

/**
 * Data class for quiz question
 */
data class QuizQuestion(
    val question: String,           // Korean word
    val options: List<QuizOption>,  // 4 options (1 correct, 3 wrong)
    val correctAnswer: String       // Indonesian meaning (correct answer)
)

/**
 * Data class for quiz option
 */
data class QuizOption(
    val text: String,       // Indonesian meaning
    val isCorrect: Boolean  // Whether this is the correct answer
)
