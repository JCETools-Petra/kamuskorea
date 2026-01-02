package com.webtech.learningkorea.data

data class VideoHafalan(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val durationMinutes: Int = 0,
    val category: String? = null,
    val order: Int = 0,
    val isPremium: Boolean = false
)
