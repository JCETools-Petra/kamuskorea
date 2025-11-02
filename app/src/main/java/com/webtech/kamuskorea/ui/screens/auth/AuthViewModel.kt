package com.webtech.kamuskorea.ui.screens.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
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
import java.io.IOException
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
        // ID Klien Web Anda
        const val WEB_CLIENT_ID = "214644364883-f0oh0k0lnd3buj07se4rlpmqd2s1lo33.apps.googleusercontent.com"
    }

    /**
     * Menangani login email & password.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                authResult.user?.let {
                    // Sinkronkan data pengguna ke MySQL saat login
                    syncUserToMySQL(it, "password", forceRefresh = false)
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in failed", e)
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Login failed. Please check your email and password."
                    is IOException -> "Login failed. Please check your internet connection."
                    else -> "Login failed. Please try again."
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    /**
     * Memvalidasi input dan memulai proses pendaftaran.
     */
    fun signUp(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Name, email, and password cannot be empty.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match.")
            return
        }
        _authState.value = AuthState.Loading
        createUser(name, email, password)
    }

    /**
     * Membuat pengguna di Firebase Auth, lalu menyinkronkan ke Firestore & MySQL.
     */
    private fun createUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                // Update nama tampilan di Firebase
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user?.updateProfile(profileUpdates)?.await()

                if (user != null) {
                    // Sinkronkan ke MySQL (dengan paksa refresh token)
                    syncUserToMySQL(user, "password", name, forceRefresh = true)

                    // Sinkronkan ke Firestore
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
                val errorMessage = when (e) {
                    is FirebaseAuthUserCollisionException -> "Registration failed. This email is already in use."
                    is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
                    is IOException -> "Registration failed. Please check your internet connection."
                    else -> "Registration Failed. Please try again."
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    /**
     * Membuat Intent untuk Google Sign-In.
     */
    fun getGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    /**
     * Menangani login Google, lalu menyinkronkan ke Firestore & MySQL.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                    val isNewUser = authResult.additionalUserInfo?.isNewUser == true
                    // Sinkronkan ke MySQL (paksa refresh token jika pengguna baru)
                    syncUserToMySQL(user, "google", forceRefresh = isNewUser)

                    // Sinkronkan ke Firestore HANYA jika pengguna baru
                    if (isNewUser) {
                        val userMap = hashMapOf(
                            "uid" to user.uid,
                            "name" to user.displayName,
                            "email" to user.email,
                            "profilePictureUrl" to user.photoUrl?.toString(),
                            "auth_type" to "google",
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
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In Failed. Please try again.")
            }
        }
    }

    /**
     * Fungsi helper untuk sinkronisasi pengguna ke backend MySQL Anda.
     */
    private suspend fun syncUserToMySQL(
        user: FirebaseUser,
        authType: String,
        nameOverride: String? = null,
        forceRefresh: Boolean = false // Wajib 'true' untuk pengguna baru
    ) {
        try {
            // 1. Dapatkan Token (Paksa refresh jika true)
            val idToken = user.getIdToken(forceRefresh).await().token
            if (idToken == null) {
                Log.e("AuthViewModel", "❌ Failed to get ID Token for sync")
                return
            }

            val bearerToken = "Bearer $idToken"

            // 2. Siapkan Request Body
            val syncRequest = UserSyncRequest(
                email = user.email,
                name = nameOverride ?: user.displayName, // Gunakan nama dari register, atau dari profil Google
                photoUrl = user.photoUrl?.toString(),
                auth_type = authType
            )

            // 3. Panggil API dengan Token
            val syncResponse = apiService.syncUser(bearerToken, syncRequest)

            if (syncResponse.isSuccessful) {
                Log.d("AuthViewModel", "✅ User synced to MySQL")
            } else {
                Log.e("AuthViewModel", "⚠️ MySQL sync failed: ${syncResponse.code()} - ${syncResponse.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            // Ini adalah error "credential malformed" yang Anda lihat sebelumnya.
            // Dengan forceRefresh=true, ini seharusnya tidak terjadi lagi.
            Log.e("AuthViewModel", "⚠️ Error syncing to MySQL", e)
        }
    }


    // ========================================
    // PASSWORD RESET
    // ========================================

    /**
     * Meminta link reset password (dikirim oleh Firebase via api.php).
     */
    fun requestForgotPassword(email: String) {
        if (email.isBlank()) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Email cannot be empty.")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Invalid email format.")
            return
        }

        viewModelScope.launch {
            try {
                _forgotPasswordState.value = ForgotPasswordState.Loading
                Log.d("AuthViewModel", "📧 Requesting password reset for: $email")

                val request = ForgotPasswordRequest(email = email)
                // Memanggil api.php, yang kemudian memanggil Firebase Admin SDK
                val response = apiService.requestPasswordReset(request)

                Log.d("AuthViewModel", "📨 Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.success == true) {
                        Log.d("AuthViewModel", "✅ Password reset email sent")
                        _forgotPasswordState.value = ForgotPasswordState.Success(
                            result.message ?: "Password reset link has been sent. Please check your inbox or spam folder."
                        )
                    } else {
                        Log.e("AuthViewModel", "❌ Failed: ${result?.message}")
                        _forgotPasswordState.value = ForgotPasswordState.Error(
                            result?.message ?: "Failed to send reset email."
                        )
                    }
                } else {
                    // Menangani error dari server (misalnya 500, 404)
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthViewModel", "❌ API Error: ${response.code()} - $errorBody")

                    var userMessage = "Failed to send reset email. Please try again."

                    try {
                        if (!errorBody.isNullOrBlank()) {
                            val errorJson = org.json.JSONObject(errorBody)
                            val serverMessage = errorJson.optString("message", "")
                            val authType = errorJson.optString("auth_type", "")

                            userMessage = if (serverMessage.isNotBlank()) {
                                serverMessage // Gunakan pesan error yang sudah ramah dari server
                            } else {
                                "An unknown error occurred. Please try again."
                            }

                            if (authType == "google") {
                                _forgotPasswordState.value = ForgotPasswordState.ErrorGoogleAccount(userMessage)
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "❌ Failed to parse error body", e)
                    }
                    _forgotPasswordState.value = ForgotPasswordState.Error(userMessage)
                }
            } catch (e: Exception) {
                // Menangani error level koneksi (tidak ada internet, timeout)
                Log.e("AuthViewModel", "❌ Exception in forgot password", e)
                val errorMessage = when (e) {
                    is IOException -> "Could not connect to server. Please check your internet connection."
                    else -> "An unexpected error occurred. Please try again."
                }
                _forgotPasswordState.value = ForgotPasswordState.Error(errorMessage)
            }
        }
    }

    /**
     * ✅ FUNGSI DIPERBAIKI
     * Menyelesaikan reset password menggunakan Firebase SDK (bukan API kustom).
     */
    fun resetPassword(token: String, newPassword: String, confirmPassword: String) {
        if (token.isBlank()) {
            _resetPasswordState.value = ResetPasswordState.Error("Invalid or expired link.")
            return
        }
        if (newPassword.isBlank()) {
            _resetPasswordState.value = ResetPasswordState.Error("Password cannot be empty.")
            return
        }
        if (newPassword.length < 6) {
            _resetPasswordState.value = ResetPasswordState.Error("Password must be at least 6 characters.")
            return
        }
        if (newPassword != confirmPassword) {
            _resetPasswordState.value = ResetPasswordState.Error("Passwords do not match.")
            return
        }

        viewModelScope.launch {
            try {
                _resetPasswordState.value = ResetPasswordState.Loading
                Log.d("AuthViewModel", "🔐 Confirming password reset with Firebase...")

                // ✅ PERBAIKAN: Gunakan Firebase Auth SDK untuk mengonfirmasi reset
                auth.confirmPasswordReset(token, newPassword).await()

                Log.d("AuthViewModel", "✅ Password reset successful")
                _resetPasswordState.value = ResetPasswordState.Success(
                    "Password reset successful! You can now log in with your new password."
                )

            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Exception in reset password", e)
                // Terjemahkan error Firebase menjadi pesan ramah pengguna
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "The reset link is invalid or has expired. Please request a new one."
                    is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
                    else -> "An error occurred. Please try again."
                }
                _resetPasswordState.value = ResetPasswordState.Error(errorMessage)
            }
        }
    }

    // Fungsi Reset State
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

// State classes (Tetap sama)
sealed class ForgotPasswordState {
    object Initial : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
    data class ErrorGoogleAccount(val message: String) : ForgotPasswordState()
}

sealed class ResetPasswordState {
    object Initial : ResetPasswordState()
    object Loading : ResetPasswordState()
    data class Success(val message: String) : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}