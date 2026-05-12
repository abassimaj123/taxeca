package com.taxeca.calculator.ui.ads

import com.taxeca.calculator.BuildConfig

/**
 * Centralized AdMob configuration.
 *
 * Debug builds use Google's official test IDs (safe to commit).
 * Release builds use production IDs — replace the TODO strings before publishing.
 *
 * Also update the manifest placeholder in build.gradle.kts:
 *   release { manifestPlaceholders["admobAppId"] = "<REAL_APP_ID>" }
 */
object AdConfig {
    private val isDebug = BuildConfig.DEBUG

    /** true = ads actives. En debug, IDs test Google sont utilisés (pas de trafic réel). */
    val ADS_ENABLED = !isDebug

    // ── App ID (also declared in build.gradle.kts as manifestPlaceholder) ─────
    val APP_ID = if (isDebug)
        "ca-app-pub-3940256099942544~3347511713"   // Google test App ID
    else
        BuildConfig.ADMOB_APP_ID                    // Injected via gradle buildConfigField

    // ── Banner ────────────────────────────────────────────────────────────────
    val BANNER_ID = if (isDebug)
        "ca-app-pub-3940256099942544/6300978111"
    else
        BuildConfig.ADMOB_BANNER_ID                // Injected via gradle buildConfigField

    // ── Interstitiel ──────────────────────────────────────────────────────────
    val INTERSTITIAL_ID = if (isDebug)
        "ca-app-pub-3940256099942544/1033173712"
    else
        BuildConfig.ADMOB_INTERSTITIAL_ID          // Injected via gradle buildConfigField

    // ── Rewarded ──────────────────────────────────────────────────────────────
    val REWARDED_ID = if (isDebug)
        "ca-app-pub-3940256099942544/5224354917"
    else
        BuildConfig.ADMOB_REWARDED_ID              // Injected via gradle buildConfigField
}
