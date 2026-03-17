package com.threemdroid.digitalwallet.data.transfer

import android.net.Uri
import androidx.room.withTransaction
import com.threemdroid.digitalwallet.core.database.DigitalWalletDatabase
import com.threemdroid.digitalwallet.core.database.dao.AppSettingsDao
import com.threemdroid.digitalwallet.core.database.dao.CardDao
import com.threemdroid.digitalwallet.core.database.dao.CategoryDao
import com.threemdroid.digitalwallet.core.database.dao.CloudSyncStateDao
import com.threemdroid.digitalwallet.core.database.dao.ExpirationReminderStateDao
import com.threemdroid.digitalwallet.core.database.dao.PendingSyncChangeDao
import com.threemdroid.digitalwallet.core.database.dao.SearchHistoryDao
import com.threemdroid.digitalwallet.core.database.mapper.asEntity
import com.threemdroid.digitalwallet.core.database.mapper.asExternalModel
import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.data.sync.SyncMutationRecorder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class OfflineFirstUserDataTransferRepository @Inject constructor(
    private val database: DigitalWalletDatabase,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val appSettingsDao: AppSettingsDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val expirationReminderStateDao: ExpirationReminderStateDao,
    private val pendingSyncChangeDao: PendingSyncChangeDao,
    private val cloudSyncStateDao: CloudSyncStateDao,
    private val categoryRepository: CategoryRepository,
    private val syncMutationRecorder: SyncMutationRecorder,
    private val documentStore: UserDataDocumentStore,
    private val backupJsonCodec: UserDataBackupJsonCodec,
    private val cardCsvExportFormatter: CardCsvExportFormatter
) : UserDataTransferRepository {

    override suspend fun backupTo(uri: Uri): BackupResult {
        val snapshot = database.withTransaction {
            UserDataBackupSnapshot(
                exportedAt = Instant.now(),
                categories = categoryDao.getCategories().map { it.asExternalModel() },
                cards = cardDao.getAllCards().map { it.asExternalModel() },
                settings = appSettingsDao.getSettings()?.asExternalModel() ?: AppSettings(),
                searchHistory = searchHistoryDao.getAllSearchHistory().map { it.asExternalModel() }
            )
        }

        val encodedSnapshot = withContext(Dispatchers.Default) {
            backupJsonCodec.encode(snapshot)
        }

        documentStore.writeText(
            uri = uri,
            text = encodedSnapshot
        )

        return BackupResult(
            categoryCount = snapshot.categories.size,
            cardCount = snapshot.cards.size,
            searchHistoryCount = snapshot.searchHistory.size
        )
    }

    override suspend fun prepareRestore(uri: Uri): PreparedRestoreData {
        val snapshot = withContext(Dispatchers.Default) {
            backupJsonCodec.decode(documentStore.readText(uri))
        }
        return PreparedRestoreData(
            preview = RestorePreview(
                exportedAt = snapshot.exportedAt,
                categoryCount = snapshot.categories.size,
                cardCount = snapshot.cards.size,
                searchHistoryCount = snapshot.searchHistory.size,
                includesSettings = true
            ),
            snapshot = snapshot
        )
    }

    override suspend fun restore(preparedRestoreData: PreparedRestoreData): RestoreResult {
        val snapshot = preparedRestoreData.snapshot

        database.withTransaction {
            val existingCategoryIds = categoryDao.getAllIds()
            val existingCardIds = cardDao.getAllIds()
            val restoredCategoryIds = snapshot.categories.map { category -> category.id }
            val restoredCardIds = snapshot.cards.map { card -> card.id }

            cardDao.deleteAllCards()
            categoryDao.deleteAllCategories()
            appSettingsDao.deleteAllSettings()
            searchHistoryDao.clearSearchHistory()
            expirationReminderStateDao.deleteAllStates()
            pendingSyncChangeDao.deleteAllChanges()
            cloudSyncStateDao.deleteAllStates()

            if (snapshot.categories.isNotEmpty()) {
                categoryDao.upsertCategories(snapshot.categories.map { category -> category.asEntity() })
            }
            if (snapshot.cards.isNotEmpty()) {
                cardDao.upsertCards(snapshot.cards.map { card -> card.asEntity() })
            }
            appSettingsDao.upsertSettings(snapshot.settings.asEntity())
            if (snapshot.searchHistory.isNotEmpty()) {
                searchHistoryDao.insertSearchHistoryEntries(
                    snapshot.searchHistory.map { entry -> entry.asEntity() }
                )
            }

            val deletedCategoryIds = existingCategoryIds - restoredCategoryIds.toSet()
            val deletedCardIds = existingCardIds - restoredCardIds.toSet()

            if (deletedCategoryIds.isNotEmpty()) {
                syncMutationRecorder.recordCategoryDeletes(deletedCategoryIds)
            }
            if (deletedCardIds.isNotEmpty()) {
                syncMutationRecorder.recordCardDeletes(deletedCardIds)
            }
            if (restoredCategoryIds.isNotEmpty()) {
                syncMutationRecorder.recordCategoryUpserts(restoredCategoryIds)
            }
            if (restoredCardIds.isNotEmpty()) {
                syncMutationRecorder.recordCardUpserts(restoredCardIds)
            }
            syncMutationRecorder.recordAppSettingsUpsert()
        }

        categoryRepository.ensureDefaultCategories()

        return RestoreResult(
            categoryCount = snapshot.categories.size,
            cardCount = snapshot.cards.size,
            searchHistoryCount = snapshot.searchHistory.size
        )
    }

    override suspend fun exportCardsTo(uri: Uri): ExportCardsResult {
        val (categories, cards) = database.withTransaction {
            categoryDao.getCategories().map { category -> category.asExternalModel() } to
                cardDao.getAllCards().map { card -> card.asExternalModel() }
        }
        val exportedCards = withContext(Dispatchers.Default) {
            cardCsvExportFormatter.format(
                categories = categories,
                cards = cards
            )
        }

        documentStore.writeText(
            uri = uri,
            text = exportedCards
        )

        return ExportCardsResult(cardCount = cards.size)
    }
}
