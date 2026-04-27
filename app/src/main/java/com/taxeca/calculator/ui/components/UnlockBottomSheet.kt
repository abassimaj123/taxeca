package com.taxeca.calculator.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.R
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockBottomSheet(onDismiss: () -> Unit) {
    val freemiumVm    = LocalFreemiumViewModel.current
    val isPremium     by freemiumVm.isPremium.collectAsStateWithLifecycle()
    val isRewarded    by freemiumVm.isRewardedActive.collectAsStateWithLifecycle()
    val timeLeftMs    by freemiumVm.rewardedTimeLeftMs.collectAsStateWithLifecycle()
    val isLoading     by freemiumVm.isLoadingAd.collectAsStateWithLifecycle()
    val canWatch      by freemiumVm.canWatchRewarded.collectAsStateWithLifecycle()
    val adUnavailable by freemiumVm.adUnavailable.collectAsStateWithLifecycle()
    val iapError      by freemiumVm.iapError.collectAsStateWithLifecycle()
    val premiumPrice  by freemiumVm.premiumPrice.collectAsStateWithLifecycle()
    val context       = LocalContext.current
    val activity      = context as? Activity

    val isHardPaywall by freemiumVm.isHardPaywall.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    // Auto-dismiss when access is granted
    LaunchedEffect(isPremium, isRewarded) {
        if (isPremium || isRewarded) onDismiss()
    }

    // Show toast on ad unavailable
    LaunchedEffect(adUnavailable) {
        if (adUnavailable) {
            Toast.makeText(context, context.getString(R.string.unlock_ad_unavailable),
                Toast.LENGTH_SHORT).show()
            freemiumVm.clearAdUnavailable()
        }
    }

    // Show toast on IAP error
    LaunchedEffect(iapError) {
        iapError?.let {
            Toast.makeText(context, context.getString(R.string.iap_error_generic),
                Toast.LENGTH_SHORT).show()
            freemiumVm.clearIapError()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Icon ──────────────────────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isRewarded) Icons.Default.LockOpen
                                      else            Icons.Outlined.Lock,
                        contentDescription = stringResource(
                            if (isRewarded) R.string.content_desc_unlocked
                            else            R.string.content_desc_locked
                        ),
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── Title + benefits ──────────────────────────────────────────
            Text(
                text       = stringResource(R.string.unlock_sheet_title),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                listOf(
                    R.string.benefit_no_ads,
                    R.string.benefit_unlimited_history,
                    R.string.benefit_all_provinces,
                    R.string.benefit_share_export,
                ).forEach { resId ->
                    Row(
                        modifier          = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text  = stringResource(resId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // ── Active session chip ───────────────────────────────────────
            if (isRewarded) {
                val minsLeft = (timeLeftMs / 60_000).toInt()
                Surface(
                    shape  = RoundedCornerShape(20.dp),
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        text     = stringResource(R.string.unlock_access_active, minsLeft),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Watch Ad tile ─────────────────────────────────────────────
            if (!isRewarded) {
                _UnlockTile(
                    icon     = Icons.Default.PlayCircleOutline,
                    title    = stringResource(R.string.freemium_watch_ad),
                    subtitle = stringResource(R.string.unlock_watch_subtitle),
                    enabled  = canWatch && !isLoading,
                    loading  = isLoading,
                    onClick  = { freemiumVm.watchRewardedForBonus(context) },
                )
                Spacer(Modifier.height(10.dp))
            }

            // ── Get Premium tile ──────────────────────────────────────────
            _UnlockTile(
                icon     = Icons.Default.StarOutline,
                title    = premiumPrice
                    ?.let { "⭐ Get Premium — $it" }
                    ?: stringResource(R.string.premium_buy_btn),
                subtitle = stringResource(R.string.unlock_premium_subtitle),
                enabled  = !isLoading,
                onClick  = { activity?.let { freemiumVm.buyPremium(it) } },
            )

            // ── Restore ───────────────────────────────────────────────────
            TextButton(onClick = { activity?.let { freemiumVm.restorePurchases(it) } }) {
                Text(
                    text  = stringResource(R.string.premium_restore_btn),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Maybe later — 50% opacity for hard paywall (sessions 7+) ────
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.alpha(if (isHardPaywall) 0.5f else 1f)
            ) {
                Text(
                    text  = stringResource(R.string.unlock_maybe_later),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Option tile ────────────────────────────────────────────────────────────────
@Composable
private fun _UnlockTile(
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    enabled:  Boolean,
    onClick:  () -> Unit,
    loading:  Boolean = false,
) {
    val primary      = MaterialTheme.colorScheme.primary
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Surface(
        modifier      = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f),
        shape         = RoundedCornerShape(14.dp),
        border        = BorderStroke(
            1.dp,
            if (enabled) primary.copy(alpha = 0.5f)
            else         MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        ),
        color         = surfaceColor,
        contentColor  = onSurface,
    ) {
        Row(
            modifier          = Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = primary.copy(alpha = 0.12f)) {
                Box(
                    modifier         = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = title, tint = primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = onSurface)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.ChevronRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
