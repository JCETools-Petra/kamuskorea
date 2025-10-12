package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.billing.BillingClientWrapper
import com.android.billingclient.api.ProductDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription = _hasActiveSubscription.asStateFlow()

    private val billingClientWrapper = BillingClientWrapper(
        context = application,
        productId = "langganan_pro_bulanan"
    )

    init {
        // Panggil fungsi untuk mengecek status langganan saat ViewModel dibuat
        billingClientWrapper.initialize()

        // Ambil detail produk dari Google Play
        billingClientWrapper.productDetails
            .onEach { productDetails ->
                _productDetails.value = productDetails
            }
            .launchIn(viewModelScope)

        // Ambil status langganan
        billingClientWrapper.hasActiveSubscription
            .onEach { hasActiveSubscription ->
                _hasActiveSubscription.value = hasActiveSubscription
            }
            .launchIn(viewModelScope)
    }

    fun launchPurchaseFlow(activity: Activity) {
        // Implementasi untuk memulai alur pembelian Google Play Billing
        billingClientWrapper.launchPurchaseFlow(activity)
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