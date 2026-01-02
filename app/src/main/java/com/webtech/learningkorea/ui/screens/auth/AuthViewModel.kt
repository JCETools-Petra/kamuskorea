package com.webtech.learningkorea.ui.screens.auth

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
import com.webtech.learningkorea.analytics.AnalyticsTracker
import com.webtech.learningkorea.data.network.ApiService
import com.webtech.learningkorea.data.network.ForgotPasswordRequest
import com.webtech.learningkorea.data.network.UserSyncRequest
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
    private val apiService: ApiService,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Initial)
    val forgotPasswordState = _forgotPasswordState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Initial)
    val resetPasswordState = _resetPasswordState.asStateFlow()

    private companion object {
        const val TAG = "AuthViewModel"
        // Web Client ID for project: Learning Korea (learning-korea)
        // From google-services.json → oauth_client → client_type: 3
        const val WEB_CLIENT_ID = "191033536798-1lq8npgrbgevqd0ep9rneaoud6blhtnt.apps.googleusercontent.com"
    }

    /**
     * Menangani login email & password.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                authResult.user?.let { user ->
                    // SECURITY: Check if email is verified for password auth
                    if (!user.isEmailVerified) {
                        Log.w(TAG, "⚠️ Email not verified for ${user.email}")
                        auth.signOut() // Sign out immediately
                        _authState.value = AuthState.Error(
                            "Email Anda belum diverifikasi. Silakan cek inbox email Anda dan klik link verifikasi sebelum login."
                        )
                        return@launch
                    }

                    // Track successful login
                    analyticsTracker.logLogin("email")
                    analyticsTracker.setUserId(user.uid)

                    // Sinkronkan data pengguna ke MySQL saat login
                    syncUserToMySQL(user, "password")
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed", e)
                val errorMessage = getAppCheckFriendlyErrorMessage(e)
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
                    // SECURITY: Send email verification to prevent bot registrations
                    try {
                        user.sendEmailVerification().await()
                        Log.d(TAG, "✅ Verification email sent to ${user.email}")
                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ Failed to send verification email", e)
                        // Don't fail registration if email sending fails
                    }

                    // Track successful sign up
                    analyticsTracker.logSignUp("email")
                    analyticsTracker.setUserId(user.uid)

                    // Sinkronkan ke MySQL (akan ditolak jika email belum diverifikasi)
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
                _authState.value = AuthState.SuccessNeedVerification
            } catch (e: Exception) {
                Log.e(TAG, "Sign up failed", e)

                // Prioritas: cek error spesifik Firebase Auth dulu, baru cek App Check
                val errorMessage = when (e) {
                    is FirebaseAuthUserCollisionException ->
                        "Registration failed. This email is already in use."
                    is FirebaseAuthWeakPasswordException ->
                        "Password is too weak. Please use at least 6 characters."
                    else -> getAppCheckFriendlyErrorMessage(e)
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    /**
     * Membuat Intent untuk Google Sign-In.
     */
    fun getGoogleSignInIntent(context: Context): Intent {
        Log.d(TAG, "=== GOOGLE SIGN IN CONFIGURATION ===")
        Log.d(TAG, "Web Client ID: $WEB_CLIENT_ID")
        Log.d(TAG, "Package Name: ${context.packageName}")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()

        val signInClient = GoogleSignIn.getClient(context, gso)
        Log.d(TAG, "Google Sign-In Client created successfully")

        return signInClient.signInIntent
    }

    /**
     * Menangani login Google, lalu menyinkronkan ke Firestore & MySQL.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== GOOGLE SIGN IN WITH TOKEN ===")
                Log.d(TAG, "ID Token length: ${idToken.length}")
                Log.d(TAG, "ID Token preview: ${idToken.take(50)}...")

                _authState.value = AuthState.Loading
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                Log.d(TAG, "Google credential created successfully")

                val authResult = auth.signInWithCredential(credential).await()
                Log.d(TAG, "Firebase authentication successful")

                val user = authResult.user

                if (user != null) {
                    val isNewUser = authResult.additionalUserInfo?.isNewUser == true

                    // Track analytics
                    if (isNewUser) {
                        analyticsTracker.logSignUp("google")
                    } else {
                        analyticsTracker.logLogin("google")
                    }
                    analyticsTracker.setUserId(user.uid)

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
                val errorMessage = getAppCheckFriendlyErrorMessage(e)
                _authState.value = AuthState.Error(errorMessage)
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

    /**
     * Set Google Sign-In error from LoginScreen
     * Used for better error handling with user-friendly messages
     */
    fun setGoogleSignInError(errorMessage: String) {
        _authState.value = AuthState.Error(errorMessage)
        Log.e(TAG, "Google Sign-In Error: $errorMessage")
    }

    /**
     * Menganalisa error dan memberikan pesan yang user-friendly,
     * khususnya untuk error yang terkait dengan Firebase App Check.
     */
    private fun getAppCheckFriendlyErrorMessage(exception: Exception): String {
        val errorMessage = exception.message ?: ""
        val exceptionType = exception.javaClass.simpleName

        Log.e(TAG, "=== ERROR ANALYSIS ===")
        Log.e(TAG, "Exception Type: $exceptionType")
        Log.e(TAG, "Error Message: $errorMessage")
        Log.e(TAG, "Full Exception: ", exception)

        // Deteksi App Check errors berdasarkan berbagai pattern
        val isAppCheckError = errorMessage.contains("app check", ignoreCase = true) ||
                errorMessage.contains("appcheck", ignoreCase = true) ||
                errorMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
                errorMessage.contains("Requests are blocked", ignoreCase = true) ||
                errorMessage.contains("too many attempts", ignoreCase = true) ||
                errorMessage.contains("quota", ignoreCase = true) ||
                errorMessage.contains("UNAUTHENTICATED", ignoreCase = true)

        return when {
            // App Check related errors
            isAppCheckError -> {
                Log.e(TAG, "🔴 APP CHECK ERROR DETECTED!")
                Log.e(TAG, "════════════════════════════════════════════════")
                Log.e(TAG, "SOLUSI CEPAT:")
                Log.e(TAG, "1. Buka Firebase Console:")
                Log.e(TAG, "   https://console.firebase.google.com/project/learning-korea/appcheck")
                Log.e(TAG, "")
                Log.e(TAG, "2. Klik tab 'APIs'")
                Log.e(TAG, "")
                Log.e(TAG, "3. Ubah semua service dari 'Enforced' ke 'Permissive'")
                Log.e(TAG, "")
                Log.e(TAG, "4. Restart aplikasi dan coba lagi")
                Log.e(TAG, "════════════════════════════════════════════════")

                when {
                    errorMessage.contains("too many attempts", ignoreCase = true) ->
                        "App Check: Terlalu banyak percobaan. Ubah App Check ke mode 'Permissive' di Firebase Console, atau tunggu 1 jam."

                    errorMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
                    errorMessage.contains("Requests are blocked", ignoreCase = true) ->
                        "App Check sedang memblokir request. Ubah ke mode 'Permissive' di Firebase Console untuk development."

                    errorMessage.contains("UNAUTHENTICATED", ignoreCase = true) ->
                        "App Check: Token tidak valid. Daftarkan debug token atau ubah ke mode 'Permissive' di Firebase Console."

                    else ->
                        "Terjadi masalah dengan App Check. Ubah ke mode 'Permissive' di Firebase Console untuk melanjutkan development."
                }
            }

            // Network errors
            exception is IOException ->
                "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."

            // Firebase Auth specific errors
            exception is FirebaseAuthInvalidCredentialsException ->
                "Login gagal. Periksa email dan password Anda."

            exception is FirebaseAuthUserCollisionException ->
                "Email ini sudah terdaftar. Gunakan email lain atau login."

            // Generic error
            else -> {
                if (errorMessage.isNotBlank()) {
                    "Login gagal: $errorMessage"
                } else {
                    "Google Sign-In gagal. Silakan coba lagi."
                }
            }
        }
    }
}