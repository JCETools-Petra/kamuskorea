package com.webtech.kamuskorea.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdManager - Helper class untuk manage Google AdMob ads
 *
 * Fitur:
 * - Interstitial Ads (fullscreen) dengan frekuensi kontrol
 * - Banner Ads untuk ditampilkan di screen
 * - Ad caching untuk performa lebih baik
 */
@Singleton
class AdManager @Inject constructor() {

    // Test Ad Unit IDs dari Google AdMob
    // IMPORTANT: Ganti dengan Ad Unit ID Anda sendiri setelah mendaftar di AdMob
    companion object {
        // Test Interstitial Ad Unit ID
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        // Test Banner Ad Unit ID
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

        // Frekuensi iklan: tampilkan interstitial setiap N kali aksi
        private const val AD_FREQUENCY = 3
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var adCounter = 0

    /**
     * Load interstitial ad untuk digunakan nanti
     */
    fun loadInterstitialAd(context: Context) {
        // Jangan load jika sedang loading atau sudah ada ad yang ready
        if (isLoadingAd || interstitialAd != null) {
            Log.d("AdManager", "⏭️ Skipping ad load - already loading or ready")
            return
        }

        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()

        Log.d("AdManager", "📡 Loading interstitial ad...")

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdManager", "❌ Ad failed to load: ${adError.message}")
                    interstitialAd = null
                    isLoadingAd = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("AdManager", "✅ Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoadingAd = false
                }
            }
        )
    }

    /**
     * Tampilkan interstitial ad dengan frequency control
     * Iklan tidak akan ditampilkan setiap kali, tapi sesuai AD_FREQUENCY
     *
     * @param activity Activity context untuk show ad
     * @param onAdDismissed Callback ketika ad ditutup atau tidak ditampilkan
     * @param forceShow Paksa tampilkan ad tanpa cek frequency (default: false)
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
        forceShow: Boolean = false
    ) {
        adCounter++

        // Cek frequency - apakah sudah waktunya tampilkan iklan?
        val shouldShowAd = forceShow || (adCounter % AD_FREQUENCY == 0)

        if (!shouldShowAd) {
            Log.d("AdManager", "⏭️ Skipping ad - frequency not met ($adCounter/$AD_FREQUENCY)")
            onAdDismissed()
            return
        }

        // Jika ad belum ready, langsung lanjut tanpa menunggu
        if (interstitialAd == null) {
            Log.d("AdManager", "⚠️ No ad available to show")
            onAdDismissed()
            // Load ad untuk next time
            loadInterstitialAd(activity)
            return
        }

        Log.d("AdManager", "📺 Showing interstitial ad")

        // Setup callback untuk ad lifecycle
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("AdManager", "✅ Ad dismissed")
                interstitialAd = null
                // Load next ad
                loadInterstitialAd(activity)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e("AdManager", "❌ Ad failed to show: ${adError.message}")
                interstitialAd = null
                // Load next ad
                loadInterstitialAd(activity)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("AdManager", "📺 Ad showed full screen")
            }
        }

        // Tampilkan ad
        interstitialAd?.show(activity)
    }

    /**
     * Reset ad counter - berguna jika ingin reset frequency counting
     */
    fun resetAdCounter() {
        adCounter = 0
        Log.d("AdManager", "🔄 Ad counter reset")
    }

    /**
     * Get ad counter untuk debugging
     */
    fun getAdCounter() = adCounter

    /**
     * Preload ad for better UX - panggil ini saat app start atau idle
     */
    fun preloadAd(context: Context) {
        loadInterstitialAd(context)
    }
}
