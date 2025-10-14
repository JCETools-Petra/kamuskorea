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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    // Fungsi Sign In dengan Email/Password
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login Gagal")
            }
        }
    }

    // Fungsi Sign Up dengan Email/Password
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
                val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                user?.updateProfile(profileUpdates)?.await()
                if (user != null) {
                    val userMap = hashMapOf("uid" to user.uid, "name" to name, "email" to email, "isPremium" to false)
                    firestore.collection("users").document(user.uid).set(userMap).await()
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Pendaftaran Gagal")
            }
        }
    }

    // --- FUNGSI GOOGLE SIGN-IN YANG DIKEMBALIKAN ---
    fun getGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_WEB_CLIENT_ID") // GANTI DENGAN WEB CLIENT ID ANDA
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

                // Simpan data pengguna ke Firestore jika baru pertama kali login
                if (authResult.additionalUserInfo?.isNewUser == true && user != null) {
                    val userMap = hashMapOf(
                        "uid" to user.uid,
                        "name" to user.displayName,
                        "email" to user.email,
                        "isPremium" to false
                    )
                    firestore.collection("users").document(user.uid).set(userMap).await()
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In Gagal")
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Initial
    }
}