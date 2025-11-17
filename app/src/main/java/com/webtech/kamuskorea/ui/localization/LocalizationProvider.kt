package com.webtech.kamuskorea.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.*

data class AppStrings(
    // Common
    val appName: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val edit: String,
    val close: String,
    val ok: String,

    // Navigation
    val home: String,
    val dictionary: String,
    val favorites: String,
    val ebook: String,
    val memorization: String,
    val quiz: String,
    val settings: String,
    val profile: String,

    // Auth
    val login: String,
    val register: String,
    val logout: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val name: String,

    // Dictionary
    val searchPlaceholder: String,
    val noResults: String,

    // Settings
    val appearance: String,
    val theme: String,
    val textSize: String,
    val language: String,
    val notifications: String,
    val dailyReminder: String,
    val setTime: String,
    val dataStorage: String,
    val offlineDownload: String,
    val clearCache: String,
    val cacheSize: String,
    val backup: String,
    val about: String,
    val version: String,
    val privacyPolicy: String,
    val termsConditions: String,
    val rateApp: String,

    // Notifications
    val notificationActive: String,
    val notificationInactive: String,

    // Sizes
    val small: String,
    val medium: String,
    val large: String
)

val LocalStrings = compositionLocalOf { getStrings("id") }

fun getStrings(languageCode: String): AppStrings {
    return when (languageCode) {
        "en" -> AppStrings(
            // Common
            appName = "Korean Dictionary",
            save = "Save",
            cancel = "Cancel",
            delete = "Delete",
            edit = "Edit",
            close = "Close",
            ok = "OK",

            // Navigation
            home = "Home",
            dictionary = "Dictionary",
            favorites = "Favorites",
            ebook = "E-Book",
            memorization = "Memorization",
            quiz = "Quiz",
            settings = "Settings",
            profile = "Profile",

            // Auth
            login = "Login",
            register = "Register",
            logout = "Logout",
            email = "Email",
            password = "Password",
            confirmPassword = "Confirm Password",
            name = "Name",

            // Dictionary
            searchPlaceholder = "Search words...",
            noResults = "No results found",

            // Settings
            appearance = "Appearance",
            theme = "App Theme",
            textSize = "Text Size",
            language = "Interface Language",
            notifications = "Notifications",
            dailyReminder = "Daily Reminder",
            setTime = "Set Time",
            dataStorage = "Data & Storage",
            offlineDownload = "Offline Download",
            clearCache = "Clear Cache",
            cacheSize = "Cache size",
            backup = "Backup & Restore",
            about = "About",
            version = "Version",
            privacyPolicy = "Privacy Policy",
            termsConditions = "Terms & Conditions",
            rateApp = "Rate App",

            // Notifications
            notificationActive = "Active",
            notificationInactive = "Inactive",

            // Sizes
            small = "Small",
            medium = "Medium",
            large = "Large"
        )

        else -> AppStrings( // Default: Indonesian
            // Common
            appName = "Kamus Korea",
            save = "Simpan",
            cancel = "Batal",
            delete = "Hapus",
            edit = "Edit",
            close = "Tutup",
            ok = "OK",

            // Navigation
            home = "Beranda",
            dictionary = "Kamus",
            favorites = "Favorit",
            ebook = "E-Book",
            memorization = "Hafalan",
            quiz = "Latihan",
            settings = "Pengaturan",
            profile = "Profil",

            // Auth
            login = "Masuk",
            register = "Daftar",
            logout = "Keluar",
            email = "Email",
            password = "Kata Sandi",
            confirmPassword = "Konfirmasi Kata Sandi",
            name = "Nama",

            // Dictionary
            searchPlaceholder = "Cari kata...",
            noResults = "Tidak ada hasil",

            // Settings
            appearance = "Tampilan",
            theme = "Tema Aplikasi",
            textSize = "Ukuran Teks",
            language = "Bahasa Interface",
            notifications = "Notifikasi",
            dailyReminder = "Pengingat Harian",
            setTime = "Atur Waktu",
            dataStorage = "Data & Penyimpanan",
            offlineDownload = "Download Konten Offline",
            clearCache = "Hapus Cache",
            cacheSize = "Ukuran cache",
            backup = "Backup & Restore",
            about = "Tentang",
            version = "Versi",
            privacyPolicy = "Kebijakan Privasi",
            termsConditions = "Syarat & Ketentuan",
            rateApp = "Beri Rating",

            // Notifications
            notificationActive = "Aktif",
            notificationInactive = "Nonaktif",

            // Sizes
            small = "Kecil",
            medium = "Sedang",
            large = "Besar"
        )
    }
}

@Composable
fun LocalizationProvider(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val strings = remember(languageCode) {
        getStrings(languageCode)
    }

    CompositionLocalProvider(
        LocalStrings provides strings,
        content = content
    )
}