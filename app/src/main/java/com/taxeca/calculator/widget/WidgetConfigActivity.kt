package com.taxeca.calculator.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.taxeca.calculator.domain.model.Province

class WidgetConfigActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "widget_prefs"

        fun keyProvinceCode(widgetId: Int) = "province_$widgetId"
        fun keyAmount(widgetId: Int)       = "amount_$widgetId"
        fun keyLang(widgetId: Int)         = "lang_$widgetId"
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var spinner: Spinner
    private lateinit var etAmount: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Résultat par défaut = annulé (obligatoire pour les configs widget)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(buildLayout())
    }

    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        // Title
        val tvTitle = TextView(this).apply {
            text = "TaxeCA — Widget"
            textSize = 20f
            setTextColor(0xFFC62828.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(tvTitle, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 32 })

        // Subtitle "Province"
        val tvProvinceLabel = TextView(this).apply {
            text = "Province / Territory"
            textSize = 14f
            setTextColor(0xFF555555.toInt())
        }
        root.addView(tvProvinceLabel)

        // Spinner provinces
        spinner = Spinner(this)
        val provinces = Province.entries.toList()
        val names = provinces.map { "${it.nameEn} / ${it.nameFr}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Pre-select saved province
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedCode = prefs.getString(keyProvinceCode(appWidgetId), "QC") ?: "QC"
        val savedIndex = provinces.indexOfFirst { it.code == savedCode }.coerceAtLeast(0)
        spinner.setSelection(savedIndex)

        root.addView(spinner, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8; bottomMargin = 24 })

        // Label "Montant"
        val tvAmountLabel = TextView(this).apply {
            text = "Amount / Montant ($)"
            textSize = 14f
            setTextColor(0xFF555555.toInt())
        }
        root.addView(tvAmountLabel)

        // EditText montant
        etAmount = EditText(this).apply {
            hint = "100.00"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            val savedAmount = prefs.getFloat(keyAmount(appWidgetId), 100f)
            setText(savedAmount.toString())
            textSize = 16f
        }
        root.addView(etAmount, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8; bottomMargin = 32 })

        // Bouton Appliquer
        val btnApply = Button(this).apply {
            text = "Appliquer / Apply"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFC62828.toInt())
            isAllCaps = false
        }
        btnApply.setOnClickListener { applyWidget(provinces) }

        root.addView(btnApply, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        return root
    }

    private fun applyWidget(provinces: List<Province>) {
        val selectedProvince = provinces[spinner.selectedItemPosition]
        val amountText = etAmount.text.toString().trim()
        val amount = amountText.toFloatOrNull() ?: 100f

        // Détecter la langue système (fr ou en)
        val langCode = if (resources.configuration.locales[0].language == "fr") "fr" else "en"

        // Sauvegarde dans SharedPreferences
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(keyProvinceCode(appWidgetId), selectedProvince.code)
            .putFloat(keyAmount(appWidgetId), amount)
            .putString(keyLang(appWidgetId), langCode)
            .apply()

        // Mise à jour du widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        TaxeCAWidget.updateWidget(this, appWidgetManager, appWidgetId)

        // Fermer avec succès
        val resultIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
