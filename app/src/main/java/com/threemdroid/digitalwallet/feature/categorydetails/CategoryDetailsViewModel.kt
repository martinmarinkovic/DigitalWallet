package com.threemdroid.digitalwallet.feature.categorydetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.core.model.displayLabel
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.data.category.DefaultCategories
import com.threemdroid.digitalwallet.data.category.FavoritesCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val cardRepository: CardRepository
) : ViewModel() {
    private val categoryId: String? = savedStateHandle[CategoryDetailsRoutes.categoryIdArg]

    private val mutableUiState = MutableStateFlow(CategoryDetailsUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<CategoryDetailsEffect>()
    val effects = mutableEffects.asSharedFlow()

    private val cardsState = MutableStateFlow<List<WalletCard>?>(null)
    private val reorderState = MutableStateFlow(CategoryDetailsCardReorderState())

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
        }

        val resolvedCategoryId = categoryId
        if (resolvedCategoryId == null) {
            mutableUiState.value = CategoryDetailsUiState(
                isLoading = false,
                isCategoryMissing = true
            )
        } else {
            val observedCards =
                if (FavoritesCategory.isVirtual(resolvedCategoryId)) {
                    cardRepository.observeAllCards().map { cards ->
                        cards.filter { card -> card.isFavorite }
                    }
                } else {
                    cardRepository.observeCards(resolvedCategoryId)
                }

            viewModelScope.launch {
                observedCards.collect { cards ->
                    cardsState.value = cards

                    val persistedIds = cards.map { card -> card.id }
                    reorderState.update { current ->
                        if (!current.isActive && current.orderedCardIds == persistedIds) {
                            CategoryDetailsCardReorderState()
                        } else {
                            current
                        }
                    }
                }
            }

            viewModelScope.launch {
                combine(
                    categoryRepository.observeCategory(resolvedCategoryId),
                    cardsState,
                    reorderState
                ) { category, cards, currentReorderState ->
                    category.toUiState(
                        cards = cards,
                        reorderState = currentReorderState
                    )
                }.collect { state ->
                    mutableUiState.value = state
                }
            }
        }
    }

    fun onEvent(event: CategoryDetailsEvent) {
        when (event) {
            CategoryDetailsEvent.OnAddCardClicked -> {
                val resolvedCategoryId = categoryId ?: return
                viewModelScope.launch {
                    mutableEffects.emit(
                        CategoryDetailsEffect.OpenAddCard(
                            categoryId = resolvedCategoryId.takeUnless(FavoritesCategory::isVirtual)
                        )
                    )
                }
            }

            CategoryDetailsEvent.OnDeleteClicked -> onDeleteClicked()

            CategoryDetailsEvent.OnDeleteDismissed -> {
                mutableUiState.update { current ->
                    current.copy(isDeleteConfirmationVisible = false)
                }
            }

            CategoryDetailsEvent.OnDeleteConfirmed -> deleteCategory()

            CategoryDetailsEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(CategoryDetailsEffect.NavigateBack)
                }
            }

            CategoryDetailsEvent.OnCardReorderCancelled -> {
                reorderState.value = CategoryDetailsCardReorderState()
            }

            CategoryDetailsEvent.OnCardReorderFinished -> {
                persistCardReorder()
            }

            is CategoryDetailsEvent.OnCardReorderMoved -> {
                reorderCards(
                    fromCardId = event.fromCardId,
                    toCardId = event.toCardId
                )
            }

            is CategoryDetailsEvent.OnCardReorderStarted -> {
                startCardReorder(event.cardId)
            }

            is CategoryDetailsEvent.OnCardClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(CategoryDetailsEffect.OpenCardDetails(event.cardId))
                }
            }
        }
    }

    private fun onDeleteClicked() {
        val resolvedCategoryId = categoryId ?: return
        if (uiState.value.isDeleteInProgress) {
            return
        }

        val blockedMessageRes = when {
            FavoritesCategory.isVirtual(resolvedCategoryId) ||
                DefaultCategories.isDefaultCategoryId(resolvedCategoryId) -> {
                R.string.category_details_delete_blocked_protected
            }

            cardsState.value?.isNotEmpty() == true -> {
                R.string.category_details_delete_blocked_not_empty
            }

            cardsState.value == null -> {
                null
            }

            else -> {
                null
            }
        }

        if (blockedMessageRes != null) {
            viewModelScope.launch {
                mutableEffects.emit(CategoryDetailsEffect.ShowDeleteMessage(blockedMessageRes))
            }
            return
        }

        mutableUiState.update { current ->
            current.copy(isDeleteConfirmationVisible = true)
        }
    }

    private fun startCardReorder(cardId: String) {
        val resolvedCategoryId = categoryId ?: return
        if (FavoritesCategory.isVirtual(resolvedCategoryId)) {
            return
        }

        val cards = cardsState.value ?: return
        if (cards.none { card -> card.id == cardId }) {
            return
        }

        reorderState.value = CategoryDetailsCardReorderState(
            isActive = true,
            orderedCardIds = cards.map { card -> card.id }
        )
    }

    private fun reorderCards(
        fromCardId: String,
        toCardId: String
    ) {
        val currentReorderState = reorderState.value
        if (!currentReorderState.isActive) {
            return
        }

        val currentOrder = currentReorderState.orderedCardIds
        if (currentOrder.isEmpty()) {
            return
        }

        val fromIndex = currentOrder.indexOf(fromCardId)
        val toIndex = currentOrder.indexOf(toCardId)
        if (
            fromIndex == -1 ||
                toIndex == -1 ||
                fromIndex == toIndex
        ) {
            return
        }

        val updatedOrder = currentOrder.toMutableList().apply {
            val movedCardId = removeAt(fromIndex)
            add(toIndex, movedCardId)
        }

        reorderState.update { state ->
            state.copy(orderedCardIds = updatedOrder)
        }
    }

    private fun persistCardReorder() {
        val resolvedCategoryId = categoryId ?: run {
            reorderState.value = CategoryDetailsCardReorderState()
            return
        }
        if (FavoritesCategory.isVirtual(resolvedCategoryId)) {
            reorderState.value = CategoryDetailsCardReorderState()
            return
        }

        val persistedOrder = reorderState.value.orderedCardIds
        if (persistedOrder.isEmpty()) {
            reorderState.value = CategoryDetailsCardReorderState()
            return
        }

        val currentPersistedOrder = cardsState.value?.map { card -> card.id } ?: run {
            reorderState.value = CategoryDetailsCardReorderState()
            return
        }
        if (persistedOrder == currentPersistedOrder) {
            reorderState.value = CategoryDetailsCardReorderState()
            return
        }

        viewModelScope.launch {
            reorderState.update { state ->
                state.copy(isActive = false)
            }

            val result = runCatching {
                cardRepository.updateCardOrder(resolvedCategoryId, persistedOrder)
            }
            if (result.isFailure) {
                reorderState.value = CategoryDetailsCardReorderState()
                mutableEffects.emit(CategoryDetailsEffect.ShowCardReorderFailedMessage)
            }
        }
    }

    private fun deleteCategory() {
        val resolvedCategoryId = categoryId ?: return
        if (uiState.value.isDeleteInProgress) {
            return
        }

        mutableUiState.update { current ->
            current.copy(
                isDeleteConfirmationVisible = false,
                isDeleteInProgress = true
            )
        }

        viewModelScope.launch {
            runCatching {
                categoryRepository.deleteCategory(resolvedCategoryId)
            }.onSuccess {
                mutableEffects.emit(CategoryDetailsEffect.NavigateBack)
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(isDeleteInProgress = false)
                }
                mutableEffects.emit(
                    CategoryDetailsEffect.ShowDeleteMessage(
                        R.string.category_details_delete_failed_message
                    )
                )
            }
        }
    }

    private fun Category?.toUiState(
        cards: List<WalletCard>?,
        reorderState: CategoryDetailsCardReorderState
    ): CategoryDetailsUiState {
        if (this == null) {
            return CategoryDetailsUiState(
                isLoading = false,
                isCategoryMissing = true
            )
        }

        if (cards == null) {
            return CategoryDetailsUiState(
                isLoading = true,
                title = name,
                colorHex = color
            )
        }

        val cardsById = cards.associateBy { card -> card.id }
        val orderedCards =
            if (reorderState.orderedCardIds.isEmpty()) {
                cards
            } else {
                reorderState.orderedCardIds.mapNotNull { cardId ->
                    cardsById[cardId]
                } + cards.filterNot { card ->
                    card.id in reorderState.orderedCardIds
                }
            }
        val today = LocalDate.now()
        return CategoryDetailsUiState(
            isLoading = false,
            title = name,
            colorHex = color,
            cards = orderedCards.map { card ->
                card.toUiModel(
                    categoryColorHex = color,
                    today = today
                )
            },
            isCardReordering = reorderState.isActive,
            isCardReorderEnabled = !isFavorites,
            isDeleteConfirmationVisible = uiState.value.isDeleteConfirmationVisible,
            isDeleteInProgress = uiState.value.isDeleteInProgress
        )
    }

    private fun WalletCard.toUiModel(
        categoryColorHex: String,
        today: LocalDate
    ): CategoryDetailsCardUiModel =
        CategoryDetailsCardUiModel(
            id = id,
            name = name,
            placeholderLabel = placeholderLabel(),
            codeTypeLabel = codeType.displayLabel(),
            colorHex = categoryColorHex,
            expirationBadge = expirationBadge(today = today)
        )

    private fun WalletCard.placeholderLabel(): String {
        val initials = name
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .joinToString(separator = "")

        return initials.ifEmpty {
            name.trim().take(2).uppercase()
        }
    }

    private fun WalletCard.expirationBadge(today: LocalDate): CategoryDetailsExpirationBadgeUiModel? {
        val date = expirationDate ?: return null
        val daysUntilExpiration = ChronoUnit.DAYS.between(today, date).toInt()

        return when {
            daysUntilExpiration < 0 -> CategoryDetailsExpirationBadgeUiModel(
                status = CategoryDetailsExpirationBadgeStatus.EXPIRED
            )

            daysUntilExpiration == 0 -> CategoryDetailsExpirationBadgeUiModel(
                status = CategoryDetailsExpirationBadgeStatus.EXPIRES_TODAY
            )

            daysUntilExpiration in 1..7 -> CategoryDetailsExpirationBadgeUiModel(
                status = CategoryDetailsExpirationBadgeStatus.EXPIRING_SOON,
                daysUntilExpiration = daysUntilExpiration
            )

            else -> null
        }
    }

    private data class CategoryDetailsCardReorderState(
        val isActive: Boolean = false,
        val orderedCardIds: List<String> = emptyList()
    )
}
