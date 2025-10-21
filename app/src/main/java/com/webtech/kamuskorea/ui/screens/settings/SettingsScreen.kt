package com.webtech.kamuskorea.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
// Import Material 3 components
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class) // Diperlukan untuk ExposedDropdownMenuBox M3
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel() // HiltViewModel sudah benar
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    // Hapus atau komentari state notifikasi jika belum diimplementasikan di ViewModel
    // val notificationsEnabled by viewModel.notificationsEnabled.collectAsState() // <-- Error di sini

    val themeOptions = listOf("Default", "Forest", "Ocean")
    var expanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Pengaturan Tampilan", style = MaterialTheme.typography.titleLarge)
        }

        item {
            // Pengaturan Tema
            Column {
                Text("Tema Aplikasi", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currentTheme,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Tema") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        themeOptions.forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(theme) },
                                onClick = {
                                    viewModel.saveTheme(theme) // PERBAIKAN: Gunakan saveTheme, bukan changeTheme
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        // --- BAGIAN NOTIFIKASI (DIKOMENTARI KARENA BELUM ADA DI VIEWMODEL) ---
        /*
        item {
            // Pengaturan Notifikasi (Contoh)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Aktifkan Notifikasi", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = notificationsEnabled, // <-- Error di sini
                    onCheckedChange = { isChecked ->
                        // viewModel.setNotificationsEnabled(isChecked) // <-- Error di sini
                    }
                )
            }
        }
        */
        // --- AKHIR BAGIAN NOTIFIKASI ---

        // Tambahkan item pengaturan lainnya di sini jika perlu
    }
}