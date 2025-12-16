package com.webtech.learningkorea.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.webtech.learningkorea.data.network.ApiService
import com.webtech.learningkorea.data.network.PremiumActivationRequest
import com.webtech.learningkorea.data.network.PremiumStatusResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk mengelola status premium user
 *
 * FIXED:
 * - Hapus manual token handling (sudah ditangani oleh AuthInterceptor)
 * - Simplified API calls
 * - Better error handling
 */
@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "UserRepository"
        private const val CHECK_INTERVAL_MS = 5 * 60 * 1000L // 5 menit
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _premiumStatusResponse = MutableStateFlow<PremiumStatusResponse?>(null)
    val premiumStatusResponse: StateFlow<PremiumStatusResponse?> = _premiumStatusResponse.asStateFlow()

    private var periodicCheckJob: Job? = null

    // Properti untuk melacak status login
    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Monitor auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d(TAG, "✅ User logged in: ${user.uid}")
                _isLoggedIn.value = true
                checkPremiumStatus()
                startPeriodicStatusCheck()
            } else {
                Log.d(TAG, "❌ User logged out")
                _isLoggedIn.value = false
                _isPremium.value = false
                _premiumStatusResponse.value = null
                stopPeriodicStatusCheck()
            }
        }

        // Initial check jika sudah ada user yang login
        if (auth.currentUser != null) {
            Log.d(TAG, "🔄 Initial premium status check")
            checkPremiumStatus()
            startPeriodicStatusCheck()
        }
    }

    /**
     * Mulai periodic check setiap 5 menit
     */
    private fun startPeriodicStatusCheck() {
        periodicCheckJob?.cancel()
        periodicCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(CHECK_INTERVAL_MS)
                Log.d(TAG, "🔄 Periodic premium status check...")
                checkPremiumStatus()
            }
        }
        Log.d(TAG, "▶️ Periodic status check started (every 5 minutes)")
    }

    /**
     * Stop periodic check
     */
    private fun stopPeriodicStatusCheck() {
        periodicCheckJob?.cancel()
        periodicCheckJob = null
        Log.d(TAG, "⏸️ Periodic status check stopped")
    }

    /**
     * Check premium status dari API
     *
     * ✅ FIXED: Tidak perlu manual token handling
     * Token akan otomatis ditambahkan oleh AuthInterceptor
     */
    fun checkPremiumStatus() {
        Log.d(TAG, "=== CHECK PREMIUM STATUS ===")

        // Validasi user login
        if (auth.currentUser == null) {
            Log.w(TAG, "❌ User not logged in (checkPremiumStatus)")
            _isPremium.value = false
            _premiumStatusResponse.value = null
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "📡 Calling API checkUserStatus...")

                // ✅ PERBAIKAN: Tidak perlu kirim token manual
                // AuthInterceptor akan otomatis menambahkan Bearer token
                val response = apiService.checkUserStatus()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val premiumStatus = response.body()
                        val isPremiumValue = premiumStatus?.isPremium ?: false

                        // Log jika status berubah
                        if (_isPremium.value != isPremiumValue) {
                            Log.w(TAG, "⚠️ PREMIUM STATUS CHANGED: ${_isPremium.value} → $isPremiumValue")
                        }

                        _isPremium.value = isPremiumValue
                        _premiumStatusResponse.value = premiumStatus

                        Log.d(TAG, "✅ Premium status from API: $isPremiumValue")
                        Log.d(TAG, "Expiry Date: ${premiumStatus?.expiryDate}")
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e(TAG, "❌ API error: ${response.code()} - $errorBody")

                        // Jika 401, kemungkinan token expired
                        if (response.code() == 401) {
                            Log.e(TAG, "🔐 Unauthorized - Token may be expired or invalid")
                        }

                        _isPremium.value = false
                        _premiumStatusResponse.value = null
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during premium check", e)
                withContext(Dispatchers.Main) {
                    _isPremium.value = false
                    _premiumStatusResponse.value = null
                }
            }
        }
    }

    /**
     * Aktivasi premium dengan purchase token
     *
     * ✅ FIXED: Tidak perlu manual token handling
     * Token akan otomatis ditambahkan oleh AuthInterceptor
     */
    suspend fun activatePremiumWithPurchase(purchaseToken: String): Boolean {
        Log.d(TAG, "=== ACTIVATE PREMIUM WITH PURCHASE ===")
        Log.d(TAG, "Purchase Token: $purchaseToken")

        return withContext(Dispatchers.IO) {
            try {
                // Validasi user login
                if (auth.currentUser == null) {
                    Log.e(TAG, "❌ User not logged in (activatePremium)")
                    return@withContext false
                }

                Log.d(TAG, "📡 Calling API activatePremium...")

                val request = PremiumActivationRequest(
                    purchase_token = purchaseToken,
                    duration_days = 30
                )

                // ✅ PERBAIKAN: Tidak perlu kirim token manual
                // AuthInterceptor akan otomatis menambahkan Bearer token
                val response = apiService.activatePremium(request = request)

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "✅ Premium activated successfully!")
                    Log.d(TAG, "Is Premium: ${result?.isPremium}")
                    Log.d(TAG, "Expiry Date: ${result?.expiryDate}")

                    withContext(Dispatchers.Main) {
                        _isPremium.value = result?.isPremium ?: true
                        _premiumStatusResponse.value = result
                    }

                    // Refresh status untuk memastikan data terbaru
                    checkPremiumStatus()

                    return@withContext true
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "❌ Activation error: ${response.code()} - $errorBody")

                    if (response.code() == 401) {
                        Log.e(TAG, "🔐 Unauthorized - Token may be expired or invalid")
                    }

                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during premium activation", e)
                return@withContext false
            }
        }
    }

    /**
     * Check apakah user sudah login
     */
    fun isUserLoggedIn(): Boolean {
        return _isLoggedIn.value
    }

    /**
     * Get current user ID
     */
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Force refresh premium status
     * Berguna untuk dipanggil setelah user login atau setelah purchase
     */
    fun refreshPremiumStatus() {
        Log.d(TAG, "🔄 Force refresh premium status")
        checkPremiumStatus()
    }
}