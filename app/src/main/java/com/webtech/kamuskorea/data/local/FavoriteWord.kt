package com.webtech.kamuskorea.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_words")
data class FavoriteWord(
    @PrimaryKey
    @ColumnInfo(name = "word_id")
    val wordId: Int,

    @ColumnInfo(name = "korean_word")
    val koreanWord: String,

    @ColumnInfo(name = "romanization")
    val romanization: String,

    @ColumnInfo(name = "indonesian_translation")
    val indonesianTranslation: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
