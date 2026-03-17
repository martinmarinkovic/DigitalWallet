package com.threemdroid.digitalwallet.feature.home

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.threemdroid.digitalwallet.R
import kotlinx.coroutines.flow.collect

@Composable
fun CreateCategoryDialogRoute(
    onDismiss: () -> Unit,
    viewModel: CreateCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CreateCategoryEffect.Dismiss -> onDismiss()
            }
        }
    }

    CreateCategoryDialog(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCategoryDialog(
    uiState: CreateCategoryUiState,
    onEvent: (CreateCategoryEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onEvent(CreateCategoryEvent.OnDismissRequested)
        },
        title = {
            Text(text = stringResource(id = R.string.create_category_title))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { onEvent(CreateCategoryEvent.OnNameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.create_category_name_label))
                    },
                    singleLine = true,
                    isError = uiState.nameError != null,
                    supportingText = {
                        if (uiState.nameError == CreateCategoryNameError.REQUIRED) {
                            Text(text = stringResource(id = R.string.create_category_name_required))
                        }
                    }
                )
                Text(
                    text = stringResource(id = R.string.create_category_color_label),
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.availableColorHexes.chunked(5).forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowColors.forEach { colorHex ->
                                CreateCategoryColorSwatch(
                                    colorHex = colorHex,
                                    isSelected = colorHex == uiState.selectedColorHex,
                                    onClick = {
                                        onEvent(CreateCategoryEvent.OnColorSelected(colorHex))
                                    }
                                )
                            }
                        }
                    }
                }
                if (uiState.isSaveErrorVisible) {
                    Text(
                        text = stringResource(id = R.string.create_category_save_error_message),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !uiState.isSaving,
                onClick = {
                    onEvent(CreateCategoryEvent.OnDismissRequested)
                }
            ) {
                Text(text = stringResource(id = R.string.create_category_cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !uiState.isSaving,
                onClick = {
                    onEvent(CreateCategoryEvent.OnSaveClicked)
                }
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(id = R.string.create_category_save))
                }
            }
        }
    )
}

@Composable
private fun CreateCategoryColorSwatch(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentDescription = stringResource(
        id = R.string.create_category_color_content_description,
        colorHex
    )
    val accentColor = runCatching {
        Color(parseColor(colorHex))
    }.getOrElse {
        MaterialTheme.colorScheme.primary
    }

    val borderColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accentColor)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
            }
    ) {
        if (isSelected) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
            )
        }
    }
}
