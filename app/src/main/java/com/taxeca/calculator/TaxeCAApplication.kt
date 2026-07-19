package com.taxeca.calculator

import android.app.Application
import android.os.Bundle
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.taxeca.calculator.ui.ads.AdConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

@HiltAndroidApp
class TaxeCAApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // MobileAds.initialize() used to be called from MainActivity, gated behind the
        // async UMP consent callback — but FreemiumViewModel's preloadInterstitial()
        // runs synchronously during Compose's first composition in MainActivity.onCreate(),
        // which fires an ad load BEFORE that callback (and therefore before the SDK
        // finished initializing). That race is the likely cause of an internal NPE
        // (com.google.android.gms.internal.ads.zzeds.zze) seen in Crashlytics across
        // 1.0.7-1.0.10. initialize() itself doesn't request ads or transmit personal
        // data, so it's safe to call unconditionally here, before any ViewModel exists
        // — only the UMP consent FORM (shown to the user) still needs to stay in
        // MainActivity, since it needs an Activity to render into.
        if (AdConfig.ADS_ENABLED) {
            if (TEST_DEVICE_IDS.isNotEmpty()) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder().setTestDeviceIds(TEST_DEVICE_IDS).build()
                )
            }
            // initialize() does disk/network I/O on first run — keep it off the main thread.
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                MobileAds.initialize(this@TaxeCAApplication) {}
            }
        }

        // Crashlytics — désactivé en debug pour éviter le bruit.
        // Crashlytics installe son propre handler d'exceptions non-catchées (remontées
        // en FATAL). On ne pose PAS de handler custom : recordException() les logge en
        // non-fatal, ce qui doublonne le crash et fausse le crash-free rate.
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Langue de l'app + app_open loggés au démarrage
        val fa = FirebaseAnalytics.getInstance(this)
        fa.logEvent("app_open", null)
        fa.logEvent("app_language", Bundle().apply {
            putString("language", Locale.getDefault().language)
        })
    }

    private companion object {
        // Paste AdMob test-device hashes (from logcat) for release QA. Empty = none.
        // Debug builds already use Google test ad-unit IDs, so no real impressions there.
        val TEST_DEVICE_IDS = emptyList<String>()
    }
}
