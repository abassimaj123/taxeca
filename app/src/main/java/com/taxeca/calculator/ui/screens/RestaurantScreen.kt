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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.R
import com.taxeca.calculator.domain.model.RestaurantResult
import com.taxeca.calculator.domain.model.ShoppingItem
import com.taxeca.calculator.ui.components.PremiumBannerSection
import com.taxeca.calculator.ui.components.GradientButton
import com.taxeca.calculator.ui.components.PillButton
import com.taxeca.calculator.ui.components.ProvinceSelector
import com.taxeca.calculator.ui.components.UnlockBottomSheet
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import com.taxeca.calculator.ui.theme.AccentGreen
import com.taxeca.calculator.ui.theme.AccentGreenDark
import com.taxeca.calculator.ui.utils.getDefaultEnterTransition
import com.taxeca.calculator.ui.utils.getDefaultExitTransition
import com.taxeca.calculator.ui.viewmodel.RestaurantViewModel
import com.taxeca.calculator.ui.viewmodel.TIP_CUSTOM
import com.taxeca.calculator.ui.viewmodel.TIP_PRESETS
import com.taxeca.calculator.utils.CurrencyFormatter

private val SPLIT_PRESETS = listOf(1, 2, 3, 4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantScreen(
    viewModel: RestaurantViewModel = hiltViewModel()
) {
    val ctx              = androidx.compose.ui.platform.LocalContext.current
    val freemiumVm       = LocalFreemiumViewModel.current
    val amountInput      by viewModel.amountInput.collectAsStateWithLifecycle()
    val province         by viewModel.selectedProvince.collectAsStateWithLifecycle()
    val selectedTip      by viewModel.selectedTipPreset.collectAsStateWithLifecycle()
    val customTipInput   by viewModel.customTipInput.collectAsStateWithLifecycle()
    val customTipIsAmount by viewModel.customTipIsAmount.collectAsStateWithLifecycle()
    val splitCount       by viewModel.splitCount.collectAsStateWithLifecycle()
    val customSplitInput by viewModel.customSplitInput.collectAsStateWithLifecycle()
    val result           by viewModel.result.collectAsStateWithLifecycle()
    val isInputValid     by viewModel.isInputValid.collectAsStateWithLifecycle()
    val saveConfirmed    by viewModel.saveConfirmed.collectAsStateWithLifecycle()
    val itemMode         by viewModel.itemMode.collectAsStateWithLifecycle()
    val restaurantItems  by viewModel.restaurantItems.collectAsStateWithLifecycle()
    val itemPriceInput   by viewModel.itemPriceInput.collectAsStateWithLifecycle()
    val isItemPriceValid by viewModel.isItemPriceValid.collectAsStateWithLifecycle()

    val hasResult    = result != null && isInputValid
    val itemPriceFocus = remember { FocusRequester() }

    var showUnlockSheet  by remember { mutableStateOf(false) }
    var pendingShareText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.logScreenView() }

    // Count each settled calculation toward the interstitial-ad frequency gate
    // (was Save/Share-only, which most users never tap — see CalculatorScreen).
    LaunchedEffect(result) {
        if (result != null) freemiumVm.trackCalculation(ctx)
    }

    // recordAction() called on Save button and tab navigation (not on every auto-recalculate)

    val fmt: (Double) -> String = { CurrencyFormatter.formatAmount(it) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Province selector
        item {
            ProvinceSelector(
                selectedProvince   = province,
                onProvinceSelected = viewModel::onProvinceChange,
                modifier           = Modifier.fillMaxWidth()
            )
        }

        // ── Item mode toggle ──────────────────────────────────────────────────
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = stringResource(R.string.desc_list_alt_icon),
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.restaurant_item_mode),
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked         = itemMode,
                        onCheckedChange = viewModel::onItemModeToggle
                    )
                }
            }
        }

        // ── Single-amount input (when NOT in item mode) ───────────────────────
        if (!itemMode) {
            item {
                OutlinedTextField(
                    value           = amountInput,
                    onValueChange   = viewModel::onAmountChange,
                    label           = { Text(stringResource(R.string.restaurant_subtotal_hint)) },
                    prefix          = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    isError         = amountInput.isNotBlank() && !isInputValid,
                    supportingText = {
                        if (amountInput.isNotBlank() && !isInputValid) {
                            Text(stringResource(R.string.error_invalid_amount))
                        }
                    },
                    modifier        = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Item mode: price input + list ─────────────────────────────────────
        if (itemMode) {
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
                            value           = itemPriceInput,
                            onValueChange   = viewModel::onItemPriceChange,
                            label           = { Text(stringResource(R.string.shopping_item_price)) },
                            prefix          = { Text("$") },
                            placeholder     = { Text("0.00") },
                            singleLine      = true,
                            isError         = itemPriceInput.isNotBlank() && !isItemPriceValid,
                            supportingText = {
                                if (itemPriceInput.isNotBlank() && !isItemPriceValid) {
                                    Text(stringResource(R.string.error_invalid_amount))
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction    = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (isItemPriceValid) {
                                    viewModel.addRestaurantItem()
                                    itemPriceFocus.requestFocus()
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(itemPriceFocus)
                        )
                        GradientButton(
                            text     = stringResource(R.string.shopping_add_btn),
                            onClick  = {
                                if (isItemPriceValid) {
                                    viewModel.addRestaurantItem()
                                    itemPriceFocus.requestFocus()
                                }
                            },
                            enabled  = isItemPriceValid,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }

            // Empty state hint
            if (restaurantItems.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = stringResource(R.string.restaurant_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            // Items header
            if (restaurantItems.isNotEmpty()) {
                item {
                    Text(
                        text  = stringResource(R.string.shopping_items_count, restaurantItems.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Item rows — swipe to delete
            items(items = restaurantItems, key = { it.id }) { item ->
                RestaurantItemRow(
                    item     = item,
                    onDelete = { viewModel.removeRestaurantItem(item.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                )
            }

            // Subtotal row
            if (restaurantItems.isNotEmpty()) {
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape     = RoundedCornerShape(10.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.restaurant_items_subtotal),
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                fmt(restaurantItems.sumOf { it.price }),
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color      = AccentGreen
                            )
                        }
                    }
                }
            }
        }

        // ── Tip selector ──────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = stringResource(R.string.restaurant_tip_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TIP_PRESETS.forEach { preset ->
                        PillButton(
                            label    = "${(preset * 100).toInt()}%",
                            selected = selectedTip == preset,
                            onClick  = { viewModel.onTipPresetSelected(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    PillButton(
                        label    = stringResource(R.string.restaurant_tip_custom),
                        selected = selectedTip == TIP_CUSTOM,
                        onClick  = { viewModel.onTipPresetSelected(TIP_CUSTOM) },
                        modifier = Modifier.weight(1f)
                    )
                }
                AnimatedVisibility(visible = selectedTip == TIP_CUSTOM,
                    enter = getDefaultEnterTransition(),
                    exit  = getDefaultExitTransition()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PillButton(
                                label    = "%",
                                selected = !customTipIsAmount,
                                onClick  = { viewModel.onCustomTipModeChange(false) },
                                modifier = Modifier.weight(1f)
                            )
                            PillButton(
                                label    = "$",
                                selected = customTipIsAmount,
                                onClick  = { viewModel.onCustomTipModeChange(true) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value           = customTipInput,
                            onValueChange   = viewModel::onCustomTipChange,
                            label           = { Text(stringResource(R.string.restaurant_tip_custom_hint)) },
                            prefix          = if (customTipIsAmount) { { Text("$") } } else null,
                            suffix          = if (!customTipIsAmount) { { Text("%") } } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine      = true,
                            modifier        = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // ── Split selector ────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = stringResource(R.string.restaurant_split_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SPLIT_PRESETS.forEach { count ->
                        val isSelected = splitCount == count && customSplitInput.isEmpty()
                        PillButton(
                            label    = if (count == 1) "÷1" else "÷$count",
                            selected = isSelected,
                            onClick  = { viewModel.onSplitSelected(count) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    PillButton(
                        label    = stringResource(R.string.restaurant_tip_custom),
                        selected = customSplitInput.isNotEmpty() || splitCount > 4,
                        onClick  = { viewModel.onSplitSelected(5) },
                        modifier = Modifier.weight(1f)
                    )
                }
                AnimatedVisibility(visible = customSplitInput.isNotEmpty() || (splitCount > 4),
                    enter = getDefaultEnterTransition(),
                    exit  = getDefaultExitTransition()) {
                    OutlinedTextField(
                        value           = customSplitInput,
                        onValueChange   = viewModel::onCustomSplitChange,
                        label           = { Text(stringResource(R.string.restaurant_split_custom_hint)) },
                        prefix          = { Text("÷") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── Result card + action buttons ──────────────────────────────────────
        item {
            AnimatedVisibility(
                visible = hasResult,
                enter   = getDefaultEnterTransition(),
                exit    = getDefaultExitTransition()
            ) {
                result?.let { r ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RestaurantResultCard(r)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick  = {
                                    viewModel.saveToHistory()
                                    freemiumVm.recordAction()
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
                                    pendingShareText = buildRestaurantShareText(r)
                                    showUnlockSheet  = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            )
                        }
                    }
                }
            }
        }

        item { PremiumBannerSection(modifier = Modifier.fillMaxWidth()) }
        item { Spacer(Modifier.height(16.dp)) }
    }
    } // end Box

    if (showUnlockSheet) {
        UnlockBottomSheet(onDismiss = {
            showUnlockSheet = false
            val text = pendingShareText
            if (text != null && freemiumVm.hasAccess.value) {
                ctx.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }, null
                    )
                )
                viewModel.logShare()
                viewModel.saveToHistory()
                viewModel.resetDisplay()
                pendingShareText = null
            }
        })
    }
}

// ── Restaurant item row (swipe to delete) ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestaurantItemRow(
    item: ShoppingItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state                       = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent           = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color  = MaterialTheme.colorScheme.errorContainer,
                        shape  = MaterialTheme.shapes.medium
                    )
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp))
            }
        },
        modifier = modifier
    ) {
        Surface(
            shape           = MaterialTheme.shapes.medium,
            tonalElevation  = 3.dp,
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
                    modifier = Modifier
                        .size(36.dp)
                        .padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Result card ───────────────────────────────────────────────────────────────

@Composable
private fun RestaurantResultCard(result: RestaurantResult) {
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
            ResultRow(
                stringResource(R.string.label_base_amount),
                CurrencyFormatter.formatAmount(result.subtotal)
            )

            if (!result.province.isHstProvince && result.province.gstRate > 0) {
                ResultRow(
                    "TPS / GST (${CurrencyFormatter.formatPercent(result.province.gstRate)})",
                    CurrencyFormatter.formatAmount(result.gstAmount)
                )
            }
            if (!result.province.isHstProvince && result.province.pstRate > 0) {
                val label = when (result.province.pstLabel) {
                    "QST" -> "TVQ / QST (${CurrencyFormatter.formatPercent(result.province.pstRate)})"
                    "RST" -> "RST (${CurrencyFormatter.formatPercent(result.province.pstRate)})"
                    else  -> "PST (${CurrencyFormatter.formatPercent(result.province.pstRate)})"
                }
                ResultRow(label, CurrencyFormatter.formatAmount(result.pstAmount))
            }
            if (result.province.isHstProvince) {
                ResultRow(
                    "TVH / HST (${CurrencyFormatter.formatPercent(result.province.hstRate)})",
                    CurrencyFormatter.formatAmount(result.hstAmount)
                )
            }

            val tipLabel = if (result.tipPercent > 0)
                stringResource(R.string.restaurant_tip_with_rate,
                    CurrencyFormatter.formatPercent(result.tipPercent))
            else stringResource(R.string.restaurant_tip_label)
            ResultRow(tipLabel, CurrencyFormatter.formatAmount(result.tipAmount))

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = stringResource(R.string.label_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = CurrencyFormatter.formatAmount(result.total),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) AccentGreenDark else AccentGreen
                )
            }

            if (result.splitCount > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = stringResource(R.string.restaurant_per_person, result.splitCount),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text  = CurrencyFormatter.formatAmount(result.perPerson),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Share text ────────────────────────────────────────────────────────────────

private fun buildRestaurantShareText(r: RestaurantResult): String {
    val sep = "─────────────────────"
    val fmt: (Double) -> String = { CurrencyFormatter.formatAmount(it) }
    val pct: (Double) -> String = { CurrencyFormatter.formatPercent(it) }

    return buildString {
        appendLine("📋 Addition TaxeCA")
        appendLine(sep)
        appendLine(rlv("Sous-total / Subtotal", fmt(r.subtotal)))
        if (!r.province.isHstProvince && r.province.gstRate > 0)
            appendLine(rlv("TPS/GST (${pct(r.province.gstRate)})", fmt(r.gstAmount)))
        if (!r.province.isHstProvince && r.province.pstRate > 0) {
            val tag = when (r.province.pstLabel) { "QST" -> "TVQ/QST"; "RST" -> "RST"; else -> "PST" }
            appendLine(rlv("$tag (${pct(r.province.pstRate)})", fmt(r.pstAmount)))
        }
        if (r.province.isHstProvince)
            appendLine(rlv("TVH/HST (${pct(r.province.hstRate)})", fmt(r.hstAmount)))
        if (r.tipPercent > 0)
            appendLine(rlv("Pourboire / Tip (${pct(r.tipPercent)})", fmt(r.tipAmount)))
        appendLine(sep)
        appendLine(rlv("Total", fmt(r.total)))
        if (r.splitCount > 1)
            appendLine(rlv("Par personne ÷${r.splitCount}", fmt(r.perPerson)))
        appendLine(sep)
        append("Calculé avec TaxeCA 🇨🇦")
    }
}

private fun rlv(label: String, value: String): String {
    val col = 26
    return "$label${" ".repeat((col - label.length).coerceAtLeast(1))}$value"
}
