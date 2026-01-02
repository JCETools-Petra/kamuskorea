package com.webtech.learningkorea.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object ForgotPassword : Screen("forgot_password_screen") // ✅ NEW
    object ResetPassword : Screen("reset_password_screen/{token}") { // ✅ NEW
        fun createRoute(token: String) = "reset_password_screen/$token"
    }
    object Home : Screen("home_screen")
    object Dictionary : Screen("dictionary_screen")
    object Favorites : Screen("favorites_screen")
    object Quiz : Screen("quiz_screen")
    object Memorization : Screen("memorization_screen")
    object QuizHafalan : Screen("quiz_hafalan_screen") // ✅ NEW - Quiz Hafalan
    object VideoHafalan : Screen("video_hafalan_screen") // ✅ NEW - Video Hafalan
    object VideoPlayer : Screen("video_player/{videoUrl}/{videoTitle}/{videoDescription}") {
        fun createRoute(videoUrl: String, videoTitle: String, videoDescription: String?): String {
            return "video_player/$videoUrl/$videoTitle/${videoDescription ?: ""}"
        }
    }
    object Ebook : Screen("ebook_screen")
    object Profile : Screen("profile_screen")
    object Settings : Screen("settings_screen")
    object PdfViewer : Screen("pdf_viewer_screen")
    object PremiumLock : Screen("premium_lock")
    object Onboarding : Screen("onboarding_screen")
    object Leaderboard : Screen("leaderboard")
    object Achievements : Screen("achievements")
}