package com.threemdroid.digitalwallet.feature.home

import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.card.OfflineFirstCardRepository
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import com.threemdroid.digitalwallet.data.searchhistory.OfflineFirstSearchHistoryRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
class CreateCategoryFlowIntegrationTest : BaseRepositoryTest() {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
    private val searchHistoryRepository by lazy {
        OfflineFirstSearchHistoryRepository(database.searchHistoryDao())
    }

    @Test
    fun save_withRealRepository_persistsCategoryAndUpdatesHomeOrdering() = runTest {
        val homeViewModel = HomeViewModel(
            categoryRepository = repository,
            cardRepository = cardRepository,
            searchHistoryRepository = searchHistoryRepository
        )
        val createCategoryViewModel = CreateCategoryViewModel(repository)

        advanceUntilIdle()

        createCategoryViewModel.onEvent(CreateCategoryEvent.OnNameChanged("Campus"))
        val dismissEffect = this.backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            createCategoryViewModel.effects.first()
        }

        createCategoryViewModel.onEvent(CreateCategoryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(CreateCategoryEffect.Dismiss, dismissEffect.await())

        val persistedCategories = repository.observeCategories().first()
        val homeCategories = homeViewModel.uiState.first { state ->
            state.categories.lastOrNull()?.name == "Campus"
        }.categories

        assertEquals(expectedDefaultCategoryNames, homeCategories.take(expectedDefaultCategoryNames.size).map { it.name })
        assertEquals("Shopping & Loyalty", persistedCategories.first().name)
        assertEquals("Favorites", homeCategories.first().name)
        assertEquals("Campus", persistedCategories.last().name)
        assertEquals(CreateCategoryDefaults.defaultColorHex, persistedCategories.last().color)
        assertEquals("Campus", homeCategories.last().name)
        assertFalse(homeCategories.any { it.name == "+ New Category" })
    }

    @Test
    fun save_withSelectedColor_persistsColorAndExposesItToHomeTiles() = runTest {
        val homeViewModel = HomeViewModel(
            categoryRepository = repository,
            cardRepository = cardRepository,
            searchHistoryRepository = searchHistoryRepository
        )
        val createCategoryViewModel = CreateCategoryViewModel(repository)

        advanceUntilIdle()

        createCategoryViewModel.onEvent(CreateCategoryEvent.OnNameChanged("Travel Club"))
        createCategoryViewModel.onEvent(CreateCategoryEvent.OnColorSelected("#0891B2"))
        val dismissEffect = this.backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            createCategoryViewModel.effects.first()
        }

        createCategoryViewModel.onEvent(CreateCategoryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(CreateCategoryEffect.Dismiss, dismissEffect.await())

        val persistedCategory = repository.observeCategories().first().last()
        val homeCategory = homeViewModel.uiState.first { state ->
            state.categories.lastOrNull()?.name == "Travel Club"
        }.categories.last()

        assertEquals("#0891B2", persistedCategory.color)
        assertEquals("#0891B2", homeCategory.colorHex)
    }

    private companion object {
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
    }
}
