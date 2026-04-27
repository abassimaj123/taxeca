package com.taxeca.calculator.ui.ads

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
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
        private val RED = Color.rgb(180, 0, 0)
    }

    fun exportAndShare(entity: HistoryEntity, isFrench: Boolean) {
        try {
            val pdf  = buildPdf(entity, isFrench)
            val file = writePdf(pdf)
            sharePdf(file)
            analytics.log("pdf_exported", "mode" to entity.mode)
        } catch (e: Exception) {
            Log.e(TAG, "PDF export failed", e)
            analytics.recordException(e)
        }
    }

    private fun buildPdf(e: HistoryEntity, fr: Boolean): PdfDocument {
        val fmt  = NumberFormat.getCurrencyInstance(if (fr) Locale.CANADA_FRENCH else Locale.CANADA)
        val date = SimpleDateFormat("MMMM d, yyyy", if (fr) Locale.FRENCH else Locale.ENGLISH)
            .format(Date(e.timestamp))

        val doc  = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(info)
        val c    = page.canvas

        val boldPaint  = Paint().apply { color = Color.BLACK; textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        val labelPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }
        val valuePaint = Paint().apply { color = Color.BLACK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        val headerPaint= Paint().apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        val bgRed      = Paint().apply { color = RED; style = Paint.Style.FILL }
        val divider    = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE }

        var y = MARGIN

        // Title
        c.drawText("TaxeCA", MARGIN, y + 18, boldPaint.apply { textSize = 20f })
        c.drawText(date, (PAGE_WIDTH - MARGIN), y + 8, labelPaint.apply { textAlign = Paint.Align.RIGHT })
        y += 30f
        c.drawText(if (fr) "Résumé du calcul de taxe" else "Tax Calculation Summary", MARGIN, y, labelPaint.apply { textAlign = Paint.Align.LEFT; color = Color.GRAY })
        y += 6f
        c.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = RED; strokeWidth = 2f })
        y += 20f

        // Section header
        fun sectionHeader(title: String) {
            c.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f, bgRed)
            c.drawText(title, MARGIN + 6f, y + 13f, headerPaint.apply { textSize = 10f })
            y += 22f
        }

        fun row(label: String, value: String) {
            c.drawText(label, MARGIN + 6f, y, labelPaint.apply { color = Color.DKGRAY; textSize = 10f; textAlign = Paint.Align.LEFT })
            c.drawText(value, PAGE_WIDTH - MARGIN - 6f, y, valuePaint.apply { textSize = 10f })
            y += 16f
            c.drawLine(MARGIN, y - 2f, PAGE_WIDTH - MARGIN, y - 2f, divider)
        }

        // Province & mode
        sectionHeader(if (fr) "DÉTAILS DU CALCUL" else "CALCULATION DETAILS")
        row(if (fr) "Province" else "Province", e.provinceCode)
        row(if (fr) "Mode" else "Mode", e.mode)
        row(if (fr) "Montant saisi" else "Input Amount", fmt.format(e.inputAmount))
        row(if (fr) "Montant de base" else "Base Amount", fmt.format(e.baseAmount))
        y += 8f

        // Tax breakdown
        sectionHeader(if (fr) "DÉTAIL DES TAXES" else "TAX BREAKDOWN")
        if (e.gstAmount > 0) row(if (fr) "TPS (GST)" else "GST", fmt.format(e.gstAmount))
        if (e.pstAmount > 0) row(if (fr) "TVP (PST)" else "PST/QST", fmt.format(e.pstAmount))
        if (e.hstAmount > 0) row(if (fr) "TVH (HST)" else "HST", fmt.format(e.hstAmount))
        row(if (fr) "Total taxes" else "Total Tax", fmt.format(e.totalTax))
        y += 8f

        // Total
        sectionHeader(if (fr) "TOTAL" else "TOTAL")
        row(if (fr) "Montant total" else "Total Amount", fmt.format(e.totalAmount))
        if (e.splitCount > 1) row(if (fr) "Par personne (${e.splitCount})" else "Per Person (${e.splitCount})", fmt.format(e.totalAmount / e.splitCount))

        // Footer
        val footerPaint = Paint().apply { color = Color.GRAY; textSize = 8f; isAntiAlias = true }
        c.drawLine(MARGIN, (PAGE_HEIGHT - 40).toFloat(), PAGE_WIDTH - MARGIN, (PAGE_HEIGHT - 40).toFloat(), divider)
        c.drawText(
            if (fr) "Généré par TaxeCA · À titre indicatif seulement."
            else "Generated by TaxeCA · For illustration purposes only.",
            MARGIN, (PAGE_HEIGHT - 25).toFloat(), footerPaint
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
        context.startActivity(Intent.createChooser(intent, "Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
