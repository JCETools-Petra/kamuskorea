package com.webtech.learningkorea.data

data class Ebook(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    // --- PERBAIKAN DI SINI ---
    val coverImageUrl: String?, // Ubah menjadi String? (nullable)
    // --- AKHIR PERBAIKAN ---
    val pdfUrl: String = "",
    val order: Int = 0,
    val isPremium: Boolean = false
)