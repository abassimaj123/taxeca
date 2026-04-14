package com.taxeca.calculator

import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.ShoppingItem
import com.taxeca.calculator.domain.usecase.CalculateRestaurantUseCase
import com.taxeca.calculator.domain.usecase.CalculateShoppingListUseCase
import com.taxeca.calculator.domain.usecase.CalculateTaxUseCase
import com.taxeca.calculator.domain.usecase.ReverseCalculateTaxUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val DELTA = 0.0001

class TaxCalculationTest {

    private lateinit var calcTax: CalculateTaxUseCase
    private lateinit var reverseTax: ReverseCalculateTaxUseCase
    private lateinit var calcShopping: CalculateShoppingListUseCase
    private lateinit var calcRestaurant: CalculateRestaurantUseCase

    @Before
    fun setUp() {
        calcTax       = CalculateTaxUseCase()
        reverseTax    = ReverseCalculateTaxUseCase()
        calcShopping  = CalculateShoppingListUseCase()
        calcRestaurant = CalculateRestaurantUseCase()
    }

    // ── Québec ────────────────────────────────────────────────────────────────

    @Test fun `QC forward 100 TPS 5 00 TVQ 9 975 total 114 975`() {
        val r = calcTax(100.0, Province.QC)
        assertEquals(5.0,     r.gstAmount,   DELTA)
        assertEquals(9.975,   r.pstAmount,   DELTA)
        assertEquals(114.975, r.totalAmount, DELTA)
    }

    @Test fun `QC inverse 114 975 base 100 00`() {
        val r = reverseTax(114.975, Province.QC)
        assertEquals(100.0, r.baseAmount,  DELTA)
        assertEquals(5.0,   r.gstAmount,   DELTA)
        assertEquals(9.975, r.pstAmount,   DELTA)
    }

    @Test fun `QC forward 1000 TPS 50 TVQ 99 75 total 1149 75`() {
        val r = calcTax(1000.0, Province.QC)
        assertEquals(50.0,   r.gstAmount,   DELTA)
        assertEquals(99.75,  r.pstAmount,   DELTA)
        assertEquals(1149.75, r.totalAmount, DELTA)
    }

    @Test fun `QC forward 0 01 total 0 0114975`() {
        val r = calcTax(0.01, Province.QC)
        assertEquals(0.0005,   r.gstAmount,   DELTA)
        assertEquals(0.0009975, r.pstAmount,  DELTA)
        assertEquals(0.0114975, r.totalAmount, DELTA)
    }

    // ── Ontario ───────────────────────────────────────────────────────────────

    @Test fun `ON forward 100 HST 13 00 total 113 00`() {
        val r = calcTax(100.0, Province.ON)
        assertEquals(13.0,  r.hstAmount,  DELTA)
        assertEquals(0.0,   r.gstAmount,  DELTA)
        assertEquals(113.0, r.totalAmount, DELTA)
    }

    @Test fun `ON inverse 113 00 base 100 00`() {
        val r = reverseTax(113.0, Province.ON)
        assertEquals(100.0, r.baseAmount,  DELTA)
        assertEquals(13.0,  r.hstAmount,  DELTA)
    }

    // ── Alberta ───────────────────────────────────────────────────────────────

    @Test fun `AB forward 100 GST 5 00 PST 0 total 105 00`() {
        val r = calcTax(100.0, Province.AB)
        assertEquals(5.0,  r.gstAmount,   DELTA)
        assertEquals(0.0,  r.pstAmount,   DELTA)
        assertEquals(105.0, r.totalAmount, DELTA)
    }

    // ── Colombie-Britannique ──────────────────────────────────────────────────

    @Test fun `BC forward 100 GST 5 00 PST 7 00 total 112 00`() {
        val r = calcTax(100.0, Province.BC)
        assertEquals(5.0,  r.gstAmount,   DELTA)
        assertEquals(7.0,  r.pstAmount,   DELTA)
        assertEquals(112.0, r.totalAmount, DELTA)
    }

    // ── Manitoba ──────────────────────────────────────────────────────────────

    @Test fun `MB forward 100 GST 5 00 RST 7 00 total 112 00`() {
        val r = calcTax(100.0, Province.MB)
        assertEquals(5.0,  r.gstAmount,   DELTA)
        assertEquals(7.0,  r.pstAmount,   DELTA)  // pstLabel = "RST"
        assertEquals(112.0, r.totalAmount, DELTA)
    }

    // ── Saskatchewan ──────────────────────────────────────────────────────────

    @Test fun `SK forward 100 GST 5 00 PST 6 00 total 111 00`() {
        val r = calcTax(100.0, Province.SK)
        assertEquals(5.0,  r.gstAmount,   DELTA)
        assertEquals(6.0,  r.pstAmount,   DELTA)
        assertEquals(111.0, r.totalAmount, DELTA)
    }

    // ── Nouveau-Brunswick ─────────────────────────────────────────────────────

    @Test fun `NB forward 100 HST 15 00 total 115 00`() {
        val r = calcTax(100.0, Province.NB)
        assertEquals(15.0, r.hstAmount,  DELTA)
        assertEquals(115.0, r.totalAmount, DELTA)
    }

    // ── Nouvelle-Écosse ───────────────────────────────────────────────────────

    @Test fun `NS forward 100 HST 15 00 total 115 00`() {
        val r = calcTax(100.0, Province.NS)
        assertEquals(15.0, r.hstAmount,  DELTA)
        assertEquals(115.0, r.totalAmount, DELTA)
    }

    // ── Île-du-Prince-Édouard ─────────────────────────────────────────────────

    @Test fun `PE forward 100 HST 15 00 total 115 00`() {
        val r = calcTax(100.0, Province.PE)
        assertEquals(15.0, r.hstAmount,  DELTA)
        assertEquals(115.0, r.totalAmount, DELTA)
    }

    // ── Terre-Neuve ───────────────────────────────────────────────────────────

    @Test fun `NL forward 100 HST 15 00 total 115 00`() {
        val r = calcTax(100.0, Province.NL)
        assertEquals(15.0, r.hstAmount,  DELTA)
        assertEquals(115.0, r.totalAmount, DELTA)
    }

    // ── Yukon ─────────────────────────────────────────────────────────────────

    @Test fun `YT forward 100 GST 5 00 PST 0 total 105 00`() {
        val r = calcTax(100.0, Province.YT)
        assertEquals(5.0,  r.gstAmount,   DELTA)
        assertEquals(0.0,  r.pstAmount,   DELTA)
        assertEquals(105.0, r.totalAmount, DELTA)
    }

    // ── Territoires du Nord-Ouest ─────────────────────────────────────────────

    @Test fun `NT forward 100 GST 5 00 total 105 00`() {
        val r = calcTax(100.0, Province.NT)
        assertEquals(5.0,  r.gstAmount,   DELTA)
        assertEquals(105.0, r.totalAmount, DELTA)
    }

    // ── Nunavut ───────────────────────────────────────────────────────────────

    @Test fun `NU forward 100 GST 5 00 total 105 00`() {
        val r = calcTax(100.0, Province.NU)
        assertEquals(5.0,  r.gstAmount,   DELTA)
        assertEquals(105.0, r.totalAmount, DELTA)
    }

    // ── Cas limites ───────────────────────────────────────────────────────────

    @Test fun `Amount 0 tout 0`() {
        val r = calcTax(0.0, Province.QC)
        assertEquals(0.0, r.gstAmount,   DELTA)
        assertEquals(0.0, r.pstAmount,   DELTA)
        assertEquals(0.0, r.totalAmount, DELTA)
    }

    // ── Pourboire sur base (pas sur taxes) ────────────────────────────────────

    @Test fun `QC 100 tip 15pct tip 15 sur base totalWithTip 129 975`() {
        val r = calcRestaurant(
            subtotal   = 100.0,
            province   = Province.QC,
            tipPercent = 0.15,
            splitCount = 1
        )
        assertEquals(15.0,   r.tipAmount, DELTA)  // tip on pre-tax base
        assertEquals(129.975, r.total,    DELTA)
    }

    @Test fun `QC 100 tip 18pct tip 18`() {
        val r = calcRestaurant(
            subtotal   = 100.0,
            province   = Province.QC,
            tipPercent = 0.18,
            splitCount = 1
        )
        assertEquals(18.0, r.tipAmount, DELTA)
    }

    // ── Liste épicerie QC ─────────────────────────────────────────────────────

    @Test fun `QC shopping 10 25 50 subtotal 85 GST 4 25 PST 8 47875 total 97 72875`() {
        val items = listOf(
            ShoppingItem(price = 10.0, displayName = "Item A"),
            ShoppingItem(price = 25.0, displayName = "Item B"),
            ShoppingItem(price = 50.0, displayName = "Item C")
        )
        val r = calcShopping(items, Province.QC)
        assertEquals(85.0,    r.subtotal,   DELTA)
        assertEquals(4.25,    r.totalGst,   DELTA)
        assertEquals(8.47875, r.totalPst,   DELTA)
        assertEquals(97.72875, r.grandTotal, DELTA)
    }

    // ── Restaurant ON split ───────────────────────────────────────────────────

    @Test fun `ON restaurant 200 tip 20pct split 4 perPerson 66 50`() {
        val r = calcRestaurant(
            subtotal   = 200.0,
            province   = Province.ON,
            tipPercent = 0.20,
            splitCount = 4
        )
        assertEquals(40.0,  r.tipAmount, DELTA)   // 20% of $200 base
        assertEquals(266.0, r.total,     DELTA)   // 200 + 26 HST + 40 tip
        assertEquals(66.5,  r.perPerson, DELTA)
    }
}
