package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.ShoppingItem
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

private const val DELTA = 0.0001

class CalculateShoppingListUseCaseTest {

    private lateinit var useCase: CalculateShoppingListUseCase

    @Before
    fun setUp() {
        useCase = CalculateShoppingListUseCase()
    }

    // ── Quebec GST+PST ────────────────────────────────────────────────────────


    @Test fun `QC three items 10 25 50`() {
        val items = listOf(
            ShoppingItem(price = 10.0, displayName = "Item A"),
            ShoppingItem(price = 25.0, displayName = "Item B"),
            ShoppingItem(price = 50.0, displayName = "Item C")
        )
        val result = useCase(items, Province.QC)
        assertEquals(85.0, result.subtotal, DELTA)
        assertEquals(4.25, result.totalGst, DELTA)
        assertEquals(8.47875, result.totalPst, DELTA)
        assertEquals(97.72875, result.grandTotal, DELTA)
    }


    // ── Ontario HST ───────────────────────────────────────────────────────────

    @Test fun `ON single item 100 dollars HST 13`() {
        val items = listOf(
            ShoppingItem(price = 100.0, displayName = "Item A")
        )
        val result = useCase(items, Province.ON)
        assertEquals(100.0, result.subtotal, DELTA)
        assertEquals(0.0, result.totalGst, DELTA)
        assertEquals(0.0, result.totalPst, DELTA)
        assertEquals(13.0, result.totalHst, DELTA)
        assertEquals(113.0, result.grandTotal, DELTA)
    }

    @Test fun `ON three items combined`() {
        val items = listOf(
            ShoppingItem(price = 50.0, displayName = "A"),
            ShoppingItem(price = 75.0, displayName = "B"),
            ShoppingItem(price = 100.0, displayName = "C")
        )
        val result = useCase(items, Province.ON)
        assertEquals(225.0, result.subtotal, DELTA)
        assertEquals(29.25, result.totalHst, DELTA)
        assertEquals(254.25, result.grandTotal, DELTA)
    }

    // ── British Columbia GST+PST ──────────────────────────────────────────────

    @Test fun `BC single item 100 GST 5 PST 7`() {
        val items = listOf(
            ShoppingItem(price = 100.0, displayName = "Item A")
        )
        val result = useCase(items, Province.BC)
        assertEquals(100.0, result.subtotal, DELTA)
        assertEquals(5.0, result.totalGst, DELTA)
        assertEquals(7.0, result.totalPst, DELTA)
        assertEquals(112.0, result.grandTotal, DELTA)
    }

    // ── Manitoba RST ──────────────────────────────────────────────────────────

    @Test fun `MB single item 100 GST 5 RST 7`() {
        val items = listOf(
            ShoppingItem(price = 100.0, displayName = "Item A")
        )
        val result = useCase(items, Province.MB)
        assertEquals(100.0, result.subtotal, DELTA)
        assertEquals(5.0,   result.totalGst, DELTA)
        assertEquals(7.0,   result.totalPst, DELTA)  // Manitoba RST 7% (since 2019-07-01)
        assertEquals(112.0, result.grandTotal, DELTA)
    }

    // ── Empty Cart ────────────────────────────────────────────────────────────

    @Test fun `Empty cart zero taxes`() {
        val items = emptyList<ShoppingItem>()
        val result = useCase(items, Province.QC)
        assertEquals(0.0, result.subtotal, DELTA)
        assertEquals(0.0, result.totalGst, DELTA)
        assertEquals(0.0, result.totalPst, DELTA)
        assertEquals(0.0, result.grandTotal, DELTA)
    }

    // ── Fractional Prices ────────────────────────────────────────────────────

    @Test fun `QC fractional prices 0 99 1 49 2 99`() {
        val items = listOf(
            ShoppingItem(price = 0.99, displayName = "A"),
            ShoppingItem(price = 1.49, displayName = "B"),
            ShoppingItem(price = 2.99, displayName = "C")
        )
        val result = useCase(items, Province.QC)
        assertEquals(5.47, result.subtotal, 0.001)
        assertEquals(true, result.totalGst > 0.0)
        assertEquals(true, result.totalPst > 0.0)
    }

    // ── Very Small Items ──────────────────────────────────────────────────────

    @Test fun `Items under penny threshold`() {
        val items = listOf(
            ShoppingItem(price = 0.01, displayName = "Tiny"),
            ShoppingItem(price = 0.01, displayName = "Tiny"),
            ShoppingItem(price = 0.01, displayName = "Tiny")
        )
        val result = useCase(items, Province.QC)
        assertEquals(0.03, result.subtotal, DELTA)
        assertEquals(true, result.grandTotal > 0.03)
    }

    // ── Multi-item with many items ────────────────────────────────────────────

    @Test fun `Many items 10 items with mixed prices`() {
        val items = (1..10).map { i ->
            ShoppingItem(price = (i * 10).toDouble(), displayName = "Item $i")
        }
        val result = useCase(items, Province.ON)
        val expectedSubtotal = (1..10).sumOf { (it * 10).toDouble() }
        assertEquals(expectedSubtotal, result.subtotal, DELTA)
        assertEquals(expectedSubtotal * 0.13, result.totalHst, DELTA)
    }

    // ── Tax Calculations Precision ────────────────────────────────────────────


    @Test fun `Item count preserved in result`() {
        val items = listOf(
            ShoppingItem(price = 10.0, displayName = "A"),
            ShoppingItem(price = 20.0, displayName = "B")
        )
        val result = useCase(items, Province.QC)
        assertEquals(2, result.items.size)
    }
}
