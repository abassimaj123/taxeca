package com.taxeca.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taxeca.calculator.ui.theme.GradientEnd
import com.taxeca.calculator.ui.theme.GradientStart

private val defaultGradient = Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
private val disabledGradient = Brush.horizontalGradient(listOf(Color(0xFFBDBDBD), Color(0xFFBDBDBD)))
private val buttonShape = RoundedCornerShape(28.dp)

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(if (enabled) 6.dp else 0.dp, buttonShape)
            .clip(buttonShape)
            .background(if (enabled) defaultGradient else disabledGradient)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color(0xFF757575),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
