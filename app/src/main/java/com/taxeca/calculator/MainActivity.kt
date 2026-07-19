package com.taxeca.calculator

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.taxeca.calculator.data.repository.LanguageManager
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
            ?: if (Locale.getDefault().language == "fr") LanguageManager.LANG_FR else LanguageManager.LANG_EN
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
        requestConsent()
    }

    // MobileAds.initialize() now lives in TaxeCAApplication.onCreate(), unconditional
    // and ahead of any ViewModel — see the comment there for why. This only handles
    // showing the UMP consent form itself, which needs an Activity to render into.
    private fun requestConsent() {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(this)
        consentInfo.requestConsentInfoUpdate(this, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                    if (formError != null) {
                        if (BuildConfig.DEBUG) Log.w("UMP", "Consent form error: ${formError.message}")
                    }
                }
            },
            { requestError ->
                if (BuildConfig.DEBUG) Log.w("UMP", "Consent info update failed: ${requestError.message}")
            }
        )
    }
}
