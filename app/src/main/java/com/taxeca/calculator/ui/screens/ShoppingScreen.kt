package com.taxeca.calculator.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.R
import com.taxeca.calculator.domain.model.ShoppingItem
import com.taxeca.calculator.domain.model.ShoppingListResult
import com.taxeca.calculator.ui.components.PremiumBannerSection
import com.taxeca.calculator.ui.components.GradientButton
import com.taxeca.calculator.ui.components.ProvinceSelector
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel
import com.taxeca.calculator.ui.theme.AccentGreen
import com.taxeca.calculator.ui.viewmodel.ShoppingViewModel
import com.taxeca.calculator.utils.CurrencyFormatter
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val context         = LocalContext.current
    val freemiumVm      = LocalFreemiumViewModel.current
    val province        by viewModel.selectedProvince.collectAsStateWithLifecycle()
    val priceInput      by viewModel.priceInput.collectAsStateWithLifecycle()
    val isPriceValid    by viewModel.isPriceValid.collectAsStateWithLifecycle()
    val items           by viewModel.items.collectAsStateWithLifecycle()
    val listResult      by viewModel.listResult.collectAsStateWithLifecycle()
    val showClearDialog by viewModel.showClearDialog.collectAsStateWithLifecycle()
    val saveConfirmed   by viewModel.saveConfirmed.collectAsStateWithLifecycle()

    val hasResult = listResult != null

    // Record a freemium action on every new valid shopping result
    LaunchedEffect(listResult) {
        if (listResult != null) freemiumVm.recordAction()
    }

    val priceFocus = remember { FocusRequester() }

    // ── Clear-all confirmation dialog ─────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClear,
            title   = { Text(stringResource(R.string.shopping_clear_title)) },
            text    = { Text(stringResource(R.string.shopping_clear_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmClear) {
                    Text(
                        stringResource(R.string.shopping_clear_ok),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClear) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Province selector
        item {
            ProvinceSelector(
                selectedProvince   = province,
                onProvinceSelected = viewModel::onProvinceChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Price input row ───────────────────────────────────────────────────
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = priceInput,
                        onValueChange = viewModel::onPriceChange,
                        label         = { Text(stringResource(R.string.shopping_item_price)) },
                        prefix        = { Text("$") },
                        placeholder   = { Text("0.00") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            if (isPriceValid) {
                                viewModel.addItem()
                                priceFocus.requestFocus()
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(priceFocus)
                    )
                    GradientButton(
                        text    = stringResource(R.string.shopping_add_btn),
                        onClick = {
                            if (isPriceValid) {
                                viewModel.addItem()
                                priceFocus.requestFocus()
                            }
                        },
                        enabled  = isPriceValid,
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }

        // ── Empty state hint ──────────────────────────────────────────────────
        if (items.isEmpty()) {
            item {
                Box(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment  = Alignment.Center,
                ) {
                    Text(
                        text  = stringResource(R.string.shopping_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }

        // ── Items list header ─────────────────────────────────────────────────
        if (items.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = stringResource(R.string.shopping_items_count, items.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = viewModel::requestClear) {
                        Icon(
                            imageVector        = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.btn_clear_all),
                            tint               = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ── Each item row — swipe left to delete ──────────────────────────────
        items(items = items, key = { it.id }) { item ->
            SwipeToDeleteItem(
                item     = item,
                onDelete = { viewModel.removeItem(item.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            )
        }

        // ── Summary card ──────────────────────────────────────────────────────
        item {
            AnimatedVisibility(
                visible = listResult != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                listResult?.let { ShoppingSummaryCard(it) }
            }
        }

        // ── Action buttons ────────────────────────────────────────────────────
        item {
            AnimatedVisibility(
                visible = hasResult,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                listResult?.let { result ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick  = {
                                viewModel.saveToHistory()
                                freemiumVm.trackCalculation(context)
                            },
                            enabled  = hasResult && !saveConfirmed,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text(
                                if (saveConfirmed) stringResource(R.string.btn_saved)
                                else stringResource(R.string.btn_save)
                            )
                        }
                        GradientButton(
                            text    = stringResource(R.string.btn_share),
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, buildShareText(result))
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                                viewModel.logShare()
                                freemiumVm.trackCalculation(context)
                                viewModel.saveToHistory()
                                viewModel.resetDisplay()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        )
                    }
                }
            }
        }

        item { PremiumBannerSection(modifier = Modifier.fillMaxWidth()) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Swipe-to-delete item row ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    item: ShoppingItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state                    = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent        = { DeleteBackground() },
        modifier                 = modifier
    ) {
        ItemRow(item = item, onDelete = onDelete)
    }
}

@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector        = Icons.Default.Delete,
            contentDescription = stringResource(R.string.btn_delete),
            tint               = MaterialTheme.colorScheme.onErrorContainer,
            modifier           = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ItemRow(item: ShoppingItem, onDelete: () -> Unit) {
    Surface(
        shape          = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text     = item.displayName,
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text       = CurrencyFormatter.formatAmount(item.price),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(36.dp).padding(start = 4.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_delete),
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun ShoppingSummaryCard(result: ShoppingListResult) {
    val fmt: (Double) -> String = { CurrencyFormatter.formatAmount(it) }
    val pct: (Double) -> String = { CurrencyFormatter.formatPercent(it) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Subtotal
            SummaryRow(stringResource(R.string.shopping_subtotal), fmt(result.subtotal))

            // Tax lines
            if (!result.province.isHstProvince && result.province.gstRate > 0) {
                SummaryRow(
                    "TPS / GST (${pct(result.province.gstRate)})",
                    fmt(result.totalGst)
                )
            }
            if (!result.province.isHstProvince && result.province.pstRate > 0) {
                val label = when (result.province.pstLabel) {
                    "QST" -> "TVQ / QST (${pct(result.province.pstRate)})"
                    "RST" -> "RST (${pct(result.province.pstRate)})"
                    else  -> "PST (${pct(result.province.pstRate)})"
                }
                SummaryRow(label, fmt(result.totalPst))
            }
            if (result.province.isHstProvince) {
                SummaryRow(
                    "TVH / HST (${pct(result.province.hstRate)})",
                    fmt(result.totalHst)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Grand total
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = stringResource(R.string.shopping_grand_total),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text       = fmt(result.grandTotal),
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color      = AccentGreen
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Share text builder ────────────────────────────────────────────────────────

private fun buildShareText(r: ShoppingListResult): String {
    val sep = "─────────────────────"
    val fmt: (Double) -> String = { CurrencyFormatter.formatAmount(it) }
    val pct: (Double) -> String = { CurrencyFormatter.formatPercent(it) }

    return buildString {
        appendLine("🛒 Liste TaxeCA")
        appendLine(sep)
        r.items.forEach { appendLine(labelValue(it.item.displayName, fmt(it.item.price))) }
        appendLine(sep)
        appendLine(labelValue("Sous-total / Subtotal", fmt(r.subtotal)))
        if (!r.province.isHstProvince && r.province.gstRate > 0)
            appendLine(labelValue("TPS/GST (${pct(r.province.gstRate)})", fmt(r.totalGst)))
        if (!r.province.isHstProvince && r.province.pstRate > 0) {
            val tag = when (r.province.pstLabel) { "QST" -> "TVQ/QST"; "RST" -> "RST"; else -> "PST" }
            appendLine(labelValue("$tag (${pct(r.province.pstRate)})", fmt(r.totalPst)))
        }
        if (r.province.isHstProvince)
            appendLine(labelValue("TVH/HST (${pct(r.province.hstRate)})", fmt(r.totalHst)))
        appendLine(sep)
        appendLine(labelValue("TOTAL", fmt(r.grandTotal)))
        appendLine(sep)
        append("Calculé avec TaxeCA 🇨🇦")
    }
}

private fun labelValue(label: String, value: String): String {
    val col     = 26
    val padding = (col - label.length).coerceAtLeast(1)
    return "$label${" ".repeat(padding)}$value"
}
