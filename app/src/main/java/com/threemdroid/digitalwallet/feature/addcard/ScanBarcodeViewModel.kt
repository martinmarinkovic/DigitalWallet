package com.threemdroid.digitalwallet.feature.addcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private companion object {
        const val scannerInitializationTimeoutMs = 8_000L
    }

    private val preselectedCategoryId: String? = savedStateHandle[AddCardRoutes.categoryIdArg]

    private val mutableUiState = MutableStateFlow(ScanBarcodeUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<ScanBarcodeEffect>(extraBufferCapacity = 1)
    val effects = mutableEffects.asSharedFlow()

    private var initializationTimeoutJob: Job? = null

    fun onEvent(event: ScanBarcodeEvent) {
        when (event) {
            ScanBarcodeEvent.OnBackClicked -> {
                cancelInitializationTimeout()
                viewModelScope.launch {
                    mutableEffects.emit(ScanBarcodeEffect.NavigateBack)
                }
            }

            ScanBarcodeEvent.OnOpenSettingsClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(ScanBarcodeEffect.OpenAppSettings)
                }
            }

            ScanBarcodeEvent.OnPermissionButtonClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(ScanBarcodeEffect.RequestCameraPermission)
                }
            }

            is ScanBarcodeEvent.OnPermissionRequestResult -> {
                if (event.granted) {
                    beginScannerInitialization()
                } else {
                    cancelInitializationTimeout()
                    mutableUiState.update { current ->
                        current.copy(
                            status = if (event.shouldShowRationale) {
                                ScanBarcodeStatus.PERMISSION_REQUIRED
                            } else {
                                ScanBarcodeStatus.PERMISSION_BLOCKED
                            }
                        )
                    }
                }
            }

            is ScanBarcodeEvent.OnPermissionStateResolved -> {
                if (event.granted) {
                    if (uiState.value.status != ScanBarcodeStatus.ACTIVE) {
                        beginScannerInitialization()
                    }
                } else {
                    cancelInitializationTimeout()
                    mutableUiState.update { current ->
                        current.copy(
                            status = if (current.status == ScanBarcodeStatus.PERMISSION_BLOCKED) {
                                ScanBarcodeStatus.PERMISSION_BLOCKED
                            } else {
                                ScanBarcodeStatus.PERMISSION_REQUIRED
                            }
                        )
                    }
                }
            }

            ScanBarcodeEvent.OnRetryClicked -> beginScannerInitialization()

            ScanBarcodeEvent.OnScannerInitializationFailed,
            ScanBarcodeEvent.OnScanProcessingFailed -> {
                cancelInitializationTimeout()
                mutableUiState.update { current ->
                    current.copy(status = ScanBarcodeStatus.FAILED)
                }
            }

            ScanBarcodeEvent.OnScannerInitialized -> {
                cancelInitializationTimeout()
                mutableUiState.update { current ->
                    current.copy(status = ScanBarcodeStatus.ACTIVE)
                }
            }

            is ScanBarcodeEvent.OnScanSucceeded -> {
                cancelInitializationTimeout()
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
        }
    }

    private fun beginScannerInitialization() {
        cancelInitializationTimeout()
        mutableUiState.update { current ->
            current.copy(status = ScanBarcodeStatus.INITIALIZING)
        }
        initializationTimeoutJob = viewModelScope.launch {
            delay(scannerInitializationTimeoutMs)
            mutableUiState.update { current ->
                if (current.status == ScanBarcodeStatus.INITIALIZING) {
                    current.copy(status = ScanBarcodeStatus.FAILED)
                } else {
                    current
                }
            }
        }
    }

    private fun cancelInitializationTimeout() {
        initializationTimeoutJob?.cancel()
        initializationTimeoutJob = null
    }
}
