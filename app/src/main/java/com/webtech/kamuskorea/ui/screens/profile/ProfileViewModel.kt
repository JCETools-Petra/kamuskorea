package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.billing.BillingClientWrapper
import com.webtech.kamuskorea.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val billingClient: BillingClientWrapper
) : AndroidViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription: StateFlow<Boolean> = _hasActiveSubscription.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            billingClient.productDetails.collectLatest { details ->
                _productDetails.value = details
                if (_hasActiveSubscription.value != null) {
                    _isLoading.value = false
                }
            }
        }
        viewModelScope.launch {
            billingClient.hasActiveSubscription.collectLatest { hasSub ->
                _hasActiveSubscription.value = hasSub
                updatePremiumStatusInFirestore(hasSub)
                if (_productDetails.value != null) {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun updatePremiumStatusInFirestore(isPremium: Boolean) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                userRepository.updatePremiumStatus(uid, isPremium)
            }
        }
    }

    fun purchaseSubscription(activity: Activity) {
        val product = _productDetails.value
        if (product != null) {
            billingClient.launchPurchaseFlow(product, activity)
        } else {
            _error.value = "Detail produk tidak tersedia untuk dibeli."
            Log.e("ProfileViewModel", "Gagal memulai pembelian, productDetails null.")
        }
    }

    companion object {
        // Factory untuk membuat ViewModel
        fun provideFactory(
            application: Application,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                    val userRepository = UserRepository()
                    // PERBAIKI BAGIAN INI: Gunakan CoroutineScope yang valid
                    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                    val billingClient = BillingClientWrapper(application, appScope)
                    return ProfileViewModel(application, userRepository, billingClient) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}