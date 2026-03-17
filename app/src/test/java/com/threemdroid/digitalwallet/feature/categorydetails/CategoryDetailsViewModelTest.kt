package com.threemdroid.digitalwallet.feature.categorydetails

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.data.category.FavoritesCategory
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
class CategoryDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsCategoryCardsAndExpirationBadge() = runTest {
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_library")
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
            cardRepository = FakeCardRepository(
                cards = listOf(
                    walletCard(
                        id = "card_library",
                        name = "City Library",
                        categoryId = "default_library",
                        codeType = CardCodeType.CODE_128,
                        expirationDate = LocalDate.now().plusDays(3),
                        position = 0
                    ),
                    walletCard(
                        id = "card_archive",
                        name = "Archive Pass",
                        categoryId = "default_library",
                        codeType = CardCodeType.QR_CODE,
                        expirationDate = null,
                        position = 1
                    )
                )
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Library", state.title)
        assertEquals(2, state.cards.size)
        assertEquals("City Library", state.cards.first().name)
        assertEquals("CL", state.cards.first().placeholderLabel)
        assertEquals("Code 128", state.cards.first().codeTypeLabel)
        assertEquals(
            CategoryDetailsExpirationBadgeStatus.EXPIRING_SOON,
            state.cards.first().expirationBadge?.status
        )
        assertEquals(3, state.cards.first().expirationBadge?.daysUntilExpiration)
        assertNull(state.cards.last().expirationBadge)
    }

    @Test
    fun init_withNoCards_exposesEmptyState() = runTest {
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_membership")
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    category(
                        id = "default_membership",
                        name = "Membership",
                        color = "#7C3AED",
                        position = 2
                    )
                )
            ),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertEquals("Membership", state.title)
    }

    @Test
    fun init_withoutCategoryId_exposesMissingStateInsteadOfCrashing() = runTest {
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isCategoryMissing)
    }

    @Test
    fun init_withVirtualFavorites_loadsFavoriteCardsAcrossRealCategoriesAndDisablesReorder() = runTest {
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to FavoritesCategory.id)
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(FavoritesCategory.create())
            ),
            cardRepository = FakeCardRepository(
                cards = listOf(
                    walletCard(
                        id = "favorite-library",
                        name = "City Library",
                        categoryId = "default_library",
                        codeType = CardCodeType.CODE_128,
                        expirationDate = null,
                        position = 0
                    ).copy(isFavorite = true),
                    walletCard(
                        id = "favorite-access",
                        name = "Office Badge",
                        categoryId = "default_access",
                        codeType = CardCodeType.QR_CODE,
                        expirationDate = null,
                        position = 0
                    ).copy(isFavorite = true),
                    walletCard(
                        id = "non-favorite",
                        name = "Transit Pass",
                        categoryId = "default_transport",
                        codeType = CardCodeType.CODE_39,
                        expirationDate = null,
                        position = 0
                    )
                )
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Favorites", state.title)
        assertEquals(listOf("favorite-access", "favorite-library"), state.cards.map { it.id }.sorted())
        assertFalse(state.isCardReorderEnabled)
        assertFalse(state.isCardReordering)
    }

    @Test
    fun init_mapsExpiresTodayAndExpiredBadges() = runTest {
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_access")
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
                        id = "card_today",
                        name = "Office Pass",
                        categoryId = "default_access",
                        codeType = CardCodeType.QR_CODE,
                        expirationDate = LocalDate.now(),
                        position = 0
                    ),
                    walletCard(
                        id = "card_expired",
                        name = "Gym Badge",
                        categoryId = "default_access",
                        codeType = CardCodeType.CODE_39,
                        expirationDate = LocalDate.now().minusDays(1),
                        position = 1
                    )
                )
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            CategoryDetailsExpirationBadgeStatus.EXPIRES_TODAY,
            state.cards.first().expirationBadge?.status
        )
        assertEquals(
            CategoryDetailsExpirationBadgeStatus.EXPIRED,
            state.cards.last().expirationBadge?.status
        )
    }

    @Test
    fun onBackClicked_emitsNavigateBack() = runTest {
        val viewModel = createViewModelForCategory("default_access")

        advanceUntilIdle()

        val backEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CategoryDetailsEvent.OnBackClicked)
        assertEquals(CategoryDetailsEffect.NavigateBack, backEffect.await())
    }

    @Test
    fun onAddCardClicked_emitsOpenAddCardForCurrentCategory() = runTest {
        val viewModel = createViewModelForCategory("default_access")

        advanceUntilIdle()

        val addCardEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CategoryDetailsEvent.OnAddCardClicked)
        assertEquals(
            CategoryDetailsEffect.OpenAddCard("default_access"),
            addCardEffect.await()
        )
    }

    @Test
    fun onAddCardClicked_fromVirtualFavorites_emitsGenericAddCardFlow() = runTest {
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to FavoritesCategory.id)
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(FavoritesCategory.create())
            ),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val addCardEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CategoryDetailsEvent.OnAddCardClicked)
        assertEquals(CategoryDetailsEffect.OpenAddCard(null), addCardEffect.await())
    }

    @Test
    fun onCardClicked_emitsOpenCardDetails() = runTest {
        val viewModel = createViewModelForCategory("default_access")

        advanceUntilIdle()

        val cardDetailsEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CategoryDetailsEvent.OnCardClicked("card_access"))
        assertEquals(
            CategoryDetailsEffect.OpenCardDetails("card_access"),
            cardDetailsEffect.await()
        )
    }

    @Test
    fun onDeleteClicked_forVirtualFavorites_emitsProtectedMessage() = runTest {
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to FavoritesCategory.id)
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(FavoritesCategory.create())
            ),
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        val deleteEffect = async { viewModel.effects.first() }
        advanceUntilIdle()
        viewModel.onEvent(CategoryDetailsEvent.OnDeleteClicked)

        assertEquals(
            CategoryDetailsEffect.ShowDeleteMessage(
                R.string.category_details_delete_blocked_protected
            ),
            deleteEffect.await()
        )
        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun onDeleteClicked_forDefaultCategory_emitsProtectedMessage() = runTest {
        val viewModel = createViewModelForCategory("default_access")

        advanceUntilIdle()

        val deleteEffect = async { viewModel.effects.first() }
        advanceUntilIdle()
        viewModel.onEvent(CategoryDetailsEvent.OnDeleteClicked)

        assertEquals(
            CategoryDetailsEffect.ShowDeleteMessage(
                R.string.category_details_delete_blocked_protected
            ),
            deleteEffect.await()
        )
        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun onDeleteClicked_forNonEmptyCustomCategory_emitsBlockedMessage() = runTest {
        val customCategory = category(
            id = "custom_campus",
            name = "Campus",
            color = "#123456",
            position = 8,
            isDefault = false
        )
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to customCategory.id)
            ),
            categoryRepository = FakeCategoryRepository(categories = listOf(customCategory)),
            cardRepository = FakeCardRepository(
                cards = listOf(
                    walletCard(
                        id = "campus-card",
                        name = "Campus Pass",
                        categoryId = customCategory.id,
                        codeType = CardCodeType.QR_CODE,
                        expirationDate = null,
                        position = 0
                    )
                )
            )
        )

        advanceUntilIdle()

        val deleteEffect = async { viewModel.effects.first() }
        advanceUntilIdle()
        viewModel.onEvent(CategoryDetailsEvent.OnDeleteClicked)

        assertEquals(
            CategoryDetailsEffect.ShowDeleteMessage(
                R.string.category_details_delete_blocked_not_empty
            ),
            deleteEffect.await()
        )
        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun deleteEmptyCustomCategory_showsConfirmationAndNavigatesBackOnSuccess() = runTest {
        val customCategory = category(
            id = "custom_campus",
            name = "Campus",
            color = "#123456",
            position = 8,
            isDefault = false
        )
        val categoryRepository = FakeCategoryRepository(categories = listOf(customCategory))
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to customCategory.id)
            ),
            categoryRepository = categoryRepository,
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(CategoryDetailsEvent.OnDeleteClicked)
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)

        val deleteEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CategoryDetailsEvent.OnDeleteConfirmed)
        advanceUntilIdle()

        assertEquals(CategoryDetailsEffect.NavigateBack, deleteEffect.await())
        assertEquals(1, categoryRepository.deleteCategoryCallCount)
        assertEquals(customCategory.id, categoryRepository.lastDeletedCategoryId)
        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun deleteEmptyCustomCategory_failureEmitsErrorMessage() = runTest {
        val customCategory = category(
            id = "custom_campus",
            name = "Campus",
            color = "#123456",
            position = 8,
            isDefault = false
        )
        val categoryRepository = FakeCategoryRepository(
            categories = listOf(customCategory),
            failDeleteCategory = true
        )
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to customCategory.id)
            ),
            categoryRepository = categoryRepository,
            cardRepository = FakeCardRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(CategoryDetailsEvent.OnDeleteClicked)
        val deleteEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CategoryDetailsEvent.OnDeleteConfirmed)
        advanceUntilIdle()

        assertEquals(
            CategoryDetailsEffect.ShowDeleteMessage(
                R.string.category_details_delete_failed_message
            ),
            deleteEffect.await()
        )
        assertFalse(viewModel.uiState.value.isDeleteInProgress)
    }

    @Test
    fun reorderCards_updatesInMemoryOrderAndPersistsOnlyOnFinish() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                walletCard(
                    id = "card_library",
                    name = "City Library",
                    categoryId = "default_library",
                    codeType = CardCodeType.CODE_128,
                    expirationDate = null,
                    position = 0
                ),
                walletCard(
                    id = "card_archive",
                    name = "Archive Pass",
                    categoryId = "default_library",
                    codeType = CardCodeType.QR_CODE,
                    expirationDate = null,
                    position = 1
                ),
                walletCard(
                    id = "card_reading",
                    name = "Reading Club",
                    categoryId = "default_library",
                    codeType = CardCodeType.CODE_39,
                    expirationDate = null,
                    position = 2
                )
            )
        )
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_library")
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

        viewModel.onEvent(CategoryDetailsEvent.OnCardReorderStarted("card_reading"))
        viewModel.onEvent(
            CategoryDetailsEvent.OnCardReorderMoved(
                fromCardId = "card_reading",
                toCardId = "card_library"
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCardReordering)
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            viewModel.uiState.value.cards.map { card -> card.id }
        )
        assertEquals(0, cardRepository.updateCardOrderCallCount)

        viewModel.onEvent(CategoryDetailsEvent.OnCardReorderFinished)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCardReordering)
        assertEquals(1, cardRepository.updateCardOrderCallCount)
        assertEquals("default_library", cardRepository.lastUpdatedCategoryId)
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            cardRepository.lastUpdatedOrder
        )
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            viewModel.uiState.value.cards.map { card -> card.id }
        )
    }

    @Test
    fun reorderCards_cancelRestoresPersistedOrderWithoutSaving() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                walletCard(
                    id = "card_library",
                    name = "City Library",
                    categoryId = "default_library",
                    codeType = CardCodeType.CODE_128,
                    expirationDate = null,
                    position = 0
                ),
                walletCard(
                    id = "card_archive",
                    name = "Archive Pass",
                    categoryId = "default_library",
                    codeType = CardCodeType.QR_CODE,
                    expirationDate = null,
                    position = 1
                ),
                walletCard(
                    id = "card_reading",
                    name = "Reading Club",
                    categoryId = "default_library",
                    codeType = CardCodeType.CODE_39,
                    expirationDate = null,
                    position = 2
                )
            )
        )
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_library")
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

        viewModel.onEvent(CategoryDetailsEvent.OnCardReorderStarted("card_reading"))
        viewModel.onEvent(
            CategoryDetailsEvent.OnCardReorderMoved(
                fromCardId = "card_reading",
                toCardId = "card_library"
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCardReordering)
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            viewModel.uiState.value.cards.map { card -> card.id }
        )

        viewModel.onEvent(CategoryDetailsEvent.OnCardReorderCancelled)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCardReordering)
        assertEquals(0, cardRepository.updateCardOrderCallCount)
        assertEquals(
            listOf("card_library", "card_archive", "card_reading"),
            viewModel.uiState.value.cards.map { card -> card.id }
        )
    }

    @Test
    fun reorderCards_forVirtualFavorites_isIgnored() = runTest {
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to FavoritesCategory.id)
            ),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(FavoritesCategory.create())
            ),
            cardRepository = FakeCardRepository(
                cards = listOf(
                    walletCard(
                        id = "favorite-library",
                        name = "City Library",
                        categoryId = "default_library",
                        codeType = CardCodeType.CODE_128,
                        expirationDate = null,
                        position = 0
                    ).copy(isFavorite = true)
                )
            )
        )

        advanceUntilIdle()

        viewModel.onEvent(CategoryDetailsEvent.OnCardReorderStarted("favorite-library"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCardReordering)
    }

    @Test
    fun reorderCards_failureResetsStateAndEmitsErrorEffect() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                walletCard(
                    id = "card_library",
                    name = "City Library",
                    categoryId = "default_library",
                    codeType = CardCodeType.CODE_128,
                    expirationDate = null,
                    position = 0
                ),
                walletCard(
                    id = "card_archive",
                    name = "Archive Pass",
                    categoryId = "default_library",
                    codeType = CardCodeType.QR_CODE,
                    expirationDate = null,
                    position = 1
                )
            ),
            failUpdateCardOrder = true
        )
        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_library")
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

        viewModel.onEvent(CategoryDetailsEvent.OnCardReorderStarted("card_archive"))
        viewModel.onEvent(
            CategoryDetailsEvent.OnCardReorderMoved(
                fromCardId = "card_archive",
                toCardId = "card_library"
            )
        )
        advanceUntilIdle()

        val reorderErrorEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CategoryDetailsEvent.OnCardReorderFinished)
        advanceUntilIdle()

        assertEquals(
            CategoryDetailsEffect.ShowCardReorderFailedMessage,
            reorderErrorEffect.await()
        )
        assertFalse(viewModel.uiState.value.isCardReordering)
        assertEquals(
            listOf("card_library", "card_archive"),
            viewModel.uiState.value.cards.map { card -> card.id }
        )
        assertEquals(1, cardRepository.updateCardOrderCallCount)
    }

    private class FakeCategoryRepository(
        categories: List<Category> = emptyList(),
        private val failDeleteCategory: Boolean = false
    ) : CategoryRepository {
        private val categoriesFlow = MutableStateFlow(categories)
        var deleteCategoryCallCount: Int = 0
        var lastDeletedCategoryId: String? = null

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
            error("Not needed for CategoryDetailsViewModelTest")
        }

        override suspend fun upsertCategory(category: Category) {
            error("Not needed for CategoryDetailsViewModelTest")
        }

        override suspend fun upsertCategories(categories: List<Category>) {
            error("Not needed for CategoryDetailsViewModelTest")
        }

        override suspend fun updateCategoryOrder(categoryIdsInOrder: List<String>) {
            error("Not needed for CategoryDetailsViewModelTest")
        }

        override suspend fun deleteCategory(categoryId: String) {
            deleteCategoryCallCount += 1
            lastDeletedCategoryId = categoryId
            if (failDeleteCategory) {
                error("delete failure")
            }
            categoriesFlow.value = categoriesFlow.value.filterNot { category ->
                category.id == categoryId
            }
        }
    }

    private class FakeCardRepository(
        cards: List<WalletCard> = emptyList(),
        private val failUpdateCardOrder: Boolean = false
    ) : CardRepository {
        private val cardsFlow = MutableStateFlow(cards)
        var updateCardOrderCallCount: Int = 0
        var lastUpdatedCategoryId: String? = null
        var lastUpdatedOrder: List<String> = emptyList()

        override fun observeAllCards(): Flow<List<WalletCard>> = cardsFlow

        override fun observeCards(categoryId: String): Flow<List<WalletCard>> =
            cardsFlow.map { cards ->
                cards.filter { card -> card.categoryId == categoryId }
                    .sortedWith(compareBy(WalletCard::position, WalletCard::createdAt))
            }

        override fun observeCard(cardId: String): Flow<WalletCard?> =
            cardsFlow.map { cards ->
                cards.firstOrNull { card -> card.id == cardId }
            }

        override suspend fun upsertCard(card: WalletCard) {
            error("Not needed for CategoryDetailsViewModelTest")
        }

        override suspend fun upsertCards(cards: List<WalletCard>) {
            error("Not needed for CategoryDetailsViewModelTest")
        }

        override suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>) {
            updateCardOrderCallCount += 1
            lastUpdatedCategoryId = categoryId
            lastUpdatedOrder = cardIdsInOrder
            if (failUpdateCardOrder) {
                throw IllegalStateException("Unable to persist card order.")
            }

            val categoryCardsById = cardsFlow.value
                .filter { card -> card.categoryId == categoryId }
                .associateBy { card -> card.id }
            val reorderedCards = cardIdsInOrder.mapIndexed { index, cardId ->
                categoryCardsById.getValue(cardId).copy(position = index)
            }
            cardsFlow.value = cardsFlow.value
                .filterNot { card -> card.categoryId == categoryId } + reorderedCards
        }

        override suspend fun deleteCard(cardId: String) {
            error("Not needed for CategoryDetailsViewModelTest")
        }
    }

    private companion object {
        val fixedTimestamp: Instant = Instant.parse("2026-03-13T10:00:00Z")

        fun createViewModelForCategory(categoryId: String): CategoryDetailsViewModel =
            CategoryDetailsViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(CategoryDetailsRoutes.categoryIdArg to categoryId)
                ),
                categoryRepository = FakeCategoryRepository(
                    categories = listOf(
                        category(
                            id = categoryId,
                            name = "Access",
                            color = "#4B5563",
                            position = 6
                        )
                    )
                ),
                cardRepository = FakeCardRepository()
            )

        fun category(
            id: String,
            name: String,
            color: String,
            position: Int,
            isDefault: Boolean = true,
            isFavorites: Boolean = false
        ): Category =
            Category(
                id = id,
                name = name,
                color = color,
                isDefault = isDefault,
                isFavorites = isFavorites,
                position = position,
                createdAt = fixedTimestamp,
                updatedAt = fixedTimestamp
            )

        fun walletCard(
            id: String,
            name: String,
            categoryId: String,
            codeType: CardCodeType,
            expirationDate: LocalDate?,
            position: Int
        ): WalletCard =
            WalletCard(
                id = id,
                name = name,
                categoryId = categoryId,
                codeValue = "CODE-$id",
                codeType = codeType,
                cardNumber = "NUM-$id",
                expirationDate = expirationDate,
                notes = null,
                isFavorite = false,
                position = position,
                createdAt = fixedTimestamp,
                updatedAt = fixedTimestamp
            )
    }
}
