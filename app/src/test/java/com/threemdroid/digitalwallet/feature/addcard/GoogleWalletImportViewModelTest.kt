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
class GoogleWalletImportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onChooseImageClicked_emitsLaunchImagePicker() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(),
            textParser = GoogleWalletImportTextParser()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(GoogleWalletImportEvent.OnChooseImageClicked)
        advanceUntilIdle()

        assertEquals(GoogleWalletImportEffect.LaunchImagePicker, deferredEffect.await())
    }

    @Test
    fun onImportTextClicked_withSupportedTextOpensConfirmationWithImportedHints() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_library")
            ),
            photoScanExtractor = FakePhotoScanExtractor(),
            textParser = GoogleWalletImportTextParser()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(
            GoogleWalletImportEvent.OnSharedTextChanged(
                """
                City Library
                Member number: LIB-7788
                Barcode: 1234567890
                https://pay.google.com/gp/v/save/test-pass
                """.trimIndent()
            )
        )
        viewModel.onEvent(GoogleWalletImportEvent.OnImportTextClicked)
        advanceUntilIdle()

        assertEquals(
            GoogleWalletImportEffect.OpenConfirmation(
                ManualEntryRoutes.googleWalletImportConfirmation(
                    categoryId = "default_library",
                    codeType = CardCodeType.OTHER,
                    codeValue = "1234567890",
                    cardNumber = "LIB-7788",
                    cardName = "City Library",
                    notes = """
                        City Library
                        Member number: LIB-7788
                        Barcode: 1234567890
                        https://pay.google.com/gp/v/save/test-pass
                    """.trimIndent()
                )
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onImportTextClicked_withBlankInputDoesNothing() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(),
            textParser = GoogleWalletImportTextParser()
        )

        viewModel.onEvent(GoogleWalletImportEvent.OnImportTextClicked)
        advanceUntilIdle()

        assertEquals(GoogleWalletImportStatus.IDLE, viewModel.uiState.value.status)
        assertEquals("", viewModel.uiState.value.sharedTextInput)
    }

    @Test
    fun onImportTextClicked_withOnlyShareLinkOpensConfirmationWithoutInventedFields() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(),
            textParser = GoogleWalletImportTextParser()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(
            GoogleWalletImportEvent.OnSharedTextChanged(
                "https://pay.google.com/gp/v/save/test-pass"
            )
        )
        viewModel.onEvent(GoogleWalletImportEvent.OnImportTextClicked)
        advanceUntilIdle()

        assertEquals(
            GoogleWalletImportEffect.OpenConfirmation(
                ManualEntryRoutes.googleWalletImportConfirmation(
                    categoryId = null,
                    codeType = CardCodeType.OTHER,
                    codeValue = null,
                    cardNumber = null,
                    cardName = null,
                    notes = "https://pay.google.com/gp/v/save/test-pass"
                )
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onImageSelected_withSuccessfulExtractionOpensConfirmation() = runTest {
        val viewModel = GoogleWalletImportViewModel(
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
            ),
            textParser = GoogleWalletImportTextParser()
        )
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(GoogleWalletImportEvent.OnImageSelected(Uri.parse("content://wallet-import/image")))
        advanceUntilIdle()

        assertEquals(
            GoogleWalletImportEffect.OpenConfirmation(
                ManualEntryRoutes.googleWalletImportConfirmation(
                    categoryId = "default_access",
                    codeType = CardCodeType.CODE_128,
                    codeValue = "ACCESS-4455",
                    cardNumber = "4455",
                    cardName = "Office Badge",
                    notes = null
                )
            ),
            deferredEffect.await()
        )
    }

    @Test
    fun onImageSelected_withExtractionFailureUpdatesFailedState() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                error = IllegalStateException("bad image")
            ),
            textParser = GoogleWalletImportTextParser()
        )

        viewModel.onEvent(GoogleWalletImportEvent.OnImageSelected(Uri.parse("content://wallet-import/bad")))
        advanceUntilIdle()

        assertEquals(GoogleWalletImportStatus.FAILED, viewModel.uiState.value.status)
    }

    @Test
    fun onLaunchFailed_updatesFailedState() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(),
            textParser = GoogleWalletImportTextParser()
        )

        viewModel.onEvent(GoogleWalletImportEvent.OnLaunchFailed)
        advanceUntilIdle()

        assertEquals(GoogleWalletImportStatus.FAILED, viewModel.uiState.value.status)
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
