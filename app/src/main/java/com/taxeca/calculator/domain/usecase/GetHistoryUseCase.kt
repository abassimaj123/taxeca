package com.taxeca.calculator.domain.usecase

import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.data.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repo: HistoryRepository
) {
    operator fun invoke(): Flow<List<HistoryEntity>> = repo.getAll()
}
