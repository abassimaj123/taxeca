package com.taxeca.calculator

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import kotlin.system.exitProcess

@HiltAndroidApp
class TaxeCAApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // MobileAds.initialize() is gated on UMP consent — called from MainActivity

        // Crashlytics — désactivé en debug pour éviter le bruit
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Hook uncaught exceptions → Crashlytics
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            crashlytics.recordException(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
            exitProcess(1)
        }

        // Langue de l'app + app_open loggés au démarrage
        val fa = FirebaseAnalytics.getInstance(this)
        fa.logEvent("app_open", null)
        fa.logEvent("app_language", Bundle().apply {
            putString("language", Locale.getDefault().language)
        })
    }
}
