package com.taxeca.calculator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.taxeca.calculator.ui.ads.AdConfig
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel

/**
 * Adaptive anchored banner — automatically scales to the screen width and
 * orientation, yielding higher eCPM than a fixed 320×50 banner.
 * No-op when ADS_ENABLED = false.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!AdConfig.ADS_ENABLED) return
    val freemiumVm = LocalFreemiumViewModel.current
    val isPremium by freemiumVm.isPremium.collectAsStateWithLifecycle()
    if (isPremium) return
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context, screenWidthDp
                )
                setAdSize(adSize)
                adUnitId = AdConfig.BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier.fillMaxWidth()
        // No fixed height — the adaptive size determines it automatically
    )
}
