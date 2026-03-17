package com.threemdroid.digitalwallet.feature.addcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScanBarcodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val preselectedCategoryId: String? = savedStateHandle[AddCardRoutes.categoryIdArg]

    private val mutableUiState = MutableStateFlow(ScanBarcodeUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<ScanBarcodeEffect>(extraBufferCapacity = 1)
    val effects = mutableEffects.asSharedFlow()

    private var hasOpenedScanner = false

    fun onEvent(event: ScanBarcodeEvent) {
        when (event) {
            ScanBarcodeEvent.OnScreenOpened -> {
                if (!hasOpenedScanner) {
                    hasOpenedScanner = true
                    launchScanner()
                }
            }

            ScanBarcodeEvent.OnRetryClicked -> launchScanner()

            ScanBarcodeEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(ScanBarcodeEffect.NavigateBack)
                }
            }

            is ScanBarcodeEvent.OnScanSucceeded -> {
                viewModelScope.launch {
                    mutableEffects.emit(
                        ScanBarcodeEffect.OpenConfirmation(
                            ManualEntryRoutes.scanConfirmation(
                                categoryId = preselectedCategoryId,
                                codeType = event.codeType,
                                codeValue = event.codeValue
                            )
                        )
                    )
                }
            }

            ScanBarcodeEvent.OnScanCancelled -> {
                mutableUiState.update { current ->
                    current.copy(status = ScanBarcodeStatus.CANCELLED)
                }
            }

            ScanBarcodeEvent.OnScanFailed -> {
                mutableUiState.update { current ->
                    current.copy(status = ScanBarcodeStatus.FAILED)
                }
            }
        }
    }

    private fun launchScanner() {
        mutableUiState.update { current ->
            current.copy(status = ScanBarcodeStatus.LAUNCHING)
        }
        viewModelScope.launch {
            mutableEffects.emit(ScanBarcodeEffect.LaunchScanner)
        }
    }
}
