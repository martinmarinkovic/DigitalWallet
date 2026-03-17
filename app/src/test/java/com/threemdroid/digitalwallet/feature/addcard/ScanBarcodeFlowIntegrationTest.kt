package com.threemdroid.digitalwallet.feature.addcard

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.R
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
class ScanBarcodeFlowIntegrationTest : BaseRepositoryTest() {
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
    fun scanFromCategoryContext_prefillsConfirmation_withoutAutoSave_andPersistsAfterSave() = runTest {
        val scanViewModel = ScanBarcodeViewModel(
            SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_access")
            )
        )
        val confirmationEffect = async { scanViewModel.effects.first() }

        scanViewModel.onEvent(
            ScanBarcodeEvent.OnScanSucceeded(
                codeType = com.threemdroid.digitalwallet.core.model.CardCodeType.QR_CODE,
                codeValue = "ACCESS-QR-123"
            )
        )
        advanceUntilIdle()

        val route = (confirmationEffect.await() as ScanBarcodeEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertEquals(R.string.scan_barcode_confirmation_title, initialState.titleRes)
        assertEquals("default_access", initialState.selectedCategoryId)
        assertEquals(
            com.threemdroid.digitalwallet.core.model.CardCodeType.QR_CODE,
            initialState.selectedCodeType
        )
        assertEquals("ACCESS-QR-123", initialState.codeValue)
        assertTrue(cardRepository.observeCards("default_access").first().isEmpty())

        confirmationViewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Office Badge"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnCardNumberChanged("12345"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnNotesChanged("Reception desk"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnFavoriteChanged(true))
        val savedEffect = async { confirmationViewModel.effects.first() }

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryEffect.CardSaved("default_access"), savedEffect.await())
        val savedCard = cardRepository.observeCards("default_access")
            .first { cards -> cards.isNotEmpty() }
            .single()
        assertEquals("Office Badge", savedCard.name)
        assertEquals("ACCESS-QR-123", savedCard.codeValue)
        assertEquals("12345", savedCard.cardNumber)
        assertEquals("Reception desk", savedCard.notes)
        assertTrue(savedCard.isFavorite)
        assertEquals(0, savedCard.position)
    }

    @Test
    fun scanFromGenericFlow_requiresCategoryBeforeSave_andPersistsAfterSelection() = runTest {
        val scanViewModel = ScanBarcodeViewModel(SavedStateHandle())
        val confirmationEffect = async { scanViewModel.effects.first() }

        scanViewModel.onEvent(
            ScanBarcodeEvent.OnScanSucceeded(
                codeType = com.threemdroid.digitalwallet.core.model.CardCodeType.CODE_128,
                codeValue = "LOYALTY-555"
            )
        )
        advanceUntilIdle()

        val route = (confirmationEffect.await() as ScanBarcodeEffect.OpenConfirmation).route
        val confirmationViewModel = ManualEntryViewModel(
            savedStateHandle = savedStateHandleFromRoute(route),
            categoryRepository = categoryRepository,
            cardRepository = cardRepository
        )

        val initialState = confirmationViewModel.uiState.first { state ->
            !state.isLoading && state.availableCategories.isNotEmpty()
        }
        assertEquals(R.string.scan_barcode_confirmation_title, initialState.titleRes)
        assertNull(initialState.selectedCategoryId)
        assertEquals(
            com.threemdroid.digitalwallet.core.model.CardCodeType.CODE_128,
            initialState.selectedCodeType
        )
        assertEquals("LOYALTY-555", initialState.codeValue)
        assertTrue(cardRepository.observeAllCards().first().isEmpty())

        confirmationViewModel.onEvent(ManualEntryEvent.OnCardNameChanged("Loyalty Card"))
        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(ManualEntryFieldError.REQUIRED, confirmationViewModel.uiState.value.categoryError)
        assertTrue(cardRepository.observeAllCards().first().isEmpty())

        confirmationViewModel.onEvent(ManualEntryEvent.OnCategorySelected("default_shopping_loyalty"))
        val savedEffect = async { confirmationViewModel.effects.first() }

        confirmationViewModel.onEvent(ManualEntryEvent.OnSaveClicked)
        advanceUntilIdle()

        assertEquals(
            ManualEntryEffect.CardSaved("default_shopping_loyalty"),
            savedEffect.await()
        )
        val savedCard = cardRepository.observeCards("default_shopping_loyalty")
            .first { cards -> cards.isNotEmpty() }
            .single()
        assertEquals("Loyalty Card", savedCard.name)
        assertEquals("LOYALTY-555", savedCard.codeValue)
        assertEquals(
            com.threemdroid.digitalwallet.core.model.CardCodeType.CODE_128,
            savedCard.codeType
        )
        assertEquals(0, savedCard.position)
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
}
