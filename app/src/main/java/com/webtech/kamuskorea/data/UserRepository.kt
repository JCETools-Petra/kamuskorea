package com.webtech.kamuskorea.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.data.network.ApiService
import com.webtech.kamuskorea.data.network.PremiumActivationRequest
import com.webtech.kamuskorea.data.network.PremiumStatusResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // ✅ TAMBAHAN: Job untuk periodic check
    private var periodicCheckJob: Job? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                checkPremiumStatus()
                // ✅ MULAI periodic check saat user login
                startPeriodicStatusCheck()
            } else {
                _isPremium.value = false
                _premiumStatusResponse.value = null
                // ✅ STOP periodic check saat user logout
                stopPeriodicStatusCheck()
            }
        }

        if (auth.currentUser != null) {
            checkPremiumStatus()
            startPeriodicStatusCheck()
        }
    }

    // ✅ FUNGSI BARU: Periodic check setiap 5 menit
    private fun startPeriodicStatusCheck() {
        // Cancel existing job jika ada
        periodicCheckJob?.cancel()

        periodicCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(5 * 60 * 1000L) // 5 menit
                Log.d("UserRepository", "🔄 Periodic premium status check...")
                checkPremiumStatus()
            }
        }
        Log.d("UserRepository", "▶️ Periodic status check dimulai (setiap 5 menit)")
    }

    private fun stopPeriodicStatusCheck() {
        periodicCheckJob?.cancel()
        periodicCheckJob = null
        Log.d("UserRepository", "⏸️ Periodic status check dihentikan")
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

                        // ✅ LOGGING PERUBAHAN STATUS
                        if (_isPremium.value != isPremiumValue) {
                            Log.w("UserRepository", "⚠️ STATUS PREMIUM BERUBAH: ${_isPremium.value} → $isPremiumValue")
                        }

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

    suspend fun activatePremiumWithPurchase(purchaseToken: String): Boolean {
        Log.d("UserRepository", "=== ACTIVATE PREMIUM WITH PURCHASE ===")
        Log.d("UserRepository", "Purchase Token: $purchaseToken")

        return withContext(Dispatchers.IO) {
            try {
                if (auth.currentUser == null) {
                    Log.e("UserRepository", "❌ User belum login!")
                    return@withContext false
                }

                Log.d("UserRepository", "📡 Memanggil API activatePremium...")

                val request = PremiumActivationRequest(
                    purchase_token = purchaseToken,
                    duration_days = 30
                )

                val response = apiService.activatePremium(request)

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("UserRepository", "✅ Premium activated successfully!")
                    Log.d("UserRepository", "Is Premium: ${result?.isPremium}")
                    Log.d("UserRepository", "Expiry Date: ${result?.expiryDate}")

                    withContext(Dispatchers.Main) {
                        _isPremium.value = result?.isPremium ?: true
                        _premiumStatusResponse.value = result
                    }

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