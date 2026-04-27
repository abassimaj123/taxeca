package com.taxeca.calculator

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    }
}
