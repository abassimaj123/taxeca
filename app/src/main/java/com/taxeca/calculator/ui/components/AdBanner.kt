package com.taxeca.calculator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.taxeca.calculator.ui.ads.AdConfig
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel

/**
 * Adaptive anchored banner — automatically scales to the screen width and
 * orientation, yielding higher eCPM than a fixed 320×50 banner.
 * No-op when ADS_ENABLED = false.
 *
 * The [AdView] is created once (remembered) and explicitly destroyed when the
 * composable leaves composition — the bottom bar mounts/unmounts on every
 * navigation, so without this the abandoned AdView would keep requesting in the
 * background (memory leak + degraded eCPM/viewability). Lifecycle pause/resume
 * stops impressions while the app is backgrounded.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!AdConfig.ADS_ENABLED) return
    val freemiumVm = LocalFreemiumViewModel.current
    val isPremium by freemiumVm.isPremium.collectAsStateWithLifecycle()
    val isRewardedActive by freemiumVm.isRewardedActive.collectAsStateWithLifecycle()
    if (isPremium || isRewardedActive) return

    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val lifecycleOwner = LocalLifecycleOwner.current

    val adView = remember {
        AdView(context).apply {
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
            )
            adUnitId = AdConfig.BANNER_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() = freemiumVm.logBannerLoaded()
                override fun onAdFailedToLoad(error: LoadAdError) = freemiumVm.logBannerLoadFailed()
            }
            onPaidEventListener = OnPaidEventListener { value ->
                freemiumVm.logBannerPaid(value.valueMicros, value.currencyCode, value.precisionType.toString())
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    // Pause/resume with the host lifecycle; destroy on leaving composition.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE  -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth()
        // No fixed height — the adaptive size determines it automatically
    )
}
