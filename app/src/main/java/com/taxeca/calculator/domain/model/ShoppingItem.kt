package com.taxeca.calculator.domain.model

import java.util.UUID

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val price: Double,
    val displayName: String,
    /** false = tax-exempt (e.g. basic groceries, zero-rated for GST/HST in Canada). */
    val taxable: Boolean = true
)
