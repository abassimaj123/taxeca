package com.taxeca.calculator.di

import android.content.Context
import androidx.room.Room
import com.taxeca.calculator.data.local.HistoryDao
import com.taxeca.calculator.data.local.HistoryDatabase
import com.taxeca.calculator.data.local.MIGRATION_1_2
import com.taxeca.calculator.data.local.MIGRATION_2_3
import com.taxeca.calculator.data.local.MIGRATION_3_4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHistoryDatabase(
        @ApplicationContext context: Context
    ): HistoryDatabase = Room.databaseBuilder(
        context.applicationContext,
        HistoryDatabase::class.java,
        "taxeca_history.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    @Provides
    @Singleton
    fun provideHistoryDao(database: HistoryDatabase): HistoryDao = database.historyDao
}
