package com.webtech.kamuskorea.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "korean_word")
    val koreanWord: String,

    @ColumnInfo(name = "romanization")
    val romanization: String,

    @ColumnInfo(name = "indonesian_translation")
    val indonesianTranslation: String
)