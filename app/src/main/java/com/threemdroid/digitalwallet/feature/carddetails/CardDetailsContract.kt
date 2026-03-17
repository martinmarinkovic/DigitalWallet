package com.threemdroid.digitalwallet.feature.carddetails

data class CardDetailsUiState(
    val isLoading: Boolean = true,
    val isCardMissing: Boolean = false,
    val title: String = "",
    val categoryName: String = "",
    val categoryColorHex: String = "",
    val codeTypeLabel: String = "",
    val codeValue: String = "",
    val cardNumber: String? = null,
    val expirationDate: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
    val isDeleteInProgress: Boolean = false,
    val isActionErrorVisible: Boolean = false
) {
    val isContentVisible: Boolean
        get() = !isLoading && !isCardMissing
}

sealed interface CardDetailsEvent {
    data object OnBackClicked : CardDetailsEvent

    data object OnEditClicked : CardDetailsEvent

    data object OnDeleteClicked : CardDetailsEvent

    data object OnDeleteDismissed : CardDetailsEvent

    data object OnDeleteConfirmed : CardDetailsEvent

    data object OnFavoriteClicked : CardDetailsEvent

    data object OnOpenFullscreenCodeClicked : CardDetailsEvent
}

sealed interface CardDetailsEffect {
    data object NavigateBack : CardDetailsEffect

    data class OpenEdit(val cardId: String) : CardDetailsEffect

    data class OpenFullscreenCode(val cardId: String) : CardDetailsEffect
}
