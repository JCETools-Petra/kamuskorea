package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.webtech.kamuskorea.billing.BillingClientWrapper
import com.webtech.kamuskorea.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val billingClient: BillingClientWrapper,
    userRepository: UserRepository,
    application: Application
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

    fun updateProfile(displayName: String, birthDate: String) {
        viewModelScope.launch {
            val user = auth.currentUser
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            user?.updateProfile(profileUpdates)?.await()
            // Simpan tanggal lahir ke Firestore atau Realtime Database jika perlu
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null && uri != null) {
                val storageRef = storage.reference.child("profile_pictures/${user.uid}")
                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await()
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(downloadUrl)
                    .build()
                user.updateProfile(profileUpdates).await()
            }
        }
    }
}