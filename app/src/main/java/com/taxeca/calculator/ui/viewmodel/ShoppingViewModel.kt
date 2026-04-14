package com.taxeca.calculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.repository.SettingsRepository
import com.taxeca.calculator.domain.model.Province
import com.taxeca.calculator.domain.model.ShoppingItem
import com.taxeca.calculator.domain.model.ShoppingListResult
import com.taxeca.calculator.domain.usecase.CalculateShoppingListUseCase
import com.taxeca.calculator.domain.usecase.SaveCalculationUseCase
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val calculateShoppingList: CalculateShoppingListUseCase,
    private val saveCalculation: SaveCalculationUseCase,
    private val settingsRepo: SettingsRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _items = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val items: StateFlow<List<ShoppingItem>> = _items.asStateFlow()

    private val _selectedProvince = MutableStateFlow(Province.QC)
    val selectedProvince: StateFlow<Province> = _selectedProvince.asStateFlow()

    /** Price field — the only input needed to add an item. */
    private val _priceInput = MutableStateFlow("")
    val priceInput: StateFlow<String> = _priceInput.asStateFlow()

    /** Controls the "clear all?" confirmation dialog. */
    private val _showClearDialog = MutableStateFlow(false)
    val showClearDialog: StateFlow<Boolean> = _showClearDialog.asStateFlow()

    /** Auto-incrementing counter — never resets on item removal, only on confirmClear(). */
    private var nextItemNumber = 1

    val isPriceValid: StateFlow<Boolean> = _priceInput.map { input ->
        input.toDoubleOrNull()?.let { it > 0.0 } == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val listResult: StateFlow<ShoppingListResult?> = combine(_items, _selectedProvince) { items, province ->
        if (items.isEmpty()) null
        else try {
            calculateShoppingList(items, province)
        } catch (e: Exception) {
            analytics.recordException(e)
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            settingsRepo.lastProvinceCode.collect { code ->
                _selectedProvince.value = Province.fromCode(code)
            }
        }
    }

    fun onPriceChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) _priceInput.value = filtered
    }

    fun addItem() {
        val price = _priceInput.value.toDoubleOrNull() ?: return
        if (price <= 0.0) return
        val item = ShoppingItem(price = price, displayName = "Article $nextItemNumber")
        nextItemNumber++
        _priceInput.value = ""   // clear first — prevents duplicate calls
        _items.value = _items.value + item
    }

    fun removeItem(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }

    // ── Save + feedback ───────────────────────────────────────────────────────
    private val _saveConfirmed = MutableStateFlow(false)
    val saveConfirmed: StateFlow<Boolean> = _saveConfirmed.asStateFlow()

    fun saveToHistory() {
        val result = listResult.value ?: return
        viewModelScope.launch {
            saveCalculation(result)
            analytics.log("shopping_list_created", "nb_articles" to _items.value.size)
            _saveConfirmed.value = true
            delay(1_500)
            resetDisplay()
        }
    }

    fun logShare() = analytics.log("shopping_list_shared", "nb_articles" to _items.value.size)

    fun resetDisplay() {
        _items.value      = emptyList()
        nextItemNumber    = 1
        _priceInput.value = ""
        _saveConfirmed.value = false
    }

    fun requestClear()  { _showClearDialog.value = true  }
    fun dismissClear()  { _showClearDialog.value = false }
    fun confirmClear()  {
        _items.value = emptyList()
        nextItemNumber = 1
        _showClearDialog.value = false
    }

    fun onProvinceChange(province: Province) {
        _selectedProvince.value = province
        viewModelScope.launch { settingsRepo.saveLastProvince(province.code) }
        analytics.log("province_selected", "province" to province.code)
    }
}
