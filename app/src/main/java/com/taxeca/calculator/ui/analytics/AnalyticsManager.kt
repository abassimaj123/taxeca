package com.taxeca.calculator.ui.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.taxeca.calculator.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val fa          = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    /** Log a Firebase Analytics event with optional key/value params. */
    fun log(event: String, vararg params: Pair<String, Any?>) {
        val bundle = Bundle()
        bundle.putString("app_name", "TaxeCA")
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int    -> bundle.putInt(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float  -> bundle.putFloat(key, value)
                is Long   -> bundle.putLong(key, value)
                else      -> value?.toString()?.let { bundle.putString(key, it) }
            }
        }
        fa.logEvent(event, bundle)
    }

    fun setUserProperty(name: String, value: String) = fa.setUserProperty(name, value)

    // ── Named funnel events ───────────────────────────────────────────────────

    fun logScreenView(screenName: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        bundle.putString("app_name", "TaxeCA")
        fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logAppOpen()           = log("app_open")
    fun logPaywallShown(type: String) = log("paywall_shown", "type" to type)
    fun logPaywallDismissed()  = log("paywall_dismissed")
    fun logPurchaseStarted()   = log("iap_purchase_started")
    fun logPurchaseSuccess()   = log("iap_purchase_success")

    fun logPurchaseCompleted(value: Double, currency: String) {
        // Dual-logging is intentional and consistent across the Kotlin portfolio
        // (see TaxUS AnalyticsManager.logPurchaseCompleted): the custom event feeds
        // internal funnel dashboards, while the GA4 standard "purchase" event below
        // joins with BigQuery / Revenue / ad-platform reporting. Keep both.
        log("iap_purchase_success")
        val bundle = Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, value)
            putString(FirebaseAnalytics.Param.CURRENCY, currency.ifBlank { "CAD" })
            putString(FirebaseAnalytics.Param.TRANSACTION_ID, System.currentTimeMillis().toString())
        }
        fa.logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
    }

    fun logPurchaseError(reason: String) = log("iap_purchase_error", "reason" to reason)
    /** Product query failed (not found / billing unreachable) — distinct from
     * [logPurchaseError], which fires only after the user actually taps buy.
     * Fires earlier, when the product itself is unreachable — the signature
     * of a broken Play Console config, otherwise invisible in GA4. */
    fun logIapPriceUnavailable(reason: String) = log("iap_price_unavailable", "reason" to reason)

    // Rewarded — split into load/show funnel steps (mirrors the Flutter portfolio's
    // calcwise_core event vocabulary so AppPulse queries work identically across
    // platforms). rewarded_ad_shown/completed/failed kept for dashboard continuity.
    fun logRewardedOffered()     = log("rewarded_offered")
    fun logRewardedLoaded()      = log("rewarded_loaded")
    fun logRewardedAdShown()     = log("rewarded_ad_shown")
    fun logRewardedAdCompleted() = log("rewarded_ad_completed")
    fun logRewardedAdFailed()    = log("rewarded_ad_failed")
    /** Ad loaded (counted as an AdMob request) but .show() itself failed —
     * distinct from a load failure, which self-heals on retry. */
    fun logRewardedShowFailed()  = log("rewarded_show_failed")
    fun logRewardedDailyLimit()  = log("rewarded_daily_limit_reached")

    // Interstitial funnel — previously not logged at all.
    fun logInterstitialLoaded()     = log("interstitial_loaded")
    fun logInterstitialLoadFailed() = log("interstitial_load_failed")
    fun logInterstitialShown()      = log("interstitial_shown")
    fun logInterstitialShowFailed() = log("interstitial_show_failed")

    // Banner funnel — logBannerAdFailed() was defined but never called; kept for
    // compat, logBannerLoadFailed() is the one actually wired now.
    fun logBannerLoaded()        = log("banner_loaded")
    fun logBannerLoadFailed()    = log("banner_load_failed")
    fun logBannerAdFailed()      = log("banner_ad_failed")

    /** AdMob revenue attribution — fires on onPaidEvent for banner/interstitial/
     * rewarded. Active users with real impressions but zero ad_paid rows is the
     * signature of an ad unit resolving to Google's TEST id instead of the real
     * one — directly cross-checkable against the AdMob console from GA4 alone. */
    fun logAdPaid(format: String, valueMicros: Long, currencyCode: String, precision: String) =
        log("ad_paid", "format" to format, "value_micros" to valueMicros,
            "currency" to currencyCode, "precision" to precision)

    /** Fires when the native review prompt is actually surfaced (not on every
     * eligibility check) — lets AppPulse correlate prompt timing with the
     * Play Console rating trend. */
    fun logReviewRequested() = log("review_requested")

    /** Persistent user property — enables filtering DAU/retention by language
     * in the Firebase console / AppPulse. */
    fun setLanguage(lang: String) = setUserProperty("app_language", lang)

    // ── Crashlytics helpers ───────────────────────────────────────────────────

    fun setKey(key: String, value: String)  = crashlytics.setCustomKey(key, value)
    fun setKey(key: String, value: Boolean) = crashlytics.setCustomKey(key, value)
    fun setKey(key: String, value: Int)     = crashlytics.setCustomKey(key, value)

    fun recordException(e: Throwable) {
        if (BuildConfig.DEBUG) Log.e("TaxeCA", "recordException: ${e.message}", e)
        crashlytics.recordException(e)
    }
}
