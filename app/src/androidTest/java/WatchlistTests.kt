import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.auth.AccountUtil
import org.wikipedia.main.MainActivity
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class WatchlistTests {

    /* Make sure that your watchlist is empty before starting the test */

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
    fun watchlistTest() {

        IdlingPolicies.setMasterPolicyTimeout(20, TimeUnit.SECONDS)

        // Skip the initial onboarding screens...
        onView(allOf(withId(R.id.fragment_onboarding_skip_button), isDisplayed())).perform(click())

        TestUtil.delay(1)

        // Dismiss the Feed customization onboarding card in the feed
        onView(allOf(withId(R.id.view_announcement_action_negative), withText("Got it"), isDisplayed()))
            .perform(click())

        TestUtil.delay(1)

        if (AccountUtil.isLoggedIn) {
            // Click on `More` menu
            onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed()))
                .perform(click())

            TestUtil.delay(2)

            // Click on `Watchlist` option
            onView(allOf(withId(R.id.main_drawer_watchlist_container), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Assert that the empty container for watchlists is shown
            onView(allOf(withId(R.id.watchlistEmptyContainer), isDisplayed()))
                .check(ViewAssertions.matches(isDisplayed()))

            TestUtil.delay(2)

            // Return to explore tab
            onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
                .perform(click())

            TestUtil.delay(2)

            // Click on featured article card
            onView(allOf(withId(R.id.view_featured_article_card_content_container)))
                .perform(scrollTo(), click())

            TestUtil.delay(2)

            // Add the article to watchlist
            onView(withId(R.id.page_toolbar_button_show_overflow_menu)).perform(click())

            TestUtil.delay(1)

            onView(withText("Watch")).perform(click())

            TestUtil.delay(1)

            // Assert we see a snackbar after adding article to watchlist
            onView(allOf(withId(com.google.android.material.R.id.snackbar_text), isDisplayed()))
                .check(ViewAssertions.matches(isDisplayed()))

            onView(allOf(withId(com.google.android.material.R.id.snackbar_action), isDisplayed()))
                .check(ViewAssertions.matches(isDisplayed()))

            // Change article watchlist expiry via the snackbar action button
            onView(allOf(withId(com.google.android.material.R.id.snackbar_action), withText("Change"), isDisplayed()))
                .perform(click())

            onView(allOf(withId(R.id.watchlistExpiryOneMonth), isDisplayed())).perform(click())

            TestUtil.delay(1)

            onView(allOf(withId(com.google.android.material.R.id.snackbar_text), isDisplayed()))
                .check(ViewAssertions.matches(isDisplayed()))

            TestUtil.delay(1)

            onView(withId(R.id.page_toolbar_button_show_overflow_menu)).perform(click())

            TestUtil.delay(1)

            // Make sure that the `Unwatch` option is shown for the article that is being watched
            onView(withText("Unwatch")).perform(click())

            TestUtil.delay(1)
        }
    }
}
