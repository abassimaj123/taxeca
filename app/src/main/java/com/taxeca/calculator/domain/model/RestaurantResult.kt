package com.taxeca.calculator.domain.model

data class RestaurantResult(
    val province: Province,
    val subtotal: Double,
    val gstAmount: Double,
    val pstAmount: Double,
    val hstAmount: Double,
    val totalTax: Double,
    val tipPercent: Double,
    val tipAmount: Double,
    val total: Double,
    val splitCount: Int,
    val perPerson: Double
)
