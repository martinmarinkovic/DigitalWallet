package com.threemdroid.digitalwallet.feature.home

import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.card.OfflineFirstCardRepository
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import com.threemdroid.digitalwallet.data.searchhistory.OfflineFirstSearchHistoryRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class HomeCategoryReorderIntegrationTest : BaseRepositoryTest() {
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
    private val searchHistoryRepository by lazy {
        OfflineFirstSearchHistoryRepository(database.searchHistoryDao())
    }

    @Test
    fun reorderCategory_persistsAcrossHomeReloadWhileKeepingFavoritesFirst() = runTest {
        val firstHomeViewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = cardRepository,
            searchHistoryRepository = searchHistoryRepository
        )

        advanceUntilIdle()

        val initialState = firstHomeViewModel.uiState.first { state ->
            state.categories.isNotEmpty()
        }
        val initialCategoryOrder = initialState.categories.map { category ->
            category.name
        }
        assertEquals("Favorites", initialCategoryOrder.first())
        assertFalse(initialState.categories.any { it.name == "+ New Category" })

        firstHomeViewModel.onEvent(HomeEvent.OnCategoryReorderStarted("default_transport"))
        firstHomeViewModel.onEvent(
            HomeEvent.OnCategoryReorderMoved(
                fromCategoryId = "default_transport",
                toCategoryId = "default_membership"
            )
        )
        advanceUntilIdle()

        val inMemoryReorderedNames = firstHomeViewModel.uiState.value.categories.map { category ->
            category.name
        }
        assertTrue(firstHomeViewModel.uiState.value.isCategoryReordering)
        assertEquals(
            listOf("Favorites", "Shopping & Loyalty", "Transport", "Membership"),
            inMemoryReorderedNames.take(4)
        )

        firstHomeViewModel.onEvent(HomeEvent.OnCategoryReorderFinished)
        advanceUntilIdle()

        assertFalse(firstHomeViewModel.uiState.value.isCategoryReordering)
        assertEquals(
            listOf("Favorites", "Shopping & Loyalty", "Transport", "Membership"),
            firstHomeViewModel.uiState.value.categories.take(4).map { category -> category.name }
        )
        val persistedCategories = categoryRepository.observeCategories().first { categories ->
            categories.take(3).map { category -> category.name } ==
                listOf("Shopping & Loyalty", "Transport", "Membership")
        }
        assertEquals(
            listOf("Shopping & Loyalty", "Transport", "Membership"),
            persistedCategories.take(3).map { category -> category.name }
        )

        val secondHomeViewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = cardRepository,
            searchHistoryRepository = searchHistoryRepository
        )
        advanceUntilIdle()
        val reloadedState = secondHomeViewModel.uiState.first { state ->
            state.categories.take(4).map { category -> category.name } ==
                listOf("Favorites", "Shopping & Loyalty", "Transport", "Membership")
        }

        assertEquals(
            listOf("Favorites", "Shopping & Loyalty", "Transport", "Membership"),
            reloadedState.categories.take(4).map { category -> category.name }
        )
        assertFalse(reloadedState.categories.any { it.name == "+ New Category" })
    }
}
