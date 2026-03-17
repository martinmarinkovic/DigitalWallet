package com.threemdroid.digitalwallet.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.threemdroid.digitalwallet.core.model.CloudSyncPhase
import java.time.Instant

@Entity(tableName = "cloud_sync_state")
data class CloudSyncStateEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val phase: CloudSyncPhase,
    @ColumnInfo(name = "last_sync_attempt_at")
    val lastSyncAttemptAt: Instant?,
    @ColumnInfo(name = "last_successful_sync_at")
    val lastSuccessfulSyncAt: Instant?,
    @ColumnInfo(name = "last_error_message")
    val lastErrorMessage: String?
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
