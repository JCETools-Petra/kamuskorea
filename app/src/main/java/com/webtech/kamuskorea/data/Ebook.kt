package com.webtech.kamuskorea.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Ebook(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val pdfUrl: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)