package com.threemdroid.digitalwallet.feature.home

import android.graphics.Color.parseColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.threemdroid.digitalwallet.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeRoute(
    onOpenCategoryDetails: (String) -> Unit,
    onOpenCreateCategory: () -> Unit,
    onOpenCardDetails: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val categoryReorderFailedMessage =
        stringResource(id = R.string.home_category_reorder_failed_message)

    LaunchedEffect(viewModel, snackbarHostState, categoryReorderFailedMessage) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                HomeEffect.OpenCreateCategory -> onOpenCreateCategory()
                is HomeEffect.OpenCategoryDetails -> onOpenCategoryDetails(effect.categoryId)
                is HomeEffect.OpenCardDetails -> onOpenCardDetails(effect.cardId)
                HomeEffect.ShowCategoryReorderFailedMessage -> {
                    snackbarHostState.showSnackbar(categoryReorderFailedMessage)
                }
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (HomeEvent) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Column {
                AnimatedContent(
                    targetState = uiState.isSearchExpanded,
                    label = "home_search_top_bar"
                ) { isSearchExpanded ->
                    if (isSearchExpanded) {
                        HomeSearchTopBar(
                            query = uiState.searchQuery,
                            onQueryChanged = { query ->
                                onEvent(HomeEvent.OnSearchQueryChanged(query))
                            },
                            onSearchSubmitted = {
                                onEvent(HomeEvent.OnSearchSubmitted)
                            },
                            onClose = {
                                onEvent(HomeEvent.OnSearchClosed)
                            }
                        )
                    } else {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(text = stringResource(id = R.string.home_screen_title_cards))
                            },
                            navigationIcon = {
                                IconButton(onClick = { onEvent(HomeEvent.OnSearchClicked) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = stringResource(id = R.string.home_search_content_description)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { onEvent(HomeEvent.OnAddCategoryClicked) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = stringResource(id = R.string.home_add_category_content_description)
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
                }

                AnimatedVisibility(
                    visible = uiState.isSearchExpanded && uiState.previousSearches.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    PreviousSearchesSection(
                        previousSearches = uiState.previousSearches,
                        onPreviousSearchClicked = { query ->
                            onEvent(HomeEvent.OnPreviousSearchClicked(query))
                        }
                    )
                }
            }
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
        } else if (uiState.isSearchExpanded && uiState.searchQuery.isNotBlank()) {
            SearchResultsContent(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
                onCategoryClicked = { categoryId ->
                    onEvent(HomeEvent.OnCategoryClicked(categoryId))
                },
                onCardClicked = { cardId ->
                    onEvent(HomeEvent.OnCardSearchResultClicked(cardId))
                }
            )
        } else {
            CategoryGridContent(
                categories = uiState.categories,
                isCategoryReordering = uiState.isCategoryReordering,
                modifier = Modifier.padding(innerPadding),
                onCategoryClicked = { categoryId ->
                    onEvent(HomeEvent.OnCategoryClicked(categoryId))
                },
                onNewCategoryClicked = {
                    onEvent(HomeEvent.OnNewCategoryClicked)
                },
                onCategoryReorderStarted = { categoryId ->
                    onEvent(HomeEvent.OnCategoryReorderStarted(categoryId))
                },
                onCategoryReorderMoved = { fromCategoryId, toCategoryId ->
                    onEvent(
                        HomeEvent.OnCategoryReorderMoved(
                            fromCategoryId = fromCategoryId,
                            toCategoryId = toCategoryId
                        )
                    )
                },
                onCategoryReorderFinished = {
                    onEvent(HomeEvent.OnCategoryReorderFinished)
                },
                onCategoryReorderCancelled = {
                    onEvent(HomeEvent.OnCategoryReorderCancelled)
                }
            )
        }
    }
}

@Composable
private fun HomeSearchTopBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                placeholder = {
                    Text(text = stringResource(id = R.string.home_search_hint))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSearchSubmitted()
                    }
                )
            )
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    onClose()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.home_close_search_content_description)
                )
            }
        }
    }
}

@Composable
private fun PreviousSearchesSection(
    previousSearches: List<HomePreviousSearchUiModel>,
    onPreviousSearchClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home_previous_searches_title),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = previousSearches,
                key = { entry -> entry.id }
            ) { entry ->
                PreviousSearchChip(
                    query = entry.query,
                    onClick = {
                        onPreviousSearchClicked(entry.query)
                    }
                )
            }
        }
    }
}

@Composable
private fun PreviousSearchChip(
    query: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = query,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryGridContent(
    categories: List<HomeCategoryTileUiModel>,
    isCategoryReordering: Boolean,
    modifier: Modifier = Modifier,
    onCategoryClicked: (String) -> Unit,
    onNewCategoryClicked: () -> Unit,
    onCategoryReorderStarted: (String) -> Unit,
    onCategoryReorderMoved: (String, String) -> Unit,
    onCategoryReorderFinished: () -> Unit,
    onCategoryReorderCancelled: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val hapticFeedback = LocalHapticFeedback.current
    val currentReorderableIds by rememberUpdatedState(
        newValue = categories.filter { category -> category.isReorderable }.map { category ->
            category.id
        }.toSet()
    )
    val currentOnCategoryReorderStarted by rememberUpdatedState(onCategoryReorderStarted)
    val currentOnCategoryReorderMoved by rememberUpdatedState(onCategoryReorderMoved)
    val currentOnCategoryReorderFinished by rememberUpdatedState(onCategoryReorderFinished)
    val currentOnCategoryReorderCancelled by rememberUpdatedState(onCategoryReorderCancelled)
    var draggedCategoryId by remember { mutableStateOf<String?>(null) }
    var draggedItemOffset by remember { mutableStateOf(Offset.Zero) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = categories,
            key = { category -> category.id }
        ) { category ->
            val isBeingDragged = draggedCategoryId == category.id
            CategoryTile(
                category = category,
                modifier = Modifier
                    .zIndex(if (isBeingDragged) 1f else 0f)
                    .graphicsLayer {
                        if (isBeingDragged) {
                            translationX = draggedItemOffset.x
                            translationY = draggedItemOffset.y
                        }
                    }
                    .pointerInput(category.id) {
                        if (!category.isReorderable) {
                            return@pointerInput
                        }

                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedCategoryId = category.id
                                draggedItemOffset = Offset.Zero
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnCategoryReorderStarted(category.id)
                            },
                            onDragCancel = {
                                draggedCategoryId = null
                                draggedItemOffset = Offset.Zero
                                currentOnCategoryReorderCancelled()
                            },
                            onDragEnd = {
                                draggedCategoryId = null
                                draggedItemOffset = Offset.Zero
                                currentOnCategoryReorderFinished()
                            }
                        ) { change, dragAmount ->
                            if (draggedCategoryId != category.id) {
                                return@detectDragGesturesAfterLongPress
                            }

                            change.consume()
                            draggedItemOffset += dragAmount

                            val reorderTarget = gridState.findCategoryReorderTarget(
                                draggedCategoryId = category.id,
                                draggedItemOffset = draggedItemOffset,
                                reorderableCategoryIds = currentReorderableIds
                            ) ?: return@detectDragGesturesAfterLongPress

                            draggedItemOffset -= reorderTarget.positionDelta
                            currentOnCategoryReorderMoved(
                                category.id,
                                reorderTarget.categoryId
                            )
                        }
                    },
                enabled = !isCategoryReordering,
                isBeingDragged = isBeingDragged,
                onClick = {
                    onCategoryClicked(category.id)
                }
            )
        }

        item(key = "new_category_tile") {
            NewCategoryTile(
                enabled = !isCategoryReordering,
                onClick = onNewCategoryClicked
            )
        }
    }
}

@Composable
private fun SearchResultsContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onCategoryClicked: (String) -> Unit,
    onCardClicked: (String) -> Unit
) {
    val hasResults =
        uiState.searchResultCategories.isNotEmpty() || uiState.searchResultCards.isNotEmpty()

    if (!hasResults) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.home_search_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        id = R.string.home_search_empty_message,
                        uiState.searchQuery.trim()
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.searchResultCategories.isNotEmpty()) {
            item(key = "search_category_header") {
                SearchSectionHeader(
                    title = stringResource(id = R.string.home_search_categories_title)
                )
            }
            items(
                items = uiState.searchResultCategories,
                key = { category -> "category_${category.id}" }
            ) { category ->
                SearchCategoryResultRow(
                    category = category,
                    onClick = {
                        onCategoryClicked(category.id)
                    }
                )
            }
        }

        if (uiState.searchResultCards.isNotEmpty()) {
            item(key = "search_card_header") {
                SearchSectionHeader(
                    title = stringResource(id = R.string.home_search_cards_title)
                )
            }
            items(
                items = uiState.searchResultCards,
                key = { card -> "card_${card.id}" }
            ) { card ->
                SearchCardResultRow(
                    card = card,
                    onClick = {
                        onCardClicked(card.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SearchCategoryResultRow(
    category: HomeCategoryTileUiModel,
    onClick: () -> Unit
) {
    val accentColor = categoryAccentColor(category.colorHex)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = category.name,
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = pluralStringResource(
                    id = R.plurals.home_category_card_count,
                    count = category.cardCount,
                    category.cardCount
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchCardResultRow(
    card: HomeCardSearchResultUiModel,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!card.cardNumber.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        id = R.string.home_search_card_number_label,
                        card.cardNumber
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(
                    id = R.string.home_search_category_label,
                    card.categoryName
                ),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryTile(
    category: HomeCategoryTileUiModel,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isBeingDragged: Boolean,
    onClick: () -> Unit
) {
    val accentColor = categoryAccentColor(category.colorHex)
    val tileShape = RoundedCornerShape(28.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(tileShape)
            .background(
                color = accentColor.copy(alpha = if (isBeingDragged) 0.9f else 1f),
                shape = tileShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.name,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = categoryTileTextColor(accentColor),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NewCategoryTile(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tileShape = RoundedCornerShape(28.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(tileShape)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = tileShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            /*Text(
                text = stringResource(id = R.string.home_new_category_tile_title),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )*/
        }
    }
}

@Composable
private fun categoryAccentColor(colorHex: String): Color =
    runCatching {
        Color(parseColor(colorHex))
    }.getOrElse {
        MaterialTheme.colorScheme.primary
    }

private fun categoryTileTextColor(backgroundColor: Color): Color =
    if (backgroundColor.luminance() > 0.42f) {
        Color(0xFF101010)
    } else {
        Color.White
    }

private data class CategoryReorderTarget(
    val categoryId: String,
    val positionDelta: Offset
)

private fun LazyGridState.findCategoryReorderTarget(
    draggedCategoryId: String,
    draggedItemOffset: Offset,
    reorderableCategoryIds: Set<String>
): CategoryReorderTarget? {
    val draggedItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { item ->
        item.key == draggedCategoryId
    } ?: return null

    val draggedCenterX =
        draggedItemInfo.offset.x + draggedItemInfo.size.width / 2f + draggedItemOffset.x
    val draggedCenterY =
        draggedItemInfo.offset.y + draggedItemInfo.size.height / 2f + draggedItemOffset.y

    val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
        val itemKey = item.key as? String ?: return@firstOrNull false
        itemKey != draggedCategoryId &&
            itemKey in reorderableCategoryIds &&
            draggedCenterX in item.horizontalBounds() &&
            draggedCenterY in item.verticalBounds()
    } ?: return null

    return CategoryReorderTarget(
        categoryId = targetItem.key as String,
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
