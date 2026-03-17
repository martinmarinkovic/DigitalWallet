package com.threemdroid.digitalwallet.feature.categorydetails
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.navigation.encodeRouteValue
import kotlinx.coroutines.flow.collectLatest

object CategoryDetailsRoutes {
    const val categoryIdArg = "categoryId"
    const val categoryDetails = "home/category-details/{$categoryIdArg}"

    fun categoryDetails(categoryId: String): String =
        "home/category-details/${encodeRouteValue(categoryId)}"
}

@Composable
fun CategoryDetailsRoute(
    onNavigateBack: () -> Unit,
    onOpenAddCard: (String?) -> Unit,
    onOpenCardDetails: (String) -> Unit,
    viewModel: CategoryDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val cardReorderFailedMessage =
        stringResource(id = R.string.category_details_card_reorder_failed_message)

    LaunchedEffect(viewModel, snackbarHostState, cardReorderFailedMessage, context) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                CategoryDetailsEffect.NavigateBack -> onNavigateBack()
                is CategoryDetailsEffect.OpenAddCard -> onOpenAddCard(effect.categoryId)
                is CategoryDetailsEffect.OpenCardDetails -> onOpenCardDetails(effect.cardId)
                CategoryDetailsEffect.ShowCardReorderFailedMessage -> {
                    snackbarHostState.showSnackbar(cardReorderFailedMessage)
                }
                is CategoryDetailsEffect.ShowDeleteMessage -> {
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                }
            }
        }
    }

    CategoryDetailsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategoryDetailsScreen(
    uiState: CategoryDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (CategoryDetailsEvent) -> Unit
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.title.isBlank()) {
                            stringResource(id = R.string.category_details_default_title)
                        } else {
                            uiState.title
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CategoryDetailsEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(CategoryDetailsEvent.OnDeleteClicked) },
                        enabled = !uiState.isDeleteInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(
                                id = R.string.category_details_delete_action
                            )
                        )
                    }
                    IconButton(onClick = { onEvent(CategoryDetailsEvent.OnAddCardClicked) }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.nav_add_card)
                        )
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

            uiState.isCategoryMissing -> {
                CategoryDetailsStatusState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(id = R.string.category_details_missing_title),
                    message = stringResource(id = R.string.category_details_missing_message)
                )
            }

            uiState.isEmpty -> {
                CategoryDetailsEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onAddCardClicked = {
                        onEvent(CategoryDetailsEvent.OnAddCardClicked)
                    }
                )
            }

            else -> {
                CategoryCardsGrid(
                    cards = uiState.cards,
                    isCardReordering = uiState.isCardReordering,
                    isCardReorderEnabled = uiState.isCardReorderEnabled,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onCardClicked = { cardId ->
                        onEvent(CategoryDetailsEvent.OnCardClicked(cardId))
                    },
                    onCardReorderStarted = { cardId ->
                        onEvent(CategoryDetailsEvent.OnCardReorderStarted(cardId))
                    },
                    onCardReorderMoved = { fromCardId, toCardId ->
                        onEvent(
                            CategoryDetailsEvent.OnCardReorderMoved(
                                fromCardId = fromCardId,
                                toCardId = toCardId
                            )
                        )
                    },
                    onCardReorderFinished = {
                        onEvent(CategoryDetailsEvent.OnCardReorderFinished)
                    },
                    onCardReorderCancelled = {
                        onEvent(CategoryDetailsEvent.OnCardReorderCancelled)
                    }
                )
            }
        }
    }

    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(CategoryDetailsEvent.OnDeleteDismissed) },
            title = {
                Text(text = stringResource(id = R.string.category_details_delete_title))
            },
            text = {
                Text(text = stringResource(id = R.string.category_details_delete_message))
            },
            confirmButton = {
                TextButton(onClick = { onEvent(CategoryDetailsEvent.OnDeleteConfirmed) }) {
                    Text(text = stringResource(id = R.string.category_details_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(CategoryDetailsEvent.OnDeleteDismissed) }) {
                    Text(text = stringResource(id = R.string.create_category_cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryCardsGrid(
    cards: List<CategoryDetailsCardUiModel>,
    isCardReordering: Boolean,
    isCardReorderEnabled: Boolean,
    modifier: Modifier = Modifier,
    onCardClicked: (String) -> Unit,
    onCardReorderStarted: (String) -> Unit,
    onCardReorderMoved: (String, String) -> Unit,
    onCardReorderFinished: () -> Unit,
    onCardReorderCancelled: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val hapticFeedback = LocalHapticFeedback.current
    val currentCardIds by rememberUpdatedState(
        newValue = cards.map { card -> card.id }.toSet()
    )
    val currentOnCardReorderStarted by rememberUpdatedState(onCardReorderStarted)
    val currentOnCardReorderMoved by rememberUpdatedState(onCardReorderMoved)
    val currentOnCardReorderFinished by rememberUpdatedState(onCardReorderFinished)
    val currentOnCardReorderCancelled by rememberUpdatedState(onCardReorderCancelled)
    var draggedCardId by remember { mutableStateOf<String?>(null) }
    var draggedItemOffset by remember { mutableStateOf(Offset.Zero) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(
            items = cards,
            key = { card -> card.id }
        ) { card ->
            val isBeingDragged = draggedCardId == card.id
            CategoryCardItem(
                card = card,
                modifier = Modifier
                    .zIndex(if (isBeingDragged) 1f else 0f)
                    .graphicsLayer {
                        if (isBeingDragged) {
                            translationX = draggedItemOffset.x
                            translationY = draggedItemOffset.y
                        }
                    }
                    .pointerInput(card.id, cards.size) {
                        if (!isCardReorderEnabled || cards.size <= 1) {
                            return@pointerInput
                        }

                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedCardId = card.id
                                draggedItemOffset = Offset.Zero
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnCardReorderStarted(card.id)
                            },
                            onDragCancel = {
                                draggedCardId = null
                                draggedItemOffset = Offset.Zero
                                currentOnCardReorderCancelled()
                            },
                            onDragEnd = {
                                draggedCardId = null
                                draggedItemOffset = Offset.Zero
                                currentOnCardReorderFinished()
                            }
                        ) { change, dragAmount ->
                            if (draggedCardId != card.id) {
                                return@detectDragGesturesAfterLongPress
                            }

                            change.consume()
                            draggedItemOffset += dragAmount

                            val reorderTarget = gridState.findCardReorderTarget(
                                draggedCardId = card.id,
                                draggedItemOffset = draggedItemOffset,
                                reorderableCardIds = currentCardIds
                            ) ?: return@detectDragGesturesAfterLongPress

                            draggedItemOffset -= reorderTarget.positionDelta
                            currentOnCardReorderMoved(
                                card.id,
                                reorderTarget.cardId
                            )
                        }
                    },
                enabled = !isCardReordering,
                isBeingDragged = isBeingDragged,
                onClick = {
                    onCardClicked(card.id)
                }
            )
        }
    }
}

@Composable
private fun CategoryCardItem(
    card: CategoryDetailsCardUiModel,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isBeingDragged: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        border = if (isBeingDragged) {
            BorderStroke(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.54f)
            )
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = card.name,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryCardTag(
                    text = card.codeTypeLabel,
                    backgroundColor = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White
                )
                card.expirationBadge?.let { badge ->
                    CategoryCardTag(
                        text = badge.text(),
                        backgroundColor = Color.White.copy(
                            alpha = if (badge.isCritical()) 0.24f else 0.16f
                        ),
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCardTag(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun CategoryDetailsEmptyState(
    modifier: Modifier = Modifier,
    onAddCardClicked: () -> Unit
) {
    CategoryDetailsStatusState(
        modifier = modifier,
        title = stringResource(id = R.string.category_details_empty_title),
        message = stringResource(id = R.string.category_details_empty_message),
        action = {
            Button(onClick = onAddCardClicked) {
                Text(text = stringResource(id = R.string.category_details_empty_action))
            }
        }
    )
}

@Composable
private fun CategoryDetailsStatusState(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    action: (@Composable () -> Unit)? = null
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
        action?.let {
            Box(modifier = Modifier.padding(top = 20.dp)) {
                it()
            }
        }
    }
}

@Composable
private fun CategoryDetailsExpirationBadgeUiModel.text(): String =
    when (status) {
        CategoryDetailsExpirationBadgeStatus.EXPIRED -> {
            stringResource(id = R.string.category_details_expired)
        }

        CategoryDetailsExpirationBadgeStatus.EXPIRES_TODAY -> {
            stringResource(id = R.string.category_details_expires_today)
        }

        CategoryDetailsExpirationBadgeStatus.EXPIRING_SOON -> {
            pluralStringResource(
                id = R.plurals.category_details_expires_in_days,
                count = daysUntilExpiration ?: 0,
                daysUntilExpiration ?: 0
            )
        }
    }

private fun CategoryDetailsExpirationBadgeUiModel.isCritical(): Boolean =
    status == CategoryDetailsExpirationBadgeStatus.EXPIRED ||
        status == CategoryDetailsExpirationBadgeStatus.EXPIRES_TODAY ||
        status == CategoryDetailsExpirationBadgeStatus.EXPIRING_SOON

private data class CardReorderTarget(
    val cardId: String,
    val positionDelta: Offset
)

private fun LazyGridState.findCardReorderTarget(
    draggedCardId: String,
    draggedItemOffset: Offset,
    reorderableCardIds: Set<String>
): CardReorderTarget? {
    val draggedItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { item ->
        item.key == draggedCardId
    } ?: return null

    val draggedCenterX =
        draggedItemInfo.offset.x + draggedItemInfo.size.width / 2f + draggedItemOffset.x
    val draggedCenterY =
        draggedItemInfo.offset.y + draggedItemInfo.size.height / 2f + draggedItemOffset.y

    val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
        val itemKey = item.key as? String ?: return@firstOrNull false
        itemKey != draggedCardId &&
            itemKey in reorderableCardIds &&
            draggedCenterX in item.horizontalBounds() &&
            draggedCenterY in item.verticalBounds()
    } ?: return null

    return CardReorderTarget(
        cardId = targetItem.key as String,
        positionDelta = Offset(
            x = (targetItem.offset.x - draggedItemInfo.offset.x).toFloat(),
            y = (targetItem.offset.y - draggedItemInfo.offset.y).toFloat()
        )
    )
}

private fun LazyGridItemInfo.horizontalBounds(): ClosedFloatingPointRange<Float> =
    offset.x.toFloat()..(offset.x + size.width).toFloat()

private fun LazyGridItemInfo.verticalBounds(): ClosedFloatingPointRange<Float> =
    offset.y.toFloat()..(offset.y + size.height).toFloat()
