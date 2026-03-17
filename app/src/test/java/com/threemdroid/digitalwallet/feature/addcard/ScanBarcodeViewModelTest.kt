package com.threemdroid.digitalwallet.feature.addcard

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanBarcodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun permissionGranted_startsScannerInitialization() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(ScanBarcodeEvent.OnPermissionStateResolved(granted = true))

        assertEquals(ScanBarcodeStatus.INITIALIZING, viewModel.uiState.value.status)
    }

    @Test
    fun permissionMissing_showsPermissionUiInsteadOfLoader() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(ScanBarcodeEvent.OnPermissionStateResolved(granted = false))

        assertEquals(ScanBarcodeStatus.PERMISSION_REQUIRED, viewModel.uiState.value.status)
    }

    @Test
    fun permissionButtonClicked_requestsCameraPermission() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ScanBarcodeEvent.OnPermissionButtonClicked)
        advanceUntilIdle()

        assertEquals(ScanBarcodeEffect.RequestCameraPermission, deferredEffect.await())
    }

    @Test
    fun permissionDeniedPermanently_showsBlockedState() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(
            ScanBarcodeEvent.OnPermissionRequestResult(
                granted = false,
                shouldShowRationale = false
            )
        )

        assertEquals(ScanBarcodeStatus.PERMISSION_BLOCKED, viewModel.uiState.value.status)
    }

    @Test
    fun openSettingsClicked_emitsOpenAppSettings() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ScanBarcodeEvent.OnOpenSettingsClicked)
        advanceUntilIdle()

        assertEquals(ScanBarcodeEffect.OpenAppSettings, deferredEffect.await())
    }

    @Test
    fun scannerInitialized_marksScannerActive() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(ScanBarcodeEvent.OnPermissionStateResolved(granted = true))
        viewModel.onEvent(ScanBarcodeEvent.OnScannerInitialized)

        assertEquals(ScanBarcodeStatus.ACTIVE, viewModel.uiState.value.status)
    }

    @Test
    fun scannerInitializationFailed_updatesStateForRetry() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(ScanBarcodeEvent.OnScannerInitializationFailed)

        assertEquals(ScanBarcodeStatus.FAILED, viewModel.uiState.value.status)
    }

    @Test
    fun initializationTimeout_fallsBackToFailed() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(ScanBarcodeEvent.OnPermissionStateResolved(granted = true))
        advanceTimeBy(8_001)
        advanceUntilIdle()

        assertEquals(ScanBarcodeStatus.FAILED, viewModel.uiState.value.status)
    }

    @Test
    fun retryClicked_restartsScannerInitialization() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(ScanBarcodeEvent.OnScannerInitializationFailed)
        viewModel.onEvent(ScanBarcodeEvent.OnRetryClicked)

        assertEquals(ScanBarcodeStatus.INITIALIZING, viewModel.uiState.value.status)
    }

    @Test
    fun onScanSucceeded_opensConfirmationRouteWithCategoryContext() = runTest {
        val viewModel = ScanBarcodeViewModel(
            SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_access")
            )
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(
            ScanBarcodeEvent.OnScanSucceeded(
                codeType = CardCodeType.QR_CODE,
                codeValue = "ACCESS-QR-123"
            )
        )
        advanceUntilIdle()

        assertEquals(
            ScanBarcodeEffect.OpenConfirmation(
                ManualEntryRoutes.scanConfirmation(
                    categoryId = "default_access",
                    codeType = CardCodeType.QR_CODE,
                    codeValue = "ACCESS-QR-123"
                )
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onScanSucceeded_withoutCategoryContext_opensConfirmationRouteWithoutPreselection() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(
            ScanBarcodeEvent.OnScanSucceeded(
                codeType = CardCodeType.CODE_128,
                codeValue = "LOYALTY-555"
            )
        )
        advanceUntilIdle()

        assertEquals(
            ScanBarcodeEffect.OpenConfirmation(
                ManualEntryRoutes.scanConfirmation(
                    categoryId = null,
                    codeType = CardCodeType.CODE_128,
                    codeValue = "LOYALTY-555"
                )
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onBackClicked_emitsNavigateBack() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ScanBarcodeEvent.OnBackClicked)
        advanceUntilIdle()

        assertEquals(ScanBarcodeEffect.NavigateBack, deferredEffect.await())
    }
}
