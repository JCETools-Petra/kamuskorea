package com.webtech.kamuskorea.data.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface Retrofit untuk semua panggilan jaringan.
 * SEMUA data class telah dipindahkan ke DataTransferObjects.kt
 */
interface ApiService {

    /**
     * Mengambil update kamus berdasarkan versi lokal.
     */
    @GET("api.php/kamus/updates")
    suspend fun getKamusUpdates(@Query("version") localVersion: Int): Response<KamusUpdateResponse>

    /**
     * Memeriksa status premium pengguna yang sedang login.
     */
    @GET("api.php/user/premium/status")
    suspend fun checkUserStatus(): Response<PremiumStatusResponse>

    /**
     * Mengaktifkan status premium untuk pengguna.
     * Tipe data 'PremiumActivationRequest' sekarang akan ditemukan
     * karena sudah ada di DataTransferObjects.kt
     */
    @POST("api.php/user/premium/activate")
    suspend fun activatePremium(@Body request: PremiumActivationRequest): Response<PremiumStatusResponse>

    /**
     * Mengambil profil pengguna yang sedang login.
     */
    @GET("api.php/user/profile")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    /**
     * Memperbarui detail profil (nama, tgl lahir) pengguna.
     */
    @PATCH("api.php/user/profile")
    suspend fun updateProfileDetails(@Body profileData: UserProfileUpdateRequest): Response<Unit>

    /**
     * Mengunggah foto profil baru.
     */
    @Multipart
    @POST("api.php/user/profile/picture")
    suspend fun updateProfilePicture(
        @Part image: MultipartBody.Part // Nama part "image" harus cocok dengan api.php
    ): Response<ProfilePictureUpdateResponse>

    /**
     * Mengambil daftar Ebook.
     */
    @GET("api.php/ebooks")
    suspend fun getEbooks(): Response<List<EbookApiResponse>>

    // Tidak ada data class di sini
}