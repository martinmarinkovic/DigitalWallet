package com.threemdroid.digitalwallet.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threemdroid.digitalwallet.data.category.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CreateCategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CreateCategoryUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<CreateCategoryEffect>()
    val effects = mutableEffects.asSharedFlow()

    fun onEvent(event: CreateCategoryEvent) {
        when (event) {
            is CreateCategoryEvent.OnColorSelected -> {
                mutableUiState.update { state ->
                    state.copy(
                        selectedColorHex = event.colorHex,
                        isSaveErrorVisible = false
                    )
                }
            }

            CreateCategoryEvent.OnDismissRequested -> {
                viewModelScope.launch {
                    mutableEffects.emit(CreateCategoryEffect.Dismiss)
                }
            }

            is CreateCategoryEvent.OnNameChanged -> {
                mutableUiState.update { state ->
                    state.copy(
                        name = event.name,
                        nameError = null,
                        isSaveErrorVisible = false
                    )
                }
            }

            CreateCategoryEvent.OnSaveClicked -> {
                saveCategory()
            }
        }
    }

    private fun saveCategory() {
        val currentState = mutableUiState.value
        if (currentState.isSaving) {
            return
        }

        val normalizedName = currentState.name.trim()
        if (normalizedName.isEmpty()) {
            mutableUiState.update { state ->
                state.copy(
                    name = normalizedName,
                    nameError = CreateCategoryNameError.REQUIRED,
                    isSaveErrorVisible = false
                )
            }
            return
        }

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    name = normalizedName,
                    nameError = null,
                    isSaving = true,
                    isSaveErrorVisible = false
                )
            }

            runCatching {
                categoryRepository.createCustomCategory(
                    name = normalizedName,
                    color = mutableUiState.value.selectedColorHex
                )
            }.onSuccess {
                mutableUiState.update { state ->
                    state.copy(isSaving = false)
                }
                mutableEffects.emit(CreateCategoryEffect.Dismiss)
            }.onFailure {
                mutableUiState.update { state ->
                    state.copy(
                        isSaving = false,
                        isSaveErrorVisible = true
                    )
                }
            }
        }
    }
}
