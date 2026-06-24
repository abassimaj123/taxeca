package com.taxeca.calculator.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.taxeca.calculator.BuildConfig
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.repository.FreemiumRepository
import com.taxeca.calculator.ui.ads.AdConfig
import com.taxeca.calculator.ui.ads.AdManager
import com.taxeca.calculator.ui.ads.IAPManager
import com.taxeca.calculator.ui.ads.ReviewManager
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FreemiumViewModel @Inject constructor(
    private val freemiumRepo: FreemiumRepository,
    private val adManager: AdManager,
    private val iapManager: IAPManager,
    private val reviewManager: ReviewManager,
    private val analytics: AnalyticsManager
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    val isPremium: StateFlow<Boolean> = iapManager.isPremium
    val premiumPrice: StateFlow<String?> = iapManager.premiumPrice

    private val _isRewardedActive = MutableStateFlow(false)
    val isRewardedActive: StateFlow<Boolean> = _isRewardedActive.asStateFlow()

    // Milliseconds remaining in rewarded session (0 = expired)
    private val _rewardedTimeLeftMs = MutableStateFlow(0L)
    val rewardedTimeLeftMs: StateFlow<Long> = _rewardedTimeLeftMs.asStateFlow()

    // hasAccess = premium OR rewarded active
    private val _hasAccess = MutableStateFlow(!AdConfig.ADS_ENABLED)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val _isLoadingAd = MutableStateFlow(false)
    val isLoadingAd: StateFlow<Boolean> = _isLoadingAd.asStateFlow()

    // true only when session is expired AND daily limit not reached
    private val _canWatchRewarded = MutableStateFlow(false)
    val canWatchRewarded: StateFlow<Boolean> = _canWatchRewarded.asStateFlow()

    private val _adUnavailable = MutableStateFlow(false)
    val adUnavailable: StateFlow<Boolean> = _adUnavailable.asStateFlow()

    private val _restoreNoneFound = MutableStateFlow(false)
    val restoreNoneFound: StateFlow<Boolean> = _restoreNoneFound.asStateFlow()
    fun clearRestoreNone() { _restoreNoneFound.value = false }

    private val _iapError = MutableStateFlow<String?>(null)
    val iapError: StateFlow<String?> = _iapError.asStateFlow()

    // ── Session-based paywall trigger ──────────────────────────────────────────
    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    /** true when paywall is triggered at session 7+ (hard gate — dismiss at 50% opacity). */
    private val _isHardPaywall = MutableStateFlow(false)
    val isHardPaywall: StateFlow<Boolean> = _isHardPaywall.asStateFlow()

    private var _sessionCount     = 0
    private var _actionCount      = 0
    private var _shownThisSession = false

    private var expiryJob: Job? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        adManager.preloadInterstitial()
        viewModelScope.launch {
            // Seed IAPManager with cached premium status to eliminate "flash paywall" on startup
            // while BillingClient reconnects asynchronously (~500ms delay).
            val cached = freemiumRepo.isPremiumCached.first()
            if (cached) iapManager.seedPremium(true)
        }
        startExpiryTimer()
        // Observe isPremium changes → update hasAccess + persist to cache
        viewModelScope.launch {
            iapManager.isPremium.collect { premium ->
                _hasAccess.value = premium || _isRewardedActive.value
                // isPremium is latched (never downgraded by a transient empty query),
                // so this only ever caches a confirmed-true. Safe across launches.
                freemiumRepo.setPremiumCached(premium)
            }
        }
    }

    private fun startExpiryTimer() {
        expiryJob?.cancel()
        expiryJob = viewModelScope.launch {
            while (isActive) {
                refreshRewardedState()
                delay(30_000L) // check every 30 seconds
            }
        }
    }

    private suspend fun refreshRewardedState() {
        val rewardedAt = freemiumRepo.rewardedUnlockedAt.first()
        val active = freemiumRepo.isRewardedActive(rewardedAt)
        _isRewardedActive.value = active
        _rewardedTimeLeftMs.value = if (active) {
            val expiry = rewardedAt + 60L * 60 * 1000
            maxOf(0L, expiry - System.currentTimeMillis())
        } else 0L
        _hasAccess.value = iapManager.isPremium.value || active || !AdConfig.ADS_ENABLED
        _canWatchRewarded.value = !iapManager.isPremium.value && freemiumRepo.canWatchRewarded()
    }

    /** Call once per app launch. */
    fun recordSession() {
        if (iapManager.isPremium.value) return
        viewModelScope.launch {
            _sessionCount = freemiumRepo.incrementSession()
            _actionCount = 0
            _shownThisSession = false
        }
    }

    /** Call on meaningful actions: tab switches, calculations.
     *  Shows paywall at most once per session. */
    fun recordAction() {
        if (iapManager.isPremium.value) return
        if (_shownThisSession) return
        if (_sessionCount == 0) return   // session not yet recorded

        _actionCount++

        // Sessions 1-3: free exploration
        if (_sessionCount <= 3) return

        // Sessions 4-6: gentle nudge after 5 actions
        if (_sessionCount <= 6 && _actionCount >= 5) {
            _shownThisSession = true
            _isHardPaywall.value = false
            analytics.log("paywall_shown", "type" to "soft")
            _showPaywall.value = true
            return
        }

        // Sessions 7+: stronger nudge after 4 actions
        if (_sessionCount >= 7 && _actionCount >= 4) {
            _shownThisSession = true
            _isHardPaywall.value = true
            analytics.log("paywall_shown", "type" to "hard")
            _showPaywall.value = true
        }
    }

    fun dismissPaywall() {
        _showPaywall.value = false
        _isHardPaywall.value = false
    }

    fun logTabChanged(tab: String) = analytics.log("tab_changed", "tab" to tab)

    val shouldShowRewardedShield: Boolean get() = _sessionCount >= 2 && !iapManager.isPremium.value

    override fun onCleared() {
        super.onCleared()
        expiryJob?.cancel()
    }

    // ── Gated access ──────────────────────────────────────────────────────────

    /**
     * Request access to a gated feature.
     * If premium or rewarded active → calls [onGranted] immediately.
     * Otherwise shows rewarded ad. Does NOT grant free access if ad fails.
     */
    fun requestAccess(context: Context, onGranted: () -> Unit) {
        if (_hasAccess.value) { onGranted(); return }
        val activity = context.findActivity() ?: return
        _isLoadingAd.value = true
        adManager.loadRewarded(
            onLoaded = { ad ->
                _isLoadingAd.value = false
                analytics.log("rewarded_ad_shown")
                adManager.showRewarded(
                    ad = ad,
                    activity = activity,
                    onRewarded = {
                        analytics.log("rewarded_ad_completed")
                        viewModelScope.launch {
                            freemiumRepo.recordRewardedWatch()
                            refreshRewardedState()
                            onGranted()
                        }
                    },
                    onDismissedWithoutReward = {}
                )
            },
            onFailed = {
                // Ad not available — do NOT grant free access; show error to user
                analytics.logRewardedAdFailed()
                _isLoadingAd.value = false
                _adUnavailable.value = true
                if (BuildConfig.DEBUG) Log.d("FreemiumVM", "Rewarded ad unavailable — access denied")
            }
        )
    }

    // ── Voluntary bonus watch ─────────────────────────────────────────────────

    fun watchRewardedForBonus(context: Context) {
        if (!_canWatchRewarded.value) return          // session active OR daily limit hit
        val activity = context.findActivity() ?: return
        _isLoadingAd.value = true
        _adUnavailable.value = false
        adManager.loadRewarded(
            onLoaded = { ad ->
                _isLoadingAd.value = false
                analytics.log("rewarded_ad_shown")
                adManager.showRewarded(
                    ad = ad,
                    activity = activity,
                    onRewarded = {
                        analytics.log("rewarded_ad_completed")
                        viewModelScope.launch {
                            freemiumRepo.recordRewardedWatch()
                            refreshRewardedState()
                        }
                    },
                    onDismissedWithoutReward = {}
                )
            },
            onFailed = {
                analytics.logRewardedAdFailed()
                _isLoadingAd.value = false
                _adUnavailable.value = true
            }
        )
    }

    fun clearAdUnavailable() { _adUnavailable.value = false }
    fun clearIapError()      { _iapError.value = null }

    // ── IAP ───────────────────────────────────────────────────────────────────

    fun buyPremium(activity: Activity) {
        analytics.log("iap_purchase_started")
        iapManager.launchPurchase(
            activity = activity,
            onSuccess = {
                analytics.logPurchaseCompleted(
                    iapManager.pendingPurchaseValue,
                    iapManager.pendingPurchaseCurrency,
                )
                analytics.setUserProperty("is_premium", "true")
                _hasAccess.value = true
                reviewManager.maybeRequestReview(activity)
            },
            onError = { reason ->
                when (reason) {
                    "cancelled" -> { /* user backed out — silent */ }
                    "pending"   -> {
                        // Deferred payment, not a failure — surface info, don't log as crash.
                        analytics.log("iap_purchase_pending")
                        _iapError.value = "pending"
                    }
                    else -> {
                        analytics.logPurchaseError(reason)
                        analytics.recordException(RuntimeException("IAP failed: $reason"))
                        _iapError.value = reason
                    }
                }
            }
        )
    }

    fun restorePurchases(activity: Activity) {
        iapManager.restorePurchases(
            onSuccess = {
                analytics.log("iap_restore_success")
                _hasAccess.value = true
            },
            onNone = {
                analytics.log("iap_restore_none")
                _restoreNoneFound.value = true
            }
        )
    }

    // ── Review ────────────────────────────────────────────────────────────────

    fun maybeRequestReview(activity: Activity) {
        reviewManager.maybeRequestReview(activity)
    }

    // ── Interstitial ──────────────────────────────────────────────────────────

    fun trackCalculation(context: Context) {
        reviewManager.trackCalculation() // count usage for review prompt threshold
        val activity = context.findActivity() ?: return
        if (iapManager.isPremium.value || _isRewardedActive.value) return // no ads during premium or rewarded session
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
