package com.taxeca.calculator.data.repository

import com.taxeca.calculator.data.local.HistoryDao
import com.taxeca.calculator.data.model.HistoryEntity
import com.taxeca.calculator.ui.ads.IAPManager
import com.taxeca.calculator.ui.analytics.AnalyticsManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao,
    private val analytics: AnalyticsManager,
    private val iapManager: IAPManager
) {
    companion object {
        const val FREE_HISTORY_LIMIT = 5
    }

    suspend fun save(entity: HistoryEntity) {
        if (!iapManager.isPremium.value) {
            while (dao.count() >= FREE_HISTORY_LIMIT) {
                dao.deleteOldest()
            }
        }
        dao.insert(entity)
        analytics.log("history_saved", "type" to entity.mode)
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
