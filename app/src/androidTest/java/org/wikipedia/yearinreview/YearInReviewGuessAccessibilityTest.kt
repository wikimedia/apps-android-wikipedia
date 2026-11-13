package org.wikipedia.yearinreview

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.rive.Result
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.presentation.YearInReviewPage
import org.wikipedia.yearinreview.presentation.YearInReviewRiveContentMapper
import org.wikipedia.yearinreview.presentation.YearInReviewRiveFonts
import org.wikipedia.yearinreview.presentation.YearInReviewRiveSlide
import org.wikipedia.yearinreview.presentation.initializeYearInReviewRive
import org.wikipedia.yearinreview.presentation.rememberYearInReviewRiveFiles
import org.wikipedia.yearinreview.presentation.rememberYearInReviewRiveFonts
import org.wikipedia.yearinreview.presentation.rememberYearInReviewRiveWorker

// Checks that the screen reader actions drive the guess artboards the way a tap does, which breaks if the Rive file renames its trigger
@RunWith(AndroidJUnit4::class)
class YearInReviewGuessAccessibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun choosingAnArticleRevealsTheLongestRead() {
        val minutesSpent = 35
        assertActionReveals(
            page = YearInReviewPage.LongestReadGuess(article = pageTitle("Moon"), fakeArticle = pageTitle("Sourdough"), minutesSpent = minutesSpent),
            guessHeadline = resources.getString(R.string.yir_most_time_article_headline),
            actionLabel = resources.getString(R.string.yir_most_time_article_choose_action, "Sourdough"),
            revealHeadline = resources.getQuantityString(R.plurals.yir_most_time_article_supporting, minutesSpent, minutesSpent, YearInReviewConfig.YEAR)
        )
    }

    @Test
    fun choosingARabbitHoleRevealsTheDeepestOne() {
        val numLinks = 23
        assertActionReveals(
            page = YearInReviewPage.RabbitHoleGuess(
                articleStart = pageTitle("Moon"),
                articleEnd = pageTitle("Apollo 11"),
                fakeArticleStart = pageTitle("Sourdough"),
                fakeArticleEnd = pageTitle("Yeast"),
                numLinks = numLinks
            ),
            guessHeadline = resources.getString(R.string.yir_rabbit_hole_headline),
            actionLabel = resources.getString(R.string.yir_rabbit_hole_choose_action, "Moon", "Apollo 11"),
            revealHeadline = resources.getQuantityString(R.plurals.yir_rabbit_hole_depth_headline, numLinks, numLinks)
        )
    }

    private fun assertActionReveals(page: YearInReviewPage, guessHeadline: String, actionLabel: String, revealHeadline: String) {
        val riveErrors = mutableListOf<Throwable>()
        // Rive asks for a frame on every frame while it plays, so Compose would never be idle if its clock advanced on its own
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val context = LocalContext.current
            remember { initializeYearInReviewRive(context) }
            val localResources = LocalResources.current
            val content = remember { YearInReviewRiveContentMapper.map(page, localResources)!! }
            val riveWorker = rememberYearInReviewRiveWorker { riveErrors += it }
            val riveFonts = rememberYearInReviewRiveFonts(riveWorker, YearInReviewRiveFonts)
            val riveFiles = rememberYearInReviewRiveFiles(riveWorker, listOf(content.spec.resourceId), riveFonts)
            YearInReviewRiveSlide(
                riveFileResult = riveFiles[content.spec.resourceId] ?: Result.Loading,
                content = content,
                playing = true,
                onRiveError = { riveErrors += it }
            )
        }

        val guessSlide = hasContentDescription(guessHeadline, substring = true)
        waitUntilShown(guessSlide)
        @OptIn(ExperimentalTestApi::class)
        composeTestRule.onNode(guessSlide).performCustomAccessibilityActionWithLabel(actionLabel)

        waitUntilShown(hasContentDescription(revealHeadline, substring = true))
        assertEquals(emptyList<Throwable>(), riveErrors)
    }

    private fun waitUntilShown(matcher: SemanticsMatcher) {
        composeTestRule.waitUntil(TIMEOUT_MILLIS) {
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun pageTitle(title: String) = PageTitle(text = title, wiki = WikiSite.preview(), thumbUrl = null, description = "", displayText = title)

    companion object {
        // Covers loading the Rive file and the guess artboard's exit animation
        private const val TIMEOUT_MILLIS = 20_000L
    }
}
