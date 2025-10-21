package com.webtech.kamuskorea.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Upsert
    suspend fun upsertAll(words: List<Word>)

    // Menggunakan nama kolom yang benar: 'korean_word', 'romanization', 'indonesian_translation'
    @Query("SELECT * FROM words WHERE korean_word LIKE :query || '%' OR romanization LIKE :query || '%' OR indonesian_translation LIKE :query || '%'")
    fun searchWords(query: String): Flow<List<Word>>

    @Query("SELECT * FROM words")
    fun getAllWords(): Flow<List<Word>>
}