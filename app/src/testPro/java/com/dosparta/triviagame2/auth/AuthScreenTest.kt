package com.dosparta.triviagame2.auth

import android.app.Application
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.theme.TriviaGame2Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.TIRAMISU],
    application = Application::class,
    qualifiers = "w600dp-h480dp"
)
class AuthScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `sign in and errors remain reachable in a short landscape viewport`() {
        assertLoginReachable(width = 600.dp, height = 240.dp, fontScale = 1f)
    }

    @Test
    fun `sign in and errors remain reachable with enlarged text`() {
        assertLoginReachable(width = 320.dp, height = 480.dp, fontScale = 2f)
    }

    private fun assertLoginReachable(width: Dp, height: Dp, fontScale: Float) {
        var signInCalls = 0
        val error = "Connection failed. Please try again."
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TriviaGame2Theme {
                    Box(Modifier.requiredSize(width, height)) {
                        AuthScreen(
                            loading = false,
                            busy = false,
                            configured = true,
                            error = error,
                            onSignIn = { signInCalls++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("google_sign_in").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, signInCalls) }
        compose.onNodeWithText(error).performScrollTo().assertIsDisplayed()
    }
}
