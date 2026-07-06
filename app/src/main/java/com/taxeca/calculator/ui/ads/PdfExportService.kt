package com.taxeca.calculator.ui.ads

import android.content.Context
import android.content.Intent
import com.taxeca.calculator.BuildConfig
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: AnalyticsManager
) {
    companion object {
        private const val TAG = "PdfExportService"
        private const val PAGE_WIDTH  = 595   // A4 72dpi
        private const val PAGE_HEIGHT = 842
        private const val MARGIN      = 48f
        private const val RIGHT_EDGE  = PAGE_WIDTH - MARGIN
        private val BRAND_RED = Color.rgb(180, 0, 0)
    }

    suspend fun exportAndShare(entity: HistoryEntity, isFrench: Boolean) {
        try {
            val pdf  = buildPdf(entity, isFrench)
            val file = writePdf(pdf)
            withContext(Dispatchers.Main) { sharePdf(file) }
            analytics.log("pdf_exported", "mode" to entity.mode)
        } catch (e: Exception) {
            // Coroutine cancellation is normal (user navigated away / job cancelled) —
            // re-throw so it isn't logged to Crashlytics as a non-fatal.
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (BuildConfig.DEBUG) Log.e(TAG, "PDF export failed", e)
            analytics.recordException(e)
        }
    }

    private fun buildPdf(e: HistoryEntity, fr: Boolean): PdfDocument {
        val fmt  = NumberFormat.getCurrencyInstance(if (fr) Locale.CANADA_FRENCH else Locale.CANADA)
        val dateFmt = if (fr) "d MMMM yyyy" else "MMMM d, yyyy"
        val date = SimpleDateFormat(dateFmt, if (fr) Locale.FRENCH else Locale.ENGLISH)
            .format(Date(e.timestamp))

        val province = Province.fromCode(e.provinceCode)
        val provinceName = if (fr) province.nameFr else province.nameEn

        val doc  = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(info)
        val c    = page.canvas

        // ── Paints (separate instances — no mutation) ────────────────────────
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 22f; isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY; textSize = 11f
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY; textSize = 11f; textAlign = Paint.Align.RIGHT
        }
        val headerBgPaint = Paint().apply {
            color = BRAND_RED; style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 11f; isFakeBoldText = true
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY; textSize = 11f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 11f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 13f; isFakeBoldText = true
        }
        val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BRAND_RED; textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        val dividerPaint = Paint().apply {
            color = Color.LTGRAY; strokeWidth = 0.5f
        }
        val redLinePaint = Paint().apply {
            color = BRAND_RED; strokeWidth = 2f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY; textSize = 8f
        }

        var y = MARGIN

        // ── Header ───────────────────────────────────────────────────────────
        c.drawText("TaxeCA", MARGIN, y + 20f, titlePaint)
        c.drawText(date, RIGHT_EDGE, y + 12f, datePaint)
        y += 28f
        c.drawText(
            if (fr) "Résumé du calcul de taxe" else "Tax Calculation Summary",
            MARGIN, y, subtitlePaint
        )
        y += 8f
        c.drawLine(MARGIN, y, RIGHT_EDGE, y, redLinePaint)
        y += 24f

        // ── Helpers ──────────────────────────────────────────────────────────
        fun sectionHeader(title: String) {
            c.drawRect(MARGIN, y, RIGHT_EDGE, y + 20f, headerBgPaint)
            c.drawText(title, MARGIN + 8f, y + 14f, headerTextPaint)
            y += 26f
        }

        fun row(label: String, value: String) {
            c.drawText(label, MARGIN + 8f, y + 12f, labelPaint)
            c.drawText(value, RIGHT_EDGE - 8f, y + 12f, valuePaint)
            y += 20f
            c.drawLine(MARGIN, y, RIGHT_EDGE, y, dividerPaint)
            y += 2f
        }

        fun totalRow(label: String, value: String) {
            y += 4f
            c.drawText(label, MARGIN + 8f, y + 14f, totalLabelPaint)
            c.drawText(value, RIGHT_EDGE - 8f, y + 14f, totalValuePaint)
            y += 24f
        }

        // ── Calculation Details ──────────────────────────────────────────────
        sectionHeader(if (fr) "DÉTAILS DU CALCUL" else "CALCULATION DETAILS")
        row(if (fr) "Province" else "Province", "$provinceName (${e.provinceCode})")
        val modeLabel = when (e.mode) {
            "FORWARD"    -> if (fr) "Avant" else "Forward"
            "REVERSE"    -> if (fr) "Inverse" else "Reverse"
            "SHOPPING"   -> if (fr) "Liste d'achats" else "Shopping"
            "RESTAURANT" -> "Restaurant"
            else -> e.mode
        }
        row(if (fr) "Mode" else "Mode", modeLabel)
        if (e.mode == "REVERSE") {
            row(if (fr) "Montant payé" else "Amount Paid", fmt.format(e.inputAmount))
        }
        row(if (fr) "Montant de base" else "Base Amount", fmt.format(e.baseAmount))
        y += 10f

        // ── Tax Breakdown ────────────────────────────────────────────────────
        sectionHeader(if (fr) "DÉTAIL DES TAXES" else "TAX BREAKDOWN")
        if (!province.isHstProvince && e.gstAmount > 0) {
            val rate = "${(province.gstRate * 100).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.3f", it) }}%"
            row(if (fr) "TPS ($rate)" else "GST ($rate)", fmt.format(e.gstAmount))
        }
        if (!province.isHstProvince && e.pstAmount > 0) {
            val pstLabel = when (province.pstLabel) {
                "QST" -> if (fr) "TVQ" else "QST"
                "RST" -> "RST"
                else  -> if (fr) "TVP" else "PST"
            }
            val rate = "${(province.pstRate * 100).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.3f", it) }}%"
            row("$pstLabel ($rate)", fmt.format(e.pstAmount))
        }
        if (province.isHstProvince && e.hstAmount > 0) {
            val rate = "${(province.hstRate * 100).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.3f", it) }}%"
            row(if (fr) "TVH ($rate)" else "HST ($rate)", fmt.format(e.hstAmount))
        }
        row(if (fr) "Total des taxes" else "Total Tax", fmt.format(e.totalTax))
        y += 10f

        // ── Tip (Restaurant or Calculator-with-tip) ──────────────────────────
        if (e.tipAmount > 0.005) {
            row(if (fr) "Pourboire" else "Tip", fmt.format(e.tipAmount))
            y += 6f
        }

        // ── Total ────────────────────────────────────────────────────────────
        c.drawLine(MARGIN, y, RIGHT_EDGE, y, redLinePaint)
        y += 4f
        totalRow(if (fr) "TOTAL" else "TOTAL", fmt.format(e.totalAmount))

        if (e.splitCount > 1) {
            y += 2f
            row(
                if (fr) "Par personne (÷${e.splitCount})" else "Per Person (÷${e.splitCount})",
                fmt.format(e.totalAmount / e.splitCount)
            )
        }

        // ── Footer ───────────────────────────────────────────────────────────
        val footerY = (PAGE_HEIGHT - 40).toFloat()
        c.drawLine(MARGIN, footerY, RIGHT_EDGE, footerY, dividerPaint)
        c.drawText(
            if (fr) "Généré par TaxeCA · À titre indicatif seulement · calcwise.com"
            else "Generated by TaxeCA · For illustration purposes only · calcwise.com",
            MARGIN, footerY + 15f, footerPaint
        )

        doc.finishPage(page)
        return doc
    }

    private fun writePdf(doc: PdfDocument): File {
        val dir  = File(context.cacheDir, "pdfs").also { it.mkdirs() }
        val file = File(dir, "taxeca_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type  = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, if (Locale.getDefault().language == "fr") "Partager PDF" else "Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
