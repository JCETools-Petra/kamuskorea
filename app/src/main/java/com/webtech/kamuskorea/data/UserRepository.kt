package com.webtech.kamuskorea.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    init {
        auth.currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _isPremium.value = false
                        return@addSnapshotListener
                    }
                    _isPremium.value = snapshot?.getBoolean("isPremium") ?: false
                }
        }
    }

    // TAMBAHKAN FUNGSI INI
    suspend fun updatePremiumStatus(uid: String, isPremium: Boolean) {
        try {
            firestore.collection("users").document(uid)
                .set(mapOf("isPremium" to isPremium), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Log.d("UserRepository", "Status premium untuk user $uid diperbarui menjadi $isPremium")
        } catch (e: Exception) {
            Log.e("UserRepository", "Gagal memperbarui status premium", e)
        }
    }


    suspend fun verifyPurchase(purchaseToken: String, productId: String) {
        val data = hashMapOf(
            "purchaseToken" to purchaseToken,
            "productId" to productId
        )
        functions.getHttpsCallable("verifyPurchase").call(data).await()
    }
}