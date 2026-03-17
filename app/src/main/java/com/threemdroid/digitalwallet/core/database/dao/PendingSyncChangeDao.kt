package com.threemdroid.digitalwallet.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.threemdroid.digitalwallet.core.database.entity.PendingSyncChangeEntity
import com.threemdroid.digitalwallet.core.database.model.PendingSyncSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncChangeDao {
    @Query("SELECT * FROM pending_sync_changes ORDER BY updated_at ASC, change_key ASC")
    suspend fun getPendingChanges(): List<PendingSyncChangeEntity>

    @Query(
        """
        SELECT COUNT(*) AS pending_count, MAX(updated_at) AS last_updated_at
        FROM pending_sync_changes
        """
    )
    fun observePendingSyncSummary(): Flow<PendingSyncSummaryRow>

    @Upsert
    suspend fun upsertChange(change: PendingSyncChangeEntity)

    @Upsert
    suspend fun upsertChanges(changes: List<PendingSyncChangeEntity>)

    @Query("DELETE FROM pending_sync_changes WHERE change_key IN (:changeKeys)")
    suspend fun deleteChanges(changeKeys: List<String>)

    @Query("DELETE FROM pending_sync_changes")
    suspend fun deleteAllChanges()
}
