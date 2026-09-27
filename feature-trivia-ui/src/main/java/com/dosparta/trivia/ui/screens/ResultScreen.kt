package com.dosparta.trivia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.components.AnimatedCounter
import com.dosparta.core.ui.components.EnterAnimated
import com.dosparta.core.ui.components.TriviaCard
import com.dosparta.core.ui.components.TriviaPrimaryButton
import com.dosparta.core.ui.components.TriviaProgressBar
import com.dosparta.core.ui.components.TriviaScreenScaffold
import com.dosparta.core.ui.components.TriviaSecondaryButton
import com.dosparta.core.ui.theme.quizColors
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.preview.PreviewFixtures
import com.dosparta.trivia.ui.preview.TriviaPreviewTheme

private const val GREAT_THRESHOLD = 0.8f
private const val GOOD_THRESHOLD = 0.5f

/**
 * A screen displaying the results of a completed trivia session.
 * @param result the [GameResult] to show
 * @param onPlayAgain callback when the user wants to replay the same questions
 * @param onStartNewGame callback when the user wants to start a new game from setup
 */
@Composable
fun ResultScreen(
    result: GameResult,
    onPlayAgain: () -> Unit,
    onStartNewGame: () -> Unit
) {
    TriviaScreenScaffold(title = stringResource(R.string.app_title)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 0) {
                Text(
                    text = stringResource(R.string.result_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(verdictFor(result)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 1) {
                ScoreCard(result = result)
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 2) {
                StatsCard(result = result)
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 3) {
                TriviaPrimaryButton(
                    text = stringResource(R.string.play_again),
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 4) {
                TriviaSecondaryButton(
                    text = stringResource(R.string.start_new_game),
                    onClick = onStartNewGame,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(result: GameResult) {
    TriviaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedCounter(
                    value = result.correctAnswers,
                    contentDescription = stringResource(
                        R.string.score_counter_description,
                        result.correctAnswers,
                        result.totalQuestions
                    ),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.quizColors.correct
                )
                Text(
                    text = stringResource(R.string.score_of_total, result.totalQuestions),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                )
            }

            TriviaProgressBar(
                progress = fractionCorrect(result),
                contentDescription = stringResource(
                    R.string.score_counter_description,
                    result.correctAnswers,
                    result.totalQuestions
                )
            )

            Text(
                text = stringResource(R.string.score_label, result.correctAnswers, result.totalQuestions),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatsCard(result: GameResult) {
    TriviaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.incorrect_label, result.incorrectAnswers),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.percentage_label, "%.1f".format(result.scorePercentage)),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.time_label, result.formattedDuration()),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun fractionCorrect(result: GameResult): Float =
    if (result.totalQuestions == 0) 0f else result.correctAnswers.toFloat() / result.totalQuestions

private fun verdictFor(result: GameResult): Int {
    val fraction = fractionCorrect(result)
    return when {
        result.totalQuestions > 0 && result.correctAnswers == result.totalQuestions -> R.string.result_perfect
        fraction >= GREAT_THRESHOLD -> R.string.result_great
        fraction >= GOOD_THRESHOLD -> R.string.result_good
        else -> R.string.result_keep_trying
    }
}

@PreviewLightDark
@Composable
private fun PartialResultPreview() {
    TriviaPreviewTheme {
        ResultScreen(result = PreviewFixtures.result, onPlayAgain = {}, onStartNewGame = {})
    }
}

@PreviewLightDark
@Composable
private fun PerfectResultPreview() {
    TriviaPreviewTheme {
        ResultScreen(
            result = PreviewFixtures.result.copy(correctAnswers = PreviewFixtures.result.totalQuestions),
            onPlayAgain = {},
            onStartNewGame = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun ZeroScoreResultPreview() {
    TriviaPreviewTheme {
        ResultScreen(
            result = PreviewFixtures.result.copy(correctAnswers = 0),
            onPlayAgain = {},
            onStartNewGame = {}
        )
    }
}
