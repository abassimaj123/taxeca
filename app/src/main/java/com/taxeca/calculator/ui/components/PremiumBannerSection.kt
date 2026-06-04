package com.taxeca.calculator.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.R
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel

/**
 * Bottom-of-screen section. Adapts to access state:
 *  - Premium:          hidden entirely
 *  - Rewarded active:  countdown chip only
 *  - Free:             single "🔒 Unlock Full Access" button → UnlockBottomSheet
 */
@Composable
fun PremiumBannerSection(modifier: Modifier = Modifier) {
    val freemiumVm       = LocalFreemiumViewModel.current
    val isPremium        by freemiumVm.isPremium.collectAsStateWithLifecycle()
    val isRewardedActive by freemiumVm.isRewardedActive.collectAsStateWithLifecycle()
    val timeLeftMs       by freemiumVm.rewardedTimeLeftMs.collectAsStateWithLifecycle()
    var showSheet        by remember { mutableStateOf(false) }

    if (isPremium) return

    Column(modifier = modifier.fillMaxWidth()) {
        if (isRewardedActive) {
            // ── Countdown only ────────────────────────────────────────────
            val minutes = (timeLeftMs / 60_000).toInt()
            val seconds = ((timeLeftMs % 60_000) / 1000).toInt()
            Text(
                text  = stringResource(R.string.rewarded_countdown, minutes, seconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // ── Single unlock button ──────────────────────────────────────
            OutlinedButton(
                onClick  = { showSheet = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text  = stringResource(R.string.unlock_btn_label),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

    }

    if (showSheet) {
        UnlockBottomSheet(onDismiss = { showSheet = false })
    }
}
