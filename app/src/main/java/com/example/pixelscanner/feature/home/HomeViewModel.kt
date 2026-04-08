package com.example.pixelscanner.feature.home

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.pixelscanner.domain.RecoverableSessionSnapshot
import com.example.pixelscanner.feature.home.ui.HomeUiState
import com.example.pixelscanner.feature.home.ui.RecoverableSessionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            shouldAutoStartScanner = savedStateHandle.get<Boolean>(KEY_SHOULD_AUTO_START_SCANNER) ?: false,
            shouldAttemptRecoverableSessionRestore = savedStateHandle.get<Boolean>(
                KEY_SHOULD_ATTEMPT_RECOVERABLE_SESSION_RESTORE,
            ) ?: true,
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun setShouldAutoStartScanner(shouldAutoStartScanner: Boolean) {
        savedStateHandle[KEY_SHOULD_AUTO_START_SCANNER] = shouldAutoStartScanner
        _uiState.value = _uiState.value.copy(shouldAutoStartScanner = shouldAutoStartScanner)
    }

    fun setShouldAttemptRecoverableSessionRestore(shouldAttemptRecoverableSessionRestore: Boolean) {
        savedStateHandle[KEY_SHOULD_ATTEMPT_RECOVERABLE_SESSION_RESTORE] =
            shouldAttemptRecoverableSessionRestore
        _uiState.value = _uiState.value.copy(
            shouldAttemptRecoverableSessionRestore = shouldAttemptRecoverableSessionRestore,
        )
    }

    fun setRecoverableSession(recoverableSession: RecoverableSessionSnapshot?) {
        _uiState.value = _uiState.value.copy(
            recoverableSession = recoverableSession?.toUiState(),
        )
    }

    fun clearRecoverableSession() {
        _uiState.value = _uiState.value.copy(recoverableSession = null)
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

internal const val KEY_SHOULD_AUTO_START_SCANNER = "home_should_auto_start_scanner"
internal const val KEY_SHOULD_ATTEMPT_RECOVERABLE_SESSION_RESTORE =
    "home_should_attempt_recoverable_session_restore"

private fun RecoverableSessionSnapshot.toUiState(): RecoverableSessionUiState {
    return RecoverableSessionUiState(
        sessionId = metadata.sessionId,
        pageCount = metadata.pageCount,
        hasMissingAssets = isPartial,
        missingAssetCount = missingPageFileNames.size + if (missingOriginalPdf) 1 else 0,
    )
}
