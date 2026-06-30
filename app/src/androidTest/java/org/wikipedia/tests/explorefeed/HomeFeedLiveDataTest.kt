package org.wikipedia.tests.explorefeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.WikipediaApp
import org.wikipedia.base.livedata.LiveDataComposeTest
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.main.MainActivity
import org.wikipedia.page.PageActivity
import org.wikipedia.robots.feature.HomeFeedRobot
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsRepository

/**
 * End-to-end home feed test backed by **live network data, no mocking**, built on
 * [LiveDataComposeTest]. The base class owns the v2 live rule, connectivity gating, animation
 * disabling, Intents lifecycle, and reusable navigation/share/snackbar assertions; this class only
 * declares feature-specific state setup and behaviour.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeFeedLiveDataTest : LiveDataComposeTest<MainActivity>(MainActivity::class.java) {

    private val robot by lazy { HomeFeedRobot(composeTestRule, device, context) }

    override fun prepareDeviceState() {
        // Known onboarding/announcement/game dialogs are suppressed centrally by the base
        // suppressKnownDialogs(); only feature-specific deterministic state remains here.
        // Start on the Community tab deterministically, regardless of prior runs.
        Prefs.homePreferenceSelection = HomePreferenceType.COMMUNITY
        // Enable English + Spanish so the language switch test has a target, and start on English.
        WikipediaApp.instance.languageState.setAppLanguageCodes(listOf("en", "es"))
        Prefs.homeLanguageCode = "en"
    }

    override suspend fun resetPersistentState() {
        // Clear any cards/modules hidden by a previous run so the feed is in a known state.
        SettingsRepository.hiddenCards.first().forEach { SettingsRepository.removeHiddenCard(it) }
        SettingsRepository.hiddenModules.first().forEach { SettingsRepository.removeHiddenModule(it) }
    }

    @Test
    fun communityFeed_loadsLiveCards() {
        robot
            .waitForCommunityFeedLoaded()
            .assertCommunityFeedHasCards()
    }

    @Test
    fun communityFeed_rendersEveryModuleTheBackendServed() {
        val servedModules = FeedContract(languageCode = "en").servedCommunityModules()
        robot
            .waitForCommunityFeedLoaded()
            .assertServedModulesRendered(servedModules)
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

        assertNavigatedTo(PageActivity::class.java)
    }

    @Test
    fun saveFeaturedArticle_showsConfirmationSnackbar() {
        robot
            .waitForCommunityFeedLoaded()
            .saveFeaturedArticle()
            .assertSaveConfirmation()
    }

    @Test
    fun shareFeaturedArticle_firesShareChooser() {
        stubShareChooser()

        robot
            .waitForCommunityFeedLoaded()
            .shareFeaturedArticle()

        assertShareChooserFired()
    }

    @Test
    fun hideFeaturedCard_removesCardFromFeed() {
        robot
            .waitForCommunityFeedLoaded()
            .openFeaturedModuleOverflow()
            .hideFeaturedCard()
            .assertFeaturedCardHidden()
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
}
