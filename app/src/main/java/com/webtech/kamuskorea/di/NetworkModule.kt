package com.webtech.kamuskorea.di

import com.webtech.kamuskorea.data.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // REKOMENDASI: Gunakan HTTPS untuk production
    // Jika server Anda sudah support HTTPS, ganti dengan "https://"
    // Jika belum, pastikan network_security_config.xml sudah benar
    private const val BASE_URL = "https://webtechsolution.my.id/kamuskorea/"

    // Jika masih menggunakan HTTP (untuk development), pastikan gunakan:
    // private const val BASE_URL = "http://webtechsolution.my.id/kamuskorea/"
    // DAN pastikan network_security_config.xml sudah dikonfigurasi dengan benar

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Untuk production, ubah ke NONE atau BASIC
            // Untuk development, gunakan BODY untuk melihat detail request/response
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            // Tambahkan timeout untuk menghindari hang
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Retry on connection failure
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}