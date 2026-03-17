package com.threemdroid.digitalwallet.data.sync

sealed interface CloudSyncRemoteResult {
    data object Success : CloudSyncRemoteResult

    data class BackendUnavailable(
        val message: String
    ) : CloudSyncRemoteResult

    data class Failure(
        val message: String
    ) : CloudSyncRemoteResult
}

interface CloudSyncRemoteDataSource {
    suspend fun sync(batch: CloudSyncBatch): CloudSyncRemoteResult
}
