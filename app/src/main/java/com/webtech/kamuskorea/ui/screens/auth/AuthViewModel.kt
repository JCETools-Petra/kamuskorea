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
import com.webtech.kamuskorea.data.network.UserSyncRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val apiService: ApiService // <=== ADDED
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

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
                    val userMap = hashMapOf(
                        "uid" to user.uid,
                        "name" to name,
                        "email" to email,
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
        Log.d("AuthViewModel", "Web Client ID: $WEB_CLIENT_ID")
        Log.d("AuthViewModel", "Package Name: ${context.packageName}")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()

        Log.d("AuthViewModel", "GoogleSignInOptions created successfully")
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
                    // ========================================
                    // ADDED: Sync user to MySQL backend
                    // ========================================
                    try {
                        Log.d("AuthViewModel", "Syncing user to backend...")
                        val syncRequest = UserSyncRequest(
                            email = user.email,
                            name = user.displayName,
                            photoUrl = user.photoUrl?.toString()
                        )

                        val response = apiService.syncUser(syncRequest)
                        if (response.isSuccessful) {
                            val syncResult = response.body()
                            Log.d("AuthViewModel", "✅ User synced: ${syncResult?.message}, isNew=${syncResult?.isNew}")
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown error"
                            Log.w("AuthViewModel", "⚠️ Failed to sync user: ${response.code()} - $errorBody")
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "❌ Error syncing user to backend", e)
                        // Continue anyway - sync akan auto-create saat user buka profile
                    }
                    // ========================================
                    // END SYNC
                    // ========================================

                    // Simpan ke Firestore jika user baru
                    if (authResult.additionalUserInfo?.isNewUser == true) {
                        val userMap = hashMapOf(
                            "uid" to user.uid,
                            "name" to user.displayName,
                            "email" to user.email,
                            "profilePictureUrl" to user.photoUrl?.toString(),
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

    fun resetAuthState() {
        _authState.value = AuthState.Initial
    }
}