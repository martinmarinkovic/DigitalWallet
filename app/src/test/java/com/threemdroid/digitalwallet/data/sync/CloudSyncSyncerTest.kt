package com.threemdroid.digitalwallet.data.sync

import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSyncSyncerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun start_updatesEnabledStateAndRunsSyncOnlyWhenCloudSyncIsEnabled() = runTest {
        val settingsRepository = FakeSettingsRepository(
            initialSettings = AppSettings(cloudSyncEnabled = false)
        )
        val syncRepository = FakeSyncRepository()
        val syncer = CloudSyncSyncer(
            settingsRepository = settingsRepository,
            syncRepository = syncRepository
        )

        syncer.start()
        advanceUntilIdle()

        assertEquals(listOf(false), syncRepository.enabledCalls)
        assertEquals(0, syncRepository.syncCalls)

        settingsRepository.settingsFlow.value = settingsRepository.settingsFlow.value.copy(
            cloudSyncEnabled = true
        )
        advanceUntilIdle()

        assertEquals(listOf(false, true), syncRepository.enabledCalls)
        assertEquals(1, syncRepository.syncCalls)

        syncRepository.pendingFingerprintFlow.value = PendingSyncFingerprint(
            pendingChangeCount = 1,
            lastUpdatedAt = Instant.parse("2026-03-16T12:00:00Z")
        )
        advanceUntilIdle()

        assertEquals(listOf(false, true, true), syncRepository.enabledCalls)
        assertEquals(2, syncRepository.syncCalls)
    }

    @Test
    fun start_doesNotRunSyncWhenPendingChangesChangeWhileCloudSyncDisabled() = runTest {
        val settingsRepository = FakeSettingsRepository(
            initialSettings = AppSettings(cloudSyncEnabled = false)
        )
        val syncRepository = FakeSyncRepository()
        val syncer = CloudSyncSyncer(
            settingsRepository = settingsRepository,
            syncRepository = syncRepository
        )

        syncer.start()
        advanceUntilIdle()

        syncRepository.pendingFingerprintFlow.value = PendingSyncFingerprint(
            pendingChangeCount = 3,
            lastUpdatedAt = Instant.parse("2026-03-16T13:00:00Z")
        )
        advanceUntilIdle()

        assertEquals(listOf(false, false), syncRepository.enabledCalls)
        assertEquals(0, syncRepository.syncCalls)
    }

    private class FakeSettingsRepository(
        initialSettings: AppSettings
    ) : SettingsRepository {
        val settingsFlow = MutableStateFlow(initialSettings)

        override fun observeSettings(): Flow<AppSettings> = settingsFlow

        override suspend fun updateSettings(settings: AppSettings) {
            settingsFlow.value = settings
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit

        override suspend fun setAutoBrightnessEnabled(enabled: Boolean) = Unit

        override suspend fun setReminderEnabled(enabled: Boolean) = Unit

        override suspend fun setReminderTiming(reminderTiming: ReminderTiming) = Unit

        override suspend fun setCloudSyncEnabled(enabled: Boolean) = Unit
    }

    private class FakeSyncRepository : SyncRepository {
        val pendingFingerprintFlow = MutableStateFlow(
            PendingSyncFingerprint(
                pendingChangeCount = 0,
                lastUpdatedAt = null
            )
        )
        val enabledCalls = mutableListOf<Boolean>()
        var syncCalls = 0

        override fun observeSyncStatus(): Flow<com.threemdroid.digitalwallet.core.model.CloudSyncStatus> =
            MutableStateFlow(com.threemdroid.digitalwallet.core.model.CloudSyncStatus())

        override fun observePendingSyncFingerprint(): Flow<PendingSyncFingerprint> =
            pendingFingerprintFlow

        override suspend fun updateSyncEnabled(enabled: Boolean) {
            enabledCalls += enabled
        }

        override suspend fun syncPendingChanges() {
            syncCalls += 1
        }
    }
}
