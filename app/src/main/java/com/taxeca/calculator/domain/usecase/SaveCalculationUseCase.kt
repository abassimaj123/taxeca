package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.data.repository.HistoryRepository
import com.taxeca.calculator.domain.model.RestaurantResult
import com.taxeca.calculator.domain.model.ShoppingItem
import com.taxeca.calculator.domain.model.ShoppingListResult
import com.taxeca.calculator.domain.model.TaxResult
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class SaveCalculationUseCase @Inject constructor(
    private val repo: HistoryRepository
) {
    suspend operator fun invoke(result: TaxResult, splitCount: Int = 1) {
        repo.save(HistoryEntity(
            provinceCode = result.province.code,
            mode         = result.mode.name,
            inputAmount  = result.inputAmount,
            baseAmount   = result.baseAmount,
            gstAmount    = result.gstAmount,
            pstAmount    = result.pstAmount,
            hstAmount    = result.hstAmount,
            totalTax     = result.totalTax,
            totalAmount  = result.totalAmount,
            splitCount   = splitCount
        ))
    }

    suspend operator fun invoke(result: ShoppingListResult) {
        repo.save(HistoryEntity(
            provinceCode = result.province.code,
            mode         = "SHOPPING",
            inputAmount  = result.subtotal,
            baseAmount   = result.subtotal,
            gstAmount    = result.totalGst,
            pstAmount    = result.totalPst,
            hstAmount    = result.totalHst,
            totalTax     = result.totalTax,
            totalAmount  = result.grandTotal,
            itemsJson    = serializeItems(result.items.map { it.item })
        ))
    }

    suspend operator fun invoke(result: RestaurantResult, items: List<ShoppingItem> = emptyList()) {
        repo.save(HistoryEntity(
            provinceCode = result.province.code,
            mode         = "RESTAURANT",
            inputAmount  = result.subtotal,
            baseAmount   = result.subtotal,
            gstAmount    = result.gstAmount,
            pstAmount    = result.pstAmount,
            hstAmount    = result.hstAmount,
            totalTax     = result.totalTax,
            totalAmount  = result.total,
            splitCount   = result.splitCount,
            itemsJson    = if (items.isNotEmpty()) serializeItems(items) else null
        ))
    }

    private fun serializeItems(items: List<ShoppingItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("name",  item.displayName)
                put("price", item.price)
            })
        }
        return arr.toString()
    }
}
