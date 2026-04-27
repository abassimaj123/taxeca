package com.taxeca.calculator.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.R
import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.ui.components.GradientButton
import com.taxeca.calculator.ui.theme.AccentGreen
import com.taxeca.calculator.ui.viewmodel.HistoryDetailViewModel
import com.taxeca.calculator.utils.CurrencyFormatter
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Parsed item from JSON ─────────────────────────────────────────────────────

private data class SavedItem(val name: String, val price: Double)

private fun parseItems(json: String?): List<SavedItem> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            SavedItem(obj.getString("name"), obj.getDouble("price"))
        }
    } catch (e: Exception) { emptyList() }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun HistoryDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel()
) {
    val entry      by viewModel.entry.collectAsStateWithLifecycle()
    val context    = LocalContext.current
    val entity = entry
    if (entity == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val province     = Province.fromCode(entity.provinceCode)
    val locale       = Locale.getDefault()
    val isFr         = locale.language == "fr"
    val provinceName = if (isFr) province.nameFr else province.nameEn

    val dateFmt = if (isFr) "d MMMM yyyy 'à' HH:mm" else "MMMM d, yyyy 'at' HH:mm"
    val dateStr = SimpleDateFormat(dateFmt, locale).format(Date(entity.timestamp))

    val fmt: (Double) -> String = { CurrencyFormatter.formatAmount(it) }
    val pct: (Double) -> String = { CurrencyFormatter.formatPercent(it) }

    val savedItems = parseItems(entity.itemsJson)

    val icon = when (entity.mode) {
        "SHOPPING"   -> "🛒"
        "RESTAURANT" -> "🍽️"
        else         -> "🧮"
    }
    val calcLabel = stringResource(R.string.tab_calculator)
    val typeLabel = when (entity.mode) {
        "SHOPPING"   -> stringResource(R.string.history_mode_shopping)
        "RESTAURANT" -> stringResource(R.string.history_mode_restaurant)
        "FORWARD"    -> calcLabel
        "REVERSE"    -> "$calcLabel (${stringResource(R.string.history_mode_reverse)})"
        else         -> entity.mode
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // ── Back button ───────────────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.btn_back)
                    )
                }
                Text(
                    text  = stringResource(R.string.btn_back),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text  = "$icon  $typeLabel",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = dateStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = "$provinceName (${entity.provinceCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Items card (Shopping / Restaurant item-mode) ──────────────────────
        if (savedItems.isNotEmpty()) {
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                        Text(
                            text  = stringResource(R.string.history_detail_items),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        savedItems.forEach { item ->
                            DetailEntryRow(
                                label = item.name,
                                value = fmt(item.price)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        DetailEntryRow(
                            label       = stringResource(R.string.shopping_subtotal),
                            value       = fmt(entity.baseAmount),
                            valueWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── Detail card ───────────────────────────────────────────────────────
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                    // REVERSE: show paid amount first
                    if (entity.mode == "REVERSE") {
                        DetailEntryRow(
                            label       = stringResource(R.string.history_detail_paid),
                            value       = fmt(entity.inputAmount),
                            valueWeight = FontWeight.SemiBold
                        )
                    }

                    // Base amount (skipped when items card is shown — subtotal already displayed there)
                    if (savedItems.isEmpty()) {
                        val baseLabel = when (entity.mode) {
                            "SHOPPING"   -> stringResource(R.string.history_detail_items_subtotal)
                            "RESTAURANT" -> stringResource(R.string.history_detail_meal_subtotal)
                            "REVERSE"    -> stringResource(R.string.history_detail_before_tax)
                            else         -> stringResource(R.string.label_base_amount)
                        }
                        DetailEntryRow(baseLabel, fmt(entity.baseAmount))
                    }

                    // Tax lines
                    if (!province.isHstProvince && entity.gstAmount > 0)
                        DetailEntryRow("TPS / GST (${pct(province.gstRate)})", fmt(entity.gstAmount))
                    if (!province.isHstProvince && entity.pstAmount > 0) {
                        val label = when (province.pstLabel) {
                            "QST" -> "TVQ / QST (${pct(province.pstRate)})"
                            "RST" -> "RST (${pct(province.pstRate)})"
                            else  -> "PST (${pct(province.pstRate)})"
                        }
                        DetailEntryRow(label, fmt(entity.pstAmount))
                    }
                    if (province.isHstProvince && entity.hstAmount > 0)
                        DetailEntryRow("TVH / HST (${pct(province.hstRate)})", fmt(entity.hstAmount))

                    DetailEntryRow(
                        stringResource(R.string.label_total_tax),
                        fmt(entity.totalTax),
                        valueWeight = FontWeight.SemiBold
                    )

                    // Restaurant tip
                    if (entity.mode == "RESTAURANT") {
                        val tip = entity.totalAmount - entity.baseAmount - entity.totalTax
                        if (tip > 0.005) {
                            DetailEntryRow(
                                stringResource(R.string.history_detail_tip),
                                fmt(tip),
                                valueWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Grand total
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
                            text  = fmt(entity.totalAmount),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }

                    // Per-person
                    if (entity.splitCount > 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text  = stringResource(R.string.history_detail_per_person, entity.splitCount),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text  = fmt(entity.totalAmount / entity.splitCount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // ── Actions ───────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val text = buildDetailShareText(entity, province, isFr, savedItems, fmt, pct)
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }, null
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.btn_share))
                }
                OutlinedButton(
                    onClick = { viewModel.delete(onNavigateBack) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun DetailEntryRow(
    label: String,
    value: String,
    valueWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueWeight
        )
    }
}

private fun buildDetailShareText(
    entity: HistoryEntity,
    province: Province,
    isFr: Boolean,
    savedItems: List<SavedItem>,
    fmt: (Double) -> String,
    pct: (Double) -> String
): String {
    val sep          = "─────────────────────"
    val provinceName = if (isFr) province.nameFr else province.nameEn
    val modeLabel    = when (entity.mode) {
        "FORWARD"    -> if (isFr) "Calculatrice" else "Calculator"
        "REVERSE"    -> if (isFr) "Calculatrice (Inverse)" else "Calculator (Reverse)"
        "SHOPPING"   -> if (isFr) "Liste courses" else "Shopping"
        "RESTAURANT" -> "Restaurant"
        else         -> entity.mode
    }
    val locale  = Locale.getDefault()
    val dateStr = SimpleDateFormat("d MMM yyyy  HH:mm", locale).format(Date(entity.timestamp))

    return buildString {
        appendLine("📋 TaxeCA — $modeLabel")
        appendLine("$provinceName (${entity.provinceCode})")
        appendLine(sep)
        if (entity.mode == "REVERSE")
            appendLine(dlv(if (isFr) "Prix payé" else "Amount paid", fmt(entity.inputAmount)))

        // Items list
        if (savedItems.isNotEmpty()) {
            appendLine(if (isFr) "Articles :" else "Items:")
            savedItems.forEach { item ->
                appendLine("  ${item.name.padEnd(22)} ${fmt(item.price)}")
            }
            appendLine(sep)
        }

        val baseLabel = when (entity.mode) {
            "SHOPPING"   -> if (isFr) "Sous-total articles" else "Items subtotal"
            "RESTAURANT" -> if (isFr) "Sous-total repas" else "Meal subtotal"
            "REVERSE"    -> if (isFr) "Avant taxes" else "Before tax"
            else         -> if (isFr) "Montant de base" else "Base amount"
        }
        appendLine(dlv(baseLabel, fmt(entity.baseAmount)))
        if (!province.isHstProvince && entity.gstAmount > 0)
            appendLine(dlv("TPS/GST (${pct(province.gstRate)})", fmt(entity.gstAmount)))
        if (!province.isHstProvince && entity.pstAmount > 0) {
            val tag = when (province.pstLabel) { "QST" -> "TVQ/QST"; "RST" -> "RST"; else -> "PST" }
            appendLine(dlv("$tag (${pct(province.pstRate)})", fmt(entity.pstAmount)))
        }
        if (province.isHstProvince && entity.hstAmount > 0)
            appendLine(dlv("TVH/HST (${pct(province.hstRate)})", fmt(entity.hstAmount)))
        if (entity.mode == "RESTAURANT") {
            val tip = entity.totalAmount - entity.baseAmount - entity.totalTax
            if (tip > 0.005)
                appendLine(dlv(if (isFr) "Pourboire / Tip" else "Tip / Pourboire", fmt(tip)))
        }
        appendLine(sep)
        appendLine(dlv("TOTAL", fmt(entity.totalAmount)))
        if (entity.splitCount > 1)
            appendLine(dlv(
                if (isFr) "Par personne (÷${entity.splitCount})" else "Per person (÷${entity.splitCount})",
                fmt(entity.totalAmount / entity.splitCount)
            ))
        appendLine(sep)
        appendLine(dateStr)
        append("Calculé avec TaxeCA 🇨🇦")
    }
}

private fun dlv(label: String, value: String): String {
    val col = 26
    return "$label${" ".repeat((col - label.length).coerceAtLeast(1))}$value"
}
