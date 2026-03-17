package com.threemdroid.digitalwallet.feature.carddetails

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.card.OfflineFirstCardRepository
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
class CardDetailsIntegrationTest : BaseRepositoryTest() {
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
    fun realRepositories_loadCardPersistFavoriteToggleAndDelete() = runTest {
        categoryRepository.upsertCategory(
            category(
                id = "default_library",
                name = "Library",
                position = 7,
                isDefault = true
            )
        )
        cardRepository.upsertCard(
            card(
                id = "card_library",
                categoryId = "default_library",
                position = 0,
                name = "City Library",
                cardNumber = "LIB-7788",
                notes = "Lower level pickup",
                isFavorite = false
            )
        )

        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "card_library")
            ),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        val initialState = viewModel.uiState.first { state ->
            !state.isLoading
        }
        assertFalse(initialState.isCardMissing)
        assertEquals("City Library", initialState.title)
        assertEquals("Library", initialState.categoryName)
        assertEquals("LIB-7788", initialState.cardNumber)
        assertEquals("Lower level pickup", initialState.notes)
        assertFalse(initialState.isFavorite)

        viewModel.onEvent(CardDetailsEvent.OnFavoriteClicked)
        advanceUntilIdle()

        val favoriteState = viewModel.uiState.first { state ->
            state.isFavorite
        }
        assertTrue(favoriteState.isFavorite)

        val updatedCard = cardRepository.observeCard("card_library").first { card ->
            card?.isFavorite == true
        }
        assertTrue(updatedCard?.isFavorite == true)

        viewModel.onEvent(CardDetailsEvent.OnDeleteClicked)
        val deferredEffect = async { viewModel.effects.first() }
        viewModel.onEvent(CardDetailsEvent.OnDeleteConfirmed)
        advanceUntilIdle()

        assertEquals(CardDetailsEffect.NavigateBack, deferredEffect.await())
        val deletedState = viewModel.uiState.first { state ->
            !state.isLoading && state.isCardMissing
        }
        assertTrue(deletedState.isCardMissing)
        assertNull(cardRepository.observeCard("card_library").first())
    }

    @Test
    fun realRepositories_loadPersistedExpirationAndOptionalFields() = runTest {
        categoryRepository.upsertCategory(
            category(
                id = "default_membership",
                name = "Membership",
                position = 2,
                isDefault = true
            )
        )
        cardRepository.upsertCard(
            card(
                id = "card_membership",
                categoryId = "default_membership",
                position = 0,
                name = "Gym Club",
                cardNumber = "MEM-4455",
                expirationDate = LocalDate.parse("2026-09-15"),
                notes = "Front desk check-in",
                isFavorite = true
            )
        )

        val viewModel = CardDetailsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(CardDetailsRoutes.cardIdArg to "card_membership")
            ),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.first { uiState ->
            !uiState.isLoading
        }
        assertEquals("Gym Club", state.title)
        assertEquals("Membership", state.categoryName)
        assertEquals("MEM-4455", state.cardNumber)
        assertEquals("2026-09-15", state.expirationDate)
        assertEquals("Front desk check-in", state.notes)
        assertTrue(state.isFavorite)
    }
}
