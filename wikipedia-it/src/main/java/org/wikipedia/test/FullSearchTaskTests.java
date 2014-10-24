package org.wikipedia.test;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.search.FullSearchArticlesTask;
import org.wikipedia.search.FullSearchResult;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for full text search.
 */
public class FullSearchTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;
    private static final int NUM_RESULTS_REQUESTED = 12;
    private static final Site SITE = new Site("test.wikipedia.org");

    public FullSearchTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testFullTextSearchWithResults() throws Throwable {
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                new FullSearchArticlesTask(app.getAPIForSite(SITE), SITE, "test", 0) {
                    @Override
                    public void onFinish(FullSearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getResults().size(), NUM_RESULTS_REQUESTED);
                        assertEquals(results.getSuggestion(), "");
                        assertEquals(results.getContinueOffset(), NUM_RESULTS_REQUESTED);

                        for (FullSearchResult result : results.getResults()) {
                            if (result.getTitle().getPrefixedText().equals("Test")) {
                                assertEquals(result.getWikiBaseId(), "Q377");
                            }
                        }
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testFullTextSearchWithSuggestion() throws Throwable {
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                new FullSearchArticlesTask(app.getAPIForSite(SITE), SITE, "teest", 0) { // small typo should produce a suggestion
                    @Override
                    public void onFinish(FullSearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getSuggestion(), "test");
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testEmptyResults() throws Throwable {
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                new FullSearchArticlesTask(app.getAPIForSite(SITE), SITE, "jkfsdfpefdsfwoirpoik", 0) { // toal gibberish, should not exist on testwiki
                    @Override
                    public void onFinish(FullSearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getResults().size(), 0);
                        assertEquals(results.getSuggestion(), "");
                        assertEquals(results.getContinueOffset(), 0);
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

