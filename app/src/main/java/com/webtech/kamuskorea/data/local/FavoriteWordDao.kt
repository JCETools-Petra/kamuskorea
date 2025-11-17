package com.webtech.kamuskorea.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteWordDao {
    @Query("SELECT * FROM favorite_words ORDER BY added_at DESC")
    fun getAllFavorites(): Flow<List<FavoriteWord>>

    @Query("SELECT word_id FROM favorite_words")
    fun getAllFavoriteIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_words WHERE word_id = :wordId)")
    fun isFavorite(wordId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favoriteWord: FavoriteWord)

    @Query("DELETE FROM favorite_words WHERE word_id = :wordId")
    suspend fun removeFavorite(wordId: Int)

    @Query("SELECT COUNT(*) FROM favorite_words")
    fun getFavoriteCount(): Flow<Int>
}
