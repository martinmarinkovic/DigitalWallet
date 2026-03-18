package com.threemdroid.digitalwallet.feature.carddetails

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
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
class CardDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsCardDetailsAndOptionalFields() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                walletCard(
                    id = "card_library",
                    categoryId = "default_library",
                    name = "City Library",
                    codeType = CardCodeType.CODE_128,
                    cardNumber = "LIB-7788",
                    expirationDate = LocalDate.parse("2026-12-31"),
                    notes = "Basement entrance only",
                    isFavorite = true
                )
            )
        )
        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "card_library")
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    category(
                        id = "default_library",
                        name = "Library",
                        color = "#1D4ED8",
                        position = 7
                    )
                )
            ),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCardMissing)
        assertEquals("City Library", state.title)
        assertEquals("Library", state.categoryName)
        assertEquals("#1D4ED8", state.categoryColorHex)
        assertEquals("Code 128", state.codeTypeLabel)
        assertEquals("CODE-card_library", state.codeValue)
        assertEquals("LIB-7788", state.cardNumber)
        assertEquals("2026-12-31", state.expirationDate)
        assertEquals("Basement entrance only", state.notes)
        assertTrue(state.isFavorite)
    }

    @Test
    fun init_withMissingOptionalFieldsKeepsUiStateClean() = runTest {
        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "card_access")
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    category(
                        id = "default_access",
                        name = "Access",
                        color = "#4B5563",
                        position = 6
                    )
                )
            ),
            cardRepository = FakeCardRepository(
                cards = listOf(
                    walletCard(
                        id = "card_access",
                        categoryId = "default_access",
                        name = "Office Badge",
                        codeType = CardCodeType.QR_CODE,
                        cardNumber = null,
                        expirationDate = null,
                        notes = null
                    )
                )
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.cardNumber)
        assertNull(state.expirationDate)
        assertNull(state.notes)
    }

    @Test
    fun init_withBlankOptionalFieldsTrimsThemOutOfUiState() = runTest {
        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "card_access")
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    category(
                        id = "default_access",
                        name = "Access",
                        color = "#4B5563",
                        position = 6
                    )
                )
            ),
            cardRepository = FakeCardRepository(
                cards = listOf(
                    walletCard(
                        id = "card_access",
                        categoryId = "default_access",
                        name = "Office Badge",
                        codeType = CardCodeType.QR_CODE,
                        cardNumber = "   ",
                        expirationDate = null,
                        notes = "   "
                    )
                )
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.cardNumber)
        assertNull(state.expirationDate)
        assertNull(state.notes)
    }

    @Test
    fun init_withoutCardId_exposesMissingStateInsteadOfCrashing() = runTest {
        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isCardMissing)
    }

    @Test
    fun onFavoriteClicked_updatesFavoriteState() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                walletCard(
                    id = "card_access",
                    categoryId = "default_access",
                    name = "Office Badge",
                    codeType = CardCodeType.QR_CODE
                )
            )
        )
        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "card_access")
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    category(
                        id = "default_access",
                        name = "Access",
                        color = "#4B5563",
                        position = 6
                    )
                )
            ),
            cardRepository = cardRepository
        )

        advanceUntilIdle()
        viewModel.onEvent(CardDetailsEvent.OnFavoriteClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFavorite)
        assertTrue(cardRepository.observeCard("card_access").first()?.isFavorite == true)
    }

    @Test
    fun onFavoriteClicked_whenAlreadyFavorite_updatesFavoriteStateToFalse() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                walletCard(
                    id = "card_access",
                    categoryId = "default_access",
                    name = "Office Badge",
                    codeType = CardCodeType.QR_CODE,
                    isFavorite = true
                )
            )
        )
        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "card_access")
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    category(
                        id = "default_access",
                        name = "Access",
                        color = "#4B5563",
                        position = 6
                    )
                )
            ),
            cardRepository = cardRepository
        )

        advanceUntilIdle()
        viewModel.onEvent(CardDetailsEvent.OnFavoriteClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFavorite)
        assertFalse(cardRepository.observeCard("card_access").first()?.isFavorite == true)
    }

    @Test
    fun onDeleteClickedAndDismissed_updatesConfirmationVisibility() = runTest {
        val viewModel = createViewModelForCard("card_access")

        advanceUntilIdle()

        viewModel.onEvent(CardDetailsEvent.OnDeleteClicked)
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)

        viewModel.onEvent(CardDetailsEvent.OnDeleteDismissed)
        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun onDeleteConfirmed_deletesCardAndNavigatesBack() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                walletCard(
                    id = "card_access",
                    categoryId = "default_access",
                    name = "Office Badge",
                    codeType = CardCodeType.QR_CODE
                )
            )
        )
        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "card_access")
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    category(
                        id = "default_access",
                        name = "Access",
                        color = "#4B5563",
                        position = 6
                    )
                )
            ),
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(CardDetailsEvent.OnDeleteClicked)
        val deferredEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CardDetailsEvent.OnDeleteConfirmed)
        advanceUntilIdle()

        assertEquals(CardDetailsEffect.NavigateBack, deferredEffect.await())
        assertNull(cardRepository.observeCard("card_access").first())
    }

    @Test
    fun onEditClicked_emitsOpenEdit() = runTest {
        val viewModel = createViewModelForCard("card_access")

        advanceUntilIdle()

        val deferredEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CardDetailsEvent.OnEditClicked)

        assertEquals(CardDetailsEffect.OpenEdit("card_access"), deferredEffect.await())
    }

    @Test
    fun onShareClicked_emitsShareSheetEffect() = runTest {
        val viewModel = createViewModelForCard("card_access")

        advanceUntilIdle()

        val deferredEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CardDetailsEvent.OnShareClicked)

        assertEquals(
            CardDetailsEffect.OpenShareSheet(
                title = "Office Badge",
                shareText = "Office Badge\nCode type: QR Code\nCode value: CODE-card_access"
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onOpenFullscreenCodeClicked_emitsOpenFullscreenCode() = runTest {
        val viewModel = createViewModelForCard("card_access")

        advanceUntilIdle()

        val deferredEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CardDetailsEvent.OnOpenFullscreenCodeClicked)

        assertEquals(
            CardDetailsEffect.OpenFullscreenCode("card_access"),
            deferredEffect.await()
        )
    }

    @Test
    fun init_withMissingCard_exposesMissingState() = runTest {
        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "missing_card")
            ),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isCardMissing)
    }

    private class FakeCategoryRepository(
        categories: List<Category> = emptyList()
    ) : CategoryRepository {
        private val categoriesFlow = MutableStateFlow(categories)

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
            error("Not needed for CardDetailsViewModelTest")
        }

        override suspend fun upsertCategory(category: Category) {
            error("Not needed for CardDetailsViewModelTest")
        }

        override suspend fun upsertCategories(categories: List<Category>) {
            error("Not needed for CardDetailsViewModelTest")
        }

        override suspend fun updateCategoryOrder(categoryIdsInOrder: List<String>) {
            error("Not needed for CardDetailsViewModelTest")
        }

        override suspend fun deleteCategory(categoryId: String) {
            error("Not needed for CardDetailsViewModelTest")
        }
    }

    private class FakeCardRepository(
        cards: List<WalletCard> = emptyList()
    ) : CardRepository {
        private val cardsFlow = MutableStateFlow(cards)

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
            cardsFlow.value = cardsFlow.value
                .filterNot { existingCard -> existingCard.id == card.id } +
                card
        }

        override suspend fun upsertCards(cards: List<WalletCard>) {
            cards.forEach { card -> upsertCard(card) }
        }

        override suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>) {
            error("Not needed for CardDetailsViewModelTest")
        }

        override suspend fun deleteCard(cardId: String) {
            cardsFlow.value = cardsFlow.value.filterNot { card -> card.id == cardId }
        }
    }

    private companion object {
        val fixedTimestamp: Instant = Instant.parse("2026-03-13T10:00:00Z")

        fun createViewModelForCard(cardId: String): CardDetailsViewModel =
            CardDetailsViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(CardDetailsRoutes.cardIdArg to cardId)
                ),
                categoryRepository = FakeCategoryRepository(
                    categories = listOf(
                        category(
                            id = "default_access",
                            name = "Access",
                            color = "#4B5563",
                            position = 6
                        )
                    )
                ),
                cardRepository = FakeCardRepository(
                    cards = listOf(
                        walletCard(
                            id = cardId,
                            categoryId = "default_access",
                            name = "Office Badge",
                            codeType = CardCodeType.QR_CODE
                        )
                    )
                )
            )

        fun category(
            id: String,
            name: String,
            color: String,
            position: Int
        ): Category =
            Category(
                id = id,
                name = name,
                color = color,
                isDefault = true,
                isFavorites = false,
                position = position,
                createdAt = fixedTimestamp,
                updatedAt = fixedTimestamp
            )

        fun walletCard(
            id: String,
            categoryId: String,
            name: String,
            codeType: CardCodeType,
            cardNumber: String? = "NUM-$id",
            expirationDate: LocalDate? = null,
            notes: String? = null,
            isFavorite: Boolean = false
        ): WalletCard =
            WalletCard(
                id = id,
                name = name,
                categoryId = categoryId,
                codeValue = "CODE-$id",
                codeType = codeType,
                cardNumber = cardNumber,
                expirationDate = expirationDate,
                notes = notes,
                isFavorite = isFavorite,
                position = 0,
                createdAt = fixedTimestamp,
                updatedAt = fixedTimestamp
            )
    }
}
