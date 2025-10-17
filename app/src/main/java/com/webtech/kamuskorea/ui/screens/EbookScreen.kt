package com.webtech.kamuskorea.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtech.kamuskorea.data.Ebook
import com.webtech.kamuskorea.ui.screens.ebook.EbookViewModel

@Composable
fun EbookScreen(
    viewModel: EbookViewModel,
    onNavigateToPdf: (pdfUrl: String, title: String) -> Unit,
    onNavigateToPremiumLock: () -> Unit
) {
    val ebooks by viewModel.ebooks
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(ebooks) { ebook ->
                EbookItem(
                    ebook = ebook,
                    onEbookClick = {
                        if (ebook.pdfUrl.isEmpty()) {
                            onNavigateToPremiumLock()
                        } else {
                            onNavigateToPdf(ebook.pdfUrl, ebook.title)
                        }
                    }
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null) {
            Text(
                text = error!!,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
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
                    Badge(
                        modifier = Modifier.padding(8.dp),
                        containerColor = if (ebook.pdfUrl.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    ) {
                        if (ebook.pdfUrl.isEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Terkunci",
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                        } else {
                            Text(
                                text = "Premium",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = ebook.title,
                style = MaterialTheme.typography.titleMedium,
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