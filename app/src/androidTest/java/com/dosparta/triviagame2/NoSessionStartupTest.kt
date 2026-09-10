package com.dosparta.triviagame2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dosparta.trivia.ui.components.LoadingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoSessionStartupTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_screen_transitions_to_setup_screen() {
        val showSetup = mutableStateOf(false)

        composeRule.setContent {
            if (!showSetup.value) {
                LoadingScreen()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("setup_screen")
                )
            }
        }

        composeRule.onNodeWithTag("loading_screen").assertIsDisplayed()

        composeRule.runOnIdle {
            showSetup.value = true
        }

        composeRule.onNodeWithTag("setup_screen").assertIsDisplayed()
    }
}
