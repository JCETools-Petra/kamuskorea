package com.webtech.kamuskorea.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.webtech.kamuskorea.data.Ebook
import com.webtech.kamuskorea.ui.navigation.Screen
import com.webtech.kamuskorea.ui.screens.ebook.EbookViewModel

@Composable
fun EbookScreen(
    navController: NavController,
    viewModel: EbookViewModel
) {
    val ebooks by viewModel.ebooks.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "E-Book Pembelajaran",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (ebooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(ebooks) { ebook ->
                    EbookItem(
                        ebook = ebook,
                        onEbookClick = {
                            if (ebook.isPremium && !isPremiumUser) {
                                navController.navigate(Screen.Profile.route)
                            } else {
                                // PERBAIKI PEMANGGILAN NAVIGASI DI SINI
                                navController.navigate("${Screen.PdfViewer.route}/${ebook.id}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EbookItem(ebook: Ebook, onEbookClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onEbookClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(contentAlignment = Alignment.TopEnd) {
                AsyncImage(
                    model = ebook.coverImageUrl,
                    contentDescription = ebook.title,
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                if (ebook.isPremium) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(8.dp).clip(MaterialTheme.shapes.small)
                    ) {
                        Text(
                            text = "PREMIUM",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = ebook.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
            )
            Text(
                text = ebook.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            )
        }
    }
}