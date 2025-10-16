package com.webtech.kamuskorea.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    init {
        // Mendengarkan perubahan status premium secara real-time
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("UserRepository", "Gagal mendengarkan snapshot", error)
                        _isPremium.value = false
                        return@addSnapshotListener
                    }
                    val isPremiumStatus = snapshot?.getBoolean("isPremium") ?: false
                    _isPremium.value = isPremiumStatus
                    Log.d("UserRepository", "Status premium user terdeteksi: $isPremiumStatus")
                }
        }
    }

    // Fungsi ini sekarang mengambil UID dari auth instance secara otomatis
    suspend fun updatePremiumStatus(isPremium: Boolean) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("UserRepository", "Gagal update, user tidak login.")
            return
        }

        try {
            firestore.collection("users").document(userId)
                .set(mapOf("isPremium" to isPremium), SetOptions.merge())
                .await()
            Log.d("UserRepository", "Status premium untuk user $userId diperbarui menjadi $isPremium")
        } catch (e: Exception) {
            Log.e("UserRepository", "Gagal memperbarui status premium", e)
        }
    }

    suspend fun verifyPurchase(purchaseToken: String, productId: String) {
        val data = hashMapOf(
            "purchaseToken" to purchaseToken,
            "productId" to productId
        )
        try {
            functions.getHttpsCallable("verifyPurchase").call(data).await()
            Log.d("UserRepository", "Verifikasi pembelian berhasil dikirim ke Cloud Function.")
        } catch (e: Exception) {
            Log.e("UserRepository", "Gagal memanggil Cloud Function verifyPurchase", e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getUserId(): String? {
        return auth.currentUser?.uid
    }
}