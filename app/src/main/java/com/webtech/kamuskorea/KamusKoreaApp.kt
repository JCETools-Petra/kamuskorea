package com.webtech.kamuskorea

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KamusKoreaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Firebase
        FirebaseApp.initializeApp(this)

        // Dapatkan instance FirebaseAppCheck
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // ========================================
        // FORCE USE DEBUG PROVIDER
        // ========================================
        // SELALU gunakan Debug Provider untuk development
        // Uncomment baris Play Integrity hanya saat deploy ke Play Store

        if (BuildConfig.DEBUG) {
            // DEBUG BUILD - Selalu gunakan Debug Provider
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d("AppCheck", "✅ Using DEBUG Provider (Development)")

        } else {
            // RELEASE BUILD
            // PILIH SALAH SATU:

            // OPSI A: Tetap gunakan Debug Provider (untuk testing release build)
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d("AppCheck", "⚠️ Using DEBUG Provider (Release Build for Testing)")

            // OPSI B: Gunakan Play Integrity (uncomment saat deploy ke Play Store)
            /*
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d("AppCheck", "✅ Using PLAY INTEGRITY Provider (Production)")
            */
        }
    }
}