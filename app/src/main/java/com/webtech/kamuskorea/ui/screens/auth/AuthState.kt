package com.webtech.kamuskorea.ui.screens.auth

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object RegistrationSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}