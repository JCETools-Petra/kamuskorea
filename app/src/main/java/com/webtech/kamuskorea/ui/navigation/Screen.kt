package com.webtech.kamuskorea.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home_screen")
    object Dictionary : Screen("dictionary_screen")
    object Memorization : Screen("memorization_screen")
    object Quiz : Screen("quiz_screen")
    object Ebook : Screen("ebook_screen")
    object Profile : Screen("profile_screen")
    object Settings : Screen("settings_screen")

    // GANTI BARIS INI:
    // object PdfViewer : Screen("pdf_viewer_screen/{filePath}") // SALAH
    // DENGAN INI:
    object PdfViewer : Screen("pdf_viewer_screen") // BENAR
}