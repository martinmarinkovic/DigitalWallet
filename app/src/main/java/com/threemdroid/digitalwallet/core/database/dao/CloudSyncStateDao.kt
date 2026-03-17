package com.threemdroid.digitalwallet.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.threemdroid.digitalwallet.core.database.entity.CloudSyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudSyncStateDao {
    @Query("SELECT * FROM cloud_sync_state WHERE id = :stateId LIMIT 1")
    fun observeState(stateId: Int = CloudSyncStateEntity.SINGLETON_ID): Flow<CloudSyncStateEntity?>

    @Query("SELECT * FROM cloud_sync_state WHERE id = :stateId LIMIT 1")
    suspend fun getState(stateId: Int = CloudSyncStateEntity.SINGLETON_ID): CloudSyncStateEntity?

    @Upsert
    suspend fun upsertState(state: CloudSyncStateEntity)

    @Query("DELETE FROM cloud_sync_state")
    suspend fun deleteAllStates()
}
