package com.webtech.kamuskorea.data.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Endpoint kamus (sesuaikan jika path berbeda)
    @GET("api.php/kamus/updates")
    suspend fun getKamusUpdates(@Query("version") localVersion: Int): Response<KamusUpdateResponse>

    // Endpoint status premium (sesuaikan jika path berbeda)
    @GET("api.php/user/premium/status")
    suspend fun checkPremiumStatus(
        @Header("Authorization") token: String // "Bearer <FirebaseIdToken>"
    ): Response<PremiumStatusResponse>

    // --- TAMBAHAN: Endpoint GET User Profile ---
    @GET("api.php/user/profile") // Atau "api.php/user"
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfileResponse>
    // --- AKHIR TAMBAHAN ---

    // Endpoint update detail profil (sesuaikan jika path berbeda)
    @PATCH("api.php/user/profile")
    suspend fun updateProfileDetails(
        @Header("Authorization") token: String,
        @Body profileData: UserProfileUpdateRequest
    ): Response<Unit> // Asumsi respons sukses 2xx tanpa body spesifik

    // Endpoint upload foto profil (sesuaikan jika path berbeda)
    @Multipart
    @POST("api.php/user/profile/picture")
    suspend fun updateProfilePicture(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part // Nama part harus "image" sesuai PHP
    ): Response<ProfilePictureUpdateResponse>

    // Endpoint Ebook (sesuaikan jika path berbeda)
    @GET("api.php/ebooks")
    suspend fun getEbooks(
        @Header("Authorization") token: String? // Token bisa null jika user belum login
    ): Response<List<EbookApiResponse>>
}