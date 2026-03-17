package com.threemdroid.digitalwallet.app

import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppThemeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun observeSettings_updatesThemeModeState() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = AppThemeViewModel(settingsRepository)

        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

        settingsRepository.settingsFlow.value = settingsRepository.settingsFlow.value.copy(
            themeMode = ThemeMode.LIGHT
        )
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)

        settingsRepository.settingsFlow.value = settingsRepository.settingsFlow.value.copy(
            themeMode = ThemeMode.DARK
        )
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        settingsRepository.settingsFlow.value = settingsRepository.settingsFlow.value.copy(
            themeMode = ThemeMode.SYSTEM
        )
        advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    @Test
    fun observeSettings_fallsBackToSystemWhenSettingsReset() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = AppThemeViewModel(settingsRepository)

        settingsRepository.settingsFlow.value = settingsRepository.settingsFlow.value.copy(
            themeMode = ThemeMode.DARK
        )
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        settingsRepository.settingsFlow.value = AppSettings()
        advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val settingsFlow = MutableStateFlow(AppSettings())

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
}
