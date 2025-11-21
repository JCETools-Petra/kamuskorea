package com.webtech.kamuskorea.gamification

/**
 * Daily Quest models and definitions
 */

enum class QuestType {
    SEARCH_WORDS,      // Search X words in dictionary
    COMPLETE_QUIZ,     // Complete X quizzes
    SAVE_FAVORITES,    // Save X words to favorites
    STUDY_STREAK,      // Maintain study streak
    EARN_XP            // Earn X XP today
}

data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val targetProgress: Int,
    val xpReward: Int,
    val iconRes: Int = android.R.drawable.ic_menu_agenda
)

data class DailyQuestProgress(
    val questId: String,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

data class DailyQuestState(
    val date: String, // Format: "yyyy-MM-dd"
    val quests: List<DailyQuest>,
    val progress: Map<String, DailyQuestProgress>,
    val allCompleted: Boolean = false
) {
    fun getQuestProgress(questId: String): DailyQuestProgress {
        return progress[questId] ?: DailyQuestProgress(questId)
    }

    fun isQuestCompleted(questId: String): Boolean {
        return progress[questId]?.isCompleted ?: false
    }

    fun getTotalQuestsCompleted(): Int {
        return progress.values.count { it.isCompleted }
    }

    fun getTotalXpEarned(): Int {
        return quests.filter { isQuestCompleted(it.id) }.sumOf { it.xpReward }
    }
}

/**
 * Predefined daily quests
 */
object DailyQuests {

    val SEARCH_3_WORDS = DailyQuest(
        id = "daily_search_3",
        title = "Jelajahi Kamus",
        description = "Cari 3 kata di kamus",
        type = QuestType.SEARCH_WORDS,
        targetProgress = 3,
        xpReward = 20,
        iconRes = android.R.drawable.ic_menu_search
    )

    val COMPLETE_1_QUIZ = DailyQuest(
        id = "daily_quiz_1",
        title = "Uji Pemahaman",
        description = "Selesaikan 1 quiz",
        type = QuestType.COMPLETE_QUIZ,
        targetProgress = 1,
        xpReward = 30,
        iconRes = android.R.drawable.ic_menu_help
    )

    val SAVE_2_FAVORITES = DailyQuest(
        id = "daily_favorite_2",
        title = "Kumpulkan Kosa Kata",
        description = "Simpan 2 kata ke favorit",
        type = QuestType.SAVE_FAVORITES,
        targetProgress = 2,
        xpReward = 15,
        iconRes = android.R.drawable.ic_menu_save
    )

    val MAINTAIN_STREAK = DailyQuest(
        id = "daily_streak",
        title = "Konsisten Belajar",
        description = "Buka aplikasi hari ini",
        type = QuestType.STUDY_STREAK,
        targetProgress = 1,
        xpReward = 10,
        iconRes = android.R.drawable.ic_menu_day
    )

    /**
     * Get default daily quests (rotates based on day of week)
     */
    fun getDailyQuests(dayOfWeek: Int): List<DailyQuest> {
        // Return 3 quests per day
        return when (dayOfWeek) {
            1 -> listOf(MAINTAIN_STREAK, SEARCH_3_WORDS, COMPLETE_1_QUIZ) // Monday
            2 -> listOf(MAINTAIN_STREAK, SAVE_2_FAVORITES, SEARCH_3_WORDS) // Tuesday
            3 -> listOf(MAINTAIN_STREAK, COMPLETE_1_QUIZ, SAVE_2_FAVORITES) // Wednesday
            4 -> listOf(MAINTAIN_STREAK, SEARCH_3_WORDS, COMPLETE_1_QUIZ) // Thursday
            5 -> listOf(MAINTAIN_STREAK, SAVE_2_FAVORITES, SEARCH_3_WORDS) // Friday
            6 -> listOf(MAINTAIN_STREAK, COMPLETE_1_QUIZ, SAVE_2_FAVORITES) // Saturday
            else -> listOf(MAINTAIN_STREAK, SEARCH_3_WORDS, COMPLETE_1_QUIZ) // Sunday
        }
    }
}
