package com.webtech.kamuskorea.data

import com.google.firebase.Timestamp

data class Ebook(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val pdfUrl: String = "",
    val createdAt: Timestamp? = null,
    val order: Int = 0,
    val isPremium: Boolean = false // <-- Tambahkan baris ini
)