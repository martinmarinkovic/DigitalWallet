package com.threemdroid.digitalwallet.data.settings

import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest : BaseRepositoryTest() {
    private val repository by lazy {
        OfflineFirstSettingsRepository(database.appSettingsDao())
    }

    @Test
    fun observeSettings_returnsDefaultsWhenNothingPersisted() = runBlocking {
        assertEquals(appSettings(), repository.observeSettings().first())
    }

    @Test
    fun updateSettingsAndGranularSetters_persistValues() = runBlocking {
        val customSettings = appSettings(
            themeMode = ThemeMode.DARK,
            autoBrightnessEnabled = false,
            reminderEnabled = false,
            reminderTiming = ReminderTiming.SEVEN_DAYS_BEFORE,
            cloudSyncEnabled = true
        )

        repository.updateSettings(customSettings)
        assertEquals(customSettings, repository.observeSettings().first())

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setAutoBrightnessEnabled(true)
        repository.setReminderEnabled(true)
        repository.setReminderTiming(ReminderTiming.ONE_DAY_BEFORE)
        repository.setCloudSyncEnabled(false)

        assertEquals(
            appSettings(
                themeMode = ThemeMode.LIGHT,
                autoBrightnessEnabled = true,
                reminderEnabled = true,
                reminderTiming = ReminderTiming.ONE_DAY_BEFORE,
                cloudSyncEnabled = false
            ),
            repository.observeSettings().first()
        )
    }

    @Test
    fun granularSetters_persistThemeBrightnessReminderAndSyncSelections() = runBlocking {
        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.observeSettings().first().themeMode)

        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.observeSettings().first().themeMode)

        repository.setThemeMode(ThemeMode.SYSTEM)
        repository.setAutoBrightnessEnabled(false)
        repository.setReminderEnabled(false)
        repository.setReminderTiming(ReminderTiming.SEVEN_DAYS_BEFORE)
        repository.setCloudSyncEnabled(true)

        val persisted = repository.observeSettings().first()
        assertEquals(ThemeMode.SYSTEM, persisted.themeMode)
        assertFalse(persisted.autoBrightnessEnabled)
        assertFalse(persisted.reminderEnabled)
        assertEquals(ReminderTiming.SEVEN_DAYS_BEFORE, persisted.reminderTiming)
        assertTrue(persisted.cloudSyncEnabled)
    }

    @Test
    fun observeSettings_fallsBackToDefaultsAfterPersistedSettingsAreCleared() = runBlocking {
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.observeSettings().first().themeMode)

        database.appSettingsDao().deleteAllSettings()

        val fallback = repository.observeSettings().first()
        assertEquals(ThemeMode.SYSTEM, fallback.themeMode)
        assertTrue(fallback.autoBrightnessEnabled)
        assertTrue(fallback.reminderEnabled)
        assertEquals(ReminderTiming.ON_DAY, fallback.reminderTiming)
        assertFalse(fallback.cloudSyncEnabled)
    }
}
