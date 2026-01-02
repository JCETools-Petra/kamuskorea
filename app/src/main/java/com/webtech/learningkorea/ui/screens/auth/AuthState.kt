// jcetools-petra/kamuskorea/kamuskorea-c5c9a4a4ef864b671ea565d3a42fd6f4156b965d/app/src/main/java/com/webtech/kamuskorea/ui/screens/auth/AuthState.kt

package com.webtech.learningkorea.ui.screens.auth

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object SuccessNeedVerification : AuthState()
    data class Error(val message: String) : AuthState()
}

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