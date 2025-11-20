package com.webtech.kamuskorea.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Analytics Tracker for the app
 * Tracks all user interactions, screen views, and important events
 */
@Singleton
class AnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    companion object {
        private const val TAG = "AnalyticsTracker"

        // Screen Names
        const val SCREEN_HOME = "home"
        const val SCREEN_DICTIONARY = "dictionary"
        const val SCREEN_FAVORITES = "favorites"
        const val SCREEN_EBOOK = "ebook"
        const val SCREEN_QUIZ = "quiz"
        const val SCREEN_MEMORIZATION = "memorization"
        const val SCREEN_PROFILE = "profile"
        const val SCREEN_SETTINGS = "settings"
        const val SCREEN_LOGIN = "login"
        const val SCREEN_REGISTER = "register"

        // Event Names - User Actions
        const val EVENT_PDF_OPENED = "pdf_opened"
        const val EVENT_QUIZ_STARTED = "quiz_started"
        const val EVENT_QUIZ_COMPLETED = "quiz_completed"
        const val EVENT_WORD_FAVORITED = "word_favorited"
        const val EVENT_WORD_UNFAVORITED = "word_unfavorited"
        const val EVENT_SEARCH_PERFORMED = "search_performed"
        const val EVENT_FLASHCARD_FLIPPED = "flashcard_flipped"
        const val EVENT_CHAPTER_COMPLETED = "chapter_completed"

        // Event Names - Ads
        const val EVENT_AD_IMPRESSION = "ad_impression"
        const val EVENT_AD_CLICKED = "ad_clicked"
        const val EVENT_AD_FAILED = "ad_failed"
        const val EVENT_REWARDED_AD_EARNED = "rewarded_ad_earned"

        // Event Names - Premium
        const val EVENT_PREMIUM_VIEWED = "premium_screen_viewed"
        const val EVENT_PREMIUM_PURCHASED = "premium_purchased"
        const val EVENT_PREMIUM_CANCELLED = "premium_cancelled"

        // Event Names - Auth
        const val EVENT_SIGN_UP = "sign_up"
        const val EVENT_LOGIN = "login"
        const val EVENT_LOGOUT = "logout"

        // Parameter Names
        const val PARAM_PDF_TITLE = "pdf_title"
        const val PARAM_QUIZ_ID = "quiz_id"
        const val PARAM_QUIZ_TITLE = "quiz_title"
        const val PARAM_QUIZ_SCORE = "quiz_score"
        const val PARAM_QUIZ_DURATION = "quiz_duration_seconds"
        const val PARAM_WORD = "word"
        const val PARAM_SEARCH_QUERY = "search_query"
        const val PARAM_CHAPTER_NUMBER = "chapter_number"
        const val PARAM_IS_PREMIUM = "is_premium"
        const val PARAM_AD_TYPE = "ad_type"
        const val PARAM_AD_PLACEMENT = "ad_placement"
        const val PARAM_ERROR_MESSAGE = "error_message"
        const val PARAM_AUTH_METHOD = "method"
    }

    /**
     * Track screen view
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let {
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, it)
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        Log.d(TAG, "📊 Screen View: $screenName")
    }

    /**
     * Track PDF opened
     */
    fun logPdfOpened(pdfTitle: String, isPremium: Boolean) {
        val bundle = Bundle().apply {
            putString(PARAM_PDF_TITLE, pdfTitle)
            putBoolean(PARAM_IS_PREMIUM, isPremium)
        }
        firebaseAnalytics.logEvent(EVENT_PDF_OPENED, bundle)
        Log.d(TAG, "📄 PDF Opened: $pdfTitle (Premium: $isPremium)")
    }

    /**
     * Track quiz started
     */
    fun logQuizStarted(quizId: Int, quizTitle: String, isPremium: Boolean) {
        val bundle = Bundle().apply {
            putInt(PARAM_QUIZ_ID, quizId)
            putString(PARAM_QUIZ_TITLE, quizTitle)
            putBoolean(PARAM_IS_PREMIUM, isPremium)
        }
        firebaseAnalytics.logEvent(EVENT_QUIZ_STARTED, bundle)
        Log.d(TAG, "🎯 Quiz Started: $quizTitle (ID: $quizId)")
    }

    /**
     * Track quiz completed
     */
    fun logQuizCompleted(
        quizId: Int,
        quizTitle: String,
        score: Int,
        totalQuestions: Int,
        durationSeconds: Int,
        isPremium: Boolean
    ) {
        val bundle = Bundle().apply {
            putInt(PARAM_QUIZ_ID, quizId)
            putString(PARAM_QUIZ_TITLE, quizTitle)
            putInt(PARAM_QUIZ_SCORE, score)
            putInt("total_questions", totalQuestions)
            putInt(PARAM_QUIZ_DURATION, durationSeconds)
            putBoolean(PARAM_IS_PREMIUM, isPremium)
            putDouble("score_percentage", (score.toDouble() / totalQuestions) * 100)
        }
        firebaseAnalytics.logEvent(EVENT_QUIZ_COMPLETED, bundle)
        Log.d(TAG, "✅ Quiz Completed: $quizTitle (Score: $score/$totalQuestions)")
    }

    /**
     * Track word favorited
     */
    fun logWordFavorited(koreanWord: String, indonesianWord: String) {
        val bundle = Bundle().apply {
            putString(PARAM_WORD, koreanWord)
            putString("translation", indonesianWord)
        }
        firebaseAnalytics.logEvent(EVENT_WORD_FAVORITED, bundle)
        Log.d(TAG, "⭐ Word Favorited: $koreanWord")
    }

    /**
     * Track word unfavorited
     */
    fun logWordUnfavorited(koreanWord: String) {
        val bundle = Bundle().apply {
            putString(PARAM_WORD, koreanWord)
        }
        firebaseAnalytics.logEvent(EVENT_WORD_UNFAVORITED, bundle)
        Log.d(TAG, "💔 Word Unfavorited: $koreanWord")
    }

    /**
     * Track search performed
     */
    fun logSearchPerformed(query: String, resultsCount: Int) {
        val bundle = Bundle().apply {
            putString(PARAM_SEARCH_QUERY, query)
            putInt("results_count", resultsCount)
        }
        firebaseAnalytics.logEvent(EVENT_SEARCH_PERFORMED, bundle)
        Log.d(TAG, "🔍 Search: '$query' ($resultsCount results)")
    }

    /**
     * Track flashcard interaction
     */
    fun logFlashcardFlipped(chapterNumber: Int) {
        val bundle = Bundle().apply {
            putInt(PARAM_CHAPTER_NUMBER, chapterNumber)
        }
        firebaseAnalytics.logEvent(EVENT_FLASHCARD_FLIPPED, bundle)
    }

    /**
     * Track chapter completed
     */
    fun logChapterCompleted(chapterNumber: Int, totalWords: Int) {
        val bundle = Bundle().apply {
            putInt(PARAM_CHAPTER_NUMBER, chapterNumber)
            putInt("total_words", totalWords)
        }
        firebaseAnalytics.logEvent(EVENT_CHAPTER_COMPLETED, bundle)
        Log.d(TAG, "📚 Chapter $chapterNumber Completed ($totalWords words)")
    }

    /**
     * Track ad impression
     */
    fun logAdImpression(adType: String, placement: String, isPremium: Boolean) {
        val bundle = Bundle().apply {
            putString(PARAM_AD_TYPE, adType) // "interstitial", "banner", "rewarded"
            putString(PARAM_AD_PLACEMENT, placement) // "pdf_open", "quiz_start", etc.
            putBoolean(PARAM_IS_PREMIUM, isPremium)
        }
        firebaseAnalytics.logEvent(EVENT_AD_IMPRESSION, bundle)
        Log.d(TAG, "📺 Ad Impression: $adType at $placement")
    }

    /**
     * Track ad clicked
     */
    fun logAdClicked(adType: String, placement: String) {
        val bundle = Bundle().apply {
            putString(PARAM_AD_TYPE, adType)
            putString(PARAM_AD_PLACEMENT, placement)
        }
        firebaseAnalytics.logEvent(EVENT_AD_CLICKED, bundle)
        Log.d(TAG, "👆 Ad Clicked: $adType at $placement")
    }

    /**
     * Track ad load failure
     */
    fun logAdFailed(adType: String, placement: String, errorMessage: String) {
        val bundle = Bundle().apply {
            putString(PARAM_AD_TYPE, adType)
            putString(PARAM_AD_PLACEMENT, placement)
            putString(PARAM_ERROR_MESSAGE, errorMessage)
        }
        firebaseAnalytics.logEvent(EVENT_AD_FAILED, bundle)
        Log.d(TAG, "❌ Ad Failed: $adType at $placement - $errorMessage")
    }

    /**
     * Track rewarded ad earned
     */
    fun logRewardedAdEarned(placement: String, rewardAmount: Int) {
        val bundle = Bundle().apply {
            putString(PARAM_AD_PLACEMENT, placement)
            putInt("reward_amount", rewardAmount)
        }
        firebaseAnalytics.logEvent(EVENT_REWARDED_AD_EARNED, bundle)
        Log.d(TAG, "💰 Rewarded Ad Earned at $placement")
    }

    /**
     * Track premium screen viewed
     */
    fun logPremiumScreenViewed() {
        firebaseAnalytics.logEvent(EVENT_PREMIUM_VIEWED, null)
        Log.d(TAG, "👑 Premium Screen Viewed")
    }

    /**
     * Track premium purchase
     */
    fun logPremiumPurchased(price: Double, currency: String = "USD") {
        val bundle = Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, price)
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
        }
        firebaseAnalytics.logEvent(EVENT_PREMIUM_PURCHASED, bundle)
        Log.d(TAG, "💳 Premium Purchased: $price $currency")
    }

    /**
     * Track sign up
     */
    fun logSignUp(method: String) {
        val bundle = Bundle().apply {
            putString(PARAM_AUTH_METHOD, method) // "email", "google"
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
        Log.d(TAG, "📝 Sign Up: $method")
    }

    /**
     * Track login
     */
    fun logLogin(method: String) {
        val bundle = Bundle().apply {
            putString(PARAM_AUTH_METHOD, method) // "email", "google"
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
        Log.d(TAG, "🔑 Login: $method")
    }

    /**
     * Track logout
     */
    fun logLogout() {
        firebaseAnalytics.logEvent(EVENT_LOGOUT, null)
        Log.d(TAG, "👋 Logout")
    }

    /**
     * Set user property - Premium status
     */
    fun setUserPremiumStatus(isPremium: Boolean) {
        firebaseAnalytics.setUserProperty("is_premium_user", isPremium.toString())
        Log.d(TAG, "👤 User Property Set: is_premium_user = $isPremium")
    }

    /**
     * Set user property - User ID
     */
    fun setUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
        Log.d(TAG, "👤 User ID Set: $userId")
    }
}
