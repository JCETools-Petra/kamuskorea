package com.webtech.kamuskorea.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
// Import Material 3 (M3)
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.webtech.kamuskorea.data.Ebook
import com.webtech.kamuskorea.ui.screens.ebook.EbookViewModel
import com.webtech.kamuskorea.ads.BannerAdView

@Composable
fun EbookScreen(
    viewModel: EbookViewModel = hiltViewModel(),
    onNavigateToPdf: (String, String) -> Unit,
    onNavigateToPremiumLock: () -> Unit,
    isPremium: Boolean = false
) {
    val ebooks by viewModel.ebooks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Main content
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error, // M3: colorScheme
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = ebooks, key = { it.id }) { ebook ->
                    EbookItem(
                        ebook = ebook,
                        onClick = {
                            if (ebook.pdfUrl.isNotEmpty()) {
                                onNavigateToPdf(Uri.encode(ebook.pdfUrl), ebook.title)
                            } else if (ebook.isPremium) {
                                onNavigateToPremiumLock()
                            }
                        }
                    )
                }
            }
        }

        // Banner Ad di bagian bawah (hanya untuk non-premium user)
        if (!isPremium) {
            BannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EbookItem(ebook: Ebook, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // M3: elevation
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.BottomStart) {
            AsyncImage(
                model = ebook.coverImageUrl,
                contentDescription = ebook.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
            // Tampilkan ikon gembok jika premium dan URL PDF kosong
            if (ebook.isPremium && ebook.pdfUrl.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Text(
                text = ebook.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), // M3: titleMedium
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            )
        }
    }
}