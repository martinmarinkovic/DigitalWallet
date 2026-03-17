package com.threemdroid.digitalwallet.feature.addcard

import androidx.annotation.StringRes
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.navigation.encodeRouteValue

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
    val showProgress: Boolean = false,
    val showRetryButton: Boolean = false,
    val showPermissionButton: Boolean = false,
    val showOpenSettingsButton: Boolean = false,
    val showScannerPreview: Boolean = false
) {
    INITIALIZING(
        titleRes = R.string.scan_barcode_launching_title,
        messageRes = R.string.scan_barcode_launching_message,
        showProgress = true,
        showScannerPreview = true
    ),
    ACTIVE(
        titleRes = R.string.scan_barcode_active_title,
        messageRes = R.string.scan_barcode_active_message,
        showScannerPreview = true
    ),
    PERMISSION_REQUIRED(
        titleRes = R.string.scan_barcode_permission_required_title,
        messageRes = R.string.scan_barcode_permission_required_message,
        showPermissionButton = true
    ),
    PERMISSION_BLOCKED(
        titleRes = R.string.scan_barcode_permission_blocked_title,
        messageRes = R.string.scan_barcode_permission_blocked_message,
        showOpenSettingsButton = true
    ),
    FAILED(
        titleRes = R.string.scan_barcode_failed_title,
        messageRes = R.string.scan_barcode_failed_message,
        showRetryButton = true
    )
}

data class ScanBarcodeUiState(
    val status: ScanBarcodeStatus = ScanBarcodeStatus.INITIALIZING
)

sealed interface ScanBarcodeEvent {
    data object OnBackClicked : ScanBarcodeEvent

    data object OnRetryClicked : ScanBarcodeEvent

    data object OnPermissionButtonClicked : ScanBarcodeEvent

    data object OnOpenSettingsClicked : ScanBarcodeEvent

    data class OnPermissionStateResolved(val granted: Boolean) : ScanBarcodeEvent

    data class OnPermissionRequestResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean
    ) : ScanBarcodeEvent

    data object OnScannerInitialized : ScanBarcodeEvent

    data object OnScannerInitializationFailed : ScanBarcodeEvent

    data class OnScanSucceeded(
        val codeType: CardCodeType,
        val codeValue: String
    ) : ScanBarcodeEvent

    data object OnScanProcessingFailed : ScanBarcodeEvent
}

sealed interface ScanBarcodeEffect {
    data object NavigateBack : ScanBarcodeEffect

    data object RequestCameraPermission : ScanBarcodeEffect

    data object OpenAppSettings : ScanBarcodeEffect

    data class OpenConfirmation(val route: String) : ScanBarcodeEffect
}
