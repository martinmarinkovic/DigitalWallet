package com.threemdroid.digitalwallet.feature.home

import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.category.FavoritesCategory
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.data.searchhistory.SearchHistoryRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.lang.reflect.Modifier
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_searchStartsCollapsedWithEmptyQueryAndPersistedHistoryAvailable() = runTest {
        val viewModel = HomeViewModel(
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository(
                initialHistory = listOf(
                    searchHistoryEntry(id = 2L, query = "membership"),
                    searchHistoryEntry(id = 1L, query = "library")
                )
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSearchExpanded)
        assertFalse(state.isCategoryReordering)
        assertEquals("", state.searchQuery)
        assertTrue(state.searchResultCategories.isEmpty())
        assertTrue(state.searchResultCards.isEmpty())
        assertEquals(listOf("membership", "library"), state.previousSearches.map { it.query })
    }

    @Test
    fun init_whenDefaultCategoriesAreMissing_initializesExpectedCategoriesInCorrectOrder() = runTest {
        val categoryRepository = FakeCategoryRepository()
        val viewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, categoryRepository.ensureDefaultCategoriesCallCount)
        assertFalse(state.isLoading)
        assertEquals(expectedDefaultCategoryNames, state.categories.map { it.name })
        assertEquals("Favorites", state.categories.first().name)
        assertFalse(state.categories.any { it.name == "+ New Category" })
    }

    @Test
    fun init_withExistingCustomCategory_keepsFavoritesFirstAndLeavesCustomCategoryAfterDefaults() = runTest {
        val categoryRepository = FakeCategoryRepository(
            initialCategories = listOf(
                categoryWithCount(
                    id = "custom-campus",
                    name = "Campus",
                    position = 0,
                    color = "#123456",
                    cardCount = 0,
                    isDefault = false,
                    isFavorites = false
                )
            )
        )
        val viewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository()
        )

        advanceUntilIdle()

        val categoryNames = viewModel.uiState.value.categories.map { it.name }
        assertEquals(1, categoryRepository.ensureDefaultCategoriesCallCount)
        assertEquals(expectedDefaultCategoryNames, categoryNames.take(expectedDefaultCategoryNames.size))
        assertEquals("Favorites", categoryNames.first())
        assertEquals("Campus", categoryNames.last())
    }

    @Test
    fun init_exposesCardCountsFromRepositoryInput() = runTest {
        val viewModel = HomeViewModel(
            categoryRepository = FakeCategoryRepository(
                initialCategories = defaultCategoriesWithCounts(
                    overrides = mapOf(
                        FavoritesCategory.id to 1,
                        "default_tickets" to 2
                    )
                )
            ),
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository()
        )

        advanceUntilIdle()

        val categoriesByName = viewModel.uiState.value.categories.associateBy { it.name }
        assertEquals(1, categoriesByName.getValue("Favorites").cardCount)
        assertEquals(2, categoriesByName.getValue("Tickets").cardCount)
        assertEquals(0, categoriesByName.getValue("Membership").cardCount)
        assertFalse(categoriesByName.getValue("Favorites").isReorderable)
        assertTrue(categoriesByName.getValue("Tickets").isReorderable)
    }

    @Test
    fun reorderCategory_keepsFavoritesFirstAndPersistsOnlyOnFinish() = runTest {
        val categoryRepository = FakeCategoryRepository()
        val viewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnCategoryReorderStarted("default_transport"))
        viewModel.onEvent(
            HomeEvent.OnCategoryReorderMoved(
                fromCategoryId = "default_transport",
                toCategoryId = "default_membership"
            )
        )
        advanceUntilIdle()

        val reorderedNames = viewModel.uiState.value.categories.map { category -> category.name }
        assertTrue(viewModel.uiState.value.isCategoryReordering)
        assertEquals(0, categoryRepository.updateCategoryOrderCallCount)
        assertEquals(
            listOf("Favorites", "Shopping & Loyalty", "Transport", "Membership"),
            reorderedNames.take(4)
        )
        assertFalse(viewModel.uiState.value.categories.any { it.name == "+ New Category" })

        viewModel.onEvent(HomeEvent.OnCategoryReorderFinished)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCategoryReordering)
        assertEquals(1, categoryRepository.updateCategoryOrderCallCount)
        assertEquals(
            listOf(
                "default_shopping_loyalty",
                "default_transport",
                "default_membership",
                "default_tickets",
                "default_vouchers",
                "default_access",
                "default_library",
                "default_other"
            ),
            categoryRepository.lastUpdatedOrder
        )
        assertEquals(
            viewModel.uiState.value.categories.size - 1,
            categoryRepository.lastUpdatedOrder.size
        )
        assertTrue(
            categoryRepository.lastUpdatedOrder.none { categoryId ->
                categoryId.contains("new_category", ignoreCase = true)
            }
        )
    }

    @Test
    fun reorderCategory_ignoresFavoritesMoves() = runTest {
        val categoryRepository = FakeCategoryRepository()
        val viewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnCategoryReorderStarted(FavoritesCategory.id))
        viewModel.onEvent(
            HomeEvent.OnCategoryReorderMoved(
                fromCategoryId = FavoritesCategory.id,
                toCategoryId = "default_transport"
            )
        )
        viewModel.onEvent(HomeEvent.OnCategoryReorderFinished)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCategoryReordering)
        assertEquals(expectedDefaultCategoryNames, viewModel.uiState.value.categories.map { it.name })
        assertEquals(0, categoryRepository.updateCategoryOrderCallCount)
    }

    @Test
    fun reorderCategory_cancelRestoresPersistedOrderWithoutSaving() = runTest {
        val categoryRepository = FakeCategoryRepository()
        val viewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnCategoryReorderStarted("default_transport"))
        viewModel.onEvent(
            HomeEvent.OnCategoryReorderMoved(
                fromCategoryId = "default_transport",
                toCategoryId = "default_membership"
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCategoryReordering)
        assertEquals(
            listOf("Favorites", "Shopping & Loyalty", "Transport", "Membership"),
            viewModel.uiState.value.categories.take(4).map { category -> category.name }
        )

        viewModel.onEvent(HomeEvent.OnCategoryReorderCancelled)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCategoryReordering)
        assertEquals(expectedDefaultCategoryNames, viewModel.uiState.value.categories.map { it.name })
        assertEquals(0, categoryRepository.updateCategoryOrderCallCount)
    }

    @Test
    fun reorderCategory_failureResetsStateAndEmitsErrorEffect() = runTest {
        val categoryRepository = FakeCategoryRepository(
            failUpdateCategoryOrder = true
        )
        val viewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = FakeSearchHistoryRepository()
        )

        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnCategoryReorderStarted("default_transport"))
        viewModel.onEvent(
            HomeEvent.OnCategoryReorderMoved(
                fromCategoryId = "default_transport",
                toCategoryId = "default_membership"
            )
        )
        advanceUntilIdle()

        val reorderErrorEffect = async { viewModel.effects.first() }
        viewModel.onEvent(HomeEvent.OnCategoryReorderFinished)
        advanceUntilIdle()

        assertEquals(
            HomeEffect.ShowCategoryReorderFailedMessage,
            reorderErrorEffect.await()
        )
        assertFalse(viewModel.uiState.value.isCategoryReordering)
        assertEquals(expectedDefaultCategoryNames, viewModel.uiState.value.categories.map { it.name })
        assertEquals(1, categoryRepository.updateCategoryOrderCallCount)
    }

    @Test
    fun search_expandsLoadsPreviousQueriesAndFiltersCategoriesCardsAndCardNumbers() = runTest {
        val viewModel = HomeViewModel(
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository(
                initialCards = listOf(
                    walletCard(
                        id = "card-access",
                        name = "Campus Pass",
                        categoryId = "default_access",
                        cardNumber = "ACCESS-001",
                        position = 0
                    ),
                    walletCard(
                        id = "card-loyalty",
                        name = "Rewards Gold",
                        categoryId = "default_shopping_loyalty",
                        cardNumber = "12345",
                        position = 1
                    )
                )
            ),
            searchHistoryRepository = FakeSearchHistoryRepository(
                initialHistory = listOf(
                    searchHistoryEntry(id = 2L, query = "membership"),
                    searchHistoryEntry(id = 1L, query = "campus")
                )
            )
        )

        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnSearchClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSearchExpanded)
        assertEquals(listOf("membership", "campus"), viewModel.uiState.value.previousSearches.map { it.query })

        viewModel.onEvent(HomeEvent.OnSearchQueryChanged("access"))
        advanceUntilIdle()
        assertEquals(listOf("Access"), viewModel.uiState.value.searchResultCategories.map { it.name })

        viewModel.onEvent(HomeEvent.OnSearchQueryChanged("Campus"))
        advanceUntilIdle()
        assertEquals(listOf("Campus Pass"), viewModel.uiState.value.searchResultCards.map { it.name })

        viewModel.onEvent(HomeEvent.OnSearchQueryChanged("12345"))
        advanceUntilIdle()
        assertEquals(listOf("Rewards Gold"), viewModel.uiState.value.searchResultCards.map { it.name })

        viewModel.onEvent(HomeEvent.OnSearchClosed)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSearchExpanded)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun navigation_hasNoSeparateSearchRoute() {
        val routeValues = HomeRoutes::class.java.declaredFields
            .filterNot { field -> field.isSynthetic }
            .filter { field ->
                Modifier.isStatic(field.modifiers) &&
                    field.type == String::class.java
            }
            .map { field -> field.get(null) as String }

        assertTrue(routeValues.none { route ->
            route.contains("search", ignoreCase = true)
        })
    }

    @Test
    fun search_submitAndPreviousSearchClick_persistAndReuseQueries() = runTest {
        val searchHistoryRepository = FakeSearchHistoryRepository()
        val viewModel = HomeViewModel(
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository(),
            searchHistoryRepository = searchHistoryRepository
        )

        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.OnSearchClicked)
        viewModel.onEvent(HomeEvent.OnSearchQueryChanged("  library  "))
        viewModel.onEvent(HomeEvent.OnSearchSubmitted)
        advanceUntilIdle()

        assertEquals(listOf("library"), searchHistoryRepository.savedQueries)
        assertEquals("library", viewModel.uiState.value.previousSearches.first().query)

        viewModel.onEvent(HomeEvent.OnPreviousSearchClicked("Tickets"))
        advanceUntilIdle()

        assertEquals("Tickets", viewModel.uiState.value.searchQuery)
        assertEquals(listOf("library", "Tickets"), searchHistoryRepository.savedQueries)
        assertEquals("Tickets", viewModel.uiState.value.previousSearches.first().query)
    }

    @Test
    fun events_openExpectedHomeFlowsWithoutAnyAddCardPath() = runTest {
        val searchHistoryRepository = FakeSearchHistoryRepository()
        val viewModel = HomeViewModel(
            categoryRepository = FakeCategoryRepository(),
            cardRepository = FakeCardRepository(
                initialCards = listOf(
                    walletCard(
                        id = "card-access",
                        name = "Campus Pass",
                        categoryId = "default_access",
                        cardNumber = "ACCESS-001",
                        position = 0
                    )
                )
            ),
            searchHistoryRepository = searchHistoryRepository
        )
        advanceUntilIdle()

        val newCategoryEffect = async { viewModel.effects.first() }
        viewModel.onEvent(HomeEvent.OnNewCategoryClicked)
        advanceUntilIdle()
        assertEquals(HomeEffect.OpenCreateCategory, newCategoryEffect.await())

        val addCategoryEffect = async { viewModel.effects.first() }
        viewModel.onEvent(HomeEvent.OnAddCategoryClicked)
        advanceUntilIdle()
        assertEquals(HomeEffect.OpenCreateCategory, addCategoryEffect.await())

        val favoritesCategoryId = viewModel.uiState.value.categories.first().id
        val openCategoryEffect = async { viewModel.effects.first() }
        viewModel.onEvent(HomeEvent.OnCategoryClicked(favoritesCategoryId))
        advanceUntilIdle()
        assertEquals(
            HomeEffect.OpenCategoryDetails(favoritesCategoryId),
            openCategoryEffect.await()
        )

        viewModel.onEvent(HomeEvent.OnSearchClicked)
        viewModel.onEvent(HomeEvent.OnSearchQueryChanged("Campus"))
        advanceUntilIdle()

        val openFromCardResultEffect = async { viewModel.effects.first() }
        viewModel.onEvent(HomeEvent.OnCardSearchResultClicked("card-access"))
        advanceUntilIdle()
        assertEquals(
            HomeEffect.OpenCardDetails("card-access"),
            openFromCardResultEffect.await()
        )
        assertEquals(listOf("Campus"), searchHistoryRepository.savedQueries)

        val stateFieldNames = HomeUiState::class.java.declaredFields
            .filterNot { field ->
                field.isSynthetic || Modifier.isStatic(field.modifiers)
            }
            .map { field -> field.name }
            .toSet()

        assertEquals(
            setOf(
                "isLoading",
                "categories",
                "isCategoryReordering",
                "isSearchExpanded",
                "searchQuery",
                "previousSearches",
                "searchResultCategories",
                "searchResultCards"
            ),
            stateFieldNames
        )
        assertTrue(stateFieldNames.none { it.contains("filter", ignoreCase = true) })
        assertTrue(stateFieldNames.none { it.contains("tag", ignoreCase = true) })
        assertTrue(stateFieldNames.none { it.contains("nearby", ignoreCase = true) })
        assertTrue(stateFieldNames.none { it.contains("recentlyUsed", ignoreCase = true) })
        assertTrue(stateFieldNames.none { it.contains("addCard", ignoreCase = true) })
    }

    private class FakeCategoryRepository(
        initialCategories: List<CategoryWithCardCount> = emptyList(),
        private val failUpdateCategoryOrder: Boolean = false
    ) : CategoryRepository {
        private val categoriesFlow = MutableStateFlow(initialCategories)

        var ensureDefaultCategoriesCallCount: Int = 0
            private set
        var updateCategoryOrderCallCount: Int = 0
            private set
        var lastUpdatedOrder: List<String> = emptyList()
            private set

        override fun observeCategories() = categoriesFlow.map { categories ->
            categories.map { it.category }.filterNot { category -> category.isFavorites }
        }

        override fun observeCategoriesWithCardCounts() = categoriesFlow

        override fun observeCategory(categoryId: String) = categoriesFlow.map { categories ->
            categories.firstOrNull { it.category.id == categoryId }?.category
        }

        override suspend fun ensureDefaultCategories() {
            ensureDefaultCategoriesCallCount += 1
            val existingById = categoriesFlow.value.associateBy { it.category.id }
            val normalizedDefaults = defaultCategoriesWithCounts().map { defaultCategory ->
                existingById[defaultCategory.category.id] ?: defaultCategory
            }
            val normalizedCustomCategories = categoriesFlow.value
                .filterNot { category ->
                    expectedDefaultCategoryIds.contains(category.category.id)
                }
                .mapIndexed { index, category ->
                    category.copy(
                        category = category.category.copy(
                            position = expectedDefaultCategoryNames.size + index
                        )
                    )
                }

            categoriesFlow.value = normalizedDefaults + normalizedCustomCategories
        }

        override suspend fun createCustomCategory(
            name: String,
            color: String
        ): Category {
            error("Not needed for HomeViewModel tests")
        }

        override suspend fun upsertCategory(category: Category) {
            error("Not needed for HomeViewModel tests")
        }

        override suspend fun upsertCategories(categories: List<Category>) {
            error("Not needed for HomeViewModel tests")
        }

        override suspend fun updateCategoryOrder(categoryIdsInOrder: List<String>) {
            updateCategoryOrderCallCount += 1
            if (failUpdateCategoryOrder) {
                throw IllegalStateException("Unable to persist category order.")
            }
            lastUpdatedOrder = categoryIdsInOrder.filterNot(FavoritesCategory::isVirtual)

            val favoritesId = categoriesFlow.value.firstOrNull { category ->
                category.category.isFavorites
            }?.category?.id
            require(favoritesId == null || categoryIdsInOrder.firstOrNull() == favoritesId) {
                "Favorites category must remain first."
            }

            val categoriesById = categoriesFlow.value.associateBy { category ->
                category.category.id
            }
            val favoritesCategory = favoritesId?.let(categoriesById::get)
            val reorderedPersistedCategories = lastUpdatedOrder.mapIndexedNotNull { index, categoryId ->
                categoriesById[categoryId]?.copy(
                    category = categoriesById.getValue(categoryId).category.copy(
                        position = index
                    )
                )
            }
            categoriesFlow.value =
                listOfNotNull(favoritesCategory) + reorderedPersistedCategories
        }

        override suspend fun deleteCategory(categoryId: String) {
            error("Not needed for HomeViewModel tests")
        }
    }

    private class FakeCardRepository(
        initialCards: List<WalletCard> = emptyList()
    ) : CardRepository {
        private val cardsFlow = MutableStateFlow(initialCards)

        override fun observeAllCards(): Flow<List<WalletCard>> = cardsFlow

        override fun observeCards(categoryId: String): Flow<List<WalletCard>> = cardsFlow.map { cards ->
            cards.filter { card -> card.categoryId == categoryId }
        }

        override fun observeCard(cardId: String): Flow<WalletCard?> = cardsFlow.map { cards ->
            cards.firstOrNull { card -> card.id == cardId }
        }

        override suspend fun upsertCard(card: WalletCard) {
            error("Not needed for HomeViewModel tests")
        }

        override suspend fun upsertCards(cards: List<WalletCard>) {
            error("Not needed for HomeViewModel tests")
        }

        override suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>) {
            error("Not needed for HomeViewModel tests")
        }

        override suspend fun deleteCard(cardId: String) {
            error("Not needed for HomeViewModel tests")
        }
    }

    private class FakeSearchHistoryRepository(
        initialHistory: List<SearchHistoryEntry> = emptyList()
    ) : SearchHistoryRepository {
        private val historyFlow = MutableStateFlow(initialHistory)
        private var nextId: Long = (initialHistory.maxOfOrNull { entry -> entry.id } ?: 0L) + 1L

        val savedQueries = mutableListOf<String>()

        override fun observeSearchHistory(limit: Int): Flow<List<SearchHistoryEntry>> = historyFlow.map { history ->
            history.take(limit.coerceAtLeast(1))
        }

        override suspend fun saveQuery(query: String) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                return
            }

            savedQueries += normalizedQuery
            historyFlow.value =
                listOf(
                    SearchHistoryEntry(
                        id = nextId++,
                        query = normalizedQuery,
                        createdAt = fixedTimestamp
                    )
                ) + historyFlow.value.filterNot { entry ->
                    entry.query == normalizedQuery
                }
        }

        override suspend fun clearSearchHistory() {
            historyFlow.value = emptyList()
        }
    }

    private companion object {
        val fixedTimestamp: Instant = Instant.parse("2026-03-13T10:00:00Z")

        val expectedDefaultCategoryIds = listOf(
            FavoritesCategory.id,
            "default_shopping_loyalty",
            "default_membership",
            "default_transport",
            "default_tickets",
            "default_vouchers",
            "default_access",
            "default_library",
            "default_other"
        )

        val expectedDefaultCategoryNames = listOf(
            "Favorites",
            "Shopping & Loyalty",
            "Membership",
            "Transport",
            "Tickets",
            "Vouchers",
            "Access",
            "Library",
            "Other"
        )

        fun defaultCategoriesWithCounts(
            overrides: Map<String, Int> = emptyMap()
        ): List<CategoryWithCardCount> = listOf(
            categoryWithCount(FavoritesCategory.id, "Favorites", 0, "#F59E0B", overrides[FavoritesCategory.id] ?: 0, true, true),
            categoryWithCount("default_shopping_loyalty", "Shopping & Loyalty", 1, "#2563EB", overrides["default_shopping_loyalty"] ?: 0, true, false),
            categoryWithCount("default_membership", "Membership", 2, "#A855F7", overrides["default_membership"] ?: 0, true, false),
            categoryWithCount("default_transport", "Transport", 3, "#0891B2", overrides["default_transport"] ?: 0, true, false),
            categoryWithCount("default_tickets", "Tickets", 4, "#DC2626", overrides["default_tickets"] ?: 0, true, false),
            categoryWithCount("default_vouchers", "Vouchers", 5, "#F97316", overrides["default_vouchers"] ?: 0, true, false),
            categoryWithCount("default_access", "Access", 6, "#16A34A", overrides["default_access"] ?: 0, true, false),
            categoryWithCount("default_library", "Library", 7, "#4F46E5", overrides["default_library"] ?: 0, true, false),
            categoryWithCount("default_other", "Other", 8, "#475569", overrides["default_other"] ?: 0, true, false)
        )

        fun categoryWithCount(
            id: String,
            name: String,
            position: Int,
            color: String,
            cardCount: Int,
            isDefault: Boolean,
            isFavorites: Boolean
        ): CategoryWithCardCount =
            CategoryWithCardCount(
                category = Category(
                    id = id,
                    name = name,
                    color = color,
                    isDefault = isDefault,
                    isFavorites = isFavorites,
                    position = position,
                    createdAt = fixedTimestamp,
                    updatedAt = fixedTimestamp
                ),
                cardCount = cardCount
            )

        fun walletCard(
            id: String,
            name: String,
            categoryId: String,
            cardNumber: String?,
            position: Int
        ): WalletCard =
            WalletCard(
                id = id,
                name = name,
                categoryId = categoryId,
                codeValue = "CODE-$id",
                codeType = CardCodeType.QR_CODE,
                cardNumber = cardNumber,
                expirationDate = null,
                notes = null,
                isFavorite = false,
                position = position,
                createdAt = fixedTimestamp,
                updatedAt = fixedTimestamp
            )

        fun searchHistoryEntry(
            id: Long,
            query: String
        ): SearchHistoryEntry =
            SearchHistoryEntry(
                id = id,
                query = query,
                createdAt = fixedTimestamp
            )
    }
}
