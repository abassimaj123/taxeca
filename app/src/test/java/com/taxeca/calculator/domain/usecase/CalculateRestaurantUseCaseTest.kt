package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.domain.model.Province
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

private const val DELTA = 0.0001

class CalculateRestaurantUseCaseTest {

    private lateinit var useCase: CalculateRestaurantUseCase

    @Before
    fun setUp() {
        useCase = CalculateRestaurantUseCase()
    }

    // ── Quebec Base Tip Calculation (no split) ────────────────────────────────

    @Test fun `QC 100 tip 15 percent 15 dollars`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.15, splitCount = 1)
        assertEquals(0.15, result.tipPercent, DELTA)
        assertEquals(15.0, result.tipAmount, DELTA)
        assertEquals(100.0, result.subtotal, DELTA)
    }

    @Test fun `QC 100 tip 18 percent 18 dollars`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.18, splitCount = 1)
        assertEquals(18.0, result.tipAmount, DELTA)
    }

    @Test fun `QC 100 tip 20 percent 20 dollars`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.20, splitCount = 1)
        assertEquals(20.0, result.tipAmount, DELTA)
    }

    @Test fun `QC 100 no tip 0 dollars`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.0, splitCount = 1)
        assertEquals(0.0, result.tipAmount, DELTA)
    }

    // ── Quebec Total with Taxes ───────────────────────────────────────────────

    @Test fun `QC 100 tip 15 total 129 975`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.15, splitCount = 1)
        // Subtotal: 100
        // Tax: 5 (GST) + 9.975 (PST) = 14.975
        // Tip: 15 (on subtotal, not on tax)
        // Total: 100 + 14.975 + 15 = 129.975
        assertEquals(129.975, result.total, DELTA)
    }

    // ── Ontario HST ───────────────────────────────────────────────────────────

    @Test fun `ON 100 tip 15 HST 13 total with tax`() {
        val result = useCase(subtotal = 100.0, province = Province.ON, tipPercent = 0.15, splitCount = 1)
        assertEquals(15.0, result.tipAmount, DELTA)
        // Subtotal 100 + HST 13 + Tip 15 = 128
        assertEquals(128.0, result.total, DELTA)
    }

    @Test fun `ON 200 tip 20 split 4 per person 66 50`() {
        val result = useCase(subtotal = 200.0, province = Province.ON, tipPercent = 0.20, splitCount = 4)
        assertEquals(40.0, result.tipAmount, DELTA)
        assertEquals(200.0 + 26.0 + 40.0, result.total, DELTA)
        assertEquals((200.0 + 26.0 + 40.0) / 4, result.perPerson, DELTA)
    }

    // ── Split Bill ────────────────────────────────────────────────────────────

    @Test fun `QC 100 split 2 people per person with tip and tax`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.15, splitCount = 2)
        // Total: 100 + 14.975 (tax) + 15 (tip) = 129.975
        // Per person: 129.975 / 2 = 64.9875
        assertEquals(64.9875, result.perPerson, DELTA)
    }


    @Test fun `QC 100 split 4 people`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.15, splitCount = 4)
        assertEquals(4, result.splitCount)
        assertEquals(true, result.perPerson > 0.0)
    }

    // ── Edge Cases: Split = 1 ─────────────────────────────────────────────────

    @Test fun `Single person split count 1 equals no split`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.15, splitCount = 1)
        assertEquals(result.total, result.perPerson, DELTA)
    }

    // ── Various Provinces ─────────────────────────────────────────────────────

    @Test fun `BC 100 tip 15 GST 5 PST 7`() {
        val result = useCase(subtotal = 100.0, province = Province.BC, tipPercent = 0.15, splitCount = 1)
        assertEquals(15.0, result.tipAmount, DELTA)
        assertEquals(100.0 + 5.0 + 7.0 + 15.0, result.total, DELTA)
    }

    @Test fun `AB 100 tip 15 GST only 5 no PST`() {
        val result = useCase(subtotal = 100.0, province = Province.AB, tipPercent = 0.15, splitCount = 1)
        assertEquals(15.0, result.tipAmount, DELTA)
        assertEquals(100.0 + 5.0 + 15.0, result.total, DELTA)
    }

    // ── Zero Subtotal ─────────────────────────────────────────────────────────

    @Test fun `Zero subtotal zero tip`() {
        val result = useCase(subtotal = 0.0, province = Province.QC, tipPercent = 0.15, splitCount = 1)
        assertEquals(0.0, result.tipAmount, DELTA)
        assertEquals(0.0, result.total, DELTA)
    }

    // ── High Tip Percentage ───────────────────────────────────────────────────

    @Test fun `100 percent tip use case`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 1.0, splitCount = 1)
        assertEquals(100.0, result.tipAmount, DELTA)
    }

    // ── Large Party Split ─────────────────────────────────────────────────────

    @Test fun `Large party 500 split 8 people`() {
        val result = useCase(subtotal = 500.0, province = Province.ON, tipPercent = 0.18, splitCount = 8)
        assertEquals(90.0, result.tipAmount, DELTA)
        // Total: 500 + 65 (HST) + 90 = 655
        assertEquals(655.0, result.total, DELTA)
        assertEquals(655.0 / 8.0, result.perPerson, DELTA)
    }

    // ── Fractional Subtotal ───────────────────────────────────────────────────

    @Test fun `Fractional subtotal 47 99 tip 15 percent`() {
        val result = useCase(subtotal = 47.99, province = Province.QC, tipPercent = 0.15, splitCount = 1)
        assertEquals(47.99 * 0.15, result.tipAmount, DELTA)
    }

    // ── Tip on Subtotal Not on Tax ────────────────────────────────────────────

    @Test fun `Verify tip calculated on subtotal not on total with tax`() {
        val result = useCase(subtotal = 100.0, province = Province.QC, tipPercent = 0.15, splitCount = 1)
        val expectedTip = 100.0 * 0.15  // 15
        assertEquals(expectedTip, result.tipAmount, DELTA)
        // Not 114.975 * 0.15 = 17.24625
    }

    // ── Rounding Consistency ──────────────────────────────────────────────────

    @Test fun `Small amount maintains precision`() {
        val result = useCase(subtotal = 1.50, province = Province.QC, tipPercent = 0.20, splitCount = 1)
        assertEquals(0.30, result.tipAmount, DELTA)
    }
}
