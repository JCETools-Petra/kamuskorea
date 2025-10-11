package com.webtech.kamuskorea.data

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
        // Dengarkan perubahan pada dokumen pengguna
        auth.currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _isPremium.value = false
                        return@addSnapshotListener
                    }
                    // Update status premium berdasarkan data di Firestore
                    _isPremium.value = snapshot?.getBoolean("isPremium") ?: false
                }
        }
    }

    // Fungsi untuk memanggil Cloud Function
    suspend fun verifyPurchase(purchaseToken: String, productId: String) {
        val data = hashMapOf(
            "purchaseToken" to purchaseToken,
            "productId" to productId
        )
        // Memanggil Cloud Function bernama 'verifyPurchase'
        functions.getHttpsCallable("verifyPurchase").call(data).await()
    }
}