package org.wikipedia.search

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.main.MainActivity
import org.wikipedia.pageobjects.NavbarPage

import org.wikipedia.pageobjects.OnboardingPage
import org.wikipedia.pageobjects.SearchHistoryPage
import org.wikipedia.pageobjects.ExplorePage
@LargeTest
@RunWith(AndroidJUnit4::class)
class InitializeSearchingAndAbort {

    private val onboardingPage = OnboardingPage()
    private val navbarPage= NavbarPage()
    private val searchHistoryPage= SearchHistoryPage()
    private val explorePage= ExplorePage()


    @Before
    fun beforeTests() {
        onboardingPage.tapOnSkipButton()
    }

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)
    val text ="Bitcoin"

    @Test
    fun searchingFeatureTestFromSearchPage() {

        explorePage.isMainPageVisible()
        navbarPage.tapOnNavSearchButton()
        searchHistoryPage.tapOnSearchBar()
        searchHistoryPage.typeTextSearch(text)
        Thread.sleep(2000)//wait to be added- line to delete
        searchHistoryPage.tapOnFoundExactResultItem(text)
        //Assertion To Be added
//        assertEquals("Bitcoin", searchHistoryPage.verifyArticleTitle());
    }

    @Test
    fun searchingFeatureTestFromExplorePage() {

        explorePage.isMainPageVisible()
        explorePage.tapOnSearchbar()
        searchHistoryPage.typeTextSearch(text)
        Thread.sleep(2000)//wait to be added- line to delete
        searchHistoryPage.tapOnFoundExactResultItem(text)
        //Assertion To Be added
    }

    @Test
    fun searchingFeatureTestFromExplorePageNoResultsFound() {
        val text ="axaxa22"
        val labelText="No results"

        explorePage.isMainPageVisible()//-> to dell
        explorePage.tapOnSearchbar()
        searchHistoryPage.typeTextSearch(text)
        Thread.sleep(2000)//wait to be added- line to delete
        assertEquals( labelText, searchHistoryPage.verifyNoResultFound());
    }
}

