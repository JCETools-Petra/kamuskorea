package com.webtech.kamuskorea.ui.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore {
    companion object {
        // ========== APPEARANCE SETTINGS ==========
        val THEME_KEY = stringPreferencesKey("theme")
        val DARK_MODE_KEY = stringPreferencesKey("dark_mode") // "system", "light", "dark"
        val TEXT_SCALE_KEY = stringPreferencesKey("text_scale")
        val LANGUAGE_KEY = stringPreferencesKey("language")

        // ========== NOTIFICATION SETTINGS ==========
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")

        // ========== DATA & STORAGE ==========
        val OFFLINE_DOWNLOAD_KEY = booleanPreferencesKey("offline_download")

        // ========== DATABASE VERSION ==========
        val KAMUS_DB_VERSION_KEY = intPreferencesKey("kamus_db_version")

        // ========== STREAK TRACKING ==========
        val LAST_STUDY_DATE_KEY = stringPreferencesKey("last_study_date") // Format: "yyyy-MM-dd"
        val CURRENT_STREAK_KEY = intPreferencesKey("current_streak")
        val LONGEST_STREAK_KEY = intPreferencesKey("longest_streak")

        // ========== GAMIFICATION ==========
        val USER_XP_KEY = intPreferencesKey("user_xp")
        val USER_LEVEL_KEY = intPreferencesKey("user_level")
        val ACHIEVEMENTS_UNLOCKED_KEY = stringSetPreferencesKey("achievements_unlocked") // Set<String> of achievement IDs
        val LAST_XP_SYNC_KEY = longPreferencesKey("last_xp_sync") // Timestamp in millis
        val LEADERBOARD_RANK_KEY = intPreferencesKey("leaderboard_rank")
    }
}