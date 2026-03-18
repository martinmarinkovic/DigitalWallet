package com.threemdroid.digitalwallet.data.sync

import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.card.OfflineFirstCardRepository
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import com.threemdroid.digitalwallet.data.settings.OfflineFirstSettingsRepository
import com.threemdroid.digitalwallet.core.model.CloudSyncPhase
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncRepositoryTest : BaseRepositoryTest() {
    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-16T10:00:00Z"),
        ZoneOffset.UTC
    )
    private val syncMutationRecorder by lazy {
        RoomSyncMutationRecorder(
            pendingSyncChangeDao = database.pendingSyncChangeDao(),
            clock = clock
        )
    }
    private val categoryRepository by lazy {
        OfflineFirstCategoryRepository(
            database = database,
            categoryDao = database.categoryDao(),
            syncMutationRecorder = syncMutationRecorder
        )
    }
    private val cardRepository by lazy {
        OfflineFirstCardRepository(
            database = database,
            cardDao = database.cardDao(),
            categoryDao = database.categoryDao(),
            syncMutationRecorder = syncMutationRecorder
        )
    }
    private val settingsRepository by lazy {
        OfflineFirstSettingsRepository(
            appSettingsDao = database.appSettingsDao(),
            syncMutationRecorder = syncMutationRecorder
        )
    }

    @Test
    fun syncPendingChanges_success_pushesLatestLocalStateAndClearsQueue() = runBlocking {
        val remoteDataSource = CapturingRemoteDataSource(
            result = CloudSyncRemoteResult.Success
        )
        val syncRepository = syncRepository(remoteDataSource)

        val firstCategory = category(id = "cat-a", name = "A", position = 0)
        val secondCategory = category(id = "cat-b", name = "B", position = 1)
        categoryRepository.upsertCategories(listOf(firstCategory, secondCategory))
        categoryRepository.updateCategoryOrder(listOf(secondCategory.id, firstCategory.id))

        val firstCard = card(id = "card-a", categoryId = secondCategory.id, position = 0)
        val secondCard = card(id = "card-b", categoryId = secondCategory.id, position = 1)
        cardRepository.upsertCards(listOf(firstCard, secondCard))
        cardRepository.updateCardOrder(secondCategory.id, listOf(secondCard.id, firstCard.id))

        settingsRepository.setThemeMode(ThemeMode.DARK)
        settingsRepository.setReminderTiming(ReminderTiming.THREE_DAYS_BEFORE)
        settingsRepository.setCloudSyncEnabled(true)

        assertEquals(5, syncRepository.observeSyncStatus().first().pendingChangeCount)

        syncRepository.updateSyncEnabled(enabled = true)
        syncRepository.syncPendingChanges()

        val batch = remoteDataSource.batches.single()
        assertEquals(2, batch.categoriesToUpsert.size)
        assertEquals(0, batch.categoriesToUpsert.first { it.id == secondCategory.id }.position)
        assertEquals(1, batch.categoriesToUpsert.first { it.id == firstCategory.id }.position)
        assertTrue(batch.categoryIdsToDelete.isEmpty())

        assertEquals(2, batch.cardsToUpsert.size)
        assertEquals(0, batch.cardsToUpsert.first { it.id == secondCard.id }.position)
        assertEquals(1, batch.cardsToUpsert.first { it.id == firstCard.id }.position)
        assertTrue(batch.cardIdsToDelete.isEmpty())

        assertEquals(ThemeMode.DARK, batch.appSettingsToUpsert?.themeMode)
        assertEquals(
            ReminderTiming.THREE_DAYS_BEFORE,
            batch.appSettingsToUpsert?.reminderTiming
        )

        assertEquals(
            listOf(secondCategory.id, firstCategory.id),
            categoryRepository.observeCategories().first().map { category -> category.id }
        )
        assertEquals(
            listOf(secondCard.id, firstCard.id),
            cardRepository.observeCards(secondCategory.id).first().map { card -> card.id }
        )

        val status = syncRepository.observeSyncStatus().first()
        assertEquals(CloudSyncPhase.IDLE, status.phase)
        assertEquals(0, status.pendingChangeCount)
        assertNotNull(status.lastSuccessfulSyncAt)
    }

    @Test
    fun syncPendingChanges_backendUnavailableKeepsPendingChangesAndStatus() = runBlocking {
        val remoteDataSource = CapturingRemoteDataSource(
            result = CloudSyncRemoteResult.BackendUnavailable(
                message = "No backend configured."
            )
        )
        val syncRepository = syncRepository(remoteDataSource)

        val category = category(id = "membership", name = "Membership", position = 0)
        categoryRepository.upsertCategory(category)
        cardRepository.upsertCard(
            card(id = "card-1", categoryId = category.id, position = 0)
        )

        syncRepository.updateSyncEnabled(enabled = true)
        syncRepository.syncPendingChanges()

        val status = syncRepository.observeSyncStatus().first()
        assertEquals(CloudSyncPhase.BACKEND_UNAVAILABLE, status.phase)
        assertEquals(2, status.pendingChangeCount)
        assertEquals("No backend configured.", status.lastErrorMessage)

        assertEquals(listOf(category.id), categoryRepository.observeCategories().first().map { it.id })
        assertEquals(listOf("card-1"), cardRepository.observeCards(category.id).first().map { it.id })
        assertEquals(1, remoteDataSource.batches.size)
    }

    @Test
    fun syncPendingChanges_failureKeepsPendingChangesLocalDataAndFailedStatus() = runBlocking {
        val remoteDataSource = CapturingRemoteDataSource(
            result = CloudSyncRemoteResult.Failure(
                message = "Temporary failure."
            )
        )
        val syncRepository = syncRepository(remoteDataSource)

        val category = category(id = "loyalty", name = "Loyalty", position = 0)
        categoryRepository.upsertCategory(category)
        cardRepository.upsertCard(
            card(id = "card-failure", categoryId = category.id, position = 0)
        )

        syncRepository.updateSyncEnabled(enabled = true)
        syncRepository.syncPendingChanges()

        val status = syncRepository.observeSyncStatus().first()
        assertEquals(CloudSyncPhase.FAILED, status.phase)
        assertEquals(2, status.pendingChangeCount)
        assertEquals("Temporary failure.", status.lastErrorMessage)
        assertEquals(
            listOf(category.id),
            categoryRepository.observeCategories().first().map { it.id }
        )
        assertEquals(
            listOf("card-failure"),
            cardRepository.observeCards(category.id).first().map { it.id }
        )

        syncRepository.updateSyncEnabled(enabled = false)

        val disabledStatus = syncRepository.observeSyncStatus().first()
        assertEquals(CloudSyncPhase.DISABLED, disabledStatus.phase)
        assertEquals(2, disabledStatus.pendingChangeCount)
        assertNull(disabledStatus.lastErrorMessage)
        assertEquals(1, remoteDataSource.batches.size)
    }

    @Test
    fun syncPendingChanges_afterFailure_retriesWithLatestLocalState() = runBlocking {
        val remoteDataSource = CapturingRemoteDataSource(
            result = CloudSyncRemoteResult.Failure(
                message = "Temporary failure."
            )
        )
        val syncRepository = syncRepository(remoteDataSource)

        val category = category(id = "membership", name = "Membership", position = 0)
        val initialCard = card(
            id = "member-card",
            categoryId = category.id,
            position = 0,
            name = "Membership Card",
            codeValue = "MEM-001"
        )
        categoryRepository.upsertCategory(category)
        cardRepository.upsertCard(initialCard)

        syncRepository.updateSyncEnabled(enabled = true)
        syncRepository.syncPendingChanges()

        assertEquals(CloudSyncPhase.FAILED, syncRepository.observeSyncStatus().first().phase)

        categoryRepository.upsertCategory(
            category.copy(
                name = "Membership Plus"
            )
        )
        cardRepository.upsertCard(
            initialCard.copy(
                name = "Membership Card Updated",
                codeValue = "MEM-999"
            )
        )
        settingsRepository.setReminderTiming(ReminderTiming.SEVEN_DAYS_BEFORE)

        remoteDataSource.result = CloudSyncRemoteResult.Success
        syncRepository.syncPendingChanges()

        val retryBatch = remoteDataSource.batches.last()
        assertEquals(2, remoteDataSource.batches.size)
        assertEquals(
            "Membership Plus",
            retryBatch.categoriesToUpsert.single { categoryPayload ->
                categoryPayload.id == category.id
            }.name
        )
        val retriedCard = retryBatch.cardsToUpsert.single { cardPayload ->
            cardPayload.id == initialCard.id
        }
        assertEquals("Membership Card Updated", retriedCard.name)
        assertEquals("MEM-999", retriedCard.codeValue)
        assertEquals(
            ReminderTiming.SEVEN_DAYS_BEFORE,
            retryBatch.appSettingsToUpsert?.reminderTiming
        )

        assertEquals(
            "Membership Plus",
            categoryRepository.observeCategories().first().single().name
        )
        val persistedCard = cardRepository.observeCards(category.id).first().single()
        assertEquals("Membership Card Updated", persistedCard.name)
        assertEquals("MEM-999", persistedCard.codeValue)

        val status = syncRepository.observeSyncStatus().first()
        assertEquals(CloudSyncPhase.IDLE, status.phase)
        assertEquals(0, status.pendingChangeCount)
        assertNotNull(status.lastSuccessfulSyncAt)
    }

    @Test
    fun syncPendingChanges_deleteMappingReplacesQueuedUpsertsAndSkipsCloudSyncToggle() = runBlocking {
        val remoteDataSource = CapturingRemoteDataSource(
            result = CloudSyncRemoteResult.Success
        )
        val syncRepository = syncRepository(remoteDataSource)

        val category = category(id = "transport", name = "Transport", position = 0)
        val card = card(id = "bus-pass", categoryId = category.id, position = 0)

        categoryRepository.upsertCategory(category)
        cardRepository.upsertCard(card)
        settingsRepository.setCloudSyncEnabled(true)
        cardRepository.deleteCard(card.id)
        categoryRepository.deleteCategory(category.id)

        assertEquals(2, syncRepository.observeSyncStatus().first().pendingChangeCount)

        syncRepository.updateSyncEnabled(enabled = true)
        syncRepository.syncPendingChanges()

        val batch = remoteDataSource.batches.single()
        assertTrue(batch.categoriesToUpsert.isEmpty())
        assertEquals(listOf(category.id), batch.categoryIdsToDelete)
        assertTrue(batch.cardsToUpsert.isEmpty())
        assertEquals(listOf(card.id), batch.cardIdsToDelete)
        assertNull(batch.appSettingsToUpsert)
        assertTrue(categoryRepository.observeCategories().first().isEmpty())
        assertTrue(cardRepository.observeCards(category.id).first().isEmpty())

        val status = syncRepository.observeSyncStatus().first()
        assertEquals(CloudSyncPhase.IDLE, status.phase)
        assertEquals(0, status.pendingChangeCount)
    }

    private fun syncRepository(
        remoteDataSource: CapturingRemoteDataSource
    ): OfflineFirstSyncRepository =
        OfflineFirstSyncRepository(
            pendingSyncChangeDao = database.pendingSyncChangeDao(),
            cloudSyncStateDao = database.cloudSyncStateDao(),
            categoryDao = database.categoryDao(),
            cardDao = database.cardDao(),
            appSettingsDao = database.appSettingsDao(),
            remoteDataSource = remoteDataSource,
            clock = clock
        )

    private class CapturingRemoteDataSource(
        var result: CloudSyncRemoteResult
    ) : CloudSyncRemoteDataSource {
        val batches = mutableListOf<CloudSyncBatch>()

        override suspend fun sync(batch: CloudSyncBatch): CloudSyncRemoteResult {
            batches += batch
            return result
        }
    }
}
