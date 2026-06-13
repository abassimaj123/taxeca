package com.taxeca.calculator.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun formatAmount(amount: Double, locale: Locale = Locale.CANADA): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.format(amount)
    }

    fun formatPercent(rate: Double): String {
        val percentage = rate * 100.0
        return if (percentage % 1.0 == 0.0) {
            "${percentage.toInt()}%"
        } else {
            // Use up to 3 decimal places, stripping trailing zeros
            val formatted = "%.3f".format(percentage).trimEnd('0').trimEnd('.')
            "$formatted%"
        }
    }
}
