---
name: live-data-e2e-test
description: >-
  Write reliable on-device UI tests for the Wikipedia Android app that drive a real screen and check
  what actually renders, using the base/livedata framework (LiveDataComposeTest + ComposeRobot +
  feature robots). Use this for ANY instrumented / androidTest / Espresso / end-to-end / "test the
  real screen" UI test — including tests that tap a tab or button, open a dialog or bottom sheet, or
  follow a flow across screens, AND including local screens that don't use the network. Trigger
  phrases: "write an espresso test", "add a UI test", "instrumented test", "androidTest", "test this
  screen", "test this dialog / bottom sheet / popup", "test the real flow without mocks", "test the
  feed/search/article/widget". Prefer this over hand-written Espresso/Compose-rule tests — it exists
  to stop the flakiness those hit. Do NOT use it for Robolectric/JVM unit tests, ViewModel logic
  tests, or screenshot tests.
---

# On-device UI tests (the "live-data" framework) vwer

These tests open a **real screen in the real app and check what actually shows up**. No fake data, no
mocks. They catch the kind of bug a unit test can't: "the real screen is broken when you actually run
it." Most of these tests also talk to the **live Wikipedia servers** (no mocking), and that is the
hard part this framework is built to handle.

**The one big problem this framework solves:** the live data changes every day. So a test must be
written to **fail only when the app is actually broken — never just because today's data was
different.** A test that randomly fails on a quiet news day teaches everyone to ignore red, which is
worse than having no test at all.

**Read the real examples first — they are the source of truth, this doc can fall behind:**
- `app/src/androidTest/java/org/wikipedia/base/livedata/LiveDataComposeTest.kt` — the base test class.
- `app/src/androidTest/java/org/wikipedia/base/livedata/ComposeRobot.kt` — the base "robot" (all the waiting/clicking helpers).
- Feed example (with the "oracle" trick for changing data): `tests/explorefeed/HomeFeedLiveDataTest.kt` + `robots/feature/HomeFeedRobot.kt` + `tests/explorefeed/FeedContract.kt`.
- Across-screens example (Compose search → WebView article): `tests/search/ArticleSearchJourneyLiveDataTest.kt` + `robots/feature/SearchResultsRobot.kt` + `robots/feature/ArticleRobot.kt`.
- Local pop-up example (a dialog, no network): `tests/widgets/SearchWidgetInstallLiveDataTest.kt` + `robots/feature/SearchWidgetInstallRobot.kt`.

## When to use this (and when not to)

**Use it** whenever you drive a real screen and check what renders. That covers a lot:
- A flow that talks to the live servers: feed loads, search → open article, save → reading list, switch language, nearby/places, games.
- A flow that crosses **more than one screen** (the base can follow the user from one Activity to the next, not just stop at "a navigation happened").
- A **local screen with no network** — a dialog, bottom sheet, or pop-up that's built into the app. This still fits! (See the note below on the two things that trip people up.)

Most screens — even ones that load from the servers, like an article page or search results — show the
same thing every time and just need a plain "did it render?" check. Only screens with optional,
server-curated content that varies day to day (mainly the feed) need the extra "always there vs.
sometimes there" work in steps 3–4.

**Don't use it** for pure logic, lots of input combinations, or error handling — those belong in
fast Robolectric/JVM unit tests that don't need a device. Keep these on-device tests few and focused.

### Two things that trip people up: the oracle and the network gate

- **Most screens don't need the "oracle" (steps 3–4).** That whole idea is only for screens with
  optional, server-curated content that may be missing today — in practice, mainly the feed. An
  article page, search results, or a local dialog show the same thing every time, so you just assert
  it's there. Don't build a `FeedContract`-style oracle for a screen that doesn't have day-to-day
  variance — it's wasted effort. (See step 3 for the test.)
- **The network gate skips *every* test offline, even ones that don't use the network.** The base
  class skips a test when the device has no internet (because the feed/search tests need it). A local
  screen (e.g. an onboarding dialog, the search-widget bottom sheet) doesn't need internet but is still
  skipped offline — a known wart. Just remember a "skipped" run tested nothing, so run on a device
  that *is* online.

Either way, the structure (base class + robot + test tags) is still the right tool — it's what keeps
the test stable.

## The steps

### 1. Pick the flow and the screen it starts on

One user flow per test method. Your test class extends `LiveDataComposeTest<StartingActivity>`. If the
flow starts in the middle of the app (e.g. search results for a query), override `launchIntent()` to
start it the right way; otherwise the base just opens the starting Activity for you.

### 2. Figure out how each screen is built, and tag/grab it the right way

A single user journey can cross **three different UI technologies**, and each one is checked with a
different tool. Before writing the robot, identify which kind each screen is (search the screen's
source: a `setContent { }` / `@Composable` is Compose; an XML layout in `res/layout/` + a
`Fragment`/`Activity` with `findViewById`/view binding is a legacy View screen; the article body is a
WebView). Then:

**A) Compose screens** (the feed, search results, most new screens) → use Compose test tags.
**Anchor on tags, not on text** — text changes every day, tags don't. Next to the screen, add a
`*TestTags` object and put `Modifier.testTag(...)` on the nodes you need: at least the **main content
container** (if it's there, the screen loaded) and anything you tap. Copy `feed/HomeScreen.kt`
(`HomeScreenTestTags`) and `search/SearchResultsScreen.kt` (`SEARCH_LIST_TAG`). These tags ship in the
real app — that's the accepted price of testability. In the robot, drive them with the `ComposeRobot`
helpers (`awaitTag`, `clickTag`, …).

**B) Legacy View / XML screens** (the bottom nav, the History tab, Reading Lists, toolbars, dialogs
built from XML) → there are no Compose tags; you can't use `testTag` or the `ComposeRobot` tag helpers.
Drive these with **Espresso using the view's XML id**: `onView(withId(R.id.<id>)).perform(click())` to
tap, and `onView(withId(R.id.<id>)).check(matches(isDisplayed()))` to assert. Find the id in the
screen's `res/layout/*.xml`. Real examples already in the tree:
- Tapping a bottom-nav tab: `onView(withId(R.id.nav_tab_search)).perform(click())` (`SearchWidgetInstallRobot`).
- A RecyclerView list: `onView(withId(R.id.history_list))` (`SearchRobot`), `onView(withId(R.id.reading_list_recycler_view))` (`ReadingListRobot`).
It's normal and expected for one robot to mix Espresso (for the View parts) and Compose helpers (for
the Compose parts) — see `SearchWidgetInstallRobot`, which taps the View bottom-nav with Espresso and
then checks the Compose dialog with tag helpers.

**C) WebView screens** (the article page) → no Compose tags and no useful view ids inside the page;
check the rendered content with **UiAutomator**. Match the WebView with
`By.clazz(Pattern.compile(".*WebView"))`, **not** the exact `"android.webkit.WebView"`, because the app
uses `ObservableWebView` (a subclass) and the matcher only matches the exact class name. Then assert
content with `device.wait(Until.hasObject(By.textContains(...)), timeout)`. Full example: `ArticleRobot`.

**Two cross-technology traps** (also in Gotchas): (1) if your *first* action on launch is an Espresso
tap on a View (e.g. a tab), wait for a Compose "screen is up" tag first or you'll hit "No activities in
stage RESUMED"; (2) a Compose tag check (`awaitTagGone`, etc.) **crashes** with "No compose hierarchies
found" if the screen currently in front is a pure-View screen — see the gone-check note in Gotchas.

### 3. Decide what's "always there" vs "only sometimes there" (most screens skip this)

This step is **only for screens that show optional content the servers may or may not include today —
in practice, mainly the Explore feed.** It is NOT about whether the screen uses the network. Plenty of
server-backed screens don't need this at all: an article page, search results, user contributions, a
settings screen, or a local dialog all show the same thing every time for the same input — so there's
nothing to sort, just assert it's there and jump to step 5.

The test for whether this step applies: *could a thing I want to check be missing today purely because
the servers chose not to include it, even though the app is working fine?* If no (an article always
renders, a results list always appears), it's simply "always there." If yes (the feed's "Top Read"
block), keep reading.

"Always there" means **guaranteed under your test's fixed setup** (the language/wiki you pin, the query
you type) — NOT a universal rule. Nothing in the feed is guaranteed on every wiki; different language
wikis serve different things, and even the Featured Article can be absent on some wikis (notice the
oracle in `FeedContract.kt` guards it with `content.tfa?.let { … }` — it can be null). The feed tests
only treat the Featured Article as reliable because they **pin the app language to a wiki where it's
reliably served** (English). Change the wiki and that assumption can break. So: pin your fixture, and
only call something "always there" relative to that fixture.

Sort each thing you want to check into one of three buckets:

- **Always there (under your fixture)** — e.g. the search **results list** for a real query (the
  cleanest example — type a word, you always get a list), or the Featured Article *on the English wiki
  your test pins*. Assert it hard. If it's missing, that's a real bug.
- **Only sometimes there** — the servers may or may not send it today (Top Read, Picture of the Day,
  News). **Never just assert "this must be on screen"** — that's the classic random failure. Use the
  oracle in step 4 instead. How to tell which is which: trace where the data comes from — fields the
  servers always compute are safer; editorial/optional ones are not. For the feed, see
  `feed/aggregated/AggregatedFeedContent.kt` (`topRead`/`potd`/`news`/`onthisday`/`dyk` are clearly
  optional; `tfa` is reliable on major wikis but still nullable, so don't assume it elsewhere).
- **Should NOT be there** — e.g. a card you hid stays hidden. Check it's gone with `awaitTagGone`.

For **taps/saves/shares**, act on an "always there (under your fixture)" element so the test doesn't
randomly fail on a day an optional thing is missing — e.g. the feed tests pin English and tap the
Featured Article card, which is reliably first there.

### 4. For "only sometimes there" data, use the oracle (most screens skip this — see step 3)

The tempting shortcut — "only ever check stuff that's always there" — is wrong, because then you'd
never notice if something that *should* have shown up went missing. The better way: **ask the servers
yourself** what they sent today, then check the UI matches:

- Servers sent it **and** the UI shows it → pass.
- Servers sent it **but** the UI doesn't → **fail with a clear message** ("the data was there, the UI
  didn't show it") — this is the most valuable bug you can catch here.
- Servers didn't send it → it's just not expected today, so no false failure.

`FeedContract.kt` is the worked example: it calls the same `ServiceFactory.getRest(...).getFeedFeatured(...)`
the app calls, and returns a list of the modules actually sent today. `HomeFeedRobot.assertServedModulesRendered(...)`
then checks each one is reachable with `ComposeRobot.assertReachableByScroll(list, target, reason)`,
which fails with a readable message. Copy this shape only when your feature has server-driven optional
content. (Today the oracle covers 3 modules — featured article, top read, picture of the day; add more
the same way if you need them.)

### 5. Write the feature robot

A "robot" is a small helper class that describes **what** the user does, not the fiddly **how**.
Extend `ComposeRobot` and build your methods from the helpers it gives you (`awaitTag`, `awaitTagGone`,
`awaitText`, `clickTag`, `scrollToAndClick`, `assertReachableByScroll`, `assertHasChildren`,
`assertSnackbarShown`). Keep methods chainable with `= apply { ... }`. Pick a name that doesn't clash
with the old `BaseRobot` robots (there's already a `SearchRobot` — the new-style one is
`SearchResultsRobot`).

### 6. Write the test

Extend `LiveDataComposeTest<StartingActivity>`. Put **only your feature's own setup** in
`prepareDeviceState()` (e.g. a fixed starting tab, the app language) and any database/DataStore
cleanup in `resetPersistentState()`. **Don't** re-list the dialog-suppression flags — the base already
turns off all the known pop-ups for every test (see Gotchas). For navigation and sharing, use the
base helpers: `assertNavigatedTo(Activity)`, and `stubShareChooser()` + `assertShareChooserFired()`.

Note on order: the base runs `suppressKnownDialogs()` **first**, then your `prepareDeviceState()`. So
if your test is *about* a pop-up the base normally suppresses, just turn it back on in
`prepareDeviceState()` — yours runs last and wins. (That's exactly what the search-widget test does.)

### 7. Prove the test actually works — including that it can fail (don't skip this)

A passing test means nothing until you've watched it fail on purpose. Do all three:

1. **Compile:** `./gradlew :app:compileDevDebugAndroidTestKotlin`
2. **Run it on a real device/emulator that has internet** (find one with `adb devices`):
   ```bash
   ANDROID_SERIAL=<serial> ./gradlew :app:connectedDevDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=org.wikipedia.tests.<pkg>.<Class>
   ```
   Make sure it's **green with 0 skipped** — a skipped run (e.g. no internet) is NOT a pass.
3. **Make it fail once on purpose:** point one check at a wrong tag/text (or break the thing it
   checks), re-run, confirm it fails with a sensible message, then undo it. This catches "checks" that
   can never actually fail.

## The rules that keep these tests stable (and why)

1. **Check the actual screen, never read `viewModel.state.value`.** In the test environment, the
   screen's background work is queued, so reading state directly often sees old data.
2. **Wait with `waitUntil { node }`, never `waitForIdle()`.** A screen loading from the network may
   never go "idle", so the base waits for a specific thing to appear instead.
3. **Check shapes, not exact text** ("at least 1 item", "went to screen X", "this module showed up") —
   never a specific article title that changes daily.
4. **For "sometimes there" data, diagnose it (step 4); don't just avoid checking it.**
5. **The internet check uses a fresh `ConnectivityManager`, not `WikipediaApp.isOnline`** (that flag
   can be a stale "false" on a cold CI machine and wrongly skip everything). The base handles this —
   just remember an offline run **skips** (looks green but ran nothing), so CI should make sure a
   non-zero number of tests actually ran.

## Gotchas (each of these cost a real debugging session at least once)

- **"no view found" / "no node found" → LOOK before you change anything.** This is almost always a
  pop-up covering the screen, not a wrong locator. Do NOT start swapping tags, tweaking navigation, or
  re-running blindly. First read what was actually on screen: the failure already prints the **view
  hierarchy**, and the base now logs a line from **`ScreenStateReporter`** naming any dialog/sheet that
  was up at failure (e.g. `…covered the screen: [SearchWidgetInstallDialog]`). If it names a dialog,
  the fix is to suppress or dismiss that dialog — not to touch your locator. `ScreenStateReporter` is
  generic: it reads the live fragment tree, so it catches even a brand-new dialog nobody added to a
  suppression list (the denylists only hide popups someone already knew about). If it says "no dialog
  was on screen", the cause really is elsewhere (wrong screen / locator / timing) — only then start
  adjusting.
- **A pop-up covers the screen → "no node found" / "WebView never appeared".** The app shows lots of
  one-time onboarding/announcement/game pop-ups from different screens (Explore feed announcement; the
  article-page On-this-day game dialog; hybrid-search onboarding; the search-widget install sheet).
  The base `suppressKnownDialogs()` turns them all off before every test. **If a new test trips a new
  pop-up, find its `Prefs` flag and add it to `suppressKnownDialogs()` once** so no future test hits
  it. Don't work around it per-test.
- **`awaitTagGone` crashes with "No compose hierarchies found" when the screen underneath has no
  Compose.** `awaitTagGone` (and any tag check) blows up — instead of returning "it's gone" — when the
  screen has zero Compose at all. This happens after you close a Compose dialog and the screen behind
  it is an old Views screen (e.g. closing a sheet over the Search tab, which is `HistoryFragment`).
  Fix: wait with a check that catches the crash and treats "no Compose at all" as "the dialog is gone."
  See `SearchWidgetInstallRobot.awaitPromptGone()`.
- **Tapping an old-style View to reach a Compose screen → "No activities in stage RESUMED".** If your
  first action is an Espresso tap (e.g. a bottom-nav tab) right after launch, the Activity may not be
  ready yet. **Wait for a Compose "the screen is up" tag first**, then tap. See
  `SearchWidgetInstallRobot.waitForHomeLoaded()`.
- **A button that opens a system pop-up (e.g. "Add to home screen") leaves your app in the
  background.** Anything that hands off to an Android system dialog you can't stub (like
  `requestPinAppWidget`) means there's no app screen in front anymore, so the next in-app tap fails
  with "No activities in stage RESUMED". Two fixes: (a) if you need to keep using the app afterward,
  dismiss via a button that DOESN'T leave the app (e.g. the Close button); (b) clean up the system
  pop-up so it doesn't bleed into the next test:
  `if (device.currentPackageName != context.packageName) device.pressBack()`. See
  `SearchWidgetInstallRobot.dismissSystemPinWidgetDialogIfPresent()`.
- **WebView matching:** match `.*WebView` by pattern, not the exact class name (see step 2).
- **Robot name clashes** with the old `BaseRobot` robots — check the names already in `robots/feature/`
  before naming a new one.
- **`hamcrest` import:** use `org.hamcrest.Matchers.allOf`, not `CoreMatchers.allOf` (the wrong one
  crashes at runtime with `NoSuchMethodError`).
- **A tag you can find but can't click:** the `await*` helpers look at the "unmerged" tree, while
  `clickTag`/`scrollTo` use the "merged" tree. A tag that exists unmerged but gets folded into its
  parent will pass `awaitTag` but then fail on `clickTag`. Fix: put the tag directly on the clickable
  node.
