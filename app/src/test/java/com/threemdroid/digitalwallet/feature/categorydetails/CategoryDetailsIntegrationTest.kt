package com.threemdroid.digitalwallet.feature.categorydetails

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.card.OfflineFirstCardRepository
import com.threemdroid.digitalwallet.data.category.FavoritesCategory
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailsIntegrationTest : BaseRepositoryTest() {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryRepository by lazy {
        OfflineFirstCategoryRepository(
            database = database,
            categoryDao = database.categoryDao()
        )
    }

    private val cardRepository by lazy {
        OfflineFirstCardRepository(
            database = database,
            cardDao = database.cardDao(),
            categoryDao = database.categoryDao()
        )
    }

    @Test
    fun realRepositories_virtualFavoritesReflectFavoriteFlagAcrossRealCategories() = runTest {
        categoryRepository.ensureDefaultCategories()
        cardRepository.upsertCards(
            listOf(
                card(
                    id = "favorite-library",
                    categoryId = "default_library",
                    position = 0,
                    name = "City Library",
                    isFavorite = true
                ),
                card(
                    id = "office-badge",
                    categoryId = "default_access",
                    position = 0,
                    name = "Office Badge",
                    isFavorite = false
                )
            )
        )

        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to FavoritesCategory.id)
            ),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        val initialState = viewModel.uiState.first { uiState ->
            !uiState.isLoading && uiState.cards.map { it.id } == listOf("favorite-library")
        }
        assertEquals("Favorites", initialState.title)
        assertFalse(initialState.isCardReorderEnabled)

        cardRepository.upsertCard(
            cardRepository.observeCard("office-badge").first()!!.copy(isFavorite = true)
        )
        advanceUntilIdle()

        val updatedState = viewModel.uiState.first { uiState ->
            uiState.cards.map { it.id }.toSet() ==
                setOf("favorite-library", "office-badge")
        }
        assertEquals(
            setOf("favorite-library", "office-badge"),
            updatedState.cards.map { it.id }.toSet()
        )
    }

    @Test
    fun realRepositories_loadOnlySelectedCategoryCardsAndMapExpirationStates() = runTest {
        categoryRepository.upsertCategories(
            listOf(
                category(
                    id = "default_library",
                    name = "Library",
                    position = 7,
                    isDefault = true
                ),
                category(
                    id = "default_access",
                    name = "Access",
                    position = 6,
                    isDefault = true
                )
            )
        )
        cardRepository.upsertCards(
            listOf(
                card(
                    id = "card_soon",
                    categoryId = "default_library",
                    position = 0,
                    name = "City Library",
                    expirationDate = LocalDate.now().plusDays(5)
                ),
                card(
                    id = "card_today",
                    categoryId = "default_library",
                    position = 1,
                    name = "Archive Access",
                    expirationDate = LocalDate.now()
                ),
                card(
                    id = "card_far",
                    categoryId = "default_library",
                    position = 2,
                    name = "Reading Club",
                    expirationDate = LocalDate.now().plusDays(30)
                ),
                card(
                    id = "card_other_category",
                    categoryId = "default_access",
                    position = 0,
                    name = "Office Badge",
                    expirationDate = LocalDate.now().minusDays(1)
                )
            )
        )

        val viewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_library")
            ),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.first { uiState ->
            !uiState.isLoading
        }
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals("Library", state.title)
        assertEquals(listOf("card_soon", "card_today", "card_far"), state.cards.map { it.id })
        assertEquals(listOf("City Library", "Archive Access", "Reading Club"), state.cards.map { it.name })
        assertTrue(state.cards.none { it.id == "card_other_category" })
        assertEquals(
            CategoryDetailsExpirationBadgeStatus.EXPIRING_SOON,
            state.cards[0].expirationBadge?.status
        )
        assertEquals(5, state.cards[0].expirationBadge?.daysUntilExpiration)
        assertEquals(
            CategoryDetailsExpirationBadgeStatus.EXPIRES_TODAY,
            state.cards[1].expirationBadge?.status
        )
        assertNull(state.cards[2].expirationBadge)
    }

    @Test
    fun reorderCards_persistsAcrossCategoryDetailsReload() = runTest {
        categoryRepository.upsertCategories(
            listOf(
                category(
                    id = "default_library",
                    name = "Library",
                    position = 7,
                    isDefault = true
                ),
                category(
                    id = "default_access",
                    name = "Access",
                    position = 6,
                    isDefault = true
                )
            )
        )
        cardRepository.upsertCards(
            listOf(
                card(
                    id = "card_library",
                    categoryId = "default_library",
                    position = 0,
                    name = "City Library"
                ),
                card(
                    id = "card_archive",
                    categoryId = "default_library",
                    position = 1,
                    name = "Archive Access"
                ),
                card(
                    id = "card_reading",
                    categoryId = "default_library",
                    position = 2,
                    name = "Reading Club"
                ),
                card(
                    id = "card_other_category",
                    categoryId = "default_access",
                    position = 0,
                    name = "Office Badge"
                )
            )
        )

        val firstViewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_library")
            ),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        val initialState = firstViewModel.uiState.first { uiState ->
            uiState.cards.map { card -> card.id } ==
                listOf("card_library", "card_archive", "card_reading")
        }
        assertEquals(
            listOf("card_library", "card_archive", "card_reading"),
            initialState.cards.map { card -> card.id }
        )

        firstViewModel.onEvent(CategoryDetailsEvent.OnCardReorderStarted("card_reading"))
        firstViewModel.onEvent(
            CategoryDetailsEvent.OnCardReorderMoved(
                fromCardId = "card_reading",
                toCardId = "card_library"
            )
        )
        advanceUntilIdle()

        val inMemoryReorderedState = firstViewModel.uiState.first { uiState ->
            uiState.isCardReordering &&
                uiState.cards.map { card -> card.id } ==
                listOf("card_reading", "card_library", "card_archive")
        }
        assertTrue(inMemoryReorderedState.isCardReordering)
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            inMemoryReorderedState.cards.map { card -> card.id }
        )

        firstViewModel.onEvent(CategoryDetailsEvent.OnCardReorderFinished)
        advanceUntilIdle()

        val persistedState = firstViewModel.uiState.first { uiState ->
            !uiState.isCardReordering &&
                uiState.cards.map { card -> card.id } ==
                listOf("card_reading", "card_library", "card_archive")
        }
        assertFalse(persistedState.isCardReordering)
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            persistedState.cards.map { card -> card.id }
        )

        val persistedCards = cardRepository.observeCards("default_library").first { cards ->
            cards.map { card -> card.id } ==
                listOf("card_reading", "card_library", "card_archive")
        }
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            persistedCards.map { card -> card.id }
        )
        assertEquals(
            listOf("card_other_category"),
            cardRepository.observeCards("default_access").first().map { card -> card.id }
        )

        cardRepository.upsertCard(
            cardRepository.observeCard("card_library").first()!!.copy(
                name = "City Library Updated",
                notes = "Edited after reorder"
            )
        )
        advanceUntilIdle()

        val reorderedAfterEdit = cardRepository.observeCards("default_library").first { cards ->
            cards.map { card -> card.id } ==
                listOf("card_reading", "card_library", "card_archive")
        }
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            reorderedAfterEdit.map { card -> card.id }
        )
        assertEquals("City Library Updated", reorderedAfterEdit[1].name)
        assertEquals("Edited after reorder", reorderedAfterEdit[1].notes)

        val secondViewModel = CategoryDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CategoryDetailsRoutes.categoryIdArg to "default_library")
            ),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )
        advanceUntilIdle()

        val reloadedState = secondViewModel.uiState.first { uiState ->
            uiState.cards.map { card -> card.id } ==
                listOf("card_reading", "card_library", "card_archive")
        }
        assertFalse(reloadedState.isCardReordering)
        assertEquals(
            listOf("card_reading", "card_library", "card_archive"),
            reloadedState.cards.map { card -> card.id }
        )
        assertEquals("City Library Updated", reloadedState.cards[1].name)
    }
}
