package com.webtech.kamuskorea.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webtech.kamuskorea.BuildConfig // Pastikan import ini ada

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val themeOptions = listOf("Default", "Forest", "Ocean")
    val currentTheme by viewModel.currentTheme.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Pilih Tema") },
            text = {
                Column {
                    themeOptions.forEach { themeName ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.changeTheme(themeName)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (themeName == currentTheme),
                                onClick = {
                                    viewModel.changeTheme(themeName)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(themeName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column {
                    Text(
                        text = "TAMPILAN",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                    ListItem(
                        headlineContent = { Text("Tema Aplikasi") },
                        supportingContent = { Text(currentTheme) },
                        leadingContent = { Icon(Icons.Default.Palette, contentDescription = "Tema") },
                        modifier = Modifier.clickable { showThemeDialog = true }
                    )
                }
            }
        }

        item {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column {
                    Text(
                        text = "UMUM",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                    ListItem(
                        headlineContent = { Text("Notifikasi Harian") },
                        supportingContent = { Text("Dapatkan rekomendasi kata setiap hari") },
                        leadingContent = { Icon(Icons.Default.Notifications, contentDescription = "Notifikasi") },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Hapus Riwayat Pencarian") },
                        leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = "Hapus Riwayat") },
                        modifier = Modifier.clickable { /* TODO: Panggil fungsi ViewModel untuk hapus riwayat */ }
                    )
                }
            }
        }

        item {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column {
                    Text(
                        text = "TENTANG",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                    ListItem(
                        headlineContent = { Text("Beri Rating Aplikasi") },
                        leadingContent = { Icon(Icons.Default.Star, contentDescription = "Rating") },
                        modifier = Modifier.clickable {
                            try {
                                val packageName = context.packageName
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val packageName = context.packageName
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                                context.startActivity(intent)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Kebijakan Privasi") },
                        leadingContent = { Icon(Icons.Default.Shield, contentDescription = "Kebijakan Privasi") },
                        modifier = Modifier.clickable {
                            val url = "https://www.google.com" // TODO: Ganti dengan URL Kebijakan Privasi Anda
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Versi Aplikasi") },
                        supportingContent = { Text(BuildConfig.VERSION_NAME) },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = "Versi") }
                    )
                }
            }
        }
    }
}