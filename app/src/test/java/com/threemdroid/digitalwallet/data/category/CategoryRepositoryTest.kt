package com.threemdroid.digitalwallet.data.category

import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.card.OfflineFirstCardRepository
import com.threemdroid.digitalwallet.core.database.mapper.asEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryRepositoryTest : BaseRepositoryTest() {
    private val repository by lazy {
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
    fun upsertObserveAndDeleteCategory_persistsCrudState() = runBlocking {
        val initialCategory = category(id = "library", name = "Library", position = 0)
        repository.upsertCategory(initialCategory)

        val created = repository.observeCategory("library").first()
        assertEquals(initialCategory, created)

        val updatedCategory = initialCategory.copy(
            name = "Library Updated",
            color = "#112233"
        )
        repository.upsertCategory(updatedCategory)

        val observedAfterUpdate = repository.observeCategories().first()
        assertEquals(listOf(updatedCategory), observedAfterUpdate)

        repository.deleteCategory(updatedCategory.id)

        assertTrue(repository.observeCategories().first().isEmpty())
        assertEquals(null, repository.observeCategory(updatedCategory.id).first())
    }

    @Test
    fun updateCategoryOrder_persistsNewOrderingAndPositionsWhileIgnoringVirtualFavoritesTile() = runBlocking {
        val firstCategory = category(id = "shopping", name = "Shopping", position = 0)
        val secondCategory = category(id = "membership", name = "Membership", position = 1)
        val thirdCategory = category(id = "transport", name = "Transport", position = 2)

        repository.upsertCategories(listOf(firstCategory, secondCategory, thirdCategory))
        repository.updateCategoryOrder(
            listOf(
                FavoritesCategory.id,
                firstCategory.id,
                thirdCategory.id,
                secondCategory.id
            )
        )

        val reordered = repository.observeCategories().first()
        assertEquals(listOf(firstCategory.id, thirdCategory.id, secondCategory.id), reordered.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.map { it.position })
    }

    @Test
    fun ensureDefaultCategories_seedsRealDefaultsAndPrependsVirtualFavoritesForHome() = runBlocking {
        repository.ensureDefaultCategories()

        val persistedCategories = repository.observeCategories().first()
        val homeCategories = repository.observeCategoriesWithCardCounts().first()

        assertEquals(expectedPersistedDefaultCategoryNames, persistedCategories.map { it.name })
        assertEquals(expectedPersistedDefaultCategoryColors, persistedCategories.map { it.color })
        assertFalse(persistedCategories.any { it.isFavorites })
        assertEquals(expectedHomeDefaultCategoryNames, homeCategories.map { it.category.name })
        assertEquals(FavoritesCategory.id, homeCategories.first().category.id)
    }

    @Test
    fun ensureDefaultCategories_keepsCustomCategoriesAfterDefaultsAndExposesCardCounts() = runBlocking {
        repository.upsertCategory(
            category(
                id = "custom-campus",
                name = "Campus",
                position = 0,
                color = "#123456"
            )
        )
        repository.ensureDefaultCategories()

        val categories = repository.observeCategories().first()
        val ticketsId = categories.first { it.name == "Tickets" }.id

        cardRepository.upsertCards(
            listOf(
                card(id = "favorites-1", categoryId = ticketsId, position = 0, isFavorite = true),
                card(id = "tickets-1", categoryId = ticketsId, position = 1),
                card(id = "tickets-2", categoryId = ticketsId, position = 2)
            )
        )

        val categoriesWithCounts = repository.observeCategoriesWithCardCounts().first()
        val names = categoriesWithCounts.map { it.category.name }
        val countsByName = categoriesWithCounts.associate { it.category.name to it.cardCount }

        assertEquals("Favorites", names.first())
        assertEquals("Shopping & Loyalty", names[1])
        assertTrue(names.containsAll(expectedHomeDefaultCategoryNames))
        assertEquals(1, countsByName.getValue("Favorites"))
        assertEquals(3, countsByName.getValue("Tickets"))
        assertEquals(0, countsByName.getValue("Campus"))
    }

    @Test
    fun createCustomCategory_persistsAtEndWhileKeepingFavoritesFirst() = runBlocking {
        val createdCategory = repository.createCustomCategory(
            name = "  Campus  ",
            color = "#123456"
        )

        val categories = repository.observeCategories().first()
        val homeCategories = repository.observeCategoriesWithCardCounts().first()

        assertEquals(
            expectedPersistedDefaultCategoryNames,
            categories.take(expectedPersistedDefaultCategoryNames.size).map { it.name }
        )
        assertEquals("Campus", createdCategory.name)
        assertEquals("#123456", createdCategory.color)
        assertFalse(createdCategory.isDefault)
        assertFalse(createdCategory.isFavorites)
        assertEquals("Campus", categories.last().name)
        assertEquals(categories.lastIndex, categories.last().position)
        assertEquals("Favorites", homeCategories.first().category.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createCustomCategory_rejectsReservedFavoritesName() {
        runBlocking {
            repository.createCustomCategory(
                name = "Favorites",
                color = "#123456"
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun upsertCategory_rejectsPersistingVirtualFavoritesCategory() = runBlocking {
        repository.upsertCategory(
            category(
                id = FavoritesCategory.id,
                name = "Favorites",
                position = 0,
                isDefault = true,
                isFavorites = true
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun deleteCategory_rejectsVirtualFavoritesCategory() = runBlocking {
        repository.deleteCategory(FavoritesCategory.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun deleteCategory_rejectsDefaultStoredCategory() = runBlocking {
        repository.ensureDefaultCategories()
        repository.deleteCategory("default_access")
    }

    @Test(expected = IllegalArgumentException::class)
    fun deleteCategory_rejectsNonEmptyCustomCategory() = runBlocking {
        repository.upsertCategory(
            category(id = "campus", name = "Campus", position = 0, isDefault = false)
        )
        cardRepository.upsertCard(
            card(id = "campus-card", categoryId = "campus", position = 0)
        )

        repository.deleteCategory("campus")
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateCategoryOrder_rejectsMovingFavoritesAwayFromFirstPosition() = runBlocking {
        val firstCategory = category(id = "shopping", name = "Shopping", position = 0)
        val secondCategory = category(id = "membership", name = "Membership", position = 1)
        val thirdCategory = category(id = "transport", name = "Transport", position = 2)

        repository.upsertCategories(listOf(firstCategory, secondCategory, thirdCategory))

        repository.updateCategoryOrder(
            listOf(
                secondCategory.id,
                FavoritesCategory.id,
                firstCategory.id,
                thirdCategory.id
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateCategoryOrder_rejectsDuplicateIds() = runBlocking {
        val firstCategory = category(id = "shopping", name = "Shopping", position = 0)
        val secondCategory = category(id = "membership", name = "Membership", position = 1)

        repository.upsertCategories(listOf(firstCategory, secondCategory))

        repository.updateCategoryOrder(
            listOf(
                FavoritesCategory.id,
                firstCategory.id,
                secondCategory.id,
                secondCategory.id
            )
        )
    }

    @Test
    fun ensureDefaultCategories_preservesExistingReorderedRealCategories() = runBlocking {
        repository.ensureDefaultCategories()

        val currentCategories = repository.observeCategories().first()
        val transportId = currentCategories.first { it.name == "Transport" }.id
        val membershipId = currentCategories.first { it.name == "Membership" }.id
        val remainingIds = currentCategories.map { category -> category.id }.filterNot { categoryId ->
            categoryId == transportId || categoryId == membershipId
        }

        repository.updateCategoryOrder(
            listOf(FavoritesCategory.id, transportId, membershipId) + remainingIds
        )
        repository.ensureDefaultCategories()

        val reorderedCategories = repository.observeCategories().first()
        assertEquals(listOf(transportId, membershipId), reorderedCategories.take(2).map { it.id })
        assertEquals(
            FavoritesCategory.id,
            repository.observeCategoriesWithCardCounts().first().first().category.id
        )
    }

    @Test
    fun ensureDefaultCategories_migratesLegacyStoredFavoritesIntoVirtualFavoritesAndRealCategoryMembership() = runBlocking {
        val legacyFavorites = category(
            id = "default_favorites",
            name = "Favorites",
            position = 0,
            isDefault = true,
            isFavorites = true
        )
        val transport = category(
            id = "default_transport",
            name = "Transport",
            position = 1,
            isDefault = true
        )
        database.categoryDao().upsertCategories(
            listOf(legacyFavorites.asEntity(), transport.asEntity())
        )
        database.cardDao().upsertCard(
            card(
                id = "legacy-favorite-card",
                categoryId = legacyFavorites.id,
                position = 0,
                isFavorite = false
            ).asEntity()
        )

        repository.ensureDefaultCategories()

        val persistedCategories = repository.observeCategories().first()
        val migratedCard = cardRepository.observeCard("legacy-favorite-card").first()
        val homeCategories = repository.observeCategoriesWithCardCounts().first()

        assertNull(persistedCategories.firstOrNull { it.id == legacyFavorites.id })
        assertTrue(persistedCategories.none { it.isFavorites })
        assertEquals("default_other", migratedCard?.categoryId)
        assertTrue(migratedCard?.isFavorite == true)
        assertEquals(FavoritesCategory.id, homeCategories.first().category.id)
        assertEquals(1, homeCategories.first().cardCount)
    }

    private companion object {
        val expectedPersistedDefaultCategoryNames = listOf(
            "Shopping & Loyalty",
            "Membership",
            "Transport",
            "Tickets",
            "Vouchers",
            "Access",
            "Library",
            "Other"
        )

        val expectedHomeDefaultCategoryNames = listOf(
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

        val expectedPersistedDefaultCategoryColors = listOf(
            "#2563EB",
            "#A855F7",
            "#0891B2",
            "#DC2626",
            "#F97316",
            "#16A34A",
            "#4F46E5",
            "#475569"
        )
    }
}
