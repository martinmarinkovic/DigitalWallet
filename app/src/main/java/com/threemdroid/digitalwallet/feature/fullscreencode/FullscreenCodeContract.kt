package com.threemdroid.digitalwallet.feature.fullscreencode

import com.threemdroid.digitalwallet.core.model.CardCodeType

data class FullscreenCodeUiState(
    val isLoading: Boolean = true,
    val isCardMissing: Boolean = false,
    val cardName: String = "",
    val cardNumber: String? = null,
    val codeValue: String = "",
    val codeType: CardCodeType = CardCodeType.QR_CODE,
    val codeTypeLabel: String = "",
    val presentation: FullscreenCodePresentation = FullscreenCodePresentation.MATRIX,
    val shouldMaximizeBrightness: Boolean = true
) {
    val isContentVisible: Boolean
        get() = !isLoading && !isCardMissing
}

enum class FullscreenCodePresentation {
    MATRIX,
    LINEAR
}

sealed interface FullscreenCodeEvent {
    data object OnBackClicked : FullscreenCodeEvent
}

sealed interface FullscreenCodeEffect {
    data object NavigateBack : FullscreenCodeEffect
}
