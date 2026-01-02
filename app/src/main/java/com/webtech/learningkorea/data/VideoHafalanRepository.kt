package com.webtech.learningkorea.data

import android.util.Log
import com.webtech.learningkorea.data.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoHafalanRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val _videos = MutableStateFlow<List<VideoHafalan>>(emptyList())
    val videos: StateFlow<List<VideoHafalan>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun fetchVideos() {
        _isLoading.value = true
        _error.value = null

        try {
            val response = apiService.getVideoHafalan()
            if (response.isSuccessful && response.body() != null) {
                val apiVideos = response.body()!!
                _videos.value = apiVideos.map { apiVideo ->
                    VideoHafalan(
                        id = apiVideo.id.toString(),
                        title = apiVideo.title,
                        description = apiVideo.description,
                        videoUrl = apiVideo.videoUrl,
                        thumbnailUrl = apiVideo.thumbnailUrl,
                        durationMinutes = apiVideo.durationMinutes,
                        category = apiVideo.category,
                        order = apiVideo.order,
                        isPremium = apiVideo.isPremium
                    )
                }
                Log.d("VideoHafalanRepo", "✅ Fetched ${_videos.value.size} videos")
            } else {
                val errorMsg = "Error: ${response.code()} ${response.message()}"
                _error.value = errorMsg
                Log.e("VideoHafalanRepo", errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Network error: ${e.message}"
            _error.value = errorMsg
            Log.e("VideoHafalanRepo", errorMsg, e)
        } finally {
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
