package com.webtech.kamuskorea.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val showDivider: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val themeOptions = listOf("Default", "Forest", "Ocean")
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Pengaturan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Sesuaikan pengalaman belajar Anda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Section
        SettingsSection(title = "Tampilan") {
            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.Palette,
                    title = "Tema Aplikasi",
                    subtitle = currentTheme
                ),
                onClick = { showThemeDialog = true }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.TextFields,
                    title = "Ukuran Teks",
                    subtitle = "Sedang"
                ),
                onClick = { /* TODO: Text size adjustment */ }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.Language,
                    title = "Bahasa Interface",
                    subtitle = "Bahasa Indonesia",
                    showDivider = false
                ),
                onClick = { /* TODO: Language selection */ }
            )
        }

        // Learning Section
        SettingsSection(title = "Pembelajaran") {
            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.Notifications,
                    title = "Pengingat Harian",
                    subtitle = "Ingatkan saya untuk belajar"
                ),
                trailing = {
                    Switch(
                        checked = false,
                        onCheckedChange = { /* TODO: Toggle notifications */ }
                    )
                }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.Quiz,
                    title = "Tingkat Kesulitan",
                    subtitle = "Menengah"
                ),
                onClick = { /* TODO: Difficulty selection */ }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "Mode Pembelajaran",
                    subtitle = "Adaptif",
                    showDivider = false
                ),
                onClick = { /* TODO: Learning mode selection */ }
            )
        }

        // Data & Storage Section
        SettingsSection(title = "Data & Penyimpanan") {
            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.CloudDownload,
                    title = "Download Konten Offline",
                    subtitle = "Simpan untuk akses tanpa internet"
                ),
                onClick = { /* TODO: Offline download */ }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Hapus Cache",
                    subtitle = "Bebaskan ruang penyimpanan"
                ),
                onClick = { /* TODO: Clear cache */ }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.Backup,
                    title = "Backup & Restore",
                    subtitle = "Simpan progress Anda",
                    showDivider = false
                ),
                onClick = { /* TODO: Backup/restore */ }
            )
        }

        // About Section
        SettingsSection(title = "Tentang") {
            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.Info,
                    title = "Tentang Aplikasi",
                    subtitle = "Versi 1.0.0"
                ),
                onClick = { showAboutDialog = true }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Kebijakan Privasi",
                    subtitle = "Baca kebijakan privasi kami"
                ),
                onClick = { /* TODO: Open privacy policy */ }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.Gavel,
                    title = "Syarat & Ketentuan",
                    subtitle = "Baca syarat penggunaan"
                ),
                onClick = { /* TODO: Open terms */ }
            )

            SettingItemRow(
                item = SettingItem(
                    icon = Icons.Default.RateReview,
                    title = "Beri Rating",
                    subtitle = "Bantu kami berkembang",
                    showDivider = false
                ),
                onClick = { /* TODO: Open Play Store rating */ }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // App Version Footer
        Text(
            "Kamus Korea v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Pilih Tema") },
            text = {
                Column {
                    themeOptions.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = theme == currentTheme,
                                onClick = {
                                    viewModel.saveTheme(theme)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(theme)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Tentang Kamus Korea") },
            text = {
                Column {
                    Text("Versi: 1.0.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Kamus Korea adalah aplikasi pembelajaran bahasa Korea yang komprehensif dengan ribuan kata, e-book gratis, dan kuis interaktif.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Dikembangkan dengan ❤️",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingItemRow(
    item: SettingItem,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (item.subtitle != null) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
        if (item.showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}