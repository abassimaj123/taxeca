package com.taxeca.calculator.ui.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
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

    fun logAppOpen()           = log("app_open")
    fun logPaywallShown(type: String) = log("paywall_shown", "type" to type)
    fun logPaywallDismissed()  = log("paywall_dismissed")
    fun logPurchaseStarted()   = log("iap_purchase_started")
    fun logPurchaseSuccess()   = log("iap_purchase_success")
    fun logPurchaseError(reason: String) = log("iap_purchase_error", "reason" to reason)
    fun logRewardedAdShown()     = log("rewarded_ad_shown")
    fun logRewardedAdCompleted() = log("rewarded_ad_completed")
    fun logRewardedAdFailed()    = log("rewarded_ad_failed")
    fun logRewardedDailyLimit()  = log("rewarded_daily_limit_reached")
    fun logBannerAdFailed()      = log("banner_ad_failed")

    // ── Crashlytics helpers ───────────────────────────────────────────────────

    fun setKey(key: String, value: String)  = crashlytics.setCustomKey(key, value)
    fun setKey(key: String, value: Boolean) = crashlytics.setCustomKey(key, value)
    fun setKey(key: String, value: Int)     = crashlytics.setCustomKey(key, value)

    fun recordException(e: Throwable) {
        Log.e("TaxeCA", "recordException: ${e.message}", e)
        crashlytics.recordException(e)
    }
}
