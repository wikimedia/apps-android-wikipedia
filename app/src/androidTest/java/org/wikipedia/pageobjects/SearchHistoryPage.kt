package org.wikipedia.pageobjects


import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.*
import org.wikipedia.R

class SearchHistoryPage : BasePage() {

    //onboarding page elements
    private val searchCard = withId(R.id.search_card)
    private val searchTextInput = withId(R.id.search_src_text)
    private val resultsItem = withId(R.id.page_list_item_title)
    private val findingsList = withId(R.id.search_results_list)
    private val noResultsLable = withId(R.id.results_text)

//    private val ArticleHeader = "//h1[@data-id='0']/span[@class='mw-page-title-main']"
   // private val articleTitle = withId(R.id.page_toc_item_text)



    fun tapOnSearchBar() {
        onView(searchCard).perform(click())
        return
    }

    fun typeTextSearch(text: String) {
        onView(searchTextInput).perform(typeText(text))
    }

    fun tapOnFoundExactResultItem(text: String) {
        onView(findingsList)
            .perform(
                actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(text)),
                    click()
                )
            );
    }

    fun verifyNoResultFound(): String {
        return invokeText(onView(noResultsLable))
    }

//    fun verifyArticleTitle(): String {
//       onWebView()
//            .withElement(findElement(ArticleHeader,"Bitcoin"))
//    }
}
