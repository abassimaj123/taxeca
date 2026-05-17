package com.taxeca.calculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.repository.LanguageManager
import com.taxeca.calculator.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val languageManager: LanguageManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "auto")

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.saveThemeMode(mode) }
    }
}
