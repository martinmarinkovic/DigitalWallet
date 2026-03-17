package com.threemdroid.digitalwallet.data.sync

import com.threemdroid.digitalwallet.core.database.dao.AppSettingsDao
import com.threemdroid.digitalwallet.core.database.dao.CardDao
import com.threemdroid.digitalwallet.core.database.dao.CategoryDao
import com.threemdroid.digitalwallet.core.database.dao.CloudSyncStateDao
import com.threemdroid.digitalwallet.core.database.dao.PendingSyncChangeDao
import com.threemdroid.digitalwallet.core.database.entity.CloudSyncStateEntity
import com.threemdroid.digitalwallet.core.database.mapper.asExternalModel
import com.threemdroid.digitalwallet.core.model.CloudSyncPhase
import com.threemdroid.digitalwallet.core.model.CloudSyncStatus
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineFirstSyncRepository @Inject constructor(
    private val pendingSyncChangeDao: PendingSyncChangeDao,
    private val cloudSyncStateDao: CloudSyncStateDao,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val appSettingsDao: AppSettingsDao,
    private val remoteDataSource: CloudSyncRemoteDataSource,
    private val clock: Clock
) : SyncRepository {
    private val syncMutex = Mutex()

    override fun observeSyncStatus(): Flow<CloudSyncStatus> =
        combine(
            cloudSyncStateDao.observeState(),
            pendingSyncChangeDao.observePendingSyncSummary()
        ) { state, summary ->
            (state?.asExternalModel() ?: CloudSyncStatus()).copy(
                pendingChangeCount = summary.pendingCount
            )
        }

    override fun observePendingSyncFingerprint(): Flow<PendingSyncFingerprint> =
        pendingSyncChangeDao.observePendingSyncSummary().map { summary ->
            PendingSyncFingerprint(
                pendingChangeCount = summary.pendingCount,
                lastUpdatedAt = summary.lastUpdatedAt
            )
        }

    override suspend fun updateSyncEnabled(enabled: Boolean) {
        val current = currentStatus()
        if (!enabled) {
            upsertStatus(
                current.copy(
                    phase = CloudSyncPhase.DISABLED,
                    lastErrorMessage = null
                )
            )
            return
        }

        if (current.phase == CloudSyncPhase.DISABLED) {
            upsertStatus(
                current.copy(
                    phase = CloudSyncPhase.IDLE,
                    lastErrorMessage = null
                )
            )
        }
    }

    override suspend fun syncPendingChanges() {
        syncMutex.withLock {
            val changes = pendingSyncChangeDao.getPendingChanges()
            val current = currentStatus()
            if (changes.isEmpty()) {
                upsertStatus(
                    current.copy(
                        phase = CloudSyncPhase.IDLE,
                        lastErrorMessage = null
                    )
                )
                return
            }

            val attemptTime = Instant.now(clock)
            upsertStatus(
                current.copy(
                    phase = CloudSyncPhase.SYNCING,
                    lastSyncAttemptAt = attemptTime,
                    lastErrorMessage = null
                )
            )

            when (val result = remoteDataSource.sync(buildBatch(changes))) {
                CloudSyncRemoteResult.Success -> {
                    pendingSyncChangeDao.deleteChanges(changes.map { it.changeKey })
                    upsertStatus(
                        currentStatus().copy(
                            phase = CloudSyncPhase.IDLE,
                            lastSyncAttemptAt = attemptTime,
                            lastSuccessfulSyncAt = attemptTime,
                            lastErrorMessage = null
                        )
                    )
                }

                is CloudSyncRemoteResult.BackendUnavailable -> {
                    upsertStatus(
                        currentStatus().copy(
                            phase = CloudSyncPhase.BACKEND_UNAVAILABLE,
                            lastSyncAttemptAt = attemptTime,
                            lastErrorMessage = result.message
                        )
                    )
                }

                is CloudSyncRemoteResult.Failure -> {
                    upsertStatus(
                        currentStatus().copy(
                            phase = CloudSyncPhase.FAILED,
                            lastSyncAttemptAt = attemptTime,
                            lastErrorMessage = result.message
                        )
                    )
                }
            }
        }
    }

    private suspend fun buildBatch(changes: List<com.threemdroid.digitalwallet.core.database.entity.PendingSyncChangeEntity>): CloudSyncBatch {
        val categoriesToUpsert = categoryDao.getCategoriesByIds(
            entityIdsFor(changes, SyncEntityType.CATEGORY, SyncChangeType.UPSERT)
        )
            .map { it.asExternalModel() }
            .sortedBy { category -> category.id }

        val cardsToUpsert = cardDao.getCardsByIds(
            entityIdsFor(changes, SyncEntityType.CARD, SyncChangeType.UPSERT)
        )
            .map { it.asExternalModel() }
            .sortedBy { card -> card.id }

        val settingsToUpsert =
            if (entityIdsFor(changes, SyncEntityType.APP_SETTINGS, SyncChangeType.UPSERT).isNotEmpty()) {
                appSettingsDao.getSettings()?.asExternalModel()?.let { settings ->
                    SyncableAppSettings(
                        themeMode = settings.themeMode,
                        autoBrightnessEnabled = settings.autoBrightnessEnabled,
                        reminderEnabled = settings.reminderEnabled,
                        reminderTiming = settings.reminderTiming
                    )
                }
            } else {
                null
            }

        return CloudSyncBatch(
            categoriesToUpsert = categoriesToUpsert,
            categoryIdsToDelete = entityIdsFor(changes, SyncEntityType.CATEGORY, SyncChangeType.DELETE),
            cardsToUpsert = cardsToUpsert,
            cardIdsToDelete = entityIdsFor(changes, SyncEntityType.CARD, SyncChangeType.DELETE),
            appSettingsToUpsert = settingsToUpsert
        )
    }

    private fun entityIdsFor(
        changes: List<com.threemdroid.digitalwallet.core.database.entity.PendingSyncChangeEntity>,
        entityType: SyncEntityType,
        changeType: SyncChangeType
    ): List<String> =
        changes
            .filter { change ->
                change.entityType == entityType && change.changeType == changeType
            }
            .map { change -> change.entityId }
            .distinct()
            .sorted()

    private suspend fun currentStatus(): CloudSyncStatus =
        cloudSyncStateDao.getState()?.asExternalModel() ?: CloudSyncStatus()

    private suspend fun upsertStatus(status: CloudSyncStatus) {
        cloudSyncStateDao.upsertState(
            CloudSyncStateEntity(
                phase = status.phase,
                lastSyncAttemptAt = status.lastSyncAttemptAt,
                lastSuccessfulSyncAt = status.lastSuccessfulSyncAt,
                lastErrorMessage = status.lastErrorMessage
            )
        )
    }
}
