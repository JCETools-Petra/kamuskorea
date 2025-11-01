package com.webtech.kamuskorea.data.network

import com.google.gson.annotations.SerializedName

/**
 * Respons untuk update kamus.
 */
data class KamusUpdateResponse(
    val latestVersion: Int,
    val words: List<KamusWord>
)

/**
 * Representasi satu kata dari API.
 */
data class KamusWord(
    val korean: String,
    val romanization: String,
    val indonesian: String
)

/**
 * Request body untuk update profil.
 */
data class UserProfileUpdateRequest(
    val name: String,
    val dob: String // Format YYYY-MM-DD
)

/**
 * Respons setelah upload foto profil.
 */
data class ProfilePictureUpdateResponse(
    val success: Boolean,
    val profilePictureUrl: String?
)

/**
 * Respons untuk profil pengguna.
 */
data class UserProfileResponse(
    val name: String?,
    val dob: String?, // Format YYYY-MM-DD
    val profilePictureUrl: String?
)

/**
 * Respons untuk status premium.
 */
data class PremiumStatusResponse(
    @SerializedName("isPremium")
    val isPremium: Boolean,

    @SerializedName("expiryDate")
    val expiryDate: String?
)

/**
 * Representasi Ebook dari API.
 */
data class EbookApiResponse(
    val id: Int,
    val title: String,
    val description: String,
    val coverImageUrl: String,
    val order: Int,
    val isPremium: Boolean,
    val pdfUrl: String
)

/**
 * Request body untuk mengaktifkan premium.
 */
data class PremiumActivationRequest(
    val purchase_token: String,
    val duration_days: Int = 365
)

// ============================================
// NEW: Google Sign-In Sync
// ============================================

/**
 * Request body untuk sync user dari Google Sign-In
 */
data class UserSyncRequest(
    val email: String?,
    val name: String?,
    val photoUrl: String?
)

/**
 * Response setelah sync user
 */
data class UserSyncResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("is_new")
    val isNew: Boolean
)