package com.threemdroid.digitalwallet.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.threemdroid.digitalwallet.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query(
        """
        SELECT * FROM search_history
        ORDER BY created_at DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeSearchHistory(limit: Int): Flow<List<SearchHistoryEntity>>

    @Query(
        """
        SELECT * FROM search_history
        ORDER BY created_at DESC, id DESC
        """
    )
    suspend fun getAllSearchHistory(): List<SearchHistoryEntity>

    @Insert
    suspend fun insertSearchQuery(entry: SearchHistoryEntity): Long

    @Insert
    suspend fun insertSearchHistoryEntries(entries: List<SearchHistoryEntity>)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteByQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    @Query(
        """
        DELETE FROM search_history
        WHERE id NOT IN (
            SELECT id FROM search_history
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
        )
        """
    )
    suspend fun trimToLimit(limit: Int)
}
