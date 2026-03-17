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
class GoogleWalletImportFlowIntegrationTest : BaseRepositoryTest() {
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
    fun googleWalletImportFromCategoryContext_prefillsConfirmationAndPersistsAfterSave() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_library")
            ),
            photoScanExtractor = FakePhotoScanExtractor(),
            textParser = GoogleWalletImportTextParser()
        )
        val confirmationEffect = async { viewModel.effects.first() }

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
        assertTrue(cardRepository.observeCards("default_library").first().isEmpty())

        val route = (confirmationEffect.await() as GoogleWalletImportEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertEquals(R.string.google_wallet_import_confirmation_title, initialState.titleRes)
        assertEquals(R.string.google_wallet_import_review_message, initialState.reviewMessageRes)
        assertEquals("default_library", initialState.selectedCategoryId)
        assertEquals("City Library", initialState.cardName)
        assertEquals("LIB-7788", initialState.cardNumber)
        assertEquals("1234567890", initialState.codeValue)
        assertTrue(initialState.notes.contains("https://pay.google.com/gp/v/save/test-pass"))

        val savedEffect = async { confirmationViewModel.effects.first() }

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryEffect.CardSaved("default_library"), savedEffect.await())
        val savedCard = cardRepository.observeCards("default_library")
            .first { cards -> cards.isNotEmpty() }
            .single()
        assertEquals("City Library", savedCard.name)
        assertEquals("LIB-7788", savedCard.cardNumber)
        assertEquals("1234567890", savedCard.codeValue)
        assertTrue((savedCard.notes ?: "").contains("https://pay.google.com/gp/v/save/test-pass"))
        assertEquals(CardCodeType.OTHER, savedCard.codeType)
    }

    @Test
    fun googleWalletImportFromGenericFlow_doesNotAutoAssignCategoryAndRequiresUserSelection() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(),
            textParser = GoogleWalletImportTextParser()
        )
        val confirmationEffect = async { viewModel.effects.first() }

        viewModel.onEvent(
            GoogleWalletImportEvent.OnSharedTextChanged(
                """
                Office Badge
                Member number: 4455
                Barcode: ACCESS-4455
                """.trimIndent()
            )
        )
        viewModel.onEvent(GoogleWalletImportEvent.OnImportTextClicked)
        advanceUntilIdle()
        assertTrue(cardRepository.observeAllCards().first().isEmpty())

        val route = (confirmationEffect.await() as GoogleWalletImportEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertNull(initialState.selectedCategoryId)
        assertEquals("Office Badge", initialState.cardName)
        assertEquals("4455", initialState.cardNumber)
        assertEquals("ACCESS-4455", initialState.codeValue)

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryFieldError.REQUIRED, confirmationViewModel.uiState.value.categoryError)
        assertTrue(cardRepository.observeAllCards().first().isEmpty())
    }

    @Test
    fun googleWalletImportWithShareLinkOnly_requiresManualCompletionBeforePersisting() = runTest {
        val viewModel = GoogleWalletImportViewModel(
            savedStateHandle = SavedStateHandle(),
            photoScanExtractor = FakePhotoScanExtractor(),
            textParser = GoogleWalletImportTextParser()
        )
        val confirmationEffect = async { viewModel.effects.first() }

        viewModel.onEvent(
            GoogleWalletImportEvent.OnSharedTextChanged(
                "https://pay.google.com/gp/v/save/test-pass"
            )
        )
        viewModel.onEvent(GoogleWalletImportEvent.OnImportTextClicked)
        advanceUntilIdle()
        assertTrue(cardRepository.observeAllCards().first().isEmpty())

        val route = (confirmationEffect.await() as GoogleWalletImportEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertNull(initialState.selectedCategoryId)
        assertEquals(CardCodeType.OTHER, initialState.selectedCodeType)
        assertEquals("", initialState.cardName)
        assertEquals("", initialState.cardNumber)
        assertEquals("", initialState.codeValue)
        assertTrue(initialState.notes.contains("https://pay.google.com/gp/v/save/test-pass"))

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        val invalidState = confirmationViewModel.uiState.value
        assertEquals(ManualEntryFieldError.REQUIRED, invalidState.cardNameError)
        assertEquals(ManualEntryFieldError.REQUIRED, invalidState.categoryError)
        assertEquals(ManualEntryFieldError.REQUIRED, invalidState.codeValueError)
        assertTrue(cardRepository.observeAllCards().first().isEmpty())

        val savedEffect = async { confirmationViewModel.effects.first() }

        confirmationViewModel.onEvent(ManualEntryEvent.OnCategorySelected("default_access"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Imported Access Pass"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnCodeValueChanged("ACCESS-7788"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnCardNumberChanged("7788"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryEffect.CardSaved("default_access"), savedEffect.await())
        val savedCard = cardRepository.observeCards("default_access")
            .first { cards -> cards.isNotEmpty() }
            .single()
        assertEquals("Imported Access Pass", savedCard.name)
        assertEquals("7788", savedCard.cardNumber)
        assertEquals("ACCESS-7788", savedCard.codeValue)
        assertEquals(CardCodeType.OTHER, savedCard.codeType)
        assertTrue((savedCard.notes ?: "").contains("https://pay.google.com/gp/v/save/test-pass"))
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

    private class FakePhotoScanExtractor : PhotoScanExtractor {
        override suspend fun extractDetails(imageUri: android.net.Uri): PhotoScanExtractionResult {
            error("Not needed for text-based GoogleWalletImportFlowIntegrationTest")
        }
    }
}
