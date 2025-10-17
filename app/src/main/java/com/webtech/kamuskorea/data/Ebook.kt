package com.webtech.kamuskorea.data

data class Ebook(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val pdfUrl: String = "",
    val isPremium: Boolean = false // Tambahkan ini
)