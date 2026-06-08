package org.wikipedia.tests.explorefeed

import android.app.Activity.RESULT_OK
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.main.MainActivity
import org.wikipedia.page.PageActivity
import org.wikipedia.robots.feature.HomeFeedRobot
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsRepository

/**
 * End-to-end home feed test backed by **live network data, no mocking**.
 *
 * Architecture notes:
 * - Drives the real [MainActivity] → MainFragment → HomeFragment → HomeViewModel stack so the
 *   actual Retrofit/network path runs. We deliberately assert through the rendered UI and never
 *   read `viewModel.state.value` directly — that synchronous-read pattern is exactly what breaks
 *   under the v2 `StandardTestDispatcher`, where launched coroutines are queued rather than run.
 * - Uses the **v2** testing rule (`...junit4.v2.createAndroidComposeRule`). The feed's network
 *   coroutines run on real dispatchers, not the Compose test clock, so the v1→v2 dispatcher change
 *   does not affect them; synchronization is done via [HomeFeedRobot]'s real-time `waitUntil` polling.
 * - Asserts structural invariants (the feed renders at least one item) rather than specific article
 *   text, because live content changes day to day.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeFeedLiveDataTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val robot = HomeFeedRobot(composeTestRule)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    init {
        // Must be set before the activity launches (rule evaluation), so HomeFragment.onCreate
        // does not launch onboarding / explore-feed-update prompts over the feed.
        Prefs.isInitialOnboardingEnabled = false
        Prefs.isExploreFeedUpdatePromptShown = true
        Prefs.isHomeSwipeToExplorePromptShown = true
        // Suppress the announcement / onboarding dialogs that otherwise cover the feed
        // (reading challenge, On This Day entry dialog, sync prompt, one-time tooltips),
        // mirroring BaseTest.DataInjector.
        Prefs.readingChallengeOnboardingShown = true
        Prefs.otdEntryDialogShown = true
        Prefs.showReadingListSyncEnablePrompt = false
        Prefs.showOneTimeCustomizeToolbarTooltip = false
        Prefs.readingListShareTooltipShown = true
        Prefs.isYearInReviewEnabled = false
        // Start on the Community tab deterministically, regardless of prior runs.
        Prefs.homePreferenceSelection = HomePreferenceType.COMMUNITY

        // Enable English + Spanish so the language switch test has a target, and start on English.
        WikipediaApp.instance.languageState.setAppLanguageCodes(listOf("en", "es"))
        Prefs.homeLanguageCode = "en"
    }

    @Before
    fun setUp() {
        assumeTrue("Live-data feed test requires a network connection", hasActiveInternet())
        // Clear any cards/modules hidden by a previous run so the feed is in a known state.
        runBlocking {
            SettingsRepository.hiddenCards.first().forEach { SettingsRepository.removeHiddenCard(it) }
            SettingsRepository.hiddenModules.first().forEach { SettingsRepository.removeHiddenModule(it) }
        }
        Intents.init()
        listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale").forEach {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put global $it 0")
        }
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    /**
     * Queries [ConnectivityManager] directly instead of [WikipediaApp.isOnline]. The app's
     * connectivity flag is cached for 60s and can be a stale `false` on a freshly booted
     * emulator (e.g. GitHub Actions), where the process starts before the network is up —
     * which would wrongly skip the whole suite. This reads the real state at test time.
     */
    private fun hasActiveInternet(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    @Test
    fun communityFeed_loadsLiveCards() {
        robot
            .waitForCommunityFeedLoaded()
            .assertCommunityFeedHasCards()
    }

    @Test
    fun forYouTab_loadsLiveModules() {
        robot
            .waitForCommunityFeedLoaded()
            .switchToForYouTab()
            .assertForYouFeedHasModules()
    }

    @Test
    fun tappingFeaturedArticleCard_opensPage() {
        robot
            .waitForCommunityFeedLoaded()
            .clickFeaturedArticleCard()

        Intents.intended(hasComponent(PageActivity::class.java.name))
    }

    @Test
    fun scrollToTopReadArticle_andTap_opensPage() {
        robot
            .waitForCommunityFeedLoaded()
            .scrollToAndClickTopReadArticle()

        Intents.intended(hasComponent(PageActivity::class.java.name))
    }

    @Test
    fun cardOverflow_save_showsConfirmationSnackbar() {
        robot
            .waitForCommunityFeedLoaded()
            .openTopReadArticleOverflow()
            .tapOverflowMenuItem(context.getString(R.string.feed_card_add_to_default_list))

        // Saving resolves the title over the network before confirming, so poll for the snackbar.
        val snackbar = device.wait(
            Until.hasObject(By.res(context.packageName, SNACKBAR_TEXT_ID)),
            SNACKBAR_TIMEOUT_MS
        )
        assertNotNull("Expected a confirmation snackbar after saving the article", snackbar)
    }

    @Test
    fun cardOverflow_share_firesShareChooser() {
        // Stub the chooser so the system share sheet does not actually launch and leave the app.
        Intents.intending(hasAction(Intent.ACTION_CHOOSER))
            .respondWith(ActivityResult(RESULT_OK, null))

        robot
            .waitForCommunityFeedLoaded()
            .openTopReadArticleOverflow()
            .tapOverflowMenuItem(context.getString(R.string.menu_page_share))

        Intents.intended(
            allOf(
                hasAction(Intent.ACTION_CHOOSER),
                hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_SEND))
            )
        )
    }

    @Test
    fun cardOverflow_hideCard_removesCardFromFeed() {
        robot
            .waitForCommunityFeedLoaded()
            .openTopReadModuleOverflow()
            .hideTopReadCard()
            .assertTopReadCardHidden()
    }

    @Test
    fun switchLanguage_reloadsFeedForNewWiki() {
        robot
            .waitForCommunityFeedLoaded()
            .selectLanguage("es")
            .assertLanguageIndicatorShows("es")
            .waitForCommunityFeedLoaded()

        assertEquals("es", Prefs.homeLanguageCode)
    }

    private companion object {
        const val SNACKBAR_TEXT_ID = "snackbar_text"
        const val SNACKBAR_TIMEOUT_MS = 10_000L
    }
}
