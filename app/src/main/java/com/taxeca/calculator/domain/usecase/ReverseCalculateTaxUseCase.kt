package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.domain.model.CalculationMode
import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.TaxResult
import javax.inject.Inject

class ReverseCalculateTaxUseCase @Inject constructor() {

    operator fun invoke(totalAmount: Double, province: Province): TaxResult {
        val baseAmount = totalAmount / (1.0 + province.totalRate)

        return if (province.isHstProvince) {
            val hstAmount = totalAmount - baseAmount
            TaxResult(
                province    = province,
                mode        = CalculationMode.REVERSE,
                inputAmount = totalAmount,
                baseAmount  = baseAmount,
                gstAmount   = 0.0,
                pstAmount   = 0.0,
                hstAmount   = hstAmount,
                totalTax    = hstAmount,
                totalAmount = totalAmount
            )
        } else {
            val gstAmount = baseAmount * province.gstRate
            val pstAmount = baseAmount * province.pstRate
            val totalTax  = gstAmount + pstAmount
            TaxResult(
                province    = province,
                mode        = CalculationMode.REVERSE,
                inputAmount = totalAmount,
                baseAmount  = baseAmount,
                gstAmount   = gstAmount,
                pstAmount   = pstAmount,
                hstAmount   = 0.0,
                totalTax    = totalTax,
                totalAmount = totalAmount
            )
        }
    }
}
