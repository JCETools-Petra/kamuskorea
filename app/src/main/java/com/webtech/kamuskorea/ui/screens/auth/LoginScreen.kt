package com.webtech.kamuskorea.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.webtech.kamuskorea.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit, // ✅ NEW
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

                    // User-friendly error messages
                    val errorMessage = when (e.statusCode) {
                        10 -> {
                            Log.e("LoginScreen", "Developer Error: Check SHA-1 certificate fingerprint in Firebase Console")
                            "Konfigurasi Google Sign-In bermasalah. Silakan hubungi developer atau coba login dengan email."
                        }
                        12500 -> {
                            Log.e("LoginScreen", "Sign in currently unavailable")
                            "Layanan Google Sign-In tidak tersedia saat ini. Silakan coba lagi nanti atau gunakan login email."
                        }
                        12501 -> {
                            Log.e("LoginScreen", "User cancelled or closed the sign-in flow")
                            null // Don't show error if user cancelled intentionally
                        }
                        else -> {
                            Log.e("LoginScreen", "Unknown error code: ${e.statusCode}")
                            "Login Google gagal. Silakan coba lagi atau gunakan login email. (Error code: ${e.statusCode})"
                        }
                    }

                    // Pass error to ViewModel to display to user
                    errorMessage?.let {
                        authViewModel.setGoogleSignInError(it)
                    }
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.w("LoginScreen", "User cancelled sign-in")
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

    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Selamat Datang",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Masuk untuk melanjutkan",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Login Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Icon"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Sembunyikan password" else "Tampilkan password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Forgot Password Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onNavigateToForgotPassword) {
                            Text(
                                "Lupa Password?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Login Button
                    Button(
                        onClick = { authViewModel.signIn(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = authState != AuthState.Loading && email.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (authState == AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Masuk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Divider with "atau"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "atau",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    // Google Sign In Button
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = authState != AuthState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(20.dp),
                                tint = androidx.compose.ui.graphics.Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Masuk dengan Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Belum punya akun? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "Daftar Sekarang",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Error Message
            when (val state = authState) {
                is AuthState.Error -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}