package com.threemdroid.digitalwallet.core.di

import com.threemdroid.digitalwallet.data.sync.CloudSyncRemoteDataSource
import com.threemdroid.digitalwallet.data.sync.RoomSyncMutationRecorder
import com.threemdroid.digitalwallet.data.sync.SyncMutationRecorder
import com.threemdroid.digitalwallet.data.sync.UnavailableCloudSyncRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun bindSyncMutationRecorder(
        recorder: RoomSyncMutationRecorder
    ): SyncMutationRecorder

    @Binds
    @Singleton
    abstract fun bindCloudSyncRemoteDataSource(
        remoteDataSource: UnavailableCloudSyncRemoteDataSource
    ): CloudSyncRemoteDataSource
}
