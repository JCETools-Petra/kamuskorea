package com.webtech.kamuskorea.data.network

import com.webtech.kamuskorea.data.assessment.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface Retrofit untuk semua panggilan jaringan.
 */
interface ApiService {

    @GET("api.php/kamus/updates")
    suspend fun getKamusUpdates(@Query("version") localVersion: Int): Response<KamusUpdateResponse>

    @GET("api.php/user/premium/status")
    suspend fun checkUserStatus(): Response<PremiumStatusResponse>

    @POST("api.php/user/premium/activate")
    suspend fun activatePremium(@Body request: PremiumActivationRequest): Response<PremiumStatusResponse>

    @GET("api.php/user/profile")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @PATCH("api.php/user/profile")
    suspend fun updateProfileDetails(@Body profileData: UserProfileUpdateRequest): Response<Unit>

    @Multipart
    @POST("api.php/user/profile/picture")
    suspend fun updateProfilePicture(@Part image: MultipartBody.Part): Response<ProfilePictureUpdateResponse>

    @GET("api.php/ebooks")
    suspend fun getEbooks(): Response<List<EbookApiResponse>>

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

    @POST("api.php/user/sync")
    suspend fun syncUser(@Body userData: UserSyncRequest): Response<UserSyncResponse>

    // ========== PASSWORD RESET ENDPOINTS (NEW) ==========

    /**
     * Request password reset - mengirim email dengan link reset
     */
    @POST("api.php/auth/forgot-password")
    suspend fun requestPasswordReset(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    /**
     * Reset password dengan token
     */
    @POST("api.php/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ResetPasswordResponse>

    /**
     * Verify reset token (optional - untuk cek apakah token masih valid)
     */
    @GET("api.php/auth/verify-reset-token")
    suspend fun verifyResetToken(@Query("token") token: String): Response<VerifyTokenResponse>

}