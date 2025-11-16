package com.webtech.kamuskorea.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {

    /**
     * Get all unique chapters with word count
     */
    @Query("""
        SELECT
            chapter_number as chapterNumber,
            chapter_title_korean as chapterTitleKorean,
            chapter_title_indonesian as chapterTitleIndonesian,
            COUNT(*) as wordCount
        FROM Vocabulary
        GROUP BY chapter_number, chapter_title_korean, chapter_title_indonesian
        ORDER BY chapter_number ASC
    """)
    fun getAllChapters(): Flow<List<ChapterInfo>>

    /**
     * Get all vocabulary words for a specific chapter
     */
    @Query("SELECT * FROM Vocabulary WHERE chapter_number = :chapterNumber ORDER BY id ASC")
    fun getVocabularyByChapter(chapterNumber: Int): Flow<List<Vocabulary>>

    /**
     * Get total word count
     */
    @Query("SELECT COUNT(*) FROM Vocabulary")
    suspend fun getTotalWordCount(): Int

    /**
     * Get word count for a specific chapter
     */
    @Query("SELECT COUNT(*) FROM Vocabulary WHERE chapter_number = :chapterNumber")
    suspend fun getChapterWordCount(chapterNumber: Int): Int
}
