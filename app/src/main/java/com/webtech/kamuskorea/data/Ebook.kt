package com.webtech.kamuskorea.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Ebook(
    // Properti ini akan diisi dari ID dokumen Firestore, jadi tidak perlu ada di database
    var id: String = "",

    // Pastikan nama field ini sama persis dengan di Firestore
    val title: String = "",
    val description: String = "",
    val coverImageUrl: String = "", // Sesuai gambar: coverImageUrl
    val pdfUrl: String = "",

    // Field untuk penanda premium dan urutan
    val isPremium: Boolean = false,
    val order: Int = 0, // Tambahkan field order

    // Field timestamp
    @ServerTimestamp
    val createdAt: Date? = null // Sesuai gambar: createdAt
)