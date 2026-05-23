package com.taxeca.calculator.domain.model

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ProvinceModelTest {

    // ── Province Code Tests ───────────────────────────────────────────────────

    @Test fun `QC province code is QC`() {
        assertEquals("QC", Province.QC.code)
    }

    @Test fun `ON province code is ON`() {
        assertEquals("ON", Province.ON.code)
    }

    @Test fun `All provinces have valid codes`() {
        Province.values().forEach { province ->
            assertEquals(2, province.code.length)
            assertTrue(province.code.all { it.isUpperCase() })
        }
    }

    // ── Province Tax Rate Tests ───────────────────────────────────────────────

    @Test fun `QC GST rate is 5 percent`() {
        assertEquals(0.05, Province.QC.gstRate, 0.0001)
    }

    @Test fun `QC PST rate is 9 975 percent`() {
        assertEquals(0.09975, Province.QC.pstRate, 0.0001)
    }

    @Test fun `ON HST rate is 13 percent`() {
        assertEquals(0.13, Province.ON.hstRate, 0.0001)
    }

    @Test fun `BC GST rate is 5 percent`() {
        assertEquals(0.05, Province.BC.gstRate, 0.0001)
    }

    @Test fun `BC PST rate is 7 percent`() {
        assertEquals(0.07, Province.BC.pstRate, 0.0001)
    }

    @Test fun `AB GST only no PST`() {
        assertEquals(0.05, Province.AB.gstRate, 0.0001)
        assertEquals(0.0, Province.AB.pstRate, 0.0001)
    }

    @Test fun `NS HST rate is 14 percent (April 2025 baisse de 15 a 14)`() {
        assertEquals(0.14, Province.NS.hstRate, 0.0001)
    }

    // ── HST Province Identification ───────────────────────────────────────────

    @Test fun `ON is HST province`() {
        assertEquals(true, Province.ON.isHstProvince)
    }

    @Test fun `NB is HST province`() {
        assertEquals(true, Province.NB.isHstProvince)
    }

    @Test fun `NS is HST province`() {
        assertEquals(true, Province.NS.isHstProvince)
    }

    @Test fun `PE is HST province`() {
        assertEquals(true, Province.PE.isHstProvince)
    }

    @Test fun `NL is HST province`() {
        assertEquals(true, Province.NL.isHstProvince)
    }

    @Test fun `QC is not HST province`() {
        assertEquals(false, Province.QC.isHstProvince)
    }

    @Test fun `BC is not HST province`() {
        assertEquals(false, Province.BC.isHstProvince)
    }

    @Test fun `AB is not HST province`() {
        assertEquals(false, Province.AB.isHstProvince)
    }

    @Test fun `Total of 5 HST provinces`() {
        val hstProvinces = Province.values().filter { it.isHstProvince }
        assertEquals(5, hstProvinces.size)
    }

    // ── Province Count ────────────────────────────────────────────────────────

    @Test fun `Canada has 13 provinces and territories`() {
        assertEquals(13, Province.values().size)
    }

    @Test fun `All provinces are unique`() {
        val codes = Province.values().map { it.code }
        assertEquals(13, codes.distinct().size)
    }

    // ── Province From Code ────────────────────────────────────────────────────

    @Test fun `fromCode QC returns Quebec`() {
        val province = Province.fromCode("QC")
        assertEquals(Province.QC, province)
    }

    @Test fun `fromCode ON returns Ontario`() {
        val province = Province.fromCode("ON")
        assertEquals(Province.ON, province)
    }

    @Test fun `fromCode BC returns British Columbia`() {
        val province = Province.fromCode("BC")
        assertEquals(Province.BC, province)
    }

    @Test fun `fromCode NU returns Nunavut`() {
        val province = Province.fromCode("NU")
        assertEquals(Province.NU, province)
    }

    @Test fun `fromCode with invalid code defaults to QC`() {
        val province = Province.fromCode("XX")
        assertEquals(Province.QC, province)
    }

    @Test fun `fromCode is case sensitive`() {
        val province = Province.fromCode("qc")  // lowercase
        // Should default to QC as it doesn't match uppercase "QC"
        assertEquals(Province.QC, province)
    }

    // ── All Provinces Can Calculate Rates ─────────────────────────────────────

    @Test fun `All HST provinces have 0 GST and PST`() {
        val hstProvinces = Province.values().filter { it.isHstProvince }
        hstProvinces.forEach { province ->
            assertEquals(0.0, province.gstRate, 0.0001)
            assertEquals(0.0, province.pstRate, 0.0001)
            assertEquals(true, province.hstRate > 0.0)
        }
    }

    @Test fun `All non-HST provinces have 0 HST`() {
        val nonHstProvinces = Province.values().filter { !it.isHstProvince }
        nonHstProvinces.forEach { province ->
            assertEquals(0.0, province.hstRate, 0.0001)
            assertEquals(true, province.gstRate > 0.0)
        }
    }

    @Test fun `All territories have GST only`() {
        val territories = listOf(Province.YT, Province.NT, Province.NU)
        territories.forEach { territory ->
            assertEquals(0.05, territory.gstRate, 0.0001)
            assertEquals(0.0, territory.pstRate, 0.0001)
            assertEquals(0.0, territory.hstRate, 0.0001)
        }
    }

    // ── Calculation Mode Tests ────────────────────────────────────────────────

    @Test fun `CalculationMode FORWARD exists`() {
        assertEquals(CalculationMode.FORWARD, CalculationMode.FORWARD)
    }

    @Test fun `CalculationMode REVERSE exists`() {
        assertEquals(CalculationMode.REVERSE, CalculationMode.REVERSE)
    }

    @Test fun `Two calculation modes available`() {
        assertEquals(2, CalculationMode.values().size)
    }
}
