package org.wikipedia.page;

import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;
import org.wikipedia.testlib.TestLatch;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.wikipedia.test.TestUtil.runOnMainSync;

/**
 * Tests for getting suggestions for further reading.
 */
@SmallTest
public class SuggestionsTaskTest {
    private static final WikiSite WIKI = WikiSite.forLanguageCode("en"); // suggestions don't seem to work on testwiki

    private WikipediaApp app = WikipediaApp.getInstance();

    @Test public void testTask() {
        final TestLatch latch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new SuggestionsTask(app.getAPIForSite(WIKI), WIKI, "test", false) {
                    @Override
                    public void onFinish(SearchResults results) {
                        assertThat(results, notNullValue());
                        assertThat(results.getResults().size(), is(Constants.MAX_SUGGESTION_RESULTS));

                        for (SearchResult result : results.getResults()) {
                            assertThat(result.getPageTitle().getPrefixedText(), not("Test"));
                        }
                        latch.countDown();
                    }
                }.execute();
            }
        });
        latch.await();
    }

    @Test public void testFilterNoResults() {
        List<SearchResult> originalResults = new ArrayList<>();
        checkFilter(0, originalResults);
    }

    @Test public void testFilter1ResultSameAsTitleIgnoreCase() {
        List<SearchResult> originalResults = new ArrayList<>();
        originalResults.add(new SearchResult(new PageTitle("Test", WIKI, null, null)));
        checkFilter(0, originalResults);
    }

    @Test public void testFilter1ResultDifferentFromTitle() {
        List<SearchResult> originalResults = new ArrayList<>();
        originalResults.add(new SearchResult(new PageTitle("something else", WIKI, null, null)));
        checkFilter(1, originalResults);
    }

    @Test public void testFilter4ResultsDifferentFromTitle() {
        List<SearchResult> originalResults = new ArrayList<>();
        originalResults.add(new SearchResult(new PageTitle("something else", WIKI, null, null)));
        originalResults.add(new SearchResult(new PageTitle("something else", WIKI, null, null)));
        originalResults.add(new SearchResult(new PageTitle("something else", WIKI, null, null)));
        originalResults.add(new SearchResult(new PageTitle("something else", WIKI, null, null)));
        checkFilter(Constants.MAX_SUGGESTION_RESULTS, originalResults);
    }

    private void checkFilter(int expected, List<SearchResult> originalResults) {
        String title = "test";
        SearchResults searchResults = new SearchResults(originalResults, null, null);
        List<SearchResult> filteredList = SearchResults.filter(searchResults, title, false).getResults();
        assertThat(expected, is(filteredList.size()));
    }
}