package com.taxeca.calculator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.taxeca.calculator.MainActivity
import com.taxeca.calculator.R
import com.taxeca.calculator.data.repository.LanguageManager
import com.taxeca.calculator.domain.model.Province
import java.text.NumberFormat
import java.util.Locale

class TaxeCAWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Re-render content when the user resizes the widget (min width/height changed).
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {

        private val CAD_FORMAT = NumberFormat.getCurrencyInstance(Locale.CANADA)

        internal fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(WidgetConfigActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val provinceCode = prefs.getString(
                WidgetConfigActivity.keyProvinceCode(appWidgetId),
                "QC"
            ) ?: "QC"
            val amount = prefs.getFloat(
                WidgetConfigActivity.keyAmount(appWidgetId),
                100f
            ).toDouble()
            // Prefer the app-wide language preference (kept in sync with the in-app
            // language picker) so the widget never drifts from a value captured once
            // at widget-configure time. Fall back to the current system locale, and
            // only use the widget's own stored flag as a last resort.
            val appLangPrefs = context.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
            val appLang = appLangPrefs.getString(LanguageManager.KEY_LANG, null)
            val systemLang = context.resources.configuration.locales[0].language
            val widgetLang = prefs.getString(WidgetConfigActivity.keyLang(appWidgetId), null)
            val useFrench = when {
                appLang != null -> appLang.startsWith("fr")
                widgetLang != null -> widgetLang == "fr"
                else -> systemLang == "fr"
            }

            val province = Province.fromCode(provinceCode)
            val provinceName = if (useFrench) province.nameFr else province.nameEn

            // --- Calcul des taxes ---
            val gstAmount: Double
            val pstAmount: Double
            val hstAmount: Double
            val totalTax: Double
            val totalAmount: Double

            if (province.isHstProvince) {
                hstAmount = amount * province.hstRate
                gstAmount = 0.0
                pstAmount = 0.0
                totalTax = hstAmount
            } else {
                hstAmount = 0.0
                gstAmount = amount * province.gstRate
                pstAmount = amount * province.pstRate
                totalTax = gstAmount + pstAmount
            }
            totalAmount = amount + totalTax

            // --- RemoteViews ---
            val views = RemoteViews(context.packageName, R.layout.widget_taxeca)

            views.setTextViewText(R.id.widget_province, provinceName)
            views.setTextViewText(R.id.widget_amount, CAD_FORMAT.format(amount))
            views.setTextViewText(R.id.widget_total_value, CAD_FORMAT.format(totalAmount))
            views.setTextViewText(R.id.widget_total_label, if (useFrench) "Total TTC" else "Total")
            views.setTextViewText(R.id.widget_open_btn, if (useFrench) "Ouvrir" else "Open")

            if (province.isHstProvince) {
                // Line 1: HST
                val hstLabel = buildTaxLabel("HST", province.hstRate)
                views.setTextViewText(R.id.widget_tax1_label, hstLabel)
                views.setTextViewText(R.id.widget_tax1_value, CAD_FORMAT.format(hstAmount))
                // Line 2: hidden
                views.setViewVisibility(R.id.widget_row2, View.GONE)
            } else {
                // Line 1: GST
                if (province.gstRate > 0.0) {
                    val gstLabel = buildTaxLabel("GST", province.gstRate)
                    views.setTextViewText(R.id.widget_tax1_label, gstLabel)
                    views.setTextViewText(R.id.widget_tax1_value, CAD_FORMAT.format(gstAmount))
                    views.setViewVisibility(R.id.widget_row1, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_row1, View.GONE)
                }
                // Line 2: PST/QST/RST
                if (province.pstRate > 0.0 && province.pstLabel.isNotEmpty()) {
                    val pstLabel = buildTaxLabel(province.pstLabel, province.pstRate)
                    views.setTextViewText(R.id.widget_tax2_label, pstLabel)
                    views.setTextViewText(R.id.widget_tax2_value, CAD_FORMAT.format(pstAmount))
                    views.setViewVisibility(R.id.widget_row2, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_row2, View.GONE)
                }
            }

            // --- PendingIntent widget entier → WidgetConfigActivity ---
            val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val configPi = PendingIntent.getActivity(
                context,
                appWidgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, configPi)
            views.setOnClickPendingIntent(R.id.widget_results, configPi)
            views.setOnClickPendingIntent(R.id.widget_amount, configPi)

            // --- PendingIntent bouton "Ouvrir" → MainActivity ---
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPi = PendingIntent.getActivity(
                context,
                appWidgetId + 10000,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_open_btn, mainPi)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun buildTaxLabel(label: String, rate: Double): String {
            val pct = (rate * 100).let {
                if (it % 1.0 == 0.0) it.toInt().toString() else "%.3g".format(it)
            }
            return "$label ($pct%)"
        }
    }
}
