package com.webtech.kamuskorea.data

import com.webtech.kamuskorea.data.local.ChapterInfo
import com.webtech.kamuskorea.data.local.Vocabulary
import com.webtech.kamuskorea.data.local.VocabularyDao
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
}
