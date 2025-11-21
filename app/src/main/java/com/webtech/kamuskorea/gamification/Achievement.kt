package com.webtech.kamuskorea.gamification

import androidx.annotation.DrawableRes
import com.webtech.kamuskorea.R

/**
 * Achievement Definition
 *
 * Defines all achievements available in the app
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    val xpReward: Int,
    val category: AchievementCategory
)

enum class AchievementCategory {
    LEARNING,
    STREAK,
    QUIZ,
    VOCABULARY,
    SOCIAL
}

/**
 * All available achievements
 */
object Achievements {

    // ========== LEARNING ACHIEVEMENTS ==========

    val FIRST_PDF = Achievement(
        id = "first_pdf",
        title = "Pembaca Pertama",
        description = "Buka materi PDF pertama kamu",
        iconRes = R.drawable.ic_book,
        xpReward = 10,
        category = AchievementCategory.LEARNING
    )

    val PDF_EXPLORER = Achievement(
        id = "pdf_explorer",
        title = "Penjelajah Materi",
        description = "Buka 10 materi PDF berbeda",
        iconRes = R.drawable.ic_book,
        xpReward = 50,
        category = AchievementCategory.LEARNING
    )

    // ========== STREAK ACHIEVEMENTS ==========

    val STREAK_7_DAYS = Achievement(
        id = "7_day_streak",
        title = "Konsisten 7 Hari",
        description = "Belajar 7 hari berturut-turut",
        iconRes = R.drawable.ic_fire,
        xpReward = 100,
        category = AchievementCategory.STREAK
    )

    val STREAK_30_DAYS = Achievement(
        id = "30_day_streak",
        title = "Master Konsistensi",
        description = "Belajar 30 hari berturut-turut",
        iconRes = R.drawable.ic_fire,
        xpReward = 500,
        category = AchievementCategory.STREAK
    )

    val STREAK_100_DAYS = Achievement(
        id = "100_day_streak",
        title = "Legenda Konsistensi",
        description = "Belajar 100 hari berturut-turut",
        iconRes = R.drawable.ic_fire,
        xpReward = 2000,
        category = AchievementCategory.STREAK
    )

    // ========== QUIZ ACHIEVEMENTS ==========

    val FIRST_QUIZ = Achievement(
        id = "first_quiz",
        title = "Pemula Kuis",
        description = "Selesaikan kuis pertama kamu",
        iconRes = R.drawable.ic_quiz,
        xpReward = 20,
        category = AchievementCategory.QUIZ
    )

    val QUIZ_MASTER_10 = Achievement(
        id = "quiz_master_10",
        title = "Ahli Kuis",
        description = "Selesaikan 10 kuis",
        iconRes = R.drawable.ic_quiz,
        xpReward = 100,
        category = AchievementCategory.QUIZ
    )

    val QUIZ_MASTER_50 = Achievement(
        id = "quiz_master_50",
        title = "Master Kuis",
        description = "Selesaikan 50 kuis",
        iconRes = R.drawable.ic_quiz,
        xpReward = 500,
        category = AchievementCategory.QUIZ
    )

    // ========== VOCABULARY ACHIEVEMENTS ==========

    val VOCAB_COLLECTOR_10 = Achievement(
        id = "vocab_10",
        title = "Kolektor Kata",
        description = "Simpan 10 kata favorit",
        iconRes = R.drawable.ic_favorite,
        xpReward = 30,
        category = AchievementCategory.VOCABULARY
    )

    val VOCAB_COLLECTOR_100 = Achievement(
        id = "vocab_100",
        title = "Master Kosakata",
        description = "Simpan 100 kata favorit",
        iconRes = R.drawable.ic_favorite,
        xpReward = 200,
        category = AchievementCategory.VOCABULARY
    )

    // ========== ALL ACHIEVEMENTS LIST ==========

    val ALL = listOf(
        FIRST_PDF,
        PDF_EXPLORER,
        STREAK_7_DAYS,
        STREAK_30_DAYS,
        STREAK_100_DAYS,
        FIRST_QUIZ,
        QUIZ_MASTER_10,
        QUIZ_MASTER_50,
        VOCAB_COLLECTOR_10,
        VOCAB_COLLECTOR_100
    )

    /**
     * Get achievement by ID
     */
    fun getById(id: String): Achievement? {
        return ALL.find { it.id == id }
    }

    /**
     * Get all achievements by category
     */
    fun getByCategory(category: AchievementCategory): List<Achievement> {
        return ALL.filter { it.category == category }
    }
}

/**
 * XP Rewards for various actions
 */
object XpRewards {
    const val QUIZ_COMPLETED = 50
    const val PDF_OPENED = 10
    const val WORD_FAVORITED = 5
    const val FLASHCARD_FLIPPED = 3
    const val DAILY_LOGIN = 20
    const val CHAPTER_COMPLETED = 30
}

/**
 * Level calculation
 * Level up setiap 100 XP
 */
object LevelSystem {
    const val XP_PER_LEVEL = 100

    fun calculateLevel(totalXp: Int): Int {
        return (totalXp / XP_PER_LEVEL) + 1
    }

    fun getXpForNextLevel(totalXp: Int): Int {
        val currentLevel = calculateLevel(totalXp)
        val xpForNextLevel = currentLevel * XP_PER_LEVEL
        return xpForNextLevel - totalXp
    }

    fun getProgressToNextLevel(totalXp: Int): Float {
        val currentLevel = calculateLevel(totalXp)
        val xpInCurrentLevel = totalXp - ((currentLevel - 1) * XP_PER_LEVEL)
        return xpInCurrentLevel.toFloat() / XP_PER_LEVEL
    }
}
