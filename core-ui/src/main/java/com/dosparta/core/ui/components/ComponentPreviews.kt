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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.theme.TriviaGame2Theme

/** Renders previewed components inside the real app theme so previews match production. */
@Composable
private fun ComponentPreview(content: @Composable () -> Unit) {
    TriviaGame2Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ButtonsPreview() {
    ComponentPreview {
        TriviaPrimaryButton(text = "Start game", onClick = {}, modifier = Modifier.fillMaxWidth())
        TriviaSecondaryButton(text = "Start new game", onClick = {}, modifier = Modifier.fillMaxWidth())
        TriviaTextButton(text = "Not now", onClick = {})
    }
}

@PreviewLightDark
@Composable
private fun AnswerOptionStatesPreview() {
    ComponentPreview {
        AnswerOptionButton(text = "Water", state = AnswerVisualState.Unanswered, onClick = {})
        AnswerOptionButton(text = "Water", state = AnswerVisualState.Correct, onClick = {})
        AnswerOptionButton(text = "Salt", state = AnswerVisualState.Incorrect, onClick = {})
        AnswerOptionButton(text = "Sugar", state = AnswerVisualState.Dimmed, onClick = {})
    }
}

@PreviewLightDark
@Composable
private fun SelectorsPreview() {
    ComponentPreview {
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
}

@PreviewLightDark
@Composable
private fun SurfacesPreview() {
    ComponentPreview {
        TriviaCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "What is H2O?", modifier = Modifier.padding(20.dp))
        }
        TriviaMetaChip(text = "Category: Science")
    }
}

@Preview(widthDp = 320, heightDp = 120)
@Composable
private fun ProgressPreview() {
    ComponentPreview {
        TriviaProgressBar(progress = 0.35f, contentDescription = "Question 4 of 10")
        AnimatedCounter(
            value = 7,
            contentDescription = "7 correct out of 10",
            style = MaterialTheme.typography.displaySmall
        )
    }
}

@Preview(widthDp = 320, heightDp = 160)
@Composable
private fun ShimmerPreview() {
    ComponentPreview {
        ShimmerBlock(modifier = Modifier.fillMaxWidth().height(28.dp))
        ShimmerBlock(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        )
    }
}

@Preview(widthDp = 320, heightDp = 120)
@Composable
private fun TopBarPreview() {
    TriviaGame2Theme {
        BrandBackground {
            TriviaTopBar(title = "Trivia Game")
        }
    }
}
