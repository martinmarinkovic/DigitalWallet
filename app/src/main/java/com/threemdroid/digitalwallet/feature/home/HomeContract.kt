package com.threemdroid.digitalwallet.feature.home

data class HomeUiState(
    val isLoading: Boolean = true,
    val categories: List<HomeCategoryTileUiModel> = emptyList(),
    val isCategoryReordering: Boolean = false,
    val isSearchExpanded: Boolean = false,
    val searchQuery: String = "",
    val previousSearches: List<HomePreviousSearchUiModel> = emptyList(),
    val searchResultCategories: List<HomeCategoryTileUiModel> = emptyList(),
    val searchResultCards: List<HomeCardSearchResultUiModel> = emptyList()
)

data class HomeCategoryTileUiModel(
    val id: String,
    val name: String,
    val colorHex: String,
    val cardCount: Int,
    val isReorderable: Boolean
)

data class HomePreviousSearchUiModel(
    val id: Long,
    val query: String
)

data class HomeCardSearchResultUiModel(
    val id: String,
    val name: String,
    val cardNumber: String?,
    val categoryId: String,
    val categoryName: String
)

sealed interface HomeEvent {
    data object OnSearchClicked : HomeEvent

    data object OnSearchClosed : HomeEvent

    data class OnSearchQueryChanged(val query: String) : HomeEvent

    data object OnSearchSubmitted : HomeEvent

    data class OnPreviousSearchClicked(val query: String) : HomeEvent

    data object OnAddCategoryClicked : HomeEvent

    data object OnNewCategoryClicked : HomeEvent

    data class OnCategoryClicked(val categoryId: String) : HomeEvent

    data class OnCardSearchResultClicked(val cardId: String) : HomeEvent

    data class OnCategoryReorderStarted(val categoryId: String) : HomeEvent

    data class OnCategoryReorderMoved(
        val fromCategoryId: String,
        val toCategoryId: String
    ) : HomeEvent

    data object OnCategoryReorderFinished : HomeEvent

    data object OnCategoryReorderCancelled : HomeEvent
}

sealed interface HomeEffect {
    data object OpenCreateCategory : HomeEffect

    data class OpenCategoryDetails(val categoryId: String) : HomeEffect

    data class OpenCardDetails(val cardId: String) : HomeEffect

    data object ShowCategoryReorderFailedMessage : HomeEffect
}
