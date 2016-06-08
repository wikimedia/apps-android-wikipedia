package org.wikipedia.test;

import android.test.ActivityUnitTestCase;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.SuggestionsTask;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for getting suggestions for further reading.
 */
public class SuggestionsTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 200000;
    private static final Site SITE = new Site("en.wikipedia.org"); // suggestions don't seem to work on testwiki

    private WikipediaApp app = WikipediaApp.getInstance();

    public SuggestionsTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testTask() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SuggestionsTask(app.getAPIForSite(SITE), SITE, "test", false) {
                    @Override
                    public void onFinish(SearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getResults().size(), Constants.MAX_SUGGESTION_RESULTS);

                        for (SearchResult result : results.getResults()) {
                            assertFalse(result.getPageTitle().getPrefixedText().equals("Test"));
                        }
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    //
    // unit tests:
    //

    public void testFilterNoResults() throws Throwable {
        List<SearchResult> originalResults = new ArrayList<>();
        checkFilter(0, originalResults);
    }

    public void testFilter1ResultSameAsTitleIgnoreCase() throws Throwable {
        List<SearchResult> originalResults = new ArrayList<>();
        originalResults.add(new SearchResult(new PageTitle("Test", SITE, null, null)));
        checkFilter(0, originalResults);
    }

    public void testFilter1ResultDifferentFromTitle() throws Throwable {
        List<SearchResult> originalResults = new ArrayList<>();
        originalResults.add(new SearchResult(new PageTitle("something else", SITE, null, null)));
        checkFilter(1, originalResults);
    }

    public void testFilter4ResultsDifferentFromTitle() throws Throwable {
        List<SearchResult> originalResults = new ArrayList<>();
        originalResults.add(new SearchResult(new PageTitle("something else", SITE, null, null)));
        originalResults.add(new SearchResult(new PageTitle("something else", SITE, null, null)));
        originalResults.add(new SearchResult(new PageTitle("something else", SITE, null, null)));
        originalResults.add(new SearchResult(new PageTitle("something else", SITE, null, null)));
        checkFilter(Constants.MAX_SUGGESTION_RESULTS, originalResults);
    }

    private void checkFilter(int expected, List<SearchResult> originalResults) {
        String title = "test";
        SearchResults searchResults = new SearchResults(originalResults, null, null);
        List<SearchResult> filteredList = SearchResults.filter(searchResults, title, false).getResults();
        assertEquals(expected, filteredList.size());
    }
}
