package org.wikipedia.test.perf;

import org.junit.Rule;
import org.junit.Test;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageLoadCallbacks;
import org.wikipedia.page.PageLoadTests;
import org.wikipedia.testlib.TestLatch;

import android.support.annotation.NonNull;
import android.support.test.rule.ActivityTestRule;
import android.test.suitebuilder.annotation.LargeTest;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test performance of page loading. Update the NUM_RUNS for better statistical significance.
 */
@LargeTest
public class PageLoadPerformanceTests {
    @Rule @NonNull
    public final ActivityTestRule<PageActivity> activityRule = new ActivityTestRule<>(PageActivity.class);

    private static final int NUM_RUNS = 1; //50;
    private final MeasurementController measurement = new MeasurementController();

    @Test
    public void testLoadPages() throws Throwable {
        loadPageMultipleTimes("Test_page_for_app_testing/Section1");
        loadPageMultipleTimes("A_long_page");
        loadPageMultipleTimes("Barack_Obama"); // much longer than previous pages, has a lead image

        measurement.analyzeAll();
    }

    private void loadPageMultipleTimes(String title) throws Throwable {
        for (int i = 0; i < NUM_RUNS; i++) {
            loadPageUi(title);
        }
    }

    private void loadPageUi(final String title) throws Throwable {
        final TestLatch latch = new TestLatch();

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                assertThat(getFragment(), notNullValue());
                getFragment().setPageLoadCallbacks(newCallbacks(title, latch));

                measurement.start(title);
                PageLoadTests.loadPage(getFragment(), title);
            }
        });
        latch.await();
    }

    private PageLoadCallbacks newCallbacks(final String title, final TestLatch latch) {
        return new PageLoadCallbacks() {
            @Override
            public void onLoadComplete() {
                measurement.stop(title);
                latch.countDown();
            }
        };
    }

    private PageFragment getFragment() {
        return (PageFragment) getActivity().getTopFragment();
    }

    private PageActivity getActivity() {
        return activityRule.getActivity();
    }
}
