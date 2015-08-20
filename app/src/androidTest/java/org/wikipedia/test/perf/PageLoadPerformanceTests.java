package org.wikipedia.test.perf;

import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageLoadCallbacks;
import org.wikipedia.page.PageLoadTests;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test performance of page loading. Update the NUM_RUNS for better statistical significance.
 */
@LargeTest
public class PageLoadPerformanceTests extends ActivityInstrumentationTestCase2<PageActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);
    private static final int NUM_RUNS = 1; //50;
    private PageActivity activity;
    private CountDownLatch completionLatch;
    private PageFragment fragment;
    private String title;
    private MeasurementController measurement = new MeasurementController();

    private PageLoadCallbacks callback = new PageLoadCallbacks() {
        @Override
        public void onLoadComplete() {
            measurement.stop(title);
            completionLatch.countDown();
        }
    };

    public PageLoadPerformanceTests() {
        super(PageActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }

    public void testLoadPages() throws Throwable {
        loadPageMultipleTimes("Test_page_for_app_testing/Section1");
        loadPageMultipleTimes("A_long_page");
        loadPageMultipleTimes("Barack_Obama"); // much longer than previous pages, has a lead image

        measurement.analyzeAll();
    }

    private void loadPageMultipleTimes(String title) throws Throwable {
        this.title = title;
        for (int i = 0; i < NUM_RUNS; i++) {
            loadPageUi();
        }
    }

    private void loadPageUi() throws Throwable {
        completionLatch = new CountDownLatch(1);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment = (PageFragment) activity.getTopFragment();
                fragment.setPageLoadCallbacks(callback);

                measurement.start(title);
                PageLoadTests.loadPage(fragment, title);
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
