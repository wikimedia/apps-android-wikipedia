package org.wikipedia.test;

import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.SuggestionsTask;
import org.wikipedia.search.SearchResults;

import android.test.ActivityUnitTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for getting suggestions for further reading.
 */
public class SuggestionsTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 200000;
    private static final int BATCH_SIZE = 3;
    private static final int THUMB_SIZE = 100;
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
                new SuggestionsTask(app.getAPIForSite(SITE), SITE, "test",
                        BATCH_SIZE + 1, BATCH_SIZE, THUMB_SIZE, false) {
                    @Override
                    public void onFinish(SearchResults results) {
                        assertNotNull(results);
                        assertEquals(results.getPageTitles().size(), BATCH_SIZE);

                        for (PageTitle result : results.getPageTitles()) {
                            assertFalse(result.getPrefixedText().equals("Test"));
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
        List<PageTitle> originalResults = new ArrayList<>();
        checkFilter(0, originalResults);
    }

    public void testFilter1ResultSameAsTitleIgnoreCase() throws Throwable {
        List<PageTitle> originalResults = new ArrayList<>();
        originalResults.add(new PageTitle("Test", SITE, null, null));
        checkFilter(0, originalResults);
    }

    public void testFilter1ResultDifferentFromTitle() throws Throwable {
        List<PageTitle> originalResults = new ArrayList<>();
        originalResults.add(new PageTitle("something else", SITE, null, null));
        checkFilter(1, originalResults);
    }

    public void testFilter4ResultsDifferentFromTitle() throws Throwable {
        List<PageTitle> originalResults = new ArrayList<>();
        originalResults.add(new PageTitle("something else", SITE, null, null));
        originalResults.add(new PageTitle("something else", SITE, null, null));
        originalResults.add(new PageTitle("something else", SITE, null, null));
        originalResults.add(new PageTitle("something else", SITE, null, null));
        checkFilter(BATCH_SIZE, originalResults);
    }

    private void checkFilter(int expected, List<PageTitle> originalResults) {
        SuggestionsTask task = new SuggestionsTask(app.getAPIForSite(SITE), SITE, "test",
                BATCH_SIZE + 1, BATCH_SIZE, THUMB_SIZE, false);
        SearchResults searchResults = new SearchResults(originalResults, null, null);
        List<PageTitle> filteredList = task.filterResults(searchResults).getPageTitles();
        assertEquals(expected, filteredList.size());
    }
}
