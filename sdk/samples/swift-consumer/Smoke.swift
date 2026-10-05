import TriviaCore

// Compile-only API boundary check; production iOS adapters are intentionally deferred.
func checkTriviaCore(
    config: TriviaConfig,
    question: TriviaQuestion,
    sdk: TriviaSdk,
    clock: GameClock
) {
    _ = config.normalizedDifficulty()
    _ = question.correctAnswer
    _ = sdk.canReplay
    _ = sdk.state
    _ = sdk.persistenceFailure
    _ = sdk.translationUnavailable
    _ = clock.wallTimeMillis()
    _ = clock.elapsedTimeMillis()

    sdk.loadCategories { categories, error in
        _ = categories
        _ = error
    }
    sdk.restore { restored, error in
        _ = restored
        _ = error
    }
    sdk.start(config: config) { error in
        _ = error
    }

    let result = GameResult(totalQuestions: 2, correctAnswers: 1, durationMillis: 2_000)
    let _: String = result.formattedDuration()
    let _: Double = result.scorePercentage
}
