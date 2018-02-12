package org.wikipedia.espresso;


import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.dataclient.okhttp.MockInstrumentationInterceptor;
import org.wikipedia.espresso.feed.ExploreFeedTest;
import org.wikipedia.espresso.onboarding.OnBoardingTest;
import org.wikipedia.espresso.page.PageActivityTest;
import org.wikipedia.espresso.search.SearchTest;
import org.wikipedia.espresso.util.CompareTools;
import org.wikipedia.main.MainActivity;

import static org.wikipedia.espresso.Constants.WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER;
import static org.wikipedia.espresso.util.ViewTools.pressBack;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class InstrumentationTestSuite {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() {
        MockInstrumentationInterceptor.setInstrumentationContext(InstrumentationRegistry.getContext());
    }

    @After
    public void tearDown() {
        MockInstrumentationInterceptor.setInstrumentationContext(null);
    }

    @Test
    public void instrumentationTests() {

        // run OnBoarding on every tests
        OnBoardingTest.runOnBoarding();

        while (!viewIsDisplayed(R.id.fragment_feed_feed)) {
            // press back until back to the feed page
            pressBack();
        }
        ExploreFeedTest.testExploreFeed(getActivity());
        SearchTest.searchKeywordAndGo("Barack Obama");

        PageActivityTest.testArticleLoad(getActivity());

        while (!viewIsDisplayed(R.id.fragment_feed_feed)) {
            // press back until back to the feed page
            pressBack();
        }

        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("FeaturedArticle") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("FeaturedArticle_Landscape") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("FeaturedImage") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("FeaturedImage_Landscape") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("MainPage") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("MainPage_Landscape") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("News") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("News_Landscape") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("OnThisDay") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("OnThisDay_Landscape") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("Randomizer") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("Randomizer_Landscape") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("Trending") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("Trending_Landscape") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("SearchSuggestionPage") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("SearchPage") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        Assert.assertTrue("Matching percentage should be " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare("Barack") == WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
        // Assert.assertTrue("Matching percentage should be higher than " + WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER, CompareTools.compare(getActivity(), "ArticleSwipeDownActionBarAndTabSeen") > WIKIPEDIA_APP_TEST_COMPARE_ALLOWANCE_NUMBER);
    }

    private Activity getActivity() {
        return mActivityTestRule.getActivity();
    }
}
