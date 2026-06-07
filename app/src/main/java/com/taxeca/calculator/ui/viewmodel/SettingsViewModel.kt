package com.taxeca.calculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.taxeca.calculator.data.repository.LanguageManager
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val languageManager: LanguageManager,
    private val analytics: AnalyticsManager
) : ViewModel() {
    fun logScreenView() = analytics.logScreenView("Settings")
}
