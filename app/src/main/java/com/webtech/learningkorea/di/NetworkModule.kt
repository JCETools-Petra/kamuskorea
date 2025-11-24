package com.webtech.learningkorea.di

import com.google.firebase.auth.FirebaseAuth
import com.webtech.learningkorea.data.network.ApiService
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
import com.webtech.learningkorea.BuildConfig

/**
 * Interceptor kustom untuk menambahkan Token Autentikasi Firebase
 * ke setiap request API secara otomatis.
 *
 * FIXED v2:
 * - Added token caching to minimize blocking calls
 * - Only refresh when token is expired or missing
 * - Use getIdToken(false) for cached token, only force refresh when needed
 * - Improved performance by avoiding runBlocking on every request
 */
class AuthInterceptor(private val firebaseAuth: FirebaseAuth) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val TOKEN_EXPIRY_BUFFER_MS = 5 * 60 * 1000L // 5 minutes before actual expiry
    }

    // Token cache with expiration
    @Volatile
    private var cachedToken: String? = null
    @Volatile
    private var tokenExpiryTime: Long = 0L
    private val tokenLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val user = firebaseAuth.currentUser

        // Jika tidak ada user yang login, lanjutkan tanpa token
        if (user == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "No authenticated user, proceeding without token")
            }
            return chain.proceed(originalRequest)
        }

        // Get token with caching to minimize blocking
        val token: String? = synchronized(tokenLock) {
            val now = System.currentTimeMillis()

            // Use cached token if still valid
            if (cachedToken != null && now < tokenExpiryTime) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Using cached token (expires in ${(tokenExpiryTime - now) / 1000}s)")
                }
                cachedToken
            } else {
                // Token expired or missing, fetch new one
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Token expired or missing, fetching new token...")
                }
                runBlocking {
                    try {
                        // Use getIdToken(false) first for cached token
                        val result = user.getIdToken(false).await()
                        val freshToken = result?.token

                        if (freshToken != null) {
                            // Cache the token with expiration (Firebase tokens valid for 1 hour)
                            cachedToken = freshToken
                            tokenExpiryTime = now + (3600 * 1000L) - TOKEN_EXPIRY_BUFFER_MS // 55 minutes

                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "✅ Token cached (valid for 55 min)")
                            }
                        }

                        freshToken
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "❌ Failed to get Firebase token: ${e.message}")
                        }
                        null
                    }
                }
            }
        }

        // Buat request baru dengan Authorization header
        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("X-User-ID", user.uid)
                .build()
        } else {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "⚠️ Proceeding without token - authentication may fail")
            }
            originalRequest
        }

        return chain.proceed(newRequest)
    }

    /**
     * Clear cached token (useful for logout or token invalidation)
     */
    fun clearTokenCache() {
        synchronized(tokenLock) {
            cachedToken = null
            tokenExpiryTime = 0L
        }
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
            // BODY untuk development, NONE untuk production
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
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