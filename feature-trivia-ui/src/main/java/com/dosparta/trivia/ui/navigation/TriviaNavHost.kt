package com.dosparta.trivia.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.core.ui.theme.LocalReducedMotion
import com.dosparta.core.ui.theme.TriviaMotion
import com.dosparta.trivia.ui.screens.ResultScreen
import com.dosparta.trivia.ui.screens.SetupScreen
import com.dosparta.trivia.ui.screens.StartupScreen
import com.dosparta.trivia.ui.screens.TriviaScreen
import com.dosparta.trivia.ui.viewmodel.StartupUiState
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel

private const val ROUTE_LOADING = "loading"
private const val ROUTE_SETUP = "setup"
private const val ROUTE_TRIVIA = "trivia"
private const val ROUTE_RESULT = "result/{totalQuestions}/{correctAnswers}/{durationMillis}"

/**
 * Hosts the navigation graph for the trivia feature using Compose Navigation and Hilt.
 */
@Composable
fun TriviaNavHost(
    reminderEnabled: Boolean = false,
    reminderHour: Int = 19,
    reminderMinute: Int = 0,
    onReminderEnabledChange: (Boolean) -> Unit = {},
    onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> }
) {
    val navController = rememberNavController()
    val viewModel: TriviaViewModel = hiltViewModel()
    val categories = viewModel.categories.collectAsState().value
    val categoriesError = viewModel.categoriesError.collectAsState().value
    val startupState = viewModel.startupState.collectAsState().value

    val reducedMotion = LocalReducedMotion.current
    val durationMillis = if (reducedMotion) 0 else TriviaMotion.SCREEN_TRANSITION_MILLIS

    NavHost(
        navController = navController,
        startDestination = ROUTE_LOADING,
        enterTransition = { forwardEnter(durationMillis) },
        exitTransition = { forwardExit(durationMillis) },
        popEnterTransition = { backEnter(durationMillis) },
        popExitTransition = { backExit(durationMillis) }
    ) {
        composable(ROUTE_LOADING) {
            LaunchedEffect(Unit) {
                viewModel.bootstrapApp()
            }

            LaunchedEffect(startupState) {
                when (startupState) {
                    is StartupUiState.NavigateToSetup -> {
                        navController.navigate(ROUTE_SETUP) {
                            popUpTo(ROUTE_LOADING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    is StartupUiState.NavigateToTrivia -> {
                        navController.navigate(ROUTE_TRIVIA) {
                            popUpTo(ROUTE_LOADING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    else -> Unit
                }
            }

            StartupScreen(
                error = (startupState as? StartupUiState.Error)?.message,
                onRetry = { viewModel.retryBootstrap() }
            )
        }

        composable(ROUTE_SETUP) {
            SetupScreen(
                categories = categories,
                categoriesError = categoriesError,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                onStartGame = { config ->
                    viewModel.loadQuestions(config)
                    navController.navigate(ROUTE_TRIVIA) {
                        popUpTo(ROUTE_SETUP) { inclusive = true }
                    }
                },
                onRetryLoadCategories = {
                    viewModel.loadCategories()
                },
                onReminderEnabledChange = onReminderEnabledChange,
                onReminderTimeChange = onReminderTimeChange
            )
        }

        composable(ROUTE_TRIVIA) {
            TriviaScreen(
                viewModel = viewModel,
                onResult = { result ->
                    navController.navigate(
                        "result/${result.totalQuestions}/${result.correctAnswers}/${result.durationMillis}"
                    )
                }
            )
        }

        composable(ROUTE_RESULT) { backStackEntry ->
            val totalQuestions = backStackEntry.arguments?.getString("totalQuestions")?.toIntOrNull() ?: 0
            val correctAnswers = backStackEntry.arguments?.getString("correctAnswers")?.toIntOrNull() ?: 0
            val durationMillis = backStackEntry.arguments?.getString("durationMillis")?.toLongOrNull() ?: 0L
            val result = GameResult(
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                durationMillis = durationMillis
            )

            ResultScreen(
                result = result,
                onPlayAgain = {
                    if (viewModel.replayLastGame()) {
                        navController.navigate(ROUTE_TRIVIA) {
                            popUpTo(ROUTE_RESULT) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onStartNewGame = {
                    viewModel.restart()
                    navController.navigate(ROUTE_SETUP) {
                        popUpTo(ROUTE_RESULT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

private const val SLIDE_FRACTION = 6
private const val ENTER_SCALE = 0.94f
private const val EXIT_SCALE = 1.04f

/** Forward navigation: the incoming screen slides in from the right while fading up. */
private fun AnimatedContentTransitionScope<*>.forwardEnter(durationMillis: Int): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(durationMillis),
        initialOffset = { it / SLIDE_FRACTION }
    ) + fadeIn(tween(durationMillis)) + scaleIn(tween(durationMillis), initialScale = ENTER_SCALE)

private fun AnimatedContentTransitionScope<*>.forwardExit(durationMillis: Int): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(durationMillis),
        targetOffset = { it / SLIDE_FRACTION }
    ) + fadeOut(tween(durationMillis))

/** Back navigation mirrors the forward transition so the stack reads as a physical space. */
private fun AnimatedContentTransitionScope<*>.backEnter(durationMillis: Int): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(durationMillis),
        initialOffset = { it / SLIDE_FRACTION }
    ) + fadeIn(tween(durationMillis))

private fun AnimatedContentTransitionScope<*>.backExit(durationMillis: Int): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(durationMillis),
        targetOffset = { it / SLIDE_FRACTION }
    ) + fadeOut(tween(durationMillis)) + scaleOut(tween(durationMillis), targetScale = EXIT_SCALE)
