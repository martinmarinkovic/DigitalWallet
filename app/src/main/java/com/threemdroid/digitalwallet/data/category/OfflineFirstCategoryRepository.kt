package com.threemdroid.digitalwallet.data.category

import androidx.room.withTransaction
import com.threemdroid.digitalwallet.core.database.DigitalWalletDatabase
import com.threemdroid.digitalwallet.core.database.dao.CategoryDao
import com.threemdroid.digitalwallet.core.database.entity.CardEntity
import com.threemdroid.digitalwallet.core.database.mapper.asEntity
import com.threemdroid.digitalwallet.core.database.mapper.asExternalModel
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import com.threemdroid.digitalwallet.data.sync.SyncMutationRecorder
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class OfflineFirstCategoryRepository @Inject constructor(
    private val database: DigitalWalletDatabase,
    private val categoryDao: CategoryDao,
    private val syncMutationRecorder: SyncMutationRecorder = SyncMutationRecorder.NO_OP
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategories().map { categories ->
            categories.map { it.asExternalModel() }
        }

    override fun observeCategoriesWithCardCounts(): Flow<List<CategoryWithCardCount>> =
        combine(
            categoryDao.observeCategoriesWithCardCounts(),
            database.cardDao().observeAllCards()
        ) { categories, cards ->
            listOf(
                FavoritesCategory.withCardCount(
                    cardCount = cards.count { card -> card.isFavorite }
                )
            ) + categories.map { category ->
                category.asExternalModel()
            }
        }

    override fun observeCategory(categoryId: String): Flow<Category?> =
        if (FavoritesCategory.isVirtual(categoryId)) {
            categoryDao.observeCategories().map {
                FavoritesCategory.create()
            }
        } else {
            categoryDao.observeCategory(categoryId).map { category ->
                category?.asExternalModel()
            }
        }

    override suspend fun ensureDefaultCategories() {
        database.withTransaction {
            val existingCategories = categoryDao.getCategories()
            val timestamp = Instant.now()
            if (existingCategories.isEmpty()) {
                val insertedCategories =
                    DefaultCategories.definitions.mapIndexed { index, definition ->
                        DefaultCategories.createEntity(
                            definition = definition,
                            position = index,
                            timestamp = timestamp
                        )
                    }
                categoryDao.upsertCategories(insertedCategories)
                syncMutationRecorder.recordCategoryUpserts(
                    insertedCategories.map { category -> category.id }
                )
                return@withTransaction
            }

            val legacyFavoritesCategoryIds = existingCategories
                .filter { category ->
                    FavoritesCategory.isLegacyStoredCategory(category)
                }
                .map { category -> category.id }
                .toSet()

            val existingNonFavorites = existingCategories
                .filterNot { category -> category.id in legacyFavoritesCategoryIds }
                .map { category ->
                    DefaultCategories.definitionForId(category.id)?.let { definition ->
                        category.normalizeWithDefinition(
                            definition = definition,
                            timestamp = timestamp
                        )
                    } ?: category
                }
            val existingById = existingNonFavorites.associateBy { it.id }
            val existingDefaultCategories = existingNonFavorites.filter { category ->
                DefaultCategories.isDefaultCategoryId(category.id)
            }
            val missingDefaultCategories = DefaultCategories.definitions
                .filterNot { definition -> definition.id in existingById }
                .map { definition ->
                    DefaultCategories.createEntity(
                        definition = definition,
                        position = 0,
                        timestamp = timestamp
                    )
                }
            val customCategories = existingNonFavorites.filterNot { category ->
                DefaultCategories.isDefaultCategoryId(category.id)
            }

            val expectedCategories = (existingDefaultCategories + missingDefaultCategories + customCategories).mapIndexed { index, category ->
                if (category.position == index) {
                    category
                } else {
                    category.copy(
                        position = index,
                        updatedAt = timestamp
                    )
                }
            }

            if (expectedCategories != existingNonFavorites) {
                categoryDao.upsertCategories(expectedCategories)
                syncMutationRecorder.recordCategoryUpserts(
                    expectedCategories.map { category -> category.id }
                )
            }

            migrateLegacyFavoritesCards(
                legacyFavoritesCategoryIds = legacyFavoritesCategoryIds,
                timestamp = timestamp
            )

            if (legacyFavoritesCategoryIds.isNotEmpty()) {
                legacyFavoritesCategoryIds.forEach { categoryId ->
                    categoryDao.deleteCategory(categoryId)
                }
                syncMutationRecorder.recordCategoryDeletes(legacyFavoritesCategoryIds.toList())
            }
        }
    }

    override suspend fun createCustomCategory(
        name: String,
        color: String
    ): Category {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) {
            "Category name is required."
        }

        ensureDefaultCategories()

        return database.withTransaction {
            val existingCategories = categoryDao.getCategories()
            val timestamp = Instant.now()
            val createdCategory = Category(
                id = UUID.randomUUID().toString(),
                name = normalizedName,
                color = color.ifBlank { FavoritesCategory.defaultColorHex() },
                isDefault = false,
                isFavorites = false,
                position = (existingCategories.maxOfOrNull { it.position } ?: -1) + 1,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            categoryDao.upsertCategory(createdCategory.asEntity())
            syncMutationRecorder.recordCategoryUpserts(listOf(createdCategory.id))
            createdCategory
        }
    }

    override suspend fun upsertCategory(category: Category) {
        database.withTransaction {
            categoryDao.upsertCategory(category.asEntity())
            syncMutationRecorder.recordCategoryUpserts(listOf(category.id))
        }
    }

    override suspend fun upsertCategories(categories: List<Category>) {
        if (categories.isEmpty()) {
            return
        }

        database.withTransaction {
            categoryDao.upsertCategories(categories.map { it.asEntity() })
            syncMutationRecorder.recordCategoryUpserts(categories.map { category -> category.id })
        }
    }

    override suspend fun updateCategoryOrder(categoryIdsInOrder: List<String>) {
        require(
            categoryIdsInOrder.count { categoryId ->
                FavoritesCategory.isVirtual(categoryId)
            } <= 1
        ) {
            "Favorites category must not appear more than once."
        }
        if (categoryIdsInOrder.any(FavoritesCategory::isVirtual)) {
            require(categoryIdsInOrder.firstOrNull() == FavoritesCategory.id) {
                "Favorites category must remain first."
            }
        }

        val persistedCategoryIds = categoryIdsInOrder.filterNot(FavoritesCategory::isVirtual)
        val existingCategories = categoryDao.getCategories()
        val existingIds = existingCategories.map { category -> category.id }
        require(
            existingIds.size == persistedCategoryIds.size &&
                existingIds.toSet() == persistedCategoryIds.toSet()
        ) {
            "Category order update must include every category exactly once."
        }

        database.withTransaction {
            val updatedAt = Instant.now()
            persistedCategoryIds.forEachIndexed { index, categoryId ->
                categoryDao.updateCategoryPosition(
                    categoryId = categoryId,
                    position = index,
                    updatedAt = updatedAt
                )
            }
            syncMutationRecorder.recordCategoryUpserts(persistedCategoryIds)
        }
    }

    override suspend fun deleteCategory(categoryId: String) {
        database.withTransaction {
            val deletedCardIds = database.cardDao().getIdsForCategory(categoryId)
            categoryDao.deleteCategory(categoryId)
            syncMutationRecorder.recordCategoryDeletes(listOf(categoryId))
            syncMutationRecorder.recordCardDeletes(deletedCardIds)
        }
    }

    private fun com.threemdroid.digitalwallet.core.database.entity.CategoryEntity.normalizeWithDefinition(
        definition: DefaultCategoryDefinition,
        timestamp: Instant
    ): com.threemdroid.digitalwallet.core.database.entity.CategoryEntity {
        val needsUpdate =
            name != definition.name ||
                color != definition.color ||
                !isDefault ||
                isFavorites

        return if (needsUpdate) {
            copy(
                name = definition.name,
                color = definition.color,
                isDefault = true,
                isFavorites = false,
                updatedAt = timestamp
            )
        } else {
            this
        }
    }

    private suspend fun migrateLegacyFavoritesCards(
        legacyFavoritesCategoryIds: Set<String>,
        timestamp: Instant
    ) {
        if (legacyFavoritesCategoryIds.isEmpty()) {
            return
        }

        val allCards = database.cardDao().getAllCards()
        val movedCards = allCards
            .filter { card -> card.categoryId in legacyFavoritesCategoryIds }
            .sortedWith(
                compareBy<CardEntity>(
                    { it.position },
                    { it.createdAt },
                    { it.id }
                )
            )
        if (movedCards.isEmpty()) {
            return
        }

        val nextPosition = allCards
            .filter { card -> card.categoryId == DefaultCategories.otherCategoryId }
            .maxOfOrNull { card -> card.position }
            ?.plus(1) ?: 0

        val normalizedCards = movedCards.mapIndexed { index, card ->
            card.copy(
                categoryId = DefaultCategories.otherCategoryId,
                isFavorite = true,
                position = nextPosition + index,
                updatedAt = timestamp
            )
        }

        database.cardDao().upsertCards(normalizedCards)
        syncMutationRecorder.recordCardUpserts(normalizedCards.map { card -> card.id })
    }
}
