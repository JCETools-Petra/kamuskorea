package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth


@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current as Activity

    // Mengambil state dari ProfileViewModel
    val productDetails by viewModel.productDetails.collectAsState()
    val hasActiveSubscription by viewModel.hasActiveSubscription.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profil Pengguna", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Login sebagai: ${currentUser?.email ?: "Tamu"}")
        Spacer(modifier = Modifier.height(32.dp))

        // Tampilan akan berbeda tergantung status langganan
        if (hasActiveSubscription) {
            // Tampilan jika pengguna sudah berlangganan
            Text("Status Langganan: Premium ✨", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            // Tampilan jika pengguna belum berlangganan
            Text("Status Langganan: Gratis", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Tampilkan detail produk jika sudah berhasil diambil dari Google Play
            productDetails?.let { details ->
                // Ambil harga dari detail produk
                val price = details.subscriptionOfferDetails?.first()?.pricingPhases?.pricingPhaseList?.first()?.formattedPrice

                Button(onClick = { viewModel.launchPurchaseFlow(context) }) {
                    Text("Berlangganan Sekarang (${price ?: "..."})")
                }
                Text(
                    text = details.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } ?: run {
                // Tampilan loading selagi mengambil detail produk
                CircularProgressIndicator()
                Text("Memuat info langganan...", modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Spacer untuk mendorong tombol logout ke bawah
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Logout")
        }
    }
}