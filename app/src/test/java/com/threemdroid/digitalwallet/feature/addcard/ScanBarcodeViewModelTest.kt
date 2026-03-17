package com.threemdroid.digitalwallet.feature.addcard

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanBarcodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onScreenOpened_launchesScannerOnlyOnce() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())
        val firstEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ScanBarcodeEvent.OnScreenOpened)
        advanceUntilIdle()

        assertEquals(ScanBarcodeEffect.LaunchScanner, firstEffect.await())

        viewModel.onEvent(ScanBarcodeEvent.OnScreenOpened)
        advanceUntilIdle()

        assertNull(
            withTimeoutOrNull(50) {
                viewModel.effects.first()
            }
        )
        assertEquals(ScanBarcodeStatus.LAUNCHING, viewModel.uiState.value.status)
    }

    @Test
    fun onRetryClicked_relaunchesScanner() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())
        val retryEffect = async { viewModel.effects.first() }

        viewModel.onEvent(ScanBarcodeEvent.OnRetryClicked)
        advanceUntilIdle()

        assertEquals(ScanBarcodeEffect.LaunchScanner, retryEffect.await())
        assertEquals(ScanBarcodeStatus.LAUNCHING, viewModel.uiState.value.status)
    }

    @Test
    fun onScanCancelled_updatesStateForRetry() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(ScanBarcodeEvent.OnScanCancelled)

        assertEquals(ScanBarcodeStatus.CANCELLED, viewModel.uiState.value.status)
    }

    @Test
    fun onScanFailed_updatesStateForRetry() = runTest {
        val viewModel = ScanBarcodeViewModel(SavedStateHandle())

        viewModel.onEvent(ScanBarcodeEvent.OnScanFailed)

        assertEquals(ScanBarcodeStatus.FAILED, viewModel.uiState.value.status)
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
