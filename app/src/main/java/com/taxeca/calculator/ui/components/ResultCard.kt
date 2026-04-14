package com.taxeca.calculator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
                    // Base amount
                    TaxRow(
                        label = stringResource(R.string.label_base_amount),
                        value = CurrencyFormatter.formatAmount(taxResult.baseAmount)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // GST line (only for non-HST provinces with GST)
                    if (!taxResult.province.isHstProvince && taxResult.province.gstRate > 0) {
                        val gstLabel = stringResource(
                            R.string.label_gst_with_rate,
                            CurrencyFormatter.formatPercent(taxResult.province.gstRate)
                        )
                        TaxRow(
                            label = gstLabel,
                            value = CurrencyFormatter.formatAmount(taxResult.gstAmount)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // PST / QST / RST line
                    if (!taxResult.province.isHstProvince && taxResult.province.pstRate > 0) {
                        val pstLabel = when (taxResult.province.pstLabel) {
                            "QST" -> stringResource(
                                R.string.label_qst_with_rate,
                                CurrencyFormatter.formatPercent(taxResult.province.pstRate)
                            )
                            "RST" -> stringResource(
                                R.string.label_rst_with_rate,
                                CurrencyFormatter.formatPercent(taxResult.province.pstRate)
                            )
                            else -> stringResource(
                                R.string.label_pst_with_rate,
                                CurrencyFormatter.formatPercent(taxResult.province.pstRate)
                            )
                        }
                        TaxRow(
                            label = pstLabel,
                            value = CurrencyFormatter.formatAmount(taxResult.pstAmount)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // HST line
                    if (taxResult.province.isHstProvince) {
                        val hstLabel = stringResource(
                            R.string.label_hst_with_rate,
                            CurrencyFormatter.formatPercent(taxResult.province.hstRate)
                        )
                        TaxRow(
                            label = hstLabel,
                            value = CurrencyFormatter.formatAmount(taxResult.hstAmount)
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

@Composable
private fun TaxRow(
    label: String,
    value: String,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
