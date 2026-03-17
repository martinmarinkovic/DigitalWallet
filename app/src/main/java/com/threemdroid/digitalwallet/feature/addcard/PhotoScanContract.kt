package com.threemdroid.digitalwallet.feature.addcard

import android.net.Uri
import androidx.annotation.StringRes
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.navigation.encodeRouteValue

object PhotoScanRoutes {
    private const val baseRoute = "add_card/scan_card_photo"
    const val launchActionArg = "launchAction"
    const val routePattern =
        "$baseRoute?" +
            "${AddCardRoutes.categoryIdArg}={${AddCardRoutes.categoryIdArg}}" +
            "&$launchActionArg={$launchActionArg}"

    fun photoScan(
        categoryId: String?,
        launchAction: PhotoScanLaunchAction? = null
    ): String {
        val queryParameters = buildList {
            categoryId?.takeIf { it.isNotBlank() }?.let { value ->
                add("${AddCardRoutes.categoryIdArg}=${encodeRouteValue(value)}")
            }
            launchAction?.let { action ->
                add("$launchActionArg=${encodeRouteValue(action.name)}")
            }
        }

        return if (queryParameters.isEmpty()) {
            baseRoute
        } else {
            "$baseRoute?${queryParameters.joinToString(separator = "&")}"
        }
    }
}

enum class PhotoScanLaunchAction {
    TAKE_PHOTO,
    CHOOSE_IMAGE;

    companion object {
        fun fromRouteValue(value: String?): PhotoScanLaunchAction? =
            entries.firstOrNull { action -> action.name == value }
    }
}

enum class PhotoScanStatus(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
    val showActions: Boolean,
    val showProgress: Boolean
) {
    IDLE(
        titleRes = R.string.scan_card_photo_intro_title,
        messageRes = R.string.scan_card_photo_intro_message,
        showActions = true,
        showProgress = false
    ),
    PROCESSING(
        titleRes = R.string.scan_card_photo_processing_title,
        messageRes = R.string.scan_card_photo_processing_message,
        showActions = false,
        showProgress = true
    ),
    FAILED(
        titleRes = R.string.scan_card_photo_failed_title,
        messageRes = R.string.scan_card_photo_failed_message,
        showActions = true,
        showProgress = false
    )
}

data class PhotoScanUiState(
    val status: PhotoScanStatus = PhotoScanStatus.IDLE
)

sealed interface PhotoScanEvent {
    data object OnBackClicked : PhotoScanEvent

    data object OnTakePhotoClicked : PhotoScanEvent

    data object OnChooseImageClicked : PhotoScanEvent

    data object OnLaunchFailed : PhotoScanEvent

    data class OnImageSelected(val uri: Uri) : PhotoScanEvent
}

sealed interface PhotoScanEffect {
    data object NavigateBack : PhotoScanEffect

    data object LaunchCameraCapture : PhotoScanEffect

    data object LaunchImagePicker : PhotoScanEffect

    data class OpenConfirmation(val route: String) : PhotoScanEffect
}
