package com.threemdroid.digitalwallet.feature.addcard

import androidx.annotation.StringRes
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType

object ScanBarcodeRoutes {
    private const val baseRoute = "add_card/scan_barcode_qr"
    const val routePattern = "$baseRoute?${AddCardRoutes.categoryIdArg}={${AddCardRoutes.categoryIdArg}}"

    fun scan(categoryId: String?): String =
        if (categoryId.isNullOrBlank()) {
            baseRoute
        } else {
            "$baseRoute?${AddCardRoutes.categoryIdArg}=${encodeRouteValue(categoryId)}"
        }
}

enum class ScanBarcodeStatus(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
    val showRetryButton: Boolean
) {
    LAUNCHING(
        titleRes = R.string.scan_barcode_launching_title,
        messageRes = R.string.scan_barcode_launching_message,
        showRetryButton = false
    ),
    CANCELLED(
        titleRes = R.string.scan_barcode_cancelled_title,
        messageRes = R.string.scan_barcode_cancelled_message,
        showRetryButton = true
    ),
    FAILED(
        titleRes = R.string.scan_barcode_failed_title,
        messageRes = R.string.scan_barcode_failed_message,
        showRetryButton = true
    )
}

data class ScanBarcodeUiState(
    val status: ScanBarcodeStatus = ScanBarcodeStatus.LAUNCHING
)

sealed interface ScanBarcodeEvent {
    data object OnScreenOpened : ScanBarcodeEvent

    data object OnBackClicked : ScanBarcodeEvent

    data object OnRetryClicked : ScanBarcodeEvent

    data class OnScanSucceeded(
        val codeType: CardCodeType,
        val codeValue: String
    ) : ScanBarcodeEvent

    data object OnScanCancelled : ScanBarcodeEvent

    data object OnScanFailed : ScanBarcodeEvent
}

sealed interface ScanBarcodeEffect {
    data object NavigateBack : ScanBarcodeEffect

    data object LaunchScanner : ScanBarcodeEffect

    data class OpenConfirmation(val route: String) : ScanBarcodeEffect
}
