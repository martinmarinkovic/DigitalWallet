package com.threemdroid.digitalwallet.feature.categorydetails

data class CategoryDetailsUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val colorHex: String = "",
    val cards: List<CategoryDetailsCardUiModel> = emptyList(),
    val isCategoryMissing: Boolean = false,
    val isCardReordering: Boolean = false,
    val isCardReorderEnabled: Boolean = true
) {
    val isEmpty: Boolean
        get() = !isLoading && !isCategoryMissing && cards.isEmpty()
}

data class CategoryDetailsCardUiModel(
    val id: String,
    val name: String,
    val placeholderLabel: String,
    val codeTypeLabel: String,
    val colorHex: String,
    val expirationBadge: CategoryDetailsExpirationBadgeUiModel? = null
)

data class CategoryDetailsExpirationBadgeUiModel(
    val status: CategoryDetailsExpirationBadgeStatus,
    val daysUntilExpiration: Int? = null
)

enum class CategoryDetailsExpirationBadgeStatus {
    EXPIRING_SOON,
    EXPIRES_TODAY,
    EXPIRED
}

sealed interface CategoryDetailsEvent {
    data object OnBackClicked : CategoryDetailsEvent

    data object OnAddCardClicked : CategoryDetailsEvent

    data class OnCardClicked(val cardId: String) : CategoryDetailsEvent

    data class OnCardReorderStarted(val cardId: String) : CategoryDetailsEvent

    data class OnCardReorderMoved(
        val fromCardId: String,
        val toCardId: String
    ) : CategoryDetailsEvent

    data object OnCardReorderFinished : CategoryDetailsEvent

    data object OnCardReorderCancelled : CategoryDetailsEvent
}

sealed interface CategoryDetailsEffect {
    data object NavigateBack : CategoryDetailsEffect

    data class OpenAddCard(val categoryId: String?) : CategoryDetailsEffect

    data class OpenCardDetails(val cardId: String) : CategoryDetailsEffect

    data object ShowCardReorderFailedMessage : CategoryDetailsEffect
}
