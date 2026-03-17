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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HomeSearchIntegrationTest : BaseRepositoryTest() {
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
    fun searchSubmit_persistsPreviousSearchForNewHomeViewModelInstance() = runTest {
        val firstViewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = cardRepository,
            searchHistoryRepository = searchHistoryRepository
        )

        advanceUntilIdle()

        firstViewModel.onEvent(HomeEvent.OnSearchClicked)
        firstViewModel.onEvent(HomeEvent.OnSearchQueryChanged("  library  "))
        firstViewModel.onEvent(HomeEvent.OnSearchSubmitted)
        advanceUntilIdle()

        assertEquals(
            listOf("library"),
            searchHistoryRepository.observeSearchHistory().first { history ->
                history.isNotEmpty()
            }.map { entry -> entry.query }
        )

        val secondViewModel = HomeViewModel(
            categoryRepository = categoryRepository,
            cardRepository = cardRepository,
            searchHistoryRepository = searchHistoryRepository
        )

        val state = secondViewModel.uiState.first { uiState ->
            uiState.previousSearches.isNotEmpty()
        }
        assertFalse(state.isSearchExpanded)
        assertEquals("", state.searchQuery)
        assertEquals(listOf("library"), state.previousSearches.map { entry -> entry.query })
    }
}
