package com.webtech.kamuskorea.ui.screens.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.webtech.kamuskorea.data.network.ApiService
import com.webtech.kamuskorea.data.network.ForgotPasswordRequest
import com.webtech.kamuskorea.data.network.ResetPasswordRequest
import com.webtech.kamuskorea.data.network.UserSyncRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val apiService: ApiService
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Initial)
    val forgotPasswordState = _forgotPasswordState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Initial)
    val resetPasswordState = _resetPasswordState.asStateFlow()

    private companion object {
        const val WEB_CLIENT_ID = "214644364883-f0oh0k0lnd3buj07se4rlpmqd2s1lo33.apps.googleusercontent.com"
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in failed", e)
                _authState.value = AuthState.Error(e.message ?: "Login Gagal")
            }
        }
    }

    fun signUp(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Nama, email, dan password tidak boleh kosong.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password minimal harus 6 karakter.")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Password dan konfirmasi password tidak cocok.")
            return
        }
        _authState.value = AuthState.Loading
        createUser(name, email, password)
    }

    private fun createUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user?.updateProfile(profileUpdates)?.await()

                if (user != null) {
                    // ✅ TAMBAHAN BARU: Sync ke MySQL database
                    try {
                        val syncRequest = UserSyncRequest(
                            email = user.email,
                            name = name,
                            photoUrl = null,
                            auth_type = "password" // ✅ TAMBAHAN
                        )
                        val syncResponse = apiService.syncUser(syncRequest)

                        if (syncResponse.isSuccessful) {
                            Log.d("AuthViewModel", "✅ User synced to MySQL: ${syncResponse.body()?.message}")
                        } else {
                            Log.e("AuthViewModel", "⚠️ MySQL sync failed: ${syncResponse.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "⚠️ Error syncing to MySQL", e)
                        // Don't fail registration if sync fails
                    }
                    // ✅ AKHIR TAMBAHAN

                    // Firestore sync (optional, keep for compatibility)
                    val userMap = hashMapOf(
                        "uid" to user.uid,
                        "name" to name,
                        "email" to email,
                        "auth_type" to "password",
                        "isPremium" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users")
                        .document(user.uid)
                        .set(userMap)
                        .await()
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign up failed", e)
                _authState.value = AuthState.Error(e.message ?: "Pendaftaran Gagal")
            }
        }
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        Log.d("AuthViewModel", "=== GOOGLE SIGN IN CONFIG ===")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                    try {
                        val syncRequest = UserSyncRequest(
                            email = user.email,
                            name = user.displayName,
                            photoUrl = user.photoUrl?.toString(),
                            auth_type = "google" // ✅ TAMBAHAN
                        )
                        apiService.syncUser(syncRequest)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error syncing user", e)
                    }

                    // ✅ Set auth_type = "google" for Google users
                    if (authResult.additionalUserInfo?.isNewUser == true) {
                        val userMap = hashMapOf(
                            "uid" to user.uid,
                            "name" to user.displayName,
                            "email" to user.email,
                            "profilePictureUrl" to user.photoUrl?.toString(),
                            "auth_type" to "google", // ← NEW!
                            "isPremium" to false,
                            "createdAt" to System.currentTimeMillis()
                        )
                        firestore.collection("users")
                            .document(user.uid)
                            .set(userMap)
                            .await()
                    }
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign in failed", e)
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In Gagal")
            }
        }
    }

    // ✅ UPDATED: Request forgot password dengan handling Google users
    fun requestForgotPassword(email: String) {
        if (email.isBlank()) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Email tidak boleh kosong")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Format email tidak valid")
            return
        }

        viewModelScope.launch {
            try {
                _forgotPasswordState.value = ForgotPasswordState.Loading
                Log.d("AuthViewModel", "📧 Sending Firebase password reset email to: $email")

                // ✅ GUNAKAN FIREBASE AUTH LANGSUNG
                auth.sendPasswordResetEmail(email).await()

                Log.d("AuthViewModel", "✅ Password reset email sent via Firebase")
                _forgotPasswordState.value = ForgotPasswordState.Success(
                    "Email reset password telah dikirim. Periksa inbox Anda."
                )

            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Firebase password reset failed", e)

                val errorMessage = when {
                    e.message?.contains("no user record", ignoreCase = true) == true ->
                        "Email tidak terdaftar"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Tidak ada koneksi internet"
                    else ->
                        "Gagal mengirim email: ${e.message}"
                }

                _forgotPasswordState.value = ForgotPasswordState.Error(errorMessage)
            }
        }
    }

    fun resetPassword(token: String, newPassword: String, confirmPassword: String) {
        if (token.isBlank()) {
            _resetPasswordState.value = ResetPasswordState.Error("Token tidak valid")
            return
        }

        if (newPassword.isBlank()) {
            _resetPasswordState.value = ResetPasswordState.Error("Password tidak boleh kosong")
            return
        }

        if (newPassword.length < 6) {
            _resetPasswordState.value = ResetPasswordState.Error("Password minimal 6 karakter")
            return
        }

        if (newPassword != confirmPassword) {
            _resetPasswordState.value = ResetPasswordState.Error("Password tidak cocok")
            return
        }

        viewModelScope.launch {
            try {
                _resetPasswordState.value = ResetPasswordState.Loading
                Log.d("AuthViewModel", "🔐 Resetting password with token")

                val request = ResetPasswordRequest(
                    token = token,
                    newPassword = newPassword
                )
                val response = apiService.resetPassword(request)

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.success == true) {
                        Log.d("AuthViewModel", "✅ Password reset successful")
                        _resetPasswordState.value = ResetPasswordState.Success(
                            result.message ?: "Password berhasil direset"
                        )
                    } else {
                        Log.e("AuthViewModel", "❌ Failed: ${result?.message}")
                        _resetPasswordState.value = ResetPasswordState.Error(
                            result?.message ?: "Gagal reset password"
                        )
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("AuthViewModel", "❌ API Error: ${response.code()} - $errorMsg")
                    _resetPasswordState.value = ResetPasswordState.Error(
                        "Gagal reset password. Token mungkin sudah kadaluarsa."
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Exception in reset password", e)
                _resetPasswordState.value = ResetPasswordState.Error(
                    "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Initial
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Initial
    }

    fun resetResetPasswordState() {
        _resetPasswordState.value = ResetPasswordState.Initial
    }
}

// State classes
sealed class ForgotPasswordState {
    object Initial : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
    data class ErrorGoogleAccount(val message: String) : ForgotPasswordState() // ✅ NEW!
}

sealed class ResetPasswordState {
    object Initial : ResetPasswordState()
    object Loading : ResetPasswordState()
    data class Success(val message: String) : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}