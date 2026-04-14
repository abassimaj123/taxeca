package com.taxeca.calculator.ui.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false

    // ── Interstitial frequency gate ──────────────────────────────────────────
    private var calcCount            = 0
    private var lastInterstitialTime = 0L
    private companion object {
        const val CALC_THRESHOLD  = 5            // show after every 5 calculations
        const val COOLDOWN_MS     = 5 * 60_000L  // 5-minute minimum cooldown
    }

    fun preloadInterstitial() {
        if (!AdConfig.ADS_ENABLED) return
        if (isLoadingInterstitial || interstitialAd != null) return
        isLoadingInterstitial = true
        InterstitialAd.load(
            context,
            AdConfig.INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        if (!AdConfig.ADS_ENABLED) { onDismissed(); return }
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            preloadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial()
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onDismissed()
            }
        }
        ad.show(activity)
    }

    fun loadRewarded(onLoaded: (RewardedAd) -> Unit, onFailed: () -> Unit) {
        if (!AdConfig.ADS_ENABLED) { onFailed(); return }
        RewardedAd.load(
            context,
            AdConfig.REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) = onLoaded(ad)
                override fun onAdFailedToLoad(error: LoadAdError) = onFailed()
            }
        )
    }

    fun showRewarded(
        ad: RewardedAd,
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissedWithoutReward: () -> Unit
    ) {
        var rewarded = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (rewarded) onRewarded() else onDismissedWithoutReward()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onRewarded() // grant access on failure
            }
        }
        ad.show(activity) { _ -> rewarded = true }
    }

    /**
     * Call after every completed calculation.
     * Shows interstitial after [CALC_THRESHOLD] calculations with a [COOLDOWN_MS] cooldown.
     * [onCompleted] is always called, whether or not an ad is shown.
     */
    fun onCalculation(activity: Activity, onCompleted: () -> Unit = {}) {
        if (!AdConfig.ADS_ENABLED) { onCompleted(); return }
        calcCount++
        if (calcCount >= CALC_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastInterstitialTime >= COOLDOWN_MS) {
                calcCount = 0
                lastInterstitialTime = now
                showInterstitial(activity, onCompleted)
                return
            }
        }
        onCompleted()
    }
}
