package com.webtech.kamuskorea.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webtech.kamuskorea.ads.BannerAdView

@Composable
fun MemorizationScreen(isPremium: Boolean, onNavigateToProfile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPremium) {
                Text("Fitur Hafalan Kata!", style = MaterialTheme.typography.headlineMedium)
            } else {
                Text(
                    "Fitur Hafalan hanya untuk anggota premium.",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = onNavigateToProfile) {
                    Text("Upgrade ke Premium")
                }
            }
        }

        // Banner Ad at the bottom (for non-premium users)
        if (!isPremium) {
            BannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}