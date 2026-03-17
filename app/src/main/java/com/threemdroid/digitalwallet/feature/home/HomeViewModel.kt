package com.threemdroid.digitalwallet.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.data.searchhistory.SearchHistoryRepository
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
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val cardRepository: CardRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<HomeEffect>()
    val effects = mutableEffects.asSharedFlow()

    private val categoriesState = MutableStateFlow<List<CategoryWithCardCount>>(emptyList())
    private val searchState = MutableStateFlow(HomeSearchState())
    private val reorderState = MutableStateFlow(HomeCategoryReorderState())

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
        }

        viewModelScope.launch {
            categoryRepository.observeCategoriesWithCardCounts().collect { categories ->
                categoriesState.value = categories

                val persistedIds = categories.map { category ->
                    category.category.id
                }
                reorderState.update { current ->
                    if (!current.isActive && current.orderedCategoryIds == persistedIds) {
                        HomeCategoryReorderState()
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                categoriesState,
                cardRepository.observeAllCards(),
                searchHistoryRepository.observeSearchHistory(),
                searchState,
                reorderState
            ) { categories, cards, history, currentSearchState, currentReorderState ->
                categories.toHomeUiState(
                    cards = cards,
                    searchHistory = history,
                    searchState = currentSearchState,
                    reorderState = currentReorderState
                )
            }.collect { state ->
                mutableUiState.value = state
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnAddCategoryClicked,
            HomeEvent.OnNewCategoryClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(HomeEffect.OpenCreateCategory)
                }
            }

            is HomeEvent.OnCardSearchResultClicked -> {
                viewModelScope.launch {
                    persistSearchQueryIfNeeded()
                    mutableEffects.emit(HomeEffect.OpenCardDetails(event.cardId))
                }
            }

            is HomeEvent.OnCategoryClicked -> {
                viewModelScope.launch {
                    persistSearchQueryIfNeeded()
                    mutableEffects.emit(HomeEffect.OpenCategoryDetails(event.categoryId))
                }
            }

            is HomeEvent.OnCategoryReorderMoved -> {
                reorderCategories(
                    fromCategoryId = event.fromCategoryId,
                    toCategoryId = event.toCategoryId
                )
            }

            is HomeEvent.OnCategoryReorderStarted -> {
                startCategoryReorder(event.categoryId)
            }

            HomeEvent.OnCategoryReorderCancelled -> {
                reorderState.value = HomeCategoryReorderState()
            }

            HomeEvent.OnCategoryReorderFinished -> {
                persistCategoryReorder()
            }

            HomeEvent.OnSearchClicked -> {
                searchState.update { current ->
                    current.copy(isExpanded = true)
                }
            }

            HomeEvent.OnSearchClosed -> {
                searchState.value = HomeSearchState()
            }

            is HomeEvent.OnSearchQueryChanged -> {
                searchState.update { current ->
                    current.copy(query = event.query)
                }
            }

            HomeEvent.OnSearchSubmitted -> {
                viewModelScope.launch {
                    val normalizedQuery = searchState.value.query.trim()
                    searchState.update { current ->
                        current.copy(query = normalizedQuery)
                    }
                    searchHistoryRepository.saveQuery(normalizedQuery)
                }
            }

            is HomeEvent.OnPreviousSearchClicked -> {
                val query = event.query.trim()
                searchState.update { current ->
                    current.copy(
                        isExpanded = true,
                        query = query
                    )
                }
                viewModelScope.launch {
                    searchHistoryRepository.saveQuery(query)
                }
            }
        }
    }

    private suspend fun persistSearchQueryIfNeeded() {
        val normalizedQuery = searchState.value.query.trim()
        if (searchState.value.isExpanded && normalizedQuery.isNotBlank()) {
            searchHistoryRepository.saveQuery(normalizedQuery)
        }
    }

    private fun startCategoryReorder(categoryId: String) {
        val categories = categoriesState.value
        val category = categories.firstOrNull { item ->
            item.category.id == categoryId
        } ?: return

        if (category.category.isFavorites) {
            return
        }

        reorderState.value = HomeCategoryReorderState(
            isActive = true,
            orderedCategoryIds = categories.map { item -> item.category.id }
        )
    }

    private fun reorderCategories(
        fromCategoryId: String,
        toCategoryId: String
    ) {
        val currentReorderState = reorderState.value
        if (!currentReorderState.isActive) {
            return
        }

        val currentOrder = currentReorderState.orderedCategoryIds
        if (currentOrder.isEmpty()) {
            return
        }

        val favoritesCategoryId = categoriesState.value.firstOrNull { category ->
            category.category.isFavorites
        }?.category?.id

        val fromIndex = currentOrder.indexOf(fromCategoryId)
        val toIndex = currentOrder.indexOf(toCategoryId)
        if (
            fromIndex <= 0 ||
                toIndex <= 0 ||
                fromIndex == -1 ||
                toIndex == -1 ||
                fromIndex == toIndex ||
                fromCategoryId == favoritesCategoryId ||
                toCategoryId == favoritesCategoryId
        ) {
            return
        }

        val updatedOrder = currentOrder.toMutableList().apply {
            val movedCategoryId = removeAt(fromIndex)
            add(toIndex, movedCategoryId)
        }

        reorderState.update { state ->
            state.copy(orderedCategoryIds = updatedOrder)
        }
    }

    private fun persistCategoryReorder() {
        val persistedOrder = reorderState.value.orderedCategoryIds
        if (persistedOrder.isEmpty()) {
            reorderState.value = HomeCategoryReorderState()
            return
        }

        val currentPersistedOrder = categoriesState.value.map { category ->
            category.category.id
        }
        if (persistedOrder == currentPersistedOrder) {
            reorderState.value = HomeCategoryReorderState()
            return
        }

        viewModelScope.launch {
            reorderState.update { state ->
                state.copy(isActive = false)
            }

            val result = runCatching {
                categoryRepository.updateCategoryOrder(persistedOrder)
            }
            if (result.isFailure) {
                reorderState.value = HomeCategoryReorderState()
                mutableEffects.emit(HomeEffect.ShowCategoryReorderFailedMessage)
            }
        }
    }

    private fun List<CategoryWithCardCount>.toHomeUiState(
        cards: List<WalletCard>,
        searchHistory: List<SearchHistoryEntry>,
        searchState: HomeSearchState,
        reorderState: HomeCategoryReorderState
    ): HomeUiState {
        val categoriesById = associateBy { category -> category.category.id }
        val orderedCategories =
            if (reorderState.orderedCategoryIds.isEmpty()) {
                this
            } else {
                reorderState.orderedCategoryIds.mapNotNull { categoryId ->
                    categoriesById[categoryId]
                } + filterNot { category ->
                    category.category.id in reorderState.orderedCategoryIds
                }
            }

        val categoryTiles = orderedCategories.map { category ->
            HomeCategoryTileUiModel(
                id = category.category.id,
                name = category.category.name,
                colorHex = category.category.color,
                cardCount = category.cardCount,
                isReorderable = !category.category.isFavorites
            )
        }

        val normalizedQuery = searchState.query.trim()
        val categoryNameById = orderedCategories.associate { category ->
            category.category.id to category.category.name
        }
        val categoryPositionById = orderedCategories.mapIndexed { index, category ->
            category.category.id to index
        }.toMap()

        val categoryResults =
            if (normalizedQuery.isBlank()) {
                emptyList()
            } else {
                orderedCategories.filter { category ->
                    category.category.name.contains(normalizedQuery, ignoreCase = true)
                }.map { category ->
                    HomeCategoryTileUiModel(
                        id = category.category.id,
                        name = category.category.name,
                        colorHex = category.category.color,
                        cardCount = category.cardCount,
                        isReorderable = !category.category.isFavorites
                    )
                }
            }

        val cardResults =
            if (normalizedQuery.isBlank()) {
                emptyList()
            } else {
                cards.filter { card ->
                    card.name.contains(normalizedQuery, ignoreCase = true) ||
                        (card.cardNumber?.contains(normalizedQuery, ignoreCase = true) == true)
                }.sortedWith(
                    compareBy<WalletCard>(
                        { categoryPositionById[it.categoryId] ?: Int.MAX_VALUE },
                        { it.position },
                        { it.createdAt }
                    )
                ).map { card ->
                    HomeCardSearchResultUiModel(
                        id = card.id,
                        name = card.name,
                        cardNumber = card.cardNumber,
                        categoryId = card.categoryId,
                        categoryName = categoryNameById[card.categoryId].orEmpty()
                    )
                }
            }

        return HomeUiState(
            isLoading = false,
            categories = categoryTiles,
            isCategoryReordering = reorderState.isActive,
            isSearchExpanded = searchState.isExpanded,
            searchQuery = searchState.query,
            previousSearches = searchHistory.map { entry ->
                HomePreviousSearchUiModel(
                    id = entry.id,
                    query = entry.query
                )
            },
            searchResultCategories = categoryResults,
            searchResultCards = cardResults
        )
    }

    private data class HomeSearchState(
        val isExpanded: Boolean = false,
        val query: String = ""
    )

    private data class HomeCategoryReorderState(
        val isActive: Boolean = false,
        val orderedCategoryIds: List<String> = emptyList()
    )
}
