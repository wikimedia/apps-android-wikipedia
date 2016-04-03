package org.wikipedia.test.perf;

import org.junit.Test;
import org.wikipedia.page.PageLoadLatchCallback;
import org.wikipedia.page.BasePageLoadTest;

/**
 * Test performance of page loading. Update the NUM_RUNS for better statistical significance.
 */
public class PageLoadPerformanceTests extends BasePageLoadTest {
    private static final int NUM_RUNS = 1; //50;
    private final MeasurementController measurement = new MeasurementController();

    @Test
    public void testLoadPages() throws Throwable {
        loadPageMultipleTimes("Test_page_for_app_testing/Section1");
        loadPageMultipleTimes("A_long_page");
        loadPageMultipleTimes("Barack_Obama"); // much longer than previous pages, has a lead image

        measurement.analyzeAll();
    }

    @Override
    protected void loadPageSync(String title) {
        measurement.start(title);
        loadPageSync(title, new Callback(title));
    }

    private void loadPageMultipleTimes(String title) {
        for (int i = 0; i < NUM_RUNS; i++) {
            loadPageSync(title);
        }
    }

    private class Callback extends PageLoadLatchCallback {
        private final String title;

        Callback(String title) {
            this.title = title;
        }

        @Override
        public void onLoadComplete() {
            measurement.stop(title);
            super.onLoadComplete();
        }
    }
}
