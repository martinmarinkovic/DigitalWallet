package com.threemdroid.digitalwallet.feature.addcard

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddCardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_exposesExpectedMethodOptionsInOrder() = runTest {
        val viewModel = AddCardViewModel(SavedStateHandle())

        advanceUntilIdle()

        assertEquals(
            listOf(
                R.string.add_card_method_scan_barcode_qr,
                R.string.add_card_method_scan_card_photo,
                R.string.add_card_method_manual_entry,
                R.string.add_card_method_import_google_wallet
            ),
            viewModel.uiState.value.methods.map { method -> method.titleRes }
        )
    }

    @Test
    fun onMethodClicked_withoutCategoryContext_emitsExpectedRoutes() = runTest {
        val viewModel = AddCardViewModel(SavedStateHandle())

        AddCardMethod.entries.forEach { method ->
            val deferredEffect = async { viewModel.effects.first() }

            viewModel.onEvent(AddCardEvent.OnMethodClicked(method))
            advanceUntilIdle()

            assertEquals(
                AddCardEffect.OpenMethod(method.destinationRoute(preselectedCategoryId = null)),
                deferredEffect.await()
            )
        }
    }

    @Test
    fun onMethodClicked_withCategoryContext_passesPreselectedCategoryToRoutes() = runTest {
        val viewModel = AddCardViewModel(
            SavedStateHandle(
                mapOf(AddCardRoutes.categoryIdArg to "default_access")
            )
        )

        assertEquals(
            listOf(
                AddCardEffect.OpenMethod(ScanBarcodeRoutes.scan("default_access")),
                AddCardEffect.OpenMethod(PhotoScanRoutes.photoScan("default_access")),
                AddCardEffect.OpenMethod(ManualEntryRoutes.manualEntry("default_access")),
                AddCardEffect.OpenMethod(GoogleWalletImportRoutes.googleWalletImport("default_access"))
            ),
            listOf(
                AddCardMethod.SCAN_BARCODE_QR,
                AddCardMethod.SCAN_CARD_PHOTO,
                AddCardMethod.MANUAL_ENTRY,
                AddCardMethod.IMPORT_GOOGLE_WALLET
            ).map { method ->
                val deferredEffect = async { viewModel.effects.first() }
                viewModel.onEvent(AddCardEvent.OnMethodClicked(method))
                advanceUntilIdle()
                deferredEffect.await()
            }
        )
    }

    @Test
    fun onBackClicked_emitsNavigateBack() = runTest {
        val viewModel = AddCardViewModel(SavedStateHandle())
        val deferredEffect = async { viewModel.effects.first() }

        viewModel.onEvent(AddCardEvent.OnBackClicked)
        advanceUntilIdle()

        assertEquals(AddCardEffect.NavigateBack, deferredEffect.await())
    }
}
