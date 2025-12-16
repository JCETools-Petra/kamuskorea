package com.webtech.learningkorea.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Vocabulary")
data class Vocabulary(
    @PrimaryKey
    val id: Int?,

    @ColumnInfo(name = "chapter_number")
    val chapterNumber: Int?,

    @ColumnInfo(name = "chapter_title_korean")
    val chapterTitleKorean: String?,

    @ColumnInfo(name = "chapter_title_indonesian")
    val chapterTitleIndonesian: String?,

    @ColumnInfo(name = "korean_word")
    val koreanWord: String?,

    @ColumnInfo(name = "indonesian_meaning")
    val indonesianMeaning: String?
)

/**
 * Data class untuk menampilkan informasi chapter
 */
data class ChapterInfo(
    val chapterNumber: Int,
    val chapterTitleKorean: String,
    val chapterTitleIndonesian: String,
    val wordCount: Int
)
