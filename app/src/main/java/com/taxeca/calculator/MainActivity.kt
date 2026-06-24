package com.taxeca.calculator

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentRequestParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                        if (BuildConfig.DEBUG) Log.w("UMP", "Consent form error: ${formError.message}")
                    }
                    // Initialize ads only if consent obtained OR not required
                    if (consentInfo.canRequestAds()) {
                        initializeAds()
                    }
                }
            },
            { requestError ->
                // Request failed — proceed conservatively without ads
                if (BuildConfig.DEBUG) Log.w("UMP", "Consent info update failed: ${requestError.message}")
            }
        )
    }

    private fun initializeAds() {
        if (!AdConfig.ADS_ENABLED) return
        // Register QA/dev devices so real-ad impressions during internal release
        // testing aren't counted as invalid clicks (AdMob account-safety).
        // The hash is NOT the adb serial — copy it from logcat on first ad load:
        //   "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList(\"<HASH>\"))"
        // then paste it into TEST_DEVICE_IDS for release QA builds.
        if (TEST_DEVICE_IDS.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(TEST_DEVICE_IDS).build()
            )
        }
        // initialize() does disk/network I/O on first run — keep it off the UI thread
        // to avoid startup jank.
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
        }
    }

    private companion object {
        // Paste AdMob test-device hashes (from logcat) for release QA. Empty = none.
        // Debug builds already use Google test ad-unit IDs, so no real impressions there.
        val TEST_DEVICE_IDS = emptyList<String>()
    }
}
