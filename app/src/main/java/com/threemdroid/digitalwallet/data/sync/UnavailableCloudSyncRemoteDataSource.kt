package com.threemdroid.digitalwallet.data.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnavailableCloudSyncRemoteDataSource @Inject constructor() : CloudSyncRemoteDataSource {
    override suspend fun sync(batch: CloudSyncBatch): CloudSyncRemoteResult =
        CloudSyncRemoteResult.BackendUnavailable(
            message = "Cloud sync backend is not configured in this build."
        )
}
