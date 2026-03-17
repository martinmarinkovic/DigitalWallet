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
class GoogleWalletImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoScanExtractor: PhotoScanExtractor,
    private val textParser: GoogleWalletImportTextParser
) : ViewModel() {
    private val preselectedCategoryId: String? = savedStateHandle[AddCardRoutes.categoryIdArg]

    private val mutableUiState = MutableStateFlow(GoogleWalletImportUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<GoogleWalletImportEffect>(extraBufferCapacity = 1)
    val effects = mutableEffects.asSharedFlow()

    fun onEvent(event: GoogleWalletImportEvent) {
        when (event) {
            GoogleWalletImportEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(GoogleWalletImportEffect.NavigateBack)
                }
            }

            is GoogleWalletImportEvent.OnSharedTextChanged -> {
                mutableUiState.update { current ->
                    current.copy(
                        sharedTextInput = event.value,
                        status = GoogleWalletImportStatus.IDLE
                    )
                }
            }

            GoogleWalletImportEvent.OnImportTextClicked -> importSharedText()

            GoogleWalletImportEvent.OnChooseImageClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(GoogleWalletImportEffect.LaunchImagePicker)
                }
            }

            GoogleWalletImportEvent.OnLaunchFailed -> {
                mutableUiState.update { current ->
                    current.copy(status = GoogleWalletImportStatus.FAILED)
                }
            }

            is GoogleWalletImportEvent.OnImageSelected -> processImage(event.uri)
        }
    }

    private fun importSharedText() {
        val rawInput = uiState.value.sharedTextInput.trim()
        if (rawInput.isBlank()) {
            return
        }

        val draft = textParser.parse(rawInput)
        openConfirmation(
            codeType = draft.codeType ?: CardCodeType.OTHER,
            codeValue = draft.codeValue,
            cardNumber = draft.cardNumber,
            cardName = draft.cardName,
            notes = draft.notes
        )
    }

    private fun processImage(uri: Uri) {
        mutableUiState.update { current ->
            current.copy(status = GoogleWalletImportStatus.PROCESSING)
        }

        viewModelScope.launch {
            runCatching {
                photoScanExtractor.extractDetails(uri)
            }.onSuccess { extraction ->
                openConfirmation(
                    codeType = extraction.codeType ?: CardCodeType.OTHER,
                    codeValue = extraction.codeValue,
                    cardNumber = extraction.cardNumber,
                    cardName = extraction.cardName,
                    notes = null
                )
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(status = GoogleWalletImportStatus.FAILED)
                }
            }
        }
    }

    private fun openConfirmation(
        codeType: CardCodeType,
        codeValue: String?,
        cardNumber: String?,
        cardName: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            mutableEffects.emit(
                GoogleWalletImportEffect.OpenConfirmation(
                    ManualEntryRoutes.googleWalletImportConfirmation(
                        categoryId = preselectedCategoryId,
                        codeType = codeType,
                        codeValue = codeValue,
                        cardNumber = cardNumber,
                        cardName = cardName,
                        notes = notes
                    )
                )
            )
        }
    }
}
