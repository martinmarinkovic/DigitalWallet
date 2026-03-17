package com.threemdroid.digitalwallet.feature.addcard

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SmartScanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onScanCodeClicked_emitsLaunchScanner() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(SmartScanEvent.OnScanCodeClicked)
        advanceUntilIdle()

        assertEquals(SmartScanEffect.LaunchScanner, deferredEffect.await())
    }

    @Test
    fun onTakePhotoClicked_emitsLaunchCameraCapture() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(SmartScanEvent.OnTakePhotoClicked)
        advanceUntilIdle()

        assertEquals(SmartScanEffect.LaunchCameraCapture, deferredEffect.await())
    }

    @Test
    fun onChooseImageClicked_emitsLaunchImagePicker() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(SmartScanEvent.OnChooseImageClicked)
        advanceUntilIdle()

        assertEquals(SmartScanEffect.LaunchImagePicker, deferredEffect.await())
    }

    @Test
    fun onBarcodeScanSucceeded_opensConfirmationWithInferredCodeOnly() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_access")
            ),
            photoScanExtractor = FakePhotoScanExtractor()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(
            SmartScanEvent.OnBarcodeScanSucceeded(
                codeType = CardCodeType.CODE_128,
                codeValue = "ACCESS-4455"
            )
        )
        advanceUntilIdle()

        assertEquals(
            SmartScanEffect.OpenConfirmation(
                ManualEntryRoutes.smartScanConfirmation(
                    categoryId = "default_access",
                    codeType = CardCodeType.CODE_128,
                    codeValue = "ACCESS-4455",
                    cardNumber = null,
                    cardName = null
                )
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onImageSelected_withSuccessfulExtraction_opensConfirmationWithExtractedFields() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_membership")
            ),
            photoScanExtractor = FakePhotoScanExtractor(
                result = PhotoScanExtractionResult(
                    codeType = CardCodeType.QR_CODE,
                    codeValue = "GYM-7788",
                    cardNumber = "7788",
                    cardName = "Gym Pass"
                )
            )
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(SmartScanEvent.OnImageSelected(Uri.parse("content://smart-scan/success")))
        advanceUntilIdle()

        assertEquals(
            SmartScanEffect.OpenConfirmation(
                ManualEntryRoutes.smartScanConfirmation(
                    categoryId = "default_membership",
                    codeType = CardCodeType.QR_CODE,
                    codeValue = "GYM-7788",
                    cardNumber = "7788",
                    cardName = "Gym Pass"
                )
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onImageSelected_withEmptyExtraction_defaultsToOtherAndOpensConfirmationWithoutPrefills() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                result = PhotoScanExtractionResult(
                    codeType = null,
                    codeValue = null,
                    cardNumber = null,
                    cardName = null
                )
            )
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(SmartScanEvent.OnImageSelected(Uri.parse("content://smart-scan/empty")))
        advanceUntilIdle()

        assertEquals(
            SmartScanEffect.OpenConfirmation(
                ManualEntryRoutes.smartScanConfirmation(
                    categoryId = null,
                    codeType = CardCodeType.OTHER,
                    codeValue = null,
                    cardNumber = null,
                    cardName = null
                )
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onBarcodeScanCancelled_returnsToIdleState() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor()
        )

        viewModel.onEvent(SmartScanEvent.OnBarcodeScanFailed)
        advanceUntilIdle()
        assertEquals(SmartScanStatus.FAILED, viewModel.uiState.value.status)

        viewModel.onEvent(SmartScanEvent.OnBarcodeScanCancelled)
        advanceUntilIdle()

        assertEquals(SmartScanStatus.IDLE, viewModel.uiState.value.status)
    }

    @Test
    fun onImageSelected_withExtractionFailure_updatesFailedState() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                error = IllegalStateException("bad image")
            )
        )

        viewModel.onEvent(SmartScanEvent.OnImageSelected(Uri.parse("content://smart-scan/failure")))
        advanceUntilIdle()

        assertEquals(SmartScanStatus.FAILED, viewModel.uiState.value.status)
    }

    @Test
    fun onLaunchFailed_updatesFailedState() = runTest {
        val viewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor()
        )

        viewModel.onEvent(SmartScanEvent.OnLaunchFailed)
        advanceUntilIdle()

        assertEquals(SmartScanStatus.FAILED, viewModel.uiState.value.status)
    }

    private class FakePhotoScanExtractor(
        private val result: PhotoScanExtractionResult = PhotoScanExtractionResult(
            codeType = CardCodeType.QR_CODE,
            codeValue = "DEFAULT-CODE",
            cardNumber = null,
            cardName = null
        ),
        private val error: Throwable? = null
    ) : PhotoScanExtractor {
        override suspend fun extractDetails(imageUri: Uri): PhotoScanExtractionResult {
            error?.let { throwable ->
                throw throwable
            }
            return result
        }
    }
}
