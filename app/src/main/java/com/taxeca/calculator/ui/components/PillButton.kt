package com.taxeca.calculator.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Pill-shaped toggle button — fixed 48dp height, text never clipped.
 * Used for tip presets and split selectors.
 */
@Composable
fun PillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.surfaceVariant
    val contentColor   = if (selected) MaterialTheme.colorScheme.onPrimary
                         else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick       = onClick,
        shape         = RoundedCornerShape(50),
        color         = containerColor,
        shadowElevation = if (selected) 4.dp else 1.dp,
        modifier      = modifier.height(48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text      = label,
                color     = contentColor,
                style     = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
        }
    }
}
