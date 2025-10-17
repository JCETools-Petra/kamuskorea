package com.webtech.kamuskorea.ui.screens.settings

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
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

private val Application.dataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val THEME_KEY = stringPreferencesKey("theme_preference")
    private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")

    val currentTheme = getApplication<Application>().dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "Default"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Default"
        )

    val notificationsEnabled = getApplication<Application>().dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true // Aktif secara default
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun changeTheme(themeName: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                preferences[THEME_KEY] = themeName
            }
        }
    }

    fun setNotificationsEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                preferences[NOTIFICATIONS_KEY] = isEnabled
            }
        }
    }

    // TODO: Tambahkan fungsi untuk menghapus riwayat pencarian
    // fun clearSearchHistory() { ... }

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