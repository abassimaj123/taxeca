package com.taxeca.calculator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.taxeca.calculator.R
import com.taxeca.calculator.domain.model.CalculationMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeToggle(
    selectedMode: CalculationMode,
    onModeSelected: (CalculationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(CalculationMode.FORWARD, CalculationMode.REVERSE)
    val labels = listOf(
        stringResource(R.string.label_mode_forward),
        stringResource(R.string.label_mode_reverse)
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                onClick = { onModeSelected(mode) },
                selected = selectedMode == mode,
                label = { Text(labels[index]) }
            )
        }
    }
}
