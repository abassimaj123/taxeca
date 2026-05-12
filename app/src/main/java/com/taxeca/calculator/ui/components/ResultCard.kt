package com.taxeca.calculator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taxeca.calculator.R
import com.taxeca.calculator.domain.model.TaxResult
import com.taxeca.calculator.ui.theme.AccentGreen
import com.taxeca.calculator.utils.CurrencyFormatter

@Composable
fun ResultCard(
    result: TaxResult?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = result != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        result?.let { taxResult ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Base amount (no tooltip — self-explanatory)
                    TaxRow(
                        label = stringResource(R.string.label_base_amount),
                        value = CurrencyFormatter.formatAmount(taxResult.baseAmount)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // GST line
                    if (!taxResult.province.isHstProvince && taxResult.province.gstRate > 0) {
                        TaxRow(
                            label = stringResource(
                                R.string.label_gst_with_rate,
                                CurrencyFormatter.formatPercent(taxResult.province.gstRate)
                            ),
                            value = CurrencyFormatter.formatAmount(taxResult.gstAmount),
                            tooltipTitle = stringResource(R.string.tooltip_gst_title),
                            tooltipBody  = stringResource(R.string.tooltip_gst_body)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // PST / QST / RST line
                    if (!taxResult.province.isHstProvince && taxResult.province.pstRate > 0) {
                        val (labelRes, titleRes, bodyRes) = when (taxResult.province.pstLabel) {
                            "QST" -> Triple(R.string.label_qst_with_rate, R.string.tooltip_qst_title, R.string.tooltip_qst_body)
                            "RST" -> Triple(R.string.label_rst_with_rate, R.string.tooltip_rst_title, R.string.tooltip_rst_body)
                            else  -> Triple(R.string.label_pst_with_rate, R.string.tooltip_pst_title, R.string.tooltip_pst_body)
                        }
                        TaxRow(
                            label = stringResource(labelRes, CurrencyFormatter.formatPercent(taxResult.province.pstRate)),
                            value = CurrencyFormatter.formatAmount(taxResult.pstAmount),
                            tooltipTitle = stringResource(titleRes),
                            tooltipBody  = stringResource(bodyRes)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // HST line
                    if (taxResult.province.isHstProvince) {
                        TaxRow(
                            label = stringResource(
                                R.string.label_hst_with_rate,
                                CurrencyFormatter.formatPercent(taxResult.province.hstRate)
                            ),
                            value = CurrencyFormatter.formatAmount(taxResult.hstAmount),
                            tooltipTitle = stringResource(R.string.tooltip_hst_title),
                            tooltipBody  = stringResource(R.string.tooltip_hst_body)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Total tax
                    TaxRow(
                        label = stringResource(R.string.label_total_tax),
                        value = CurrencyFormatter.formatAmount(taxResult.totalTax),
                        labelStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Total amount (prominent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_total),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatAmount(taxResult.totalAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }

                    // ── Donut chart for tax breakdown ─────────────────────────
                    Spacer(modifier = Modifier.height(16.dp))
                    TaxDonutChart(taxResult = taxResult)
                }
            }
        }
    }
}

@Composable
private fun TaxDonutChart(taxResult: TaxResult) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary  = MaterialTheme.colorScheme.tertiary

    // Resolve labels at composable scope before building segment list
    val labelGst = stringResource(R.string.label_gst)
    val labelPst = stringResource(R.string.label_pst)
    val labelQst = stringResource(R.string.label_qst)
    val labelRst = stringResource(R.string.label_rst)
    val labelHst = stringResource(R.string.label_hst)

    // Build non-zero segments: (label, amount, color)
    data class Segment(val label: String, val amount: Double, val color: Color)
    val segments = buildList {
        if (!taxResult.province.isHstProvince && taxResult.gstAmount > 0)
            add(Segment(labelGst, taxResult.gstAmount, primary))
        if (!taxResult.province.isHstProvince && taxResult.pstAmount > 0) {
            val label = when (taxResult.province.pstLabel) {
                "QST" -> labelQst
                "RST" -> labelRst
                else  -> labelPst
            }
            add(Segment(label, taxResult.pstAmount, secondary))
        }
        if (taxResult.province.isHstProvince && taxResult.hstAmount > 0)
            add(Segment(labelHst, taxResult.hstAmount, tertiary))
    }

    if (segments.isEmpty()) return

    val totalTax = segments.sumOf { it.amount }
    if (totalTax <= 0) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut canvas: draw each segment sequentially
        Canvas(modifier = Modifier.size(120.dp)) {
            val strokeWidth = 24.dp.toPx()
            var startAngle = -90f
            // Grey background ring
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Colored segments
            segments.forEach { seg ->
                val sweep = (seg.amount / totalTax).toFloat() * 360f
                drawArc(
                    color = seg.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }

        Spacer(Modifier.width(20.dp))

        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            segments.forEach { seg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = seg.color)
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            seg.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            CurrencyFormatter.formatAmount(seg.amount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaxRow(
    label: String,
    value: String,
    tooltipTitle: String? = null,
    tooltipBody: String? = null,
    labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }

    if (showTooltip && tooltipTitle != null && tooltipBody != null) {
        AlertDialog(
            onDismissRequest = { showTooltip = false },
            title   = { Text(tooltipTitle, style = MaterialTheme.typography.titleMedium) },
            text    = { Text(tooltipBody,  style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showTooltip = false }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text  = label,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tooltipTitle != null) {
                Spacer(Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = tooltipTitle,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { showTooltip = true }
                )
            }
        }
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
