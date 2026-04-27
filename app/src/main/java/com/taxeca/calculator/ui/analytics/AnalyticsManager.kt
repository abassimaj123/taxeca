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

    fun setKey(key: String, value: String)  = crashlytics.setCustomKey(key, value)
    fun setKey(key: String, value: Boolean) = crashlytics.setCustomKey(key, value)
    fun setKey(key: String, value: Int)     = crashlytics.setCustomKey(key, value)

    fun recordException(e: Throwable) {
        Log.e("TaxeCA", "recordException: ${e.message}", e)
        crashlytics.recordException(e)
    }
}
