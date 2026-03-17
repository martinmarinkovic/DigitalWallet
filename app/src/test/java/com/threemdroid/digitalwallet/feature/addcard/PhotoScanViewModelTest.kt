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
class PhotoScanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onTakePhotoClicked_emitsLaunchCameraCapture() = runTest {
        val viewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(PhotoScanEvent.OnTakePhotoClicked)
        advanceUntilIdle()

        assertEquals(PhotoScanEffect.LaunchCameraCapture, deferredEffect.await())
    }

    @Test
    fun onChooseImageClicked_emitsLaunchImagePicker() = runTest {
        val viewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(PhotoScanEvent.OnChooseImageClicked)
        advanceUntilIdle()

        assertEquals(PhotoScanEffect.LaunchImagePicker, deferredEffect.await())
    }

    @Test
    fun onImageSelected_withSuccessfulExtraction_opensConfirmationWithExtractedFields() = runTest {
        val viewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_access")
            ),
            photoScanExtractor = FakePhotoScanExtractor(
                result = PhotoScanExtractionResult(
                    codeType = CardCodeType.CODE_128,
                    codeValue = "ACCESS-4455",
                    cardNumber = "4455",
                    cardName = "Office Badge"
                )
            )
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(PhotoScanEvent.OnImageSelected(Uri.parse("content://photo-scan/success")))
        advanceUntilIdle()

        assertEquals(
            PhotoScanEffect.OpenConfirmation(
                ManualEntryRoutes.photoScanConfirmation(
                    categoryId = "default_access",
                    codeType = CardCodeType.CODE_128,
                    codeValue = "ACCESS-4455",
                    cardNumber = "4455",
                    cardName = "Office Badge"
                )
            ),
            deferredEffect.await()
        )
        assertEquals(PhotoScanStatus.IDLE, viewModel.uiState.value.status)
    }

    @Test
    fun onImageSelected_withPartialExtraction_defaultsCodeTypeAndStillOpensConfirmation() = runTest {
        val viewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                result = PhotoScanExtractionResult(
                    codeType = null,
                    codeValue = null,
                    cardNumber = "LIB-7788",
                    cardName = "City Library"
                )
            )
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(PhotoScanEvent.OnImageSelected(Uri.parse("content://photo-scan/partial")))
        advanceUntilIdle()

        assertEquals(
            PhotoScanEffect.OpenConfirmation(
                ManualEntryRoutes.photoScanConfirmation(
                    categoryId = null,
                    codeType = CardCodeType.OTHER,
                    codeValue = null,
                    cardNumber = "LIB-7788",
                    cardName = "City Library"
                )
            ),
            deferredEffect.await()
        )
        assertEquals(PhotoScanStatus.IDLE, viewModel.uiState.value.status)
    }

    @Test
    fun onImageSelected_withEmptyExtraction_opensConfirmationWithoutPrefills() = runTest {
        val viewModel = PhotoScanViewModel(
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

        viewModel.onEvent(PhotoScanEvent.OnImageSelected(Uri.parse("content://photo-scan/empty")))
        advanceUntilIdle()

        assertEquals(
            PhotoScanEffect.OpenConfirmation(
                ManualEntryRoutes.photoScanConfirmation(
                    categoryId = null,
                    codeType = CardCodeType.OTHER,
                    codeValue = null,
                    cardNumber = null,
                    cardName = null
                )
            ),
            deferredEffect.await()
        )
        assertEquals(PhotoScanStatus.IDLE, viewModel.uiState.value.status)
    }

    @Test
    fun onImageSelected_withExtractionFailure_updatesFailedState() = runTest {
        val viewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                error = IllegalStateException("bad image")
            )
        )

        viewModel.onEvent(PhotoScanEvent.OnImageSelected(Uri.parse("content://photo-scan/failure")))
        advanceUntilIdle()

        assertEquals(PhotoScanStatus.FAILED, viewModel.uiState.value.status)
    }

    @Test
    fun onLaunchFailed_updatesFailedState() = runTest {
        val viewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor()
        )

        viewModel.onEvent(PhotoScanEvent.OnLaunchFailed)
        advanceUntilIdle()

        assertEquals(PhotoScanStatus.FAILED, viewModel.uiState.value.status)
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
