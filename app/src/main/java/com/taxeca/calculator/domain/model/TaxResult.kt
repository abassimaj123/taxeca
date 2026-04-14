package com.taxeca.calculator.domain.model

data class TaxResult(
    val province: Province,
    val mode: CalculationMode,
    val inputAmount: Double,
    val baseAmount: Double,
    val gstAmount: Double,
    val pstAmount: Double,
    val hstAmount: Double,
    val totalTax: Double,
    val totalAmount: Double
)
