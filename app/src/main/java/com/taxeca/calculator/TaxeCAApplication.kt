package com.taxeca.calculator

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class TaxeCAApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // MobileAds.initialize() is gated on UMP consent — called from MainActivity

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
}
