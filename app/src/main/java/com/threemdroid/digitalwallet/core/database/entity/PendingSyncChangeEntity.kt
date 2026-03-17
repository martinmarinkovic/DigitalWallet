package com.threemdroid.digitalwallet.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.threemdroid.digitalwallet.data.sync.SyncChangeType
import com.threemdroid.digitalwallet.data.sync.SyncEntityType
import java.time.Instant

@Entity(tableName = "pending_sync_changes")
data class PendingSyncChangeEntity(
    @PrimaryKey
    @ColumnInfo(name = "change_key")
    val changeKey: String,
    @ColumnInfo(name = "entity_type")
    val entityType: SyncEntityType,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "change_type")
    val changeType: SyncChangeType,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
) {
    companion object {
        fun create(
            entityType: SyncEntityType,
            entityId: String,
            changeType: SyncChangeType,
            updatedAt: Instant
        ): PendingSyncChangeEntity =
            PendingSyncChangeEntity(
                changeKey = "$entityType|$entityId",
                entityType = entityType,
                entityId = entityId,
                changeType = changeType,
                updatedAt = updatedAt
            )
    }
}
