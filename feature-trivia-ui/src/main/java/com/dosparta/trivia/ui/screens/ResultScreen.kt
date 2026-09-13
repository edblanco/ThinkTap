package com.dosparta.trivia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.ui.R

/**
 * A screen displaying the results of a completed trivia session.
 * @param result the [GameResult] to show
 * @param onPlayAgain callback when the user wants to replay the same questions
 * @param onStartNewGame callback when the user wants to start a new game from setup
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: GameResult,
    onPlayAgain: () -> Unit,
    onStartNewGame: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trivia Game") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.result_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.score_label, result.correctAnswers, result.totalQuestions),
                        style = MaterialTheme.typography.titleLarge
                    )
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

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = stringResource(R.string.play_again))
            }

            Button(
                onClick = onStartNewGame,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = stringResource(R.string.start_new_game))
            }
        }
    }
}
