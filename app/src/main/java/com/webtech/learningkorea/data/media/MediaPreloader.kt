package com.webtech.learningkorea.data.media

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.webtech.learningkorea.data.assessment.Question
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media Preloader Service
 * Handles preloading and caching of images, audio, and video content for quiz/exam questions
 * to provide smooth user experience without loading delays.
 */
@Singleton
class MediaPreloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "MediaPreloader"
        private const val CACHE_DIR = "media_cache"
        private const val PRELOAD_AHEAD_COUNT = 3 // Preload next 3 questions
        private const val MAX_CACHE_SIZE_MB = 500 // Max cache size in MB
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Track which media URLs have been cached
    private val cachedMedia = ConcurrentHashMap<String, CacheStatus>()

    // Loading state for UI feedback
    private val _loadingStates = MutableStateFlow<Map<String, LoadingState>>(emptyMap())
    val loadingStates: StateFlow<Map<String, LoadingState>> = _loadingStates.asStateFlow()

    // Cache directory for audio/video
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    enum class CacheStatus {
        NOT_CACHED,
        CACHING,
        CACHED,
        ERROR
    }

    enum class LoadingState {
        IDLE,
        LOADING,
        READY,
        ERROR
    }

    /**
     * Preload media for a list of questions starting from current index
     */
    fun preloadQuestions(questions: List<Question>, currentIndex: Int) {
        if (questions.isEmpty()) return

        scope.launch {
            // Preload current question immediately
            if (currentIndex in questions.indices) {
                preloadQuestion(questions[currentIndex])
            }

            // Preload next questions in background
            val endIndex = minOf(currentIndex + PRELOAD_AHEAD_COUNT, questions.size)
            for (i in (currentIndex + 1) until endIndex) {
                preloadQuestion(questions[i])
            }

            Log.d(TAG, "📦 Preloading questions $currentIndex to ${endIndex - 1}")
        }
    }

    /**
     * Preload media for a single question
     */
    private suspend fun preloadQuestion(question: Question) {
        val mediaUrl = question.mediaUrl ?: return

        if (cachedMedia[mediaUrl] == CacheStatus.CACHED ||
            cachedMedia[mediaUrl] == CacheStatus.CACHING) {
            return // Already cached or caching
        }

        updateLoadingState(mediaUrl, LoadingState.LOADING)
        cachedMedia[mediaUrl] = CacheStatus.CACHING

        try {
            when (question.questionType) {
                "image" -> preloadImage(mediaUrl)
                "audio" -> preloadAudioVideo(mediaUrl, "audio")
                "video" -> preloadAudioVideo(mediaUrl, "video")
            }
            cachedMedia[mediaUrl] = CacheStatus.CACHED
            updateLoadingState(mediaUrl, LoadingState.READY)
            Log.d(TAG, "✅ Cached: $mediaUrl")
        } catch (e: Exception) {
            cachedMedia[mediaUrl] = CacheStatus.ERROR
            updateLoadingState(mediaUrl, LoadingState.ERROR)
            Log.e(TAG, "❌ Failed to cache: $mediaUrl", e)
        }
    }

    /**
     * Preload image using Coil (with automatic disk/memory caching)
     */
    private suspend fun preloadImage(url: String) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()

        imageLoader.execute(request)
    }

    /**
     * Preload audio/video by downloading to local cache
     */
    private suspend fun preloadAudioVideo(url: String, type: String) = withContext(Dispatchers.IO) {
        val fileName = "${type}_${url.hashCode()}.cache"
        val cacheFile = File(cacheDir, fileName)

        if (cacheFile.exists()) {
            Log.d(TAG, "📁 Cache hit: $fileName")
            return@withContext
        }

        // Check cache size before downloading
        cleanCacheIfNeeded()

        val request = Request.Builder()
            .url(url)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download: ${response.code}")
            }

            response.body?.let { body ->
                FileOutputStream(cacheFile).use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }

        Log.d(TAG, "💾 Downloaded $type: $fileName (${cacheFile.length() / 1024} KB)")
    }

    /**
     * Get cached file path for audio/video
     */
    fun getCachedFilePath(url: String, type: String): String? {
        val fileName = "${type}_${url.hashCode()}.cache"
        val cacheFile = File(cacheDir, fileName)
        return if (cacheFile.exists()) cacheFile.absolutePath else null
    }

    /**
     * Check if media is already cached
     */
    fun isCached(url: String): Boolean {
        return cachedMedia[url] == CacheStatus.CACHED
    }

    /**
     * Get loading state for a specific URL
     */
    fun getLoadingState(url: String): LoadingState {
        return _loadingStates.value[url] ?: LoadingState.IDLE
    }

    private fun updateLoadingState(url: String, state: LoadingState) {
        _loadingStates.value = _loadingStates.value.toMutableMap().apply {
            put(url, state)
        }
    }

    /**
     * Clean cache if it exceeds maximum size
     */
    private fun cleanCacheIfNeeded() {
        val maxSizeBytes = MAX_CACHE_SIZE_MB * 1024 * 1024L
        var totalSize = 0L
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return

        files.forEach { totalSize += it.length() }

        if (totalSize > maxSizeBytes) {
            Log.d(TAG, "🧹 Cache cleanup: ${totalSize / 1024 / 1024}MB > ${MAX_CACHE_SIZE_MB}MB")

            // Remove oldest files until under limit
            for (file in files) {
                if (totalSize <= maxSizeBytes * 0.8) break // Clean to 80% capacity
                totalSize -= file.length()
                file.delete()
                Log.d(TAG, "🗑️ Deleted: ${file.name}")
            }
        }
    }

    /**
     * Clear all cached media
     */
    fun clearCache() {
        scope.launch {
            cacheDir.listFiles()?.forEach { it.delete() }
            cachedMedia.clear()
            _loadingStates.value = emptyMap()
            Log.d(TAG, "🗑️ Cache cleared")
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val files = cacheDir.listFiles() ?: emptyArray()
        val totalSize = files.sumOf { it.length() }
        return CacheStats(
            fileCount = files.size,
            totalSizeBytes = totalSize,
            totalSizeMB = totalSize / 1024 / 1024
        )
    }

    /**
     * Cancel all preloading operations
     */
    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
    }

    data class CacheStats(
        val fileCount: Int,
        val totalSizeBytes: Long,
        val totalSizeMB: Long
    )
}
