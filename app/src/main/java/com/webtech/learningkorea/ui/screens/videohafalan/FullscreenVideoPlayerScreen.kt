package com.webtech.learningkorea.ui.screens.videohafalan

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.webtech.learningkorea.MainActivity

@Composable
fun FullscreenVideoPlayerScreen(
    videoUrl: String,
    videoTitle: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Enable fullscreen immersive mode
    DisposableEffect(Unit) {
        (activity as? MainActivity)?.enableFullscreenMode()

        // Lock to landscape for better video viewing
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            (activity as? MainActivity)?.disableFullscreenMode()
            // Restore to sensor orientation when leaving
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Handle back button press
    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer
        AndroidView(
            factory = { context ->
                StyledPlayerView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Configure player view
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = true
                    controllerAutoShow = true
                    controllerHideOnTouch = true
                    controllerShowTimeoutMs = 3000

                    // Create ExoPlayer
                    val exoPlayer = ExoPlayer.Builder(context).build().apply {
                        // Parse video URL and create MediaItem
                        val mediaItem = when {
                            // YouTube URL - convert to direct stream (if possible)
                            // Note: For production, you might need youtube-dl or similar service
                            videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be") -> {
                                // For YouTube, we'll still use the URL directly
                                // In production, you'd need a YouTube data extraction service
                                MediaItem.fromUri(videoUrl)
                            }
                            // Direct video URL (.mp4, .webm, etc.)
                            else -> MediaItem.fromUri(videoUrl)
                        }

                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }

                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { playerView ->
                playerView.player?.release()
            }
        )
    }
}
