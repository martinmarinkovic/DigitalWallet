package com.threemdroid.digitalwallet.data.sync

import com.threemdroid.digitalwallet.core.model.CloudSyncStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class PendingSyncFingerprint(
    val pendingChangeCount: Int,
    val lastUpdatedAt: Instant?
)

interface SyncRepository {
    fun observeSyncStatus(): Flow<CloudSyncStatus>

    fun observePendingSyncFingerprint(): Flow<PendingSyncFingerprint>

    suspend fun updateSyncEnabled(enabled: Boolean)

    suspend fun syncPendingChanges()
}
