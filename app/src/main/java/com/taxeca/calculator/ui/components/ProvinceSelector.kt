package com.taxeca.calculator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.taxeca.calculator.R
import com.taxeca.calculator.domain.model.Province
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvinceSelector(
    selectedProvince: Province,
    onProvinceSelected: (Province) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val isFrench = locale.language == "fr"

    fun Province.displayName(): String = if (isFrench) nameFr else nameEn

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedProvince.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_province)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Province.entries.forEach { province ->
                DropdownMenuItem(
                    text = {
                        Text("${province.code} — ${province.displayName()}")
                    },
                    onClick = {
                        onProvinceSelected(province)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
