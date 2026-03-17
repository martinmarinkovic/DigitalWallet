package com.threemdroid.digitalwallet.feature.addcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val cardRepository: CardRepository
) : ViewModel() {
    private val initialCategoryId: String? = savedStateHandle[AddCardRoutes.categoryIdArg]
    private val editingCardId: String? = savedStateHandle[ManualEntryRoutes.cardIdArg]
    private val entrySource = if (editingCardId != null) {
        ManualEntrySource.EDIT
    } else {
        savedStateHandle.get<String>(ManualEntryRoutes.sourceArg)
            .toManualEntrySource()
    }
    private val initialCodeType = savedStateHandle.get<String>(ManualEntryRoutes.codeTypeArg)
        .toCardCodeType()
    private val initialCodeValue: String = savedStateHandle.get<String>(ManualEntryRoutes.codeValueArg)
        .orEmpty()
    private val initialCardNumber: String = savedStateHandle.get<String>(ManualEntryRoutes.cardNumberArg)
        .orEmpty()
    private val initialCardName: String = savedStateHandle.get<String>(ManualEntryRoutes.cardNameArg)
        .orEmpty()
    private val initialNotes: String = savedStateHandle.get<String>(ManualEntryRoutes.notesArg)
        .orEmpty()

    private val mutableUiState = MutableStateFlow(
        ManualEntryUiState(
            titleRes = entrySource.titleRes,
            saveButtonRes = entrySource.saveButtonRes,
            reviewMessageRes = entrySource.reviewMessageRes,
            cardName = initialCardName,
            selectedCodeType = initialCodeType,
            codeValue = initialCodeValue,
            cardNumber = initialCardNumber,
            notes = initialNotes
        )
    )
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<ManualEntryEffect>()
    val effects = mutableEffects.asSharedFlow()

    private var hasAppliedInitialCategory = false
    private var hasAppliedInitialEditCard = false
    private var currentEditingCard: WalletCard? = null

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
        }

        if (editingCardId == null) {
            observeCreateState()
        } else {
            observeEditState(editingCardId)
        }
    }

    fun onEvent(event: ManualEntryEvent) {
        when (event) {
            ManualEntryEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(ManualEntryEffect.NavigateBack)
                }
            }

            is ManualEntryEvent.OnCardNameChanged -> {
                mutableUiState.update { current ->
                    current.copy(
                        cardName = event.value,
                        cardNameError = null,
                        isSaveErrorVisible = false
                    )
                }
            }

            is ManualEntryEvent.OnCategorySelected -> {
                mutableUiState.update { current ->
                    current.copy(
                        selectedCategoryId = event.categoryId,
                        categoryError = null,
                        isSaveErrorVisible = false
                    )
                }
            }

            is ManualEntryEvent.OnCodeTypeSelected -> {
                mutableUiState.update { current ->
                    current.copy(
                        selectedCodeType = event.codeType,
                        isSaveErrorVisible = false
                    )
                }
            }

            is ManualEntryEvent.OnCodeValueChanged -> {
                mutableUiState.update { current ->
                    current.copy(
                        codeValue = event.value,
                        codeValueError = null,
                        isSaveErrorVisible = false
                    )
                }
            }

            is ManualEntryEvent.OnCardNumberChanged -> {
                mutableUiState.update { current ->
                    current.copy(
                        cardNumber = event.value,
                        isSaveErrorVisible = false
                    )
                }
            }

            is ManualEntryEvent.OnExpirationDateChanged -> {
                mutableUiState.update { current ->
                    current.copy(
                        expirationDateInput = event.value,
                        expirationDateError = event.value.invalidExpirationDateError(),
                        isSaveErrorVisible = false
                    )
                }
            }

            is ManualEntryEvent.OnNotesChanged -> {
                mutableUiState.update { current ->
                    current.copy(
                        notes = event.value,
                        isSaveErrorVisible = false
                    )
                }
            }

            is ManualEntryEvent.OnFavoriteChanged -> {
                mutableUiState.update { current ->
                    current.copy(
                        isFavorite = event.value,
                        isSaveErrorVisible = false
                    )
                }
            }

            ManualEntryEvent.OnSaveClicked -> saveCard()
        }
    }

    private fun saveCard() {
        val snapshot = uiState.value
        val editingCard = currentEditingCard
        val trimmedCardName = snapshot.cardName.trim()
        val selectedCategoryId = snapshot.selectedCategoryId?.takeIf { categoryId ->
            snapshot.availableCategories.any { category -> category.id == categoryId }
        }
        val trimmedCodeValue = snapshot.codeValue.trim()
        val expirationDate = snapshot.expirationDateInput.parseExpirationDate()

        val cardNameError = if (trimmedCardName.isBlank()) {
            ManualEntryFieldError.REQUIRED
        } else {
            null
        }
        val categoryError = if (selectedCategoryId == null) {
            ManualEntryFieldError.REQUIRED
        } else {
            null
        }
        val codeValueError = if (trimmedCodeValue.isBlank()) {
            ManualEntryFieldError.REQUIRED
        } else {
            null
        }
        val expirationDateError = when {
            snapshot.expirationDateInput.isBlank() -> null
            expirationDate == null -> ManualEntryExpirationDateError.INVALID_FORMAT
            else -> null
        }

        mutableUiState.update { current ->
            current.copy(
                cardNameError = cardNameError,
                categoryError = categoryError,
                codeValueError = codeValueError,
                expirationDateError = expirationDateError,
                isSaveErrorVisible = false
            )
        }

        if (
            cardNameError != null ||
                categoryError != null ||
                codeValueError != null ||
                expirationDateError != null ||
                selectedCategoryId == null ||
                expirationDate == null && snapshot.expirationDateInput.isNotBlank()
        ) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { current ->
                current.copy(isSaving = true)
            }

            runCatching {
                val timestamp = Instant.now()
                if (editingCard == null) {
                    val existingCards = cardRepository.observeCards(selectedCategoryId).first()
                    val nextPosition = (existingCards.maxOfOrNull { card -> card.position } ?: -1) + 1
                    val createdCard = WalletCard(
                        id = UUID.randomUUID().toString(),
                        name = trimmedCardName,
                        categoryId = selectedCategoryId,
                        codeValue = trimmedCodeValue,
                        codeType = snapshot.selectedCodeType,
                        cardNumber = snapshot.cardNumber.trim().ifBlank { null },
                        expirationDate = expirationDate,
                        notes = snapshot.notes.trim().ifBlank { null },
                        isFavorite = snapshot.isFavorite,
                        position = nextPosition,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                    cardRepository.upsertCard(createdCard)
                    createdCard
                } else {
                    val nextPosition = if (selectedCategoryId == editingCard.categoryId) {
                        editingCard.position
                    } else {
                        val destinationCards = cardRepository.observeCards(selectedCategoryId).first()
                        (destinationCards.maxOfOrNull { card -> card.position } ?: -1) + 1
                    }
                    val updatedCard = editingCard.copy(
                        name = trimmedCardName,
                        categoryId = selectedCategoryId,
                        codeValue = trimmedCodeValue,
                        codeType = snapshot.selectedCodeType,
                        cardNumber = snapshot.cardNumber.trim().ifBlank { null },
                        expirationDate = expirationDate,
                        notes = snapshot.notes.trim().ifBlank { null },
                        isFavorite = snapshot.isFavorite,
                        position = nextPosition,
                        updatedAt = timestamp
                    )
                    cardRepository.upsertCard(updatedCard)
                    if (selectedCategoryId != editingCard.categoryId) {
                        val remainingSourceCardIds = cardRepository.observeCards(editingCard.categoryId)
                            .first()
                            .map { card -> card.id }
                        cardRepository.updateCardOrder(
                            categoryId = editingCard.categoryId,
                            cardIdsInOrder = remainingSourceCardIds
                        )
                    }
                    updatedCard
                }
            }.onSuccess { card ->
                if (editingCard == null) {
                    mutableEffects.emit(ManualEntryEffect.CardSaved(card.categoryId))
                } else {
                    mutableEffects.emit(ManualEntryEffect.NavigateBack)
                }
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(
                        isSaving = false,
                        isSaveErrorVisible = true
                    )
                }
            }
        }
    }

    private fun Category.toOption(): ManualEntryCategoryOptionUiModel =
        ManualEntryCategoryOptionUiModel(
            id = id,
            name = name
        )

    private fun observeCreateState() {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                mutableUiState.update { current ->
                    val availableCategories = categories.map { category ->
                        category.toOption()
                    }
                    val currentSelection = current.selectedCategoryId
                        ?.takeIf { selectedCategoryId ->
                            categories.any { category -> category.id == selectedCategoryId }
                        }
                    val initialSelection = when {
                        hasAppliedInitialCategory -> null
                        initialCategoryId.isNullOrBlank() -> {
                            hasAppliedInitialCategory = true
                            null
                        }
                        categories.any { category -> category.id == initialCategoryId } -> {
                            hasAppliedInitialCategory = true
                            initialCategoryId
                        }
                        categories.isNotEmpty() -> {
                            hasAppliedInitialCategory = true
                            null
                        }
                        else -> null
                    }

                    current.copy(
                        isLoading = false,
                        isCardMissing = false,
                        availableCategories = availableCategories,
                        selectedCategoryId = currentSelection ?: initialSelection
                    )
                }
            }
        }
    }

    private fun observeEditState(cardId: String) {
        viewModelScope.launch {
            combine(
                categoryRepository.observeCategories(),
                cardRepository.observeCard(cardId)
            ) { categories, card ->
                categories to card
            }.collect { (categories, card) ->
                currentEditingCard = card
                mutableUiState.update { current ->
                    val availableCategories = categories.map { category ->
                        category.toOption()
                    }
                    if (card == null) {
                        return@update current.copy(
                            isLoading = false,
                            isCardMissing = true,
                            availableCategories = availableCategories,
                            selectedCategoryId = current.selectedCategoryId
                                ?.takeIf { selectedCategoryId ->
                                    categories.any { category -> category.id == selectedCategoryId }
                                }
                        )
                    }

                    if (!hasAppliedInitialEditCard) {
                        hasAppliedInitialEditCard = true
                        return@update current.copy(
                            isLoading = false,
                            isCardMissing = false,
                            availableCategories = availableCategories,
                            cardName = card.name,
                            selectedCategoryId = card.categoryId.takeIf { categoryId ->
                                categories.any { category -> category.id == categoryId }
                            },
                            selectedCodeType = card.codeType,
                            codeValue = card.codeValue,
                            cardNumber = card.cardNumber.orEmpty(),
                            expirationDateInput = card.expirationDate?.toString().orEmpty(),
                            notes = card.notes.orEmpty(),
                            isFavorite = card.isFavorite,
                            cardNameError = null,
                            categoryError = null,
                            codeValueError = null,
                            expirationDateError = null,
                            isSaveErrorVisible = false
                        )
                    }

                    current.copy(
                        isLoading = false,
                        isCardMissing = false,
                        availableCategories = availableCategories,
                        selectedCategoryId = current.selectedCategoryId
                            ?.takeIf { selectedCategoryId ->
                                categories.any { category -> category.id == selectedCategoryId }
                            }
                    )
                }
            }
        }
    }

    private fun String.parseExpirationDate(): LocalDate? =
        trim()
            .takeIf { value -> value.isNotBlank() }
            ?.let { value ->
                runCatching {
                    LocalDate.parse(value)
                }.getOrNull()
            }

    private fun String.invalidExpirationDateError(): ManualEntryExpirationDateError? =
        when {
            isBlank() -> null
            parseExpirationDate() == null -> ManualEntryExpirationDateError.INVALID_FORMAT
            else -> null
        }

    private fun String?.toManualEntrySource(): ManualEntrySource =
        entriesOrDefault(ManualEntrySource.MANUAL) { value ->
            ManualEntrySource.valueOf(value)
        }

    private fun String?.toCardCodeType(): CardCodeType =
        entriesOrDefault(CardCodeType.QR_CODE) { value ->
            CardCodeType.valueOf(value)
        }

    private inline fun <T> String?.entriesOrDefault(
        defaultValue: T,
        parse: (String) -> T
    ): T =
        takeIf { !it.isNullOrBlank() }
            ?.let { value ->
                runCatching {
                    parse(value)
                }.getOrNull()
            }
            ?: defaultValue
}
