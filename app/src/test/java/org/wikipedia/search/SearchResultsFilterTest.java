package org.wikipedia.search;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class) public class SearchResultsFilterTest {
    private static final WikiSite WIKI = WikiSite.forLanguageCode("test");

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
