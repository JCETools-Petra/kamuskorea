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

/**
 * Interceptor kustom untuk menambahkan Token Autentikasi Firebase
 * ke setiap request API secara otomatis.
 */
class AuthInterceptor(private val firebaseAuth: FirebaseAuth) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = firebaseAuth.currentUser

        // Jika tidak ada user yang login, lanjutkan request tanpa token
        if (user == null) {
            return chain.proceed(chain.request())
        }

        // Jika ada user, ambil token-nya.
        // runBlocking digunakan untuk menjembatani dunia sinkron (Interceptor)
        // dengan dunia asinkron (getIdToken)
        val token: String? = runBlocking {
            try {
                // Menggunakan getIdToken(true) akan otomatis me-refresh token jika kedaluwarsa
                user.getIdToken(true).await()?.token
            } catch (e: Exception) {
                // Gagal mendapatkan token (mungkin user ter-logout, network error, dll)
                null
            }
        }

        // Buat request baru dengan header Authorization jika token berhasil didapat
        val newRequest = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            // Lanjutkan request asli jika gagal dapat token
            chain.request()
        }

        return chain.proceed(newRequest)
    }
}


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // BASE_URL Anda sudah benar menggunakan HTTPS
    private const val BASE_URL = "https://webtechsolution.my.id/kamuskorea/"

    //
    // FUNGSI 'provideFirebaseAuth' TELAH DIHAPUS DARI SINI
    // Hilt akan mengambilnya dari 'FirebaseModule.kt'
    //

    /**
     * Sediakan (Provide) AuthInterceptor.
     * Hilt akan otomatis meng-inject FirebaseAuth dari FirebaseModule.kt ke sini.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(firebaseAuth: FirebaseAuth): AuthInterceptor {
        return AuthInterceptor(firebaseAuth)
    }

    /**
     * Sediakan (Provide) OkHttpClient.
     * Fungsi ini sekarang menerima AuthInterceptor dan menambahkannya ke builder.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor // Hilt akan menyediakan ini dari fungsi di atas
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Untuk production, ubah ke NONE atau BASIC
            // Untuk development, gunakan BODY untuk melihat detail request/response
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor) // <-- INI PENAMBAHAN KUNCINYA
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Sediakan (Provide) Retrofit.
     * Fungsi ini tidak perlu diubah, Hilt akan otomatis
     * menggunakan OkHttpClient baru yang sudah ada interceptor-nya.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // OkHttpClient ini sekarang sudah punya AuthInterceptor
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Sediakan (Provide) ApiService.
     * Fungsi ini juga tidak perlu diubah.
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}