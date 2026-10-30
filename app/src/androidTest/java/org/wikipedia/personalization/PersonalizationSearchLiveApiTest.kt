package org.wikipedia.personalization

import org.junit.Test
import org.wikipedia.base.BaseTest
import org.wikipedia.dataclient.okhttp.TestStubInterceptor
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.robots.PersonalizationRobot
import org.wikipedia.robots.SearchRobot
import org.wikipedia.settings.Prefs

/**
 * Searches against the real API and checks the chosen article lands on the interest screen.
 * The expected title is read from the search results rather than hardcoded, so the test never
 * assumes what the live API returns for a query.
 */
class PersonalizationSearchLiveApiTest : BaseTest<PersonalizationActivity>(
    activityClass = PersonalizationActivity::class.java,
    beforeActivityLaunch = {
        Prefs.shouldMatchSystemTheme = false
        // Never inherit a stub left behind by a fixture test that failed partway.
        TestStubInterceptor.CALLBACK = null
    }
) {
    private val personalizationRobot by lazy {
        PersonalizationRobot(composeTestRule, targetContext, LIVE_TIMEOUT_MILLIS)
    }
    private val searchRobot by lazy {
        SearchRobot(composeTestRule, LIVE_TIMEOUT_MILLIS)
    }

    @Test
    fun aSecondArticleFromSearchIsAddedWithoutDeselectingTheFirst() {
        personalizationRobot
            .assertCuriosityScreenIsDisplayed()
            .tapNext()
            .assertInterestsScreenIsDisplayed()
            .tapSearchBar()

        searchRobot
            .typeQuery(SEARCH_QUERY)
            .waitForResults()

        val firstArticle = searchRobot.firstResultTitle()
        searchRobot.tapFirstResult()

        personalizationRobot
            .assertArticleIsDisplayed(firstArticle)
            .assertSelectedCountIsDisplayed(1)
            .tapSearchBar()

        searchRobot
            .typeQuery(SEARCH_QUERY_2)
            .waitForResults()

        val secondArticle = searchRobot.firstResultTitle()
        searchRobot.tapFirstResult()

        personalizationRobot
            .assertArticleIsDisplayed(secondArticle)
            .assertArticleIsDisplayed(firstArticle)
            .assertSelectedCountIsDisplayed(2) // proves the first article is still selected
    }

    private companion object {
        const val LIVE_TIMEOUT_MILLIS = 30_000L
        const val SEARCH_QUERY = "obama"
        const val SEARCH_QUERY_2 = "coffee"
    }
}
