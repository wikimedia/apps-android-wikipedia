package org.wikipedia.main

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotFocused
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.auth.AccountUtil

@LargeTest
@RunWith(AndroidJUnit4::class)
class SEScreensTests {
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
    fun sEScreensTest() {
        // Skip the initial onboarding screens...
        onView(allOf(ViewMatchers.withId(R.id.fragment_onboarding_skip_button), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        // Dismiss the Feed customization onboarding card in the feed
        onView(
            allOf(ViewMatchers.withId(R.id.view_announcement_action_negative),
            withText("Got it"), isDisplayed())
        )
            .perform(click())

        TestUtil.delay(1)

        // Go to `Edits` tab
        onView(allOf(withId(R.id.nav_tab_edits), withContentDescription("Edits"),
          childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 3), isDisplayed())).perform(click())

        TestUtil.delay(2)

        if (AccountUtil.isLoggedIn) {
            // Click through `Edits` screen stats onboarding - also confirming tooltip display
            for (i in 1 until 5) {
                onView(allOf(withId(R.id.buttonView), withText("Got it"),
                    childAtPosition(childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1), 0), isDisplayed()))
                    .perform(click())
                TestUtil.delay(2)
            }
        }

        // User contributions screen tests. Enter contributions screen
        onView(allOf(withId(R.id.userStatsArrow), withContentDescription("My contributions"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on filter button to view filter options
        onView(allOf(withId(R.id.filter_by_button), withContentDescription("Filter by"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Assert the presence of all filters
        onView(allOf(withId(R.id.item_title), withText("Wikimedia Commons"),
            ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.recycler_view))), isDisplayed()))
            .check(ViewAssertions.matches(withText("Wikimedia Commons")))

        onView(allOf(withId(R.id.item_title), withText("Wikidata"),
            ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.recycler_view))), isDisplayed()))
        .check(ViewAssertions.matches(withText("Wikidata")))

        onView(allOf(withId(R.id.item_title), withText("Article"),
            ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.recycler_view))), isDisplayed()))
            .check(ViewAssertions.matches(withText("Article")))

        onView(allOf(withId(R.id.item_title), withText("Talk"),
            ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.recycler_view))), isDisplayed()))
            .check(ViewAssertions.matches(withText("Talk")))

        onView(allOf(withId(R.id.item_title), withText("User talk"),
            ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.recycler_view))), isDisplayed()))
            .check(ViewAssertions.matches(withText("User talk")))

        onView(allOf(withId(R.id.item_title), withText("User"),
            ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.recycler_view))), isDisplayed()))
            .check(ViewAssertions.matches(withText("User")))

        TestUtil.delay(2)

        // Navigate back to se tasks screen
        onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        // Click on `Add description` task
        onView(allOf(withId(R.id.tasksRecyclerView), childAtPosition(withId(R.id.tasksContainer), 2)))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        TestUtil.delay(2)

        // Assert the presence of correct action button
        onView(allOf(withId(R.id.addContributionButton), withText("Add description"),
            ViewMatchers.withParent(allOf(withId(R.id.bottomButtonContainer))), isDisplayed()))
        .check(ViewAssertions.matches(isDisplayed()))

        TestUtil.delay(2)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.tasksRecyclerView), childAtPosition(withId(R.id.tasksContainer), 2)))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        TestUtil.delay(2)

        // Assert the presence of correct action button
        onView(allOf(withId(R.id.addContributionButton), withText("Add caption"),
            ViewMatchers.withParent(allOf(withId(R.id.bottomButtonContainer))), isDisplayed()))
        .check(ViewAssertions.matches(isDisplayed()))

        TestUtil.delay(2)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.tasksRecyclerView), childAtPosition(withId(R.id.tasksContainer), 2)))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))

        TestUtil.delay(2)

        onView(allOf(withId(R.id.onboarding_done_button), withText("Get started"),
            /*    childAtPosition(
                    childAtPosition(
                        withClassName(Matchers.`is`("android.widget.LinearLayout")),
                        1
                    ),
                    0
                ),*/
                isDisplayed()
            )
        ).perform(click())
        TestUtil.delay(2)

        // Assert the presence of correct action button
        onView(allOf(withText("Add tag"), ViewMatchers.withParent(allOf(withId(R.id.tagsChipGroup))), isDisplayed()))
            .check(ViewAssertions.matches(isDisplayed()))

        TestUtil.delay(2)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.learnMoreButton), withText("Learn more"),
            childAtPosition(allOf(withId(R.id.learnMoreCard)), 2), isNotFocused()))
            .perform(scrollTo())

        TestUtil.delay(2)

        onView(allOf(withText("What is Suggested edits?"), ViewMatchers.withParent(allOf(withId(R.id.learnMoreCard))), isDisplayed()))
        .check(ViewAssertions.matches(withText("What is Suggested edits?")))

        TestUtil.delay(2)
    }
}
