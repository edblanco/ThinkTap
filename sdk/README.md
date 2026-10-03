# ThinkTap Trivia SDK

An independently buildable Android trivia SDK with a pure Kotlin/JVM core. It owns
game rules, scoring, replay, session restoration, active-play timing, persistence,
OpenTDB requests and repeat avoidance, and content translation. It has no Compose,
navigation, ViewModel, or application dependency.

## Artifacts and builds

Requires JDK 17 or a compatible newer JDK and the checked-in wrapper. Android
artifacts use Android SDK 37, Build Tools 36.0.0, and support API 24+.
Use JDK 17 for quality checks (JDK 19 is also validated). The current Detekt
version cannot run on JDK 25, including Android Studio versions that bundle it;
select a compatible Gradle JDK when running `check`.

From this directory:

```bash
./gradlew check
./gradlew publish
./gradlew -p samples/jvm-consumer run
```

From the repository root, use `./sdk/gradlew -p sdk` instead.

Publication creates Maven artifacts and transitive dependency metadata in
`build/repository`:

| Coordinate | Contents |
|---|---|
| `com.dosparta.trivia:trivia-sdk-core:0.1.0` | Public facade, models, adapter contracts, Kotlin/JVM game rules |
| `com.dosparta.trivia:trivia-sdk-android:0.1.0` | Android initialization and data adapters; exports core API |
| `com.dosparta.trivia:trivia-sdk-network:0.1.0` | Internal Android network support, resolved transitively |

Use `-PsdkRepository=/absolute/path/to/repository` for a different file-based
repository. Set the same property on the app/consumer build. No remote publication
is configured. Publish all artifacts together; copying an AAR alone omits its
dependencies.

The SDK version is owned by [build.gradle.kts](build.gradle.kts). When releasing a
new version, update the application's version catalog and the isolated sample
dependency too. Version `0.1.0` is the initial API; incompatible changes before
`1.0` should increment the minor version and document migration.

## Android integration

Add your local repository in consumer settings:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("/absolute/path/to/sdk/build/repository")
            content { includeGroup("com.dosparta.trivia") }
        }
        google()
        mavenCentral()
    }
}
```

Then depend on the SDK:

```kotlin
implementation("com.dosparta.trivia:trivia-sdk-android:0.1.0")
```

Initialize a controller per game owner, typically a ViewModel:

```kotlin
val sdk = AndroidTriviaSdk.create(context)
```

Import `com.dosparta.trivia.sdk.android.AndroidTriviaSdk`. Only the application
context is retained. Hilt is not required; the ThinkTap app uses its own Hilt
provider to create a ViewModel-scoped controller.

Networking, Room, token storage, and ML Kit clients are application/process-owned
and cached. Localization is shared between Android controllers. There is one
persisted active-game slot per application, so use one active controller at a time.
The SDK owns no coroutine scope or background jobs and requires no controller
disposal call. Cancel the caller's scope when the game owner is disposed.

## Commands and observable state

Use `com.dosparta.trivia.sdk.TriviaSdk` as the gameplay boundary:

| API | Behavior |
|---|---|
| `loadCategories()` | Retrieves localized setup categories |
| `setContentLanguage(language)` | Updates future content requests; returns whether language changed |
| `restore()` | Restores saved progress; returns false when no saved game exists |
| `start(config)` | Fetches questions, starts a game, and saves the initial session |
| `submit(answer)` | Checks the answer and advances or finishes |
| `finishEarly()` | Finishes with unanswered questions counted as incorrect |
| `pause()` / `resume()` | Excludes background time and persists paused progress |
| `reset()` | Clears active progress and returns to idle |
| `canReplay` / `replay()` | Reuses the last game's questions without a network fetch |

All commands except language selection and `canReplay` are suspending. Launch them
in the caller's lifecycle-owned coroutine scope. Game mutations and persistence
are serialized; category requests are independently serialized. Cancellation of
a pending fetch restores the prior state and propagates cancellation. Once a
game transition commits, its storage write completes before the command lock is
released, even if the caller is canceled, preventing stale saves after clearing.

Collect:

- `state`: `Idle`, `Loading`, `Playing(session)`, `Finished(result)`, or
  `Failed(failure)`. Start failures are represented here.
- `persistenceFailure`: nullable `TriviaSdkException`; nonfatal save/clear failures
  do not discard gameplay. Show a warning that progress may not restore correctly.
  A successful write clears the warning.
- `translationUnavailable`: English fallback notification.

`loadCategories()` and `restore()` throw typed `TriviaSdkException` on operational
failure. Read `error: TriviaError` and preserve/log the cause; do not match exception
messages. Cancellation is never converted to an operational error. Categories,
gameplay loading failures, and startup failures should offer retry. Storage errors
must not be silently treated as an absent game.

The app owns localized UI strings, navigation, loading presentation, category
reload presentation after language selection, per-app locale preferences, and
daily reminders. It must not calculate scores, mutate sessions to advance games,
or implement restoration/timing rules.

## Lifecycle and compatibility

Forward foreground/background events to `resume()`/`pause()` using the owner's
scope. Timing uses a monotonic clock; wall time is retained only for public
session timestamps and compatibility with legacy saved timestamps. Saved elapsed
time is restored without counting time while the application was stopped.

The Android adapter retains the database name `trivia_database`, existing Room
schema, and question serialization. The original domain model packages are
retained intentionally to minimize integration and serialization risk. Game
questions keep the language they were started in; changing language affects new
content, not active or restored questions.

## Custom JVM integrations

Depend only on `trivia-sdk-core` and construct:

```kotlin
val sdk = TriviaSdk(repository, sessions, localization, clock)
```

Implement the public `ITriviaRepository`, `IGameSessionRepository`, and
`IContentLocalizationRepository` contracts. `GameClock` is optional and enables
deterministic tests. Repository adapters should throw `TriviaSdkException` for
known operational failures and propagate cancellation.

The [standalone consumer](samples/jvm-consumer/) is an independent build with no
composite substitution or SDK source references. It compiles and runs a game using
only published artifacts and fake adapters, without Android or Hilt.

## Verification and local development

Core `check` enforces at least 70% line coverage, including orchestration. Reports:
`trivia-sdk-core/build/reports/jacoco/test/`. Adapter tests cover networking,
translation, token recovery, storage mapping, and Android factory initialization.

The application normally resolves published release artifacts. For source editing,
its optional `-PuseLocalTriviaSdk=true` composite build substitutes SDK modules.
SDK checks must still pass independently, and CI verifies both the isolated JVM
consumer and application consumption of published artifacts.

`TriviaSdk` intentionally exposes a cohesive command facade, so its class has a
narrow `TooManyFunctions` suppression rather than splitting gameplay ownership
across presentation-facing services.
