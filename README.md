# ThinkTap

ThinkTap is an Android trivia game built with Kotlin and Jetpack Compose. It retrieves trivia
content from OpenTDB and supports English, German, Spanish, and Simplified Chinese, including
on-device translation of trivia content.

## Requirements

- JDK 17
- Android SDK 37 and Android Build Tools 36.0.0
- Android Studio or the checked-in Gradle wrapper
- An Android device or emulator to install and run the app

The app supports Android API 24 and later. Instrumentation tests require a running emulator or
connected device.

## Build and run

From the repository root:

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Install on a connected device or emulator
./gradlew :app:installDebug
```

## Project modules

| Module | Responsibility |
|---|---|
| `app` | Android application entry point, language settings, and daily quiz reminders |
| `feature-trivia-ui` | Trivia game and setup user interface |
| `trivia-domain` | Trivia and game business logic |
| `data-trivia` | Trivia data and repository implementations |
| `core-network` | Network access used by data modules |
| `core-ui` | Shared Compose UI components and theme |
| `quality-detekt-rules` | Custom Detekt rules for project code |

## Tests and quality checks

```bash
# JVM and Robolectric unit tests
./gradlew test

# Static analysis and Android lint
./gradlew check

# Verify Compose screenshot baselines
./gradlew verifyRoborazziDebug

# Run instrumentation tests (requires a connected device or emulator)
./gradlew connectedDebugAndroidTest
```

See the guides below for CI coverage requirements, screenshot testing, and localization details.

## Documentation

- [Localization](docs/localization.md) — supported languages, runtime content translation, and
  instructions for adding a language.
- [Pull-request CI](docs/ci.md) — workflow triggers, required checks, coverage gate, and failure
  reports.
- [Screenshot testing](docs/screenshot-testing.md) — recording and verifying Compose screenshot
  baselines across macOS and Linux.
- [TriviaGame Copilot agent](.github/agents/TriviaGame.agent.md) — repository agent configuration
  template.
