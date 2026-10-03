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
# Build and publish the independent SDK to sdk/build/repository
./sdk/gradlew -p sdk publish

# Build the debug APK
./gradlew :app:assembleDebug

# Install on a connected device or emulator
./gradlew :app:installDebug
```

For local SDK source development, use `./gradlew -PuseLocalTriviaSdk=true :app:assembleDebug`.
The normal build uses versioned Maven artifacts, not project dependencies. See
[SDK integration](sdk/README.md) for publication, API, and lifecycle details.

## Independent projects

The repository root is the application build. [sdk/](sdk/) is a separate Gradle build
with its own wrapper, settings, dependency catalog, tests, and publication configuration.
The SDK has no dependency on the application or its UI.

| Module | Responsibility |
|---|---|
| `app` | Android application entry point, language settings, and daily quiz reminders |
| `feature-trivia-ui` | Trivia game and setup user interface |
| `core-ui` | Shared Compose UI components and theme |
| `quality-detekt-rules` | Custom Detekt rules for project code |
| `sdk/trivia-sdk-core` | Pure Kotlin/JVM game rules, public API, and session orchestration |
| `sdk/trivia-sdk-android` | SDK initialization, OpenTDB, Room storage, and ML Kit content translation |
| `sdk/trivia-sdk-network` | SDK-owned internal network infrastructure |
| `sdk/quality-detekt-rules` | SDK-owned static analysis rules |

## Tests and quality checks

```bash
# Independently test/analyze the SDK and enforce 70% core line coverage
./sdk/gradlew -p sdk check

# Verify a standalone JVM consumer using published artifacts
./sdk/gradlew -p sdk/samples/jvm-consumer run

# JVM and Robolectric unit tests
./gradlew test

# Static analysis and Android lint
./gradlew check

# Verify Compose screenshot baselines
./gradlew verifyRoborazziDebug

# Run instrumentation tests (requires a connected device or emulator)
./sdk/gradlew -p sdk connectedDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

See the guides below for CI coverage requirements, screenshot testing, and localization details.

## Documentation

- [Localization](docs/localization.md) — supported languages, runtime content translation, and
  instructions for adding a language.
- [Trivia SDK](sdk/README.md) — independent builds, public API, publication, and integration.
- [Pull-request CI](docs/ci.md) — workflow triggers, required checks, coverage gate, and failure
  reports.
- [Screenshot testing](docs/screenshot-testing.md) — recording and verifying Compose screenshot
  baselines across macOS and Linux.
- [TriviaGame Copilot agent](.github/agents/TriviaGame.agent.md) — repository agent configuration
  template.
