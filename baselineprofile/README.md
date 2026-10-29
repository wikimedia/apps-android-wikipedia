# Baseline Profile and Startup Profile

This module generates the profiles that make the app start and run faster. The generated files are
committed to the app module:

- `app/src/main/generated/baselineProfiles/baseline-prof.txt`: the **Baseline Profile**. It lists
  the classes and methods used on startup and on the most common journeys (browsing the feed,
  reading an article).
- `app/src/main/generated/baselineProfiles/startup-prof.txt`: the **Startup Profile**. It lists only
  the classes and methods used during a cold start into the home feed.

## Why they help

Our code ships as DEX bytecode. Without a profile, a freshly installed or updated app runs mostly in
ART's interpreter and JIT, and is only compiled ahead of time once the device has collected enough
usage data, which can take days. Until then, startup and first-run jank are noticeably worse.

- The **Baseline Profile** is packaged into the APK/AAB (as `assets/dexopt/baseline.prof`). The Play
  Store and `profileinstaller` use it to AOT-compile the listed code at install time, so it's fast
  from the very first launch.
- The **Startup Profile** is used at build time by R8 (`dexLayoutOptimization = true` in
  `app/build.gradle`). R8 puts the startup classes into the primary `classes.dex` file and orders
  them, so startup touches fewer DEX files and fewer memory pages.

Neither profile changes behavior: if a profile is missing or out of date, the app still works, just
with less of a speed benefit. Rules for classes that no longer exist are ignored.

## How to regenerate

Do this periodically (e.g. before each release, or after significant changes to startup, the home
feed, or the article screen), and commit the updated files.

1. Run:
   ```
   ./gradlew :app:generateBaselineProfile
   ```
   This builds the `alphaNonMinifiedRelease` variant, boots a dedicated headless emulator (the
   `pixel6Api35` Gradle Managed Device, defined in `baselineprofile/build.gradle.kts`), runs
   `BaselineProfileGenerator` on it, and copies the result into
   `app/src/main/generated/baselineProfiles/`. The same profile is used by all flavors. The first run
   downloads the API 35 system image if you don't already have it. The whole run takes a few minutes.
2. Check that both files were actually updated. If the app couldn't be installed, or no device was
   available, the task can still report `BUILD SUCCESSFUL` without generating anything (look for
   "No baseline profile rules were generated" in the output).
3. Review the diff (it's expected to be large and noisy) and commit it.

To generate on a device or emulator that's already running instead, add `-PgenerateOnConnectedDevice`.
That device must run **API 33+** (or be rooted on API 28+), be set to **English**, be online, and have
enough free storage for the app.

## How to verify the benefit

`StartupBenchmarks` measures cold startup with no precompilation versus with the Baseline Profile.
Run it on a **physical device** (emulator timings aren't representative):
```
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
    -P android.testInstrumentationRunnerArguments.class=org.wikipedia.baselineprofile.StartupBenchmarks
```
Compare `timeToInitialDisplayMs` and `timeToFullDisplayMs` between the two tests in the output, or in
`baselineprofile/build/outputs/connected_android_test_additional_output/`.

To check that a release build contains the profile, look for `assets/dexopt/baseline.prof` in the
APK, or in the AAB under `BUNDLE-METADATA/com.android.tools.build.profiles/`.

## How to maintain the journeys

The journeys live in `AppJourneys.kt`, and are run by `BaselineProfileGenerator`:

- `startup()` uses `includeInStartupProfile = true`, so its code goes into both profiles. Keep it
  limited to what happens on a normal cold start; anything else in the startup profile pushes
  real startup classes out of the primary DEX file.
- `browseAndRead()` covers the most common journeys after startup, and only goes into the Baseline
  Profile. This is the place to add other important journeys (e.g. search, or the Places map).

The journeys find UI elements with UI Automator, by English text, content description, or view ID.
If a generation run fails with a timeout, the UI has probably changed; update the selectors in
`AppJourneys.kt`. `skipOnboarding()` gets past the first-run screens before collection starts, so
that onboarding code isn't treated as part of startup.
