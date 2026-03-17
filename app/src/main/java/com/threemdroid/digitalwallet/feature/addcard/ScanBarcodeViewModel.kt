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
    private var hasAutoRequestedCameraPermission = false

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

            ScanBarcodeEvent.OnTakePhotoClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(
                        ScanBarcodeEffect.OpenRoute(
                            PhotoScanRoutes.photoScan(
                                categoryId = preselectedCategoryId,
                                launchAction = PhotoScanLaunchAction.TAKE_PHOTO
                            )
                        )
                    )
                }
            }

            ScanBarcodeEvent.OnChooseImageClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(
                        ScanBarcodeEffect.OpenRoute(
                            PhotoScanRoutes.photoScan(
                                categoryId = preselectedCategoryId,
                                launchAction = PhotoScanLaunchAction.CHOOSE_IMAGE
                            )
                        )
                    )
                }
            }

            ScanBarcodeEvent.OnTryOtherWayClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(
                        ScanBarcodeEffect.OpenRoute(
                            AddCardRoutes.alternativeMethods(preselectedCategoryId)
                        )
                    )
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
                    if (
                        uiState.value.status != ScanBarcodeStatus.ACTIVE &&
                            initializationTimeoutJob == null
                    ) {
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
                    if (
                        !hasAutoRequestedCameraPermission &&
                            uiState.value.status == ScanBarcodeStatus.PERMISSION_REQUIRED
                    ) {
                        hasAutoRequestedCameraPermission = true
                        viewModelScope.launch {
                            mutableEffects.emit(ScanBarcodeEffect.RequestCameraPermission)
                        }
                    }
                }
            }

            ScanBarcodeEvent.OnRetryClicked -> beginScannerInitialization()

            ScanBarcodeEvent.OnScannerInitializationFailed,
            ScanBarcodeEvent.OnScanProcessingFailed -> {
                mutableUiState.update { current ->
                    if (current.status == ScanBarcodeStatus.INITIALIZING) {
                        cancelInitializationTimeout()
                        current.copy(status = ScanBarcodeStatus.FAILED)
                    } else {
                        current
                    }
                }
            }

            ScanBarcodeEvent.OnScannerInitialized -> {
                mutableUiState.update { current ->
                    if (current.status == ScanBarcodeStatus.INITIALIZING) {
                        cancelInitializationTimeout()
                        current.copy(status = ScanBarcodeStatus.ACTIVE)
                    } else {
                        current
                    }
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
