package com.threemdroid.digitalwallet.feature.addcard

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.core.model.CardCodeType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SmartScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoScanExtractor: PhotoScanExtractor
) : ViewModel() {
    private val preselectedCategoryId: String? = savedStateHandle[AddCardRoutes.categoryIdArg]

    private val mutableUiState = MutableStateFlow(SmartScanUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<SmartScanEffect>(extraBufferCapacity = 1)
    val effects = mutableEffects.asSharedFlow()

    fun onEvent(event: SmartScanEvent) {
        when (event) {
            SmartScanEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(SmartScanEffect.NavigateBack)
                }
            }

            SmartScanEvent.OnScanCodeClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(SmartScanEffect.LaunchScanner)
                }
            }

            SmartScanEvent.OnTakePhotoClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(SmartScanEffect.LaunchCameraCapture)
                }
            }

            SmartScanEvent.OnChooseImageClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(SmartScanEffect.LaunchImagePicker)
                }
            }

            is SmartScanEvent.OnImageSelected -> processImage(event.uri)

            is SmartScanEvent.OnBarcodeScanSucceeded -> openConfirmation(
                codeType = event.codeType,
                codeValue = event.codeValue,
                cardNumber = null,
                cardName = null
            )

            SmartScanEvent.OnBarcodeScanCancelled -> {
                mutableUiState.update { current ->
                    current.copy(status = SmartScanStatus.IDLE)
                }
            }

            SmartScanEvent.OnBarcodeScanFailed,
            SmartScanEvent.OnLaunchFailed -> {
                mutableUiState.update { current ->
                    current.copy(status = SmartScanStatus.FAILED)
                }
            }
        }
    }

    private fun processImage(uri: Uri) {
        mutableUiState.update { current ->
            current.copy(status = SmartScanStatus.PROCESSING)
        }

        viewModelScope.launch {
            runCatching {
                photoScanExtractor.extractDetails(uri)
            }.onSuccess { extraction ->
                openConfirmation(
                    codeType = extraction.codeType ?: CardCodeType.OTHER,
                    codeValue = extraction.codeValue,
                    cardNumber = extraction.cardNumber,
                    cardName = extraction.cardName
                )
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(status = SmartScanStatus.FAILED)
                }
            }
        }
    }

    private fun openConfirmation(
        codeType: CardCodeType?,
        codeValue: String?,
        cardNumber: String?,
        cardName: String?
    ) {
        viewModelScope.launch {
            mutableEffects.emit(
                SmartScanEffect.OpenConfirmation(
                    ManualEntryRoutes.smartScanConfirmation(
                        categoryId = preselectedCategoryId,
                        codeType = codeType,
                        codeValue = codeValue,
                        cardNumber = cardNumber,
                        cardName = cardName
                    )
                )
            )
        }
    }
}
