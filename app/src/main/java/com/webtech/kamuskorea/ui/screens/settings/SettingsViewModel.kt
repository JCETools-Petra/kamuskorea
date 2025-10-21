package com.webtech.kamuskorea.ui.screens.settings

import androidx.datastore.core.DataStore // <-- TAMBAHKAN IMPORT INI
import androidx.datastore.preferences.core.Preferences // <-- TAMBAHKAN IMPORT INI
import androidx.datastore.preferences.core.edit // <-- TAMBAHKAN IMPORT INI
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.ui.datastore.SettingsDataStore // <-- IMPORT COMPANION OBJECT KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map // <-- TAMBAHKAN IMPORT INI
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // --- PERUBAHAN DI SINI: Inject DataStore<Preferences> ---
    private val dataStore: DataStore<Preferences>
    // --- AKHIR PERUBAHAN ---
) : ViewModel() {

    // --- PERUBAHAN DI SINI: Akses DataStore langsung ---
    val currentTheme = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.THEME_KEY] ?: "Default" // Gunakan key dari Companion
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Default"
    )

    fun saveTheme(theme: String) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsDataStore.THEME_KEY] = theme // Gunakan key dari Companion
            }
        }
    }
    // --- AKHIR PERUBAHAN ---

    // Jika Anda punya state/fungsi lain di sini yang menggunakan
    // SettingsDataStore, ubah juga untuk menggunakan `dataStore` langsung
    // Contoh:
    /*
    val isDarkMode = dataStore.data.map { preferences ->
        preferences[SettingsDataStore.DARK_MODE_KEY] ?: false
    }.stateIn(...)

    fun saveDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SettingsDataStore.DARK_MODE_KEY] = isDark
            }
        }
    }
    */
}