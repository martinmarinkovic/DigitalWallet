package com.threemdroid.digitalwallet.feature.addcard

import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.displayLabel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class ManualEntrySource(
    val titleRes: Int,
    val saveButtonRes: Int = R.string.manual_entry_save_button,
    val reviewMessageRes: Int? = null
) {
    MANUAL(R.string.manual_entry_title),
    EDIT(
        titleRes = R.string.edit_card_title,
        saveButtonRes = R.string.edit_card_save_button
    ),
    SCAN_CONFIRMATION(R.string.scan_barcode_confirmation_title),
    GOOGLE_WALLET_IMPORT_CONFIRMATION(
        titleRes = R.string.google_wallet_import_confirmation_title,
        reviewMessageRes = R.string.google_wallet_import_review_message
    ),
    SMART_SCAN_CONFIRMATION(
        titleRes = R.string.smart_scan_confirmation_title,
        reviewMessageRes = R.string.smart_scan_review_message
    ),
    PHOTO_SCAN_CONFIRMATION(
        titleRes = R.string.scan_card_photo_confirmation_title,
        reviewMessageRes = R.string.scan_card_photo_review_message
    )
}

object ManualEntryRoutes {
    const val baseRoute = "add_card/manual_entry"
    const val cardIdArg = "cardId"
    const val sourceArg = "source"
    const val codeTypeArg = "codeType"
    const val codeValueArg = "codeValue"
    const val cardNumberArg = "cardNumber"
    const val cardNameArg = "cardName"
    const val notesArg = "notes"
    const val routePattern =
        "$baseRoute?" +
            "${AddCardRoutes.categoryIdArg}={${AddCardRoutes.categoryIdArg}}" +
            "&$sourceArg={$sourceArg}" +
            "&$codeTypeArg={$codeTypeArg}" +
            "&$codeValueArg={$codeValueArg}" +
            "&$cardNumberArg={$cardNumberArg}" +
            "&$cardNameArg={$cardNameArg}" +
            "&$notesArg={$notesArg}"

    fun manualEntry(categoryId: String?): String =
        buildRoute(
            categoryId = categoryId,
            cardId = null,
            source = ManualEntrySource.MANUAL,
            codeType = null,
            codeValue = null,
            cardNumber = null,
            cardName = null,
            notes = null
        )

    fun editCard(cardId: String): String =
        buildRoute(
            categoryId = null,
            cardId = cardId,
            source = ManualEntrySource.EDIT,
            codeType = null,
            codeValue = null,
            cardNumber = null,
            cardName = null,
            notes = null
        )

    fun scanConfirmation(
        categoryId: String?,
        codeType: CardCodeType,
        codeValue: String
    ): String =
        buildRoute(
            categoryId = categoryId,
            cardId = null,
            source = ManualEntrySource.SCAN_CONFIRMATION,
            codeType = codeType,
            codeValue = codeValue,
            cardNumber = null,
            cardName = null,
            notes = null
        )

    fun googleWalletImportConfirmation(
        categoryId: String?,
        codeType: CardCodeType?,
        codeValue: String?,
        cardNumber: String?,
        cardName: String?,
        notes: String?
    ): String =
        buildRoute(
            categoryId = categoryId,
            cardId = null,
            source = ManualEntrySource.GOOGLE_WALLET_IMPORT_CONFIRMATION,
            codeType = codeType,
            codeValue = codeValue,
            cardNumber = cardNumber,
            cardName = cardName,
            notes = notes
        )

    fun smartScanConfirmation(
        categoryId: String?,
        codeType: CardCodeType?,
        codeValue: String?,
        cardNumber: String?,
        cardName: String?
    ): String =
        buildRoute(
            categoryId = categoryId,
            cardId = null,
            source = ManualEntrySource.SMART_SCAN_CONFIRMATION,
            codeType = codeType,
            codeValue = codeValue,
            cardNumber = cardNumber,
            cardName = cardName,
            notes = null
        )

    fun photoScanConfirmation(
        categoryId: String?,
        codeType: CardCodeType?,
        codeValue: String?,
        cardNumber: String?,
        cardName: String?
    ): String =
        buildRoute(
            categoryId = categoryId,
            cardId = null,
            source = ManualEntrySource.PHOTO_SCAN_CONFIRMATION,
            codeType = codeType,
            codeValue = codeValue,
            cardNumber = cardNumber,
            cardName = cardName,
            notes = null
        )

    private fun buildRoute(
        categoryId: String?,
        cardId: String?,
        source: ManualEntrySource,
        codeType: CardCodeType?,
        codeValue: String?,
        cardNumber: String?,
        cardName: String?,
        notes: String?
    ): String {
        val queryParameters = buildList {
            categoryId?.takeIf { it.isNotBlank() }?.let { value ->
                add("${AddCardRoutes.categoryIdArg}=${encodeRouteValue(value)}")
            }
            cardId?.takeIf { it.isNotBlank() }?.let { value ->
                add("$cardIdArg=${encodeRouteValue(value)}")
            }
            add("$sourceArg=${encodeRouteValue(source.name)}")
            codeType?.let { value ->
                add("$codeTypeArg=${encodeRouteValue(value.name)}")
            }
            codeValue?.takeIf { it.isNotBlank() }?.let { value ->
                add("$codeValueArg=${encodeRouteValue(value)}")
            }
            cardNumber?.takeIf { it.isNotBlank() }?.let { value ->
                add("$cardNumberArg=${encodeRouteValue(value)}")
            }
            cardName?.takeIf { it.isNotBlank() }?.let { value ->
                add("$cardNameArg=${encodeRouteValue(value)}")
            }
            notes?.takeIf { it.isNotBlank() }?.let { value ->
                add("$notesArg=${encodeRouteValue(value)}")
            }
        }

        return if (queryParameters.isEmpty()) {
            baseRoute
        } else {
            "$baseRoute?${queryParameters.joinToString(separator = "&")}"
        }
    }
}

internal fun encodeRouteValue(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")

data class ManualEntryUiState(
    val titleRes: Int = R.string.manual_entry_title,
    val saveButtonRes: Int = R.string.manual_entry_save_button,
    val reviewMessageRes: Int? = null,
    val isLoading: Boolean = true,
    val isCardMissing: Boolean = false,
    val isSaving: Boolean = false,
    val availableCategories: List<ManualEntryCategoryOptionUiModel> = emptyList(),
    val availableCodeTypes: List<ManualEntryCodeTypeUiModel> = CardCodeType.entries.map { codeType ->
        ManualEntryCodeTypeUiModel(
            codeType = codeType,
            label = codeType.displayLabel()
        )
    },
    val cardName: String = "",
    val selectedCategoryId: String? = null,
    val selectedCodeType: CardCodeType = CardCodeType.QR_CODE,
    val codeValue: String = "",
    val cardNumber: String = "",
    val expirationDateInput: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val cardNameError: ManualEntryFieldError? = null,
    val categoryError: ManualEntryFieldError? = null,
    val codeValueError: ManualEntryFieldError? = null,
    val expirationDateError: ManualEntryExpirationDateError? = null,
    val isSaveErrorVisible: Boolean = false
)

data class ManualEntryCategoryOptionUiModel(
    val id: String,
    val name: String
)

data class ManualEntryCodeTypeUiModel(
    val codeType: CardCodeType,
    val label: String
)

enum class ManualEntryFieldError {
    REQUIRED
}

enum class ManualEntryExpirationDateError {
    INVALID_FORMAT
}

sealed interface ManualEntryEvent {
    data object OnBackClicked : ManualEntryEvent

    data class OnCardNameChanged(val value: String) : ManualEntryEvent

    data class OnCategorySelected(val categoryId: String) : ManualEntryEvent

    data class OnCodeTypeSelected(val codeType: CardCodeType) : ManualEntryEvent

    data class OnCodeValueChanged(val value: String) : ManualEntryEvent

    data class OnCardNumberChanged(val value: String) : ManualEntryEvent

    data class OnExpirationDateChanged(val value: String) : ManualEntryEvent

    data class OnNotesChanged(val value: String) : ManualEntryEvent

    data class OnFavoriteChanged(val value: Boolean) : ManualEntryEvent

    data object OnSaveClicked : ManualEntryEvent
}

sealed interface ManualEntryEffect {
    data object NavigateBack : ManualEntryEffect

    data class CardSaved(val categoryId: String) : ManualEntryEffect
}
