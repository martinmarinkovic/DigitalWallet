package com.threemdroid.digitalwallet.feature.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.core.navigation.TopLevelDestination
import com.threemdroid.digitalwallet.ui.theme.walletSwitchColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.collectLatest

fun NavGraphBuilder.settingsScreen(
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    composable(route = TopLevelDestination.SETTINGS.startRoute) {
        SettingsRoute(
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            onOpenTerms = onOpenTerms
        )
    }
}

@Composable
private fun SettingsRoute(
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appVersion = remember(context) { context.resolveAppVersionLabel() }
    val backupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        viewModel.onEvent(SettingsEvent.OnBackupDestinationSelected(uri))
    }
    val exportCardsDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        viewModel.onEvent(SettingsEvent.OnExportCardsDestinationSelected(uri))
    }
    val restoreDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onEvent(SettingsEvent.OnRestoreSourceSelected(uri))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onEvent(SettingsEvent.OnReminderNotificationPermissionChanged(granted))
    }

    LaunchedEffect(viewModel, context) {
        viewModel.onEvent(
            SettingsEvent.OnReminderNotificationPermissionChanged(
                granted = context.hasReminderNotificationPermission()
            )
        )
    }

    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(
                    SettingsEvent.OnReminderNotificationPermissionChanged(
                        granted = context.hasReminderNotificationPermission()
                    )
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(effect.messageRes)
                    )
                }

                SettingsEffect.OpenPrivacyPolicy -> onOpenPrivacyPolicy()

                SettingsEffect.OpenHelpAndFeedback -> {
                    if (!context.openHelpAndFeedbackEmail()) {
                        snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.settings_help_feedback_unavailable_message
                            )
                        )
                    }
                }

                SettingsEffect.OpenTerms -> onOpenTerms()

                is SettingsEffect.LaunchBackupDocument -> {
                    backupDocumentLauncher.launch(effect.suggestedFileName)
                }

                is SettingsEffect.LaunchExportCardsDocument -> {
                    exportCardsDocumentLauncher.launch(effect.suggestedFileName)
                }

                SettingsEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                SettingsEffect.LaunchRestoreDocumentPicker -> {
                    restoreDocumentLauncher.launch(arrayOf("application/json", "text/*"))
                }
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        appVersion = appVersion,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsScreen(
    uiState: SettingsUiState,
    appVersion: String,
    snackbarHostState: SnackbarHostState,
    onEvent: (SettingsEvent) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.nav_settings)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsSectionCard(
                        title = stringResource(id = R.string.settings_section_appearance)
                    ) {
                        SettingsRadioRow(
                            title = stringResource(id = R.string.settings_theme_system),
                            selected = uiState.themeMode == ThemeMode.SYSTEM,
                            onClick = {
                                onEvent(SettingsEvent.OnThemeModeSelected(ThemeMode.SYSTEM))
                            }
                        )
                        SettingsRadioRow(
                            title = stringResource(id = R.string.settings_theme_light),
                            selected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = {
                                onEvent(SettingsEvent.OnThemeModeSelected(ThemeMode.LIGHT))
                            }
                        )
                        SettingsRadioRow(
                            title = stringResource(id = R.string.settings_theme_dark),
                            selected = uiState.themeMode == ThemeMode.DARK,
                            onClick = {
                                onEvent(SettingsEvent.OnThemeModeSelected(ThemeMode.DARK))
                            }
                        )
                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_auto_brightness_title),
                            subtitle = stringResource(id = R.string.settings_auto_brightness_subtitle),
                            checked = uiState.autoBrightnessEnabled,
                            onCheckedChange = { enabled ->
                                onEvent(SettingsEvent.OnAutoBrightnessChanged(enabled))
                            }
                        )
                    }
                }

                item {
                    SettingsSectionCard(
                        title = stringResource(id = R.string.settings_section_data)
                    ) {
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_backup_title),
                            subtitle = stringResource(id = R.string.settings_backup_subtitle),
                            enabled = !uiState.isDataTransferInProgress,
                            onClick = { onEvent(SettingsEvent.OnBackupClicked) }
                        )
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_restore_title),
                            subtitle = stringResource(id = R.string.settings_restore_subtitle),
                            enabled = !uiState.isDataTransferInProgress,
                            onClick = { onEvent(SettingsEvent.OnRestoreClicked) }
                        )
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_export_cards_title),
                            subtitle = stringResource(id = R.string.settings_export_cards_subtitle),
                            enabled = !uiState.isDataTransferInProgress,
                            onClick = { onEvent(SettingsEvent.OnExportCardsClicked) }
                        )
                    }
                }

                item {
                    SettingsSectionCard(
                        title = stringResource(id = R.string.settings_section_reminders)
                    ) {
                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_reminders_title),
                            subtitle = uiState.reminderDeliveryStatus.subtitle(),
                            checked = uiState.reminderEnabled,
                            onCheckedChange = { enabled ->
                                onEvent(SettingsEvent.OnReminderEnabledChanged(enabled))
                            }
                        )
                        if (uiState.reminderDeliveryStatus == ReminderDeliveryStatusUiModel.BLOCKED) {
                            SettingsActionRow(
                                title = stringResource(id = R.string.settings_reminders_allow_notifications_title),
                                subtitle = stringResource(id = R.string.settings_reminders_allow_notifications_subtitle),
                                onClick = {
                                    onEvent(SettingsEvent.OnReminderNotificationPermissionRequestClicked)
                                }
                            )
                        }
                        ReminderTiming.entries.forEach { reminderTiming ->
                            SettingsRadioRow(
                                title = reminderTiming.label(),
                                selected = uiState.reminderTiming == reminderTiming,
                                enabled = uiState.reminderEnabled,
                                onClick = {
                                    onEvent(SettingsEvent.OnReminderTimingSelected(reminderTiming))
                                }
                            )
                        }
                    }
                }

                item {
                    SettingsSectionCard(
                        title = stringResource(id = R.string.settings_section_search)
                    ) {
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_clear_search_history_title),
                            subtitle = if (uiState.hasSearchHistory) {
                                stringResource(id = R.string.settings_clear_search_history_subtitle)
                            } else {
                                stringResource(id = R.string.settings_search_history_empty_subtitle)
                            },
                            enabled = uiState.hasSearchHistory,
                            onClick = { onEvent(SettingsEvent.OnClearSearchHistoryClicked) }
                        )
                    }
                }

                item {
                    SettingsSectionCard(
                        title = stringResource(id = R.string.settings_section_app)
                    ) {
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_help_feedback_title),
                            onClick = { onEvent(SettingsEvent.OnHelpAndFeedbackClicked) }
                        )
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_privacy_policy_title),
                            onClick = { onEvent(SettingsEvent.OnPrivacyPolicyClicked) }
                        )
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_terms_title),
                            onClick = { onEvent(SettingsEvent.OnTermsClicked) }
                        )
                        SettingsValueRow(
                            title = stringResource(id = R.string.settings_app_version_title),
                            value = appVersion
                        )
                    }
                }
            }
        }

        uiState.pendingRestorePreview?.let { preview ->
            RestoreBackupDialog(
                preview = preview,
                isBusy = uiState.isDataTransferInProgress,
                onConfirm = { onEvent(SettingsEvent.OnRestoreConfirmed) },
                onDismiss = { onEvent(SettingsEvent.OnRestoreDismissed) }
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    SettingsRowContainer(
        title = title,
        subtitle = subtitle,
        onClick = { onCheckedChange(!checked) },
        role = Role.Switch
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = walletSwitchColors()
        )
    }
}

@Composable
private fun SettingsRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    SettingsRowContainer(
        title = title,
        enabled = enabled,
        onClick = onClick,
        role = Role.RadioButton
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    SettingsRowContainer(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
private fun SettingsValueRow(
    title: String,
    value: String
) {
    SettingsRowContainer(
        title = title,
        subtitle = value,
        enabled = false,
        onClick = {}
    )
}

@Composable
private fun RestoreBackupDialog(
    preview: RestorePreviewUiState,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
    }

    AlertDialog(
        onDismissRequest = {
            if (!isBusy) {
                onDismiss()
            }
        },
        title = {
            Text(text = stringResource(id = R.string.settings_restore_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.settings_restore_dialog_message))
                Text(
                    text = stringResource(
                        id = R.string.settings_restore_dialog_summary,
                        formatter.format(preview.exportedAt),
                        preview.categoryCount,
                        preview.cardCount,
                        preview.searchHistoryCount
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (preview.includesSettings) {
                    Text(
                        text = stringResource(id = R.string.settings_restore_dialog_settings_note),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isBusy
            ) {
                Text(text = stringResource(id = R.string.settings_restore_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy
            ) {
                Text(text = stringResource(id = R.string.create_category_cancel))
            }
        }
    )
}

@Composable
private fun SettingsRowContainer(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    role: Role? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    role = role,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                subtitle?.let { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailingContent?.invoke()
        }
        HorizontalDivider()
    }
}

@Composable
private fun ReminderTiming.label(): String =
    when (this) {
        ReminderTiming.ON_DAY -> stringResource(id = R.string.settings_reminder_on_day)
        ReminderTiming.ONE_DAY_BEFORE -> stringResource(id = R.string.settings_reminder_one_day)
        ReminderTiming.THREE_DAYS_BEFORE -> stringResource(id = R.string.settings_reminder_three_days)
        ReminderTiming.SEVEN_DAYS_BEFORE -> stringResource(id = R.string.settings_reminder_seven_days)
    }

@Composable
private fun ReminderDeliveryStatusUiModel.subtitle(): String =
    when (this) {
        ReminderDeliveryStatusUiModel.ENABLED -> {
            stringResource(id = R.string.settings_reminders_enabled_subtitle)
        }

        ReminderDeliveryStatusUiModel.DISABLED -> {
            stringResource(id = R.string.settings_reminders_disabled_subtitle)
        }

        ReminderDeliveryStatusUiModel.BLOCKED -> {
            stringResource(id = R.string.settings_reminders_blocked_subtitle)
        }
    }

private fun Context.hasReminderNotificationPermission(): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

private fun Context.resolveAppVersionLabel(): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    val versionName = packageInfo.versionName.orEmpty().ifBlank { "1.0" }
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return "$versionName ($versionCode)"
}

private fun Context.openHelpAndFeedbackEmail(): Boolean {
    val emailIntent = Intent(
        Intent.ACTION_SENDTO,
        Uri.parse("mailto:threemdroid@gmail.com")
    ).apply {
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_help_feedback_title))
    }
    return try {
        if (this !is Activity) {
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(emailIntent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
