package org.wikipedia.search;

import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.testlib.TestLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.wikipedia.test.TestUtil.runOnMainSync;

/**
 * Tests for full text search.
 */
@SmallTest public class FullSearchArticlesTaskTest {
    private static final int BATCH_SIZE = 12;
    private static final WikiSite WIKI = WikiSite.forLanguageCode("en");

    /** Have to use enwiki since I don't think there are any Wikidata descriptions for testwiki. */
    @Test public void testFullTextSearchWithResults() throws Throwable {
        final TestLatch latch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WikipediaApp app = WikipediaApp.getInstance();
                new FullSearchArticlesTask(app.getAPIForSite(WIKI), WIKI, "test", BATCH_SIZE, null, false) {
                    @Override
                    public void onFinish(SearchResults results) {
                        assertThat(results, notNullValue());
                        assertThat(results.getResults().size(), is(BATCH_SIZE));
                        assertThat(results.getSuggestion(), nullValue());
                        assertThat(results.getContinueOffset(), notNullValue());

                        for (SearchResult result : results.getResults()) {
                            if (result.getPageTitle().getPrefixedText().equals("Test")) {
                                assertThat(result.getPageTitle().getDescription(), is("Wikipedia disambiguation page"));
                            }
                        }
                        latch.countDown();
                    }
                }.execute();
            }
        });
        latch.await();
    }

    // TODO: move to TitleSearchTest once we have it

//    @Test public void testFullTextSearchWithSuggestion() throws Throwable {
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

    @Test public void testEmptyResults() throws Throwable {
        final TestLatch latch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WikipediaApp app = WikipediaApp.getInstance();
                new FullSearchArticlesTask(app.getAPIForSite(WIKI), WIKI, "jkfsdfpefdsfwoirpoik", BATCH_SIZE, null, false) { // total gibberish, should not exist on enwiki
                    @Override
                    public void onFinish(SearchResults results) {
                        assertThat(results, notNullValue());
                        assertThat(results.getResults().size(), is(0));
                        assertThat(results.getSuggestion(), is(""));
                        assertThat(results.getContinueOffset(), nullValue());
                        latch.countDown();
                    }
                }.execute();
            }
        });
        latch.await();
    }
}

