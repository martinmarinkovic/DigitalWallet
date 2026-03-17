package com.threemdroid.digitalwallet.feature.home

import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import com.threemdroid.digitalwallet.data.category.FavoritesCategory
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateCategoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialState_defaultsColorSelection() {
        val viewModel = CreateCategoryViewModel(FakeCategoryRepository())

        val state = viewModel.uiState.value
        assertEquals(CreateCategoryDefaults.defaultColorHex, state.selectedColorHex)
        assertEquals(CreateCategoryDefaults.colorHexes, state.availableColorHexes)
        assertEquals(15, state.availableColorHexes.size)
        assertEquals(15, state.availableColorHexes.distinct().size)
        assertFalse(state.isSaving)
        assertEquals(null, state.nameError)
    }

    @Test
    fun save_withBlankName_showsRequiredValidationAndDoesNotPersist() = runTest {
        val repository = FakeCategoryRepository()
        val viewModel = CreateCategoryViewModel(repository)

        viewModel.onEvent(CreateCategoryEvent.OnNameChanged("   "))
        viewModel.onEvent(CreateCategoryEvent.OnSaveClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CreateCategoryNameError.REQUIRED, state.nameError)
        assertTrue(repository.createdCategories.isEmpty())
    }

    @Test
    fun nameChanged_afterRequiredValidation_clearsValidationError() = runTest {
        val viewModel = CreateCategoryViewModel(FakeCategoryRepository())

        viewModel.onEvent(CreateCategoryEvent.OnSaveClicked)
        advanceUntilIdle()
        assertEquals(CreateCategoryNameError.REQUIRED, viewModel.uiState.value.nameError)

        viewModel.onEvent(CreateCategoryEvent.OnNameChanged("Campus"))

        assertEquals(null, viewModel.uiState.value.nameError)
    }

    @Test
    fun save_withoutSelectingColor_usesDefaultColorAndDismisses() = runTest {
        val repository = FakeCategoryRepository()
        val viewModel = CreateCategoryViewModel(repository)

        viewModel.onEvent(CreateCategoryEvent.OnNameChanged("Campus"))
        val dismissEffect = async { viewModel.effects.first() }

        viewModel.onEvent(CreateCategoryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(CreateCategoryEffect.Dismiss, dismissEffect.await())
        assertEquals(CreateCategoryDefaults.defaultColorHex, repository.createdCategories.single().color)
    }

    @Test
    fun save_withValidInput_persistsCategoryAndDismisses() = runTest {
        val repository = FakeCategoryRepository()
        val viewModel = CreateCategoryViewModel(repository)

        viewModel.onEvent(CreateCategoryEvent.OnNameChanged("  Campus  "))
        viewModel.onEvent(CreateCategoryEvent.OnColorSelected("#123456"))
        val dismissEffect = async { viewModel.effects.first() }

        viewModel.onEvent(CreateCategoryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(CreateCategoryEffect.Dismiss, dismissEffect.await())
        assertEquals(1, repository.createdCategories.size)
        assertEquals("Campus", repository.createdCategories.single().name)
        assertEquals("#123456", repository.createdCategories.single().color)
        assertEquals("Favorites", repository.categories.value.first().category.name)
        assertEquals("Campus", repository.categories.value.last().category.name)
    }

    @Test
    fun colorPalette_matchesExpectedDistinctSpectrum() {
        assertEquals(
            listOf(
                "#F97316",
                "#DC2626",
                "#EC4899",
                "#A855F7",
                "#4F46E5",
                "#2563EB",
                "#0891B2",
                "#0F766E",
                "#16A34A",
                "#84CC16",
                "#EAB308",
                "#F59E0B",
                "#92400E",
                "#6B7280",
                "#475569"
            ),
            CreateCategoryDefaults.colorHexes
        )
    }

    @Test
    fun save_whenRepositoryFails_surfacesSaveErrorState() = runTest {
        val repository = FakeCategoryRepository(shouldFailOnCreate = true)
        val viewModel = CreateCategoryViewModel(repository)

        viewModel.onEvent(CreateCategoryEvent.OnNameChanged("Campus"))
        viewModel.onEvent(CreateCategoryEvent.OnSaveClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertTrue(state.isSaveErrorVisible)
    }

    private class FakeCategoryRepository(
        private val shouldFailOnCreate: Boolean = false,
        initialCategories: List<CategoryWithCardCount> = emptyList()
    ) : CategoryRepository {
        val categories = MutableStateFlow(initialCategories)
        val createdCategories = mutableListOf<Category>()

        override fun observeCategories() = categories.map { items ->
            items.map { it.category }
        }

        override fun observeCategoriesWithCardCounts() = categories

        override fun observeCategory(categoryId: String) = categories.map { items ->
            items.firstOrNull { it.category.id == categoryId }?.category
        }

        override suspend fun ensureDefaultCategories() {
            val existingById = categories.value.associateBy { it.category.id }
            val defaults = defaultCategories().map { existingById[it.category.id] ?: it }
            val customCategories = categories.value
                .filterNot { item ->
                    expectedDefaultCategoryIds.contains(item.category.id)
                }
                .mapIndexed { index, item ->
                    item.copy(
                        category = item.category.copy(
                            position = defaults.size + index
                        )
                    )
                }

            categories.value = defaults + customCategories
        }

        override suspend fun createCustomCategory(
            name: String,
            color: String
        ): Category {
            if (shouldFailOnCreate) {
                error("create failure")
            }

            ensureDefaultCategories()
            val timestamp = fixedTimestamp
            val createdCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                color = color.ifBlank { CreateCategoryDefaults.defaultColorHex },
                isDefault = false,
                isFavorites = false,
                position = categories.value.size,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            createdCategories += createdCategory
            categories.value = categories.value + CategoryWithCardCount(
                category = createdCategory,
                cardCount = 0
            )
            return createdCategory
        }

        override suspend fun upsertCategory(category: Category) {
            error("Not needed for CreateCategoryViewModel tests")
        }

        override suspend fun upsertCategories(categories: List<Category>) {
            error("Not needed for CreateCategoryViewModel tests")
        }

        override suspend fun updateCategoryOrder(categoryIdsInOrder: List<String>) {
            error("Not needed for CreateCategoryViewModel tests")
        }

        override suspend fun deleteCategory(categoryId: String) {
            error("Not needed for CreateCategoryViewModel tests")
        }
    }

    private companion object {
        val fixedTimestamp: Instant = Instant.parse("2026-03-13T10:00:00Z")

        val expectedDefaultCategoryIds = listOf(
            FavoritesCategory.id,
            "default_shopping_loyalty",
            "default_membership",
            "default_transport",
            "default_tickets",
            "default_vouchers",
            "default_access",
            "default_library",
            "default_other"
        )

        fun defaultCategories(): List<CategoryWithCardCount> = listOf(
            categoryWithCount(FavoritesCategory.id, "Favorites", 0, "#F59E0B", true, true),
            categoryWithCount("default_shopping_loyalty", "Shopping & Loyalty", 1, "#2563EB", true, false),
            categoryWithCount("default_membership", "Membership", 2, "#7C3AED", true, false),
            categoryWithCount("default_transport", "Transport", 3, "#0F766E", true, false),
            categoryWithCount("default_tickets", "Tickets", 4, "#DC2626", true, false),
            categoryWithCount("default_vouchers", "Vouchers", 5, "#EA580C", true, false),
            categoryWithCount("default_access", "Access", 6, "#4B5563", true, false),
            categoryWithCount("default_library", "Library", 7, "#1D4ED8", true, false),
            categoryWithCount("default_other", "Other", 8, "#475569", true, false)
        )

        fun categoryWithCount(
            id: String,
            name: String,
            position: Int,
            color: String,
            isDefault: Boolean,
            isFavorites: Boolean
        ): CategoryWithCardCount =
            CategoryWithCardCount(
                category = Category(
                    id = id,
                    name = name,
                    color = color,
                    isDefault = isDefault,
                    isFavorites = isFavorites,
                    position = position,
                    createdAt = fixedTimestamp,
                    updatedAt = fixedTimestamp
                ),
                cardCount = 0
            )
    }
}
