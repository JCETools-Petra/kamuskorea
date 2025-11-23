package com.webtech.learningkorea

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
class LearningKoreaApp : MultiDexApplication() {

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
                Log.d("AppCheck", "════════════════════════════════════════════════════════════════")
                Log.d("AppCheck", "📋 FIREBASE APP CHECK - SETUP UNTUK DEVELOPMENT")
                Log.d("AppCheck", "════════════════════════════════════════════════════════════════")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "🔧 OPSI 1: PERMISSIVE MODE (DIREKOMENDASIKAN UNTUK DEVELOPMENT)")
                Log.d("AppCheck", "────────────────────────────────────────────────────────────────")
                Log.d("AppCheck", "Cara tercepat untuk development tanpa perlu register debug token:")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "1. Buka Firebase Console:")
                Log.d("AppCheck", "   https://console.firebase.google.com/project/learning-korea/appcheck")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "2. Klik tab 'APIs'")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "3. Untuk setiap service (Firebase Auth, Firestore, dll):")
                Log.d("AppCheck", "   • Klik service → Settings (⚙️)")
                Log.d("AppCheck", "   • Ubah dari 'Enforced' menjadi 'Permissive'")
                Log.d("AppCheck", "   • Permissive = Allow requests tapi log yang tidak verified")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "4. SELESAI! Google Sign-In akan langsung berfungsi")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "📌 CATATAN: Permissive mode HANYA untuk development.")
                Log.d("AppCheck", "   Production tetap gunakan Enforced mode.")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "════════════════════════════════════════════════════════════════")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "🔑 OPSI 2: DEBUG TOKEN (JIKA TETAP INGIN GUNAKAN ENFORCED MODE)")
                Log.d("AppCheck", "────────────────────────────────────────────────────────────────")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "1. CARI debug token di Logcat dengan:")
                Log.d("AppCheck", "   • Tag: DebugAppCheckProvider")
                Log.d("AppCheck", "   • Atau filter: tag:DebugAppCheckProvider")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "2. Token format seperti ini:")
                Log.d("AppCheck", "   'Enter this debug secret into the allow list...'")
                Log.d("AppCheck", "   'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX'")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "3. DAFTARKAN token di Firebase Console:")
                Log.d("AppCheck", "   • Buka: https://console.firebase.google.com/project/learning-korea/appcheck")
                Log.d("AppCheck", "   • Menu: Apps → com.webtech.learningkorea")
                Log.d("AppCheck", "   • Klik: 'Manage debug tokens'")
                Log.d("AppCheck", "   • Klik: 'Add debug token'")
                Log.d("AppCheck", "   • Paste token dan beri nama (misal: 'My Device')")
                Log.d("AppCheck", "   • Klik 'Done'")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "4. TUNGGU ~1-2 menit untuk token aktif")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "5. RESTART aplikasi")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "════════════════════════════════════════════════════════════════")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "⚠️ TROUBLESHOOTING:")
                Log.d("AppCheck", "   • Google Sign-In gagal? → Gunakan Permissive Mode (Opsi 1)")
                Log.d("AppCheck", "   • 'Too many attempts'? → Tunggu 1 jam atau gunakan Permissive Mode")
                Log.d("AppCheck", "   • Debug token tidak muncul? → Cek Logcat dengan tag: DebugAppCheckProvider")
                Log.d("AppCheck", "")
                Log.d("AppCheck", "════════════════════════════════════════════════════════════════")

                // Force generate token untuk memastikan debug token muncul di log
                firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AppCheck", "✅ App Check token berhasil di-generate")
                        Log.d("AppCheck", "   Jika masih error saat login, gunakan PERMISSIVE MODE")
                    } else {
                        Log.e("AppCheck", "❌ App Check token gagal di-generate!")
                        Log.e("AppCheck", "   Error: ${task.exception?.message}")
                        Log.e("AppCheck", "")
                        Log.e("AppCheck", "🔴 SOLUSI CEPAT:")
                        Log.e("AppCheck", "   1. Buka Firebase Console → App Check → APIs")
                        Log.e("AppCheck", "   2. Ubah semua service dari 'Enforced' ke 'Permissive'")
                        Log.e("AppCheck", "   3. Restart aplikasi")
                        Log.e("AppCheck", "")
                        Log.e("AppCheck", "   ATAU daftarkan debug token (lihat instruksi di atas)")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppCheck", "❌ Error initializing App Check Debug Provider: ${e.message}")
                Log.e("AppCheck", "")
                Log.e("AppCheck", "🔴 SOLUSI:")
                Log.e("AppCheck", "   Ubah App Check ke PERMISSIVE MODE di Firebase Console")
                Log.e("AppCheck", "   https://console.firebase.google.com/project/learning-korea/appcheck")
                e.printStackTrace()
            }

        } else {
            // RELEASE BUILD - Use Play Integrity for Production
            try {
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.d("AppCheck", "✅ App Check Play Integrity Provider initialized")
                Log.d("AppCheck", "   Mode: Production (Enforced)")

                // Verify Play Integrity token
                firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AppCheck", "✅ Play Integrity token berhasil di-generate")
                    } else {
                        Log.e("AppCheck", "❌ Play Integrity token gagal!")
                        Log.e("AppCheck", "   Error: ${task.exception?.message}")
                        Log.e("AppCheck", "")
                        Log.e("AppCheck", "⚠️ PASTIKAN:")
                        Log.e("AppCheck", "   1. SHA-1 & SHA-256 dari Play Console sudah didaftarkan")
                        Log.e("AppCheck", "   2. Play Integrity API sudah diaktifkan")
                        Log.e("AppCheck", "   3. App sudah di-publish di Play Console (minimal Internal Testing)")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppCheck", "❌ Error initializing App Check Play Integrity Provider: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}