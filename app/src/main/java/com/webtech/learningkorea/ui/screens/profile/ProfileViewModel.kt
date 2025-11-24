package com.webtech.learningkorea.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.webtech.learningkorea.billing.BillingClientWrapper
import com.webtech.learningkorea.data.UserRepository
import com.webtech.learningkorea.data.network.ApiService
import com.webtech.learningkorea.data.network.UserProfileUpdateRequest
import com.webtech.learningkorea.gamification.GamificationRepository
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
    private val gamificationRepository: GamificationRepository,
    @ApplicationContext private val context: Context
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

    // --- PERBAIKAN 1: Buat AuthStateListener ---
    private val authStateListener: FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            // User login, SEKARANG baru aman memanggil API
            Log.d("ProfileViewModel", "AuthStateListener: User terdeteksi (${user.uid}). Memanggil fetchUserProfile.")
            fetchUserProfile()
        } else {
            // User logout
            Log.d("ProfileViewModel", "AuthStateListener: User logout.")
            _isLoadingProfile.value = false
            _displayName.value = ""
            _profilePictureUrl.value = null
            _dateOfBirth.value = ""
        }
    }

    init {
        // Ambil info dari Firebase Auth sebagai fallback cepat (mungkin null)
        _displayName.value = auth.currentUser?.displayName ?: ""
        _profilePictureUrl.value = auth.currentUser?.photoUrl?.toString()

        // --- PERBAIKAN 2: Daftarkan listener ---
        // Listener ini akan memanggil fetchUserProfile() saat auth siap
        auth.addAuthStateListener(authStateListener)

        // Pengecekan awal jika user SUDAH login (misal buka aplikasi)
        if (auth.currentUser == null) {
            _isLoadingProfile.value = false // User belum login, berhenti loading
        }
        // Jika user sudah login, listener di atas akan otomatis terpicu
    }

    // --- PERBAIKAN 3: Hapus listener saat ViewModel hancur ---
    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun fetchUserProfile() {
        // Cek lagi untuk keamanan ganda, walau seharusnya sudah dicek listener
        if (auth.currentUser == null) {
            Log.w("ProfileViewModel", "fetchUserProfile dipanggil tapi user null. Batal.")
            _isLoadingProfile.value = false
            return
        }

        _isLoadingProfile.value = true
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Memanggil API getUserProfile...")
                // Argumen token dihapus, Interceptor akan menanganinya
                val response = apiService.getUserProfile()

                if (response.isSuccessful) {
                    val profileData = response.body()
                    profileData?.let {
                        _displayName.value = it.name ?: auth.currentUser?.displayName ?: ""
                        _dateOfBirth.value = it.dob ?: ""
                        // FIXED: Use Firebase Auth photoUrl as fallback if backend doesn't return photo URL
                        _profilePictureUrl.value = it.profilePictureUrl
                            ?: auth.currentUser?.photoUrl?.toString()
                        Log.d("ProfileViewModel", "Profil diambil: Nama=${it.name}, DOB=${it.dob}, Foto=${it.profilePictureUrl ?: "fallback to Firebase: ${auth.currentUser?.photoUrl}"}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("ProfileViewModel", "Gagal fetch profil API: ${response.code()} - $errorBody")
                    // FIXED: On API error, keep Firebase Auth data as fallback
                    if (_profilePictureUrl.value == null) {
                        _profilePictureUrl.value = auth.currentUser?.photoUrl?.toString()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception saat fetch profil", e)
                // FIXED: On exception, keep Firebase Auth data as fallback
                if (_profilePictureUrl.value == null) {
                    _profilePictureUrl.value = auth.currentUser?.photoUrl?.toString()
                }
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

    fun updateProfileDetails() {
        viewModelScope.launch {
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

                // Argumen token dihapus
                val response = apiService.updateProfileDetails(requestBody)

                if (response.isSuccessful) {
                    _updateStatus.value = "Profil berhasil diperbarui."

                    // Update display name di Firebase Auth
                    val profileUpdates = userProfileChangeRequest {
                        displayName = nameToSend
                    }
                    try {
                        // Update Firebase Auth displayName
                        auth.currentUser?.updateProfile(profileUpdates)?.await()
                        Log.d("ProfileViewModel", "✅ Display name updated in Firebase Auth: $nameToSend")

                        // CRITICAL: Reload user to ensure Firebase has latest data
                        auth.currentUser?.reload()?.await()
                        Log.d("ProfileViewModel", "✅ Firebase user reloaded with latest profile")

                        // Immediately sync XP to server so leaderboard shows new name
                        try {
                            gamificationRepository.syncToServer()
                            Log.d("ProfileViewModel", "✅ XP synced to server with new username")
                        } catch (e: Exception) {
                            Log.w("ProfileViewModel", "⚠️ Failed to sync XP after name change", e)
                            // Don't fail the whole operation if sync fails
                        }
                    } catch (e: Exception) {
                        Log.w("ProfileViewModel", "Gagal update display name di Firebase Auth", e)
                    }
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

                // Argumen token dihapus
                val response = apiService.updateProfilePicture(body)

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