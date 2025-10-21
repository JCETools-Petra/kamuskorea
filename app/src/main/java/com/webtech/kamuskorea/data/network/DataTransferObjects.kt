package com.webtech.kamuskorea.data.network

import com.google.gson.annotations.SerializedName

// --- Kamus Update ---
data class KamusUpdateResponse(
    val latestVersion: Int,
    val words: List<WordApi>
)

data class WordApi(
    // Sesuaikan tipe data 'id' jika di server bukan Int
    val id: Int,
    val korean: String,
    val romanization: String,
    val indonesian: String
)

// --- User Profile ---
data class UserProfileUpdateRequest(
    val name: String,
    val dob: String // Format "YYYY-MM-DD"
)

data class ProfilePictureUpdateResponse(
    val success: Boolean,
    val profilePictureUrl: String? // Bisa null jika gagal update DB
)

// --- TAMBAHAN: Untuk GET User Profile ---
data class UserProfileResponse(
    val name: String?,
    val dob: String?, // Format "YYYY-MM-DD" atau null
    val profilePictureUrl: String?
)

// --- Premium Status ---
data class PremiumStatusResponse(
    val isPremium: Boolean,
    val expiryDate: String? // Format "YYYY-MM-DD" atau null
)

// --- Ebook (dari api.php Anda) ---
data class EbookApiResponse(
    val id: Int,
    val title: String,
    val description: String,
    val coverImageUrl: String,
    @SerializedName("order") val order: Int,
    val isPremium: Boolean,
    val pdfUrl: String
)