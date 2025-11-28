package com.webtech.learningkorea.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Upsert
    suspend fun upsertAll(words: List<Word>)

    @Query("SELECT * FROM words WHERE korean_word LIKE :query COLLATE NOCASE OR romanization LIKE :query COLLATE NOCASE OR indonesian_translation LIKE :query COLLATE NOCASE ORDER BY id ASC")
    fun searchWords(query: String): Flow<List<Word>>

    @Query("SELECT * FROM words ORDER BY id ASC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int
}