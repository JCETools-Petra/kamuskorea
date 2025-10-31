package com.webtech.kamuskorea.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.webtech.kamuskorea.billing.BillingClientWrapper
import com.webtech.kamuskorea.data.UserRepository
import com.webtech.kamuskorea.data.network.ApiService
import com.webtech.kamuskorea.data.network.UserProfileUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val billingClient: BillingClientWrapper,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context // DIPERBAIKI: Gunakan Context, bukan Application
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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

    // State untuk UI
    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus = _updateStatus.asStateFlow()

    private val _profilePictureUrl = MutableStateFlow<String?>(null)
    val profilePictureUrl = _profilePictureUrl.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName = _displayName.asStateFlow()

    private val _dateOfBirth = MutableStateFlow("") // Format "YYYY-MM-DD"
    val dateOfBirth = _dateOfBirth.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(true)
    val isLoadingProfile = _isLoadingProfile.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        _isLoadingProfile.value = true
        viewModelScope.launch {
            var token = getFirebaseIdToken(false)
            if (token == null && auth.currentUser != null) {
                Log.w("ProfileViewModel", "Mencoba force refresh token untuk fetch profil...")
                token = getFirebaseIdToken(true)
            }

            if (token == null) {
                Log.w("ProfileViewModel", "Gagal fetch profil: User tidak login atau token tidak valid.")
                _displayName.value = auth.currentUser?.displayName ?: ""
                _profilePictureUrl.value = auth.currentUser?.photoUrl?.toString()
                _dateOfBirth.value = ""
                _isLoadingProfile.value = false
                return@launch
            }

            try {
                Log.d("ProfileViewModel", "Memanggil API getUserProfile...")
                val response = apiService.getUserProfile("Bearer $token")
                if (response.isSuccessful) {
                    val profileData = response.body()
                    profileData?.let {
                        _displayName.value = it.name ?: auth.currentUser?.displayName ?: ""
                        _dateOfBirth.value = it.dob ?: ""
                        _profilePictureUrl.value = it.profilePictureUrl
                        Log.d("ProfileViewModel", "Profil diambil: Nama=${it.name}, DOB=${it.dob}, Foto=${it.profilePictureUrl}")
                    } ?: run {
                        _displayName.value = auth.currentUser?.displayName ?: ""
                        _profilePictureUrl.value = auth.currentUser?.photoUrl?.toString()
                        _dateOfBirth.value = ""
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("ProfileViewModel", "Gagal fetch profil API: ${response.code()} - $errorBody")
                    _displayName.value = auth.currentUser?.displayName ?: ""
                    _profilePictureUrl.value = auth.currentUser?.photoUrl?.toString()
                    _dateOfBirth.value = ""
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception saat fetch profil", e)
                _displayName.value = auth.currentUser?.displayName ?: ""
                _profilePictureUrl.value = auth.currentUser?.photoUrl?.toString()
                _dateOfBirth.value = ""
            } finally {
                _isLoadingProfile.value = false
            }
        }
    }

    fun onDisplayNameChange(name: String) {
        _displayName.value = name
    }

    fun onDateOfBirthChange(dob: String) {
        _dateOfBirth.value = dob
    }

    private suspend fun getFirebaseIdToken(forceRefresh: Boolean = true): String? {
        return try {
            auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Gagal mendapatkan Firebase ID Token", e)
            null
        }
    }

    fun updateProfileDetails() {
        viewModelScope.launch {
            val token = getFirebaseIdToken(true)
            if (token == null) {
                _updateStatus.value = "Gagal: Sesi tidak valid. Silakan login ulang."
                return@launch
            }

            val nameToSend = _displayName.value
            val dobToSend = _dateOfBirth.value

            if (nameToSend.isBlank() || dobToSend.isBlank()) {
                _updateStatus.value = "Gagal: Nama dan Tanggal Lahir tidak boleh kosong."
                return@launch
            }
            if (!dobToSend.matches("""^\d{4}-\d{2}-\d{2}$""".toRegex())) {
                _updateStatus.value = "Gagal: Format Tanggal Lahir harus YYYY-MM-DD."
                return@launch
            }

            try {
                _updateStatus.value = "Memperbarui profil..."
                val requestBody = UserProfileUpdateRequest(name = nameToSend, dob = dobToSend)
                val response = apiService.updateProfileDetails("Bearer $token", requestBody)

                if (response.isSuccessful) {
                    _updateStatus.value = "Profil berhasil diperbarui."

                    // Update display name di Firebase Auth
                    val profileUpdates = userProfileChangeRequest {
                        displayName = nameToSend
                    }
                    try {
                        auth.currentUser?.updateProfile(profileUpdates)?.await()
                    } catch (e: Exception) {
                        Log.w("ProfileViewModel", "Gagal update display name di Firebase Auth", e)
                    }

                    fetchUserProfile()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error tidak diketahui"
                    _updateStatus.value = "Gagal memperbarui profil: ${response.code()} - $errorBody"
                    Log.e("ProfileViewModel", "Gagal update profil API: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error update profile details", e)
                _updateStatus.value = "Gagal: Terjadi kesalahan. ${e.message}"
            }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            val token = getFirebaseIdToken(true)
            if (token == null) {
                _updateStatus.value = "Gagal: Sesi tidak valid. Silakan login ulang."
                return@launch
            }

            try {
                _updateStatus.value = "Mengunggah foto..."
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val fileBytes = inputStream?.readBytes()
                inputStream?.close()

                if (fileBytes == null) {
                    _updateStatus.value = "Gagal membaca file gambar."
                    return@launch
                }

                val requestFile = fileBytes.toRequestBody(
                    context.contentResolver.getType(uri)?.toMediaTypeOrNull()
                )
                val body = MultipartBody.Part.createFormData("image", "profile_picture.jpg", requestFile)

                val response = apiService.updateProfilePicture("Bearer $token", body)

                if (response.isSuccessful && response.body()?.success == true) {
                    val newPhotoUrl = response.body()?.profilePictureUrl
                    if (newPhotoUrl != null) {
                        // Update Firebase Auth photoUri
                        val profileUpdates = userProfileChangeRequest {
                            photoUri = Uri.parse(newPhotoUrl)
                        }
                        try {
                            auth.currentUser?.updateProfile(profileUpdates)?.await()
                        } catch (e: Exception) {
                            Log.w("ProfileViewModel", "Gagal update photoUri di Firebase Auth", e)
                        }

                        _profilePictureUrl.value = newPhotoUrl
                        _updateStatus.value = "Foto profil berhasil diperbarui."
                        fetchUserProfile()
                    } else {
                        _updateStatus.value = "Gagal: URL gambar tidak diterima dari server."
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error tidak diketahui"
                    _updateStatus.value = "Gagal mengunggah foto: ${response.code()} - $errorBody"
                    Log.e("ProfileViewModel", "Gagal upload foto API: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error uploading profile picture", e)
                _updateStatus.value = "Gagal: Terjadi kesalahan. ${e.message}"
            }
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = null
    }

    fun launchBilling(activity: android.app.Activity) {
        val currentProductDetails = productDetails.value
        if (currentProductDetails != null) {
            billingClient.launchPurchaseFlow(currentProductDetails, activity)
        } else {
            Log.e("ProfileViewModel", "Gagal memulai billing: ProductDetails is null.")
            _updateStatus.value = "Gagal: Detail produk langganan tidak ditemukan."
        }
    }
}