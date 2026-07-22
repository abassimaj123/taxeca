package com.taxeca.calculator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
                            subtitle = stringResource(R.string.gst_subtitle),
                            tooltipTitle = stringResource(R.string.tooltip_gst_title),
                            tooltipBody  = stringResource(R.string.tooltip_gst_body)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // PST / QST / RST line
                    if (!taxResult.province.isHstProvince && taxResult.province.pstRate > 0) {
                        val (labelRes, titleRes, bodyRes, subtitleRes) = when (taxResult.province.pstLabel) {
                            "QST" -> QuadRes(R.string.label_qst_with_rate, R.string.tooltip_qst_title, R.string.tooltip_qst_body, R.string.qst_subtitle)
                            "RST" -> QuadRes(R.string.label_rst_with_rate, R.string.tooltip_rst_title, R.string.tooltip_rst_body, R.string.rst_subtitle)
                            else  -> QuadRes(R.string.label_pst_with_rate, R.string.tooltip_pst_title, R.string.tooltip_pst_body, R.string.pst_subtitle)
                        }
                        TaxRow(
                            label = stringResource(labelRes, CurrencyFormatter.formatPercent(taxResult.province.pstRate)),
                            value = CurrencyFormatter.formatAmount(taxResult.pstAmount),
                            subtitle = stringResource(subtitleRes),
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
                            subtitle = stringResource(R.string.hst_subtitle),
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

                }
            }
        }
    }
}

private data class QuadRes(val label: Int, val title: Int, val body: Int, val subtitle: Int)

@Composable
private fun TaxRow(
    label: String,
    value: String,
    subtitle: String? = null,
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
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
