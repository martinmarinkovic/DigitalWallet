package com.threemdroid.digitalwallet.feature.addcard

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.data.card.OfflineFirstCardRepository
import com.threemdroid.digitalwallet.data.category.OfflineFirstCategoryRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoScanFlowIntegrationTest : BaseRepositoryTest() {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryRepository by lazy {
        OfflineFirstCategoryRepository(
            database = database,
            categoryDao = database.categoryDao()
        )
    }

    private val cardRepository by lazy {
        OfflineFirstCardRepository(
            database = database,
            cardDao = database.cardDao(),
            categoryDao = database.categoryDao()
        )
    }

    @Test
    fun photoScanFromCategoryContext_prefillsConfirmationAndPersistsAfterSave() = runTest {
        val photoScanViewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_access")
            ),
            photoScanExtractor = FakePhotoScanExtractor(
                PhotoScanExtractionResult(
                    codeType = CardCodeType.CODE_128,
                    codeValue = "ACCESS-4455",
                    cardNumber = "4455",
                    cardName = "Office Badge"
                )
            )
        )
        val confirmationEffect = async { photoScanViewModel.effects.first() }

        photoScanViewModel.onEvent(PhotoScanEvent.OnImageSelected(Uri.parse("content://photo-scan/access")))
        advanceUntilIdle()

        val route = (confirmationEffect.await() as PhotoScanEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertEquals(R.string.scan_card_photo_confirmation_title, initialState.titleRes)
        assertEquals(R.string.scan_card_photo_review_message, initialState.reviewMessageRes)
        assertEquals("default_access", initialState.selectedCategoryId)
        assertEquals("Office Badge", initialState.cardName)
        assertEquals("4455", initialState.cardNumber)
        assertEquals("ACCESS-4455", initialState.codeValue)
        assertTrue(cardRepository.observeCards("default_access").first().isEmpty())

        val savedEffect = async { confirmationViewModel.effects.first() }

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryEffect.CardSaved("default_access"), savedEffect.await())
        val savedCard = cardRepository.observeCards("default_access")
            .first { cards -> cards.isNotEmpty() }
            .single()
        assertEquals("Office Badge", savedCard.name)
        assertEquals("4455", savedCard.cardNumber)
        assertEquals("ACCESS-4455", savedCard.codeValue)
        assertEquals(CardCodeType.CODE_128, savedCard.codeType)
    }

    @Test
    fun photoScanFromGenericFlow_requiresCategoryBeforeSave() = runTest {
        val photoScanViewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                PhotoScanExtractionResult(
                    codeType = CardCodeType.OTHER,
                    codeValue = "LIB-CODE-7788",
                    cardNumber = "LIB-7788",
                    cardName = "City Library"
                )
            )
        )
        val confirmationEffect = async { photoScanViewModel.effects.first() }

        photoScanViewModel.onEvent(PhotoScanEvent.OnImageSelected(Uri.parse("content://photo-scan/library")))
        advanceUntilIdle()

        val route = (confirmationEffect.await() as PhotoScanEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertNull(initialState.selectedCategoryId)
        assertEquals("City Library", initialState.cardName)
        assertEquals("LIB-7788", initialState.cardNumber)
        assertEquals("LIB-CODE-7788", initialState.codeValue)

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryFieldError.REQUIRED, confirmationViewModel.uiState.value.categoryError)
        assertTrue(cardRepository.observeAllCards().first().isEmpty())
    }

    @Test
    fun photoScanWithEmptyExtraction_doesNotAutoSaveAndRequiresManualReviewFields() = runTest {
        val photoScanViewModel = PhotoScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                PhotoScanExtractionResult(
                    codeType = null,
                    codeValue = null,
                    cardNumber = null,
                    cardName = null
                )
            )
        )
        val confirmationEffect = async { photoScanViewModel.effects.first() }

        photoScanViewModel.onEvent(PhotoScanEvent.OnImageSelected(Uri.parse("content://photo-scan/empty")))
        advanceUntilIdle()

        assertTrue(cardRepository.observeAllCards().first().isEmpty())

        val route = (confirmationEffect.await() as PhotoScanEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertNull(initialState.selectedCategoryId)
        assertEquals("", initialState.cardName)
        assertEquals("", initialState.cardNumber)
        assertEquals("", initialState.codeValue)
        assertEquals(CardCodeType.OTHER, initialState.selectedCodeType)

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryFieldError.REQUIRED, confirmationViewModel.uiState.value.cardNameError)
        assertEquals(ManualEntryFieldError.REQUIRED, confirmationViewModel.uiState.value.categoryError)
        assertEquals(ManualEntryFieldError.REQUIRED, confirmationViewModel.uiState.value.codeValueError)
        assertTrue(cardRepository.observeAllCards().first().isEmpty())
    }

    private fun savedStateHandleFromRoute(route: String): SavedStateHandle {
        val query = route.substringAfter('?', missingDelimiterValue = "")
        if (query.isBlank()) {
            return SavedStateHandle()
        }

        val arguments = query
            .split('&')
            .filter { it.isNotBlank() }
            .associate { part ->
                val key = part.substringBefore('=')
                val rawValue = part.substringAfter('=', "")
                key to URLDecoder.decode(rawValue, StandardCharsets.UTF_8.toString())
            }

        return SavedStateHandle(arguments)
    }

    private class FakePhotoScanExtractor(
        private val result: PhotoScanExtractionResult
    ) : PhotoScanExtractor {
        override suspend fun extractDetails(imageUri: Uri): PhotoScanExtractionResult = result
    }
}
