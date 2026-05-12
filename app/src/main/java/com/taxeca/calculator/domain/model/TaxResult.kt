package com.taxeca.calculator.domain.model

data class TaxResult(
    val province: Province,
    val mode: CalculationMode,
    val inputAmount: Double,
    val baseAmount: Double,
    val gstAmount: Double,
    val pstAmount: Double,
    val hstAmount: Double,
    val totalTax: Double,
    val totalAmount: Double
)

/**
 * Structured breakdown of a tax result — eliminates the isHstProvince/GST/PST/HST
 * if-else pattern that was duplicated 6+ times across ResultCard, CalculatorScreen,
 * HistoryDetailScreen, RestaurantScreen, ShoppingScreen and PdfExportService.
 *
 * Call [TaxResult.taxLines] once; render the returned list.
 */
data class TaxLine(
    val label: String,   // e.g. "GST", "QST", "HST"
    val amount: Double
)

/**
 * Returns the list of non-zero tax lines for this result.
 * Handles both HST provinces and GST+PST/QST/RST provinces transparently.
 */
fun TaxResult.taxLines(): List<TaxLine> = buildList {
    if (province.isHstProvince) {
        if (hstAmount > 0.0) add(TaxLine("HST", hstAmount))
    } else {
        if (gstAmount > 0.0) add(TaxLine("GST", gstAmount))
        if (pstAmount > 0.0) add(TaxLine(province.pstLabel.ifBlank { "PST" }, pstAmount))
    }
}
