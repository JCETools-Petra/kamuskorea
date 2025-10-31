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
 * Properti ini (isPremium, expiryDate) cocok dengan api.php.
 */
data class PremiumStatusResponse(
    @SerializedName("isPremium")
    val isPremium: Boolean,

    @SerializedName("expiryDate")
    val expiryDate: String?
)

/**
 * Representasi Ebook dari API.
 * (Cocok dengan api.php handleGetEbooks)
 */
data class EbookApiResponse(
    val id: Int,
    val title: String,
    val description: String,
    val coverImageUrl: String,
    val order: Int,
    val isPremium: Boolean,
    val pdfUrl: String // Akan kosong jika isPremium=true dan user free
)

/**
 * *** KELAS BARU YANG HILANG DITAMBAHKAN DI SINI ***
 * Request body untuk mengaktifkan premium.
 */
data class PremiumActivationRequest(
    val purchase_token: String,
    val duration_days: Int = 365 // Anda bisa sesuaikan default ini
)