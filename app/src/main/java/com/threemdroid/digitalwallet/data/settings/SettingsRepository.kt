package com.threemdroid.digitalwallet.data.settings

import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun updateSettings(settings: AppSettings)

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setAutoBrightnessEnabled(enabled: Boolean)

    suspend fun setReminderEnabled(enabled: Boolean)

    suspend fun setReminderTiming(reminderTiming: ReminderTiming)

    suspend fun setCloudSyncEnabled(enabled: Boolean)
}
