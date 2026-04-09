package com.example.pixelscanner.app

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.pixelscanner.core.model.ExportPostActionTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppFlowViewModelTest {

    @Test
    fun `restores pending export action across configuration change style recreation`() {
        val savedStateHandle = SavedStateHandle()
        val target = ExportPostActionTarget(
            outputUris = listOf(
                Uri.parse("content://pixelscanner/export/page-1.jpg"),
                Uri.parse("content://pixelscanner/export/page-2.jpg"),
            ),
            mimeType = "image/jpeg",
        )

        createViewModel(savedStateHandle = savedStateHandle).setPendingExportAction(target)

        val recreatedViewModel = createViewModel(savedStateHandle = savedStateHandle)

        assertEquals(target, recreatedViewModel.uiState.value.pendingExportAction?.toTarget())
    }

    @Test
    fun `restores pending export action from snapshot after process death style recreation`() {
        val savedStateHandle = SavedStateHandle()
        val target = ExportPostActionTarget(
            outputUris = listOf(Uri.parse("content://pixelscanner/export/report.pdf")),
            mimeType = "application/pdf",
        )

        val initialViewModel = createViewModel(savedStateHandle = savedStateHandle)
        initialViewModel.setPendingExportAction(target)

        val snapshot = savedStateHandle.keys().associateWith { key ->
            savedStateHandle.get<Any?>(key)
        }
        val restoredViewModel = createViewModel(savedStateHandle = SavedStateHandle(snapshot))

        assertEquals(target, restoredViewModel.uiState.value.pendingExportAction?.toTarget())
    }

    @Test
    fun `clears pending export action from saveable state`() {
        val savedStateHandle = SavedStateHandle()
        val target = ExportPostActionTarget(
            outputUris = listOf(Uri.parse("content://pixelscanner/export/report.pdf")),
            mimeType = "application/pdf",
        )

        val viewModel = createViewModel(savedStateHandle = savedStateHandle)
        viewModel.setPendingExportAction(target)
        viewModel.clearPendingExportAction()

        val recreatedViewModel = createViewModel(savedStateHandle = savedStateHandle)

        assertNull(recreatedViewModel.uiState.value.pendingExportAction)
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): AppFlowViewModel {
        return AppFlowViewModel(savedStateHandle = savedStateHandle)
    }
}
