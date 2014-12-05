package org.wikipedia.test;

import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.SuggestionsTask;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for getting suggestions for further reading.
 */
public class SuggestionsTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 200000;
    private static final int BATCH_SIZE = 3;
    private static final Site SITE = new Site("test.wikipedia.org");

    public SuggestionsTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testFullTextSearchWithResults() throws Throwable {
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                new SuggestionsTask(app.getAPIForSite(SITE), SITE, "test") {
                    @Override
                    public void onCatch(Throwable caught) {
                        super.onCatch(caught);
                    }

                    @Override
                    public void onFinish(FullSearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getResults().size(), BATCH_SIZE);

                        for (PageTitle result : results.getResults()) {
                            assertFalse(result.getPrefixedText().equals("Test"));
                        }
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
