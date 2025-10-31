package com.webtech.kamuskorea.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.data.network.ApiService
import com.webtech.kamuskorea.data.network.PremiumActivationRequest
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
    private val auth: FirebaseAuth
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

        if (auth.currentUser != null) {
            checkPremiumStatus()
        }
    }

    private suspend fun getFirebaseIdToken(forceRefresh: Boolean = true): String? {
        return try {
            auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (e: Exception) {
            Log.e("UserRepository", "Gagal mendapatkan Firebase ID Token", e)
            null
        }
    }

    fun checkPremiumStatus() {
        Log.d("UserRepository", "=== CHECK PREMIUM STATUS ===")

        CoroutineScope(Dispatchers.IO).launch {
            var token = getFirebaseIdToken(false)
            if (token == null && auth.currentUser != null) {
                Log.w("UserRepository", "Token null, mencoba force refresh...")
                token = getFirebaseIdToken(true)
            }

            if (token == null) {
                Log.w("UserRepository", "❌ User tidak login atau token tidak valid")
                withContext(Dispatchers.Main) {
                    _isPremium.value = false
                    _premiumStatusResponse.value = null
                }
                return@launch
            }

            try {
                Log.d("UserRepository", "📡 Memanggil API checkUserStatus...")
                val response = apiService.checkUserStatus()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val premiumStatus = response.body()
                        val isPremiumValue = premiumStatus?.isPremium ?: false

                        _isPremium.value = isPremiumValue
                        _premiumStatusResponse.value = premiumStatus

                        Log.d("UserRepository", "✅ Status premium dari API: $isPremiumValue")
                        Log.d("UserRepository", "Expiry Date: ${premiumStatus?.expiryDate}")
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("UserRepository", "❌ Error cek premium API: ${response.code()} - $errorBody")
                        _isPremium.value = false
                        _premiumStatusResponse.value = null
                    }
                }

            } catch (e: Exception) {
                Log.e("UserRepository", "❌ Exception saat cek premium API", e)
                withContext(Dispatchers.Main) {
                    _isPremium.value = false
                    _premiumStatusResponse.value = null
                }
            }
        }
    }

    /**
     * FUNGSI BARU: Aktivasi premium dengan purchase token
     */
    suspend fun activatePremiumWithPurchase(purchaseToken: String): Boolean {
        Log.d("UserRepository", "=== ACTIVATE PREMIUM WITH PURCHASE ===")
        Log.d("UserRepository", "Purchase Token: $purchaseToken")

        return withContext(Dispatchers.IO) {
            try {
                // Pastikan user sudah login
                if (auth.currentUser == null) {
                    Log.e("UserRepository", "❌ User belum login!")
                    return@withContext false
                }

                Log.d("UserRepository", "📡 Memanggil API activatePremium...")

                // Buat request body
                val request = PremiumActivationRequest(
                    purchase_token = purchaseToken,
                    duration_days = 30 // Sesuaikan dengan durasi langganan Anda
                )

                val response = apiService.activatePremium(request)

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("UserRepository", "✅ Premium activated successfully!")
                    Log.d("UserRepository", "Is Premium: ${result?.isPremium}")
                    Log.d("UserRepository", "Expiry Date: ${result?.expiryDate}")

                    // Update state lokal
                    withContext(Dispatchers.Main) {
                        _isPremium.value = result?.isPremium ?: true
                        _premiumStatusResponse.value = result
                    }

                    // Refresh status untuk memastikan sinkronisasi
                    checkPremiumStatus()

                    return@withContext true
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("UserRepository", "❌ Error aktivasi premium: ${response.code()} - $errorBody")
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e("UserRepository", "❌ Exception saat aktivasi premium", e)
                return@withContext false
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