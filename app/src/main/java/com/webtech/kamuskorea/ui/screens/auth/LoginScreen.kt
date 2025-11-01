package com.webtech.kamuskorea.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreen", "=== GOOGLE SIGN IN RESULT ===")
        Log.d("LoginScreen", "Result code: ${result.resultCode}")
        Log.d("LoginScreen", "RESULT_OK: ${Activity.RESULT_OK}")
        Log.d("LoginScreen", "RESULT_CANCELED: ${Activity.RESULT_CANCELED}")

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.d("LoginScreen", "User completed sign-in flow")
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d("LoginScreen", "Sign in successful")
                    Log.d("LoginScreen", "Account email: ${account?.email}")
                    Log.d("LoginScreen", "Account name: ${account?.displayName}")
                    Log.d("LoginScreen", "ID Token: ${if (account?.idToken != null) "Present" else "NULL"}")

                    if (account?.idToken != null) {
                        authViewModel.signInWithGoogle(account.idToken!!)
                    } else {
                        Log.e("LoginScreen", "ID Token is NULL!")
                    }
                } catch (e: ApiException) {
                    Log.e("LoginScreen", "Google sign in failed with ApiException")
                    Log.e("LoginScreen", "Status code: ${e.statusCode}")
                    Log.e("LoginScreen", "Status message: ${e.message}")

                    // Common error codes
                    when (e.statusCode) {
                        10 -> Log.e("LoginScreen", "Developer Error: Check SHA-1 certificate fingerprint in Firebase Console")
                        12500 -> Log.e("LoginScreen", "Sign in currently unavailable")
                        12501 -> Log.e("LoginScreen", "User cancelled or closed the sign-in flow")
                        else -> Log.e("LoginScreen", "Unknown error code: ${e.statusCode}")
                    }
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.w("LoginScreen", "User cancelled sign-in")
                Log.w("LoginScreen", "This can happen if:")
                Log.w("LoginScreen", "1. User pressed back button")
                Log.w("LoginScreen", "2. SHA-1 fingerprint not configured in Firebase")
                Log.w("LoginScreen", "3. OAuth client ID not properly configured")
                Log.w("LoginScreen", "4. google-services.json not up to date")
            }
            else -> {
                Log.e("LoginScreen", "Unexpected result code: ${result.resultCode}")
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            authViewModel.resetAuthState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Login",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { authViewModel.signIn(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState != AuthState.Loading && email.isNotBlank() && password.isNotBlank()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("atau")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                Log.d("LoginScreen", "=== STARTING GOOGLE SIGN IN ===")
                try {
                    val signInIntent = authViewModel.getGoogleSignInIntent(context)
                    Log.d("LoginScreen", "Sign in intent created successfully")
                    googleSignInLauncher.launch(signInIntent)
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Failed to create sign in intent", e)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState != AuthState.Loading
        ) {
            Text("Login dengan Google")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("Belum punya akun? Daftar")
        }

        when (val state = authState) {
            is AuthState.Loading -> {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            is AuthState.Error -> {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {}
        }
    }
}