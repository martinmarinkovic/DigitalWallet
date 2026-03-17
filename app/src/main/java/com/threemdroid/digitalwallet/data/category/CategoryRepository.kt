package com.threemdroid.digitalwallet.data.category

import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    fun observeCategoriesWithCardCounts(): Flow<List<CategoryWithCardCount>>

    fun observeCategory(categoryId: String): Flow<Category?>

    suspend fun ensureDefaultCategories()

    suspend fun createCustomCategory(
        name: String,
        color: String
    ): Category

    suspend fun upsertCategory(category: Category)

    suspend fun upsertCategories(categories: List<Category>)

    suspend fun updateCategoryOrder(categoryIdsInOrder: List<String>)

    suspend fun deleteCategory(categoryId: String)
}
