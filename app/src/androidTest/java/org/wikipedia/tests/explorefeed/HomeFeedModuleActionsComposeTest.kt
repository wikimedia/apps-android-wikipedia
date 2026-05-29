package org.wikipedia.tests.explorefeed

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.base.BaseTest
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.CommunityModuleContainer
import org.wikipedia.feed.CommunityModuleTestTags
import org.wikipedia.feed.featured.FeaturedArticleModule
import org.wikipedia.feed.featured.FeaturedArticleModuleTestTags
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.image.FeaturedImageModule
import org.wikipedia.feed.image.FeaturedImageModuleTestTags
import org.wikipedia.feed.topread.TopRead
import org.wikipedia.feed.topread.TopReadModule
import org.wikipedia.feed.topread.TopReadModuleTestTags
import org.wikipedia.main.MainActivity
import org.wikipedia.theme.Theme

@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeFeedModuleActionsComposeTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java
) {

    private val wikiSite = WikiSite.forLanguageCode("en")

    @Test
    fun communityModuleOverflow_clickHideCard_invokesCallback() {
        var hideCardClicked = false

        setModuleContent {
            CommunityModuleContainer(
                wikiSite = wikiSite,
                titleResId = R.string.view_featured_article_card_title,
                subTitleResId = R.string.explore_feed_featured_article_subtitle,
                onHideCardClick = { hideCardClicked = true },
                onHideModuleClick = {}
            ) {
                Box {}
            }
        }

        val titleResId = R.string.view_featured_article_card_title
        composeTestRule.onNodeWithTag(CommunityModuleTestTags.overflowButton(titleResId)).performClick()
        composeTestRule.onNodeWithTag(CommunityModuleTestTags.hideCardItem(titleResId)).performClick()

        assertTrue(hideCardClicked)
    }

    @Test
    fun communityModuleOverflow_clickHideModule_invokesCallback() {
        var hideModuleClicked = false

        setModuleContent {
            CommunityModuleContainer(
                wikiSite = wikiSite,
                titleResId = R.string.view_featured_article_card_title,
                subTitleResId = R.string.explore_feed_featured_article_subtitle,
                onHideCardClick = {},
                onHideModuleClick = { hideModuleClicked = true }
            ) {
                Box {}
            }
        }

        val titleResId = R.string.view_featured_article_card_title
        composeTestRule.onNodeWithTag(CommunityModuleTestTags.overflowButton(titleResId)).performClick()
        composeTestRule.onNodeWithTag(CommunityModuleTestTags.hideModuleItem(titleResId)).performClick()

        assertTrue(hideModuleClicked)
    }

    @Test
    fun featuredArticle_clickCard_invokesOpenCallback() {
        var openClicked = false

        setModuleContent {
            FeaturedArticleModule(
                wikiSite = wikiSite,
                article = sampleSummary("Earth"),
                onPageClick = { openClicked = true }
            )
        }

        composeTestRule.onNodeWithTag(FeaturedArticleModuleTestTags.CARD).performClick()

        assertTrue(openClicked)
    }

    @Test
    fun featuredArticle_clickBookmarkAndShare_invokesCallbacks() {
        var bookmarkClicked = false
        var shareClicked = false

        setModuleContent {
            FeaturedArticleModule(
                wikiSite = wikiSite,
                article = sampleSummary("Earth"),
                onBookmarkClick = { bookmarkClicked = true },
                onShareClick = { shareClicked = true }
            )
        }

        composeTestRule.onNodeWithTag(FeaturedArticleModuleTestTags.BOOKMARK_BUTTON).performClick()
        composeTestRule.onNodeWithTag(FeaturedArticleModuleTestTags.SHARE_BUTTON).performClick()

        assertTrue(bookmarkClicked)
        assertTrue(shareClicked)
    }

    @Test
    fun featuredImage_clickCardDownloadShare_invokesCallbacks() {
        var cardClicked = false
        var downloadClicked = false
        var shareClicked = false

        setModuleContent {
            FeaturedImageModule(
                wikiSite = wikiSite,
                card = FeaturedImageCard(FeaturedImage("File:Earth.jpg"), 0, wikiSite),
                onClick = { cardClicked = true },
                onDownloadClick = { downloadClicked = true },
                onShareClick = { shareClicked = true }
            )
        }

        composeTestRule.onNodeWithTag(FeaturedImageModuleTestTags.CARD).performClick()
        composeTestRule.onNodeWithTag(FeaturedImageModuleTestTags.DOWNLOAD_BUTTON).performClick()
        composeTestRule.onNodeWithTag(FeaturedImageModuleTestTags.SHARE_BUTTON).performClick()

        assertTrue(cardClicked)
        assertTrue(downloadClicked)
        assertTrue(shareClicked)
    }

    @Test
    fun topRead_clickItemOverflowFooter_invokesCallbacks() {
        var openedTitle: String? = null
        var overflowIndex = -1
        var footerClicked = false

        setModuleContent {
            TopReadModule(
                wikiSite = wikiSite,
                topRead = TopRead(articles = listOf(sampleSummary("Earth"), sampleSummary("Moon"))),
                pageOverflowContent = {},
                onHideModuleClick = {},
                onPageClick = { openedTitle = it.displayTitle },
                onPageOverflowClick = { _, index -> overflowIndex = index },
                onFooterClick = { footerClicked = true }
            )
        }

        composeTestRule.onNodeWithTag(TopReadModuleTestTags.item(0)).performClick()
        composeTestRule.onNodeWithTag(TopReadModuleTestTags.overflowButton(1)).performClick()
        composeTestRule.onNodeWithTag(TopReadModuleTestTags.FOOTER_BUTTON).performClick()

        assertEquals("Earth", openedTitle)
        assertEquals(1, overflowIndex)
        assertTrue(footerClicked)
    }

    private fun setModuleContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent {
            BaseTheme(currentTheme = Theme.LIGHT) {
                content()
            }
        }
    }

    private fun sampleSummary(title: String): PageSummary {
        return PageSummary(
            displayTitle = title,
            prefixTitle = title,
            description = "Sample description",
            extract = "Sample extract",
            thumbnail = null,
            lang = "en"
        )
    }
}
