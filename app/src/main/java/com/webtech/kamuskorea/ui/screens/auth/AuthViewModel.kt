package com.webtech.kamuskorea.ui.screens.auth

import android.app.Application
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.webtech.kamuskorea.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    // --- FUNGSI REGISTER DIMODIFIKASI ---
    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Buat pengguna seperti biasa
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                // 2. Kirim email verifikasi ke pengguna yang baru dibuat
                result.user?.sendEmailVerification()?.await()
                // 3. Langsung logout agar pengguna tidak masuk dalam keadaan belum terverifikasi
                auth.signOut()
                // 4. Kirim state khusus untuk memberitahu UI bahwa registrasi berhasil dan email sudah dikirim
                _authState.value = AuthState.RegistrationSuccess
            } catch (e: FirebaseAuthException) {
                val errorMessage = mapFirebaseError(e)
                _authState.value = AuthState.Error(errorMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Terjadi error yang tidak diketahui.")
            }
        }
    }

    // --- FUNGSI LOGIN DIMODIFIKASI ---
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                // 1. Setelah kredensial benar, periksa status verifikasi email
                if (result.user?.isEmailVerified == true) {
                    // Jika sudah terverifikasi, lanjutkan ke state sukses
                    _authState.value = AuthState.Success
                } else {
                    // Jika belum, kirim pesan error dan langsung logout lagi
                    auth.signOut()
                    _authState.value = AuthState.Error("Email belum diverifikasi. Silakan cek inbox Anda.")
                }
            } catch (e: FirebaseAuthException) {
                val errorMessage = mapFirebaseError(e)
                _authState.value = AuthState.Error(errorMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Terjadi error yang tidak diketahui.")
            }
        }
    }

    // --- Fungsi Google Sign-In (tidak berubah) ---
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(application.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    private val googleSignInClient = GoogleSignIn.getClient(application, gso)

    fun getGoogleSignInIntent(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    fun signInWithGoogle(result: ActivityResult) {
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                viewModelScope.launch {
                    _authState.value = AuthState.Loading
                    try {
                        auth.signInWithCredential(credential).await()
                        _authState.value = AuthState.Success
                    } catch (e: Exception) {
                        _authState.value = AuthState.Error(e.message ?: "Login dengan Google gagal")
                    }
                }
            } catch (e: ApiException) {
                _authState.value = AuthState.Error("Gagal mendapatkan info akun Google.")
            }
        } else {
            _authState.value = AuthState.Error("Login dengan Google dibatalkan.")
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    private fun mapFirebaseError(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
            "ERROR_WRONG_PASSWORD" -> "Password salah."
            "ERROR_USER_NOT_FOUND" -> "Pengguna tidak ditemukan."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah terdaftar."
            "ERROR_WEAK_PASSWORD" -> "Password terlalu lemah (minimal 6 karakter)."
            else -> "Otentikasi gagal: ${e.message}"
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object RegistrationSuccess : AuthState() // State baru
    data class Error(val message: String) : AuthState()
}