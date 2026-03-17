package com.threemdroid.digitalwallet.data.settings

import com.threemdroid.digitalwallet.core.database.dao.AppSettingsDao
import com.threemdroid.digitalwallet.core.database.mapper.asEntity
import com.threemdroid.digitalwallet.core.database.mapper.asExternalModel
import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.data.sync.SyncMutationRecorder
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstSettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao,
    private val syncMutationRecorder: SyncMutationRecorder = SyncMutationRecorder.NO_OP
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        appSettingsDao.observeSettings().map { settings ->
            settings?.asExternalModel() ?: AppSettings()
        }

    override suspend fun updateSettings(settings: AppSettings) {
        val currentSettings = appSettingsDao.getSettings()?.asExternalModel() ?: AppSettings()
        appSettingsDao.upsertSettings(settings.asEntity())
        recordSyncableSettingsChangeIfNeeded(
            before = currentSettings,
            after = settings
        )
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        updateCurrentSettings { copy(themeMode = themeMode) }
    }

    override suspend fun setAutoBrightnessEnabled(enabled: Boolean) {
        updateCurrentSettings { copy(autoBrightnessEnabled = enabled) }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        updateCurrentSettings { copy(reminderEnabled = enabled) }
    }

    override suspend fun setReminderTiming(reminderTiming: ReminderTiming) {
        updateCurrentSettings { copy(reminderTiming = reminderTiming) }
    }

    override suspend fun setCloudSyncEnabled(enabled: Boolean) {
        updateCurrentSettings { copy(cloudSyncEnabled = enabled) }
    }

    private suspend fun updateCurrentSettings(transform: AppSettings.() -> AppSettings) {
        val currentSettings = appSettingsDao.getSettings()?.asExternalModel() ?: AppSettings()
        val updatedSettings = currentSettings.transform()
        appSettingsDao.upsertSettings(updatedSettings.asEntity())
        recordSyncableSettingsChangeIfNeeded(
            before = currentSettings,
            after = updatedSettings
        )
    }

    private suspend fun recordSyncableSettingsChangeIfNeeded(
        before: AppSettings,
        after: AppSettings
    ) {
        if (before.toSyncableSnapshot() != after.toSyncableSnapshot()) {
            syncMutationRecorder.recordAppSettingsUpsert()
        }
    }
}

private data class SyncableSettingsSnapshot(
    val themeMode: ThemeMode,
    val autoBrightnessEnabled: Boolean,
    val reminderEnabled: Boolean,
    val reminderTiming: ReminderTiming
)

private fun AppSettings.toSyncableSnapshot(): SyncableSettingsSnapshot =
    SyncableSettingsSnapshot(
        themeMode = themeMode,
        autoBrightnessEnabled = autoBrightnessEnabled,
        reminderEnabled = reminderEnabled,
        reminderTiming = reminderTiming
    )
