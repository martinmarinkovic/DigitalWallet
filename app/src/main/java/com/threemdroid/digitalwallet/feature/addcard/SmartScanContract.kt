package com.threemdroid.digitalwallet.feature.addcard

import android.net.Uri
import androidx.annotation.StringRes
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.navigation.encodeRouteValue

object SmartScanRoutes {
    private const val baseRoute = "add_card/smart_scanning"
    const val routePattern = "$baseRoute?${AddCardRoutes.categoryIdArg}={${AddCardRoutes.categoryIdArg}}"

    fun smartScan(categoryId: String?): String =
        if (categoryId.isNullOrBlank()) {
            baseRoute
        } else {
            "$baseRoute?${AddCardRoutes.categoryIdArg}=${encodeRouteValue(categoryId)}"
        }
}

enum class SmartScanStatus(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
    val showActions: Boolean,
    val showProgress: Boolean
) {
    IDLE(
        titleRes = R.string.smart_scan_intro_title,
        messageRes = R.string.smart_scan_intro_message,
        showActions = true,
        showProgress = false
    ),
    PROCESSING(
        titleRes = R.string.smart_scan_processing_title,
        messageRes = R.string.smart_scan_processing_message,
        showActions = false,
        showProgress = true
    ),
    FAILED(
        titleRes = R.string.smart_scan_failed_title,
        messageRes = R.string.smart_scan_failed_message,
        showActions = true,
        showProgress = false
    )
}

data class SmartScanUiState(
    val status: SmartScanStatus = SmartScanStatus.IDLE
)

sealed interface SmartScanEvent {
    data object OnBackClicked : SmartScanEvent

    data object OnScanCodeClicked : SmartScanEvent

    data object OnTakePhotoClicked : SmartScanEvent

    data object OnChooseImageClicked : SmartScanEvent

    data object OnLaunchFailed : SmartScanEvent

    data class OnImageSelected(val uri: Uri) : SmartScanEvent

    data class OnBarcodeScanSucceeded(
        val codeType: CardCodeType,
        val codeValue: String
    ) : SmartScanEvent

    data object OnBarcodeScanCancelled : SmartScanEvent

    data object OnBarcodeScanFailed : SmartScanEvent
}

sealed interface SmartScanEffect {
    data object NavigateBack : SmartScanEffect

    data object LaunchScanner : SmartScanEffect

    data object LaunchCameraCapture : SmartScanEffect

    data object LaunchImagePicker : SmartScanEffect

    data class OpenConfirmation(val route: String) : SmartScanEffect
}
