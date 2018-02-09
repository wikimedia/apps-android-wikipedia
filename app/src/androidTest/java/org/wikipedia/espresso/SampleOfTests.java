package org.wikipedia.espresso;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.onboarding.OnBoardingTest;
import org.wikipedia.espresso.page.PageActivityTest;
import org.wikipedia.espresso.search.SearchTest;
import org.wikipedia.espresso.util.CompareTools;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.main.MainActivity;

import static org.wikipedia.espresso.Constants.WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.pressBack;
import static org.wikipedia.espresso.util.ViewTools.rotateScreen;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SampleOfTests {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void sampleOfTests() {

        // run OnBoarding on every tests
        OnBoardingTest.runOnBoarding();

        while (!viewIsDisplayed(R.id.fragment_feed_feed)) {
            // press back until back to the feed page
            pressBack();
        }

        ScreenshotTools.snap("FeedPage");
        waitFor(WAIT_FOR_2000);

        // test orientation
        rotateScreen(getActivity(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("FeedPage_Landscape");
        rotateScreen(getActivity(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("FeedPage_Portrait");

        SearchTest.searchKeywordAndGo("Barack Obama");

        if (!viewIsDisplayed(R.id.page_toc_drawer)) {
            waitFor(WAIT_FOR_1000);
        }
        PageActivityTest.testArticleLoad(getActivity());
        // TODO: find a better way to generate reference images
        // Reference images should be put under /assets/espresso folder
        Assert.assertTrue("Matching percentage should be higher than " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare(getActivity(), "FeedPage") > WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be higher than " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare(getActivity(), "SearchSuggestionPage") > WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be higher than " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare(getActivity(), "SearchPage") > WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be higher than " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare(getActivity(), "Barack") > WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        //Assert.assertTrue("Matching percentage should be higher than " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare(getActivity(), "ArticleSwipeUpNoActionBar") > WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        //Assert.assertTrue("Matching percentage should be higher than " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare(getActivity(), "ArticleSwipeDownActionBarAndTabSeen") > WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
    }

    private Activity getActivity() {
        return mActivityTestRule.getActivity();
    }
}
