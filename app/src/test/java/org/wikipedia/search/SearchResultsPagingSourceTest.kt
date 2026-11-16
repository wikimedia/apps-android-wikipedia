package org.wikipedia.search

import androidx.paging.PagingSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.Constants
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

@RunWith(RobolectricTestRunner::class)
class SearchResultsPagingSourceTest {
    private val wikiSite = WikiSite.forLanguageCode("en")

    @Test
    fun testLocalResultsStayOnTopAndHideTheirDuplicatesOnEveryPage() {
        val historyResult = searchResult("Eiffel Tower", SearchResult.SearchResultType.HISTORY)
        val prefixResult = searchResult("Paris", SearchResult.SearchResultType.PREFIX)
        val fullTextResult = searchResult("Gustave Eiffel", SearchResult.SearchResultType.FULL_TEXT)
        val pagingSource = pagingSource(mapOf(
            null to StandardSearchResults(
                results = listOf(searchResult("Eiffel Tower", SearchResult.SearchResultType.PREFIX), prefixResult),
                continuation = NEXT_PAGE_KEY,
                localResults = listOf(historyResult)
            ),
            NEXT_PAGE_KEY to StandardSearchResults(
                results = listOf(searchResult("Eiffel Tower", SearchResult.SearchResultType.FULL_TEXT), fullTextResult),
                continuation = null
            )
        ))

        assertEquals(listOf(historyResult, prefixResult), loadPage(pagingSource, PagingSource.LoadParams.Refresh(null, BATCH_SIZE, false)))
        assertEquals(listOf(fullTextResult), loadPage(pagingSource, PagingSource.LoadParams.Append(NEXT_PAGE_KEY, BATCH_SIZE, false)))
    }

    @Test
    fun testResultsAreUnchangedWithoutLocalResults() {
        val results = listOf(
            searchResult("Eiffel Tower", SearchResult.SearchResultType.PREFIX),
            searchResult("Paris", SearchResult.SearchResultType.FULL_TEXT)
        )
        val pagingSource = pagingSource(mapOf(null to StandardSearchResults(results, continuation = null)))

        assertEquals(results, loadPage(pagingSource, PagingSource.LoadParams.Refresh(null, BATCH_SIZE, false)))
    }

    private fun pagingSource(pages: Map<Int?, StandardSearchResults>): SearchResultsViewModel.SearchResultsPagingSource {
        return SearchResultsViewModel.SearchResultsPagingSource(
            searchTerm = "eiffel",
            languageCode = wikiSite.languageCode,
            countsPerLanguageCode = mutableListOf(),
            invokeSource = Constants.InvokeSource.SEARCH,
            repository = FakeSearchRepository(pages),
            onFirstPageLoaded = {}
        )
    }

    private fun loadPage(pagingSource: SearchResultsViewModel.SearchResultsPagingSource, params: PagingSource.LoadParams<Int>): List<SearchResult> {
        return runBlocking { (pagingSource.load(params) as PagingSource.LoadResult.Page).data }
    }

    private fun searchResult(title: String, type: SearchResult.SearchResultType): SearchResult {
        return SearchResult(PageTitle(title, wikiSite), type)
    }

    private class FakeSearchRepository(private val pages: Map<Int?, StandardSearchResults>) : SearchRepository<StandardSearchResults> {
        override suspend fun search(
            searchTerm: String,
            languageCode: String,
            invokeSource: Constants.InvokeSource,
            continuation: Int?,
            batchSize: Int,
            isPrefixSearch: Boolean,
            countsPerLanguageCode: MutableList<Pair<String, Int>>
        ): StandardSearchResults {
            return pages.getValue(continuation)
        }
    }

    companion object {
        private const val BATCH_SIZE = 10
        private const val NEXT_PAGE_KEY = 10
    }
}
