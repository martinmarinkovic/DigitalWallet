package com.threemdroid.digitalwallet.data.searchhistory

import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchHistoryRepositoryTest : BaseRepositoryTest() {
    private val repository by lazy {
        OfflineFirstSearchHistoryRepository(database.searchHistoryDao())
    }

    @Test
    fun saveQuery_persistsMostRecentUniqueHistoryAndClearRemovesAll() = runBlocking {
        repository.saveQuery("shopping")
        repository.saveQuery("membership")
        repository.saveQuery("shopping")
        repository.saveQuery("   ")

        val history = repository.observeSearchHistory().first()

        assertEquals(listOf("shopping", "membership"), history.map { it.query })

        repository.clearSearchHistory()

        assertTrue(repository.observeSearchHistory().first().isEmpty())
    }

    @Test
    fun saveQuery_trimsHistoryToConfiguredLimit() = runBlocking {
        repeat(12) { index ->
            repository.saveQuery("query-$index")
        }

        val history = repository.observeSearchHistory().first()

        assertEquals(SearchHistoryRepository.DEFAULT_HISTORY_LIMIT, history.size)
        assertEquals("query-11", history.first().query)
        assertEquals("query-2", history.last().query)
    }
}
