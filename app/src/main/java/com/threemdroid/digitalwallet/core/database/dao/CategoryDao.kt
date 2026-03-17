package com.threemdroid.digitalwallet.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.threemdroid.digitalwallet.core.database.entity.CategoryEntity
import com.threemdroid.digitalwallet.core.database.model.CategoryWithCardCountRow
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY position ASC, created_at ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT categories.*, COUNT(wallet_cards.id) AS card_count
        FROM categories
        LEFT JOIN wallet_cards ON categories.id = wallet_cards.category_id
        GROUP BY categories.id
        ORDER BY categories.position ASC, categories.created_at ASC
        """
    )
    fun observeCategoriesWithCardCounts(): Flow<List<CategoryWithCardCountRow>>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    fun observeCategory(categoryId: String): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories ORDER BY position ASC, created_at ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT id FROM categories")
    suspend fun getAllIds(): List<String>

    @Query("SELECT id FROM categories WHERE id IN (:categoryIds)")
    suspend fun getExistingIds(categoryIds: List<String>): List<String>

    @Query("SELECT * FROM categories WHERE id IN (:categoryIds)")
    suspend fun getCategoriesByIds(categoryIds: List<String>): List<CategoryEntity>

    @Upsert
    suspend fun upsertCategory(category: CategoryEntity)

    @Upsert
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query(
        """
        UPDATE categories
        SET position = :position, updated_at = :updatedAt
        WHERE id = :categoryId
        """
    )
    suspend fun updateCategoryPosition(
        categoryId: String,
        position: Int,
        updatedAt: Instant
    )

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)
}
