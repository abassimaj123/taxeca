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
import com.taxeca.calculator.data.repository.AdRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adRepo: AdRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false

    // ── Interstitial frequency gate ──────────────────────────────────────────
    // In-memory for fast gate checks; seeded from DataStore on init so counter
    // survives app restarts (users can't reset it by force-closing).
    @Volatile private var calcCount            = 0
    @Volatile private var lastInterstitialTime = 0L

    private companion object {
        const val CALC_THRESHOLD  = 3            // show after every 3 calculations
        const val COOLDOWN_MS     = 3 * 60_000L  // 3-minute minimum cooldown
    }

    init {
        scope.launch {
            calcCount            = adRepo.getCalcCount()
            lastInterstitialTime = adRepo.getLastInterstitialMs()
        }
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
        onShown: () -> Unit = {},
        onRewarded: () -> Unit,
        onDismissedWithoutReward: () -> Unit
    ) {
        var rewarded = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                onShown()
            }
            override fun onAdDismissedFullScreenContent() {
                if (rewarded) onRewarded() else onDismissedWithoutReward()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onDismissedWithoutReward()
            }
        }
        ad.show(activity) { _ -> rewarded = true }
    }

    /**
     * Call after every completed calculation.
     * Shows interstitial after [CALC_THRESHOLD] calculations with a [COOLDOWN_MS] cooldown.
     * Counter persists across restarts — users cannot bypass by force-closing.
     * [onCompleted] is always called, whether or not an ad is shown.
     */
    fun onCalculation(activity: Activity, onCompleted: () -> Unit = {}) {
        if (!AdConfig.ADS_ENABLED) { onCompleted(); return }
        calcCount++
        scope.launch { adRepo.saveCalcCount(calcCount) }
        if (calcCount >= CALC_THRESHOLD) {
            val now = System.currentTimeMillis()
            val cooldownElapsed = now - lastInterstitialTime >= COOLDOWN_MS
            // Only consume the slot + arm the cooldown when an ad is actually
            // ready to show. Otherwise a not-yet-loaded ad would burn the
            // impression opportunity AND start a 3-min cooldown for nothing.
            if (cooldownElapsed && interstitialAd != null) {
                calcCount = 0
                lastInterstitialTime = now
                scope.launch {
                    adRepo.saveCalcCount(0)
                    adRepo.saveLastInterstitialMs(now)
                }
                showInterstitial(activity, onCompleted)
                return
            }
            // Threshold reached but no ad ready — make sure one is loading.
            preloadInterstitial()
        }
        onCompleted()
    }
}
