package com.taxeca.calculator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: HistoryRepository
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])

    private val _entry = MutableStateFlow<HistoryEntity?>(null)
    val entry: StateFlow<HistoryEntity?> = _entry.asStateFlow()

    init {
        viewModelScope.launch {
            _entry.value = repo.getById(entryId)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.deleteById(entryId)
            onDeleted()
        }
    }
}
