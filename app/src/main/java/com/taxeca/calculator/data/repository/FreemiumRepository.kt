package com.taxeca.calculator.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.taxeca.calculator.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreemiumRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val KEY_FIRST_LAUNCH      = longPreferencesKey("first_launch_date")
    private val KEY_REWARDED_AT       = longPreferencesKey("rewarded_unlocked_at")
    private val KEY_CALC_COUNT        = intPreferencesKey("calc_count")
    private val KEY_LAST_INTERSTITIAL = longPreferencesKey("last_interstitial_at")

    val firstLaunchDate: Flow<Long>    = dataStore.data.map { it[KEY_FIRST_LAUNCH]  ?: 0L }
    val rewardedUnlockedAt: Flow<Long> = dataStore.data.map { it[KEY_REWARDED_AT]   ?: 0L }

    suspend fun ensureFirstLaunch() {
        dataStore.edit { prefs ->
            if (prefs[KEY_FIRST_LAUNCH] == null) {
                prefs[KEY_FIRST_LAUNCH] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setRewardedUnlockedAt() {
        dataStore.edit { it[KEY_REWARDED_AT] = System.currentTimeMillis() }
    }

    suspend fun incrementCalcCount(): Int {
        var newCount = 0
        dataStore.edit { prefs ->
            newCount = (prefs[KEY_CALC_COUNT] ?: 0) + 1
            prefs[KEY_CALC_COUNT] = newCount
        }
        return newCount
    }

    suspend fun getLastInterstitialAt(): Long =
        dataStore.data.first()[KEY_LAST_INTERSTITIAL] ?: 0L

    suspend fun setLastInterstitialAt() {
        dataStore.edit { it[KEY_LAST_INTERSTITIAL] = System.currentTimeMillis() }
    }

    /**
     * Trial duration:
     *  - DEBUG  → 0 ms  (trial always expired — for testing the freemium gate)
     *  - RELEASE → 7 days
     */
    private val trialDurationMs: Long =
        if (BuildConfig.DEBUG) 0L else 7L * 24 * 60 * 60 * 1000

    fun isTrialActive(firstLaunch: Long): Boolean {
        if (firstLaunch == 0L) return true      // not yet recorded → assume still in trial
        return System.currentTimeMillis() - firstLaunch < trialDurationMs
    }

    fun isRewardedActive(rewardedAt: Long): Boolean {
        if (rewardedAt == 0L) return false
        return System.currentTimeMillis() - rewardedAt < 60L * 60 * 1000
    }
}
