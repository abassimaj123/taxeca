package com.taxeca.calculator.domain.model

data class ShoppingItemResult(
    val item: ShoppingItem,
    val gstAmount: Double,
    val pstAmount: Double,
    val hstAmount: Double,
    val totalTax: Double,
    val totalPrice: Double
)

data class ShoppingListResult(
    val province: Province,
    val items: List<ShoppingItemResult>,
    val subtotal: Double,
    val totalGst: Double,
    val totalPst: Double,
    val totalHst: Double,
    val totalTax: Double,
    val grandTotal: Double
)
