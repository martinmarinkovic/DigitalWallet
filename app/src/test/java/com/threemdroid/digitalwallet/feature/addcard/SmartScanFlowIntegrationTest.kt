package com.threemdroid.digitalwallet.feature.addcard

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
class SmartScanFlowIntegrationTest : BaseRepositoryTest() {
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
    fun smartScanFromCategoryContext_prefillsConfirmationAndPersistsAfterSave() = runTest {
        val smartScanViewModel = SmartScanViewModel(
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
        val confirmationEffect = async { smartScanViewModel.effects.first() }

        smartScanViewModel.onEvent(SmartScanEvent.OnImageSelected(android.net.Uri.parse("content://smart-scan/access")))
        advanceUntilIdle()

        val route = (confirmationEffect.await() as SmartScanEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertEquals(R.string.smart_scan_confirmation_title, initialState.titleRes)
        assertEquals(R.string.smart_scan_review_message, initialState.reviewMessageRes)
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
    fun smartScanLiveCodeFromGenericFlow_requiresCategoryAndDoesNotAutoAssignOne() = runTest {
        val smartScanViewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                PhotoScanExtractionResult(
                    codeType = CardCodeType.OTHER,
                    codeValue = "UNUSED",
                    cardNumber = null,
                    cardName = null
                )
            )
        )
        val confirmationEffect = async { smartScanViewModel.effects.first() }

        smartScanViewModel.onEvent(
            SmartScanEvent.OnBarcodeScanSucceeded(
                codeType = CardCodeType.QR_CODE,
                codeValue = "LIB-CODE-7788"
            )
        )
        advanceUntilIdle()
        assertTrue(cardRepository.observeAllCards().first().isEmpty())

        val route = (confirmationEffect.await() as SmartScanEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertNull(initialState.selectedCategoryId)
        assertEquals(CardCodeType.QR_CODE, initialState.selectedCodeType)
        assertEquals("LIB-CODE-7788", initialState.codeValue)

        confirmationViewModel.onEvent(ManualEntryEvent.OnCardNameChanged("City Library"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryFieldError.REQUIRED, confirmationViewModel.uiState.value.categoryError)
        assertTrue(cardRepository.observeAllCards().first().isEmpty())
    }

    @Test
    fun smartScanImageHintsFromGenericFlow_doNotAutoAssignCategoryOrPersistBeforeConfirmation() = runTest {
        val smartScanViewModel = SmartScanViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(
                PhotoScanExtractionResult(
                    codeType = CardCodeType.CODE_128,
                    codeValue = "ACCESS-4455",
                    cardNumber = "4455",
                    cardName = "Office Badge"
                )
            )
        )
        val confirmationEffect = async { smartScanViewModel.effects.first() }

        smartScanViewModel.onEvent(
            SmartScanEvent.OnImageSelected(android.net.Uri.parse("content://smart-scan/generic-image"))
        )
        advanceUntilIdle()
        assertTrue(cardRepository.observeAllCards().first().isEmpty())

        val route = (confirmationEffect.await() as SmartScanEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertNull(initialState.selectedCategoryId)
        assertEquals(R.string.smart_scan_confirmation_title, initialState.titleRes)
        assertEquals(R.string.smart_scan_review_message, initialState.reviewMessageRes)
        assertEquals("Office Badge", initialState.cardName)
        assertEquals("4455", initialState.cardNumber)
        assertEquals("ACCESS-4455", initialState.codeValue)
        assertEquals(CardCodeType.CODE_128, initialState.selectedCodeType)

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryFieldError.REQUIRED, confirmationViewModel.uiState.value.categoryError)
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
        override suspend fun extractDetails(imageUri: android.net.Uri): PhotoScanExtractionResult = result
    }
}
