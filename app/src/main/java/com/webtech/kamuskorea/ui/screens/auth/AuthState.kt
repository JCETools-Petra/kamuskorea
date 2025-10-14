package com.webtech.kamuskorea.ui.screens.auth

sealed class AuthState {
    object Initial : AuthState() // Pastikan baris ini ada
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}