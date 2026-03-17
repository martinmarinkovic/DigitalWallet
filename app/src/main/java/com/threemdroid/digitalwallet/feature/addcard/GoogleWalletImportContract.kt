package com.threemdroid.digitalwallet.feature.addcard

import android.net.Uri
import androidx.annotation.StringRes
import com.threemdroid.digitalwallet.R

object GoogleWalletImportRoutes {
    private const val baseRoute = "add_card/import_google_wallet"
    const val routePattern = "$baseRoute?${AddCardRoutes.categoryIdArg}={${AddCardRoutes.categoryIdArg}}"

    fun googleWalletImport(categoryId: String?): String =
        if (categoryId.isNullOrBlank()) {
            baseRoute
        } else {
            "$baseRoute?${AddCardRoutes.categoryIdArg}=${encodeRouteValue(categoryId)}"
        }
}

enum class GoogleWalletImportStatus(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int
) {
    IDLE(
        titleRes = R.string.google_wallet_import_title,
        messageRes = R.string.google_wallet_import_message
    ),
    PROCESSING(
        titleRes = R.string.google_wallet_import_processing_title,
        messageRes = R.string.google_wallet_import_processing_message
    ),
    FAILED(
        titleRes = R.string.google_wallet_import_failed_title,
        messageRes = R.string.google_wallet_import_failed_message
    )
}

data class GoogleWalletImportUiState(
    val status: GoogleWalletImportStatus = GoogleWalletImportStatus.IDLE,
    val sharedTextInput: String = ""
)

sealed interface GoogleWalletImportEvent {
    data object OnBackClicked : GoogleWalletImportEvent

    data class OnSharedTextChanged(val value: String) : GoogleWalletImportEvent

    data object OnImportTextClicked : GoogleWalletImportEvent

    data object OnChooseImageClicked : GoogleWalletImportEvent

    data object OnLaunchFailed : GoogleWalletImportEvent

    data class OnImageSelected(val uri: Uri) : GoogleWalletImportEvent
}

sealed interface GoogleWalletImportEffect {
    data object NavigateBack : GoogleWalletImportEffect

    data object LaunchImagePicker : GoogleWalletImportEffect

    data class OpenConfirmation(val route: String) : GoogleWalletImportEffect
}
