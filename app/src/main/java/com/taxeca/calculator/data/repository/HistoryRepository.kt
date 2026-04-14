package com.taxeca.calculator.data.repository

import com.taxeca.calculator.data.local.HistoryDao
import com.taxeca.calculator.data.model.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao
) {
    suspend fun save(entity: HistoryEntity) {
        dao.insert(entity)
    }

    fun getAll(): Flow<List<HistoryEntity>> = dao.getAll()

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun getById(id: Long): HistoryEntity? = dao.getById(id)
}
