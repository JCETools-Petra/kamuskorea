package com.webtech.learningkorea.ui.screens.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.learningkorea.ui.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ========== THEME ==========
    val currentTheme = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.THEME_KEY] ?: "Default"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Default"
    )

    fun saveTheme(theme: String) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsDataStore.THEME_KEY] = theme
            }
        }
    }

    // ========== DARK MODE ==========
    val darkMode = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.DARK_MODE_KEY] ?: "light"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "light"
    )

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsDataStore.DARK_MODE_KEY] = mode
            }
        }
    }

    // ========== TEXT SCALE ==========
    val textScale = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.TEXT_SCALE_KEY] ?: "Sedang"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Sedang"
    )

    fun setTextScale(scale: String) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsDataStore.TEXT_SCALE_KEY] = scale
            }
        }
    }

    // Fungsi helper untuk mendapatkan multiplier
    fun getTextScaleMultiplier(scale: String): Float {
        return when (scale) {
            "Kecil" -> 0.85f
            "Besar" -> 1.15f
            else -> 1.0f // Sedang
        }
    }

    // ========== LANGUAGE ==========
    val language = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.LANGUAGE_KEY] ?: "id"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "id"
    )

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsDataStore.LANGUAGE_KEY] = langCode
            }
        }
    }

    // ========== NOTIFICATIONS ==========
    val notificationsEnabled = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.NOTIFICATIONS_ENABLED_KEY] ?: false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val notificationHour = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.NOTIFICATION_HOUR_KEY] ?: 9
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 9
    )

    val notificationMinute = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.NOTIFICATION_MINUTE_KEY] ?: 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsDataStore.NOTIFICATIONS_ENABLED_KEY] = enabled
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsDataStore.NOTIFICATION_HOUR_KEY] = hour
                settings[SettingsDataStore.NOTIFICATION_MINUTE_KEY] = minute
            }
        }
    }

    // ========== CACHE MANAGEMENT ==========
    private val _cacheSize = MutableStateFlow("Menghitung...")
    val cacheSize = _cacheSize.asStateFlow()

    private val _clearCacheStatus = MutableStateFlow<String?>(null)
    val clearCacheStatus = _clearCacheStatus.asStateFlow()

    init {
        calculateCacheSize()
    }

    fun calculateCacheSize() {
        viewModelScope.launch {
            try {
                val size = context.cacheDir?.let { getCacheSize(it) } ?: 0L
                _cacheSize.value = formatFileSize(size)
            } catch (e: Exception) {
                _cacheSize.value = "Tidak dapat menghitung"
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                context.cacheDir?.deleteRecursively()
                _clearCacheStatus.value = "Cache berhasil dihapus"
                calculateCacheSize()
            } catch (e: Exception) {
                _clearCacheStatus.value = "Gagal menghapus cache: ${e.message}"
            }
        }
    }

    fun resetClearCacheStatus() {
        _clearCacheStatus.value = null
    }

    private fun getCacheSize(dir: File): Long {
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getCacheSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }

    // ========== OFFLINE DOWNLOAD ==========
    val offlineDownloadEnabled = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.OFFLINE_DOWNLOAD_KEY] ?: false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleOfflineDownload(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsDataStore.OFFLINE_DOWNLOAD_KEY] = enabled
            }
        }
    }
}