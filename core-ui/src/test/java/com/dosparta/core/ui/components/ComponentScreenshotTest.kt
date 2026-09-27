package com.dosparta.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.screenshot.ScreenshotTest
import org.junit.Test

/**
 * Golden images for the shared component kit.
 *
 * These mirror the groupings in `ComponentPreviews.kt` so that a visual change to any building
 * block is caught here, at the component level, rather than showing up diffused across every
 * screen golden in `:feature-trivia-ui`.
 */
class ComponentScreenshotTest : ScreenshotTest() {

    @Test
    fun buttonsLight() = captureComponents("components_buttons_light", darkTheme = false) {
        Buttons()
    }

    @Test
    fun buttonsDark() = captureComponents("components_buttons_dark", darkTheme = true) {
        Buttons()
    }

    @Test
    fun answerOptionStatesLight() =
        captureComponents("components_answer_states_light", darkTheme = false) {
            AnswerStates()
        }

    @Test
    fun answerOptionStatesDark() =
        captureComponents("components_answer_states_dark", darkTheme = true) {
            AnswerStates()
        }

    @Test
    fun selectorsLight() = captureComponents("components_selectors_light", darkTheme = false) {
        Selectors()
    }

    @Test
    fun selectorsDark() = captureComponents("components_selectors_dark", darkTheme = true) {
        Selectors()
    }

    @Test
    fun surfacesLight() = captureComponents("components_surfaces_light", darkTheme = false) {
        Surfaces()
    }

    @Test
    fun surfacesDark() = captureComponents("components_surfaces_dark", darkTheme = true) {
        Surfaces()
    }

    @Test
    fun progressAndCounter() = captureComponents("components_progress") {
        TriviaProgressBar(progress = PROGRESS, contentDescription = "Question 4 of 10")
        AnimatedCounter(
            value = SCORE,
            contentDescription = "7 correct out of 10",
            style = MaterialTheme.typography.displaySmall
        )
    }

    /**
     * The shimmer sweep runs forever, so this golden also asserts that reduced motion parks it at
     * a fixed frame instead of hanging the capture.
     */
    @Test
    fun shimmer() = captureComponents("components_shimmer") {
        ShimmerBlock(modifier = Modifier.fillMaxWidth().height(SMALL_BLOCK.dp))
        ShimmerBlock(
            modifier = Modifier.fillMaxWidth().height(LARGE_BLOCK.dp),
            shape = MaterialTheme.shapes.large
        )
    }

    @Test
    fun topBar() = captureScreen("components_top_bar") {
        BrandBackground {
            TriviaTopBar(title = "Trivia Game")
        }
    }

    @Composable
    private fun Buttons() {
        TriviaPrimaryButton(text = "Start game", onClick = {}, modifier = Modifier.fillMaxWidth())
        TriviaSecondaryButton(
            text = "Start new game",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
        TriviaTextButton(text = "Not now", onClick = {})
    }

    @Composable
    private fun AnswerStates() {
        AnswerOptionButton(text = "Water", state = AnswerVisualState.Unanswered, onClick = {})
        AnswerOptionButton(text = "Water", state = AnswerVisualState.Correct, onClick = {})
        AnswerOptionButton(text = "Salt", state = AnswerVisualState.Incorrect, onClick = {})
        AnswerOptionButton(text = "Sugar", state = AnswerVisualState.Dimmed, onClick = {})
    }

    @Composable
    private fun Selectors() {
        TriviaDropdownField(
            label = "Category",
            value = "Science",
            options = listOf("Any category", "Science", "History"),
            onOptionSelected = {}
        )
        TriviaSegmentedSelector(
            options = listOf("Mixed", "Easy", "Hard"),
            selectedIndex = 1,
            onOptionSelected = {}
        )
        TriviaSwitchRow(
            label = "Daily reminder",
            supportingText = "A nudge each day",
            checked = true,
            onCheckedChange = {}
        )
    }

    @Composable
    private fun Surfaces() {
        TriviaCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "What is H2O?", modifier = Modifier.padding(PADDING.dp))
        }
        TriviaMetaChip(text = "Category: Science")
    }

    /** Lays components out on the themed background, matching how `ComponentPreviews` frames them. */
    private fun captureComponents(
        name: String,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit
    ) = captureScreen(name, darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(SPACING.dp)
            ) {
                content()
            }
        }
    }

    private companion object {
        const val PADDING = 16
        const val SPACING = 12
        const val SMALL_BLOCK = 28
        const val LARGE_BLOCK = 56
        const val PROGRESS = 0.35f
        const val SCORE = 7
    }
}
