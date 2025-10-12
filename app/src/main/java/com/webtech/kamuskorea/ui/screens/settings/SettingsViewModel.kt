package com.webtech.kamuskorea.ui.screens.settings

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Buat DataStore di level top-level
private val Application.dataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Kunci untuk menyimpan nama tema
    private val THEME_KEY = stringPreferencesKey("theme_preference")

    // StateFlow untuk "membaca" tema yang tersimpan
    val currentTheme = getApplication<Application>().dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "Default" // Nilai default adalah "Default"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Default"
        )

    // Fungsi yang akan dipanggil dari UI untuk "menulis" / mengubah tema
    fun changeTheme(themeName: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                preferences[THEME_KEY] = themeName
            }
        }
    }

    // Factory untuk membuat ViewModel (tidak berubah)
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        return SettingsViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}