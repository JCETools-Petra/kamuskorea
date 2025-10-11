package com.webtech.kamuskorea.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY korean_word ASC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE korean_word LIKE :query || '%' OR romanization LIKE :query || '%'")
    fun searchWords(query: String): Flow<List<Word>>
}