package com.webtech.kamuskorea.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
)
val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
)

// Definisikan skema warna untuk tema tambahan
val ForestLightColorScheme = lightColorScheme(
    primary = forest_light_primary,
    onPrimary = forest_light_onPrimary,
    primaryContainer = forest_light_primaryContainer,
    onPrimaryContainer = forest_light_onPrimaryContainer,
)
val ForestDarkColorScheme = darkColorScheme(
    primary = forest_dark_primary,
    onPrimary = forest_dark_onPrimary,
    primaryContainer = forest_dark_primaryContainer,
    onPrimaryContainer = forest_dark_onPrimaryContainer,
)
val OceanLightColorScheme = lightColorScheme(
    primary = ocean_light_primary,
    onPrimary = ocean_light_onPrimary,
    primaryContainer = ocean_light_primaryContainer,
    onPrimaryContainer = ocean_light_onPrimaryContainer,
)
val OceanDarkColorScheme = darkColorScheme(
    primary = ocean_dark_primary,
    onPrimary = ocean_dark_onPrimary,
    primaryContainer = ocean_dark_primaryContainer,
    onPrimaryContainer = ocean_dark_onPrimaryContainer,
)

@Composable
fun KamusKoreaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    // TAMBAHKAN PARAMETER INI UNTUK MENERIMA "CAT" DARI LUAR
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    // Tentukan skema warna yang akan digunakan
    val finalColorScheme = when {
        // Jika "cat" diberikan dari luar, langsung gunakan itu
        colorScheme != null -> colorScheme

        // Jika tidak, gunakan logika lama (warna dinamis atau default)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}