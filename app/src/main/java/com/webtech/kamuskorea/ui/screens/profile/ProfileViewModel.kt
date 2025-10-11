package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.billing.BillingClientWrapper
import com.webtech.kamuskorea.data.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository()

    // Sekarang, status premium diambil dari UserRepository (Firestore)
    val hasActiveSubscription = userRepository.isPremium

    // BillingClientWrapper masih digunakan untuk PROSES PEMBELIAN
    private val billingClientWrapper = BillingClientWrapper(
        context = application,
        onPurchaseVerified = { purchaseToken, productId ->
            // Saat pembelian berhasil, panggil UserRepository untuk verifikasi server
            viewModelScope.launch {
                userRepository.verifyPurchase(purchaseToken, productId)
            }
        }
    )

    val productDetails = billingClientWrapper.productDetails

    init {
        billingClientWrapper.initialize()
    }

    fun launchPurchaseFlow(activity: Activity) {
        billingClientWrapper.launchPurchaseFlow(activity)
    }

    // Factory untuk membuat ViewModel ini
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                        return ProfileViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}