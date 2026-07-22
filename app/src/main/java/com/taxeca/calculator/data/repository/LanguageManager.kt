package com.taxeca.calculator.data.repository

import android.content.Context
import android.content.res.Configuration
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: AnalyticsManager
) {
    companion object {
        const val PREFS_NAME = "taxeca_language"
        const val KEY_LANG   = "lang"
        const val LANG_FR    = "fr-CA"
        const val LANG_EN    = "en-CA"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Saved language code, or system default if never set.
     *  Migrates old "fr"/"en" keys to "fr-CA"/"en-CA" transparently. */
    val savedLang: String
        get() {
            val saved = prefs.getString(KEY_LANG, null)
            return when {
                saved == null -> if (Locale.getDefault().language == "fr") LANG_FR else LANG_EN
                saved.startsWith("fr") -> LANG_FR
                else -> LANG_EN
            }
        }

    private val _isFrench = MutableStateFlow(savedLang == LANG_FR)
    val isFrench: StateFlow<Boolean> = _isFrench

    init {
        // Sync once at startup so DAU/retention can be filtered by language from
        // day one, not only after the user explicitly switches.
        analytics.setLanguage(if (_isFrench.value) "fr" else "en")
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
        _isFrench.value = (lang == LANG_FR)
        analytics.setLanguage(if (_isFrench.value) "fr" else "en")
    }

    /** Apply saved locale to a Context (called from attachBaseContext). */
    fun applyLocale(base: Context): Context {
        val lang   = savedLang
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
