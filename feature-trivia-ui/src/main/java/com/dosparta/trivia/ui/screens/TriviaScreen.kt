package com.dosparta.trivia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import com.dosparta.trivia.ui.components.LoadingScreen
import com.dosparta.trivia.ui.viewmodel.TriviaUiState
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel

/**
 * The main trivia screen, showing questions, handling user answers,
 * and delegating to [onResult] when the game finishes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriviaScreen(
    viewModel: TriviaViewModel = hiltViewModel(),
    onResult: (GameResult) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    DisposableEffect(viewModel) {
        onDispose { viewModel.cancelPendingLoad() }
    }

    LaunchedEffect(Unit) {
        if (categories.isEmpty()) {
            viewModel.loadCategories()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is TriviaUiState.Result) {
            onResult((uiState as TriviaUiState.Result).result)
        }
    }

    when (val state = uiState) {
        is TriviaUiState.Loading -> LoadingScreen()
        is TriviaUiState.Error -> ErrorScreen(
            message = state.message,
            onRetry = { viewModel.loadQuestions() }
        )
        is TriviaUiState.Result -> {
            // Navigation is triggered in the side effect above; the UI doesn't need to render it.
        }
        is TriviaUiState.Game -> {
            val current = state.session
            val question = current.currentQuestion

            if (question == null) {
                ErrorScreen(
                    message = UiText.StringResource(R.string.error_no_valid_question),
                    onRetry = { viewModel.loadQuestions() }
                )
                return
            }

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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.question_counter, current.currentIndex + 1, current.questions.size),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Category: ${question.category}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Difficulty: ${question.difficulty.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = question.question,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEach { option ->
                            Button(
                                onClick = { viewModel.submit(option) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = option)
                            }
                        }
                    }
                }
            }
        }
        // todo why is this needed? Could TriviaNavHost handle this?
        TriviaUiState.Idle -> SetupScreen(
            categories = categories,
            categoriesError = null,
            onStartGame = { config ->
                viewModel.loadQuestions(config)
            },
            onRetryLoadCategories = { viewModel.loadCategories() }
        )
    }
}
