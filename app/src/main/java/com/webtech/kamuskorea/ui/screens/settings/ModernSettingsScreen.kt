package com.webtech.kamuskorea.ui.screens.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.webtech.kamuskorea.notifications.NotificationHelper
import java.util.*

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
    val context = LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }

    // ========== STATE COLLECTION ==========
    val currentTheme by viewModel.currentTheme.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val textScale by viewModel.textScale.collectAsState()
    val language by viewModel.language.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationHour by viewModel.notificationHour.collectAsState()
    val notificationMinute by viewModel.notificationMinute.collectAsState()
    val offlineDownloadEnabled by viewModel.offlineDownloadEnabled.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val clearCacheStatus by viewModel.clearCacheStatus.collectAsState()

    // ========== DIALOG STATES ==========
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    // ========== OPTIONS ==========
    val themeOptions = listOf(
        "Default", "Forest", "Ocean", "Sunset", "Lavender",
        "Cherry", "Midnight", "Mint", "Autumn", "Coral"
    )
    val darkModeOptions = mapOf(
        "system" to "Ikuti Sistem",
        "light" to "Mode Terang",
        "dark" to "Mode Gelap"
    )
    val textSizeOptions = listOf("Kecil", "Sedang", "Besar")
    val languageOptions = mapOf(
        "id" to "Bahasa Indonesia",
        "en" to "English"
    )

    // ========== SNACKBAR ==========
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(clearCacheStatus) {
        clearCacheStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetClearCacheStatus()
        }
    }

    // ========== TIME PICKER ==========
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            viewModel.setNotificationTime(hour, minute)
            if (notificationsEnabled) {
                notificationHelper.scheduleDailyNotification(hour, minute)
            }
        },
        notificationHour,
        notificationMinute,
        true
    )

    // ========== FORMAT TIME ==========
    val formattedTime = String.format("%02d:%02d", notificationHour, notificationMinute)

    // Language display name
    val languageDisplay = languageOptions[language] ?: "Bahasa Indonesia"
    val darkModeDisplay = darkModeOptions[darkMode] ?: "Ikuti Sistem"

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ========== HEADER ==========
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

            // ========== APPEARANCE SECTION ==========
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
                        icon = Icons.Default.DarkMode,
                        title = "Mode Gelap",
                        subtitle = darkModeDisplay
                    ),
                    onClick = { showDarkModeDialog = true }
                )

                SettingItemRow(
                    item = SettingItem(
                        icon = Icons.Default.TextFields,
                        title = "Ukuran Teks",
                        subtitle = textScale
                    ),
                    onClick = { showTextSizeDialog = true }
                )

                SettingItemRow(
                    item = SettingItem(
                        icon = Icons.Default.Language,
                        title = "Bahasa Interface",
                        subtitle = languageDisplay,
                        showDivider = false
                    ),
                    onClick = { showLanguageDialog = true }
                )
            }

            // ========== NOTIFICATIONS SECTION ==========
            SettingsSection(title = "Notifikasi") {
                SettingItemRow(
                    item = SettingItem(
                        icon = Icons.Default.Notifications,
                        title = "Pengingat Harian",
                        subtitle = if (notificationsEnabled) "Aktif - $formattedTime" else "Nonaktif"
                    ),
                    trailing = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.toggleNotifications(enabled)
                                if (enabled) {
                                    notificationHelper.scheduleDailyNotification(
                                        notificationHour,
                                        notificationMinute
                                    )
                                } else {
                                    notificationHelper.cancelDailyNotification()
                                }
                            }
                        )
                    }
                )

                if (notificationsEnabled) {
                    SettingItemRow(
                        item = SettingItem(
                            icon = Icons.Default.AccessTime,
                            title = "Atur Waktu",
                            subtitle = "Jam $formattedTime",
                            showDivider = false
                        ),
                        onClick = { timePickerDialog.show() }
                    )
                }
            }

            // ========== DATA & STORAGE SECTION ==========
            SettingsSection(title = "Data & Penyimpanan") {
                SettingItemRow(
                    item = SettingItem(
                        icon = Icons.Default.CloudDownload,
                        title = "Download Konten Offline",
                        subtitle = if (offlineDownloadEnabled) "Aktif" else "Nonaktif"
                    ),
                    trailing = {
                        Switch(
                            checked = offlineDownloadEnabled,
                            onCheckedChange = { viewModel.toggleOfflineDownload(it) }
                        )
                    }
                )

                SettingItemRow(
                    item = SettingItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Hapus Cache",
                        subtitle = "Ukuran cache: $cacheSize"
                    ),
                    onClick = { showClearCacheDialog = true }
                )

                SettingItemRow(
                    item = SettingItem(
                        icon = Icons.Default.Backup,
                        title = "Backup & Restore",
                        subtitle = "Simpan progress Anda",
                        showDivider = false
                    ),
                    onClick = {
                        // TODO: Implement backup/restore
                    }
                )
            }

            // ========== ABOUT SECTION ==========
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
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://webtechsolution.my.id/kamuskorea/privacy"))
                        context.startActivity(intent)
                    }
                )

                SettingItemRow(
                    item = SettingItem(
                        icon = Icons.Default.Gavel,
                        title = "Syarat & Ketentuan",
                        subtitle = "Baca syarat penggunaan"
                    ),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://webtechsolution.my.id/kamuskorea/terms"))
                        context.startActivity(intent)
                    }
                )

                SettingItemRow(
                    item = SettingItem(
                        icon = Icons.Default.RateReview,
                        title = "Beri Rating",
                        subtitle = "Bantu kami berkembang",
                        showDivider = false
                    ),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            context.startActivity(intent)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ========== APP VERSION FOOTER ==========
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
    }

    // ========== DIALOGS ==========

    // Theme Dialog
    if (showThemeDialog) {
        SelectionDialog(
            title = "Pilih Tema",
            options = themeOptions.map { it to it },
            currentSelection = currentTheme,
            onSelect = { viewModel.saveTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Dark Mode Dialog
    if (showDarkModeDialog) {
        SelectionDialog(
            title = "Mode Gelap",
            options = darkModeOptions.toList(),
            currentSelection = darkMode,
            onSelect = { viewModel.setDarkMode(it) },
            onDismiss = { showDarkModeDialog = false }
        )
    }

    // Text Size Dialog
    if (showTextSizeDialog) {
        SelectionDialog(
            title = "Ukuran Teks",
            options = textSizeOptions.map { it to it },
            currentSelection = textScale,
            onSelect = { viewModel.setTextScale(it) },
            onDismiss = { showTextSizeDialog = false }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        SelectionDialog(
            title = "Bahasa Interface",
            options = languageOptions.toList(),
            currentSelection = language,
            onSelect = { viewModel.setLanguage(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            title = { Text("Hapus Cache") },
            text = {
                Text("Apakah Anda yakin ingin menghapus semua cache? Ukuran cache saat ini: $cacheSize")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheDialog = false
                }) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Batal")
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
                        "Dikembangkan dengan ❤️ oleh WebTech Solution",
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

// ========== REUSABLE COMPONENTS ==========

@Composable
fun SelectionDialog(
    title: String,
    options: List<Pair<String, String>>, // key to display name
    currentSelection: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (key, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(key)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = key == currentSelection,
                            onClick = {
                                onSelect(key)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
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