package com.taxeca.calculator.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.R
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel
import androidx.compose.animation.AnimatedVisibility

/**
 * Composite section placed at the bottom of every screen:
 *  1. "🔓 Débloquer 1h premium" button — always visible, triggers rewarded ad voluntarily.
 *  2. Adaptive AdBanner below.
 */
@Composable
fun PremiumBannerSection(modifier: Modifier = Modifier) {
    val freemiumVm = LocalFreemiumViewModel.current
    val isLoading  by freemiumVm.isLoadingAd.collectAsStateWithLifecycle()
    val hasAccess  by freemiumVm.hasAccess.collectAsStateWithLifecycle()
    val context    = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = !hasAccess) {
            Column {
                OutlinedButton(
                    onClick  = { freemiumVm.watchRewardedForBonus(context) },
                    enabled  = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text  = stringResource(R.string.premium_unlock_btn),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        AdBanner()
    }
}
