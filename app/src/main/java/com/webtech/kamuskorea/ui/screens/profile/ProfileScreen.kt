package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    // Kita akan menggunakan state yang sudah ada di ViewModel
    val productDetails by viewModel.productDetails.collectAsState()
    val hasActiveSubscription by viewModel.hasActiveSubscription.collectAsState()

    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Kita bisa sederhanakan dengan tidak menampilkan loading indicator di sini,
            // karena data subscription dan produk sudah di-load di background.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Profil & Langganan",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text("Email: ${currentUser?.email ?: "Tidak ada"}")
                Spacer(modifier = Modifier.height(16.dp))

                if (hasActiveSubscription) {
                    Text(
                        "Status: Pengguna Premium",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        "Status: Pengguna Gratis",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    productDetails?.let { details ->
                        Button(onClick = {
                            // Panggil fungsi `purchase` dari ViewModel
                            viewModel.purchase(context as Activity)
                        }) {
                            // Tampilkan harga dari detail produk
                            val price = details.subscriptionOfferDetails?.firstOrNull()
                                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
                            Text("Langganan Sekarang ($price)")
                        }
                    } ?: run {
                        // Tampilkan loading atau pesan jika detail belum siap
                        CircularProgressIndicator()
                        Text(
                            "Memuat informasi langganan...",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}