package com.webtech.kamuskorea.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.webtech.kamuskorea.data.Ebook
import com.webtech.kamuskorea.ui.screens.ebook.EbookViewModel

@Composable
fun EbookScreen(
    isPremium: Boolean,
    onNavigateToProfile: () -> Unit,
    // Tambahkan parameter NavController
    onEbookClick: (Ebook) -> Unit
) {
    if (isPremium) {
        val viewModel: EbookViewModel = viewModel()
        val ebooks by viewModel.ebooks.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ebooks) { ebook ->
                        EbookItem(ebook = ebook, onClick = { onEbookClick(ebook) })
                    }
                }
            }
        }
    } else {
        // Tampilan untuk non-premium (sudah ada)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Fitur E-Book hanya untuk anggota premium.",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = onNavigateToProfile) {
                Text("Upgrade ke Premium")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EbookItem(ebook: Ebook, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ebook.coverImageUrl,
                contentDescription = "Cover ${ebook.title}",
                modifier = Modifier.size(60.dp, 90.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = ebook.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = ebook.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
        }
    }
}