package com.taxeca.calculator.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "ReviewManager"
        const val PREF_KEY = "review_v2"
    }

    private val prefs = context.getSharedPreferences("taxeca_review", Context.MODE_PRIVATE)

    fun maybeRequestReview(activity: Activity) {
        if (prefs.getBoolean(PREF_KEY, false)) return
        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                prefs.edit().putBoolean(PREF_KEY, true).apply()
                manager.launchReviewFlow(activity, task.result)
                    .addOnCompleteListener {
                        Log.d(TAG, "Review flow completed")
                    }
            } else {
                Log.d(TAG, "Review request failed: ${task.exception}")
            }
        }
    }
}
