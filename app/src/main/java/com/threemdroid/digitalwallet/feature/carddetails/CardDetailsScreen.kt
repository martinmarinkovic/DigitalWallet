package com.threemdroid.digitalwallet.feature.carddetails

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.navigation.encodeRouteValue
import kotlinx.coroutines.flow.collectLatest

object CardDetailsRoutes {
    const val cardIdArg = "cardId"
    const val cardDetails = "home/card-details/{$cardIdArg}"

    fun cardDetails(cardId: String): String =
        "home/card-details/${encodeRouteValue(cardId)}"
}

object EditCardRoutes {
    const val cardIdArg = CardDetailsRoutes.cardIdArg
    const val editCard = "home/card-details/{$cardIdArg}/edit"

    fun editCard(cardId: String): String =
        "home/card-details/${encodeRouteValue(cardId)}/edit"
}

object FullscreenCodeRoutes {
    const val cardIdArg = CardDetailsRoutes.cardIdArg
    const val fullscreenCode = "home/card-details/{$cardIdArg}/fullscreen-code"

    fun fullscreenCode(cardId: String): String =
        "home/card-details/${encodeRouteValue(cardId)}/fullscreen-code"
}

@Composable
fun CardDetailsRoute(
    onNavigateBack: () -> Unit,
    onOpenEdit: (String) -> Unit,
    onOpenFullscreenCode: (String) -> Unit,
    viewModel: CardDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                CardDetailsEffect.NavigateBack -> onNavigateBack()
                is CardDetailsEffect.OpenEdit -> onOpenEdit(effect.cardId)
                is CardDetailsEffect.OpenFullscreenCode -> onOpenFullscreenCode(effect.cardId)
            }
        }
    }

    CardDetailsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CardDetailsScreen(
    uiState: CardDetailsUiState,
    onEvent: (CardDetailsEvent) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.title.isBlank()) {
                            stringResource(id = R.string.card_details_title)
                        } else {
                            uiState.title
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CardDetailsEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    if (uiState.isContentVisible) {
                        IconButton(
                            onClick = { onEvent(CardDetailsEvent.OnFavoriteClicked) },
                            enabled = !uiState.isFavoriteUpdating && !uiState.isDeleteInProgress
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = if (uiState.isFavorite) {
                                    stringResource(id = R.string.card_details_remove_favorite)
                                } else {
                                    stringResource(id = R.string.card_details_add_favorite)
                                }
                            )
                        }
                        IconButton(onClick = { onEvent(CardDetailsEvent.OnEditClicked) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(id = R.string.card_details_edit)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.isCardMissing -> {
                CardDetailsStatusState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(id = R.string.card_details_missing_title),
                    message = stringResource(id = R.string.card_details_missing_message)
                )
            }

            else -> {
                CardDetailsContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }

    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(CardDetailsEvent.OnDeleteDismissed) },
            title = {
                Text(text = stringResource(id = R.string.card_details_delete_title))
            },
            text = {
                Text(text = stringResource(id = R.string.card_details_delete_message))
            },
            confirmButton = {
                TextButton(onClick = { onEvent(CardDetailsEvent.OnDeleteConfirmed) }) {
                    Text(text = stringResource(id = R.string.card_details_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(CardDetailsEvent.OnDeleteDismissed) }) {
                    Text(text = stringResource(id = R.string.create_category_cancel))
                }
            }
        )
    }
}

@Composable
private fun CardDetailsContent(
    modifier: Modifier = Modifier,
    uiState: CardDetailsUiState,
    onEvent: (CardDetailsEvent) -> Unit
) {
    val accentColor = uiState.categoryColorHex.toComposeColor()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isActionErrorVisible) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(id = R.string.card_details_action_error_message),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryPill(
                        categoryName = uiState.categoryName.ifBlank {
                            stringResource(id = R.string.card_details_unknown_category)
                        },
                        accentColor = accentColor
                    )
                    FavoritePill(isFavorite = uiState.isFavorite)
                }

                Text(
                    text = stringResource(id = R.string.card_details_code_preview_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = accentColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = uiState.codeTypeLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        SelectionContainer {
                            Text(
                                text = uiState.codeValue,
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { onEvent(CardDetailsEvent.OnOpenFullscreenCodeClicked) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInFull,
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(id = R.string.card_details_open_fullscreen_code),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        DetailsCard(
            title = stringResource(id = R.string.card_details_information_title),
            details = listOfNotNull(
                CardDetailItemUiModel(
                    label = stringResource(id = R.string.card_details_category_label),
                    value = uiState.categoryName.ifBlank {
                        stringResource(id = R.string.card_details_unknown_category)
                    }
                ),
                uiState.cardNumber?.let { value ->
                    CardDetailItemUiModel(
                        label = stringResource(id = R.string.card_details_card_number_label),
                        value = value
                    )
                },
                uiState.expirationDate?.let { value ->
                    CardDetailItemUiModel(
                        label = stringResource(id = R.string.card_details_expiration_date_label),
                        value = value
                    )
                },
                uiState.notes?.let { value ->
                    CardDetailItemUiModel(
                        label = stringResource(id = R.string.card_details_notes_label),
                        value = value
                    )
                }
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_details_actions")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onEvent(CardDetailsEvent.OnEditClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(id = R.string.card_details_edit),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                TextButton(
                    onClick = { onEvent(CardDetailsEvent.OnDeleteClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isDeleteInProgress
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(id = R.string.card_details_delete),
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(
    categoryName: String,
    accentColor: Color
) {
    Surface(
        color = accentColor.copy(alpha = 0.12f),
        contentColor = accentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accentColor, CircleShape)
            )
            Text(
                text = categoryName,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun FavoritePill(isFavorite: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = if (isFavorite) {
                stringResource(id = R.string.card_details_favorite_on)
            } else {
                stringResource(id = R.string.card_details_favorite_off)
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private data class CardDetailItemUiModel(
    val label: String,
    val value: String
)

@Composable
private fun DetailsCard(
    title: String,
    details: List<CardDetailItemUiModel>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            details.forEach { detail ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = detail.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionContainer {
                        Text(
                            text = detail.value,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardDetailsStatusState(
    modifier: Modifier = Modifier,
    title: String,
    message: String
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

private fun String.toComposeColor(): Color =
    runCatching {
        Color(parseColor(this))
    }.getOrDefault(Color(0xFF64748B))
