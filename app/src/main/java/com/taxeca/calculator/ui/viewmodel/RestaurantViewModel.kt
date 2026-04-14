package com.taxeca.calculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.repository.SettingsRepository
import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.RestaurantResult
import com.taxeca.calculator.domain.model.ShoppingItem
import com.taxeca.calculator.domain.usecase.CalculateRestaurantUseCase
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
import javax.inject.Inject

/** Standard tip presets (as fractions). */
val TIP_PRESETS = listOf(0.15, 0.18, 0.20, 0.25)
/** -1.0 signals "custom" mode. */
const val TIP_CUSTOM = -1.0

@HiltViewModel
class RestaurantViewModel @Inject constructor(
    private val calculateRestaurant: CalculateRestaurantUseCase,
    private val saveCalculation: SaveCalculationUseCase,
    private val settingsRepo: SettingsRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {

    // ── Single-amount mode ────────────────────────────────────────────────────
    private val _amountInput = MutableStateFlow("")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    // ── Item mode ─────────────────────────────────────────────────────────────
    private val _itemMode = MutableStateFlow(false)
    val itemMode: StateFlow<Boolean> = _itemMode.asStateFlow()

    private val _restaurantItems = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val restaurantItems: StateFlow<List<ShoppingItem>> = _restaurantItems.asStateFlow()

    private val _itemPriceInput = MutableStateFlow("")
    val itemPriceInput: StateFlow<String> = _itemPriceInput.asStateFlow()

    private var nextItemNumber = 1

    val isItemPriceValid: StateFlow<Boolean> = _itemPriceInput.map {
        it.toDoubleOrNull()?.let { v -> v > 0.0 } == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ── Province ──────────────────────────────────────────────────────────────
    private val _selectedProvince = MutableStateFlow(Province.QC)
    val selectedProvince: StateFlow<Province> = _selectedProvince.asStateFlow()

    // ── Tip ───────────────────────────────────────────────────────────────────
    private val _selectedTipPreset = MutableStateFlow(0.15)
    val selectedTipPreset: StateFlow<Double> = _selectedTipPreset.asStateFlow()

    private val _customTipInput = MutableStateFlow("")
    val customTipInput: StateFlow<String> = _customTipInput.asStateFlow()

    private val _customTipIsAmount = MutableStateFlow(false)
    val customTipIsAmount: StateFlow<Boolean> = _customTipIsAmount.asStateFlow()

    // ── Split ─────────────────────────────────────────────────────────────────
    private val _splitCount = MutableStateFlow(1)
    val splitCount: StateFlow<Int> = _splitCount.asStateFlow()

    private val _customSplitInput = MutableStateFlow("")
    val customSplitInput: StateFlow<String> = _customSplitInput.asStateFlow()

    // ── Result ────────────────────────────────────────────────────────────────
    private val _result = MutableStateFlow<RestaurantResult?>(null)
    val result: StateFlow<RestaurantResult?> = _result.asStateFlow()

    val isInputValid: StateFlow<Boolean> =
        combine(_amountInput, _itemMode, _restaurantItems) { input, itemMode, items ->
            if (itemMode) items.isNotEmpty()
            else input.toDoubleOrNull()?.let { it > 0.0 } == true
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var calcJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepo.lastProvinceCode.collect { code ->
                _selectedProvince.value = Province.fromCode(code)
            }
        }
    }

    // ── Amount (single-amount mode) ───────────────────────────────────────────
    fun onAmountChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _amountInput.value = filtered
            scheduleCalculation()
        }
    }

    // ── Item mode ─────────────────────────────────────────────────────────────
    fun onItemModeToggle(enabled: Boolean) {
        _itemMode.value = enabled
        _result.value   = null
        if (!enabled) {
            _restaurantItems.value = emptyList()
            _itemPriceInput.value  = ""
            nextItemNumber         = 1
        } else {
            _amountInput.value = ""
        }
        scheduleCalculation()
    }

    fun onItemPriceChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) _itemPriceInput.value = filtered
    }

    fun addRestaurantItem() {
        val price = _itemPriceInput.value.toDoubleOrNull() ?: return
        if (price <= 0.0) return
        val item = ShoppingItem(price = price, displayName = "Article $nextItemNumber")
        nextItemNumber++
        _itemPriceInput.value  = ""
        _restaurantItems.value = _restaurantItems.value + item
        scheduleCalculation()
    }

    fun removeRestaurantItem(id: String) {
        _restaurantItems.value = _restaurantItems.value.filterNot { it.id == id }
        if (_restaurantItems.value.isEmpty()) _result.value = null
        scheduleCalculation()
    }

    // ── Province ──────────────────────────────────────────────────────────────
    fun onProvinceChange(province: Province) {
        _selectedProvince.value = province
        viewModelScope.launch { settingsRepo.saveLastProvince(province.code) }
        analytics.log("province_selected", "province" to province.code)
        scheduleCalculation()
    }

    // ── Tip ───────────────────────────────────────────────────────────────────
    fun onTipPresetSelected(preset: Double) {
        _selectedTipPreset.value = preset
        scheduleCalculation()
    }

    fun onCustomTipModeChange(isAmount: Boolean) {
        _customTipIsAmount.value = isAmount
        _customTipInput.value    = ""
        scheduleCalculation()
    }

    fun onCustomTipChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _customTipInput.value    = filtered
            _selectedTipPreset.value = TIP_CUSTOM
            scheduleCalculation()
        }
    }

    // ── Split ─────────────────────────────────────────────────────────────────
    fun onSplitSelected(count: Int) {
        _splitCount.value       = count
        _customSplitInput.value = ""
        scheduleCalculation()
    }

    fun onCustomSplitChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        _customSplitInput.value = filtered
        val count = filtered.toIntOrNull() ?: 1
        _splitCount.value = count.coerceAtLeast(1)
        scheduleCalculation()
    }

    // ── Calculate ─────────────────────────────────────────────────────────────
    fun calculate() {
        val subtotal: Double = if (_itemMode.value) {
            val items = _restaurantItems.value
            if (items.isEmpty()) return
            items.sumOf { it.price }
        } else {
            val amount = _amountInput.value.toDoubleOrNull() ?: return
            if (amount <= 0.0) return
            amount
        }

        val tipFraction = if (_selectedTipPreset.value == TIP_CUSTOM) {
            if (_customTipIsAmount.value) {
                val tipAmt = _customTipInput.value.toDoubleOrNull() ?: 0.0
                if (tipAmt > 0 && subtotal > 0) tipAmt / subtotal else 0.0
            } else {
                (_customTipInput.value.toDoubleOrNull() ?: 0.0) / 100.0
            }
        } else {
            _selectedTipPreset.value
        }

        try {
            val province = _selectedProvince.value
            analytics.setKey("province", province.code)
            analytics.setKey("screen", "Restaurant")
            _result.value = calculateRestaurant(
                subtotal   = subtotal,
                province   = province,
                tipPercent = tipFraction,
                splitCount = _splitCount.value.coerceAtLeast(1)
            )
            analytics.log(
                "restaurant_calc_used",
                "tip_pct" to (tipFraction * 100).toInt(),
                "split"   to _splitCount.value
            )
        } catch (e: Exception) {
            analytics.recordException(e)
        }
    }

    // ── Save + feedback ───────────────────────────────────────────────────────
    private val _saveConfirmed = MutableStateFlow(false)
    val saveConfirmed: StateFlow<Boolean> = _saveConfirmed.asStateFlow()

    fun saveToHistory() {
        val r = _result.value ?: return
        viewModelScope.launch {
            saveCalculation(r, if (_itemMode.value) _restaurantItems.value else emptyList())
            if (_itemMode.value) {
                analytics.log("restaurant_articles_mode", "nb_articles" to _restaurantItems.value.size)
            }
            _saveConfirmed.value = true
            delay(1_500)
            resetDisplay()
        }
    }

    fun logShare() = analytics.log("restaurant_calc_shared")

    fun resetDisplay() {
        _amountInput.value       = ""
        _result.value            = null
        _selectedTipPreset.value = 0.15
        _customTipInput.value    = ""
        _customTipIsAmount.value = false
        _splitCount.value        = 1
        _customSplitInput.value  = ""
        _saveConfirmed.value     = false
        _itemMode.value          = false
        _restaurantItems.value   = emptyList()
        _itemPriceInput.value    = ""
        nextItemNumber           = 1
    }

    private fun scheduleCalculation() {
        calcJob?.cancel()
        calcJob = viewModelScope.launch {
            delay(300)
            calculate()
        }
    }
}
