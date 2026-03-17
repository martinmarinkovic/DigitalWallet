package com.threemdroid.digitalwallet.data.category

import com.threemdroid.digitalwallet.core.database.entity.CategoryEntity
import java.time.Instant

internal data class DefaultCategoryDefinition(
    val id: String,
    val name: String,
    val color: String
)

internal object DefaultCategories {
    val definitions: List<DefaultCategoryDefinition> = listOf(
        DefaultCategoryDefinition(
            id = "default_shopping_loyalty",
            name = "Shopping & Loyalty",
            color = "#2563EB"
        ),
        DefaultCategoryDefinition(
            id = "default_membership",
            name = "Membership",
            color = "#A855F7"
        ),
        DefaultCategoryDefinition(
            id = "default_transport",
            name = "Transport",
            color = "#0891B2"
        ),
        DefaultCategoryDefinition(
            id = "default_tickets",
            name = "Tickets",
            color = "#DC2626"
        ),
        DefaultCategoryDefinition(
            id = "default_vouchers",
            name = "Vouchers",
            color = "#F97316"
        ),
        DefaultCategoryDefinition(
            id = "default_access",
            name = "Access",
            color = "#16A34A"
        ),
        DefaultCategoryDefinition(
            id = "default_library",
            name = "Library",
            color = "#4F46E5"
        ),
        DefaultCategoryDefinition(
            id = "default_other",
            name = "Other",
            color = "#475569"
        )
    )

    private val definitionIds = definitions.map { it.id }.toSet()
    private val definitionsById = definitions.associateBy { it.id }
    val otherCategoryId: String = "default_other"

    fun isDefaultCategoryId(categoryId: String): Boolean = categoryId in definitionIds

    fun definitionForId(categoryId: String): DefaultCategoryDefinition? = definitionsById[categoryId]

    fun createEntity(
        definition: DefaultCategoryDefinition,
        position: Int,
        timestamp: Instant
    ): CategoryEntity =
        CategoryEntity(
            id = definition.id,
            name = definition.name,
            color = definition.color,
            isDefault = true,
            isFavorites = false,
            position = position,
            createdAt = timestamp,
            updatedAt = timestamp
        )
}
