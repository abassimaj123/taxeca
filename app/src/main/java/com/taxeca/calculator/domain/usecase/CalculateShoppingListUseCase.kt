package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.ShoppingItem
import com.taxeca.calculator.domain.model.ShoppingItemResult
import com.taxeca.calculator.domain.model.ShoppingListResult
import javax.inject.Inject

class CalculateShoppingListUseCase @Inject constructor() {

    operator fun invoke(items: List<ShoppingItem>, province: Province): ShoppingListResult {
        val itemResults = items.map { item -> calculateItem(item, province) }

        val subtotal   = itemResults.sumOf { it.item.price }
        val totalGst   = itemResults.sumOf { it.gstAmount }
        val totalPst   = itemResults.sumOf { it.pstAmount }
        val totalHst   = itemResults.sumOf { it.hstAmount }
        val totalTax   = itemResults.sumOf { it.totalTax }
        val grandTotal = itemResults.sumOf { it.totalPrice }

        return ShoppingListResult(
            province   = province,
            items      = itemResults,
            subtotal   = subtotal,
            totalGst   = totalGst,
            totalPst   = totalPst,
            totalHst   = totalHst,
            totalTax   = totalTax,
            grandTotal = grandTotal
        )
    }

    private fun calculateItem(item: ShoppingItem, province: Province): ShoppingItemResult {
        return if (province.isHstProvince) {
            val hst = item.price * province.hstRate
            ShoppingItemResult(
                item       = item,
                gstAmount  = 0.0,
                pstAmount  = 0.0,
                hstAmount  = hst,
                totalTax   = hst,
                totalPrice = item.price + hst
            )
        } else {
            val gst = item.price * province.gstRate
            val pst = item.price * province.pstRate
            ShoppingItemResult(
                item       = item,
                gstAmount  = gst,
                pstAmount  = pst,
                hstAmount  = 0.0,
                totalTax   = gst + pst,
                totalPrice = item.price + gst + pst
            )
        }
    }
}
