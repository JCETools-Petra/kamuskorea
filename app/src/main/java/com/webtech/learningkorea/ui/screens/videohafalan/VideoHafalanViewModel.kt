package com.webtech.learningkorea.ui.screens.videohafalan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.learningkorea.data.VideoHafalan
import com.webtech.learningkorea.data.VideoHafalanRepository
import com.webtech.learningkorea.analytics.AnalyticsTracker
import com.webtech.learningkorea.gamification.GamificationRepository
import com.webtech.learningkorea.gamification.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoHafalanViewModel @Inject constructor(
    private val repository: VideoHafalanRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    val videos: StateFlow<List<VideoHafalan>> = repository.videos
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error

    init {
        fetchVideos()
    }

    fun fetchVideos() {
        viewModelScope.launch {
            repository.fetchVideos()
        }
    }

    /**
     * Track when a video is opened and award XP
     * Call this from UI when user opens a video
     */
    fun onVideoOpened(videoTitle: String, isPremium: Boolean) {
        viewModelScope.launch {
            // Track analytics (using similar pattern as PDF tracking)
            analyticsTracker.logPdfOpened(videoTitle, isPremium) // Reusing PDF tracking for videos

            // Award XP (same as PDF for now)
            gamificationRepository.addXp(XpRewards.PDF_OPENED, "video_hafalan_opened")
            Log.d("VideoHafalanVM", "⭐ Awarded ${XpRewards.PDF_OPENED} XP for opening video: $videoTitle")
        }
    }

    fun clearError() {
        repository.clearError()
    }
}
