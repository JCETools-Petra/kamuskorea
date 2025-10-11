package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.ProductDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription = _hasActiveSubscription.asStateFlow()

    init {
        // TODO: Panggil fungsi untuk mengecek status langganan saat ViewModel dibuat
        // TODO: Panggil fungsi untuk mengambil detail produk dari Google Play
    }

    fun launchPurchaseFlow(activity: Activity) {
        // TODO: Implementasi untuk memulai alur pembelian Google Play Billing
    }

    companion object {
        fun provideFactory(app: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                        return ProfileViewModel(app) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}