package com.threemdroid.digitalwallet.feature.addcard

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class EditCardFlowIntegrationTest : BaseRepositoryTest() {
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
    fun editCard_updatesFields_movesCategory_andCompactsSourceOrdering() = runTest {
        categoryRepository.upsertCategories(
            listOf(
                category(id = "access", name = "Access", position = 0),
                category(id = "membership", name = "Membership", position = 1)
            )
        )
        cardRepository.upsertCards(
            listOf(
                card(
                    id = "card-1",
                    categoryId = "access",
                    position = 0,
                    name = "Office Badge",
                    cardNumber = "ACC-001",
                    notes = "Old note"
                ),
                card(
                    id = "card-2",
                    categoryId = "access",
                    position = 1,
                    name = "Visitor Badge"
                ),
                card(
                    id = "card-3",
                    categoryId = "membership",
                    position = 0,
                    name = "Gym Club"
                )
            )
        )

        val viewModel = ManualEntryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ManualEntryRoutes.cardIdArg to "card-1")
            ),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = viewModel.uiState.first { state ->
            !state.isLoading
        }
        assertFalse(initialState.isCardMissing)
        assertEquals("Office Badge", initialState.cardName)
        assertEquals("access", initialState.selectedCategoryId)

        viewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Updated Membership Card"))
        viewModel.onEvent(ManualEntryEvent.OnCategorySelected("membership"))
        viewModel.onEvent(ManualEntryEvent.OnCodeTypeSelected(com.threemdroid.digitalwallet.core.model.CardCodeType.CODE_128))
        viewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("MEM-999"))
        viewModel.onEvent(ManualEntryEvent.OnCardNumberChanged("MEM-123"))
        viewModel.onEvent(ManualEntryEvent.OnExpirationDateChanged("2026-12-31"))
        viewModel.onEvent(ManualEntryEvent.OnNotesChanged("Updated note"))
        viewModel.onEvent(ManualEntryEvent.OnFavoriteChanged(true))
        val savedEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryEffect.NavigateBack, savedEffect.await())

        val movedCard = cardRepository.observeCard("card-1").first()
        assertEquals("Updated Membership Card", movedCard?.name)
        assertEquals("membership", movedCard?.categoryId)
        assertEquals(com.threemdroid.digitalwallet.core.model.CardCodeType.CODE_128, movedCard?.codeType)
        assertEquals("MEM-999", movedCard?.codeValue)
        assertEquals("MEM-123", movedCard?.cardNumber)
        assertEquals(LocalDate.parse("2026-12-31"), movedCard?.expirationDate)
        assertEquals("Updated note", movedCard?.notes)
        assertTrue(movedCard?.isFavorite == true)
        assertEquals(1, movedCard?.position)

        val sourceCards = cardRepository.observeCards("access").first()
        assertEquals(listOf("card-2"), sourceCards.map { card -> card.id })
        assertEquals(listOf(0), sourceCards.map { card -> card.position })

        val destinationCards = cardRepository.observeCards("membership").first()
        assertEquals(listOf("card-3", "card-1"), destinationCards.map { card -> card.id })
        assertEquals(listOf(0, 1), destinationCards.map { card -> card.position })
    }
}
