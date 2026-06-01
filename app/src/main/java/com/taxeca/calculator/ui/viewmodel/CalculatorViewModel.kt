package com.taxeca.calculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.repository.SettingsRepository
import com.taxeca.calculator.domain.model.CalculationMode
import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.TaxResult
import com.taxeca.calculator.domain.usecase.CalculateTaxUseCase
import com.taxeca.calculator.domain.usecase.ReverseCalculateTaxUseCase
import com.taxeca.calculator.domain.usecase.SaveCalculationUseCase
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculateTax: CalculateTaxUseCase,
    private val reverseCalculateTax: ReverseCalculateTaxUseCase,
    private val saveCalculation: SaveCalculationUseCase,
    private val settingsRepo: SettingsRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _amountInput = MutableStateFlow("")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    private val _selectedProvince = MutableStateFlow(Province.QC)
    val selectedProvince: StateFlow<Province> = _selectedProvince.asStateFlow()

    private val _mode = MutableStateFlow(CalculationMode.FORWARD)
    val mode: StateFlow<CalculationMode> = _mode.asStateFlow()

    private val _taxResult = MutableStateFlow<TaxResult?>(null)
    val taxResult: StateFlow<TaxResult?> = _taxResult.asStateFlow()

    val isInputValid: StateFlow<Boolean> = _amountInput.map { input ->
        input.toDoubleOrNull()?.let { it > 0.0 } == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ── Tip ───────────────────────────────────────────────────────────────────
    private val _tipEnabled = MutableStateFlow(false)
    val tipEnabled: StateFlow<Boolean> = _tipEnabled.asStateFlow()

    private val _tipPreset = MutableStateFlow(0.15)
    val tipPreset: StateFlow<Double> = _tipPreset.asStateFlow()

    private val _customTipInput = MutableStateFlow("")
    val customTipInput: StateFlow<String> = _customTipInput.asStateFlow()

    /** true = user entered a fixed dollar amount; false = percentage */
    private val _customTipIsAmount = MutableStateFlow(false)
    val customTipIsAmount: StateFlow<Boolean> = _customTipIsAmount.asStateFlow()

    // Intermediate: bundles the three custom-tip inputs so downstream can use 3-arg combine
    private val tipConfig = combine(_tipPreset, _customTipInput, _customTipIsAmount) { preset, input, isAmt ->
        Triple(preset, input, isAmt)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Triple(0.15, "", false))

    val tipAmount: StateFlow<Double> = combine(_taxResult, _tipEnabled, tipConfig) { result, enabled, (preset, input, isAmt) ->
        if (result == null || !enabled) 0.0
        else when {
            preset != TIP_CUSTOM -> result.baseAmount * preset
            isAmt                -> input.toDoubleOrNull() ?: 0.0
            else                 -> result.baseAmount * (input.toDoubleOrNull() ?: 0.0) / 100.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val grandTotal: StateFlow<Double> = combine(_taxResult, tipAmount) { result, tip ->
        (result?.totalAmount ?: 0.0) + tip
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Split ─────────────────────────────────────────────────────────────────
    private val _splitEnabled = MutableStateFlow(false)
    val splitEnabled: StateFlow<Boolean> = _splitEnabled.asStateFlow()

    private val _splitCount = MutableStateFlow(2)
    val splitCount: StateFlow<Int> = _splitCount.asStateFlow()

    private val _customSplitInput = MutableStateFlow("")
    val customSplitInput: StateFlow<String> = _customSplitInput.asStateFlow()

    val perPerson: StateFlow<Double> = combine(grandTotal, _splitEnabled, _splitCount) { total, enabled, count ->
        if (!enabled || count < 2) 0.0 else total / count
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Save feedback ─────────────────────────────────────────────────────────
    private val _saveConfirmed = MutableStateFlow(false)
    val saveConfirmed: StateFlow<Boolean> = _saveConfirmed.asStateFlow()

    private var calculateJob: Job? = null

    init {
        _amountInput.value = "100"
        viewModelScope.launch {
            settingsRepo.lastProvinceCode.collect { code ->
                // Smart Auto: empty code = first launch, no explicit user selection.
                // Detect province from device locale:
                //   French (fr_*)  → QC  (most French Canadians are in Quebec)
                //   English CA (en_CA) → ON  (most populous English-CA province)
                //   Other → QC (international or unrecognized locale)
                val resolvedCode = when {
                    code.isNotEmpty() -> code  // Respect saved user preference
                    Locale.getDefault().language == "fr" -> "QC"
                    Locale.getDefault().country == "CA"  -> "ON"
                    else -> "QC"
                }
                _selectedProvince.value = Province.fromCode(resolvedCode)
                scheduleCalculation()
            }
        }
    }

    fun onAmountChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _amountInput.value = filtered
            scheduleCalculation()
        }
    }

    fun onProvinceChange(province: Province) {
        _selectedProvince.value = province
        viewModelScope.launch { settingsRepo.saveLastProvince(province.code) }
        analytics.log("province_selected", "province" to province.code)
        scheduleCalculation()
    }

    fun onModeChange(newMode: CalculationMode) {
        _mode.value = newMode
        scheduleCalculation()
    }

    fun onTipToggle(enabled: Boolean) {
        _tipEnabled.value = enabled
        if (enabled) analytics.log("calc_tip_used", "tip_pct" to (_tipPreset.value * 100).toInt())
    }

    fun onTipPresetSelected(preset: Double) { _tipPreset.value = preset }

    fun onCustomTipChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _customTipInput.value = filtered
            _tipPreset.value = TIP_CUSTOM
        }
    }

    fun onCustomTipModeChange(isAmount: Boolean) {
        _customTipIsAmount.value = isAmount
        _customTipInput.value = ""
    }

    fun onSplitToggle(enabled: Boolean) {
        _splitEnabled.value = enabled
        if (enabled) analytics.log("calc_split_used", "split_count" to _splitCount.value)
    }

    fun onSplitCountSelected(count: Int) {
        _splitCount.value = count
        _customSplitInput.value = ""
    }

    fun onCustomSplitChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        _customSplitInput.value = filtered
        _splitCount.value = (filtered.toIntOrNull() ?: 2).coerceAtLeast(2)
    }

    /** Save result to history, show brief confirmation, then reset display. */
    fun saveToHistory() {
        val result = _taxResult.value ?: return
        val split  = if (_splitEnabled.value) _splitCount.value else 1
        viewModelScope.launch {
            saveCalculation(result, splitCount = split)
            _saveConfirmed.value = true
            delay(1_500)
            resetDisplay()
        }
    }

    fun logShareResult(provinceCode: String, format: String) {
        analytics.log("share_result", "province" to provinceCode, "format" to format)
    }

    fun resetDisplay() {
        _amountInput.value       = ""
        _taxResult.value         = null
        _tipEnabled.value        = false
        _tipPreset.value         = 0.15
        _customTipInput.value    = ""
        _customTipIsAmount.value = false
        _splitEnabled.value      = false
        _splitCount.value        = 2
        _customSplitInput.value  = ""
        _saveConfirmed.value     = false
    }

    private fun scheduleCalculation() {
        calculateJob?.cancel()
        calculateJob = viewModelScope.launch {
            delay(300)
            val input = _amountInput.value.toDoubleOrNull() ?: return@launch
            if (input <= 0.0) return@launch
            try {
                val province = _selectedProvince.value
                analytics.setKey("province", province.code)
                analytics.setKey("screen", "Calculator")
                // Analytics logged only on Save (saveToHistory) to avoid per-keystroke event spam.
                // Crashlytics keys kept here for debugging context if a crash happens.
                _taxResult.value = when (_mode.value) {
                    CalculationMode.FORWARD -> calculateTax(input, province)
                    CalculationMode.REVERSE -> reverseCalculateTax(input, province)
                }
            } catch (e: Exception) {
                analytics.recordException(e)
            }
        }
    }
}
