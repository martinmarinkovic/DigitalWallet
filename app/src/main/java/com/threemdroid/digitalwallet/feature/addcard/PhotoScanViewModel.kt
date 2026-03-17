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
class PhotoScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoScanExtractor: PhotoScanExtractor
) : ViewModel() {
    private val preselectedCategoryId: String? = savedStateHandle[AddCardRoutes.categoryIdArg]

    private val mutableUiState = MutableStateFlow(PhotoScanUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<PhotoScanEffect>(extraBufferCapacity = 1)
    val effects = mutableEffects.asSharedFlow()

    fun onEvent(event: PhotoScanEvent) {
        when (event) {
            PhotoScanEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(PhotoScanEffect.NavigateBack)
                }
            }

            PhotoScanEvent.OnTakePhotoClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(PhotoScanEffect.LaunchCameraCapture)
                }
            }

            PhotoScanEvent.OnChooseImageClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(PhotoScanEffect.LaunchImagePicker)
                }
            }

            PhotoScanEvent.OnLaunchFailed -> {
                mutableUiState.update { current ->
                    current.copy(status = PhotoScanStatus.FAILED)
                }
            }

            is PhotoScanEvent.OnImageSelected -> processImage(event.uri)
        }
    }

    private fun processImage(uri: Uri) {
        mutableUiState.update { current ->
            current.copy(status = PhotoScanStatus.PROCESSING)
        }

        viewModelScope.launch {
            runCatching {
                photoScanExtractor.extractDetails(uri)
            }.onSuccess { extraction ->
                mutableUiState.update { current ->
                    current.copy(status = PhotoScanStatus.IDLE)
                }
                mutableEffects.emit(
                    PhotoScanEffect.OpenConfirmation(
                        ManualEntryRoutes.photoScanConfirmation(
                            categoryId = preselectedCategoryId,
                            codeType = extraction.codeType ?: CardCodeType.OTHER,
                            codeValue = extraction.codeValue,
                            cardNumber = extraction.cardNumber,
                            cardName = extraction.cardName
                        )
                    )
                )
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(status = PhotoScanStatus.FAILED)
                }
            }
        }
    }
}
