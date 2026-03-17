package com.threemdroid.digitalwallet.data.transfer

import android.net.Uri
import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import com.threemdroid.digitalwallet.core.model.WalletCard
import java.time.Instant

interface UserDataTransferRepository {
    suspend fun backupTo(uri: Uri): BackupResult

    suspend fun prepareRestore(uri: Uri): PreparedRestoreData

    suspend fun restore(preparedRestoreData: PreparedRestoreData): RestoreResult

    suspend fun exportCardsTo(uri: Uri): ExportCardsResult
}

data class BackupResult(
    val categoryCount: Int,
    val cardCount: Int,
    val searchHistoryCount: Int
)

data class ExportCardsResult(
    val cardCount: Int
)

data class RestoreResult(
    val categoryCount: Int,
    val cardCount: Int,
    val searchHistoryCount: Int
)

data class RestorePreview(
    val exportedAt: Instant,
    val categoryCount: Int,
    val cardCount: Int,
    val searchHistoryCount: Int,
    val includesSettings: Boolean
)

data class PreparedRestoreData(
    val preview: RestorePreview,
    val snapshot: UserDataBackupSnapshot
)

data class UserDataBackupSnapshot(
    val exportedAt: Instant,
    val categories: List<Category>,
    val cards: List<WalletCard>,
    val settings: AppSettings,
    val searchHistory: List<SearchHistoryEntry>
)
