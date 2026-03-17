package com.threemdroid.digitalwallet.feature.settings

import android.net.Uri
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.data.searchhistory.SearchHistoryRepository
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import com.threemdroid.digitalwallet.data.transfer.BackupResult
import com.threemdroid.digitalwallet.data.transfer.ExportCardsResult
import com.threemdroid.digitalwallet.data.transfer.PreparedRestoreData
import com.threemdroid.digitalwallet.data.transfer.RestorePreview
import com.threemdroid.digitalwallet.data.transfer.RestoreResult
import com.threemdroid.digitalwallet.data.transfer.UserDataBackupSnapshot
import com.threemdroid.digitalwallet.data.transfer.UserDataTransferRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsPersistedSettingsAndSearchHistoryState() = runTest {
        val settingsRepository = FakeSettingsRepository(
            appSettings = AppSettings(
                themeMode = ThemeMode.DARK,
                autoBrightnessEnabled = false,
                reminderEnabled = false,
                reminderTiming = ReminderTiming.SEVEN_DAYS_BEFORE,
                cloudSyncEnabled = true
            )
        )
        val searchHistoryRepository = FakeSearchHistoryRepository(
            entries = listOf(
                SearchHistoryEntry(
                    id = 1L,
                    query = "membership",
                    createdAt = fixedTimestamp
                )
            )
        )

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            searchHistoryRepository = searchHistoryRepository,
            userDataTransferRepository = FakeUserDataTransferRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertFalse(state.autoBrightnessEnabled)
        assertTrue(state.cloudSyncEnabled)
        assertFalse(state.reminderEnabled)
        assertEquals(ReminderTiming.SEVEN_DAYS_BEFORE, state.reminderTiming)
        assertTrue(state.hasSearchHistory)
    }

    @Test
    fun settingEvents_persistAndUpdateState() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = FakeUserDataTransferRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnThemeModeSelected(ThemeMode.LIGHT))
        viewModel.onEvent(SettingsEvent.OnAutoBrightnessChanged(false))
        viewModel.onEvent(SettingsEvent.OnCloudSyncChanged(true))
        viewModel.onEvent(SettingsEvent.OnReminderEnabledChanged(false))
        viewModel.onEvent(SettingsEvent.OnReminderTimingSelected(ReminderTiming.THREE_DAYS_BEFORE))
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, settingsRepository.settingsFlow.value.themeMode)
        assertFalse(settingsRepository.settingsFlow.value.autoBrightnessEnabled)
        assertTrue(settingsRepository.settingsFlow.value.cloudSyncEnabled)
        assertFalse(settingsRepository.settingsFlow.value.reminderEnabled)
        assertEquals(
            ReminderTiming.THREE_DAYS_BEFORE,
            settingsRepository.settingsFlow.value.reminderTiming
        )

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.LIGHT, state.themeMode)
        assertFalse(state.autoBrightnessEnabled)
        assertTrue(state.cloudSyncEnabled)
        assertFalse(state.reminderEnabled)
        assertEquals(ReminderTiming.THREE_DAYS_BEFORE, state.reminderTiming)
    }

    @Test
    fun reminderPermissionChanges_updateReminderDeliveryStatus() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = FakeUserDataTransferRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnReminderNotificationPermissionChanged(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasReminderNotificationPermission)
        assertEquals(
            ReminderDeliveryStatusUiModel.BLOCKED,
            viewModel.uiState.value.reminderDeliveryStatus
        )

        viewModel.onEvent(SettingsEvent.OnReminderNotificationPermissionChanged(true))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasReminderNotificationPermission)
        assertEquals(
            ReminderDeliveryStatusUiModel.ENABLED,
            viewModel.uiState.value.reminderDeliveryStatus
        )
    }

    @Test
    fun enablingRemindersWithoutPermission_requestsNotificationPermission() = runTest {
        val settingsRepository = FakeSettingsRepository(
            appSettings = AppSettings(
                reminderEnabled = false
            )
        )
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = FakeUserDataTransferRepository()
        )

        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.OnReminderNotificationPermissionChanged(false))
        advanceUntilIdle()

        val effect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnReminderEnabledChanged(true))
        advanceUntilIdle()

        assertTrue(settingsRepository.settingsFlow.value.reminderEnabled)
        assertEquals(SettingsEffect.RequestNotificationPermission, effect.await())
        assertEquals(
            ReminderDeliveryStatusUiModel.BLOCKED,
            viewModel.uiState.value.reminderDeliveryStatus
        )
    }

    @Test
    fun blockedReminders_canRetryNotificationPermissionRequest() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = FakeUserDataTransferRepository()
        )

        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.OnReminderNotificationPermissionChanged(false))
        advanceUntilIdle()

        val effect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnReminderNotificationPermissionRequestClicked)

        assertEquals(SettingsEffect.RequestNotificationPermission, effect.await())
    }

    @Test
    fun themeSelection_supportsLightDarkAndSystemModes() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = FakeUserDataTransferRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnThemeModeSelected(ThemeMode.LIGHT))
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, settingsRepository.settingsFlow.value.themeMode)
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)

        viewModel.onEvent(SettingsEvent.OnThemeModeSelected(ThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, settingsRepository.settingsFlow.value.themeMode)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.onEvent(SettingsEvent.OnThemeModeSelected(ThemeMode.SYSTEM))
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, settingsRepository.settingsFlow.value.themeMode)
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    @Test
    fun backupAndExportClicks_emitDocumentLaunchEffects() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = FakeUserDataTransferRepository()
        )

        advanceUntilIdle()

        val backupEffect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnBackupClicked)
        assertEquals(
            SettingsEffect.LaunchBackupDocument("digital-wallet-backup.json"),
            backupEffect.await()
        )

        val restoreEffect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnRestoreClicked)
        assertEquals(
            SettingsEffect.LaunchRestoreDocumentPicker,
            restoreEffect.await()
        )

        val exportEffect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnExportCardsClicked)
        assertEquals(
            SettingsEffect.LaunchExportCardsDocument("digital-wallet-cards.csv"),
            exportEffect.await()
        )
    }

    @Test
    fun backupDestinationSelected_createsBackupAndShowsSuccessMessage() = runTest {
        val transferRepository = FakeUserDataTransferRepository()
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = transferRepository
        )

        advanceUntilIdle()

        val effect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnBackupDestinationSelected(TEST_URI))
        advanceUntilIdle()

        assertEquals(listOf(TEST_URI), transferRepository.backupUris)
        assertEquals(
            SettingsEffect.ShowMessage(R.string.settings_backup_success_message),
            effect.await()
        )
        assertFalse(viewModel.uiState.value.isDataTransferInProgress)
    }

    @Test
    fun restoreSourceSelected_loadsPreviewAndConfirmRestoresBackup() = runTest {
        val transferRepository = FakeUserDataTransferRepository()
        val preparedRestoreData = PreparedRestoreData(
            preview = RestorePreview(
                exportedAt = fixedTimestamp,
                categoryCount = 3,
                cardCount = 7,
                searchHistoryCount = 2,
                includesSettings = true
            ),
            snapshot = UserDataBackupSnapshot(
                exportedAt = fixedTimestamp,
                categories = emptyList(),
                cards = emptyList(),
                settings = AppSettings(),
                searchHistory = emptyList()
            )
        )
        transferRepository.preparedRestoreData = preparedRestoreData
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = transferRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnRestoreSourceSelected(TEST_URI))
        advanceUntilIdle()

        val preview = viewModel.uiState.value.pendingRestorePreview
        assertEquals(3, preview?.categoryCount)
        assertEquals(7, preview?.cardCount)
        assertEquals(2, preview?.searchHistoryCount)

        val effect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnRestoreConfirmed)
        advanceUntilIdle()

        assertEquals(listOf(TEST_URI), transferRepository.prepareRestoreUris)
        assertEquals(listOf(preparedRestoreData), transferRepository.restoreRequests)
        assertEquals(
            SettingsEffect.ShowMessage(R.string.settings_restore_success_message),
            effect.await()
        )
        assertEquals(null, viewModel.uiState.value.pendingRestorePreview)
        assertFalse(viewModel.uiState.value.isDataTransferInProgress)
    }

    @Test
    fun restoreDismissed_clearsPreviewAndDoesNotRunRestore() = runTest {
        val transferRepository = FakeUserDataTransferRepository()
        val preparedRestoreData = PreparedRestoreData(
            preview = RestorePreview(
                exportedAt = fixedTimestamp,
                categoryCount = 2,
                cardCount = 4,
                searchHistoryCount = 1,
                includesSettings = true
            ),
            snapshot = UserDataBackupSnapshot(
                exportedAt = fixedTimestamp,
                categories = emptyList(),
                cards = emptyList(),
                settings = AppSettings(),
                searchHistory = emptyList()
            )
        )
        transferRepository.preparedRestoreData = preparedRestoreData
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = transferRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnRestoreSourceSelected(TEST_URI))
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.pendingRestorePreview?.categoryCount)
        assertTrue(transferRepository.restoreRequests.isEmpty())

        viewModel.onEvent(SettingsEvent.OnRestoreDismissed)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.pendingRestorePreview)
        assertTrue(transferRepository.restoreRequests.isEmpty())
    }

    @Test
    fun exportCardsDestinationSelected_exportsCardsAndShowsSuccessMessage() = runTest {
        val transferRepository = FakeUserDataTransferRepository()
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository(),
            userDataTransferRepository = transferRepository
        )

        advanceUntilIdle()

        val effect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnExportCardsDestinationSelected(TEST_URI))
        advanceUntilIdle()

        assertEquals(listOf(TEST_URI), transferRepository.exportUris)
        assertEquals(
            SettingsEffect.ShowMessage(R.string.settings_export_success_message),
            effect.await()
        )
    }

    @Test
    fun clearSearchHistory_clearsRepositoryAndEmitsMessage() = runTest {
        val searchHistoryRepository = FakeSearchHistoryRepository(
            entries = listOf(
                SearchHistoryEntry(
                    id = 1L,
                    query = "membership",
                    createdAt = fixedTimestamp
                )
            )
        )
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            searchHistoryRepository = searchHistoryRepository,
            userDataTransferRepository = FakeUserDataTransferRepository()
        )

        advanceUntilIdle()

        val deferredEffect = async { viewModel.effects.first() }
        viewModel.onEvent(SettingsEvent.OnClearSearchHistoryClicked)
        advanceUntilIdle()

        assertTrue(searchHistoryRepository.entriesFlow.value.isEmpty())
        assertEquals(
            SettingsEffect.ShowMessage(R.string.settings_search_history_cleared_message),
            deferredEffect.await()
        )
        assertFalse(viewModel.uiState.value.hasSearchHistory)
    }

    private class FakeSettingsRepository(
        appSettings: AppSettings = AppSettings()
    ) : SettingsRepository {
        val settingsFlow = MutableStateFlow(appSettings)

        override fun observeSettings(): Flow<AppSettings> = settingsFlow

        override suspend fun updateSettings(settings: AppSettings) {
            settingsFlow.value = settings
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            settingsFlow.value = settingsFlow.value.copy(themeMode = themeMode)
        }

        override suspend fun setAutoBrightnessEnabled(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(autoBrightnessEnabled = enabled)
        }

        override suspend fun setReminderEnabled(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(reminderEnabled = enabled)
        }

        override suspend fun setReminderTiming(reminderTiming: ReminderTiming) {
            settingsFlow.value = settingsFlow.value.copy(reminderTiming = reminderTiming)
        }

        override suspend fun setCloudSyncEnabled(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(cloudSyncEnabled = enabled)
        }
    }

    private class FakeSearchHistoryRepository(
        entries: List<SearchHistoryEntry> = emptyList()
    ) : SearchHistoryRepository {
        val entriesFlow = MutableStateFlow(entries)

        override fun observeSearchHistory(limit: Int): Flow<List<SearchHistoryEntry>> = entriesFlow

        override suspend fun saveQuery(query: String) = Unit

        override suspend fun clearSearchHistory() {
            entriesFlow.value = emptyList()
        }
    }

    private class FakeUserDataTransferRepository : UserDataTransferRepository {
        val backupUris = mutableListOf<Uri>()
        val prepareRestoreUris = mutableListOf<Uri>()
        val restoreRequests = mutableListOf<PreparedRestoreData>()
        val exportUris = mutableListOf<Uri>()
        var preparedRestoreData: PreparedRestoreData = PreparedRestoreData(
            preview = RestorePreview(
                exportedAt = fixedTimestamp,
                categoryCount = 1,
                cardCount = 1,
                searchHistoryCount = 0,
                includesSettings = true
            ),
            snapshot = UserDataBackupSnapshot(
                exportedAt = fixedTimestamp,
                categories = emptyList(),
                cards = emptyList(),
                settings = AppSettings(),
                searchHistory = emptyList()
            )
        )

        override suspend fun backupTo(uri: Uri): BackupResult {
            backupUris += uri
            return BackupResult(categoryCount = 0, cardCount = 0, searchHistoryCount = 0)
        }

        override suspend fun prepareRestore(uri: Uri): PreparedRestoreData {
            prepareRestoreUris += uri
            return preparedRestoreData
        }

        override suspend fun restore(preparedRestoreData: PreparedRestoreData): RestoreResult {
            restoreRequests += preparedRestoreData
            return RestoreResult(categoryCount = 0, cardCount = 0, searchHistoryCount = 0)
        }

        override suspend fun exportCardsTo(uri: Uri): ExportCardsResult {
            exportUris += uri
            return ExportCardsResult(cardCount = 0)
        }
    }

    private companion object {
        val fixedTimestamp: Instant = Instant.parse("2026-03-16T10:00:00Z")
        val TEST_URI: Uri = Uri.parse("content://settings/test")
    }
}
