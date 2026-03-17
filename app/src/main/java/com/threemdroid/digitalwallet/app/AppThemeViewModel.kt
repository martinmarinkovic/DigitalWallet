package com.threemdroid.digitalwallet.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppThemeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AppThemeUiState())
    val uiState = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                mutableUiState.update { current ->
                    current.copy(themeMode = settings.themeMode)
                }
            }
        }
    }
}
