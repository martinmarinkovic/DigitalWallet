package com.threemdroid.digitalwallet.data.sync

import com.threemdroid.digitalwallet.core.database.dao.PendingSyncChangeDao
import com.threemdroid.digitalwallet.core.database.entity.PendingSyncChangeEntity
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class RoomSyncMutationRecorder @Inject constructor(
    private val pendingSyncChangeDao: PendingSyncChangeDao,
    private val clock: Clock
) : SyncMutationRecorder {

    override suspend fun recordCategoryUpserts(categoryIds: Collection<String>) {
        recordChanges(
            entityType = SyncEntityType.CATEGORY,
            entityIds = categoryIds,
            changeType = SyncChangeType.UPSERT
        )
    }

    override suspend fun recordCategoryDeletes(categoryIds: Collection<String>) {
        recordChanges(
            entityType = SyncEntityType.CATEGORY,
            entityIds = categoryIds,
            changeType = SyncChangeType.DELETE
        )
    }

    override suspend fun recordCardUpserts(cardIds: Collection<String>) {
        recordChanges(
            entityType = SyncEntityType.CARD,
            entityIds = cardIds,
            changeType = SyncChangeType.UPSERT
        )
    }

    override suspend fun recordCardDeletes(cardIds: Collection<String>) {
        recordChanges(
            entityType = SyncEntityType.CARD,
            entityIds = cardIds,
            changeType = SyncChangeType.DELETE
        )
    }

    override suspend fun recordAppSettingsUpsert() {
        recordChanges(
            entityType = SyncEntityType.APP_SETTINGS,
            entityIds = listOf(APP_SETTINGS_ENTITY_ID),
            changeType = SyncChangeType.UPSERT
        )
    }

    private suspend fun recordChanges(
        entityType: SyncEntityType,
        entityIds: Collection<String>,
        changeType: SyncChangeType
    ) {
        if (entityIds.isEmpty()) {
            return
        }

        val timestamp = Instant.now(clock)
        pendingSyncChangeDao.upsertChanges(
            entityIds
                .distinct()
                .sorted()
                .map { entityId ->
                    PendingSyncChangeEntity.create(
                        entityType = entityType,
                        entityId = entityId,
                        changeType = changeType,
                        updatedAt = timestamp
                    )
                }
        )
    }

    companion object {
        private const val APP_SETTINGS_ENTITY_ID = "app_settings"
    }
}
