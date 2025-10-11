package com.webtech.kamuskorea.ui.navigation

sealed class Screen(val route: String) {
    // Halaman utama
    object Dictionary : Screen("dictionary_screen")
    object Ebook : Screen("ebook_screen")
    object Quiz : Screen("quiz_screen")
    object Profile : Screen("profile_screen")
    // Halaman otentikasi (TAMBAHKAN INI)
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")

    object Memorization : Screen("memorization_screen")
}