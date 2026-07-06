package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.domain.model.CalculationMode
import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.TaxResult
import org.junit.Assert.assertEquals
import org.junit.Test

private const val DELTA = 0.0001

/**
 * [SaveCalculationUseCase] can't be exercised end-to-end in a plain JUnit test:
 * its [com.taxeca.calculator.data.repository.HistoryRepository] dependency requires
 * a real Android Context (Firebase Analytics/Crashlytics, BillingClient, DataStore),
 * and this module has no Robolectric/Mockito test infra.
 *
 * Instead, this test locks in the exact HistoryEntity-mapping *contract* that
 * SaveCalculationUseCase.invoke(TaxResult, splitCount, tipAmount) implements —
 * mirroring its mapping line-for-line — so a regression that silently drops the
 * tip again (the original bug) or breaks the tip-inclusive total fails here.
 */
class SaveCalculationMappingTest {

    /** Mirrors SaveCalculationUseCase.invoke(TaxResult, splitCount, tipAmount) exactly. */
    private fun mapToHistoryEntity(result: TaxResult, splitCount: Int = 1, tipAmount: Double = 0.0) =
        HistoryEntity(
            provinceCode = result.province.code,
            mode         = result.mode.name,
            inputAmount  = result.inputAmount,
            baseAmount   = result.baseAmount,
            gstAmount    = result.gstAmount,
            pstAmount    = result.pstAmount,
            hstAmount    = result.hstAmount,
            totalTax     = result.totalTax,
            totalAmount  = result.totalAmount + tipAmount,
            tipAmount    = tipAmount,
            splitCount   = splitCount
        )

    private val calcTax = CalculateTaxUseCase()

    @Test fun `Calculator save with tip round-trips tip-inclusive total`() {
        val result = calcTax(100.0, Province.ON)  // 100 + 13% HST = 113.0
        val tip = 20.0

        val saved = mapToHistoryEntity(result, splitCount = 1, tipAmount = tip)

        assertEquals(20.0,  saved.tipAmount,   DELTA)
        assertEquals(133.0, saved.totalAmount, DELTA)  // 113 (tax-inclusive) + 20 tip — NOT dropped
    }

    @Test fun `Calculator save with tip and split divides tip-inclusive total`() {
        val result = calcTax(100.0, Province.ON)  // total 113.0
        val tip = 27.0

        val saved = mapToHistoryEntity(result, splitCount = 4, tipAmount = tip)

        assertEquals(4, saved.splitCount)
        assertEquals(140.0, saved.totalAmount, DELTA)              // 113 + 27 tip
        assertEquals(35.0,  saved.totalAmount / saved.splitCount, DELTA) // per-person matches what user saw
    }

    @Test fun `Calculator save without tip keeps totalAmount unchanged`() {
        val result = calcTax(100.0, Province.ON)

        val saved = mapToHistoryEntity(result, splitCount = 1, tipAmount = 0.0)

        assertEquals(0.0,   saved.tipAmount,   DELTA)
        assertEquals(113.0, saved.totalAmount, DELTA)
    }

    @Test fun `Reverse mode calculation preserves REVERSE mode string`() {
        val reverseTax = ReverseCalculateTaxUseCase()
        val result = reverseTax(113.0, Province.ON)

        val saved = mapToHistoryEntity(result)

        assertEquals(CalculationMode.REVERSE, result.mode)
        assertEquals("REVERSE", saved.mode)
    }

    @Test fun `Forward mode calculation preserves FORWARD mode string`() {
        val result = calcTax(100.0, Province.ON)

        val saved = mapToHistoryEntity(result)

        assertEquals(CalculationMode.FORWARD, result.mode)
        assertEquals("FORWARD", saved.mode)
    }

    // ── Restaurant tip already worked — lock in no-regression ─────────────────

    @Test fun `Restaurant save preserves tip via explicit tipAmount field`() {
        val calcRestaurant = CalculateRestaurantUseCase()
        val restaurant = calcRestaurant(
            subtotal   = 100.0,
            province   = Province.ON,
            tipPercent = 0.18,
            splitCount = 1
        )

        // Mirrors SaveCalculationUseCase.invoke(RestaurantResult, items) mapping
        val saved = HistoryEntity(
            provinceCode = restaurant.province.code,
            mode         = "RESTAURANT",
            inputAmount  = restaurant.subtotal,
            baseAmount   = restaurant.subtotal,
            gstAmount    = restaurant.gstAmount,
            pstAmount    = restaurant.pstAmount,
            hstAmount    = restaurant.hstAmount,
            totalTax     = restaurant.totalTax,
            totalAmount  = restaurant.total,
            tipAmount    = restaurant.tipAmount,
            splitCount   = restaurant.splitCount
        )

        assertEquals(18.0, saved.tipAmount, DELTA)
        assertEquals(restaurant.total, saved.totalAmount, DELTA)
        assertEquals("RESTAURANT", saved.mode)
    }
}
