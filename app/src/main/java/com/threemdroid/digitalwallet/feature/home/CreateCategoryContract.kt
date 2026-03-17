package com.threemdroid.digitalwallet.feature.home

data class CreateCategoryUiState(
    val name: String = "",
    val selectedColorHex: String = CreateCategoryDefaults.defaultColorHex,
    val availableColorHexes: List<String> = CreateCategoryDefaults.colorHexes,
    val isSaving: Boolean = false,
    val nameError: CreateCategoryNameError? = null,
    val isSaveErrorVisible: Boolean = false
)

enum class CreateCategoryNameError {
    REQUIRED
}

sealed interface CreateCategoryEvent {
    data class OnNameChanged(val name: String) : CreateCategoryEvent

    data class OnColorSelected(val colorHex: String) : CreateCategoryEvent

    data object OnSaveClicked : CreateCategoryEvent

    data object OnDismissRequested : CreateCategoryEvent
}

sealed interface CreateCategoryEffect {
    data object Dismiss : CreateCategoryEffect
}

internal object CreateCategoryDefaults {
    val colorHexes: List<String> = listOf(
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
    )

    val defaultColorHex: String = colorHexes.first()
}
