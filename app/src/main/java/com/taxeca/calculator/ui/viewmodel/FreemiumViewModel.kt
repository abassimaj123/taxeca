package com.taxeca.calculator.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.repository.FreemiumRepository
import com.taxeca.calculator.ui.ads.AdConfig
import com.taxeca.calculator.ui.ads.AdManager
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FreemiumViewModel @Inject constructor(
    private val freemiumRepo: FreemiumRepository,
    private val adManager: AdManager,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private var trialExpiredLogged = false

    private val _hasAccess   = MutableStateFlow(!AdConfig.ADS_ENABLED)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val _isLoadingAd = MutableStateFlow(false)
    val isLoadingAd: StateFlow<Boolean> = _isLoadingAd.asStateFlow()

    init {
        viewModelScope.launch {
            val isFirstTime = freemiumRepo.firstLaunchDate.first() == 0L
            freemiumRepo.ensureFirstLaunch()
            if (isFirstTime) analytics.log("trial_started")
        }
        viewModelScope.launch {
            combine(
                freemiumRepo.firstLaunchDate,
                freemiumRepo.rewardedUnlockedAt
            ) { firstLaunch, rewardedAt ->
                val trialActive    = freemiumRepo.isTrialActive(firstLaunch)
                val rewardedActive = freemiumRepo.isRewardedActive(rewardedAt)
                Log.d("Trial",
                    "firstLaunch=$firstLaunch " +
                    "trialActive=$trialActive " +
                    "rewardedAt=$rewardedAt " +
                    "rewardedActive=$rewardedActive " +
                    "→ hasAccess=${trialActive || rewardedActive}"
                )
                if (!trialActive && !rewardedActive && !trialExpiredLogged) {
                    trialExpiredLogged = true
                    analytics.log("trial_expired")
                }
                trialActive || rewardedActive
            }.collect { access ->
                analytics.setKey("trial_active", access)
                _hasAccess.value = access
            }
        }
        adManager.preloadInterstitial()
    }

    // ── Gated access (Share / History) ────────────────────────────────────────

    /**
     * Request access to a gated feature (Share / History).
     * If already active → calls [onGranted] immediately.
     * Otherwise loads rewarded ad → grants 60 min on success.
     * If no ad available → grants free access.
     */
    fun requestAccess(context: Context, onGranted: () -> Unit) {
        if (_hasAccess.value) {
            onGranted()
            return
        }
        val activity = context.findActivity() ?: run { onGranted(); return }
        _isLoadingAd.value = true
        adManager.loadRewarded(
            onLoaded = { ad ->
                _isLoadingAd.value = false
                analytics.log("rewarded_ad_shown")
                adManager.showRewarded(
                    ad       = ad,
                    activity = activity,
                    onRewarded = {
                        analytics.log("rewarded_ad_completed")
                        analytics.log("rewarded_access_granted")
                        viewModelScope.launch {
                            freemiumRepo.setRewardedUnlockedAt()
                            _hasAccess.value = true
                            onGranted()
                        }
                    },
                    onDismissedWithoutReward = {}
                )
            },
            onFailed = {
                // Ad unavailable — grant access for free
                _isLoadingAd.value = false
                viewModelScope.launch {
                    freemiumRepo.setRewardedUnlockedAt()
                    _hasAccess.value = true
                    onGranted()
                }
            }
        )
    }

    // ── Voluntary "Débloquer 1h premium" button ───────────────────────────────

    /**
     * User voluntarily watches a rewarded ad to get/extend 60-min premium access.
     * Unlike [requestAccess], does NOT grant free access if the ad fails to load
     * (this is an optional bonus action, not a gate).
     */
    fun watchRewardedForBonus(context: Context) {
        val activity = context.findActivity() ?: return
        _isLoadingAd.value = true
        adManager.loadRewarded(
            onLoaded = { ad ->
                _isLoadingAd.value = false
                analytics.log("rewarded_ad_shown")
                adManager.showRewarded(
                    ad       = ad,
                    activity = activity,
                    onRewarded = {
                        analytics.log("rewarded_ad_completed")
                        analytics.log("rewarded_access_granted")
                        viewModelScope.launch {
                            freemiumRepo.setRewardedUnlockedAt()
                            _hasAccess.value = true
                            Log.d("Trial", "watchRewardedForBonus: 60 min unlocked")
                        }
                    },
                    onDismissedWithoutReward = {}
                )
            },
            onFailed = {
                _isLoadingAd.value = false
                Log.d("Trial", "watchRewardedForBonus: no ad available")
            }
        )
    }

    // ── Interstitial tracking ─────────────────────────────────────────────────

    /**
     * Call after each calculation result.
     * Shows interstitial after 5 calculations with a 5-minute cooldown.
     */
    fun trackCalculation(context: Context) {
        val activity = context.findActivity() ?: return
        adManager.onCalculation(activity)
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
