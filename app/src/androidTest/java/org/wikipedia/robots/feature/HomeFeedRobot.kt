package org.wikipedia.robots.feature

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertTrue
import org.wikipedia.R
import org.wikipedia.feed.CommunityModuleTestTags
import org.wikipedia.feed.HomeScreenTestTags
import org.wikipedia.feed.featured.FeaturedArticleModuleTestTags
import org.wikipedia.feed.topread.TopReadModuleTestTags

/**
 * Drives the home feed against live network data.
 *
 * Synchronization note: the feed loads through [org.wikipedia.feed.HomeViewModel] on
 * `viewModelScope` + Retrofit, which run on real dispatchers — not on the Compose test
 * clock. So we never gate on `waitForIdle()` (the loading indicator may keep the UI from
 * ever becoming idle); instead we poll for the rendered feed list with [waitUntil], which
 * waits against real elapsed time. This behaves identically under the v1 and v2 testing APIs.
 */
class HomeFeedRobot(private val composeTestRule: ComposeTestRule) {

    fun waitForCommunityFeedLoaded(timeoutMillis: Long = FEED_LOAD_TIMEOUT_MS) = apply {
        awaitTag(HomeScreenTestTags.COMMUNITY_FEED_LIST, timeoutMillis)
    }

    fun assertCommunityFeedHasCards() = apply {
        assertListHasChildren(HomeScreenTestTags.COMMUNITY_FEED_LIST)
    }

    fun clickFeaturedArticleCard() = apply {
        composeTestRule.onNodeWithTag(FeaturedArticleModuleTestTags.CARD).performClick()
    }

    /**
     * Mirrors a real user: scroll the Community feed down to a "Top read" article row and tap it.
     * Top read sits below the featured article, so reaching it exercises the scroll path.
     */
    fun scrollToAndClickTopReadArticle(index: Int = 0) = apply {
        val itemTag = TopReadModuleTestTags.item(index)
        composeTestRule.onNodeWithTag(HomeScreenTestTags.COMMUNITY_FEED_LIST)
            .performScrollToNode(hasTestTag(itemTag))
        composeTestRule.onNodeWithTag(itemTag).performClick()
    }

    fun openTopReadArticleOverflow(index: Int = 0) = apply {
        val overflowTag = TopReadModuleTestTags.overflowButton(index)
        composeTestRule.onNodeWithTag(HomeScreenTestTags.COMMUNITY_FEED_LIST)
            .performScrollToNode(hasTestTag(overflowTag))
        composeTestRule.onNodeWithTag(overflowTag).performClick()
    }

    fun tapOverflowMenuItem(label: String) = apply {
        composeTestRule.onNodeWithText(label).performClick()
    }

    fun openTopReadModuleOverflow() = apply {
        val overflowTag = CommunityModuleTestTags.overflowButton(R.string.view_top_read_card_title)
        composeTestRule.onNodeWithTag(HomeScreenTestTags.COMMUNITY_FEED_LIST)
            .performScrollToNode(hasTestTag(overflowTag))
        composeTestRule.onNodeWithTag(overflowTag).performClick()
    }

    fun hideTopReadCard() = apply {
        composeTestRule.onNodeWithTag(
            CommunityModuleTestTags.hideCardItem(R.string.view_top_read_card_title)
        ).performClick()
    }

    fun assertTopReadCardHidden(timeoutMillis: Long = FEED_LOAD_TIMEOUT_MS) = apply {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithTag(TopReadModuleTestTags.item(0), useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    fun switchToForYouTab(timeoutMillis: Long = FEED_LOAD_TIMEOUT_MS) = apply {
        composeTestRule.onNodeWithTag(HomeScreenTestTags.TAB_FOR_YOU).performClick()
        awaitTag(HomeScreenTestTags.FOR_YOU_FEED_LIST, timeoutMillis)
    }

    fun assertForYouFeedHasModules() = apply {
        assertListHasChildren(HomeScreenTestTags.FOR_YOU_FEED_LIST)
    }

    fun selectLanguage(languageCode: String) = apply {
        composeTestRule.onNodeWithTag(HomeScreenTestTags.LANGUAGE_MENU_BUTTON).performClick()
        composeTestRule.onNodeWithTag(HomeScreenTestTags.languageItem(languageCode)).performClick()
    }

    fun assertLanguageIndicatorShows(languageCode: String, timeoutMillis: Long = FEED_LOAD_TIMEOUT_MS) = apply {
        val label = languageCode.uppercase()
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitTag(tag: String, timeoutMillis: Long) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertListHasChildren(tag: String) {
        val childCount = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode().children.size
        assertTrue("Expected feed list '$tag' to render at least one item", childCount > 0)
    }

    companion object {
        // Generous: real network on CI emulators can be slow. We poll, so this is only an upper bound.
        private const val FEED_LOAD_TIMEOUT_MS = 20_000L
    }
}
