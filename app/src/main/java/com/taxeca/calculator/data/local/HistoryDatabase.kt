package com.taxeca.calculator.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.taxeca.calculator.data.model.HistoryEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE calculations ADD COLUMN splitCount INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE calculations ADD COLUMN itemsJson TEXT DEFAULT NULL")
    }
}

@Database(
    entities = [HistoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract val historyDao: HistoryDao
}
