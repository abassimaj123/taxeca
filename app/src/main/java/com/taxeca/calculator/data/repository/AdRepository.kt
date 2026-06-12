package com.taxeca.calculator.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_CALC_COUNT        = intPreferencesKey("interstitial_calc_count")
        private val KEY_LAST_INTERSTITIAL = longPreferencesKey("last_interstitial_ms")
    }

    suspend fun getCalcCount(): Int =
        dataStore.data.first()[KEY_CALC_COUNT] ?: 0

    suspend fun getLastInterstitialMs(): Long =
        dataStore.data.first()[KEY_LAST_INTERSTITIAL] ?: 0L

    suspend fun saveCalcCount(count: Int) {
        dataStore.edit { it[KEY_CALC_COUNT] = count }
    }

    suspend fun saveLastInterstitialMs(time: Long) {
        dataStore.edit { it[KEY_LAST_INTERSTITIAL] = time }
    }
}
