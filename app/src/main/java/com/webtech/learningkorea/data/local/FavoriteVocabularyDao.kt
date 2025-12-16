package com.webtech.learningkorea.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteVocabularyDao {
    @Query("SELECT * FROM favorite_vocabulary ORDER BY added_at DESC")
    fun getAllFavorites(): Flow<List<FavoriteVocabulary>>

    @Query("SELECT vocabulary_id FROM favorite_vocabulary")
    fun getAllFavoriteIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_vocabulary WHERE vocabulary_id = :vocabularyId)")
    fun isFavorite(vocabularyId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favoriteVocabulary: FavoriteVocabulary)

    @Query("DELETE FROM favorite_vocabulary WHERE vocabulary_id = :vocabularyId")
    suspend fun removeFavorite(vocabularyId: Int)

    @Query("SELECT COUNT(*) FROM favorite_vocabulary")
    fun getFavoriteCount(): Flow<Int>
}
