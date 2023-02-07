package org.wikipedia.testspawel

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.main.MainActivity
import org.wikipedia.pageobjects.*
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchBarTestSuite {

    private val onboardingPage = OnboardingPage()
    private val navbarPage = NavbarPage()
    private val searchHistoryPage = SearchHistoryPage()
    private val explorePage = ExplorePage()
    private val articlePage = ArticlePage()

    @Before
    fun beforeTests() {
        onboardingPage.tapOnSkipButton()
    }

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)
    val searchedPhrase = "Bitcoin"

    @Test
    fun searchingFeatureTestFromSearchPage() {

        navbarPage.tapOnNavSearchBtn()
        searchHistoryPage.tapOnSearchBar()
        searchHistoryPage.typeTextSearch(searchedPhrase)
        searchHistoryPage.tapOnSearchResultItem(searchedPhrase)
        assertTrue("Article header `$searchedPhrase` is not displayed in article view.", articlePage.isArticleDisplayed(searchedPhrase))
    }

    @Test
    fun searchingFeatureTestFromExplorePage() {

        explorePage.tapOnSearchBar()
        searchHistoryPage.typeTextSearch(searchedPhrase)
        searchHistoryPage.tapOnSearchResultItem(searchedPhrase)
        assertTrue("Article header `$searchedPhrase` is not displayed in article view.", articlePage.isArticleDisplayed(searchedPhrase))
    }

    @Test
    fun searchingFeatureTestFromExplorePageNoResultsFound() {
        val typedPhrase = "axaxa22"
        val labelText = "No results"

        explorePage.tapOnSearchBar()
        searchHistoryPage.typeTextSearch(typedPhrase)
        assertEquals( labelText, searchHistoryPage.verifyNoResultFound());
    }
}

