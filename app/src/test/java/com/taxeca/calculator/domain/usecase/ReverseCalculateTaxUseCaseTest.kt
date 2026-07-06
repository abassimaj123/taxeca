package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.domain.model.Province
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

private const val DELTA = 0.0001

class ReverseCalculateTaxUseCaseTest {

    private lateinit var useCase: ReverseCalculateTaxUseCase

    @Before
    fun setUp() {
        useCase = ReverseCalculateTaxUseCase()
    }

    // ── HST Provinces (ON, NB, NS, PE, NL) ────────────────────────────────────

    @Test fun `ON HST reverse 113 extract base 100 13 HST`() {
        val result = useCase(113.0, Province.ON)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(13.0, result.hstAmount, DELTA)
        assertEquals(0.0, result.gstAmount, DELTA)
        assertEquals(0.0, result.pstAmount, DELTA)
    }

    @Test fun `NB HST reverse 115 base 100 15 HST`() {
        val result = useCase(115.0, Province.NB)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(15.0, result.hstAmount, DELTA)
    }

    @Test fun `NS HST 14 percent reverse 114 base 100 14 HST`() {
        val result = useCase(114.0, Province.NS)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(14.0, result.hstAmount, DELTA)
    }

    @Test fun `PE HST reverse 115 base 100 15 HST`() {
        val result = useCase(115.0, Province.PE)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(15.0, result.hstAmount, DELTA)
    }

    @Test fun `NL HST reverse 115 base 100 15 HST`() {
        val result = useCase(115.0, Province.NL)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(15.0, result.hstAmount, DELTA)
    }

    // ── GST+PST Provinces (QC, BC, MB, SK, AB, YT, NT, NU) ───────────────────

    @Test fun `QC GST+PST reverse 114 975 base 100 GST 5 PST 9 975`() {
        val result = useCase(114.975, Province.QC)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(5.0, result.gstAmount, DELTA)
        assertEquals(9.975, result.pstAmount, DELTA)
        assertEquals(0.0, result.hstAmount, DELTA)
    }

    @Test fun `BC GST+PST reverse 112 base 100 GST 5 PST 7`() {
        val result = useCase(112.0, Province.BC)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(5.0, result.gstAmount, DELTA)
        assertEquals(7.0, result.pstAmount, DELTA)
    }

    @Test fun `MB RST reverse 112 base 100 GST 5 RST 7`() {
        val result = useCase(112.0, Province.MB)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(5.0,   result.gstAmount,  DELTA)
        assertEquals(7.0,   result.pstAmount,  DELTA)  // Manitoba RST 7% (since 2019-07-01)
    }

    @Test fun `SK GST+PST reverse 111 base 100 GST 5 PST 6`() {
        val result = useCase(111.0, Province.SK)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(5.0, result.gstAmount, DELTA)
        assertEquals(6.0, result.pstAmount, DELTA)
    }

    @Test fun `AB GST only reverse 105 base 100 GST 5`() {
        val result = useCase(105.0, Province.AB)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(5.0, result.gstAmount, DELTA)
        assertEquals(0.0, result.pstAmount, DELTA)
    }

    @Test fun `YT GST only reverse 105 base 100 GST 5`() {
        val result = useCase(105.0, Province.YT)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(5.0, result.gstAmount, DELTA)
    }

    @Test fun `NT GST only reverse 105 base 100 GST 5`() {
        val result = useCase(105.0, Province.NT)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(5.0, result.gstAmount, DELTA)
    }

    @Test fun `NU GST only reverse 105 base 100 GST 5`() {
        val result = useCase(105.0, Province.NU)
        assertEquals(100.0, result.baseAmount, DELTA)
        assertEquals(5.0, result.gstAmount, DELTA)
    }

    // ── Large Amounts ─────────────────────────────────────────────────────────


    @Test fun `ON reverse 10000 large amount HST`() {
        val result = useCase(10000.0, Province.ON)
        val expectedBase = 10000.0 / 1.13
        assertEquals(expectedBase, result.baseAmount, DELTA)
    }

    // ── Precision Edge Cases ──────────────────────────────────────────────────

    @Test fun `Reverse 0 01 small amount`() {
        val result = useCase(0.01, Province.QC)
        assertEquals(true, result.baseAmount > 0.0)
        assertEquals(true, result.baseAmount < 0.01)
    }

    @Test fun `Reverse calculation verifies round-trip`() {
        val original = 100.0
        val province = Province.QC

        // Forward: 100 + GST + PST
        val forward = useCase(original * 1.14975, Province.QC)
        assertEquals(original, forward.baseAmount, 0.001)
    }

    @Test fun `All provinces round-trip consistency`() {
        val provinces = listOf(
            Province.QC, Province.ON, Province.BC, Province.AB, Province.MB,
            Province.SK, Province.NB, Province.NS, Province.PE, Province.NL,
            Province.YT, Province.NT, Province.NU
        )

        provinces.forEach { province ->
            val base = 100.0
            val total = base * (1.0 + (if (province.isHstProvince) province.hstRate else province.gstRate + province.pstRate))
            val reversed = useCase(total, province)
            assertEquals("Province $province", base, reversed.baseAmount, 0.01)
        }
    }

    // ── Zero and Negative ──────────────────────────────────────────────────────

    @Test fun `Reverse zero amount`() {
        val result = useCase(0.0, Province.QC)
        assertEquals(0.0, result.baseAmount, DELTA)
    }

}
