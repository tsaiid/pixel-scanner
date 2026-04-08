package com.example.pixelscanner.feature.home

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.pixelscanner.feature.home.ui.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            shouldAutoStartScanner = savedStateHandle.get<Boolean>(KEY_SHOULD_AUTO_START_SCANNER) ?: false,
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun setShouldAutoStartScanner(shouldAutoStartScanner: Boolean) {
        savedStateHandle[KEY_SHOULD_AUTO_START_SCANNER] = shouldAutoStartScanner
        _uiState.value = _uiState.value.copy(shouldAutoStartScanner = shouldAutoStartScanner)
    }
}

class HomeViewModelFactory(
    owner: SavedStateRegistryOwner,
) : AbstractSavedStateViewModelFactory(owner, null) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return HomeViewModel(savedStateHandle = handle) as T
    }
}

private const val KEY_SHOULD_AUTO_START_SCANNER = "home_should_auto_start_scanner"
