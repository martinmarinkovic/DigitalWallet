package com.threemdroid.digitalwallet.data.transfer

import android.net.Uri
import com.threemdroid.digitalwallet.core.database.entity.CloudSyncStateEntity
import com.threemdroid.digitalwallet.core.database.entity.ExpirationReminderStateEntity
import com.threemdroid.digitalwallet.core.database.entity.SearchHistoryEntity
import com.threemdroid.digitalwallet.core.database.mapper.asEntity
import com.threemdroid.digitalwallet.core.model.CloudSyncPhase
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.category.FavoritesCategory
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import com.threemdroid.digitalwallet.data.sync.RoomSyncMutationRecorder
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstUserDataTransferRepositoryTest : BaseRepositoryTest() {
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-03-16T11:00:00Z"),
        ZoneOffset.UTC
    )
    private val documentStore = FakeUserDataDocumentStore()
    private val backupCodec = UserDataBackupJsonCodec()
    private val syncMutationRecorder by lazy {
        RoomSyncMutationRecorder(
            pendingSyncChangeDao = database.pendingSyncChangeDao(),
            clock = fixedClock
        )
    }
    private val categoryRepository by lazy {
        OfflineFirstCategoryRepository(
            database = database,
            categoryDao = database.categoryDao(),
            syncMutationRecorder = syncMutationRecorder
        )
    }
    private val repository by lazy {
        OfflineFirstUserDataTransferRepository(
            database = database,
            categoryDao = database.categoryDao(),
            cardDao = database.cardDao(),
            appSettingsDao = database.appSettingsDao(),
            searchHistoryDao = database.searchHistoryDao(),
            expirationReminderStateDao = database.expirationReminderStateDao(),
            pendingSyncChangeDao = database.pendingSyncChangeDao(),
            cloudSyncStateDao = database.cloudSyncStateDao(),
            categoryRepository = categoryRepository,
            syncMutationRecorder = syncMutationRecorder,
            documentStore = documentStore,
            backupJsonCodec = backupCodec,
            cardCsvExportFormatter = CardCsvExportFormatter()
        )
    }

    @Test
    fun backupTo_writesJsonSnapshotWithCategoriesCardsSettingsAndSearchHistory() = runBlocking {
        val library = category(
            id = "library",
            name = "Library",
            position = 0,
            isDefault = true
        )
        val transport = category(
            id = "transport",
            name = "Transport",
            position = 1
        )
        database.categoryDao().upsertCategories(listOf(library.asEntity(), transport.asEntity()))
        database.cardDao().upsertCard(
            card(id = "card-1", categoryId = library.id, position = 0).asEntity()
        )
        database.appSettingsDao().upsertSettings(
            appSettings(cloudSyncEnabled = true).asEntity()
        )
        database.searchHistoryDao().insertSearchQuery(
            SearchHistoryEntity(
                query = "library",
                createdAt = Instant.parse("2026-03-16T09:00:00Z")
            )
        )

        val result = repository.backupTo(BACKUP_URI)
        val snapshot = backupCodec.decode(documentStore.requireText(BACKUP_URI))

        assertEquals(2, result.categoryCount)
        assertEquals(1, result.cardCount)
        assertEquals(1, result.searchHistoryCount)
        assertEquals(listOf(library.id, transport.id), snapshot.categories.map { it.id })
        assertEquals(listOf("card-1"), snapshot.cards.map { it.id })
        assertTrue(snapshot.settings.cloudSyncEnabled)
        assertEquals(listOf("library"), snapshot.searchHistory.map { it.query })
    }

    @Test
    fun backupTo_writesExplicitFormatVersionSettingsAndOrderingFields() = runBlocking {
        val access = category(
            id = "access",
            name = "Access",
            position = 0,
            isDefault = true
        )
        val membership = category(
            id = "membership",
            name = "Membership",
            position = 1
        )
        database.categoryDao().upsertCategories(
            listOf(membership.asEntity(), access.asEntity())
        )
        database.cardDao().upsertCards(
            listOf(
                card(id = "late-card", categoryId = access.id, position = 1).asEntity(),
                card(id = "first-card", categoryId = access.id, position = 0).asEntity()
            )
        )
        database.appSettingsDao().upsertSettings(
            appSettings(
                themeMode = ThemeMode.DARK,
                autoBrightnessEnabled = false,
                reminderEnabled = false,
                reminderTiming = ReminderTiming.SEVEN_DAYS_BEFORE,
                cloudSyncEnabled = true
            ).asEntity()
        )

        repository.backupTo(BACKUP_URI)

        val root = JSONObject(documentStore.requireText(BACKUP_URI))
        assertEquals("digital_wallet_backup", root.getString("format"))
        assertEquals(1, root.getInt("schemaVersion"))

        val categories = root.getJSONArray("categories")
        assertEquals("access", categories.getJSONObject(0).getString("id"))
        assertEquals(0, categories.getJSONObject(0).getInt("position"))
        assertEquals("membership", categories.getJSONObject(1).getString("id"))

        val settings = root.getJSONObject("settings")
        assertEquals("DARK", settings.getString("themeMode"))
        assertFalse(settings.getBoolean("autoBrightnessEnabled"))
        assertFalse(settings.getBoolean("reminderEnabled"))
        assertEquals("SEVEN_DAYS_BEFORE", settings.getString("reminderTiming"))
        assertTrue(settings.getBoolean("cloudSyncEnabled"))

        val cards = root.getJSONArray("cards")
        assertEquals(0, cards.getJSONObject(0).getInt("position"))
        assertEquals(1, cards.getJSONObject(1).getInt("position"))
    }

    @Test
    fun prepareRestoreAndRestore_replaceLocalDataAndClearInternalStateSafely() = runBlocking {
        val oldCategory = category(id = "old-category", name = "Old", position = 0)
        val oldCard = card(id = "old-card", categoryId = oldCategory.id, position = 0)
        database.categoryDao().upsertCategory(oldCategory.asEntity())
        database.cardDao().upsertCard(oldCard.asEntity())
        database.appSettingsDao().upsertSettings(appSettings(cloudSyncEnabled = false).asEntity())
        database.searchHistoryDao().insertSearchQuery(
            SearchHistoryEntity(
                query = "old query",
                createdAt = Instant.parse("2026-03-16T08:00:00Z")
            )
        )
        database.expirationReminderStateDao().upsertState(
            ExpirationReminderStateEntity(
                cardId = oldCard.id,
                scheduleKey = "old",
                scheduledAt = Instant.parse("2026-03-16T08:30:00Z"),
                deliveredAt = null
            )
        )
        database.cloudSyncStateDao().upsertState(
            CloudSyncStateEntity(
                phase = CloudSyncPhase.FAILED,
                lastSyncAttemptAt = Instant.parse("2026-03-16T08:40:00Z"),
                lastSuccessfulSyncAt = null,
                lastErrorMessage = "Old sync state"
            )
        )

        val backupSnapshot = UserDataBackupSnapshot(
            exportedAt = Instant.parse("2026-03-16T10:30:00Z"),
            categories = listOf(
                category(
                    id = "default_membership",
                    name = "Membership",
                    position = 0,
                    isDefault = true,
                )
            ),
            cards = listOf(
                card(
                    id = "member-card",
                    categoryId = "default_membership",
                    position = 0,
                    expirationDate = LocalDate.parse("2026-12-31")
                )
            ),
            settings = appSettings(
                cloudSyncEnabled = true
            ),
            searchHistory = listOf(
                searchHistoryEntry(query = "membership")
            )
        )
        documentStore.putText(BACKUP_URI, backupCodec.encode(backupSnapshot))

        val preparedRestore = repository.prepareRestore(BACKUP_URI)
        val result = repository.restore(preparedRestore)

        assertEquals(1, preparedRestore.preview.categoryCount)
        assertEquals(1, preparedRestore.preview.cardCount)
        assertEquals(1, preparedRestore.preview.searchHistoryCount)
        assertEquals(1, result.categoryCount)
        assertEquals(1, result.cardCount)
        assertEquals(1, result.searchHistoryCount)
        val restoredCategoryIds = database.categoryDao().getCategories().map { it.id }
        assertFalse(restoredCategoryIds.contains("favorites"))
        assertTrue(restoredCategoryIds.contains("default_membership"))
        assertTrue(restoredCategoryIds.contains("default_other"))
        assertFalse(restoredCategoryIds.contains(oldCategory.id))
        assertEquals(
            listOf("member-card"),
            database.cardDao().getAllCards().map { it.id }
        )
        assertTrue(database.appSettingsDao().getSettings()?.cloudSyncEnabled == true)
        assertEquals(
            listOf("membership"),
            database.searchHistoryDao().getAllSearchHistory().map { it.query }
        )
        assertTrue(database.expirationReminderStateDao().getAllStates().isEmpty())
        assertEquals(null, database.cloudSyncStateDao().getState())

        val pendingChanges = database.pendingSyncChangeDao().getPendingChanges()
        assertTrue(pendingChanges.any { change -> change.entityId == oldCategory.id })
        assertTrue(pendingChanges.any { change -> change.entityId == oldCard.id })
        assertTrue(pendingChanges.any { change -> change.entityId == "default_membership" })
        assertTrue(pendingChanges.any { change -> change.entityId == "member-card" })
        assertTrue(categoryRepository.observeCategories().first().none { it.isFavorites })
        assertEquals(
            FavoritesCategory.id,
            categoryRepository.observeCategoriesWithCardCounts().first().first().category.id
        )
    }

    @Test
    fun restore_restoresRelevantSettingsAndCardOrderingWithoutBreakingRelationships() = runBlocking {
        val backupSnapshot = UserDataBackupSnapshot(
            exportedAt = Instant.parse("2026-03-16T10:30:00Z"),
            categories = listOf(
                category(
                    id = "default_shopping_loyalty",
                    name = "Shopping & Loyalty",
                    position = 0,
                    color = "#2563EB",
                    isDefault = true
                ),
                category(
                    id = "default_membership",
                    name = "Membership",
                    position = 1,
                    color = "#7C3AED",
                    isDefault = true
                ),
                category(
                    id = "default_transport",
                    name = "Transport",
                    position = 2,
                    color = "#0F766E",
                    isDefault = true
                ),
                category(
                    id = "default_tickets",
                    name = "Tickets",
                    position = 3,
                    color = "#DC2626",
                    isDefault = true
                ),
                category(
                    id = "default_vouchers",
                    name = "Vouchers",
                    position = 4,
                    color = "#EA580C",
                    isDefault = true
                ),
                category(
                    id = "default_access",
                    name = "Access",
                    position = 5,
                    color = "#4B5563",
                    isDefault = true
                ),
                category(
                    id = "default_library",
                    name = "Library",
                    position = 6,
                    color = "#1D4ED8",
                    isDefault = true
                ),
                category(
                    id = "default_other",
                    name = "Other",
                    position = 7,
                    color = "#475569",
                    isDefault = true
                ),
                category(
                    id = "custom_gym",
                    name = "Gym",
                    position = 8,
                    color = "#22C55E"
                )
            ),
            cards = listOf(
                card(
                    id = "gym-second",
                    categoryId = "custom_gym",
                    position = 1,
                    name = "Gym Card B"
                ),
                card(
                    id = "gym-first",
                    categoryId = "custom_gym",
                    position = 0,
                    name = "Gym Card A"
                ),
                card(
                    id = "ticket-card",
                    categoryId = "default_tickets",
                    position = 0,
                    name = "Ticket"
                )
            ),
            settings = appSettings(
                themeMode = ThemeMode.DARK,
                autoBrightnessEnabled = false,
                reminderEnabled = false,
                reminderTiming = ReminderTiming.THREE_DAYS_BEFORE,
                cloudSyncEnabled = true
            ),
            searchHistory = listOf(
                searchHistoryEntry(query = "gym"),
                searchHistoryEntry(query = "ticket")
            )
        )
        documentStore.putText(BACKUP_URI, backupCodec.encode(backupSnapshot))

        repository.restore(repository.prepareRestore(BACKUP_URI))

        val restoredCategories = database.categoryDao().getCategories()
        assertEquals("default_shopping_loyalty", restoredCategories.first().id)
        assertEquals("custom_gym", restoredCategories.last().id)
        assertEquals(
            listOf("gym-first", "gym-second"),
            database.cardDao().observeCardsByCategory("custom_gym").first().map { it.id }
        )
        assertEquals(
            listOf("custom_gym", "default_tickets"),
            database.cardDao().getAllCards().map { it.categoryId }.distinct().sorted()
        )

        val restoredSettings = database.appSettingsDao().getSettings()
        assertEquals(ThemeMode.DARK, restoredSettings?.themeMode)
        assertFalse(restoredSettings?.autoBrightnessEnabled ?: true)
        assertFalse(restoredSettings?.reminderEnabled ?: true)
        assertEquals(ReminderTiming.THREE_DAYS_BEFORE, restoredSettings?.reminderTiming)
        assertTrue(restoredSettings?.cloudSyncEnabled ?: false)
    }

    @Test
    fun restore_replacesExistingSearchHistoryWithBackupHistoryInPersistedOrder() = runBlocking {
        database.searchHistoryDao().insertSearchHistoryEntries(
            listOf(
                SearchHistoryEntity(
                    query = "old newest",
                    createdAt = Instant.parse("2026-03-16T09:30:00Z")
                ),
                SearchHistoryEntity(
                    query = "old oldest",
                    createdAt = Instant.parse("2026-03-16T09:00:00Z")
                )
            )
        )

        val backupSnapshot = UserDataBackupSnapshot(
            exportedAt = Instant.parse("2026-03-16T10:30:00Z"),
            categories = emptyList(),
            cards = emptyList(),
            settings = appSettings(),
            searchHistory = listOf(
                searchHistoryEntry(
                    query = "latest backup",
                    createdAt = Instant.parse("2026-03-16T10:20:00Z")
                ),
                searchHistoryEntry(
                    query = "middle backup",
                    createdAt = Instant.parse("2026-03-16T10:10:00Z")
                ),
                searchHistoryEntry(
                    query = "oldest backup",
                    createdAt = Instant.parse("2026-03-16T10:00:00Z")
                )
            )
        )
        documentStore.putText(BACKUP_URI, backupCodec.encode(backupSnapshot))

        val result = repository.restore(repository.prepareRestore(BACKUP_URI))

        assertEquals(3, result.searchHistoryCount)
        assertEquals(
            listOf("latest backup", "middle backup", "oldest backup"),
            database.searchHistoryDao().getAllSearchHistory().map { entry -> entry.query }
        )
        assertFalse(
            database.searchHistoryDao().getAllSearchHistory().any { entry ->
                entry.query.startsWith("old ")
            }
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun prepareRestore_rejectsBackupWithCardReferencingMissingCategory() {
        runBlocking {
            val invalidSnapshot = UserDataBackupSnapshot(
                exportedAt = Instant.parse("2026-03-16T10:30:00Z"),
                categories = emptyList(),
                cards = listOf(
                    card(id = "orphan", categoryId = "missing", position = 0)
                ),
                settings = appSettings(),
                searchHistory = emptyList()
            )
            documentStore.putText(BACKUP_URI, backupCodec.encode(invalidSnapshot))
            repository.prepareRestore(BACKUP_URI)
        }
    }

    @Test
    fun exportCardsTo_writesCsvWithMeaningfulColumns() = runBlocking {
        val category = category(id = "tickets", name = "Tickets", position = 0)
        val card = card(id = "concert", categoryId = category.id, position = 0, name = "Concert Pass")
        database.categoryDao().upsertCategory(category.asEntity())
        database.cardDao().upsertCard(card.asEntity())

        val result = repository.exportCardsTo(EXPORT_URI)
        val csv = documentStore.requireText(EXPORT_URI)

        assertEquals(1, result.cardCount)
        assertTrue(csv.contains("\"card_name\",\"category_name\",\"barcode_type\""))
        assertTrue(csv.contains("\"Concert Pass\",\"Tickets\""))
        assertTrue(csv.contains("\"QR_CODE\""))
    }

    @Test
    fun exportCardsTo_ordersRowsByCategoryAndCardPositionAndEscapesCells() = runBlocking {
        val access = category(id = "access", name = "Access", position = 0)
        val tickets = category(id = "tickets", name = "Tickets", position = 1)
        database.categoryDao().upsertCategories(listOf(tickets.asEntity(), access.asEntity()))
        database.cardDao().upsertCards(
            listOf(
                card(
                    id = "late-access",
                    categoryId = access.id,
                    position = 1,
                    name = "=Late Access",
                    notes = "He said \"hello\""
                ).asEntity(),
                card(
                    id = "first-access",
                    categoryId = access.id,
                    position = 0,
                    name = "Early Access"
                ).asEntity(),
                card(
                    id = "ticket-card",
                    categoryId = tickets.id,
                    position = 0,
                    name = "@Ticket"
                ).asEntity()
            )
        )

        repository.exportCardsTo(EXPORT_URI)

        val rows = documentStore.requireText(EXPORT_URI).lines()
        assertEquals("\"card_name\",\"category_name\",\"barcode_type\",\"barcode_or_qr_value\",\"card_number\",\"expiration_date\",\"notes\",\"favorite\",\"category_position\",\"card_position\",\"created_at\",\"updated_at\"", rows.first())
        assertTrue(rows[1].contains("\"Early Access\",\"Access\""))
        assertTrue(rows[2].contains("\"'=Late Access\",\"Access\""))
        assertTrue(rows[2].contains("\"He said \"\"hello\"\"\""))
        assertTrue(rows[3].contains("\"'@Ticket\",\"Tickets\""))
    }

    private class FakeUserDataDocumentStore : UserDataDocumentStore {
        private val contents = linkedMapOf<String, String>()

        override suspend fun readText(uri: Uri): String =
            contents[uri.toString()] ?: throw IllegalStateException("Missing file content")

        override suspend fun writeText(uri: Uri, text: String) {
            contents[uri.toString()] = text
        }

        fun putText(uri: Uri, text: String) {
            contents[uri.toString()] = text
        }

        fun requireText(uri: Uri): String =
            contents[uri.toString()] ?: throw IllegalStateException("Missing file content")
    }

    private companion object {
        val BACKUP_URI: Uri = Uri.parse("content://backup.json")
        val EXPORT_URI: Uri = Uri.parse("content://cards.csv")
    }
}
