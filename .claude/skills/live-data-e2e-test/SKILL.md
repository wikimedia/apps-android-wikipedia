---
name: live-data-e2e-test
description: >-
  Write reliable live-data (no-mock) end-to-end UI instrumented tests for the Wikipedia Android app
  using the base/livedata framework (LiveDataComposeTest + ComposeRobot + feature robots, empty
  Compose rule + ActivityScenario, the FeedContract-style diagnostic oracle, and suppressKnownDialogs).
  Use this WHENEVER the task involves adding or fixing an end-to-end / E2E / instrumented / on-device /
  connected UI test that drives a real Activity against the live network — including phrases like
  "write an espresso test", "add a UI test", "test this screen end to end", "instrumented test for the
  feed/search/article", "androidTest", or "test the real flow without mocks". Prefer this over ad-hoc
  Espresso/Compose-rule tests: it exists specifically to stop the flakiness those hit. Do NOT use it
  for Robolectric/JVM unit tests, ViewModel logic tests, or screenshot tests.
---

# Live-data end-to-end UI tests

These tests drive a **real Activity against the live Wikipedia backend with no mocking**, and assert on
the rendered UI. They exist to catch the bug class unit tests can't: "the real screen, talking to the
real API, is broken." The whole framework is shaped around one hard problem — **making a test that
talks to live data fail only when the *app* is broken, never because the data varied today.** A test
that flakes on quiet days trains people to ignore red, which is worse than no test.

Reference implementations (read these first — they are the source of truth, this doc can drift):
- `app/src/androidTest/java/org/wikipedia/base/livedata/LiveDataComposeTest.kt` — base test class.
- `app/src/androidTest/java/org/wikipedia/base/livedata/ComposeRobot.kt` — base robot (sync vocabulary).
- `app/src/androidTest/java/org/wikipedia/tests/explorefeed/HomeFeedLiveDataTest.kt` + `robots/feature/HomeFeedRobot.kt` + `tests/explorefeed/FeedContract.kt` — single-screen feed example **with the diagnostic oracle**.
- `app/src/androidTest/java/org/wikipedia/tests/search/ArticleSearchJourneyLiveDataTest.kt` + `robots/feature/SearchResultsRobot.kt` + `robots/feature/ArticleRobot.kt` — **cross-Activity** journey (Compose search → WebView article).

## When this is the right tool

Use it for a real **user journey** through rendered UI against the real backend: feed loads, search →
open article, save → reading list, language switch, places/nearby, games. It can span **multiple
Activities** (the base uses an empty Compose rule + `ActivityScenario`, so a test follows the user from
one screen to the next instead of stopping at "a navigation Intent fired").

Do **not** use it for logic, state permutations, or error paths — those belong in Robolectric/JVM unit
tests, which are faster and don't need a device or network. Keep live-data tests few and focused.

## The workflow

### 1. Pick the journey and its entry Activity

One coherent user flow per test method. The test class extends `LiveDataComposeTest<EntryActivity>`.
For a flow that starts mid-app (e.g. search results for a query), override `launchIntent()` to launch
with the right intent; otherwise the base launches the entry Activity bare.

### 2. Add stable test tags to the production UI (only if missing)

Anchor on **semantics, not live text** (text changes daily). Add a `*TestTags` object next to the
screen and `Modifier.testTag(...)` the nodes you need: at minimum the **content container** (its
presence means "loaded") and anything you interact with. Follow the existing pattern in
`feed/HomeScreen.kt` (`HomeScreenTestTags`) and `search/SearchResultsScreen.kt` (`SEARCH_LIST_TAG`).
These tags ship in production — that's the accepted cost of testability here.

WebView/legacy-View screens (e.g. the article page) have no Compose tags; assert those via UiAutomator
(see `ArticleRobot`) — match a WebView with `By.clazz(Pattern.compile(".*WebView"))`, **not** the exact
`"android.webkit.WebView"`, because the app uses `ObservableWebView` (a subclass) and `By.clazz` matches
the concrete class name.

### 3. Classify every target — this is what makes the test reliable

For each thing you assert, decide which kind it is. This is the core judgment; get it wrong and the
test either flakes or catches nothing.

- **Invariant** — guaranteed present by the app/API contract (today's Featured Article; the search
  results list for a real query). Assert it hard. Absence = a real bug.
- **Conditional** — present only if the backend served it today (Top Read, Picture of the Day, News).
  **Never blind-assert these** — that's the classic flake. Instead use the **diagnostic oracle**
  (step 4). To tell which modules are conditional, trace the data source: algorithmic/always-returned
  fields are safe anchors, editorial/optional ones are not. For the feed, the contract is
  `feed/aggregated/AggregatedFeedContent.kt` (`tfa` invariant; `topRead`/`potd`/`news`/`onthisday`/`dyk`
  conditional).
- **Negative** — should *not* be present (a hidden card stays hidden after you hide it). Assert absence
  with `awaitTagGone`.

For **interactions** (tap/save/share), always anchor on an **invariant** element so the test doesn't
flake on a day an optional module is absent — e.g. the feed interaction tests all target the Featured
Article card, which is always first.

### 4. For conditional content, use the diagnostic oracle (don't avoid it — diagnose it)

The old instinct "only ever assert always-present content" is wrong: it means you never catch a
regression where something expected *disappeared*. The fix is not avoidance, it's **diagnosis** — make
a failure say *why*. Independently read the same backend the app reads, derive what the UI *should*
show, then assert parity:

- Backend served it **and** UI shows it → pass.
- Backend served it **but** UI doesn't → **hard, named failure** ("data had it, UI didn't render it" —
  the most valuable bug you can catch here).
- Backend didn't serve it → it's simply not in the expected set, so no false failure.

`FeedContract.kt` is the worked example: it calls the same `ServiceFactory.getRest(...).getFeedFeatured(...)`
the app uses and returns a tag→name map of the tagged modules actually served today;
`HomeFeedRobot.assertServedModulesRendered(...)` then asserts each is reachable via
`ComposeRobot.assertReachableByScroll(list, target, reason)`, which rethrows with a diagnostic message.
Copy this shape for any feature with conditional content.

### 5. Write the feature robot

Extend `ComposeRobot`; express **intent**, not mechanics. Build methods from the inherited helpers
(`awaitTag`, `awaitTagGone`, `awaitText`, `clickTag`, `scrollToAndClick`, `assertReachableByScroll`,
`assertHasChildren`, `assertSnackbarShown`). Keep methods fluent with `= apply { ... }` so calls chain.
Name it to avoid colliding with legacy `BaseRobot` robots (there is already a `SearchRobot` — the
live-data one is `SearchResultsRobot`).

### 6. Write the test

Extend `LiveDataComposeTest<EntryActivity>`. Put only **feature-specific** state in
`prepareDeviceState()` (a deterministic starting tab, the app language) and DataStore/DB cleanup in
`resetPersistentState()`. Do **not** re-list dialog-suppression Prefs — the base already runs
`suppressKnownDialogs()` for every test (see Gotchas). Use the base assertions for navigation and
sharing: `assertNavigatedTo(Activity)`, `stubShareChooser()` + `assertShareChooserFired()`.

### 7. Verify — including that it can fail (do not skip this)

A green test proves nothing until you've shown it can go red. Run all three:

1. **Compile**: `./gradlew :app:compileDevDebugAndroidTestKotlin`
2. **Run on a connected device/emulator with a validated network** (find one with `adb devices`):
   ```bash
   ANDROID_SERIAL=<serial> ./gradlew :app:connectedDevDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=org.wikipedia.tests.<pkg>.<Class>
   ```
   Confirm it runs and is **green with 0 skipped** — a skipped run (offline) is not a pass.
3. **Prove non-vacuity**: temporarily point one assertion at a bogus tag/text (or break the target),
   re-run, confirm it fails **with your diagnostic message**, then revert. This is how you catch
   assertions that can never fail (e.g. `scrollTo` already throws implicitly if a node is missing, so
   a following "assert present" can be vacuous).

## Reliability rules (the *why* behind the base class)

1. **Assert through the rendered UI, never read `viewModel.state.value`.** Under the v2 Compose test
   dispatcher, launched coroutines are queued, so a synchronous state read sees stale data.
2. **Synchronize with `waitUntil { node }`, never `waitForIdle()`.** A screen loading from the network
   may never go idle; the base polls semantic anchors against real elapsed time instead.
3. **Assert structural invariants, not live text** ("≥1 item", "navigated to X", "served module
   rendered") — never a specific article title that changes daily.
4. **Make conditional absence diagnostic, don't avoid it** (step 4). This supersedes the older "only
   anchor on always-present content" rule.
5. **Gate on a fresh `ConnectivityManager` check, not `WikipediaApp.isOnline`** — the app's flag caches
   for 60s and can be a stale `false` on a cold CI emulator, wrongly skipping the suite. The base
   already does this; just know an offline run **skips** (green-but-skipped), so CI should assert a
   non-zero executed count.

## Gotchas catalog (each cost a real debugging cycle once)

- **Popups cover the screen → "no node found" / "WebView never appeared".** The app fires many one-off
  onboarding/announcement/game dialogs from different screens (Explore feed announcement; the
  **article page On-this-day game dialog**; hybrid-search onboarding redirect). The base
  `suppressKnownDialogs()` sets the whole known battery before launch for every test. **When a new test
  trips a new dialog, find its gating `Prefs` flag and add it to `suppressKnownDialogs()` once** — so no
  future test rediscovers it. Don't paper over it per-test.
- **WebView class matching**: see step 2 — match `.*WebView` by pattern.
- **Robot name collisions** with legacy `BaseRobot` robots — check existing names in
  `robots/feature/` before naming a new robot.
- **`hamcrest` import**: use `org.hamcrest.Matchers.allOf`, not `CoreMatchers.allOf` (runtime
  `NoSuchMethodError`).
- **Reaching a node you scrolled to**: the `await*` helpers query the unmerged tree; `clickTag`/`scrollTo`
  use the merged tree. A tag present unmerged but merged into a parent will pass `awaitTag` then throw
  on `clickTag`. If that bites, tag the clickable node directly.
