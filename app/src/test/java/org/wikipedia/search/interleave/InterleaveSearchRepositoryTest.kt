package org.wikipedia.search.interleave

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.Constants
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchRepository
import org.wikipedia.search.SearchResult
import org.wikipedia.search.StandardSearchResults

@RunWith(RobolectricTestRunner::class)
class InterleaveSearchRepositoryTest {
    private val wikiSite = WikiSite.forLanguageCode("en")

    @Test
    fun testLocalResultsAreNotMergedAndDoNotChangeTheMergeInputs() {
        val historyResult = searchResult("Eiffel Tower", SearchResult.SearchResultType.HISTORY, pageId = 0)
        val lexicalResults = listOf(
            searchResult("Eiffel Tower", SearchResult.SearchResultType.PREFIX, pageId = 1),
            searchResult("Paris", SearchResult.SearchResultType.PREFIX, pageId = 2)
        )
        val semanticResults = listOf(
            searchResult("Eiffel Tower", SearchResult.SearchResultType.SEMANTIC, pageId = 1),
            searchResult("Gustave Eiffel", SearchResult.SearchResultType.SEMANTIC, pageId = 3)
        )
        val repository = InterleaveSearchRepository(
            lexicalRepository = FakeSearchRepository(StandardSearchResults(lexicalResults, continuation = null, localResults = listOf(historyResult))),
            semanticRepository = FakeSearchRepository(StandardSearchResults(semanticResults, continuation = null)),
            newSeed = { SEMANTIC_FIRST_SEED }
        )

        // The semantic "Eiffel Tower" is placed first even though it is also a local result.
        val firstPage = search(repository, continuation = null, isPrefixSearch = true)
        assertEquals(listOf(semanticResults[0], lexicalResults[1]), firstPage.results)
        assertEquals(listOf(historyResult), firstPage.localResults)

        val nextPage = search(repository, continuation = firstPage.continuation, isPrefixSearch = false)
        assertEquals(listOf(semanticResults[1]), nextPage.results)
        assertTrue(nextPage.localResults.isEmpty())
    }

    private fun search(repository: InterleaveSearchRepository, continuation: Int?, isPrefixSearch: Boolean): StandardSearchResults {
        return runBlocking {
            repository.search("eiffel", wikiSite.languageCode, Constants.InvokeSource.SEARCH, continuation, BATCH_SIZE, isPrefixSearch)
        }
    }

    private fun searchResult(title: String, type: SearchResult.SearchResultType, pageId: Int): SearchResult {
        return SearchResult(PageTitle(title, wikiSite), redirectFrom = null, type = type, pageId = pageId)
    }

    private class FakeSearchRepository(private val results: StandardSearchResults) : SearchRepository<StandardSearchResults> {
        override suspend fun search(
            searchTerm: String,
            languageCode: String,
            invokeSource: Constants.InvokeSource,
            continuation: Int?,
            batchSize: Int,
            isPrefixSearch: Boolean,
            countsPerLanguageCode: MutableList<Pair<String, Int>>
        ): StandardSearchResults {
            return results
        }
    }

    companion object {
        private const val SEMANTIC_FIRST_SEED = 0uL
        private const val BATCH_SIZE = 2
    }
}
