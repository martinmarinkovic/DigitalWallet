package com.threemdroid.digitalwallet.data.searchhistory

import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun observeSearchHistory(limit: Int = DEFAULT_HISTORY_LIMIT): Flow<List<SearchHistoryEntry>>

    suspend fun saveQuery(query: String)

    suspend fun clearSearchHistory()

    companion object {
        const val DEFAULT_HISTORY_LIMIT = 10
    }
}
