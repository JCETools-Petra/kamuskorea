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

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel
) {
    val currentUser = viewModel.currentUser
    val context = LocalContext.current as Activity

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

        if (hasActiveSubscription) {
            Text("Status Langganan: Premium ✨", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            Text("Status Langganan: Gratis", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            productDetails?.let { details ->
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
                CircularProgressIndicator()
                Text("Memuat info langganan...", modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Logout")
        }
    }
}