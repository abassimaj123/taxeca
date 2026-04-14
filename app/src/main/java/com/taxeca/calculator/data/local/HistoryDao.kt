package com.taxeca.calculator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taxeca.calculator.data.model.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Query("SELECT * FROM calculations ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM calculations")
    suspend fun deleteAll()

    @Query("DELETE FROM calculations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM calculations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryEntity?
}
