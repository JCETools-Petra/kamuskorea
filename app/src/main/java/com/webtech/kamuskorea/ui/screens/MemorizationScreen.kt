package com.webtech.kamuskorea.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun MemorizationScreen(
    isPremium: Boolean,
    onNavigateToProfile: () -> Unit
) {
    // Logika penguncian premium sama seperti Ebook dan Quiz
    if (isPremium) {
        // Tampilan jika pengguna adalah premium
        PremiumContentScreen(content = "Selamat! Ini adalah konten Hafalan Premium.")
    } else {
        // Tampilan jika pengguna bukan premium
        PremiumLockScreen(onNavigateToProfile = onNavigateToProfile)
    }
}