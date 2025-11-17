// app/src/main/java/com/webtech/kamuskorea/ads/AdManager.kt

package com.webtech.kamuskorea.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdManager - Optimized for maximum revenue without disrupting UX
 *
 * Features:
 * - Multiple action-based interstitial triggers
 * - Smart frequency control per action type
 * - Rate limiting (max ads per hour)
 * - Minimum interval between ads
 * - Auto preloading for better performance
 */
@Singleton
class AdManager @Inject constructor() {

    companion object {
        private const val TAG = "AdManager"

        // Ad Unit IDs
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7038054430257806/5551158856"
        const val BANNER_AD_UNIT_ID = "ca-app-pub-7038054430257806/1559108807"
        // TODO: Replace with your actual Rewarded Video Ad Unit ID
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Test ID

        // Flashcard click frequency
        private const val FLASHCARD_CLICK_FREQUENCY = 20 // Show ad every 20 clicks

        // ✅ OPTIMIZED: Different frequency for each action
        // DEBUG: Set frequencies to 1 for testing (show every time)
        private const val PDF_OPEN_FREQUENCY = 1          // Show ad every PDF open (DEBUG)
        private const val QUIZ_START_FREQUENCY = 1        // Show ad every quiz/exam start (DEBUG)
        private const val QUIZ_COMPLETE_FREQUENCY = 1     // Show ad every quiz completion (DEBUG)
        private const val SESSION_START_FREQUENCY = 1     // Show ad every session (DEBUG)
        private const val NAVIGATION_FREQUENCY = 10       // Show ad every 10 navigations

        // PRODUCTION: Uncomment these and comment above for production
        // private const val PDF_OPEN_FREQUENCY = 2          // Show ad every 2 PDF opens
        // private const val QUIZ_START_FREQUENCY = 2        // Show ad every 2 quiz/exam starts
        // private const val QUIZ_COMPLETE_FREQUENCY = 3     // Show ad every 3 quiz completions
        // private const val SESSION_START_FREQUENCY = 5     // Show ad every 5 app sessions
        // private const val NAVIGATION_FREQUENCY = 10       // Show ad every 10 navigations

        // ✅ RATE LIMITING: Prevent ad fatigue
        private const val MAX_INTERSTITIAL_PER_HOUR = 4   // Maximum 4 interstitial ads per hour
        // DEBUG: Set to 5 seconds for testing, change to 60 for production
        private const val MIN_INTERVAL_SECONDS = 5        // Minimum 5 seconds between ads (DEBUG)
        // private const val MIN_INTERVAL_SECONDS = 60    // Minimum 60 seconds between ads (PRODUCTION)
    }

    // Ad instances
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewardedAd = false

    // ✅ Separate counters for each action type
    private var pdfOpenCounter = 0
    private var quizStartCounter = 0
    private var quizCompleteCounter = 0
    private var sessionStartCounter = 0
    private var navigationCounter = 0
    private var flashcardClickCounter = 0

    // ✅ Rate limiting tracking
    private var lastAdShownTime = 0L
    private var interstitialCountThisHour = 0
    private var hourStartTime = System.currentTimeMillis()

    init {
        Log.d(TAG, "AdManager initialized with smart frequency control")
    }

    /**
     * Reset hourly counter if an hour has passed
     */
    private fun resetHourlyCounterIfNeeded() {
        val now = System.currentTimeMillis()
        val oneHourInMillis = 60 * 60 * 1000

        if (now - hourStartTime >= oneHourInMillis) {
            Log.d(TAG, "🔄 Resetting hourly ad counter")
            interstitialCountThisHour = 0
            hourStartTime = now
        }
    }

    /**
     * Check if we can show an interstitial ad based on rate limits
     */
    private fun canShowInterstitial(): Boolean {
        resetHourlyCounterIfNeeded()

        val now = System.currentTimeMillis()
        val timeSinceLastAd = (now - lastAdShownTime) / 1000 // in seconds

        // Check minimum interval
        if (timeSinceLastAd < MIN_INTERVAL_SECONDS) {
            Log.d(TAG, "⏭️ Skipping ad - Too soon (${timeSinceLastAd}s since last ad)")
            return false
        }

        // Check hourly limit
        if (interstitialCountThisHour >= MAX_INTERSTITIAL_PER_HOUR) {
            Log.d(TAG, "⏭️ Skipping ad - Hourly limit reached ($interstitialCountThisHour/$MAX_INTERSTITIAL_PER_HOUR)")
            return false
        }

        return true
    }

    /**
     * Load interstitial ad for future use
     */
    fun loadInterstitialAd(context: Context) {
        if (isLoadingAd || interstitialAd != null) {
            Log.d(TAG, "⏭️ Skipping ad load - already loading or ready")
            return
        }

        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()

        Log.d(TAG, "📡 Loading interstitial ad...")

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "❌ Ad failed to load: ${adError.message}")
                    interstitialAd = null
                    isLoadingAd = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoadingAd = false
                }
            }
        )
    }

    /**
     * Show interstitial ad when PDF is opened
     */
    fun showInterstitialOnPdfOpen(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        pdfOpenCounter++
        Log.d(TAG, "📄 PDF open counter: $pdfOpenCounter (show every $PDF_OPEN_FREQUENCY)")

        if (pdfOpenCounter % PDF_OPEN_FREQUENCY == 0 && canShowInterstitial()) {
            showInterstitialInternal(activity, "PDF_OPEN", onAdDismissed)
        } else {
            onAdDismissed()
        }
    }

    /**
     * Show interstitial ad when starting quiz/exam
     */
    fun showInterstitialOnQuizStart(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        quizStartCounter++
        Log.d(TAG, "🎯 Quiz/Exam start counter: $quizStartCounter (show every $QUIZ_START_FREQUENCY)")

        if (quizStartCounter % QUIZ_START_FREQUENCY == 0 && canShowInterstitial()) {
            showInterstitialInternal(activity, "QUIZ_START", onAdDismissed)
        } else {
            onAdDismissed()
        }
    }

    /**
     * Show interstitial ad when quiz is completed
     */
    fun showInterstitialOnQuizComplete(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        quizCompleteCounter++
        Log.d(TAG, "✅ Quiz complete counter: $quizCompleteCounter (show every $QUIZ_COMPLETE_FREQUENCY)")

        if (quizCompleteCounter % QUIZ_COMPLETE_FREQUENCY == 0 && canShowInterstitial()) {
            showInterstitialInternal(activity, "QUIZ_COMPLETE", onAdDismissed)
        } else {
            onAdDismissed()
        }
    }

    /**
     * Show interstitial ad on app session start
     */
    fun showInterstitialOnSessionStart(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        sessionStartCounter++
        Log.d(TAG, "🚀 Session start counter: $sessionStartCounter (show every $SESSION_START_FREQUENCY)")

        if (sessionStartCounter % SESSION_START_FREQUENCY == 0 && canShowInterstitial()) {
            showInterstitialInternal(activity, "SESSION_START", onAdDismissed)
        } else {
            onAdDismissed()
        }
    }

    /**
     * Show interstitial ad on navigation events
     */
    fun showInterstitialOnNavigation(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        navigationCounter++
        Log.d(TAG, "🧭 Navigation counter: $navigationCounter (show every $NAVIGATION_FREQUENCY)")

        if (navigationCounter % NAVIGATION_FREQUENCY == 0 && canShowInterstitial()) {
            showInterstitialInternal(activity, "NAVIGATION", onAdDismissed)
        } else {
            onAdDismissed()
        }
    }

    /**
     * Internal function to actually show the ad
     */
    private fun showInterstitialInternal(
        activity: Activity,
        source: String,
        onAdDismissed: () -> Unit
    ) {
        if (interstitialAd == null) {
            Log.w(TAG, "⚠️ No ad available to show (source: $source)")
            onAdDismissed()
            loadInterstitialAd(activity)
            return
        }

        Log.d(TAG, "📺 Showing interstitial ad (source: $source)")

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "✅ Ad dismissed (source: $source)")
                interstitialAd = null
                lastAdShownTime = System.currentTimeMillis()
                interstitialCountThisHour++

                Log.d(TAG, "📊 Ad stats - Shown this hour: $interstitialCountThisHour/$MAX_INTERSTITIAL_PER_HOUR")

                // Preload next ad
                loadInterstitialAd(activity)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "❌ Ad failed to show: ${adError.message} (source: $source)")
                interstitialAd = null
                loadInterstitialAd(activity)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "📺 Ad showed full screen (source: $source)")
            }

            override fun onAdClicked() {
                Log.d(TAG, "👆 Ad clicked (source: $source)")
            }

            override fun onAdImpression() {
                Log.d(TAG, "👁️ Ad impression recorded (source: $source)")
            }
        }

        interstitialAd?.show(activity)
    }

    /**
     * Legacy function - kept for backward compatibility
     * Use specific functions (showInterstitialOnPdfOpen, etc.) instead
     */
    @Deprecated(
        message = "Use specific functions like showInterstitialOnPdfOpen instead",
        replaceWith = ReplaceWith("showInterstitialOnPdfOpen(activity, onAdDismissed)")
    )
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
        forceShow: Boolean = false
    ) {
        if (forceShow && canShowInterstitial()) {
            showInterstitialInternal(activity, "LEGACY_FORCE", onAdDismissed)
        } else {
            onAdDismissed()
        }
    }

    /**
     * Load rewarded video ad
     */
    private fun loadRewardedAd(context: Context) {
        if (isLoadingRewardedAd || rewardedAd != null) {
            Log.d(TAG, "⏭️ Skipping rewarded ad load (already loading or loaded)")
            return
        }

        isLoadingRewardedAd = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "✅ Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isLoadingRewardedAd = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ Rewarded ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    isLoadingRewardedAd = false
                }
            }
        )
    }

    /**
     * Show rewarded ad after flashcard clicks
     */
    fun showRewardedAdOnFlashcardClick(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        flashcardClickCounter++
        Log.d(TAG, "🎴 Flashcard click counter: $flashcardClickCounter (show every $FLASHCARD_CLICK_FREQUENCY)")

        if (flashcardClickCounter % FLASHCARD_CLICK_FREQUENCY == 0) {
            if (rewardedAd != null) {
                Log.d(TAG, "🎬 Showing rewarded ad for flashcard clicks")

                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "📺 Rewarded ad dismissed")
                        rewardedAd = null
                        loadRewardedAd(activity) // Preload next ad
                        onAdDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "❌ Rewarded ad failed to show: ${adError.message}")
                        rewardedAd = null
                        loadRewardedAd(activity) // Preload next ad
                        onAdDismissed()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "📺 Rewarded ad shown successfully")
                    }
                }

                rewardedAd?.show(activity, OnUserEarnedRewardListener { reward ->
                    Log.d(TAG, "💰 User earned reward: ${reward.amount} ${reward.type}")
                })
            } else {
                Log.d(TAG, "⚠️ Rewarded ad not ready, loading now")
                loadRewardedAd(activity)
                onAdDismissed()
            }
        } else {
            onAdDismissed()
        }
    }

    /**
     * Preload ad for better performance
     * Call this in MainActivity.onCreate or Application.onCreate
     */
    fun preloadAd(context: Context) {
        Log.d(TAG, "🔄 Preloading ads for better performance")
        loadInterstitialAd(context)
        loadRewardedAd(context)
    }

    /**
     * Reset all counters (useful for testing)
     */
    fun resetAllCounters() {
        pdfOpenCounter = 0
        quizStartCounter = 0
        quizCompleteCounter = 0
        sessionStartCounter = 0
        navigationCounter = 0
        flashcardClickCounter = 0
        interstitialCountThisHour = 0
        Log.d(TAG, "🔄 All counters reset")
    }

    /**
     * Get current ad statistics (useful for debugging)
     */
    fun getAdStats(): String {
        return """
            PDF Opens: $pdfOpenCounter
            Quiz/Exam Starts: $quizStartCounter
            Quiz Completions: $quizCompleteCounter
            Sessions: $sessionStartCounter
            Navigations: $navigationCounter
            Flashcard Clicks: $flashcardClickCounter
            Ads This Hour: $interstitialCountThisHour/$MAX_INTERSTITIAL_PER_HOUR
            Interstitial Ready: ${interstitialAd != null}
            Rewarded Ad Ready: ${rewardedAd != null}
        """.trimIndent()
    }
}