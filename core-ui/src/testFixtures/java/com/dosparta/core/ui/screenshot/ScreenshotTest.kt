package com.dosparta.core.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.dosparta.core.ui.theme.TriviaGame2Theme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Base class for screenshot tests.
 *
 * Subclasses only describe *what* to render; this class owns every decision that makes the
 * resulting image reproducible:
 *
 * - **Native graphics at SDK 33.** Roborazzi needs Robolectric's native graphics pipeline, which
 *   requires SDK 26+. The rest of this module's Robolectric tests pin SDK 24, so screenshot tests
 *   deliberately override it here rather than in a shared `robolectric.properties`.
 * - **A fixed device.** Without pinned qualifiers the image size would follow Robolectric's
 *   default device and silently change across upgrades.
 * - **Reduced motion.** [TriviaGame2Theme] is always entered with `reducedMotion = true`, which
 *   collapses every `TriviaMotion` spec to `snap()`, skips the staggered entrance delays, and
 *   stops the shimmer and pulsing-dot loops. Those loops never idle, so capturing without this
 *   would hang `waitForIdle()` forever.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = PIXEL_5_QUALIFIERS)
abstract class ScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Renders [content] inside the app theme and writes the result to `<name>.png`.
     *
     * Compose allows content to be set only once per test, so each capture needs its own test
     * method. Light and dark variants are therefore separate tests rather than a loop.
     *
     * @param advanceTimeMillis when set, the test clock is driven manually and advanced by exactly
     *   this much before capturing. Needed for UI whose animations are not reduced-motion aware,
     *   such as the answer auto-advance countdown: letting the clock free-run would play its full
     *   five seconds and capture a finished countdown that has already confirmed the answer.
     */
    protected fun captureScreen(
        name: String,
        darkTheme: Boolean = false,
        advanceTimeMillis: Long? = null,
        content: @Composable () -> Unit
    ) {
        if (advanceTimeMillis != null) {
            composeTestRule.mainClock.autoAdvance = false
        }
        composeTestRule.setContent {
            TriviaGame2Theme(darkTheme = darkTheme, reducedMotion = true) {
                content()
            }
        }
        if (advanceTimeMillis != null) {
            composeTestRule.mainClock.advanceTimeBy(advanceTimeMillis)
        }
        composeTestRule.onRoot().captureRoboImage(filePath = goldenPath(name))
    }

    /**
     * Resolves a golden's path inside Roborazzi's configured output directory.
     *
     * An explicit `filePath` is interpreted relative to the test working directory rather than the
     * plugin's `outputDir`, so the directory has to be prepended by hand. The plugin publishes it
     * as a system property; the fallback matches Roborazzi's own default.
     */
    private fun goldenPath(name: String): String {
        val outputDir = System.getProperty("roborazzi.output.dir")
            ?: "build/outputs/roborazzi"
        return "$outputDir/$name.png"
    }
}
