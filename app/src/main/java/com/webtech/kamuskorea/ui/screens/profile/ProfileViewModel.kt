// File: app/src/main/java/com/webtech/kamuskorea/ui/screens/profile/ProfileViewModel.kt

package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.billing.BillingClientWrapper
import com.webtech.kamuskorea.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val billingClient: BillingClientWrapper,
    userRepository: UserRepository,
    application: Application
) : ViewModel() {

    val productDetails = billingClient.productDetails.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    val hasActiveSubscription = userRepository.isPremium.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    fun purchase(activity: Activity) {
        val currentProductDetails = productDetails.value
        if (currentProductDetails != null) {
            billingClient.launchPurchaseFlow(currentProductDetails, activity)
        }
    }

    // CATATAN: Factory tidak lagi diperlukan saat menggunakan Hilt.
    // Kode lama yang menyebabkan error sudah dihapus.
}