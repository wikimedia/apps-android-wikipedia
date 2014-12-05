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
    private static final int BATCH_SIZE = 12;
    private static final Site SITE = new Site("test.wikipedia.org");
    private static final Site EN_SITE = new Site("en.wikipedia.org");

    public FullSearchTaskTests() {
        super(TestDummyActivity.class);
    }

    /** Have to use enwiki since I don't think there are any Wikidata descriptions for testwiki. */
    public void testFullTextSearchWithResults() throws Throwable {
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                new FullSearchArticlesTask(app.getAPIForSite(EN_SITE), EN_SITE, "test", BATCH_SIZE, null) {
                    @Override
                    public void onFinish(FullSearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getResults().size(), BATCH_SIZE);
                        assertEquals(results.getSuggestion(), "");
                        assertNotNull(results.getContinueOffset());

                        for (FullSearchResult result : results.getResults()) {
                            if (result.getTitle().getPrefixedText().equals("Test")) {
                                assertEquals(result.getDescription(), "Wikipedia disambiguation page");
                            }
                        }
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    // can't seem to get suggestions anymore since search has changed

//    public void testFullTextSearchWithSuggestion() throws Throwable {
//        startActivity(new Intent(), null, null);
//        final CountDownLatch completionLatch = new CountDownLatch(1);
//        runTestOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
//                new FullSearchArticlesTask(app.getAPIForSite(SITE), SITE, "teest", BATCH_SIZE, null) { // small typo should produce a suggestion
//                    @Override
//                    public void onFinish(FullSearchResults results) {
//                        assertNotNull(results);
//                        assertEquals(results.getSuggestion(), "test");
//                        completionLatch.countDown();
//                    }
//                }.execute();
//            }
//        });
//        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
//    }

    public void testEmptyResults() throws Throwable {
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                new FullSearchArticlesTask(app.getAPIForSite(SITE), SITE, "jkfsdfpefdsfwoirpoik", BATCH_SIZE, null) { // total gibberish, should not exist on testwiki
                    @Override
                    public void onFinish(FullSearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getResults().size(), 0);
                        assertEquals(results.getSuggestion(), "");
                        assertNull(results.getContinueOffset());
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

