package com.threemdroid.digitalwallet.data.searchhistory

import com.threemdroid.digitalwallet.core.database.dao.SearchHistoryDao
import com.threemdroid.digitalwallet.core.database.entity.SearchHistoryEntity
import com.threemdroid.digitalwallet.core.database.mapper.asExternalModel
import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstSearchHistoryRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun observeSearchHistory(limit: Int): Flow<List<SearchHistoryEntry>> {
        val safeLimit = limit.coerceAtLeast(1)
        return searchHistoryDao.observeSearchHistory(safeLimit).map { entries ->
            entries.map { it.asExternalModel() }
        }
    }

    override suspend fun saveQuery(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return
        }

        searchHistoryDao.deleteByQuery(normalizedQuery)
        searchHistoryDao.insertSearchQuery(
            SearchHistoryEntity(
                query = normalizedQuery,
                createdAt = Instant.now()
            )
        )
        searchHistoryDao.trimToLimit(SearchHistoryRepository.DEFAULT_HISTORY_LIMIT)
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearSearchHistory()
    }
}
