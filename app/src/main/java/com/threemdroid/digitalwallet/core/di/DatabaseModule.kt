package com.threemdroid.digitalwallet.core.di

import android.content.Context
import androidx.room.Room
import com.threemdroid.digitalwallet.core.database.DigitalWalletDatabase
import com.threemdroid.digitalwallet.core.database.dao.AppSettingsDao
import com.threemdroid.digitalwallet.core.database.dao.CardDao
import com.threemdroid.digitalwallet.core.database.dao.CategoryDao
import com.threemdroid.digitalwallet.core.database.dao.CloudSyncStateDao
import com.threemdroid.digitalwallet.core.database.dao.ExpirationReminderStateDao
import com.threemdroid.digitalwallet.core.database.dao.PendingSyncChangeDao
import com.threemdroid.digitalwallet.core.database.dao.SearchHistoryDao
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
    fun provideDigitalWalletDatabase(
        @ApplicationContext context: Context
    ): DigitalWalletDatabase =
        Room.databaseBuilder(
            context,
            DigitalWalletDatabase::class.java,
            DigitalWalletDatabase.DATABASE_NAME
        )
            .addMigrations(DigitalWalletDatabase.MIGRATION_1_2)
            .addMigrations(DigitalWalletDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideCategoryDao(database: DigitalWalletDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideCardDao(database: DigitalWalletDatabase): CardDao = database.cardDao()

    @Provides
    fun provideSearchHistoryDao(database: DigitalWalletDatabase): SearchHistoryDao =
        database.searchHistoryDao()

    @Provides
    fun provideAppSettingsDao(database: DigitalWalletDatabase): AppSettingsDao =
        database.appSettingsDao()

    @Provides
    fun provideExpirationReminderStateDao(
        database: DigitalWalletDatabase
    ): ExpirationReminderStateDao = database.expirationReminderStateDao()

    @Provides
    fun providePendingSyncChangeDao(
        database: DigitalWalletDatabase
    ): PendingSyncChangeDao = database.pendingSyncChangeDao()

    @Provides
    fun provideCloudSyncStateDao(
        database: DigitalWalletDatabase
    ): CloudSyncStateDao = database.cloudSyncStateDao()
}
