package com.example.pixelscanner.feature.home

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun `defaults to not auto starting scanner`() {
        val viewModel = HomeViewModel(SavedStateHandle())

        assertFalse(viewModel.uiState.value.shouldAutoStartScanner)
    }

    @Test
    fun `restores auto start scanner state from saved state handle`() {
        val viewModel = HomeViewModel(
            SavedStateHandle(mapOf(KEY_SHOULD_AUTO_START_SCANNER to true))
        )

        assertTrue(viewModel.uiState.value.shouldAutoStartScanner)
    }

    @Test
    fun `updates auto start scanner state`() {
        val viewModel = HomeViewModel(SavedStateHandle())

        viewModel.setShouldAutoStartScanner(true)

        assertTrue(viewModel.uiState.value.shouldAutoStartScanner)
    }
}
