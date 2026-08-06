package org.wikipedia.robots.feature

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.uiautomator.UiDevice
import org.wikipedia.R
import org.wikipedia.base.livedata.ComposeRobot
import org.wikipedia.feed.CommunityModuleTestTags
import org.wikipedia.feed.HomeScreenTestTags
import org.wikipedia.feed.featured.FeaturedArticleModuleTestTags

/**
 * Drives the home feed against live network data. All synchronization lives in [ComposeRobot];
 * this class only expresses feature intent.
 *
 * Anchoring note: interaction tests target the **Featured Article** card. With live data it is the
 * one card guaranteed to be present (always first in the Community feed), so tests don't flake on
 * days when optional modules like "Top read" are absent from the API response.
 */
class HomeFeedRobot(
    composeTestRule: ComposeTestRule,
    device: UiDevice,
    context: Context
) : ComposeRobot(composeTestRule, device, context) {

    fun waitForCommunityFeedLoaded() = apply {
        awaitTag(HomeScreenTestTags.COMMUNITY_FEED_LIST)
    }

    fun assertCommunityFeedHasCards() = apply {
        assertHasChildren(HomeScreenTestTags.COMMUNITY_FEED_LIST)
    }

    fun clickFeaturedArticleCard() = apply {
        scrollToAndClick(HomeScreenTestTags.COMMUNITY_FEED_LIST, FeaturedArticleModuleTestTags.CARD)
    }

    fun saveFeaturedArticle() = apply {
        scrollToAndClick(HomeScreenTestTags.COMMUNITY_FEED_LIST, FeaturedArticleModuleTestTags.BOOKMARK_BUTTON)
    }

    fun assertSaveConfirmation() = apply {
        assertSnackbarShown("Expected a confirmation snackbar after saving the article")
    }

    fun shareFeaturedArticle() = apply {
        scrollToAndClick(HomeScreenTestTags.COMMUNITY_FEED_LIST, FeaturedArticleModuleTestTags.SHARE_BUTTON)
    }

    fun openFeaturedModuleOverflow() = apply {
        scrollToAndClick(
            HomeScreenTestTags.COMMUNITY_FEED_LIST,
            CommunityModuleTestTags.overflowButton(R.string.view_featured_article_card_title)
        )
    }

    fun hideFeaturedCard() = apply {
        clickTag(CommunityModuleTestTags.hideCardItem(R.string.view_featured_article_card_title))
    }

    fun assertFeaturedCardHidden() = apply {
        awaitTagGone(FeaturedArticleModuleTestTags.CARD)
    }

    /**
     * Contract-driven parity check: every module the backend served today (from FeedContract) must
     * be reachable in the rendered feed. A served-but-unreachable module fails with a diagnostic
     * naming it — that's the "data had it, UI didn't render it" regression. Modules the backend did
     * not serve are absent from [servedModules], so a quiet day never produces a false failure.
     */
    fun assertServedModulesRendered(servedModules: Map<String, String>) = apply {
        servedModules.forEach { (tag, name) ->
            assertReachableByScroll(
                HomeScreenTestTags.COMMUNITY_FEED_LIST,
                tag,
                "Backend served the '$name' module today, but it never rendered in the Community " +
                    "feed — regression in the card-to-module mapping, the module composable, or its test tag."
            )
        }
    }

    fun switchToForYouTab() = apply {
        clickTag(HomeScreenTestTags.TAB_FOR_YOU)
        awaitTag(HomeScreenTestTags.FOR_YOU_FEED_LIST)
    }

    fun assertForYouFeedHasModules() = apply {
        assertHasChildren(HomeScreenTestTags.FOR_YOU_FEED_LIST)
    }

    fun selectLanguage(languageCode: String) = apply {
        clickTag(HomeScreenTestTags.LANGUAGE_MENU_BUTTON)
        clickTag(HomeScreenTestTags.languageItem(languageCode))
    }

    fun assertLanguageIndicatorShows(languageCode: String) = apply {
        awaitText(languageCode.uppercase())
    }
}
