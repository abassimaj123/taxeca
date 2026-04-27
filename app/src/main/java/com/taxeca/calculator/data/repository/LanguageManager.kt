package com.taxeca.calculator.data.repository

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PREFS_NAME = "taxeca_language"
        const val KEY_LANG   = "lang"
        const val LANG_FR    = "fr"
        const val LANG_EN    = "en"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Saved language code, or system default if never set. */
    val savedLang: String
        get() = prefs.getString(KEY_LANG, null)
            ?: Locale.getDefault().language.takeIf { it == LANG_FR } ?: LANG_EN

    private val _isFrench = MutableStateFlow(savedLang == LANG_FR)
    val isFrench: StateFlow<Boolean> = _isFrench

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
        _isFrench.value = (lang == LANG_FR)
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
