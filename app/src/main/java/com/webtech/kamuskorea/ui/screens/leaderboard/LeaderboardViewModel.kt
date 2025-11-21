package com.webtech.kamuskorea.ui.screens.leaderboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.gamification.GamificationRepository
import com.webtech.kamuskorea.gamification.LeaderboardEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val currentUserEntry: LeaderboardEntry? = null,
    val error: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val TAG = "LeaderboardViewModel"

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    // Observe gamification state for current user stats
    val gamificationState = gamificationRepository.gamificationState

    init {
        // Sync XP to server first, then load leaderboard
        syncAndLoadLeaderboard()
    }

    private fun syncAndLoadLeaderboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Step 1: Sync local XP to server
                Log.d(TAG, "🔄 Syncing local XP to server before loading leaderboard...")
                gamificationRepository.syncToServer()
                Log.d(TAG, "✅ XP synced successfully")

                // Step 2: Load leaderboard from server
                loadLeaderboard()
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Sync failed, loading leaderboard anyway", e)
                // Even if sync fails, still load leaderboard
                loadLeaderboard()
            }
        }
    }

    fun loadLeaderboard(limit: Int = 100) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Log.d(TAG, "📊 Fetching leaderboard (limit: $limit)...")
                val result = gamificationRepository.getLeaderboard(limit)

                result.fold(
                    onSuccess = { leaderboard ->
                        Log.d(TAG, "✅ Leaderboard loaded: ${leaderboard.size} entries")

                        // Find current user in leaderboard
                        val currentUserEntry = leaderboard.find {
                            it.userId == gamificationRepository.getCurrentUserId()
                        }

                        if (currentUserEntry != null) {
                            Log.d(TAG, "👤 Current user found in leaderboard: Rank #${currentUserEntry.rank}, XP ${currentUserEntry.totalXp}")
                        } else {
                            Log.w(TAG, "⚠️ Current user NOT found in leaderboard")
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            leaderboard = leaderboard,
                            currentUserEntry = currentUserEntry
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Failed to load leaderboard", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Gagal memuat leaderboard: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception loading leaderboard", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        syncAndLoadLeaderboard()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
