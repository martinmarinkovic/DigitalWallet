package com.threemdroid.digitalwallet.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.data.searchhistory.SearchHistoryRepository
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import com.threemdroid.digitalwallet.data.transfer.PreparedRestoreData
import com.threemdroid.digitalwallet.data.transfer.UserDataTransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val userDataTransferRepository: UserDataTransferRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<SettingsEffect>()
    val effects = mutableEffects.asSharedFlow()

    private var pendingRestoreData: PreparedRestoreData? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.observeSettings(),
                searchHistoryRepository.observeSearchHistory(limit = 1)
            ) { settings, searchHistory ->
                settings to searchHistory
            }.collect { (settings, searchHistory) ->
                mutableUiState.update { current ->
                    current.copy(
                        isLoading = false,
                        themeMode = settings.themeMode,
                        autoBrightnessEnabled = settings.autoBrightnessEnabled,
                        cloudSyncEnabled = settings.cloudSyncEnabled,
                        reminderEnabled = settings.reminderEnabled,
                        reminderTiming = settings.reminderTiming,
                        hasSearchHistory = searchHistory.isNotEmpty()
                    )
                }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnThemeModeSelected -> {
                persistSetting {
                    settingsRepository.setThemeMode(event.themeMode)
                }
            }

            is SettingsEvent.OnAutoBrightnessChanged -> {
                persistSetting {
                    settingsRepository.setAutoBrightnessEnabled(event.enabled)
                }
            }

            is SettingsEvent.OnCloudSyncChanged -> {
                persistSetting {
                    settingsRepository.setCloudSyncEnabled(event.enabled)
                }
            }

            is SettingsEvent.OnReminderNotificationPermissionChanged -> {
                mutableUiState.update { current ->
                    current.copy(hasReminderNotificationPermission = event.granted)
                }
            }

            is SettingsEvent.OnReminderEnabledChanged -> {
                updateReminderEnabled(event.enabled)
            }

            SettingsEvent.OnReminderNotificationPermissionRequestClicked -> {
                requestReminderNotificationPermissionIfNeeded()
            }

            is SettingsEvent.OnReminderTimingSelected -> {
                persistSetting {
                    settingsRepository.setReminderTiming(event.reminderTiming)
                }
            }

            SettingsEvent.OnBackupClicked -> launchBackupDocument()

            is SettingsEvent.OnBackupDestinationSelected -> createBackup(event.uri)

            SettingsEvent.OnRestoreClicked -> launchRestoreDocumentPicker()

            is SettingsEvent.OnRestoreSourceSelected -> prepareRestore(event.uri)

            SettingsEvent.OnRestoreConfirmed -> restoreBackup()

            SettingsEvent.OnRestoreDismissed -> dismissRestorePreview()

            SettingsEvent.OnExportCardsClicked -> launchExportCardsDocument()

            is SettingsEvent.OnExportCardsDestinationSelected -> exportCards(event.uri)

            SettingsEvent.OnClearSearchHistoryClicked -> clearSearchHistory()

            SettingsEvent.OnPrivacyPolicyClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(SettingsEffect.OpenPrivacyPolicy)
                }
            }

            SettingsEvent.OnHelpAndFeedbackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(SettingsEffect.OpenHelpAndFeedback)
                }
            }

            SettingsEvent.OnTermsClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(SettingsEffect.OpenTerms)
                }
            }
        }
    }

    private fun clearSearchHistory() {
        if (!uiState.value.hasSearchHistory) {
            return
        }

        viewModelScope.launch {
            runCatching {
                searchHistoryRepository.clearSearchHistory()
            }.onSuccess {
                mutableEffects.emit(
                    SettingsEffect.ShowMessage(R.string.settings_search_history_cleared_message)
                )
            }.onFailure {
                mutableEffects.emit(
                    SettingsEffect.ShowMessage(R.string.settings_action_failed_message)
                )
            }
        }
    }

    private fun updateReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.setReminderEnabled(enabled)
            }.onSuccess {
                if (enabled) {
                    requestReminderNotificationPermissionIfNeeded()
                }
            }.onFailure {
                mutableEffects.emit(
                    SettingsEffect.ShowMessage(R.string.settings_action_failed_message)
                )
            }
        }
    }

    private fun requestReminderNotificationPermissionIfNeeded() {
        viewModelScope.launch {
            if (uiState.value.hasReminderNotificationPermission) {
                return@launch
            }
            mutableEffects.emit(SettingsEffect.RequestNotificationPermission)
        }
    }

    private fun persistSetting(update: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching {
                update()
            }.onFailure {
                mutableEffects.emit(
                    SettingsEffect.ShowMessage(R.string.settings_action_failed_message)
                )
            }
        }
    }

    private fun launchBackupDocument() {
        viewModelScope.launch {
            mutableEffects.emit(
                SettingsEffect.LaunchBackupDocument(
                    suggestedFileName = BACKUP_FILE_NAME
                )
            )
        }
    }

    private fun launchRestoreDocumentPicker() {
        viewModelScope.launch {
            mutableEffects.emit(SettingsEffect.LaunchRestoreDocumentPicker)
        }
    }

    private fun launchExportCardsDocument() {
        viewModelScope.launch {
            mutableEffects.emit(
                SettingsEffect.LaunchExportCardsDocument(
                    suggestedFileName = EXPORT_FILE_NAME
                )
            )
        }
    }

    private fun createBackup(uri: Uri?) {
        if (uri == null) {
            return
        }

        runDataTransfer(
            action = {
                userDataTransferRepository.backupTo(uri)
                mutableEffects.emit(
                    SettingsEffect.ShowMessage(R.string.settings_backup_success_message)
                )
            },
            onFailureMessageRes = R.string.settings_backup_failed_message
        )
    }

    private fun prepareRestore(uri: Uri?) {
        if (uri == null) {
            return
        }

        pendingRestoreData = null
        mutableUiState.update { current ->
            current.copy(pendingRestorePreview = null)
        }

        runDataTransfer(
            action = {
                val preparedRestore = userDataTransferRepository.prepareRestore(uri)
                pendingRestoreData = preparedRestore
                mutableUiState.update { current ->
                    current.copy(
                        pendingRestorePreview = RestorePreviewUiState(
                            exportedAt = preparedRestore.preview.exportedAt,
                            categoryCount = preparedRestore.preview.categoryCount,
                            cardCount = preparedRestore.preview.cardCount,
                            searchHistoryCount = preparedRestore.preview.searchHistoryCount,
                            includesSettings = preparedRestore.preview.includesSettings
                        )
                    )
                }
            },
            onFailureMessageRes = R.string.settings_restore_invalid_message
        )
    }

    private fun restoreBackup() {
        val restoreData = pendingRestoreData ?: return

        runDataTransfer(
            action = {
                userDataTransferRepository.restore(restoreData)
                dismissRestorePreview()
                mutableEffects.emit(
                    SettingsEffect.ShowMessage(R.string.settings_restore_success_message)
                )
            },
            onFailureMessageRes = R.string.settings_restore_failed_message
        )
    }

    private fun dismissRestorePreview() {
        pendingRestoreData = null
        mutableUiState.update { current ->
            current.copy(pendingRestorePreview = null)
        }
    }

    private fun exportCards(uri: Uri?) {
        if (uri == null) {
            return
        }

        runDataTransfer(
            action = {
                userDataTransferRepository.exportCardsTo(uri)
                mutableEffects.emit(
                    SettingsEffect.ShowMessage(R.string.settings_export_success_message)
                )
            },
            onFailureMessageRes = R.string.settings_export_failed_message
        )
    }

    private fun runDataTransfer(
        action: suspend () -> Unit,
        onFailureMessageRes: Int
    ) {
        viewModelScope.launch {
            mutableUiState.update { current ->
                current.copy(isDataTransferInProgress = true)
            }
            runCatching {
                action()
            }.onFailure {
                mutableEffects.emit(SettingsEffect.ShowMessage(onFailureMessageRes))
            }
            mutableUiState.update { current ->
                current.copy(isDataTransferInProgress = false)
            }
        }
    }

    companion object {
        private const val BACKUP_FILE_NAME = "digital-wallet-backup.json"
        private const val EXPORT_FILE_NAME = "digital-wallet-cards.csv"
    }
}
