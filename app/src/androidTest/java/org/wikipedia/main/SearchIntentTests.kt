package org.wikipedia.main

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.search.SearchActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchIntentTests {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule<SearchActivity>(SearchActivity.newIntent(ApplicationProvider.getApplicationContext(),
            Constants.InvokeSource.INTENT_SHARE, "barack obama"))

    @Test
    fun searchActivityTest() {

        TestUtil.delay(5)

        onView(allOf(withId(R.id.page_list_item_title), withText("Barack Obama"), isDisplayed()))
                .check(matches(withText("Barack Obama")))

        TestUtil.delay(5)
    }
}
