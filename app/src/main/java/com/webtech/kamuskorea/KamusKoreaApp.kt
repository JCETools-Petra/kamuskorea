package com.webtech.kamuskorea

import android.util.Log
import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.appcheck.debug.internal.DebugAppCheckProvider

@HiltAndroidApp
class KamusKoreaApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Firebase
        FirebaseApp.initializeApp(this)

        // Inisialisasi Google AdMob
        MobileAds.initialize(this) { initializationStatus ->
            Log.d("AdMob", "✅ AdMob initialized: ${initializationStatus.adapterStatusMap}")
        }

        // Setup test devices untuk development (opsional, untuk testing)
        if (BuildConfig.DEBUG) {
            val testDeviceIds = listOf(
                "EMULATOR" // Emulator akan otomatis terdeteksi sebagai test device
                // Tambahkan device ID Anda di sini jika perlu untuk testing
                // Lihat logcat untuk "Use RequestConfiguration.Builder().setTestDeviceIds()" untuk mendapatkan ID
            )
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
            Log.d("AdMob", "⚠️ Test mode enabled for development")
        }

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

            // Log debug token untuk registrasi di Firebase Console
            Log.d("AppCheck", "════════════════════════════════════════════════")
            Log.d("AppCheck", "📋 FIREBASE APP CHECK DEBUG TOKEN:")
            Log.d("AppCheck", "════════════════════════════════════════════════")
            Log.d("AppCheck", "")
            Log.d("AppCheck", "Cari log dengan tag 'DebugAppCheckProvider' di Logcat")
            Log.d("AppCheck", "Atau filter dengan: tag:DebugAppCheckProvider")
            Log.d("AppCheck", "")
            Log.d("AppCheck", "Token akan terlihat seperti:")
            Log.d("AppCheck", "DebugAppCheckProvider: Enter this debug secret into the allow list in")
            Log.d("AppCheck", "the Firebase Console for your project: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
            Log.d("AppCheck", "")
            Log.d("AppCheck", "Kemudian daftarkan token tersebut di:")
            Log.d("AppCheck", "Firebase Console → App Check → Apps → Debug tokens")
            Log.d("AppCheck", "════════════════════════════════════════════════")

        } else {
            // RELEASE BUILD
            // PILIH SALAH SATU:

            // OPSI A: Tetap gunakan Debug Provider (untuk testing release build)
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d("AppCheck", "⚠️ Using DEBUG Provider (Release Build for Testing)")

            // Log debug token untuk registrasi di Firebase Console
            Log.d("AppCheck", "════════════════════════════════════════════════")
            Log.d("AppCheck", "📋 FIREBASE APP CHECK DEBUG TOKEN:")
            Log.d("AppCheck", "════════════════════════════════════════════════")
            Log.d("AppCheck", "")
            Log.d("AppCheck", "Cari log dengan tag 'DebugAppCheckProvider' di Logcat")
            Log.d("AppCheck", "Atau filter dengan: tag:DebugAppCheckProvider")
            Log.d("AppCheck", "")
            Log.d("AppCheck", "Token akan terlihat seperti:")
            Log.d("AppCheck", "DebugAppCheckProvider: Enter this debug secret into the allow list in")
            Log.d("AppCheck", "the Firebase Console for your project: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
            Log.d("AppCheck", "")
            Log.d("AppCheck", "Kemudian daftarkan token tersebut di:")
            Log.d("AppCheck", "Firebase Console → App Check → Apps → Debug tokens")
            Log.d("AppCheck", "════════════════════════════════════════════════")

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