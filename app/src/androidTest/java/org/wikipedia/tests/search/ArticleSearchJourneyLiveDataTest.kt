package org.wikipedia.tests.search

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.base.livedata.LiveDataComposeTest
import org.wikipedia.page.PageActivity
import org.wikipedia.robots.feature.ArticleRobot
import org.wikipedia.robots.feature.SearchResultsRobot
import org.wikipedia.search.SearchActivity

/**
 * End-to-end **cross-Activity** journey on live data: launch search → open the first result →
 * land on the article page and confirm it actually rendered. This is the test the single-Activity
 * Compose rule could not write — it spans `SearchActivity` (Compose results) and `PageActivity`
 * (WebView article), proving the empty-rule + [androidx.test.core.app.ActivityScenario] base.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticleSearchJourneyLiveDataTest : LiveDataComposeTest<SearchActivity>(SearchActivity::class.java) {

    private val searchRobot by lazy { SearchResultsRobot(composeTestRule, device, context) }
    private val articleRobot by lazy { ArticleRobot(composeTestRule, device, context) }

    override fun prepareDeviceState() {
        // Known dialogs (incl. hybrid-search onboarding and the article-page OTD game dialog) are
        // suppressed centrally by the base suppressKnownDialogs(); only language pinning remains.
        WikipediaApp.instance.languageState.setAppLanguageCodes(listOf("en"))
    }

    // Enter directly with a query so results load deterministically without driving the keyboard.
    override fun launchIntent(): Intent =
        SearchActivity.newIntent(context, InvokeSource.NAV_MENU, QUERY)

    @Test
    fun searchResult_opensAndRendersArticle() {
        searchRobot
            .waitForResults()
            .openFirstResult()

        assertNavigatedTo(PageActivity::class.java)

        articleRobot.assertArticleRendered(QUERY)
    }

    companion object {
        // A stable, always-present article that is the top hit for its own title.
        private const val QUERY = "Albert Einstein"
    }
}
