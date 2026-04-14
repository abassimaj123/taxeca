package com.taxeca.calculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.data.repository.HistoryRepository
import com.taxeca.calculator.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getHistory: GetHistoryUseCase,
    private val historyRepo: HistoryRepository
) : ViewModel() {

    val history: StateFlow<List<HistoryEntity>> = getHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteAll() {
        viewModelScope.launch {
            historyRepo.deleteAll()
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            historyRepo.deleteById(id)
        }
    }
}
