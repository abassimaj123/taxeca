package com.taxeca.calculator

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.taxeca.calculator.data.repository.LanguageManager
import com.taxeca.calculator.ui.ads.AdConfig
import com.taxeca.calculator.ui.navigation.AppNavigation
import com.taxeca.calculator.ui.theme.TaxeCATheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language before Hilt/Compose are available
        val prefs  = newBase.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
        val lang   = prefs.getString(LanguageManager.KEY_LANG, null)
            ?: Locale.getDefault().language.takeIf { it == LanguageManager.LANG_FR }
            ?: LanguageManager.LANG_EN
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaxeCATheme {
                AppNavigation()
            }
        }
        requestConsentAndInitAds()
    }

    private fun requestConsentAndInitAds() {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(this)
        consentInfo.requestConsentInfoUpdate(this, params,
            {
                // Success — load consent form if required
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                    if (formError != null) {
                        // Log error but proceed — don't block app
                        Log.w("UMP", "Consent form error: ${formError.message}")
                    }
                    // Initialize ads only if consent obtained OR not required
                    if (consentInfo.canRequestAds()) {
                        initializeAds()
                    }
                }
            },
            { requestError ->
                // Request failed — proceed conservatively without ads
                Log.w("UMP", "Consent info update failed: ${requestError.message}")
            }
        )
    }

    private fun initializeAds() {
        if (AdConfig.ADS_ENABLED) {
            MobileAds.initialize(this) {}
        }
    }
}
