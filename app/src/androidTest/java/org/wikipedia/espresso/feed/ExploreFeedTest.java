package org.wikipedia.espresso.feed;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.espresso.util.ViewTools;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_500;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_6000;
import static org.wikipedia.espresso.util.ViewTools.rotateScreen;
import static org.wikipedia.espresso.util.ViewTools.setTextInTextView;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("checkstyle:magicnumber")
public final class ExploreFeedTest {

    public static void testExploreFeed(Activity activity) {
        while (!viewIsDisplayed(R.id.fragment_feed_feed)) {
            waitFor(WAIT_FOR_2000);
        }

        //Todo: Remove the code to dismiss Announcement cards by adding logic to not display any announcement cards
        while (true) {
            try {
                //Idling till SnackBar disappears
                onView(ViewTools.first(withText("Got it"))).perform(click());
                waitFor(WAIT_FOR_6000);
                onView(withId(R.id.fragment_feed_feed))
                        .perform(swipeDown());
            } catch (NoMatchingViewException e) {
                break;
            } catch (PerformException e) {
                break;
            }
        }

        testCards("");
        while (!viewIsDisplayed(R.id.fragment_feed_feed)) {
            waitFor(WAIT_FOR_2000);
        }

        rotateScreen(activity, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        waitFor(WAIT_FOR_2000);
        testCards("_Landscape");
        rotateScreen(activity, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        waitFor(WAIT_FOR_2000);
    }


    private static void testCards(String postFix) {
        while (!viewIsDisplayed(R.id.fragment_feed_feed)) {
            waitFor(WAIT_FOR_2000);
        }
        //Scrool further and scroll back to required position to ensure display of view on top of screen
        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(7));
        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(5));
        //Wait for page load
        waitFor(WAIT_FOR_500);
        setDate();
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("Randomizer" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(2));
        waitFor(WAIT_FOR_500);
        setDate();
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("OnThisDay" + postFix);

        //Scrool further and scroll back to required position to ensure display of view on top of screen
        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(11));
        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(7));
        waitFor(WAIT_FOR_500);
        setDate();
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("FeaturedImage" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(4));
        waitFor(WAIT_FOR_500);
        setDate();
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("MainPage" + postFix);

        //Scrool further and scroll back to required position to ensure display of view on top of screen
        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(7));
        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(3));
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("Trending" + postFix);

        //Scrool further and scroll back to required position to ensure display of view on top of screen
        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(11));
        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(6));
        waitFor(WAIT_FOR_500);
        setDate();
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("FeaturedArticle" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(1));
        setDate();
        ScreenshotTools.snap("News" + postFix);
    }

    private static void setDate() {
        try {
            for (int i = 0; i < 3; i++) {
                ViewInteraction colorButton = onView(
                        allOf(
                                ViewTools.matchPosition(allOf(withId(R.id.view_card_header_subtitle)), i),
                                isDisplayed()));
                colorButton.perform(setTextInTextView("Feb 5, 2017"));
            }
        } catch (NoMatchingViewException | PerformException e) {
            return;
        }
    }

    private ExploreFeedTest() {
    }

}
