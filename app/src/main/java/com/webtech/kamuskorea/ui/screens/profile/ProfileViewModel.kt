package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.webtech.kamuskorea.billing.BillingClientWrapper

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    // Hapus UserRepository

    private val billingClientWrapper = BillingClientWrapper(application)

    // Kembalikan sumber data ke billingClientWrapper
    val hasActiveSubscription = billingClientWrapper.hasActiveSubscription
    val productDetails = billingClientWrapper.productDetails

    init {
        billingClientWrapper.initialize()
    }

    fun launchPurchaseFlow(activity: Activity) {
        billingClientWrapper.launchPurchaseFlow(activity)
    }

    // Factory untuk membuat ViewModel ini (tidak berubah)
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