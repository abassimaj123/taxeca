package com.taxeca.calculator

import android.app.Application
import android.os.Bundle
import com.google.android.gms.ads.MobileAds
import com.taxeca.calculator.ui.ads.AdConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class TaxeCAApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (AdConfig.ADS_ENABLED) MobileAds.initialize(this)

        // Crashlytics — désactivé en debug pour éviter le bruit
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Langue de l'app — loggée une fois au démarrage
        FirebaseAnalytics.getInstance(this).logEvent("app_language", Bundle().apply {
            putString("language", Locale.getDefault().language)
        })
    }
}
