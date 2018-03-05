package org.wikipedia.espresso.page;

import android.Manifest;
import android.support.test.espresso.DataInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.search.SearchBehaviors;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.page.PageActivity;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.CompareTools.assertScreenshotWithinTolerance;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_3000;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

@RunWith(AndroidJUnit4.class)
public final class PageActivityTest {

    @Rule
    public ActivityTestRule<PageActivity> activityTestRule = new ActivityTestRule<>(PageActivity.class);

    @Rule
    public GrantPermissionRule runtimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void testArticleLoad() throws Exception {

        SearchBehaviors.searchKeywordAndGo("Barack Obama", true);

        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.search_results_list),
                () -> waitFor(WAIT_FOR_1000));

        DataInteraction view = onData(anything())
                .inAdapterView(allOf(withId(R.id.search_results_list),
                        childAtPosition(
                                withId(R.id.search_results_container),
                                1)))
                .atPosition(0);
        view.perform(click());

        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.view_page_header_image),
                () -> waitFor(WAIT_FOR_2000));

        waitFor(WAIT_FOR_3000);
        ScreenshotTools.snap("PageActivityWithObama");

        //Todo: Create espresso.screenshots to show hide/show of tab layout and actionBar
        runComparisons();
    }

    private void runComparisons() throws Exception {
        ScreenshotTools.snap("ArticleSwipeDownActionBarAndTabSeen");
        assertScreenshotWithinTolerance("PageActivityWithObama");
        assertScreenshotWithinTolerance("SearchSuggestionPage");
        assertScreenshotWithinTolerance("SearchPage");
    }
}
