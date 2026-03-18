package com.threemdroid.digitalwallet.feature.settings

import android.net.Uri
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import java.time.Instant

enum class ReminderDeliveryStatusUiModel {
    ENABLED,
    DISABLED,
    BLOCKED
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val isDataTransferInProgress: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoBrightnessEnabled: Boolean = true,
    val cloudSyncEnabled: Boolean = false,
    val reminderEnabled: Boolean = true,
    val hasReminderNotificationPermission: Boolean = true,
    val reminderTiming: ReminderTiming = ReminderTiming.ON_DAY,
    val hasSearchHistory: Boolean = false,
    val pendingRestorePreview: RestorePreviewUiState? = null
) {
    val reminderDeliveryStatus: ReminderDeliveryStatusUiModel
        get() = when {
            !reminderEnabled -> ReminderDeliveryStatusUiModel.DISABLED
            hasReminderNotificationPermission -> ReminderDeliveryStatusUiModel.ENABLED
            else -> ReminderDeliveryStatusUiModel.BLOCKED
        }
}

data class RestorePreviewUiState(
    val exportedAt: Instant,
    val categoryCount: Int,
    val cardCount: Int,
    val searchHistoryCount: Int,
    val includesSettings: Boolean
)

sealed interface SettingsEvent {
    data class OnThemeModeSelected(val themeMode: ThemeMode) : SettingsEvent

    data class OnAutoBrightnessChanged(val enabled: Boolean) : SettingsEvent

    data class OnCloudSyncChanged(val enabled: Boolean) : SettingsEvent

    data class OnReminderEnabledChanged(val enabled: Boolean) : SettingsEvent

    data class OnReminderNotificationPermissionChanged(val granted: Boolean) : SettingsEvent

    data object OnReminderNotificationPermissionRequestClicked : SettingsEvent

    data class OnReminderTimingSelected(val reminderTiming: ReminderTiming) : SettingsEvent

    data object OnBackupClicked : SettingsEvent

    data class OnBackupDestinationSelected(val uri: Uri?) : SettingsEvent

    data object OnRestoreClicked : SettingsEvent

    data class OnRestoreSourceSelected(val uri: Uri?) : SettingsEvent

    data object OnRestoreConfirmed : SettingsEvent

    data object OnRestoreDismissed : SettingsEvent

    data object OnExportCardsClicked : SettingsEvent

    data class OnExportCardsDestinationSelected(val uri: Uri?) : SettingsEvent

    data object OnClearSearchHistoryClicked : SettingsEvent

    data object OnPrivacyPolicyClicked : SettingsEvent

    data object OnHelpAndFeedbackClicked : SettingsEvent

    data object OnTermsClicked : SettingsEvent
}

sealed interface SettingsEffect {
    data class ShowMessage(val messageRes: Int) : SettingsEffect

    data object OpenPrivacyPolicy : SettingsEffect

    data object OpenHelpAndFeedback : SettingsEffect

    data object OpenTerms : SettingsEffect

    data object RequestNotificationPermission : SettingsEffect

    data class LaunchBackupDocument(val suggestedFileName: String) : SettingsEffect

    data class LaunchExportCardsDocument(val suggestedFileName: String) : SettingsEffect

    data object LaunchRestoreDocumentPicker : SettingsEffect
}
