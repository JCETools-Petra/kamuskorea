package com.webtech.kamuskorea.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_vocabulary")
data class FavoriteVocabulary(
    @PrimaryKey
    @ColumnInfo(name = "vocabulary_id")
    val vocabularyId: Int,

    @ColumnInfo(name = "korean_word")
    val koreanWord: String,

    @ColumnInfo(name = "indonesian_meaning")
    val indonesianMeaning: String,

    @ColumnInfo(name = "chapter_number")
    val chapterNumber: Int,

    @ColumnInfo(name = "chapter_title_indonesian")
    val chapterTitleIndonesian: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
