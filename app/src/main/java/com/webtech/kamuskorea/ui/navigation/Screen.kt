package com.webtech.kamuskorea.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Dictionary : Screen("dictionary_screen")
    object Quiz : Screen("quiz_screen")
    object Memorization : Screen("memorization_screen")
    object Ebook : Screen("ebook_screen")
    object Profile : Screen("profile_screen")
    object Settings : Screen("settings_screen")
}