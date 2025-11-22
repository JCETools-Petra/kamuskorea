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

@HiltAndroidApp
class KamusKoreaApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Firebase
        FirebaseApp.initializeApp(this)

        // Inisialisasi Google AdMob
        MobileAds.initialize(this) { initializationStatus ->
            if (BuildConfig.DEBUG) {
                Log.d("AdMob", "✅ AdMob initialized: ${initializationStatus.adapterStatusMap}")
            }
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
        // CONFIGURE APP CHECK PROVIDER
        // ========================================
        if (BuildConfig.DEBUG) {
            // DEBUG BUILD - Selalu gunakan Debug Provider
            try {
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d("AppCheck", "✅ App Check Debug Provider initialized")
                Log.d("AppCheck", "════════════════════════════════════════════════")
                Log.d("AppCheck", "📋 FIREBASE APP CHECK DEBUG TOKEN SETUP:")
                Log.d("AppCheck", "════════════════════════════════════════════════")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "1. CARI debug token di Logcat dengan:")
                Log.d("AppCheck", "   • Tag: DebugAppCheckProvider")
                Log.d("AppCheck", "   • Filter: tag:DebugAppCheckProvider")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "2. Token format seperti ini:")
                Log.d("AppCheck", "   DebugAppCheckProvider: Enter this debug secret")
                Log.d("AppCheck", "   into the allow list in the Firebase Console")
                Log.d("AppCheck", "   for your project: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "3. COPY token tersebut (XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX)")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "4. DAFTARKAN token di Firebase Console:")
                Log.d("AppCheck", "   • Buka: https://console.firebase.google.com/")
                Log.d("AppCheck", "   • Pilih project: kamus-korea-apps-dcf09")
                Log.d("AppCheck", "   • Menu: App Check → Apps")
                Log.d("AppCheck", "   • Pilih app: com.webtech.kamuskorea")
                Log.d("AppCheck", "   • Klik: Debug tokens → Add debug token")
                Log.d("AppCheck", "   • Paste token dan klik Add")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "5. TUNGGU ~1-2 menit untuk token aktif")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "6. RESTART aplikasi")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "════════════════════════════════════════════════")

                // Force generate token untuk memastikan debug token muncul di log
                firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AppCheck", "✅ App Check token berhasil di-generate")
                    } else {
                        Log.e("AppCheck", "❌ App Check token gagal di-generate: ${task.exception?.message}")
                        Log.e("AppCheck", "⚠️ PASTIKAN debug token sudah didaftarkan di Firebase Console!")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppCheck", "❌ Error initializing App Check Debug Provider: ${e.message}")
                e.printStackTrace()
            }

        } else {
            // RELEASE BUILD - Use Play Integrity for Production
            try {
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.d("AppCheck", "✅ App Check Play Integrity Provider initialized")
            } catch (e: Exception) {
                Log.e("AppCheck", "❌ Error initializing App Check Play Integrity Provider: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}