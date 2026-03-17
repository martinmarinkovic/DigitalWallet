package com.threemdroid.digitalwallet.feature.addcard

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualEntryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_withCategoryContext_preselectsCategory() = runTest {
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_membership")
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("default_membership", state.selectedCategoryId)
        assertEquals(com.threemdroid.digitalwallet.R.string.manual_entry_title, state.titleRes)
        assertEquals(
            "Membership",
            state.availableCategories.first { category ->
                category.id == "default_membership"
            }.name
        )
    }

    @Test
    fun init_fromScanConfirmation_prefillsCodeFieldsAndTitle() = runTest {
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AddCardRoutes.categoryIdArg to "default_access",
                    ManualEntryRoutes.sourceArg to ManualEntrySource.SCAN_CONFIRMATION.name,
                    ManualEntryRoutes.codeTypeArg to CardCodeType.CODE_128.name,
                    ManualEntryRoutes.codeValueArg to "SCANNED-123"
                )
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(com.threemdroid.digitalwallet.R.string.scan_barcode_confirmation_title, state.titleRes)
        assertEquals("default_access", state.selectedCategoryId)
        assertEquals(CardCodeType.CODE_128, state.selectedCodeType)
        assertEquals("SCANNED-123", state.codeValue)
    }

    @Test
    fun init_fromPhotoScanConfirmation_prefillsExtractedFieldsAndReviewMessage() = runTest {
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AddCardRoutes.categoryIdArg to "default_library",
                    ManualEntryRoutes.sourceArg to ManualEntrySource.PHOTO_SCAN_CONFIRMATION.name,
                    ManualEntryRoutes.codeTypeArg to CardCodeType.OTHER.name,
                    ManualEntryRoutes.codeValueArg to "",
                    ManualEntryRoutes.cardNumberArg to "LIB-7788",
                    ManualEntryRoutes.cardNameArg to "City Library"
                )
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(com.threemdroid.digitalwallet.R.string.scan_card_photo_confirmation_title, state.titleRes)
        assertEquals(
            com.threemdroid.digitalwallet.R.string.scan_card_photo_review_message,
            state.reviewMessageRes
        )
        assertEquals("default_library", state.selectedCategoryId)
        assertEquals(CardCodeType.OTHER, state.selectedCodeType)
        assertEquals("", state.codeValue)
        assertEquals("LIB-7788", state.cardNumber)
        assertEquals("City Library", state.cardName)
    }

    @Test
    fun init_fromSmartScanConfirmation_prefillsExtractedFieldsAndReviewMessage() = runTest {
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AddCardRoutes.categoryIdArg to "default_access",
                    ManualEntryRoutes.sourceArg to ManualEntrySource.SMART_SCAN_CONFIRMATION.name,
                    ManualEntryRoutes.codeTypeArg to CardCodeType.CODE_128.name,
                    ManualEntryRoutes.codeValueArg to "ACCESS-4455",
                    ManualEntryRoutes.cardNumberArg to "4455",
                    ManualEntryRoutes.cardNameArg to "Office Badge"
                )
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(com.threemdroid.digitalwallet.R.string.smart_scan_confirmation_title, state.titleRes)
        assertEquals(
            com.threemdroid.digitalwallet.R.string.smart_scan_review_message,
            state.reviewMessageRes
        )
        assertEquals("default_access", state.selectedCategoryId)
        assertEquals(CardCodeType.CODE_128, state.selectedCodeType)
        assertEquals("ACCESS-4455", state.codeValue)
        assertEquals("4455", state.cardNumber)
        assertEquals("Office Badge", state.cardName)
    }

    @Test
    fun init_fromGoogleWalletImportConfirmation_prefillsImportedFieldsAndNotes() = runTest {
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AddCardRoutes.categoryIdArg to "default_library",
                    ManualEntryRoutes.sourceArg to ManualEntrySource.GOOGLE_WALLET_IMPORT_CONFIRMATION.name,
                    ManualEntryRoutes.codeTypeArg to CardCodeType.OTHER.name,
                    ManualEntryRoutes.codeValueArg to "",
                    ManualEntryRoutes.cardNumberArg to "LIB-7788",
                    ManualEntryRoutes.cardNameArg to "City Library",
                    ManualEntryRoutes.notesArg to "https://pay.google.com/gp/v/save/test-pass"
                )
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(com.threemdroid.digitalwallet.R.string.google_wallet_import_confirmation_title, state.titleRes)
        assertEquals(
            com.threemdroid.digitalwallet.R.string.google_wallet_import_review_message,
            state.reviewMessageRes
        )
        assertEquals("default_library", state.selectedCategoryId)
        assertEquals(CardCodeType.OTHER, state.selectedCodeType)
        assertEquals("", state.codeValue)
        assertEquals("LIB-7788", state.cardNumber)
        assertEquals("City Library", state.cardName)
        assertEquals("https://pay.google.com/gp/v/save/test-pass", state.notes)
    }

    @Test
    fun init_inEditMode_prefillsExistingCardFieldsAndUsesEditCopy() = runTest {
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ManualEntryRoutes.cardIdArg to "card_access")
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository(
                initialCards = listOf(
                    walletCard(
                        id = "card_access",
                        categoryId = "default_access",
                        position = 0
                    ).copy(
                        name = "Office Badge",
                        codeType = CardCodeType.CODE_128,
                        cardNumber = "ACC-4455",
                        expirationDate = java.time.LocalDate.parse("2026-11-15"),
                        notes = "Reception desk",
                        isFavorite = true
                    )
                )
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCardMissing)
        assertEquals(com.threemdroid.digitalwallet.R.string.edit_card_title, state.titleRes)
        assertEquals(com.threemdroid.digitalwallet.R.string.edit_card_save_button, state.saveButtonRes)
        assertEquals("Office Badge", state.cardName)
        assertEquals("default_access", state.selectedCategoryId)
        assertEquals(CardCodeType.CODE_128, state.selectedCodeType)
        assertEquals("CODE-card_access", state.codeValue)
        assertEquals("ACC-4455", state.cardNumber)
        assertEquals("2026-11-15", state.expirationDateInput)
        assertEquals("Reception desk", state.notes)
        assertTrue(state.isFavorite)
    }

    @Test
    fun save_withoutSelectingCategory_blocksSaveForGenericFlow() = runTest {
        val cardRepository = FakeCardRepository()
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Library Card"))
        viewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("LIB-123"))
        viewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ManualEntryFieldError.REQUIRED, state.categoryError)
        assertTrue(cardRepository.savedCards.isEmpty())
    }

    @Test
    fun save_withBlankNameAndCodeValue_showsRequiredValidationAndDoesNotPersist() = runTest {
        val cardRepository = FakeCardRepository()
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_membership")
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(ManualEntryEvent.OnCardNameChanged("   "))
        viewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("   "))
        viewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ManualEntryFieldError.REQUIRED, state.cardNameError)
        assertEquals(ManualEntryFieldError.REQUIRED, state.codeValueError)
        assertTrue(cardRepository.savedCards.isEmpty())
    }

    @Test
    fun save_withInvalidExpirationDate_showsValidationError() = runTest {
        val cardRepository = FakeCardRepository()
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Gym Pass"))
        viewModel.onEvent(ManualEntryEvent.OnCategorySelected("default_membership"))
        viewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("GYM-777"))
        viewModel.onEvent(ManualEntryEvent.OnExpirationDateChanged("2026/12/31"))
        viewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(
            ManualEntryExpirationDateError.INVALID_FORMAT,
            viewModel.uiState.value.expirationDateError
        )
        assertTrue(cardRepository.savedCards.isEmpty())
    }

    @Test
    fun save_inGenericFlowAfterChoosingCategory_persistsIntoSelectedCategory() = runTest {
        val cardRepository = FakeCardRepository()
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Library Pass"))
        viewModel.onEvent(ManualEntryEvent.OnCategorySelected("default_library"))
        viewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("LIB-001"))
        val savedEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        val savedCard = cardRepository.savedCards.single()
        assertEquals(ManualEntryEffect.CardSaved("default_library"), savedEffect.await())
        assertEquals("default_library", savedCard.categoryId)
        assertEquals(0, savedCard.position)
    }

    @Test
    fun save_withValidInput_persistsCardAndAppendsPosition() = runTest {
        val cardRepository = FakeCardRepository(
            initialCards = listOf(
                walletCard(
                    id = "existing-card",
                    categoryId = "default_access",
                    position = 0
                )
            )
        )
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_access")
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(ManualEntryEvent.OnCardNameChanged("  Office Pass  "))
        viewModel.onEvent(ManualEntryEvent.OnCodeTypeSelected(CardCodeType.CODE_128))
        viewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("  PASS-456  "))
        viewModel.onEvent(ManualEntryEvent.OnCardNumberChanged("  123456  "))
        viewModel.onEvent(ManualEntryEvent.OnExpirationDateChanged("2026-12-31"))
        viewModel.onEvent(ManualEntryEvent.OnNotesChanged("  Visitor entrance  "))
        viewModel.onEvent(ManualEntryEvent.OnFavoriteChanged(true))
        val savedEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        val savedCard = cardRepository.savedCards.single()
        assertEquals(ManualEntryEffect.CardSaved("default_access"), savedEffect.await())
        assertEquals("Office Pass", savedCard.name)
        assertEquals("PASS-456", savedCard.codeValue)
        assertEquals("123456", savedCard.cardNumber)
        assertEquals("2026-12-31", savedCard.expirationDate.toString())
        assertEquals("Visitor entrance", savedCard.notes)
        assertEquals(CardCodeType.CODE_128, savedCard.codeType)
        assertTrue(savedCard.isFavorite)
        assertEquals(1, savedCard.position)
        assertNull(viewModel.uiState.value.cardNameError)
        assertNull(viewModel.uiState.value.categoryError)
        assertNull(viewModel.uiState.value.codeValueError)
    }

    @Test
    fun save_inEditMode_updatesExistingCardAndNavigatesBack() = runTest {
        val existingCard = walletCard(
            id = "card_access",
            categoryId = "default_access",
            position = 0
        ).copy(
            name = "Office Badge",
            codeType = CardCodeType.QR_CODE,
            cardNumber = "ACC-001",
            notes = "Old note"
        )
        val cardRepository = FakeCardRepository(
            initialCards = listOf(
                existingCard,
                walletCard(
                    id = "card_access_2",
                    categoryId = "default_access",
                    position = 1
                )
            )
        )
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ManualEntryRoutes.cardIdArg to existingCard.id)
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Updated Badge"))
        viewModel.onEvent(ManualEntryEvent.OnCodeTypeSelected(CardCodeType.CODE_39))
        viewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("UPDATED-999"))
        viewModel.onEvent(ManualEntryEvent.OnCardNumberChanged("NEW-123"))
        viewModel.onEvent(ManualEntryEvent.OnExpirationDateChanged("2026-12-31"))
        viewModel.onEvent(ManualEntryEvent.OnNotesChanged("Updated note"))
        viewModel.onEvent(ManualEntryEvent.OnFavoriteChanged(true))
        val savedEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryEffect.NavigateBack, savedEffect.await())
        val savedCard = cardRepository.observeCard(existingCard.id).first()
        assertEquals("Updated Badge", savedCard?.name)
        assertEquals("default_access", savedCard?.categoryId)
        assertEquals("UPDATED-999", savedCard?.codeValue)
        assertEquals(CardCodeType.CODE_39, savedCard?.codeType)
        assertEquals("NEW-123", savedCard?.cardNumber)
        assertEquals("2026-12-31", savedCard?.expirationDate.toString())
        assertEquals("Updated note", savedCard?.notes)
        assertTrue(savedCard?.isFavorite == true)
        assertEquals(0, savedCard?.position)
    }

    @Test
    fun save_inEditMode_withoutValidCategory_showsRequiredErrorAndDoesNotPersist() = runTest {
        val existingCard = walletCard(
            id = "card_access",
            categoryId = "default_access",
            position = 0
        ).copy(
            name = "Office Badge",
            codeType = CardCodeType.QR_CODE
        )
        val cardRepository = FakeCardRepository(
            initialCards = listOf(existingCard)
        )
        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ManualEntryRoutes.cardIdArg to existingCard.id)
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Updated Badge"))
        viewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("UPDATED-999"))
        viewModel.onEvent(ManualEntryEvent.OnCategorySelected("missing-category"))
        viewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryFieldError.REQUIRED, viewModel.uiState.value.categoryError)
        val persistedCard = cardRepository.observeCard(existingCard.id).first()
        assertEquals("Office Badge", persistedCard?.name)
        assertEquals("default_access", persistedCard?.categoryId)
        assertTrue(cardRepository.savedCards.isEmpty())
    }

    private class FakeCategoryRepository : CategoryRepository {
        private val categoriesFlow = MutableStateFlow(defaultCategories)

        override fun observeCategories(): Flow<List<Category>> = categoriesFlow

        override fun observeCategoriesWithCardCounts(): Flow<List<CategoryWithCardCount>> =
            categoriesFlow.map { categories ->
                categories.map { category ->
                    CategoryWithCardCount(category = category, cardCount = 0)
                }
            }

        override fun observeCategory(categoryId: String): Flow<Category?> =
            categoriesFlow.map { categories ->
                categories.firstOrNull { category -> category.id == categoryId }
            }

        override suspend fun ensureDefaultCategories() = Unit

        override suspend fun createCustomCategory(name: String, color: String): Category {
            error("Not needed for ManualEntryViewModelTest")
        }

        override suspend fun upsertCategory(category: Category) {
            error("Not needed for ManualEntryViewModelTest")
        }

        override suspend fun upsertCategories(categories: List<Category>) {
            error("Not needed for ManualEntryViewModelTest")
        }

        override suspend fun updateCategoryOrder(categoryIdsInOrder: List<String>) {
            error("Not needed for ManualEntryViewModelTest")
        }

        override suspend fun deleteCategory(categoryId: String) {
            error("Not needed for ManualEntryViewModelTest")
        }
    }

    private class FakeCardRepository(
        initialCards: List<WalletCard> = emptyList()
    ) : CardRepository {
        private val cardsFlow = MutableStateFlow(initialCards)

        val savedCards = mutableListOf<WalletCard>()

        override fun observeAllCards(): Flow<List<WalletCard>> = cardsFlow

        override fun observeCards(categoryId: String): Flow<List<WalletCard>> =
            cardsFlow.map { cards ->
                cards.filter { card -> card.categoryId == categoryId }
            }

        override fun observeCard(cardId: String): Flow<WalletCard?> =
            cardsFlow.map { cards ->
                cards.firstOrNull { card -> card.id == cardId }
            }

        override suspend fun upsertCard(card: WalletCard) {
            savedCards += card
            cardsFlow.value = cardsFlow.value
                .filterNot { existingCard -> existingCard.id == card.id } +
                card
        }

        override suspend fun upsertCards(cards: List<WalletCard>) {
            error("Not needed for ManualEntryViewModelTest")
        }

        override suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>) {
            val cardsById = cardsFlow.value.associateBy { card -> card.id }
            val reorderedCards = cardIdsInOrder.mapIndexedNotNull { index, cardId ->
                cardsById[cardId]?.copy(position = index)
            }
            cardsFlow.value = cardsFlow.value
                .filterNot { card -> card.categoryId == categoryId } +
                reorderedCards
        }

        override suspend fun deleteCard(cardId: String) {
            error("Not needed for ManualEntryViewModelTest")
        }
    }

    private companion object {
        val fixedTimestamp: Instant = Instant.parse("2026-03-13T10:00:00Z")

        val defaultCategories = listOf(
            category("default_shopping_loyalty", "Shopping & Loyalty", 0, true, false),
            category("default_membership", "Membership", 1, true, false),
            category("default_transport", "Transport", 2, true, false),
            category("default_tickets", "Tickets", 3, true, false),
            category("default_vouchers", "Vouchers", 4, true, false),
            category("default_access", "Access", 5, true, false),
            category("default_library", "Library", 6, true, false),
            category("default_other", "Other", 7, true, false)
        )

        fun category(
            id: String,
            name: String,
            position: Int,
            isDefault: Boolean,
            isFavorites: Boolean
        ): Category =
            Category(
                id = id,
                name = name,
                color = "#123456",
                isDefault = isDefault,
                isFavorites = isFavorites,
                position = position,
                createdAt = fixedTimestamp,
                updatedAt = fixedTimestamp
            )

        fun walletCard(
            id: String,
            categoryId: String,
            position: Int
        ): WalletCard =
            WalletCard(
                id = id,
                name = "Card $id",
                categoryId = categoryId,
                codeValue = "CODE-$id",
                codeType = CardCodeType.QR_CODE,
                cardNumber = null,
                expirationDate = null,
                notes = null,
                isFavorite = false,
                position = position,
                createdAt = fixedTimestamp,
                updatedAt = fixedTimestamp
            )
    }
}
