# Pull-request CI

[The CI workflow](../.github/workflows/ci.yml) runs on **non-draft pull requests targeting
`main`**. Opening, reopening, updating, editing, or marking a PR ready for review triggers
a run. Draft PRs skip all test jobs; converting a PR back to draft cancels its older run.
There are no changed-path filters, so documentation-only PRs also run the full suite.
Pushes without a qualifying PR do not trigger CI.

New runs cancel older runs for the same PR. The three jobs are independent: failure in
one suite does not prevent another suite from running. Gradle's `--continue` runs other
independent tasks after a failure without hiding the failing exit status.

## Checks and local commands

Run these commands from the repository root using JDK 17 and the checked-in wrapper:

| Check name | Command | Coverage |
|---|---|---|
| `JVM tests` | `./gradlew test --continue --stacktrace` | All configured Android local/Robolectric test tasks and `quality-detekt-rules:test` |
| `Screenshot tests` | `./gradlew verifyRoborazziDebug --continue --stacktrace` | Strict golden verification in `core-ui` and `feature-trivia-ui` |
| `Instrumentation tests (API 35)` | `./gradlew connectedDebugAndroidTest --continue --stacktrace` | All modules' debug instrumentation tests on the connected emulator |

The instrumentation command requires a running emulator. CI creates a headless API 35
Google APIs x86_64 Pixel 5 emulator with KVM acceleration and animations disabled. Use a
host-compatible architecture locally, and disconnect unrelated devices before running
the command to avoid deploying tests to them.

All jobs use `ubuntu-24.04`, Temurin JDK 17, Android platform `platforms;android-37.0`,
and Build Tools `36.0.0`. Compile SDK 37 is separate from emulator API 35 and the
screenshot harness's Robolectric SDK 33.

Gradle caches are managed by `gradle/actions/setup-gradle`. Same-repository PRs can
write PR-scoped caches for later runs; fork PRs only read caches. Jobs require no
repository secrets and have only `contents: read` token permissions. Actions are pinned
to commit SHAs. GitHub may require a maintainer to approve workflows from outside
contributors before any tests run.

## Failure reports

Open the failed check's **Details**, then the workflow run's **Artifacts** section:

- `jvm-test-reports`: module HTML test reports and XML test results.
- `screenshot-test-reports`: test reports, Roborazzi reports/results, comparison images,
  and committed baselines.
- `instrumentation-test-reports-api-35`: connected Android test reports and results.

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
3. Select `JVM tests`, `Screenshot tests`, and `Instrumentation tests (API 35)` from the
   observed GitHub Actions checks, and enable the rule.

Without such a rule, failed CI does not itself prevent merging. Drafts skip checks but cannot be merged until ready for review, which triggers the full suite. Merge queues
are not configured; enabling one also requires adding `merge_group` workflow support.

## Validating workflow changes

Run `actionlint .github/workflows/ci.yml` and inspect task coverage with:

```bash
./gradlew test verifyRoborazziDebug connectedDebugAndroidTest --dry-run
```

A dry run confirms task selection, not passing tests. Local macOS runs also cannot
prove Linux screenshot portability or hosted emulator behavior: verify all three checks
on a real GitHub PR before making them required.
