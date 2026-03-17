package com.threemdroid.digitalwallet.data.category

import com.threemdroid.digitalwallet.core.database.entity.CategoryEntity
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import java.time.Instant

internal object FavoritesCategory {
    const val id = "virtual_favorites"
    const val legacyDefaultId = "default_favorites"

    private const val name = "Favorites"
    private const val colorHex = "#F97316"
    private val timestamp: Instant = Instant.EPOCH

    fun isVirtual(categoryId: String): Boolean = categoryId == id

    fun matchesReservedSemantics(category: Category): Boolean =
        matchesReservedSemantics(
            categoryId = category.id,
            categoryName = category.name,
            isFavorites = category.isFavorites
        )

    fun matchesReservedSemantics(
        categoryId: String? = null,
        categoryName: String? = null,
        isFavorites: Boolean = false
    ): Boolean =
        isFavorites ||
            categoryId == id ||
            categoryId == legacyDefaultId ||
            categoryName?.trim()?.equals(name, ignoreCase = true) == true

    fun isLegacyStoredCategory(category: CategoryEntity): Boolean =
        category.isFavorites ||
            category.id == legacyDefaultId ||
            category.name.equals(name, ignoreCase = true)

    fun create(position: Int = 0): Category =
        Category(
            id = id,
            name = name,
            color = colorHex,
            isDefault = true,
            isFavorites = true,
            position = position,
            createdAt = timestamp,
            updatedAt = timestamp
        )

    fun withCardCount(cardCount: Int): CategoryWithCardCount =
        CategoryWithCardCount(
            category = create(),
            cardCount = cardCount
        )

    fun defaultColorHex(): String = colorHex
}
