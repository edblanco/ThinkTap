# Pull-request CI

[The CI workflow](../.github/workflows/ci.yml) runs on **non-draft pull requests targeting
`main`**. Opening, reopening, updating, editing, or marking a PR ready for review triggers
a run. Draft PRs skip all test jobs; converting a PR back to draft cancels its older run.
There are no changed-path filters, so documentation-only PRs also run the full suite.
Pushes without a qualifying PR do not trigger CI.

New runs cancel older runs for the same PR. The four jobs are independent: failure in
one suite does not prevent another suite from running. Gradle's `--continue` runs other
independent tasks after a failure without hiding the failing exit status.

## Checks and local commands

Run these commands from the repository root using JDK 17 and the checked-in wrapper:

Each Linux job first publishes the JVM/Android SDK into `sdk/build/repository`
with `publishJvm`, without requiring Xcode or native publications. The app uses
these versioned Maven artifacts, without composite source substitution. The
macOS job verifies the iOS core separately.

| Check name | Command | Coverage |
|---|---|---|
| `JVM tests` | `./sdk/gradlew -p sdk checkJvm publishJvm`, then `./sdk/gradlew -p sdk/samples/jvm-consumer run`, then `./gradlew test` | Independent SDK checks, standalone artifact consumer, and app unit tests; 70% SDK core line-coverage minimum |
| `Screenshot tests` | `./gradlew verifyRoborazziDebug --continue --stacktrace` | Strict golden verification in `core-ui` and `feature-trivia-ui` |
| `Instrumentation tests (API 35)` | `./sdk/gradlew -p sdk connectedDebugAndroidTest` and `./gradlew connectedDebugAndroidTest` | SDK and application debug instrumentation tests on the connected emulator |
| `iOS core` | `./sdk/gradlew -p sdk :trivia-sdk-core:iosSimulatorArm64Test :trivia-sdk-core:assembleTriviaCoreReleaseXCFramework`, then `bash sdk/scripts/check-swift.sh` | Shared gameplay tests on ARM64 simulator, device/simulator framework linking, native publication, and Swift API type checking |

The instrumentation command requires a running emulator. CI creates a headless API 35
Google APIs x86_64 Pixel 5 emulator with KVM acceleration and animations disabled. Use a
host-compatible architecture locally, and disconnect unrelated devices before running
the command to avoid deploying tests to them.

Linux jobs use `ubuntu-24.04`, Temurin JDK 17, Android platform `platforms;android-37.0`,
and Build Tools `36.0.0`. Compile SDK 37 is separate from emulator API 35 and the
screenshot harness's Robolectric SDK 33.

The `iOS core` job uses the Apple Silicon `macos-15` runner with its configured
Xcode and simulator runtime. It checks the host architecture explicitly, publishes
both native libraries locally, and uploads the release `TriviaCore.xcframework`.
Android SDK setup is still needed because the independent SDK build also configures
its Android adapter modules. Local native runs require full Xcode, completed
first-launch setup, and a license accepted by the developer; standalone Command
Line Tools cannot replace it.

Gradle caches are managed by `gradle/actions/setup-gradle`. Same-repository PRs can
write PR-scoped caches for later runs; fork PRs only read caches. Jobs require no
repository secrets and have only `contents: read` token permissions. Actions are pinned
to commit SHAs. GitHub may require a maintainer to approve workflows from outside
contributors before any tests run.

## Coverage requirement

The `JVM tests` check fails when **line coverage is below 70%** in
`sdk/trivia-sdk-core`. Exactly 70% passes. This includes both existing game rules
and session orchestration extracted from the ViewModel. UI, Android adapters, and
generated Android classes are not counted. Coverage comes from Kotlin/JVM core
unit tests, not instrumentation or screenshot tests.

Run the gate locally with `./sdk/gradlew -p sdk :trivia-sdk-core:jacocoTestCoverageVerification`. It runs the
required tests and generates HTML and XML reports under
`sdk/trivia-sdk-core/build/reports/jacoco/jacocoTestReport/` before checking the threshold.
Missing execution data or compiled classes fail explicitly instead of silently
skipping the coverage gate.
Run `./sdk/gradlew -p sdk :trivia-sdk-core:jacocoTestReport` to generate reports without
enforcing the minimum.

## Failure reports

Open the failed check's **Details**, then the workflow run's **Artifacts** section:

- `jvm-test-reports`: module HTML test reports, XML test results, and JaCoCo HTML/XML
  coverage reports (including when coverage is below 70%).
- `screenshot-test-reports`: test reports, Roborazzi reports/results, comparison images,
  and committed baselines.
- `instrumentation-test-reports-api-35`: connected Android test reports and results.
- `ios-core-reports`: native test reports and the release device/simulator XCFramework.

Artifacts are retained for 14 days and uploaded after successes or failures, unless the
run is cancelled. Setup/compilation failures may leave no reports; the upload step warns
if none exist, and the original failure remains visible in the job log.

See [screenshot testing](screenshot-testing.md) for reviewing visual failures. CI never
records new baselines or accepts a nonzero image-difference threshold. Linux verification
uses each screenshot module's committed `src/test/screenshots/linux/` baseline set;
macOS recording and verification continue to use the original directory. Intentional UI
changes must update and review both platform sets.

## Require checks before merging

This workflow does not change repository rules. After its first successful run on a
non-draft PR to `main`, a maintainer can:

1. Open **Settings > Rules > Rulesets** and create or edit a branch ruleset targeting
   `main` (or use the existing branch-protection settings).
2. Require pull requests and status checks before merging.
3. Select `JVM tests`, `Screenshot tests`, `Instrumentation tests (API 35)`, and `iOS core` from the
   observed GitHub Actions checks, and enable the rule.

Without such a rule, failed CI does not itself prevent merging. Drafts skip checks but cannot be merged until ready for review, which triggers the full suite. Merge queues
are not configured; enabling one also requires adding `merge_group` workflow support.

## Validating workflow changes

Run `actionlint .github/workflows/ci.yml` and inspect task coverage with:

```bash
./sdk/gradlew -p sdk checkJvm publishJvm --dry-run
./sdk/gradlew -p sdk publishJvm
./sdk/gradlew -p sdk/samples/jvm-consumer run
./gradlew test verifyRoborazziDebug connectedDebugAndroidTest --dry-run
# On an Apple Silicon Mac with configured Xcode
./sdk/gradlew -p sdk :trivia-sdk-core:iosSimulatorArm64Test \
  :trivia-sdk-core:assembleTriviaCoreReleaseXCFramework
bash sdk/scripts/check-swift.sh
```

A dry run confirms task selection, not passing tests. Local macOS runs also cannot
prove Linux screenshot portability or hosted emulator behavior: verify all four checks
on a real GitHub PR before making them required.
