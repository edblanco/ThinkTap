# Screenshot testing

Screenshot tests render Compose UI to PNGs on the JVM with
[Roborazzi](https://github.com/takahirom/roborazzi) and compare them against committed golden
images. They complement the existing behavioural tests: those assert semantics (`testTag`, text,
`stateDescription`) and would happily pass if the palette inverted, the type scale collapsed, or a
row started wrapping.

No emulator is involved — everything runs through Robolectric inside `testDebugUnitTest`.

## Where things live

| | |
|---|---|
| Harness | `core-ui/src/testFixtures/java/com/dosparta/core/ui/screenshot/` |
| Component goldens | `core-ui/src/test/screenshots/` |
| Screen goldens | `feature-trivia-ui/src/test/screenshots/` |
| Linux goldens | `linux/` inside each module's screenshot directory |

Goldens are committed. They deliberately sit outside `build/`, which `clean` wipes and
`.gitignore` excludes.

## Commands

```bash
# Verify against the committed goldens (also runs as part of `check`).
./gradlew verifyRoborazziDebug

# Re-record goldens after an intentional UI change, then review the diff before committing.
./gradlew recordRoborazziDebug
```

A plain `./gradlew test` neither captures nor verifies anything, so the normal inner loop is
unaffected.

`check` depends on `verifyRoborazziDebug`, so a screenshot regression fails the build in the same
way Detekt and Lint failures do. When verification fails, Roborazzi writes side-by-side comparison
images to `<module>/build/outputs/roborazzi/`.

## Writing a new screenshot test

Extend `ScreenshotTest` and call `captureScreen`:

```kotlin
class MyScreenshotTest : ScreenshotTest() {
    @Test
    fun myScreen() = captureScreen("my_screen") {
        MyScreen(state = fixture)
    }
}
```

Then run `recordRoborazziDebug` to create the golden and commit it alongside the test.

Compose only allows content to be set once per test, so each image needs its own test method —
light and dark variants are separate tests rather than a loop.

## Determinism

An image is only useful if it is byte-identical across runs. The harness pins every input:

- **Reduced motion.** `captureScreen` always enters `TriviaGame2Theme` with `reducedMotion = true`.
  That collapses every `TriviaMotion` spec to `snap()`, skips the staggered entrance delays, and
  stops the shimmer and pulsing-dot loops. Those loops never idle, so capturing without this would
  hang `waitForIdle()` forever.
- **A fixed device.** Qualifiers pin a 411x891dp, 420dpi phone so image dimensions do not drift
  with Robolectric's default device.
- **SDK 33.** Roborazzi needs Robolectric's native graphics pipeline, which requires SDK 26+. The
  rest of the Robolectric suite pins SDK 24, so screenshot tests override it in the harness.
- **Host-specific baselines.** Linux uses `src/test/screenshots/linux/`; macOS keeps the
  original `src/test/screenshots/` baselines. Verification and recording both select the
  directory through the module's Roborazzi configuration, without falling back to another
  platform's images.

### Animations that ignore reduced motion

A few animations use plain Compose specs rather than `TriviaMotion`, so reduced motion does not
affect them — most notably the answer auto-advance countdown in `AnswerOptionsSection`. Letting the
clock free-run would play its full five seconds and capture a finished countdown that had already
confirmed the answer.

For those, pass `advanceTimeMillis` to drive the test clock manually:

```kotlin
captureScreen("answer_options_correct", advanceTimeMillis = 500L) { … }
```

## Caveats

Goldens are rendered by the host JVM, so font rasterisation can differ across architectures and
JDK versions. The existing goldens were recorded on developer machines.

## CI verification

[Pull-request CI](ci.md) verifies both screenshot modules on `ubuntu-24.04` (x86_64) with
Temurin JDK 17. Comparisons are strict: CI does not set a nonzero
`roborazzi.compare.changeThreshold` and never runs `recordRoborazziDebug`.

Robolectric's native renderer produces small host-dependent color differences, particularly
at rounded edges and in translucent fills. The first Linux run had six mismatches whose
per-channel differences were at most 3 out of 255, with matching dimensions, text, and layout.
The Linux baseline set contains the reviewed actual images for those six cases and the
unchanged references for the other 21. Separate baselines preserve verification on both
Linux x86_64 and macOS ARM without increasing the comparison tolerance.

For a failure, download the `screenshot-test-reports` artifact from the GitHub Actions run.
It includes the module test reports, Roborazzi reports, committed baselines, and generated
comparison images. An ordinary `test` run is not a substitute for this verification job.

Before changing a baseline, inspect the comparison and distinguish an intentional UI change
from an unintended regression or a confirmed host/JDK rendering difference. If existing
developer-machine baselines differ only because of the Linux environment, reproduce recording
on Linux x86_64 with the same JDK and harness settings, review every changed PNG, and commit
the approved baselines in a deliberate change. Do not automatically accept CI output or
loosen the comparison threshold to make the build green.

For an intentional UI change, record and review both platform sets: run
`./gradlew recordRoborazziDebug` on macOS, then on Linux x86_64 with Temurin JDK 17.
Commit both sets with the UI change. Running only the macOS recording command does not
update CI's Linux baselines. A new screenshot test also needs a baseline on both hosts.
