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
        "#F59E0B",
        "#2563EB",
        "#7C3AED",
        "#0F766E",
        "#DC2626",
        "#EA580C",
        "#4B5563",
        "#1D4ED8",
        "#475569"
    )

    val defaultColorHex: String = colorHexes.first()
}
