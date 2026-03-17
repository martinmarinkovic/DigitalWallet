package com.threemdroid.digitalwallet.feature.fullscreencode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.core.model.displayLabel
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class FullscreenCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val cardId: String? = savedStateHandle[FullscreenCodeRoutes.cardIdArg]

    private val mutableUiState = MutableStateFlow(FullscreenCodeUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<FullscreenCodeEffect>()
    val effects = mutableEffects.asSharedFlow()

    init {
        val resolvedCardId = cardId
        viewModelScope.launch {
            if (resolvedCardId == null) {
                settingsRepository.observeSettings().collect { settings ->
                    mutableUiState.value = FullscreenCodeUiState(
                        isLoading = false,
                        isCardMissing = true,
                        shouldMaximizeBrightness = settings.autoBrightnessEnabled
                    )
                }
            } else {
                combine(
                    cardRepository.observeCard(resolvedCardId),
                    settingsRepository.observeSettings()
                ) { card, settings ->
                    card.toUiState(settings)
                }.collect { state ->
                    mutableUiState.value = state
                }
            }
        }
    }

    fun onEvent(event: FullscreenCodeEvent) {
        when (event) {
            FullscreenCodeEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(FullscreenCodeEffect.NavigateBack)
                }
            }
        }
    }

    private fun WalletCard?.toUiState(settings: AppSettings): FullscreenCodeUiState {
        if (this == null) {
            return FullscreenCodeUiState(
                isLoading = false,
                isCardMissing = true,
                shouldMaximizeBrightness = settings.autoBrightnessEnabled
            )
        }

        return FullscreenCodeUiState(
            isLoading = false,
            isCardMissing = false,
            cardName = name,
            cardNumber = cardNumber?.trim()?.takeIf { value -> value.isNotBlank() },
            codeValue = codeValue,
            codeType = codeType,
            codeTypeLabel = codeType.displayLabel(),
            presentation = codeType.toFullscreenPresentation(),
            shouldMaximizeBrightness = settings.autoBrightnessEnabled
        )
    }
}
