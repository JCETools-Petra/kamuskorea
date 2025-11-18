package com.webtech.kamuskorea.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.webtech.kamuskorea.R
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(
    onTimeout: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Fade in animation for the splash image
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        )
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(3000) // Show splash for 3 seconds
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Full screen splash image with fade in effect
        // TODO: Replace R.drawable.ic_splash_logo with R.drawable.splash_screen
        // after adding splash_screen.png to app/src/main/res/drawable/
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "Splash Screen",
            modifier = Modifier
                .size(200.dp)
                .alpha(alphaAnim.value),
            contentScale = ContentScale.Fit
        )
    }
}