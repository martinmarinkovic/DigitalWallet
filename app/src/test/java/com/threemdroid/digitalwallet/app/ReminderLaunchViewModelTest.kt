package com.threemdroid.digitalwallet.app

import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderLaunchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun resolveReminderDestination_returnsCardDetailsWhenCardExists() = runTest {
        val viewModel = ReminderLaunchViewModel(
            cardRepository = FakeCardRepository(
                initialCards = listOf(walletCard(id = "card-1"))
            )
        )

        val destination = viewModel.resolveReminderDestination("card-1")

        assertEquals(
            ReminderLaunchDestination.CardDetails("card-1"),
            destination
        )
    }

    @Test
    fun resolveReminderDestination_returnsHomeWhenCardMissing() = runTest {
        val viewModel = ReminderLaunchViewModel(
            cardRepository = FakeCardRepository()
        )

        val destination = viewModel.resolveReminderDestination("missing-card")

        assertEquals(ReminderLaunchDestination.Home, destination)
    }

    private class FakeCardRepository(
        initialCards: List<WalletCard> = emptyList()
    ) : CardRepository {
        private val cardsFlow = MutableStateFlow(initialCards)

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
            error("Not needed for ReminderLaunchViewModelTest")
        }

        override suspend fun upsertCards(cards: List<WalletCard>) {
            error("Not needed for ReminderLaunchViewModelTest")
        }

        override suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>) {
            error("Not needed for ReminderLaunchViewModelTest")
        }

        override suspend fun deleteCard(cardId: String) {
            error("Not needed for ReminderLaunchViewModelTest")
        }
    }

    private companion object {
        val fixedTimestamp: Instant = Instant.parse("2026-03-16T12:00:00Z")

        fun walletCard(id: String): WalletCard =
            WalletCard(
                id = id,
                name = "Card $id",
                categoryId = "default_access",
                codeValue = "CODE-$id",
                codeType = CardCodeType.QR_CODE,
                cardNumber = null,
                expirationDate = null,
                notes = null,
                isFavorite = false,
                position = 0,
                createdAt = fixedTimestamp,
                updatedAt = fixedTimestamp
            )
    }
}
