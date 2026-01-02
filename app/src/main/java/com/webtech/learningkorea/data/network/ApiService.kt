package com.webtech.learningkorea.data.network

import com.webtech.learningkorea.data.assessment.*
import com.webtech.learningkorea.gamification.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface Retrofit untuk semua panggilan jaringan.
 *
 * CATATAN: Authorization header akan ditambahkan otomatis oleh AuthInterceptor,
 * jadi tidak perlu @Header("Authorization") di setiap endpoint.
 */
interface ApiService {

    @GET("api.php/kamus/updates")
    suspend fun getKamusUpdates(@Query("version") localVersion: Int): Response<KamusUpdateResponse>

    // ✅ HAPUS @Header("Authorization") - sudah ditangani oleh AuthInterceptor
    @GET("api.php/user/premium/status")
    suspend fun checkUserStatus(): Response<PremiumStatusResponse>

    // ✅ HAPUS @Header("Authorization") - sudah ditangani oleh AuthInterceptor
    @POST("api.php/user/premium/activate")
    suspend fun activatePremium(
        @Body request: PremiumActivationRequest
    ): Response<PremiumStatusResponse>

    @GET("api.php/user/profile")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @PATCH("api.php/user/profile")
    suspend fun updateProfileDetails(@Body profileData: UserProfileUpdateRequest): Response<Unit>

    @Multipart
    @POST("api.php/user/profile/picture")
    suspend fun updateProfilePicture(@Part image: MultipartBody.Part): Response<ProfilePictureUpdateResponse>

    @GET("api.php/ebooks")
    suspend fun getEbooks(): Response<List<EbookApiResponse>>

    @GET("api.php/video_hafalan")
    suspend fun getVideoHafalan(): Response<List<VideoHafalanApiResponse>>

    // ========== ASSESSMENT ENDPOINTS ==========

    @GET("api.php/assessments/categories")
    suspend fun getAssessmentCategories(@Query("type") type: String? = null): Response<List<AssessmentCategory>>

    @GET("api.php/assessments")
    suspend fun getAssessments(
        @Query("category_id") categoryId: Int? = null,
        @Query("type") type: String? = null
    ): Response<List<Assessment>>

    @GET("api.php/assessments/{id}/questions")
    suspend fun getAssessmentQuestions(@Path("id") assessmentId: Int): Response<List<Question>>

    @POST("api.php/assessments/{id}/submit")
    suspend fun submitAssessment(
        @Path("id") assessmentId: Int,
        @Body request: SubmitAssessmentRequest
    ): Response<AssessmentResult>

    @GET("api.php/results")
    suspend fun getAssessmentResults(@Query("assessment_id") assessmentId: Int? = null): Response<List<AssessmentHistory>>

    @GET("api.php/assessments/leaderboard")
    suspend fun getAssessmentLeaderboard(
        @Query("type") type: String,
        @Query("assessment_id") assessmentId: Int? = null
    ): Response<List<AssessmentLeaderboardEntry>>

    // ✅ HAPUS @Header("Authorization") - sudah ditangani oleh AuthInterceptor
    @POST("api.php/user/sync")
    suspend fun syncUser(
        @Body request: UserSyncRequest
    ): Response<UserSyncResponse>

    // ========== PASSWORD RESET ENDPOINTS ==========

    @POST("api.php/auth/forgot-password")
    suspend fun requestPasswordReset(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    @POST("api.php/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ResetPasswordResponse>

    @GET("api.php/auth/verify-reset-token")
    suspend fun verifyResetToken(@Query("token") token: String): Response<VerifyTokenResponse>

    // ========== GAMIFICATION ENDPOINTS ==========

    @POST("api.php/gamification/sync-xp")
    suspend fun syncXp(@Body request: SyncXpRequest): Response<SyncXpResponse>

    @GET("api.php/gamification/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 100): Response<LeaderboardResponse>

    @GET("api.php/gamification/user-rank/{user_id}")
    suspend fun getUserRank(@Path("user_id") userId: String): Response<UserRankResponse>

    @POST("api.php/gamification/add-xp")
    suspend fun addXp(@Body request: AddXpRequest): Response<AddXpResponse>
}