package com.webtech.kamuskorea.di

import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.data.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import android.util.Log

/**
 * Interceptor kustom untuk menambahkan Token Autentikasi Firebase
 * ke setiap request API secara otomatis.
 *
 * FIXED:
 * - Force refresh token dengan getIdToken(true)
 * - Improved error handling
 * - Better logging
 */
class AuthInterceptor(private val firebaseAuth: FirebaseAuth) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val user = firebaseAuth.currentUser

        // Jika tidak ada user yang login, lanjutkan tanpa token
        if (user == null) {
            Log.w(TAG, "No authenticated user, proceeding without token")
            return chain.proceed(originalRequest)
        }

        // Dapatkan token dengan force refresh (true) untuk memastikan token fresh
        val token: String? = runBlocking {
            try {
                // ✅ PENTING: getIdToken(true) untuk force refresh
                val result = user.getIdToken(true).await()
                val freshToken = result?.token

                if (freshToken != null) {
                    Log.d(TAG, "✅ Token obtained successfully for UID: ${user.uid}")
                    Log.d(TAG, "Token preview: ${freshToken.take(20)}...")
                } else {
                    Log.e(TAG, "❌ Token is null after getIdToken")
                }

                freshToken
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to get Firebase token: ${e.message}", e)
                null
            }
        }

        // Buat request baru dengan Authorization header
        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("X-User-ID", user.uid) // Backup untuk development
                .build()
        } else {
            Log.e(TAG, "⚠️ Proceeding without token - authentication may fail")
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://webtechsolution.my.id/kamuskorea/"

    /**
     * Provide AuthInterceptor
     * Hilt akan otomatis inject FirebaseAuth dari FirebaseModule.kt
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(firebaseAuth: FirebaseAuth): AuthInterceptor {
        return AuthInterceptor(firebaseAuth)
    }

    /**
     * Provide OkHttpClient dengan AuthInterceptor
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BODY untuk development, ubah ke BASIC atau NONE untuk production
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // ✅ Auth interceptor PERTAMA
            .addInterceptor(logging)         // Logging kedua
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provide Retrofit instance
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provide ApiService
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}