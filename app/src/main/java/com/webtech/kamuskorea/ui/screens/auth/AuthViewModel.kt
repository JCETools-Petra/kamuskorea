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
        const val TAG = "AuthViewModel"
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
                    syncUserToMySQL(it, "password")
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed", e)
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
                    // Sinkronkan ke MySQL
                    syncUserToMySQL(user, "password", name)

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
                Log.e(TAG, "Sign up failed", e)
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

                    // Sinkronkan ke MySQL
                    syncUserToMySQL(user, "google")

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
                Log.e(TAG, "Google sign in failed", e)
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In Failed. Please try again.")
            }
        }
    }

    /**
     * Fungsi helper untuk sinkronisasi pengguna ke backend MySQL.
     *
     * ✅ FIXED: Tidak perlu manual token handling
     * Token akan otomatis ditambahkan oleh AuthInterceptor
     */
    private suspend fun syncUserToMySQL(
        user: FirebaseUser,
        authType: String,
        nameOverride: String? = null
    ) {
        try {
            Log.d(TAG, "🔄 Syncing user to MySQL...")
            Log.d(TAG, "User UID: ${user.uid}")
            Log.d(TAG, "Auth Type: $authType")

            // Siapkan Request Body
            val syncRequest = UserSyncRequest(
                email = user.email,
                name = nameOverride ?: user.displayName,
                photoUrl = user.photoUrl?.toString(),
                auth_type = authType
            )

            // ✅ PERBAIKAN: Tidak perlu kirim token manual
            // AuthInterceptor akan otomatis menambahkan Bearer token
            val syncResponse = apiService.syncUser(request = syncRequest)

            if (syncResponse.isSuccessful) {
                Log.d(TAG, "✅ User synced to MySQL successfully")
                val responseBody = syncResponse.body()
                Log.d(TAG, "Response: ${responseBody?.message}")
            } else {
                val errorBody = syncResponse.errorBody()?.string()
                Log.e(TAG, "⚠️ MySQL sync failed: ${syncResponse.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error syncing to MySQL", e)
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
                Log.d(TAG, "📧 Requesting password reset for: $email")

                val request = ForgotPasswordRequest(email = email)
                val response = apiService.requestPasswordReset(request)

                Log.d(TAG, "📨 Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.success == true) {
                        Log.d(TAG, "✅ Password reset email sent")
                        _forgotPasswordState.value = ForgotPasswordState.Success(
                            result.message ?: "Password reset link has been sent. Please check your inbox or spam folder."
                        )
                    } else {
                        Log.e(TAG, "❌ Failed: ${result?.message}")
                        _forgotPasswordState.value = ForgotPasswordState.Error(
                            result?.message ?: "Failed to send reset email."
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ API Error: ${response.code()} - $errorBody")

                    var userMessage = "Failed to send reset email. Please try again."

                    try {
                        if (!errorBody.isNullOrBlank()) {
                            val errorJson = org.json.JSONObject(errorBody)
                            val serverMessage = errorJson.optString("message", "")
                            val authType = errorJson.optString("auth_type", "")

                            userMessage = if (serverMessage.isNotBlank()) {
                                serverMessage
                            } else {
                                "An unknown error occurred. Please try again."
                            }

                            if (authType == "google") {
                                _forgotPasswordState.value = ForgotPasswordState.ErrorGoogleAccount(userMessage)
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to parse error body", e)
                    }
                    _forgotPasswordState.value = ForgotPasswordState.Error(userMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in forgot password", e)
                val errorMessage = when (e) {
                    is IOException -> "Could not connect to server. Please check your internet connection."
                    else -> "An unexpected error occurred. Please try again."
                }
                _forgotPasswordState.value = ForgotPasswordState.Error(errorMessage)
            }
        }
    }

    /**
     * Menyelesaikan reset password menggunakan Firebase SDK.
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
                Log.d(TAG, "🔐 Confirming password reset with Firebase...")

                // Gunakan Firebase Auth SDK untuk mengonfirmasi reset
                auth.confirmPasswordReset(token, newPassword).await()

                Log.d(TAG, "✅ Password reset successful")
                _resetPasswordState.value = ResetPasswordState.Success(
                    "Password reset successful! You can now log in with your new password."
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in reset password", e)
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