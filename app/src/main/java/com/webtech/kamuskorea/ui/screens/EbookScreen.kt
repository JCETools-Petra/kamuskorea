package com.webtech.kamuskorea.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun EbookScreen(
    isPremium: Boolean,
    onNavigateToProfile: () -> Unit
) {
    if (isPremium) {
        // Tampilan jika pengguna adalah premium
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Selamat! Ini adalah konten E-Book Premium.")
        }
    } else {
        // Tampilan jika pengguna bukan premium
        PremiumLockScreen(onNavigateToProfile = onNavigateToProfile)
    }
}