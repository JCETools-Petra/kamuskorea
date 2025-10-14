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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                authViewModel.signInWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("LoginScreen", "Google sign in failed", e)
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { authViewModel.signIn(email, password) }, // Gunakan signIn
            modifier = Modifier.fillMaxWidth(),
            enabled = authState != AuthState.Loading
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("atau")
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val signInIntent = authViewModel.getGoogleSignInIntent(context)
                googleSignInLauncher.launch(signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
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
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }
    }
}