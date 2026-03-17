package com.threemdroid.digitalwallet.feature.addcard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.feature.carddetails.EditCardRoutes
import com.threemdroid.digitalwallet.ui.theme.walletSwitchColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

fun NavGraphBuilder.manualEntryScreen(
    onNavigateBack: () -> Unit,
    onCardSaved: (String) -> Unit
) {
    composable(
        route = ManualEntryRoutes.routePattern,
        arguments = listOf(
            navArgument(AddCardRoutes.categoryIdArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ManualEntryRoutes.sourceArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ManualEntryRoutes.codeTypeArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ManualEntryRoutes.codeValueArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ManualEntryRoutes.cardNumberArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ManualEntryRoutes.cardNameArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ManualEntryRoutes.notesArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        ManualEntryRoute(
            onNavigateBack = onNavigateBack,
            onCardSaved = onCardSaved
        )
    }
}

fun NavGraphBuilder.editCardScreen(
    onNavigateBack: () -> Unit
) {
    composable(
        route = EditCardRoutes.editCard,
        arguments = listOf(
            navArgument(EditCardRoutes.cardIdArg) {
                type = NavType.StringType
            }
        )
    ) {
        ManualEntryRoute(
            onNavigateBack = onNavigateBack,
            onCardSaved = {}
        )
    }
}

@Composable
fun ManualEntryRoute(
    onNavigateBack: () -> Unit,
    onCardSaved: (String) -> Unit,
    viewModel: ManualEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                ManualEntryEffect.NavigateBack -> onNavigateBack()
                is ManualEntryEffect.CardSaved -> onCardSaved(effect.categoryId)
            }
        }
    }

    ManualEntryScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

private enum class ManualEntrySelectionSheet {
    CATEGORY,
    CODE_TYPE
}

private data class ManualEntrySelectionOption(
    val id: String,
    val label: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ManualEntryScreen(
    uiState: ManualEntryUiState,
    onEvent: (ManualEntryEvent) -> Unit
) {
    var activeSheet by rememberSaveable { mutableStateOf<ManualEntrySelectionSheet?>(null) }
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = {
                    Text(text = androidx.compose.ui.res.stringResource(id = uiState.titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ManualEntryEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(
                                id = R.string.navigate_back
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            uiState.isCardMissing -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = R.string.card_details_missing_title
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = R.string.card_details_missing_message
                            ),
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ManualEntryFormFields(
                                uiState = uiState,
                                onEvent = onEvent,
                                onOpenCategorySheet = {
                                    activeSheet = ManualEntrySelectionSheet.CATEGORY
                                },
                                onOpenCodeTypeSheet = {
                                    activeSheet = ManualEntrySelectionSheet.CODE_TYPE
                                },
                                onOpenDatePicker = {
                                    isDatePickerVisible = true
                                }
                            )
                        }

                        Surface(
                            modifier = Modifier.imePadding(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            ManualEntrySaveBar(
                                uiState = uiState,
                                onEvent = onEvent
                            )
                        }
                    }
                }
            }
        }
    }

    when (activeSheet) {
        ManualEntrySelectionSheet.CATEGORY -> {
            SelectionBottomSheet(
                title = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_category_label),
                options = uiState.availableCategories.map { option ->
                    ManualEntrySelectionOption(
                        id = option.id,
                        label = option.name
                    )
                },
                selectedOptionId = uiState.selectedCategoryId,
                onDismiss = { activeSheet = null },
                onOptionSelected = { categoryId ->
                    activeSheet = null
                    onEvent(ManualEntryEvent.OnCategorySelected(categoryId))
                }
            )
        }

        ManualEntrySelectionSheet.CODE_TYPE -> {
            SelectionBottomSheet(
                title = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_code_type_label),
                options = uiState.availableCodeTypes.map { option ->
                    ManualEntrySelectionOption(
                        id = option.codeType.name,
                        label = option.label
                    )
                },
                selectedOptionId = uiState.selectedCodeType.name,
                onDismiss = { activeSheet = null },
                onOptionSelected = { optionId ->
                    activeSheet = null
                    CardCodeType.entries.firstOrNull { codeType ->
                        codeType.name == optionId
                    }?.let { codeType ->
                        onEvent(ManualEntryEvent.OnCodeTypeSelected(codeType))
                    }
                }
            )
        }

        null -> Unit
    }

    if (isDatePickerVisible) {
        ExpirationDatePickerDialog(
            initialDate = uiState.expirationDateInput,
            onDismiss = { isDatePickerVisible = false },
            onDateSelected = { selectedDate ->
                isDatePickerVisible = false
                onEvent(ManualEntryEvent.OnExpirationDateChanged(selectedDate))
            },
            onClear = {
                isDatePickerVisible = false
                onEvent(ManualEntryEvent.OnExpirationDateChanged(""))
            }
        )
    }
}

@Composable
private fun ManualEntryFormFields(
    uiState: ManualEntryUiState,
    onEvent: (ManualEntryEvent) -> Unit,
    onOpenCategorySheet: () -> Unit,
    onOpenCodeTypeSheet: () -> Unit,
    onOpenDatePicker: () -> Unit
) {
    uiState.reviewMessageRes?.let { reviewMessageRes ->
        Text(
            text = androidx.compose.ui.res.stringResource(id = reviewMessageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OutlinedTextField(
        value = uiState.cardName,
        onValueChange = { onEvent(ManualEntryEvent.OnCardNameChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_card_name_label))
        },
        enabled = !uiState.isSaving,
        singleLine = true,
        isError = uiState.cardNameError != null,
        supportingText = {
            if (uiState.cardNameError != null) {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        id = R.string.manual_entry_required_field_error
                    )
                )
            }
        }
    )

    SelectorField(
        title = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_category_label),
        value = uiState.availableCategories.firstOrNull { category ->
            category.id == uiState.selectedCategoryId
        }?.name.orEmpty(),
        enabled = !uiState.isSaving,
        isError = uiState.categoryError != null,
        errorText = if (uiState.categoryError != null) {
            androidx.compose.ui.res.stringResource(id = R.string.manual_entry_required_field_error)
        } else {
            null
        },
        onClick = onOpenCategorySheet
    )

    SelectorField(
        title = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_code_type_label),
        value = uiState.availableCodeTypes.firstOrNull { codeType ->
            codeType.codeType == uiState.selectedCodeType
        }?.label.orEmpty(),
        enabled = !uiState.isSaving,
        onClick = onOpenCodeTypeSheet
    )

    OutlinedTextField(
        value = uiState.codeValue,
        onValueChange = { onEvent(ManualEntryEvent.OnCodeValueChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_code_value_label))
        },
        enabled = !uiState.isSaving,
        singleLine = true,
        isError = uiState.codeValueError != null,
        supportingText = {
            if (uiState.codeValueError != null) {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        id = R.string.manual_entry_required_field_error
                    )
                )
            }
        }
    )

    OutlinedTextField(
        value = uiState.cardNumber,
        onValueChange = { onEvent(ManualEntryEvent.OnCardNumberChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_card_number_label))
        },
        enabled = !uiState.isSaving,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    DateSelectorField(
        value = uiState.expirationDateInput,
        enabled = !uiState.isSaving,
        isError = uiState.expirationDateError != null,
        supportingText = if (uiState.expirationDateError != null) {
            androidx.compose.ui.res.stringResource(id = R.string.manual_entry_expiration_date_error)
        } else {
            null
        },
        onClick = onOpenDatePicker
    )

    OutlinedTextField(
        value = uiState.notes,
        onValueChange = { onEvent(ManualEntryEvent.OnNotesChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_notes_label))
        },
        enabled = !uiState.isSaving,
        minLines = 3,
        maxLines = 5
    )

    FavoriteToggleRow(
        checked = uiState.isFavorite,
        enabled = !uiState.isSaving,
        onCheckedChange = { isFavorite ->
            onEvent(ManualEntryEvent.OnFavoriteChanged(isFavorite))
        }
    )
}

@Composable
private fun ManualEntrySaveBar(
    uiState: ManualEntryUiState,
    onEvent: (ManualEntryEvent) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (uiState.isSaveErrorVisible) {
            Text(
                text = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_save_error_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = { onEvent(ManualEntryEvent.OnSaveClicked) },
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (uiState.isSaveErrorVisible) 10.dp else 0.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 12.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            }
            Text(text = androidx.compose.ui.res.stringResource(id = uiState.saveButtonRes))
        }
    }
}

@Composable
private fun SelectorField(
    title: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isError: Boolean = false,
    errorText: String? = null
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            label = { Text(text = title) },
            singleLine = true,
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            },
            supportingText = {
                if (errorText != null) {
                    Text(text = errorText)
                }
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled, onClick = onClick)
        )
    }
}

@Composable
private fun DateSelectorField(
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isError: Boolean,
    supportingText: String?
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            label = {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        id = R.string.manual_entry_expiration_date_label
                    )
                )
            },
            placeholder = {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        id = R.string.manual_entry_expiration_date_placeholder
                    )
                )
            },
            singleLine = true,
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null
                )
            },
            supportingText = {
                if (supportingText != null) {
                    Text(text = supportingText)
                }
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled, onClick = onClick)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelectionBottomSheet(
    title: String,
    options: List<ManualEntrySelectionOption>,
    selectedOptionId: String?,
    onDismiss: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = options,
                    key = { option -> option.id }
                ) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option.id) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (option.id == selectedOptionId) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExpirationDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
    onClear: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toDatePickerMillisOrNull()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = androidx.compose.material3.DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            headlineContentColor = MaterialTheme.colorScheme.onBackground,
            weekdayContentColor = MaterialTheme.colorScheme.onBackground,
            subheadContentColor = MaterialTheme.colorScheme.onBackground,
            yearContentColor = MaterialTheme.colorScheme.onBackground,
            currentYearContentColor = MaterialTheme.colorScheme.primary,
            selectedYearContentColor = Color.White,
            selectedYearContainerColor = MaterialTheme.colorScheme.primary,
            dayContentColor = MaterialTheme.colorScheme.onBackground,
            disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedDayContentColor = Color.White,
            disabledSelectedDayContentColor = Color.White,
            selectedDayContainerColor = MaterialTheme.colorScheme.primary,
            todayContentColor = MaterialTheme.colorScheme.primary,
            todayDateBorderColor = MaterialTheme.colorScheme.primary
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                        ?.toManualEntryDateString()
                    if (selectedDate != null) {
                        onDateSelected(selectedDate)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(text = androidx.compose.ui.res.stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            id = R.string.manual_entry_expiration_date_clear
                        )
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            id = android.R.string.cancel
                        )
                    )
                }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun FavoriteToggleRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.manual_entry_favorite_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        id = R.string.manual_entry_favorite_supporting_text
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = walletSwitchColors()
            )
        }
    }
}

private fun String.toDatePickerMillisOrNull(): Long? =
    runCatching {
        LocalDate.parse(this)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

private fun Long.toManualEntryDateString(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
