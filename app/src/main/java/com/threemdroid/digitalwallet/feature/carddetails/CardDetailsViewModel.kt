package com.threemdroid.digitalwallet.feature.carddetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.core.model.displayLabel
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
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
class CardDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val cardRepository: CardRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val cardId: String? = savedStateHandle[CardDetailsRoutes.cardIdArg]

    private val mutableUiState = MutableStateFlow(CardDetailsUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<CardDetailsEffect>()
    val effects = mutableEffects.asSharedFlow()

    private var currentCard: WalletCard? = null

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
        }

        val resolvedCardId = cardId
        if (resolvedCardId == null) {
            mutableUiState.value = CardDetailsUiState(
                isLoading = false,
                isCardMissing = true
            )
        } else {
            viewModelScope.launch {
                combine(
                    cardRepository.observeCard(resolvedCardId),
                    categoryRepository.observeCategories(),
                    settingsRepository.observeSettings()
                ) { card, categories, settings ->
                    val category = card?.let { cardValue ->
                        categories.firstOrNull { categoryValue ->
                            categoryValue.id == cardValue.categoryId
                        }
                    }
                    Triple(card, category, settings.autoBrightnessEnabled)
                }.collect { (card, category, shouldMaximizeBrightness) ->
                    currentCard = card
                    mutableUiState.update { current ->
                        card.toUiState(
                            category = category,
                            currentState = current,
                            shouldMaximizeBrightness = shouldMaximizeBrightness
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: CardDetailsEvent) {
        when (event) {
            CardDetailsEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(CardDetailsEffect.NavigateBack)
                }
            }

            CardDetailsEvent.OnDeleteClicked -> {
                if (currentCard == null) {
                    return
                }
                mutableUiState.update { current ->
                    current.copy(
                        isDeleteConfirmationVisible = true,
                        isActionErrorVisible = false
                    )
                }
            }

            CardDetailsEvent.OnDeleteDismissed -> {
                mutableUiState.update { current ->
                    current.copy(isDeleteConfirmationVisible = false)
                }
            }

            CardDetailsEvent.OnDeleteConfirmed -> deleteCard()

            CardDetailsEvent.OnEditClicked -> {
                val card = currentCard ?: return
                viewModelScope.launch {
                    mutableEffects.emit(CardDetailsEffect.OpenEdit(card.id))
                }
            }

            CardDetailsEvent.OnFavoriteClicked -> toggleFavorite()

            CardDetailsEvent.OnShareClicked -> {
                val card = currentCard ?: return
                viewModelScope.launch {
                    mutableEffects.emit(
                        CardDetailsEffect.OpenShareSheet(
                            title = card.name,
                            shareText = buildString {
                                appendLine(card.name)
                                appendLine("Code type: ${card.codeType.displayLabel()}")
                                append("Code value: ${card.codeValue}")
                            }
                        )
                    )
                }
            }

            CardDetailsEvent.OnOpenFullscreenCodeClicked -> {
                val card = currentCard ?: return
                viewModelScope.launch {
                    mutableEffects.emit(CardDetailsEffect.OpenFullscreenCode(card.id))
                }
            }
        }
    }

    private fun toggleFavorite() {
        val card = currentCard ?: return
        if (uiState.value.isDeleteInProgress || uiState.value.isFavoriteUpdating) {
            return
        }

        mutableUiState.update { current ->
            current.copy(
                isFavoriteUpdating = true,
                isActionErrorVisible = false
            )
        }

        viewModelScope.launch {
            runCatching {
                cardRepository.upsertCard(
                    card.copy(
                        isFavorite = !card.isFavorite,
                        updatedAt = Instant.now()
                    )
                )
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(
                        isFavoriteUpdating = false,
                        isActionErrorVisible = true
                    )
                }
            }
        }
    }

    private fun deleteCard() {
        val card = currentCard ?: return
        if (uiState.value.isDeleteInProgress) {
            return
        }

        mutableUiState.update { current ->
            current.copy(
                isDeleteConfirmationVisible = false,
                isDeleteInProgress = true,
                isActionErrorVisible = false
            )
        }

        viewModelScope.launch {
            runCatching {
                cardRepository.deleteCard(card.id)
            }.onSuccess {
                mutableEffects.emit(CardDetailsEffect.NavigateBack)
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(
                        isDeleteInProgress = false,
                        isActionErrorVisible = true
                    )
                }
            }
        }
    }

    private fun WalletCard?.toUiState(
        category: Category?,
        currentState: CardDetailsUiState,
        shouldMaximizeBrightness: Boolean
    ): CardDetailsUiState {
        if (this == null) {
            return currentState.copy(
                isLoading = false,
                isCardMissing = true,
                title = "",
                categoryName = "",
                categoryColorHex = "",
                codeType = com.threemdroid.digitalwallet.core.model.CardCodeType.QR_CODE,
                codeTypeLabel = "",
                codeValue = "",
                cardNumber = null,
                expirationDate = null,
                notes = null,
                shouldMaximizeBrightness = shouldMaximizeBrightness,
                isFavorite = false,
                isDeleteConfirmationVisible = false,
                isFavoriteUpdating = false,
                isDeleteInProgress = false
            )
        }

        return currentState.copy(
            isLoading = false,
            isCardMissing = false,
            title = name,
            categoryName = category?.name.orEmpty(),
            categoryColorHex = category?.color.orEmpty(),
            codeType = codeType,
            codeTypeLabel = codeType.displayLabel(),
            codeValue = codeValue,
            cardNumber = cardNumber?.trim()?.takeIf { it.isNotBlank() },
            expirationDate = expirationDate?.toString(),
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            shouldMaximizeBrightness = shouldMaximizeBrightness,
            isFavorite = isFavorite,
            isDeleteConfirmationVisible = currentState.isDeleteConfirmationVisible,
            isFavoriteUpdating = false,
            isDeleteInProgress = currentState.isDeleteInProgress,
            isActionErrorVisible = false
        )
    }
}
