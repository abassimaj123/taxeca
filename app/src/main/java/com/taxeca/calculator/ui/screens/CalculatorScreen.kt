package com.taxeca.calculator.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.R
import com.taxeca.calculator.domain.model.TaxResult
import com.taxeca.calculator.ui.components.PremiumBannerSection
import com.taxeca.calculator.ui.components.UnlockBottomSheet
import com.taxeca.calculator.ui.components.GradientButton
import com.taxeca.calculator.ui.components.ModeToggle
import com.taxeca.calculator.ui.components.PillButton
import com.taxeca.calculator.ui.components.ProvinceSelector
import com.taxeca.calculator.ui.components.ResultCard
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import com.taxeca.calculator.ui.theme.AccentGreen
import com.taxeca.calculator.ui.theme.AccentGreenDark
import com.taxeca.calculator.ui.viewmodel.CalculatorViewModel
import com.taxeca.calculator.ui.viewmodel.TIP_CUSTOM
import com.taxeca.calculator.ui.viewmodel.TIP_PRESETS
import com.taxeca.calculator.ui.utils.getDefaultEnterTransition
import com.taxeca.calculator.ui.utils.getDefaultExitTransition
import com.taxeca.calculator.utils.CurrencyFormatter

private val SPLIT_PRESETS = listOf(2, 3, 4, 5)

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val context          = LocalContext.current
    val freemiumVm       = LocalFreemiumViewModel.current
    val amountInput      by viewModel.amountInput.collectAsStateWithLifecycle()
    val selectedProvince by viewModel.selectedProvince.collectAsStateWithLifecycle()
    val mode             by viewModel.mode.collectAsStateWithLifecycle()
    val taxResult        by viewModel.taxResult.collectAsStateWithLifecycle()
    val isInputValid     by viewModel.isInputValid.collectAsStateWithLifecycle()
    val tipEnabled       by viewModel.tipEnabled.collectAsStateWithLifecycle()
    val tipPreset        by viewModel.tipPreset.collectAsStateWithLifecycle()
    val customTipInput      by viewModel.customTipInput.collectAsStateWithLifecycle()
    val customTipIsAmount   by viewModel.customTipIsAmount.collectAsStateWithLifecycle()
    val tipAmount        by viewModel.tipAmount.collectAsStateWithLifecycle()
    val grandTotal       by viewModel.grandTotal.collectAsStateWithLifecycle()
    val splitEnabled     by viewModel.splitEnabled.collectAsStateWithLifecycle()
    val splitCount       by viewModel.splitCount.collectAsStateWithLifecycle()
    val customSplitInput by viewModel.customSplitInput.collectAsStateWithLifecycle()
    val perPerson        by viewModel.perPerson.collectAsStateWithLifecycle()
    val saveConfirmed    by viewModel.saveConfirmed.collectAsStateWithLifecycle()

    val accentGreen = if (isSystemInDarkTheme()) AccentGreenDark else AccentGreen

    var showUnlockSheet  by remember { mutableStateOf(false) }
    var pendingShareData by remember { mutableStateOf<String?>(null) }

    val hasResult = taxResult != null && isInputValid
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.logScreenView() }

    // Show snackbar when calculation is saved
    LaunchedEffect(saveConfirmed) {
        if (saveConfirmed) {
            snackbarHostState.showSnackbar(context.getString(R.string.save_success_message))
        }
    }

    // Count each settled calculation toward the interstitial-ad frequency gate.
    // Was previously wired to Save/Share only, which most users never tap —
    // this key already debounces via CalculatorViewModel.scheduleCalculation's
    // 300ms delay, so it doesn't fire per-keystroke.
    LaunchedEffect(taxResult) {
        if (taxResult != null) freemiumVm.trackCalculation(context)
    }

    // recordAction() called on Save button and tab navigation (not on every auto-recalculate)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            ProvinceSelector(
                selectedProvince   = selectedProvince,
                onProvinceSelected = viewModel::onProvinceChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ModeToggle(
                selectedMode  = mode,
                onModeSelected = viewModel::onModeChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            val amountLabel = if (mode == com.taxeca.calculator.domain.model.CalculationMode.REVERSE) {
                stringResource(R.string.hint_enter_amount_ttc)
            } else {
                stringResource(R.string.hint_enter_amount)
            }
            OutlinedTextField(
                value         = amountInput,
                onValueChange = viewModel::onAmountChange,
                label         = { Text(amountLabel) },
                prefix        = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                isError       = amountInput.isNotBlank() && !isInputValid,
                supportingText = {
                    if (amountInput.isNotBlank() && !isInputValid) {
                        Text(stringResource(R.string.error_invalid_amount))
                    }
                },
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // Tax result card
        item { ResultCard(result = taxResult, modifier = Modifier.fillMaxWidth()) }

        // ── Tip toggle card ────────────────────────────────────────────────────
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Savings,
                                contentDescription = stringResource(R.string.desc_tip_icon),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.calculator_tip_toggle),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                        }
                        Switch(checked = tipEnabled, onCheckedChange = viewModel::onTipToggle)
                    }

                    AnimatedVisibility(visible = tipEnabled,
                        enter = getDefaultEnterTransition(),
                        exit  = getDefaultExitTransition()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TIP_PRESETS.forEach { preset ->
                                    PillButton(
                                        label    = "${(preset * 100).toInt()}%",
                                        selected = tipPreset == preset,
                                        onClick  = { viewModel.onTipPresetSelected(preset) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                PillButton(
                                    label    = stringResource(R.string.restaurant_tip_custom),
                                    selected = tipPreset == TIP_CUSTOM,
                                    onClick  = { viewModel.onTipPresetSelected(TIP_CUSTOM) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            AnimatedVisibility(visible = tipPreset == TIP_CUSTOM,
                                enter = getDefaultEnterTransition(),
                                exit  = getDefaultExitTransition()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        value = customTipInput,
                                        onValueChange = viewModel::onCustomTipChange,
                                        label = { Text(stringResource(R.string.calculator_tip_percent)) },
                                        prefix = if (customTipIsAmount) { { Text("$") } } else null,
                                        suffix = if (!customTipIsAmount) { { Text("%") } } else null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Tip + grand total summary
                    AnimatedVisibility(visible = tipEnabled && hasResult,
                        enter = getDefaultEnterTransition(),
                        exit  = getDefaultExitTransition()) {
                        taxResult?.let { result ->
                            TipSummary(result = result, tipAmount = tipAmount, grandTotal = grandTotal)
                        }
                    }
                }
            }
        }

        // ── Split toggle card ──────────────────────────────────────────────────
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.CallSplit,
                                contentDescription = stringResource(R.string.desc_split_icon),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.calculator_split_toggle),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                        }
                        Switch(checked = splitEnabled, onCheckedChange = viewModel::onSplitToggle)
                    }

                    AnimatedVisibility(visible = splitEnabled,
                        enter = getDefaultEnterTransition(),
                        exit  = getDefaultExitTransition()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SPLIT_PRESETS.forEach { count ->
                                    val isSelected = splitCount == count && customSplitInput.isEmpty()
                                    PillButton(
                                        label    = "÷$count",
                                        selected = isSelected,
                                        onClick  = { viewModel.onSplitCountSelected(count) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                PillButton(
                                    label    = stringResource(R.string.restaurant_tip_custom),
                                    selected = customSplitInput.isNotEmpty() || splitCount > 5,
                                    onClick  = { viewModel.onSplitCountSelected(6) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            AnimatedVisibility(visible = customSplitInput.isNotEmpty() || splitCount > 5,
                                enter = getDefaultEnterTransition(),
                                exit  = getDefaultExitTransition()) {
                                OutlinedTextField(
                                    value = customSplitInput,
                                    onValueChange = viewModel::onCustomSplitChange,
                                    label = { Text(stringResource(R.string.restaurant_split_custom_hint)) },
                                    prefix = { Text("÷") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Per-person total
                    AnimatedVisibility(visible = splitEnabled && hasResult,
                        enter = getDefaultEnterTransition(),
                        exit  = getDefaultExitTransition()) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text  = stringResource(R.string.calculator_per_person, splitCount),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text       = CurrencyFormatter.formatAmount(perPerson),
                                    style      = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = accentGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Save + Share buttons ───────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = {
                        viewModel.saveToHistory()
                        freemiumVm.recordAction()
                        freemiumVm.maybeRequestReview(context as android.app.Activity)
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
                    text     = stringResource(R.string.btn_share),
                    onClick  = {
                        taxResult?.let { r ->
                            val text = buildCalculatorShareText(
                                r, tipEnabled, tipAmount, grandTotal,
                                splitEnabled, splitCount, perPerson
                            )
                            pendingShareData = text
                            showUnlockSheet  = true
                        }
                    },
                    enabled  = hasResult,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )
            }
        }

        item { PremiumBannerSection(modifier = Modifier.fillMaxWidth()) }
        item { Spacer(Modifier.height(16.dp)) }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }

    if (showUnlockSheet) {
        UnlockBottomSheet(onDismiss = {
            showUnlockSheet = false
            val text = pendingShareData
            if (text != null && freemiumVm.hasAccess.value) {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }, null
                    )
                )
                taxResult?.let { r ->
                    viewModel.logShareResult(r.province.code, "text")
                    viewModel.saveToHistory()
                    viewModel.resetDisplay()
                }
                pendingShareData = null
            }
        })
    }
}

// ── Tip summary section ───────────────────────────────────────────────────────

@Composable
private fun TipSummary(result: TaxResult, tipAmount: Double, grandTotal: Double) {
    val accentGreen = if (isSystemInDarkTheme()) AccentGreenDark else AccentGreen
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        TipRow(stringResource(R.string.label_base_amount), CurrencyFormatter.formatAmount(result.baseAmount))
        TipRow(stringResource(R.string.restaurant_tip_label), CurrencyFormatter.formatAmount(tipAmount))
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.label_total),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(CurrencyFormatter.formatAmount(grandTotal),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold, color = accentGreen)
        }
    }
}

@Composable
private fun TipRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// ── Share text ────────────────────────────────────────────────────────────────

private fun buildCalculatorShareText(
    r: TaxResult,
    tipEnabled: Boolean,
    tipAmount: Double,
    grandTotal: Double,
    splitEnabled: Boolean,
    splitCount: Int,
    perPerson: Double
): String {
    val sep = "─────────────────────"
    val fmt: (Double) -> String = { CurrencyFormatter.formatAmount(it) }
    val pct: (Double) -> String = { CurrencyFormatter.formatPercent(it) }

    return buildString {
        val modeLabel = if (r.mode == com.taxeca.calculator.domain.model.CalculationMode.REVERSE) "TTC" else "HT"
        appendLine("📋 TaxeCA — Calculatrice ($modeLabel)")
        appendLine("${r.province.nameFr} / ${r.province.nameEn} (${r.province.code})")
        appendLine(sep)
        appendLine(lv("Montant HT / Base Amount", fmt(r.baseAmount)))
        if (!r.province.isHstProvince && r.province.gstRate > 0)
            appendLine(lv("TPS/GST (${pct(r.province.gstRate)})", fmt(r.gstAmount)))
        if (!r.province.isHstProvince && r.province.pstRate > 0) {
            val tag = when (r.province.pstLabel) { "QST" -> "TVQ/QST"; "RST" -> "RST"; else -> "PST" }
            appendLine(lv("$tag (${pct(r.province.pstRate)})", fmt(r.pstAmount)))
        }
        if (r.province.isHstProvince)
            appendLine(lv("TVH/HST (${pct(r.province.hstRate)})", fmt(r.hstAmount)))
        if (tipEnabled && tipAmount > 0)
            appendLine(lv("Pourboire / Tip", fmt(tipAmount)))
        appendLine(sep)
        appendLine(lv("TOTAL", fmt(grandTotal)))
        if (splitEnabled && splitCount >= 2)
            appendLine(lv("Par personne / Per person (÷$splitCount)", fmt(perPerson)))
        appendLine(sep)
        append("Calculé avec TaxeCA 🇨🇦")
    }
}

private fun lv(label: String, value: String): String {
    val col = 28
    return "$label${" ".repeat((col - label.length).coerceAtLeast(1))}$value"
}
