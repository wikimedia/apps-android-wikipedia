package org.wikipedia.espresso.page;

import android.app.Activity;
import android.support.test.espresso.DataInteraction;
import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.espresso.util.ViewTools;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_3000;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

@RunWith(AndroidJUnit4.class)
public final class PageActivityTest {
    public static void testArticleLoad(Activity activity) {

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
        ScreenshotTools.snap("Barack");
        onView(withId(R.id.view_page_header_image))
                .perform(swipeUp());
        onView(withId(R.id.page_fragment))
                .perform(swipeUp());
        onView(withId(R.id.page_fragment))
                .perform(swipeUp());
        onView(withId(R.id.page_fragment))
                .perform(swipeDown());
        ScreenshotTools.snap("ArticleSwipeDownActionBarAndTabSeen");

        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_feed_feed),
                ViewTools::pressBack);
    }

    private PageActivityTest() {
    }

}
