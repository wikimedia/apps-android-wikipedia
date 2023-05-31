package org.wikipedia.main

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.auth.AccountUtil

@LargeTest
@RunWith(AndroidJUnit4::class)
class MiscellaneousSmokeTests {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)
    private lateinit var activity: MainActivity

    @Before
    fun setActivity() {
        mActivityTestRule.scenario.onActivity {
            activity = it
        }
    }

    @Test
    fun miscellaneousTest() {

        // Skip the initial onboarding screens...
        onView(Matchers.allOf(withId(R.id.fragment_onboarding_skip_button), isDisplayed()))
            .perform(ViewActions.click())

        TestUtil.delay(2)

        // Dismiss the Feed customization onboarding card in the feed
        onView(Matchers.allOf(withId(R.id.view_announcement_action_negative), withText("Got it"), isDisplayed()))
            .perform(ViewActions.click())

        TestUtil.delay(1)

        // Click on `More` menu
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Settings` option
        onView(Matchers.allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

        TestUtil.delay(2)

        if (AccountUtil.isLoggedIn) {
            // Turn off images in `Settings`
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>
                    (ViewMatchers.hasDescendant(withText(R.string.preference_title_show_images)), click()))

            TestUtil.delay(2)

            onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Assert that images arent shown anymore
            onView(Matchers.allOf(withId(R.id.articleImage), ViewMatchers.withParent(Matchers.allOf(withId(R.id.articleImageContainer),
                ViewMatchers.withParent(withId(R.id.view_wiki_article_card)))), isDisplayed())).check(ViewAssertions.doesNotExist())

            TestUtil.delay(2)

            // Click on `More` menu
            onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Click on `Settings` option
            onView(Matchers.allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Click to enter `About` screen
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>
                    (ViewMatchers.hasDescendant(withText(R.string.about_description)), click()))

            TestUtil.delay(2)

            onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Scroll to logOut option and click
            onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>
                (ViewMatchers.hasDescendant(withText(R.string.preference_title_logout)), click()))

            TestUtil.delay(2)

            onView(Matchers.allOf(withText("Log out"), isDisplayed())).perform(scrollTo(), click())

            TestUtil.delay(2)

            onView(allOf(withId(android.R.id.message), isDisplayed()))
                .check(ViewAssertions.matches(withText("This will log you out on all devices where you are currently logged in. Do you want to continue?")))
        }
    }
}
