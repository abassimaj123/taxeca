package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.domain.model.CalculationMode
import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.TaxResult
import javax.inject.Inject

class CalculateTaxUseCase @Inject constructor() {

    operator fun invoke(amount: Double, province: Province): TaxResult {
        return if (province.isHstProvince) {
            val hstAmount = amount * province.hstRate
            val totalTax = hstAmount
            TaxResult(
                province = province,
                mode = CalculationMode.FORWARD,
                inputAmount = amount,
                baseAmount = amount,
                gstAmount = 0.0,
                pstAmount = 0.0,
                hstAmount = hstAmount,
                totalTax = totalTax,
                totalAmount = amount + totalTax
            )
        } else {
            val gstAmount = amount * province.gstRate
            val pstAmount = amount * province.pstRate
            val totalTax = gstAmount + pstAmount
            TaxResult(
                province = province,
                mode = CalculationMode.FORWARD,
                inputAmount = amount,
                baseAmount = amount,
                gstAmount = gstAmount,
                pstAmount = pstAmount,
                hstAmount = 0.0,
                totalTax = totalTax,
                totalAmount = amount + totalTax
            )
        }
    }
}
