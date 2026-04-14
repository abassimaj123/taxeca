package com.taxeca.calculator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculations")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val provinceCode: String,
    val mode: String,
    val inputAmount: Double,
    val baseAmount: Double,
    val gstAmount: Double,
    val pstAmount: Double,
    val hstAmount: Double,
    val totalTax: Double,
    val totalAmount: Double,
    val splitCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val itemsJson: String? = null   // JSON array of {name, price} for SHOPPING / RESTAURANT item-mode
)
