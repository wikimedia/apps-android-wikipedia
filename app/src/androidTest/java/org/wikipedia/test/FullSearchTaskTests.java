package org.wikipedia.test;

import android.support.test.filters.SmallTest;
import android.test.ActivityUnitTestCase;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.WikipediaApp;
import org.wikipedia.search.FullSearchArticlesTask;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for full text search.
 */
@SmallTest
public class FullSearchTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;
    private static final int BATCH_SIZE = 12;
    private static final WikiSite WIKI = WikiSite.forLanguageCode("en");

    public FullSearchTaskTests() {
        super(TestDummyActivity.class);
    }

    /** Have to use enwiki since I don't think there are any Wikidata descriptions for testwiki. */
    public void testFullTextSearchWithResults() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                WikipediaApp app = WikipediaApp.getInstance();
                new FullSearchArticlesTask(app.getAPIForSite(WIKI), WIKI, "test", BATCH_SIZE, null, false) {
                    @Override
                    public void onFinish(SearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getResults().size(), BATCH_SIZE);
                        assertNull(results.getSuggestion());
                        assertNotNull(results.getContinueOffset());

                        for (SearchResult result : results.getResults()) {
                            if (result.getPageTitle().getPrefixedText().equals("Test")) {
                                assertEquals(result.getPageTitle().getDescription(), "Wikipedia disambiguation page");
                            }
                        }
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    // TODO: move to TitleSearchTest once we have it

//    public void testFullTextSearchWithSuggestion() throws Throwable {
//        startActivity(new Intent(), null, null);
//        final CountDownLatch completionLatch = new CountDownLatch(1);
//        runTestOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                WikipediaApp app = WikipediaApp.getInstance();
//                new FullSearchArticlesTask(app.getAPIForSite(WIKI), WIKI, "teest", BATCH_SIZE, null) { // small typo should produce a suggestion
//                    @Override
//                    public void onFinish(SearchResults results) {
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
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                WikipediaApp app = WikipediaApp.getInstance();
                new FullSearchArticlesTask(app.getAPIForSite(WIKI), WIKI, "jkfsdfpefdsfwoirpoik", BATCH_SIZE, null, false) { // total gibberish, should not exist on enwiki
                    @Override
                    public void onFinish(SearchResults results) {
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

