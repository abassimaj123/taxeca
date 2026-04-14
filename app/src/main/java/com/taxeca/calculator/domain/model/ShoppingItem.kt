package com.taxeca.calculator.domain.model

import java.util.UUID

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val price: Double,
    val displayName: String
)
