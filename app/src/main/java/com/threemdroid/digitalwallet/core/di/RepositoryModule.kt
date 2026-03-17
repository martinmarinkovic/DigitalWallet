package com.threemdroid.digitalwallet.core.di

import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.card.OfflineFirstCardRepository
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import com.threemdroid.digitalwallet.data.searchhistory.OfflineFirstSearchHistoryRepository
import com.threemdroid.digitalwallet.data.searchhistory.SearchHistoryRepository
import com.threemdroid.digitalwallet.data.settings.OfflineFirstSettingsRepository
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import com.threemdroid.digitalwallet.data.sync.OfflineFirstSyncRepository
import com.threemdroid.digitalwallet.data.sync.SyncRepository
import com.threemdroid.digitalwallet.data.transfer.OfflineFirstUserDataTransferRepository
import com.threemdroid.digitalwallet.data.transfer.UserDataTransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        repository: OfflineFirstCategoryRepository
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(
        repository: OfflineFirstCardRepository
    ): CardRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(
        repository: OfflineFirstSearchHistoryRepository
    ): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        repository: OfflineFirstSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        repository: OfflineFirstSyncRepository
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindUserDataTransferRepository(
        repository: OfflineFirstUserDataTransferRepository
    ): UserDataTransferRepository
}
