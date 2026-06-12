package org.wikipedia.base.livedata

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Instrumentation.ActivityResult
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.wikipedia.base.ScreenStateReporter
import org.wikipedia.settings.Prefs

/**
 * Base class for **live-data, no-mock** end-to-end Compose tests. A subclass points at an entry
 * [Activity] and drives feature behaviour through a [ComposeRobot]; the shared, fiddly setup lives
 * here: empty Compose rule, connectivity gate, disabled animations, [Intents] lifecycle,
 * notifications permission, and reusable navigation/share assertions.
 *
 * Uses the **empty** v2 Compose rule and launches the entry Activity itself via [ActivityScenario].
 * Unlike a rule bound to one Activity, this lets a test follow the user **across Activities** (e.g.
 * search → article page) while still synchronizing on the resumed screen — a real end-to-end journey
 * rather than a single screen plus a "navigation fired" check.
 *
 * See the `live-data-e2e-test` skill (`.claude/skills/live-data-e2e-test/`) for the full recipe and rules.
 */
abstract class LiveDataComposeTest<A : AppCompatActivity>(
    private val activityClass: Class<A>
) {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    // On failure, names any dialog/sheet that was covering the screen — the usual hidden cause of a
    // "no node found" failure. See ScreenStateReporter.
    @get:Rule
    val screenStateReporter = ScreenStateReporter()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    protected val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    protected val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    protected lateinit var scenario: ActivityScenario<out Activity>

    /**
     * Override to set global state (Prefs, language) before the Activity launches — typically to
     * suppress onboarding/announcement dialogs that would cover the screen and hide its nodes. Runs
     * in [liveDataBaseSetUp] right before launch, so the first frame already sees the state.
     */
    protected open fun prepareDeviceState() {}

    /** Override to clear DataStore/DB state for a deterministic, repeatable run. */
    protected open suspend fun resetPersistentState() {}

    /**
     * Suppresses the app's known one-off onboarding / announcement / game-entry popups. The app
     * fires these from many screens (the Explore feed announcement, the article-page On-this-day
     * game dialog, reading-list prompts, tooltips, …) and each renders over a freshly launched
     * screen, covering its nodes and making them unreachable — the single most common cause of a
     * "no node found" flake. Runs before [prepareDeviceState] for EVERY live-data test, because no
     * live-data test ever wants a random interstitial. When a new dialog is discovered, add its flag
     * here once so no future test has to rediscover it.
     */
    protected open fun suppressKnownDialogs() {
        Prefs.isInitialOnboardingEnabled = false
        Prefs.isExploreFeedUpdatePromptShown = true
        Prefs.isHomeSwipeToExplorePromptShown = true
        Prefs.readingChallengeOnboardingShown = true
        Prefs.otdEntryDialogShown = true
        Prefs.isHybridSearchOnboardingShown = true
        Prefs.showReadingListSyncEnablePrompt = false
        Prefs.showOneTimeCustomizeToolbarTooltip = false
        Prefs.readingListShareTooltipShown = true
        Prefs.isYearInReviewEnabled = false
        Prefs.searchWidgetInstallPromptShown = true
    }

    /** Override to launch from a custom entry [Intent] (e.g. a deep link); default launches [activityClass]. */
    protected open fun launchIntent(): Intent? = null

    @Before
    fun liveDataBaseSetUp() {
        assumeTrue(
            "Live-data test requires a validated network connection",
            hasValidatedInternet()
        )
        Intents.init()
        disableAnimations()
        runBlocking { resetPersistentState() }
        // Order matters: gate + persistent reset + Prefs must all be in place BEFORE the first frame.
        suppressKnownDialogs()
        prepareDeviceState()
        scenario = launchIntent()?.let { ActivityScenario.launch(it) }
            ?: ActivityScenario.launch(activityClass)
    }

    @After
    fun liveDataBaseTearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        Intents.release()
    }

    /** Asserts an Activity of type [target] was started (e.g. navigation to an article page). */
    protected fun assertNavigatedTo(target: Class<out Activity>) {
        Intents.intended(hasComponent(target.name))
    }

    /** Prevents external Activities (share sheets, browsers, etc.) from actually launching. */
    protected fun stubExternalIntents() {
        Intents.intending(not(isInternal())).respondWith(ActivityResult(RESULT_OK, null))
    }

    /** Stubs the share chooser so the system share sheet does not leave the app. Call before sharing. */
    protected fun stubShareChooser() {
        Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(ActivityResult(RESULT_OK, null))
    }

    /** Asserts a share was fired: an ACTION_CHOOSER wrapping an ACTION_SEND. */
    protected fun assertShareChooserFired() {
        Intents.intended(
            allOf(
                hasAction(Intent.ACTION_CHOOSER),
                hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_SEND))
            )
        )
    }

    private fun hasValidatedInternet(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun disableAnimations() {
        listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale").forEach {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put global $it 0")
        }
    }
}
