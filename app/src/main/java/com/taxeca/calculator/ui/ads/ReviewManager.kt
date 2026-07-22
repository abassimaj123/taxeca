package com.taxeca.calculator.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.taxeca.calculator.BuildConfig
import com.google.android.play.core.review.ReviewManagerFactory
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: AnalyticsManager
) {
    private companion object {
        const val TAG = "ReviewManager"
        const val PREF_KEY = "review_v2"
        const val CALC_COUNT_KEY = "review_calc_count"
        const val MIN_CALCS_BEFORE_REVIEW = 5
    }

    private val prefs = context.getSharedPreferences("taxeca_review", Context.MODE_PRIVATE)

    /** Call on every calculation to track usage before review prompt. */
    fun trackCalculation() {
        val count = prefs.getInt(CALC_COUNT_KEY, 0) + 1
        prefs.edit().putInt(CALC_COUNT_KEY, count).apply()
    }

    fun maybeRequestReview(activity: Activity) {
        if (prefs.getBoolean(PREF_KEY, false)) return
        // Require minimum usage before prompting
        if (prefs.getInt(CALC_COUNT_KEY, 0) < MIN_CALCS_BEFORE_REVIEW) return
        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                prefs.edit().putBoolean(PREF_KEY, true).apply()
                analytics.logReviewRequested()
                manager.launchReviewFlow(activity, task.result)
                    .addOnCompleteListener {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Review flow completed")
                    }
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Review request failed: ${task.exception}")
            }
        }
    }
}
