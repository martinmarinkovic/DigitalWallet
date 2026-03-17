package com.threemdroid.digitalwallet.feature.addcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.threemdroid.digitalwallet.feature.carddetails.EditCardRoutes
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.res.stringResource

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
            }
            ,
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ManualEntryScreen(
    uiState: ManualEntryUiState,
    onEvent: (ManualEntryEvent) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = uiState.titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ManualEntryEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back)
                        )
                    }
                }
            )
        },
        bottomBar = if (!uiState.isLoading && !uiState.isCardMissing) {
            {
                Surface(
                    modifier = Modifier.imePadding(),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (uiState.isSaveErrorVisible) {
                            Text(
                                text = stringResource(id = R.string.manual_entry_save_error_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = { onEvent(ManualEntryEvent.OnSaveClicked) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (uiState.isSaveErrorVisible) 12.dp else 0.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 12.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(text = stringResource(id = uiState.saveButtonRes))
                        }
                    }
                }
            }
        } else {
            {}
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        if (uiState.isCardMissing) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.card_details_missing_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.card_details_missing_message),
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.reviewMessageRes?.let { reviewMessageRes ->
                    item {
                        Text(
                            text = stringResource(id = reviewMessageRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.cardName,
                        onValueChange = { onEvent(ManualEntryEvent.OnCardNameChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.manual_entry_card_name_label)) },
                        enabled = !uiState.isSaving,
                        singleLine = true,
                        isError = uiState.cardNameError != null,
                        supportingText = {
                            if (uiState.cardNameError != null) {
                                Text(text = stringResource(id = R.string.manual_entry_required_field_error))
                            }
                        }
                    )
                }

                item {
                    CategorySelector(
                        categories = uiState.availableCategories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        enabled = !uiState.isSaving,
                        isError = uiState.categoryError != null,
                        onCategorySelected = { categoryId ->
                            onEvent(ManualEntryEvent.OnCategorySelected(categoryId))
                        }
                    )
                }

                item {
                    CodeTypeSelector(
                        codeTypes = uiState.availableCodeTypes,
                        selectedCodeType = uiState.selectedCodeType,
                        enabled = !uiState.isSaving,
                        onCodeTypeSelected = { codeType ->
                            onEvent(ManualEntryEvent.OnCodeTypeSelected(codeType))
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.codeValue,
                        onValueChange = { onEvent(ManualEntryEvent.OnCodeValueChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.manual_entry_code_value_label)) },
                        enabled = !uiState.isSaving,
                        singleLine = true,
                        isError = uiState.codeValueError != null,
                        supportingText = {
                            if (uiState.codeValueError != null) {
                                Text(text = stringResource(id = R.string.manual_entry_required_field_error))
                            }
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.cardNumber,
                        onValueChange = { onEvent(ManualEntryEvent.OnCardNumberChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.manual_entry_card_number_label)) },
                        enabled = !uiState.isSaving,
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.expirationDateInput,
                        onValueChange = { onEvent(ManualEntryEvent.OnExpirationDateChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.manual_entry_expiration_date_label)) },
                        placeholder = { Text(text = stringResource(id = R.string.manual_entry_expiration_date_placeholder)) },
                        enabled = !uiState.isSaving,
                        singleLine = true,
                        isError = uiState.expirationDateError != null,
                        supportingText = {
                            Text(
                                text = if (uiState.expirationDateError != null) {
                                    stringResource(id = R.string.manual_entry_expiration_date_error)
                                } else {
                                    stringResource(id = R.string.manual_entry_expiration_date_supporting_text)
                                }
                            )
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = { onEvent(ManualEntryEvent.OnNotesChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.manual_entry_notes_label)) },
                        enabled = !uiState.isSaving,
                        minLines = 3
                    )
                }

                item {
                    FavoriteToggleRow(
                        checked = uiState.isFavorite,
                        enabled = !uiState.isSaving,
                        onCheckedChange = { isFavorite ->
                            onEvent(ManualEntryEvent.OnFavoriteChanged(isFavorite))
                        }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategorySelector(
    categories: List<ManualEntryCategoryOptionUiModel>,
    selectedCategoryId: String?,
    enabled: Boolean,
    isError: Boolean,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.firstOrNull { category ->
        category.id == selectedCategoryId
    }?.name.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { shouldExpand ->
            if (enabled) {
                expanded = shouldExpand
            }
        }
    ) {
        OutlinedTextField(
            value = selectedCategoryName,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            enabled = enabled,
            label = { Text(text = stringResource(id = R.string.manual_entry_category_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(text = stringResource(id = R.string.manual_entry_required_field_error))
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(text = category.name) },
                    onClick = {
                        expanded = false
                        onCategorySelected(category.id)
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CodeTypeSelector(
    codeTypes: List<ManualEntryCodeTypeUiModel>,
    selectedCodeType: com.threemdroid.digitalwallet.core.model.CardCodeType,
    enabled: Boolean,
    onCodeTypeSelected: (com.threemdroid.digitalwallet.core.model.CardCodeType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCodeTypeLabel = codeTypes.firstOrNull { codeType ->
        codeType.codeType == selectedCodeType
    }?.label.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { shouldExpand ->
            if (enabled) {
                expanded = shouldExpand
            }
        }
    ) {
        OutlinedTextField(
            value = selectedCodeTypeLabel,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            enabled = enabled,
            label = { Text(text = stringResource(id = R.string.manual_entry_code_type_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            codeTypes.forEach { codeType ->
                DropdownMenuItem(
                    text = { Text(text = codeType.label) },
                    onClick = {
                        expanded = false
                        onCodeTypeSelected(codeType.codeType)
                    }
                )
            }
        }
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
        tonalElevation = 1.dp
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
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(id = R.string.manual_entry_favorite_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(id = R.string.manual_entry_favorite_supporting_text),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        }
    }
}
