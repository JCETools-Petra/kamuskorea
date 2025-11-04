package com.webtech.kamuskorea.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Upsert
    suspend fun upsertAll(words: List<Word>)

    /**
     * DIPERBARUI:
     * Menggunakan 'COLLATE NOCASE' (cara standar SQLite) untuk pencarian
     * case-insensitive. Ini jauh lebih baik daripada LOWER().
     *
     * Kita juga akan mengirim :query DENGAN wildcard '%' dari ViewModel.
     */
    @Query("SELECT * FROM words WHERE korean_word LIKE :query COLLATE NOCASE OR romanization LIKE :query COLLATE NOCASE OR indonesian_translation LIKE :query COLLATE NOCASE")
    fun searchWords(query: String): Flow<List<Word>>

    @Query("SELECT * FROM words")
    fun getAllWords(): Flow<List<Word>>
}