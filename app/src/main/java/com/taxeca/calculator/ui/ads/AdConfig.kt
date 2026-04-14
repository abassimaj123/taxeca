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

    /** Mettre à true quand les IDs AdMob de production sont configurés. */
    const val ADS_ENABLED = false

    // ── App ID (also declared in build.gradle.kts as manifestPlaceholder) ─────
    val APP_ID = if (isDebug)
        "ca-app-pub-3940256099942544~3347511713"   // Google test App ID
    else
        "TODO_ADMOB_APP_ID_PRODUCTION"              // ← replace before release

    // ── Banner ────────────────────────────────────────────────────────────────
    val BANNER_ID = if (isDebug)
        "ca-app-pub-3940256099942544/6300978111"
    else
        "TODO_ADMOB_BANNER_ID_PRODUCTION"           // ← replace before release

    // ── Interstitiel ──────────────────────────────────────────────────────────
    val INTERSTITIAL_ID = if (isDebug)
        "ca-app-pub-3940256099942544/1033173712"
    else
        "TODO_ADMOB_INTERSTITIAL_ID_PRODUCTION"     // ← replace before release

    // ── Rewarded ──────────────────────────────────────────────────────────────
    val REWARDED_ID = if (isDebug)
        "ca-app-pub-3940256099942544/5224354917"
    else
        "TODO_ADMOB_REWARDED_ID_PRODUCTION"         // ← replace before release
}
