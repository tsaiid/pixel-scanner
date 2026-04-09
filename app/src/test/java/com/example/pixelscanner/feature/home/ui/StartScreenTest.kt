package com.example.pixelscanner.feature.home.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.pixelscanner.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StartScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows primary start action and invokes callback when tapped`() {
        var startClicks = 0

        composeRule.setContent {
            StartScreen(
                recoverableSession = null,
                onStartScanClicked = { startClicks += 1 },
            )
        }

        composeRule.waitForIdle()

        val appContext = RuntimeEnvironment.getApplication()
        composeRule.onNodeWithText(appContext.getString(R.string.start_primary_action))
            .assertIsDisplayed()
            .performClick()

        composeRule.onAllNodesWithText(appContext.getString(R.string.start_recover_session_title))
            .assertCountEquals(0)
        assertEquals(1, startClicks)
    }
}
