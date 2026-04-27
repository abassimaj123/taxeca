package com.taxeca.calculator.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreemiumRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val MAX_REWARDED_PER_DAY  = 2
        private const val SESSION_MIN_GAP_MS = 2 * 60 * 1000L // 2 min — prevents force-close cycling
    }

    private val KEY_REWARDED_AT    = longPreferencesKey("rewarded_unlocked_at")
    private val KEY_REWARDED_DAY   = intPreferencesKey("rewarded_day_of_year")
    private val KEY_REWARDED_COUNT = intPreferencesKey("rewarded_count_today")
    private val KEY_SESSION_COUNT  = intPreferencesKey("paywall_session_count")
    private val KEY_LAST_SESSION_MS = longPreferencesKey("last_session_timestamp_ms")

    val sessionCount: Flow<Int> = dataStore.data.map { it[KEY_SESSION_COUNT] ?: 0 }

    suspend fun incrementSession(): Int {
        var newCount = 0
        dataStore.edit { prefs ->
            val lastMs = prefs[KEY_LAST_SESSION_MS] ?: 0L
            val now    = System.currentTimeMillis()
            if (now - lastMs < SESSION_MIN_GAP_MS) {
                // Too soon after last session — don't count as a new session
                newCount = prefs[KEY_SESSION_COUNT] ?: 0
            } else {
                newCount = (prefs[KEY_SESSION_COUNT] ?: 0) + 1
                prefs[KEY_SESSION_COUNT]   = newCount
                prefs[KEY_LAST_SESSION_MS] = now
            }
        }
        return newCount
    }

    val rewardedUnlockedAt: Flow<Long> = dataStore.data.map { it[KEY_REWARDED_AT] ?: 0L }

    /** How many rewarded watches have been used today (resets at midnight). */
    val rewardedCountToday: Flow<Int> = dataStore.data.map { prefs ->
        val savedDay = prefs[KEY_REWARDED_DAY] ?: -1
        if (savedDay == todayOfYear()) prefs[KEY_REWARDED_COUNT] ?: 0 else 0
    }

    fun isRewardedActive(rewardedAt: Long): Boolean {
        if (rewardedAt == 0L) return false
        return System.currentTimeMillis() - rewardedAt < 60L * 60 * 1000
    }

    /** Returns true only if the user can watch a rewarded ad right now:
     *  - current session must be EXPIRED (no extending while active)
     *  - daily limit not yet reached
     */
    suspend fun canWatchRewarded(): Boolean {
        val prefs    = dataStore.data.first()
        val rewardedAt = prefs[KEY_REWARDED_AT] ?: 0L
        if (isRewardedActive(rewardedAt)) return false   // already active — no extend

        val savedDay = prefs[KEY_REWARDED_DAY] ?: -1
        val count    = if (savedDay == todayOfYear()) prefs[KEY_REWARDED_COUNT] ?: 0 else 0
        return count < MAX_REWARDED_PER_DAY
    }

    suspend fun recordRewardedWatch() {
        val today = todayOfYear()
        dataStore.edit { prefs ->
            val savedDay = prefs[KEY_REWARDED_DAY] ?: -1
            val count    = if (savedDay == today) prefs[KEY_REWARDED_COUNT] ?: 0 else 0
            prefs[KEY_REWARDED_AT]    = System.currentTimeMillis()
            prefs[KEY_REWARDED_DAY]   = today
            prefs[KEY_REWARDED_COUNT] = count + 1
        }
    }

    private fun todayOfYear(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
}
