package com.taxeca.calculator.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.data.repository.HistoryRepository
import com.taxeca.calculator.ui.ads.PdfExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: HistoryRepository,
    private val pdfExportService: PdfExportService
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

    fun exportPdf(context: Context, isFrench: Boolean) {
        val entity = _entry.value ?: return
        pdfExportService.exportAndShare(entity, isFrench)
    }
}
