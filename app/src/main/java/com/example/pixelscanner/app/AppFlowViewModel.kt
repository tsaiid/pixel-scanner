package com.example.pixelscanner.app

import android.net.Uri
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.pixelscanner.core.model.ExportPostActionTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppFlowViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppFlowUiState(
            pendingExportAction = readPendingExportAction(),
        )
    )
    val uiState: StateFlow<AppFlowUiState> = _uiState.asStateFlow()

    fun setPendingExportAction(target: ExportPostActionTarget) {
        val pendingExportAction = PendingExportActionState.fromTarget(target)
        savedStateHandle[KEY_PENDING_EXPORT_ACTION_OUTPUT_URIS] =
            ArrayList(pendingExportAction.outputUriStrings)
        savedStateHandle[KEY_PENDING_EXPORT_ACTION_MIME_TYPE] = pendingExportAction.mimeType
        _uiState.value = _uiState.value.copy(pendingExportAction = pendingExportAction)
    }

    fun clearPendingExportAction() {
        savedStateHandle.remove<ArrayList<String>>(KEY_PENDING_EXPORT_ACTION_OUTPUT_URIS)
        savedStateHandle.remove<String>(KEY_PENDING_EXPORT_ACTION_MIME_TYPE)
        _uiState.value = _uiState.value.copy(pendingExportAction = null)
    }

    private fun readPendingExportAction(): PendingExportActionState? {
        val outputUriStrings = savedStateHandle.get<ArrayList<String>>(
            KEY_PENDING_EXPORT_ACTION_OUTPUT_URIS,
        ) ?: return null
        val mimeType = savedStateHandle.get<String>(KEY_PENDING_EXPORT_ACTION_MIME_TYPE)
            ?: return null
        if (outputUriStrings.isEmpty()) {
            return null
        }
        return PendingExportActionState(
            outputUriStrings = outputUriStrings.toList(),
            mimeType = mimeType,
        )
    }
}

class AppFlowViewModelFactory(
    owner: SavedStateRegistryOwner,
) : AbstractSavedStateViewModelFactory(owner, null) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        require(modelClass.isAssignableFrom(AppFlowViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return AppFlowViewModel(savedStateHandle = handle) as T
    }
}

internal const val KEY_PENDING_EXPORT_ACTION_OUTPUT_URIS =
    "app_pending_export_action_output_uris"
internal const val KEY_PENDING_EXPORT_ACTION_MIME_TYPE =
    "app_pending_export_action_mime_type"

data class AppFlowUiState(
    val pendingExportAction: PendingExportActionState? = null,
)

data class PendingExportActionState(
    val outputUriStrings: List<String>,
    val mimeType: String,
) {
    fun toTarget(): ExportPostActionTarget {
        return ExportPostActionTarget(
            outputUris = outputUriStrings.map(Uri::parse),
            mimeType = mimeType,
        )
    }

    companion object {
        fun fromTarget(target: ExportPostActionTarget): PendingExportActionState {
            return PendingExportActionState(
                outputUriStrings = target.outputUris.map(Uri::toString),
                mimeType = target.mimeType,
            )
        }
    }
}
