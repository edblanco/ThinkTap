# ThinkTap Trivia SDK

An independently buildable trivia SDK with a Kotlin Multiplatform core and Android
adapters. The core targets JVM 17, iOS ARM64 devices (`iosArm64`), and Apple Silicon
simulators (`iosSimulatorArm64`); Intel simulators are not supported. It owns
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
./gradlew checkJvm
./gradlew publishJvm
./gradlew -p samples/jvm-consumer run
```

From the repository root, use `./sdk/gradlew -p sdk` instead.

These commands require no Xcode and preserve the Android/JVM development path.
On a Mac configured for iOS development, `./gradlew check` also runs native tests,
and `./gradlew publish` publishes all targets. See [iOS core](#ios-core) below.

Publication creates Maven artifacts and transitive dependency metadata in
`build/repository`:

| Coordinate | Contents |
|---|---|
| `com.dosparta.trivia:trivia-sdk-core:0.2.0` | KMP entry point and metadata; Gradle selects the platform artifact |
| `com.dosparta.trivia:trivia-sdk-core-jvm:0.2.0` | JVM core selected transitively, also usable by Maven-only JVM consumers |
| `com.dosparta.trivia:trivia-sdk-core-iosarm64:0.2.0` | Kotlin/Native device library, published on macOS |
| `com.dosparta.trivia:trivia-sdk-core-iossimulatorarm64:0.2.0` | Kotlin/Native simulator library, published on macOS |
| `com.dosparta.trivia:trivia-sdk-android:0.2.0` | Android initialization and data adapters; exports core API |
| `com.dosparta.trivia:trivia-sdk-network:0.2.0` | Internal Android network support, resolved transitively |

Use `-PsdkRepository=/absolute/path/to/repository` for a different file-based
repository. Set the same property on the app/consumer build. No remote publication
is configured. `publishJvm` publishes common metadata, the JVM core, and Android
artifacts together; it does not publish the native libraries. A complete KMP
repository also needs both iOS publications from macOS under the same version.
Copying an AAR alone omits its dependencies. XCFramework distribution is separate
from Maven publication.

The SDK version is owned by [build.gradle.kts](build.gradle.kts). When releasing a
new version, update the application's version catalog and the isolated sample
dependency too. Version `0.1.0` is the initial API; incompatible changes before
`1.0` should increment the minor version and document migration.

### Migrating from 0.1.0

Update all SDK dependencies to `0.2.0` and republish with `publishJvm`. The core's
publication changed from a JVM JAR to a KMP entry point with platform variants,
so the version increments rather than replacing `0.1.0` in place. Gradle consumers
keep using `trivia-sdk-core`; Maven-only JVM consumers use `trivia-sdk-core-jvm`.
Public packages, Android factory usage, and saved Android sessions are retained.
Core injection annotations are removed from internal implementations; the app
continues to supply its own Hilt provider.

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
implementation("com.dosparta.trivia:trivia-sdk-android:0.2.0")
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

## iOS core

This release makes gameplay portable, **not a turnkey iOS SDK**. Android's
Retrofit/OkHttp networking, Room/Gson storage, SharedPreferences tokens, and ML Kit
translation remain Android-only. Production iOS data adapters, token storage,
translation, and ergonomic Swift async/await/state-observation wrappers are deferred.
There is no Compose Multiplatform UI, CocoaPods setup, or remote Swift package.

The shared `TriviaSdk`, models, and adapter contracts live in `commonMain`.
The existing command-locking, cancellation, replay, persistence notifications,
and active-play timing semantics are shared across platforms. Platform clocks
provide epoch wall time and monotonic elapsed time. Native adapters should report
known operational failures using `TriviaSdkException`, including `NETWORK`;
JVM adapters retain automatic `IOException` network classification.

On an Apple Silicon Mac, install a compatible full Xcode with an iOS simulator
runtime. Complete Xcode's first-launch setup and review/accept its license yourself.
The active developer directory must point to full Xcode rather than standalone
Command Line Tools. You can select it per command with
`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`.

From this SDK directory:

```bash
# Shared behavioral tests run with fake repositories and clocks, not real iOS adapters
./gradlew :trivia-sdk-core:iosSimulatorArm64Test

# Link device/simulator frameworks and package both slices
./gradlew :trivia-sdk-core:assembleTriviaCoreReleaseXCFramework

# Verify both ARM64 slices and compile the public API from Swift
bash scripts/check-swift.sh

# Optional: publish both native libraries into the same repository as publishJvm
./gradlew :trivia-sdk-core:publishIosArm64PublicationToLocalRepository \
  :trivia-sdk-core:publishIosSimulatorArm64PublicationToLocalRepository
```

The static framework is
`trivia-sdk-core/build/XCFrameworks/release/TriviaCore.xcframework`.
Add it to an Xcode project's frameworks (do not embed a static framework), then
`import TriviaCore`. [The Swift smoke consumer](samples/swift-consumer/Smoke.swift)
checks model, controller, clock, and observable-state visibility. It is a
compile-only check, not an iOS application or production integration.

For a Kotlin Multiplatform consumer, depend on
`com.dosparta.trivia:trivia-sdk-core:0.2.0` in `commonMain`, implement
`ITriviaRepository`, `IGameSessionRepository`, and `IContentLocalizationRepository`,
and create `TriviaSdk` with those adapters. The framework exports coroutines so
the public StateFlow contracts remain visible to Swift, but observing them still
requires a consumer-owned bridge. Suspending methods export completion-handler
APIs; `loadCategories()` and `restore()` declare typed operational exceptions for
NSError bridging. Cancellation/lifecycle ownership still belongs to the caller;
no managed Swift Task cancellation bridge is provided in this release.

## Verification and local development

Core `checkJvm` and `check` enforce at least 70% JVM line coverage, including
orchestration. Shared tests use `kotlin.test`, coroutine test utilities, and fake
adapters, and run on JVM and iOS simulator. MockK/Java-specific tests remain in
`jvmTest`. Detekt checks shared, JVM, and iOS sources. Reports:
`trivia-sdk-core/build/reports/jacoco/jacocoTestReport/`. Adapter tests cover networking,
translation, token recovery, storage mapping, and Android factory initialization.

The application normally resolves published release artifacts. For source editing,
its optional `-PuseLocalTriviaSdk=true` composite build substitutes SDK modules.
SDK checks must still pass independently. Linux CI verifies the isolated JVM
consumer and application consumption of published artifacts; macOS CI runs native
tests, assembles the XCFramework, publishes native libraries locally, and checks
Swift API compilation.

`TriviaSdk` intentionally exposes a cohesive command facade, so its class has a
narrow `TooManyFunctions` suppression rather than splitting gameplay ownership
across presentation-facing services.
