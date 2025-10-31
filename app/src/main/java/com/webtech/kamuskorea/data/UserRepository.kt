package com.webtech.kamuskorea.data

import android.util.Log
import androidx.datastore.core.DataStore // Import DataStore
import androidx.datastore.preferences.core.Preferences // Import Preferences
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.data.network.ApiService
import com.webtech.kamuskorea.data.network.PremiumStatusResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val auth: FirebaseAuth,
    // Inject DataStore jika perlu menyimpan preferensi terkait user
    // private val dataStore: DataStore<Preferences>
) {
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _premiumStatusResponse = MutableStateFlow<PremiumStatusResponse?>(null)
    val premiumStatusResponse: StateFlow<PremiumStatusResponse?> = _premiumStatusResponse.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                checkPremiumStatus()
            } else {
                _isPremium.value = false
                _premiumStatusResponse.value = null
            }
        }
        // Panggil saat inisialisasi jika user sudah login
        if (auth.currentUser != null) {
            checkPremiumStatus()
        }
    }

    /**
     * Helper suspend function untuk mendapatkan Firebase ID Token user saat ini.
     * Menggunakan force refresh (true) untuk mendapatkan token terbaru.
     */
    private suspend fun getFirebaseIdToken(forceRefresh: Boolean = true): String? {
        return try {
            auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (e: Exception) {
            Log.e("UserRepository", "Gagal mendapatkan Firebase ID Token", e)
            null
        }
    }

    /**
     * Memanggil API backend untuk mengecek status premium user.
     * Hasilnya akan diperbarui di state flow `isPremium`.
     */
    fun checkPremiumStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            // Coba dapatkan token tanpa force refresh dulu, baru force jika perlu
            var token = getFirebaseIdToken(false)
            if (token == null && auth.currentUser != null) {
                Log.w("UserRepository", "Mencoba force refresh token untuk cek premium...")
                token = getFirebaseIdToken(true)
            }


            if (token == null) {
                Log.w("UserRepository", "User tidak login atau token tidak valid, tidak bisa cek premium status API")
                withContext(Dispatchers.Main) {
                    _isPremium.value = false
                    _premiumStatusResponse.value = null
                }
                return@launch
            }

            try {
                Log.d("UserRepository", "Memanggil API checkPremiumStatus...")
                // --- PERBAIKAN DI SINI ---
                val response = apiService.checkUserStatus() // Menggunakan nama fungsi yang benar
                // --- AKHIR PERBAIKAN ---

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val premiumStatus = response.body()
                        _isPremium.value = premiumStatus?.isPremium ?: false
                        _premiumStatusResponse.value = premiumStatus
                        Log.d("UserRepository", "Status premium dari API: ${premiumStatus?.isPremium}, Expiry: ${premiumStatus?.expiryDate}")
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("UserRepository", "Error cek premium API: ${response.code()} - $errorBody")
                        _isPremium.value = false // Set ke false jika error
                        _premiumStatusResponse.value = null
                    }
                }

            } catch (e: Exception) {
                Log.e("UserRepository", "Exception saat cek premium API", e)
                withContext(Dispatchers.Main) {
                    _isPremium.value = false
                    _premiumStatusResponse.value = null
                }
            }
        }
    }


    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getUserId(): String? {
        return auth.currentUser?.uid
    }
}