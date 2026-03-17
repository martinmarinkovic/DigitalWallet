package com.threemdroid.digitalwallet.data.card

import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardRepositoryTest : BaseRepositoryTest() {
    private val categoryRepository by lazy {
        OfflineFirstCategoryRepository(
            database = database,
            categoryDao = database.categoryDao()
        )
    }
    private val repository by lazy {
        OfflineFirstCardRepository(
            database = database,
            cardDao = database.cardDao(),
            categoryDao = database.categoryDao()
        )
    }

    @Test
    fun upsertObserveAndDeleteCard_persistsCrudState() = runBlocking {
        val category = category(id = "membership", name = "Membership", position = 0)
        categoryRepository.upsertCategory(category)

        val initialCard = card(id = "card-1", categoryId = category.id, position = 0)
        repository.upsertCard(initialCard)

        assertEquals(initialCard, repository.observeCard(initialCard.id).first())
        assertEquals(listOf(initialCard), repository.observeCards(category.id).first())

        val updatedCard = initialCard.copy(
            name = "Updated Card",
            notes = "Updated notes",
            isFavorite = true
        )
        repository.upsertCard(updatedCard)

        assertEquals(updatedCard, repository.observeCard(updatedCard.id).first())

        repository.deleteCard(updatedCard.id)

        assertTrue(repository.observeCards(category.id).first().isEmpty())
        assertEquals(null, repository.observeCard(updatedCard.id).first())
    }

    @Test
    fun updateCardOrder_persistsNewOrderingAndPositions() = runBlocking {
        val category = category(id = "tickets", name = "Tickets", position = 0)
        categoryRepository.upsertCategory(category)

        val firstCard = card(id = "card-1", categoryId = category.id, position = 0)
        val secondCard = card(id = "card-2", categoryId = category.id, position = 1)
        val thirdCard = card(id = "card-3", categoryId = category.id, position = 2)
        repository.upsertCards(listOf(firstCard, secondCard, thirdCard))

        repository.updateCardOrder(category.id, listOf(thirdCard.id, firstCard.id, secondCard.id))

        val reordered = repository.observeCards(category.id).first()
        assertEquals(listOf(thirdCard.id, firstCard.id, secondCard.id), reordered.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.map { it.position })
    }

    @Test
    fun updateCardOrder_isScopedToRequestedCategoryOnly() = runBlocking {
        val ticketsCategory = category(id = "tickets", name = "Tickets", position = 0)
        val accessCategory = category(id = "access", name = "Access", position = 1)
        categoryRepository.upsertCategories(listOf(ticketsCategory, accessCategory))

        val ticketsFirstCard = card(id = "ticket-1", categoryId = ticketsCategory.id, position = 0)
        val ticketsSecondCard = card(id = "ticket-2", categoryId = ticketsCategory.id, position = 1)
        val accessFirstCard = card(id = "access-1", categoryId = accessCategory.id, position = 0)
        val accessSecondCard = card(id = "access-2", categoryId = accessCategory.id, position = 1)
        repository.upsertCards(
            listOf(
                ticketsFirstCard,
                ticketsSecondCard,
                accessFirstCard,
                accessSecondCard
            )
        )

        repository.updateCardOrder(
            ticketsCategory.id,
            listOf(ticketsSecondCard.id, ticketsFirstCard.id)
        )

        assertEquals(
            listOf(ticketsSecondCard.id, ticketsFirstCard.id),
            repository.observeCards(ticketsCategory.id).first().map { it.id }
        )
        assertEquals(
            listOf(accessFirstCard.id, accessSecondCard.id),
            repository.observeCards(accessCategory.id).first().map { it.id }
        )
    }

    @Test
    fun upsertEditedCard_afterReorder_keepsPersistedOrderStable() = runBlocking {
        val category = category(id = "library", name = "Library", position = 0)
        categoryRepository.upsertCategory(category)

        val firstCard = card(id = "card-1", categoryId = category.id, position = 0)
        val secondCard = card(id = "card-2", categoryId = category.id, position = 1)
        val thirdCard = card(id = "card-3", categoryId = category.id, position = 2)
        repository.upsertCards(listOf(firstCard, secondCard, thirdCard))

        repository.updateCardOrder(category.id, listOf(thirdCard.id, firstCard.id, secondCard.id))

        repository.upsertCard(
            repository.observeCard(firstCard.id).first()!!.copy(
                name = "Updated Library Card",
                notes = "Edited after reorder",
                isFavorite = true
            )
        )

        val reloadedCards = repository.observeCards(category.id).first()
        assertEquals(
            listOf(thirdCard.id, firstCard.id, secondCard.id),
            reloadedCards.map { it.id }
        )
        assertEquals(listOf(0, 1, 2), reloadedCards.map { it.position })
        assertEquals("Updated Library Card", reloadedCards[1].name)
        assertEquals("Edited after reorder", reloadedCards[1].notes)
        assertTrue(reloadedCards[1].isFavorite)
    }

    @Test
    fun upsertEditedCard_withNewCategory_persistsMoveAndUpdatedFields() = runBlocking {
        val accessCategory = category(id = "access", name = "Access", position = 0)
        val membershipCategory = category(id = "membership", name = "Membership", position = 1)
        categoryRepository.upsertCategories(listOf(accessCategory, membershipCategory))

        val originalCard = card(
            id = "card-1",
            categoryId = accessCategory.id,
            position = 0,
            name = "Office Badge",
            codeValue = "ACCESS-001"
        )
        repository.upsertCard(originalCard)

        val movedCard = originalCard.copy(
            categoryId = membershipCategory.id,
            name = "Gym Club",
            codeValue = "MEM-999",
            codeType = com.threemdroid.digitalwallet.core.model.CardCodeType.CODE_128,
            cardNumber = "MEM-123",
            notes = "Updated after move",
            isFavorite = true,
            position = 1
        )
        repository.upsertCard(movedCard)

        assertTrue(repository.observeCards(accessCategory.id).first().isEmpty())
        val destinationCards = repository.observeCards(membershipCategory.id).first()
        assertEquals(listOf("card-1"), destinationCards.map { it.id })
        assertEquals("Gym Club", destinationCards.single().name)
        assertEquals("MEM-999", destinationCards.single().codeValue)
        assertEquals("MEM-123", destinationCards.single().cardNumber)
        assertEquals("Updated after move", destinationCards.single().notes)
        assertTrue(destinationCards.single().isFavorite)
        assertEquals(1, destinationCards.single().position)
    }

    @Test(expected = IllegalArgumentException::class)
    fun upsertCard_rejectsMissingCategory() = runBlocking {
        repository.upsertCard(card(id = "orphan", categoryId = "missing-category", position = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateCardOrder_rejectsDuplicateIds() = runBlocking {
        val category = category(id = "access", name = "Access", position = 0)
        categoryRepository.upsertCategory(category)

        val firstCard = card(id = "card-1", categoryId = category.id, position = 0)
        val secondCard = card(id = "card-2", categoryId = category.id, position = 1)
        repository.upsertCards(listOf(firstCard, secondCard))

        repository.updateCardOrder(category.id, listOf(firstCard.id, secondCard.id, secondCard.id))
    }
}
