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
            color = "#DC2626"
        ),
        DefaultCategoryDefinition(
            id = "default_membership",
            name = "Membership",
            color = "#A855F7"
        ),
        DefaultCategoryDefinition(
            id = "default_transport",
            name = "Transport",
            color = "#0F766E"
        ),
        DefaultCategoryDefinition(
            id = "default_tickets",
            name = "Tickets",
            color = "#2563EB"
        ),
        DefaultCategoryDefinition(
            id = "default_vouchers",
            name = "Vouchers",
            color = "#475569"
        ),
        DefaultCategoryDefinition(
            id = "default_access",
            name = "Access",
            color = "#16A34A"
        ),
        DefaultCategoryDefinition(
            id = "default_library",
            name = "Library",
            color = "#EC4899"
        ),
        DefaultCategoryDefinition(
            id = "default_other",
            name = "Other",
            color = "#92400E"
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
