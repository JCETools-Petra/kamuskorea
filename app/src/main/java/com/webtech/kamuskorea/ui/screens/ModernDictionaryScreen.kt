package com.webtech.kamuskorea.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.ui.screens.dictionary.DictionaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDictionaryScreen(
    viewModel: DictionaryViewModel = hiltViewModel()
) {
    val TAG = "ModernDictionaryScreen"

    val searchQuery by viewModel.searchQuery.collectAsState()
    val words by viewModel.words.collectAsState()
    var selectedWord by remember { mutableStateOf<Word?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // ✅ TAMBAHKAN: Log untuk debug UI
    LaunchedEffect(words) {
        Log.d(TAG, "========== UI UPDATE ==========")
        Log.d(TAG, "Words state updated: ${words.size} items")
        words.take(3).forEach { word ->
            Log.d(TAG, "  - ${word.koreanWord}")
        }
        Log.d(TAG, "================================")
    }

    Scaffold(
        topBar = {
            Column {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        Log.d(TAG, "SearchBar onQueryChange: '$it'")
                        viewModel.onSearchQueryChange(it)
                    },
                    onSearch = { /* Do nothing or handle search */ },
                    active = searchQuery.isNotEmpty(),
                    onActiveChange = {},
                    placeholder = { Text("Cari kata dalam Korea, Romanisasi, atau Indonesia...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        Row {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    Log.d(TAG, "Clear button clicked")
                                    viewModel.onSearchQueryChange("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search suggestions bisa ditambahkan di sini
                }

                // Filter chips
                AnimatedVisibility(visible = showFilterMenu) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { /* TODO: Implement filter */ },
                            label = { Text("Kata Dasar") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { /* TODO: Implement filter */ },
                            label = { Text("Kalimat") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { /* TODO: Implement filter */ },
                            label = { Text("Favorit") }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // ✅ PERBAIKAN: Cek kondisi dengan lebih teliti
                words.isEmpty() && searchQuery.isNotEmpty() -> {
                    Log.d(TAG, "Showing empty state")
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Tidak ada hasil untuk \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Coba kata kunci lain atau periksa ejaan Anda",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                words.isEmpty() && searchQuery.isEmpty() -> {
                    Log.d(TAG, "Showing initial state")
                    // Initial state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Cari Kata",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ketik kata dalam Korea, Romanisasi, atau Bahasa Indonesia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    // ✅ PERBAIKAN: Tampilkan hasil dengan logging
                    Log.d(TAG, "Showing results list with ${words.size} items")

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background), // ✅ Tambahkan background
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ✅ PERBAIKAN: Gunakan items dengan key
                        items(
                            items = words,
                            key = { word -> word.id }
                        ) { word ->
                            ModernWordCard(
                                word = word,
                                isSelected = selectedWord == word,
                                onClick = {
                                    Log.d(TAG, "Word clicked: ${word.koreanWord}")
                                    selectedWord = if (selectedWord == word) null else word
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernWordCard(
    word: Word,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word.koreanWord,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "[${word.romanization}]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { /* TODO: Add to favorites */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = "Simpan",
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = { /* TODO: Share word */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Bagikan",
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = word.indonesianTranslation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(visible = isSelected) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Example sentence (placeholder)
                    Text(
                        "Contoh Kalimat:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• ${word.koreanWord}을/를 사용한 예문이 여기에 표시됩니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}