package org.wikipedia.espresso.feed;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.espresso.util.ViewTools;
import org.wikipedia.main.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.CompareTools.assertScreenshotWithinTolerance;
import static org.wikipedia.espresso.util.ViewTools.rotateScreen;
import static org.wikipedia.espresso.util.ViewTools.setTextInTextView;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("checkstyle:magicnumber")
public class ExploreFeedTest {

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule runtimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void testExploreFeed() throws Exception {
        waitUntilFeedDisplayed();

        testCards("");

        waitUntilFeedDisplayed();

        rotateScreen(getActivity(), ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        waitFor(2000);
        testCards("_Landscape");
        rotateScreen(getActivity(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        waitFor(2000);

        runComparisons();
    }

    private void runComparisons() throws Exception {
        assertScreenshotWithinTolerance("FeaturedArticle");
        assertScreenshotWithinTolerance("FeaturedArticle_Landscape");
        assertScreenshotWithinTolerance("FeaturedImage");
        assertScreenshotWithinTolerance("FeaturedImage_Landscape");
        assertScreenshotWithinTolerance("MainPage");
        assertScreenshotWithinTolerance("MainPage_Landscape");
        assertScreenshotWithinTolerance("News");
        assertScreenshotWithinTolerance("News_Landscape");
        assertScreenshotWithinTolerance("OnThisDay");
        assertScreenshotWithinTolerance("OnThisDay_Landscape");
        assertScreenshotWithinTolerance("Randomizer");
        assertScreenshotWithinTolerance("Randomizer_Landscape");
        assertScreenshotWithinTolerance("Trending");
        assertScreenshotWithinTolerance("Trending_Landscape");
    }

    private Activity getActivity() {
        return activityTestRule.getActivity();
    }

    private static void testCards(String postFix) {
        waitUntilFeedDisplayed();

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(10));
        waitFor(1000);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(7));
        setDate();
        waitFor(1000);
        ScreenshotTools.snap("FeaturedImage" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(6));
        setDate();
        waitFor(1000);
        ScreenshotTools.snap("FeaturedArticle" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(5));
        setDate();
        waitFor(1000);
        ScreenshotTools.snap("Randomizer" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(4));
        setDate();
        waitFor(1000);
        ScreenshotTools.snap("MainPage" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(3));
        setDate();
        waitFor(1000);
        ScreenshotTools.snap("Trending" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(2));
        setDate();
        waitFor(1000);
        ScreenshotTools.snap("OnThisDay" + postFix);

        onView(withId(R.id.fragment_feed_feed)).perform(RecyclerViewActions.scrollToPosition(1));
        setDate();
        ScreenshotTools.snap("News" + postFix);
    }

    private static void waitUntilFeedDisplayed() {
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_feed_feed),
                () -> waitFor(2000));
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
}
