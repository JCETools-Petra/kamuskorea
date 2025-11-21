package com.webtech.kamuskorea.gamification

import com.google.gson.annotations.SerializedName

/**
 * API Request/Response models for Gamification endpoints
 */

// ========== REQUESTS ==========

data class SyncXpRequest(
    @SerializedName("total_xp") val totalXp: Int,
    @SerializedName("current_level") val currentLevel: Int,
    @SerializedName("achievements_unlocked") val achievementsUnlocked: List<String>,
    @SerializedName("username") val username: String? = null
)

data class AddXpRequest(
    @SerializedName("xp_amount") val xpAmount: Int,
    @SerializedName("source") val source: String,
    @SerializedName("metadata") val metadata: Map<String, Any>? = null
)

// ========== RESPONSES ==========

data class SyncXpResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("leaderboard_rank") val leaderboardRank: Int
)

data class LeaderboardResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<LeaderboardEntry>
)

data class LeaderboardEntry(
    @SerializedName("rank") val rank: Int,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("total_xp") val totalXp: Int,
    @SerializedName("level") val level: Int,
    @SerializedName("achievement_count") val achievementCount: Int
)

data class UserRankResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("rank") val rank: Int,
    @SerializedName("total_users") val totalUsers: Int,
    @SerializedName("percentile") val percentile: Double,
    @SerializedName("username") val username: String,
    @SerializedName("total_xp") val totalXp: Int,
    @SerializedName("level") val level: Int,
    @SerializedName("achievements_unlocked") val achievementsUnlocked: List<String>,
    @SerializedName("achievement_count") val achievementCount: Int
)

data class AddXpResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("new_total_xp") val newTotalXp: Int,
    @SerializedName("new_level") val newLevel: Int,
    @SerializedName("level_up") val levelUp: Boolean,
    @SerializedName("xp_earned") val xpEarned: Int
)

// ========== LOCAL MODELS ==========

/**
 * Local gamification state
 */
data class GamificationState(
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val achievementsUnlocked: Set<String> = emptySet(),
    val leaderboardRank: Int = 0,
    val lastSyncTimestamp: Long = 0L
) {
    fun hasAchievement(achievementId: String): Boolean {
        return achievementsUnlocked.contains(achievementId)
    }

    fun unlockAchievement(achievementId: String): GamificationState {
        return copy(achievementsUnlocked = achievementsUnlocked + achievementId)
    }

    fun addXp(amount: Int): GamificationState {
        val newTotalXp = totalXp + amount
        val newLevel = LevelSystem.calculateLevel(newTotalXp)
        return copy(
            totalXp = newTotalXp,
            currentLevel = newLevel
        )
    }
}

/**
 * Events emitted by the gamification system
 */
sealed class GamificationEvent {
    data class XpEarned(val amount: Int, val source: String, val totalXp: Int) : GamificationEvent()
    data class LevelUp(val newLevel: Int, val totalXp: Int) : GamificationEvent()
    data class AchievementUnlocked(val achievement: Achievement) : GamificationEvent()
}
