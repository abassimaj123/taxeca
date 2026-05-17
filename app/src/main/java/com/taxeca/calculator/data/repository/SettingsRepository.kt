package com.taxeca.calculator.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val lastProvinceKey = stringPreferencesKey("last_province_code")
    private val themeModeKey    = stringPreferencesKey("theme_mode")

    val lastProvinceCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[lastProvinceKey] ?: "QC"
    }

    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "auto"
    }

    suspend fun saveLastProvince(code: String) {
        dataStore.edit { prefs ->
            prefs[lastProvinceKey] = code
        }
    }

    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[themeModeKey] = mode
        }
    }
}
