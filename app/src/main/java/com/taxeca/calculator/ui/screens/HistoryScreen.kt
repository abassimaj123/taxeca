package com.taxeca.calculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.R
import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.ui.components.GradientButton
import com.taxeca.calculator.ui.components.PremiumBannerSection
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel
import com.taxeca.calculator.ui.theme.AccentGreen
import com.taxeca.calculator.ui.viewmodel.HistoryViewModel
import com.taxeca.calculator.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history      by viewModel.history.collectAsStateWithLifecycle()
    val freemiumVm   = LocalFreemiumViewModel.current
    val hasAccess    by freemiumVm.hasAccess.collectAsStateWithLifecycle()
    val isLoadingAd  by freemiumVm.isLoadingAd.collectAsStateWithLifecycle()
    val context      = LocalContext.current

    // ── Confirmation dialogs state ────────────────────────────────────────────
    var pendingDeleteId      by remember { mutableStateOf<Long?>(null) }
    var showDeleteAllDialog  by remember { mutableStateOf(false) }

    // Delete single entry dialog
    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title   = { Text(stringResource(R.string.history_delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId?.let { viewModel.deleteById(it) }
                    pendingDeleteId = null
                }) {
                    Text(
                        stringResource(R.string.btn_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Delete all dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title   = { Text(stringResource(R.string.history_deleteall_title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showDeleteAllDialog = false
                }) {
                    Text(
                        stringResource(R.string.history_deleteall_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.tab_history),
                style = MaterialTheme.typography.titleLarge
            )
            if (history.isNotEmpty() && hasAccess) {
                IconButton(onClick = { showDeleteAllDialog = true }) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.btn_clear_all),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (!hasAccess) {
            // ── Freemium gate ─────────────────────────────────────────────────
            FreemiumGate(
                isLoading = isLoadingAd,
                onWatchAd = { freemiumVm.requestAccess(context) {} }
            )
        } else if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.History, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                items(items = history, key = { it.id }) { entity ->
                    // Swipe triggers confirmation dialog (returns false = snap back)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                pendingDeleteId = entity.id
                            }
                            false   // always snap back — deletion confirmed via dialog
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete, null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        },
                        modifier = Modifier.animateItem()
                    ) {
                        HistoryCompactCard(
                            entity = entity,
                            onClick = { onNavigateToDetail(entity.id) }
                        )
                    }
                }

                item { PremiumBannerSection(modifier = Modifier.fillMaxWidth()) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Freemium gate ─────────────────────────────────────────────────────────────

@Composable
private fun FreemiumGate(isLoading: Boolean, onWatchAd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                Icons.Default.Lock, null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.freemium_gate_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.freemium_gate_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                GradientButton(
                    text    = stringResource(R.string.freemium_watch_ad),
                    onClick = onWatchAd
                )
            }
        }
    }
}

// ── Compact card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCompactCard(entity: HistoryEntity, onClick: () -> Unit) {
    val locale  = Locale.getDefault()
    val isFr    = locale.language == "fr"
    val dateStr = SimpleDateFormat("d MMM yyyy  HH:mm", locale).format(Date(entity.timestamp))

    val icon = when (entity.mode) {
        "SHOPPING"   -> "🛒"
        "RESTAURANT" -> "🍽️"
        else         -> "🧮"
    }
    // Fix: FORWARD shows just "Calculatrice", REVERSE shows "Calculatrice (Inverse)"
    val calcLabel = stringResource(R.string.tab_calculator)
    val typeLabel = when (entity.mode) {
        "SHOPPING"   -> stringResource(R.string.history_mode_shopping)
        "RESTAURANT" -> stringResource(R.string.history_mode_restaurant)
        "FORWARD"    -> calcLabel
        "REVERSE"    -> "$calcLabel (${stringResource(R.string.history_mode_reverse)})"
        else         -> entity.mode
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$icon  $typeLabel",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFr) "Total : ${CurrencyFormatter.formatAmount(entity.totalAmount)}"
                           else "Total: ${CurrencyFormatter.formatAmount(entity.totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
