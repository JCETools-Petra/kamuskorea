package com.webtech.kamuskorea

import android.app.Application
// Import-import baru untuk App Check
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KamusKoreaApp : Application() {

    // --- TAMBAHKAN SEMUA KODE DI BAWAH INI ---
    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Firebase terlebih dahulu
        FirebaseApp.initializeApp(this)

        // Dapatkan instance FirebaseAppCheck
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // Cek apakah ini build DEBUG (dijalankan dari Android Studio)
        // atau build RELEASE (untuk Play Store)
        // 'BuildConfig.DEBUG' dibuat otomatis oleh Gradle
        if (BuildConfig.DEBUG) {
            // --- MODE DEBUG ---
            // Kita gunakan Debug Provider.
            // Ini akan mencetak token di Logcat yang harus Anda masukkan ke Firebase Console.
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // --- MODE RELEASE ---
            // Kita gunakan Play Integrity Provider.
            // Ini akan bekerja secara otomatis saat aplikasi diunduh dari Play Store.
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
    // --- BATAS PENAMBAHAN KODE ---
}