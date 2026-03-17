package com.threemdroid.digitalwallet.core.di

import com.threemdroid.digitalwallet.data.transfer.ContentResolverUserDataDocumentStore
import com.threemdroid.digitalwallet.data.transfer.UserDataDocumentStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataTransferModule {
    @Binds
    @Singleton
    abstract fun bindUserDataDocumentStore(
        documentStore: ContentResolverUserDataDocumentStore
    ): UserDataDocumentStore
}
