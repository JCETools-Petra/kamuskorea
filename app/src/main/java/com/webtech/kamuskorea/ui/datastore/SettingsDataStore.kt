package com.webtech.kamuskorea.ui.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Definisikan delegate di top-level (tanpa 'private')
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Kelas ini sekarang hanya sebagai pemegang Key dan helper (tidak di-inject)
class SettingsDataStore { // Hapus (context: Context) jika tidak perlu instance

    companion object {
        // Definisikan semua key Anda di sini
        val THEME_KEY = stringPreferencesKey("theme")
        // Tambahkan key lain yang mungkin Anda butuhkan dari kode asli
        // val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        // val KOREAN_TEXT_SIZE_KEY = intPreferencesKey("korean_text_size")
        // val ROMANIZATION_TEXT_SIZE_KEY = intPreferencesKey("romanization_text_size")
        // val INDONESIAN_TEXT_SIZE_KEY = intPreferencesKey("indonesian_text_size")
        val KAMUS_DB_VERSION_KEY = intPreferencesKey("kamus_db_version")
        // val NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("notification_enabled") // Contoh jika perlu
    }

    // Fungsi helper bisa dipindahkan ke ViewModel atau Repository
    // yang menginject DataStore<Preferences> langsung.
    // Atau bisa dibuat sebagai extension function jika lebih suka.
    /* Contoh extension function:
    fun DataStore<Preferences>.getThemeFlow(): Flow<String> = this.data.map { preferences ->
        preferences[SettingsDataStore.THEME_KEY] ?: "Default"
    }

    suspend fun DataStore<Preferences>.saveTheme(theme: String) {
        this.edit { settings ->
            settings[SettingsDataStore.THEME_KEY] = theme
        }
    }
    */
}