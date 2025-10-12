package com.webtech.kamuskorea.ui.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

// Pastikan impor ini ada
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
    }

    val getTheme = dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "Default"
    }

    suspend fun saveTheme(theme: String) {
        dataStore.edit { settings ->
            settings[THEME_KEY] = theme
        }
    }
}