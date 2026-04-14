package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.RestaurantResult
import javax.inject.Inject

class CalculateRestaurantUseCase @Inject constructor() {

    /**
     * In Canada, tip is customarily calculated on the pre-tax subtotal.
     * Some people tip on the total — this is a UI option but defaults to pre-tax.
     *
     * @param subtotal     Pre-tax amount
     * @param province     Selected province (determines tax type and rates)
     * @param tipPercent   Tip percentage (e.g. 0.18 for 18%)
     * @param splitCount   Number of people to split the bill
     * @param tipOnTotal   If true, tip is calculated on (subtotal + tax). Default false.
     */
    operator fun invoke(
        subtotal: Double,
        province: Province,
        tipPercent: Double,
        splitCount: Int,
        tipOnTotal: Boolean = false
    ): RestaurantResult {
        val gst: Double
        val pst: Double
        val hst: Double
        val totalTax: Double

        if (province.isHstProvince) {
            gst      = 0.0
            pst      = 0.0
            hst      = subtotal * province.hstRate
            totalTax = hst
        } else {
            gst      = subtotal * province.gstRate
            pst      = subtotal * province.pstRate
            hst      = 0.0
            totalTax = gst + pst
        }

        val taxedSubtotal = subtotal + totalTax
        val tipBase       = if (tipOnTotal) taxedSubtotal else subtotal
        val tipAmount     = tipBase * tipPercent
        val total         = taxedSubtotal + tipAmount
        val perPerson     = if (splitCount > 0) total / splitCount else total

        return RestaurantResult(
            province   = province,
            subtotal   = subtotal,
            gstAmount  = gst,
            pstAmount  = pst,
            hstAmount  = hst,
            totalTax   = totalTax,
            tipPercent = tipPercent,
            tipAmount  = tipAmount,
            total      = total,
            splitCount = splitCount,
            perPerson  = perPerson
        )
    }
}
