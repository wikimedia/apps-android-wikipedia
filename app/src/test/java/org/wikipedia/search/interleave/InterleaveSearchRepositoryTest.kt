package org.wikipedia.search.interleave

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.Constants
import org.wikipedia.dataclient.CoreRestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.Attribution
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchRepository
import org.wikipedia.search.SearchResult
import org.wikipedia.search.StandardSearchResults
import java.io.IOException

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
            lexicalRepository = FakeSearchRepository { StandardSearchResults(lexicalResults, continuation = null, localResults = listOf(historyResult)) },
            semanticRepository = FakeSearchRepository { StandardSearchResults(semanticResults, continuation = null) },
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

    @Test
    fun testLaterPagesReuseTheMergeOfTheFirstPage() {
        val lexicalRepository = FakeSearchRepository {
            StandardSearchResults(listOf(
                searchResult("Paris", SearchResult.SearchResultType.PREFIX, pageId = 2),
                searchResult("Lyon", SearchResult.SearchResultType.PREFIX, pageId = 4)
            ), continuation = null)
        }
        val semanticRepository = FakeSearchRepository {
            StandardSearchResults(listOf(searchResult("Gustave Eiffel", SearchResult.SearchResultType.SEMANTIC, pageId = 3)), continuation = null)
        }
        val repository = InterleaveSearchRepository(lexicalRepository, semanticRepository, newSeed = { SEMANTIC_FIRST_SEED })

        val firstPage = search(repository, continuation = null, isPrefixSearch = true)
        search(repository, continuation = firstPage.continuation, isPrefixSearch = false)

        assertEquals(1, lexicalRepository.searchCount)
        assertEquals(1, semanticRepository.searchCount)
    }

    @Test
    fun testFirstPageLoadRetriesSemanticSearchThatFailedBefore() {
        val lexicalResults = listOf(searchResult("Paris", SearchResult.SearchResultType.PREFIX, pageId = 2))
        val semanticResults = listOf(searchResult("Gustave Eiffel", SearchResult.SearchResultType.SEMANTIC, pageId = 3))
        var isSemanticSearchAvailable = false
        val repository = InterleaveSearchRepository(
            lexicalRepository = FakeSearchRepository { StandardSearchResults(lexicalResults, continuation = null) },
            semanticRepository = FakeSearchRepository {
                if (!isSemanticSearchAvailable) {
                    throw IOException()
                }
                StandardSearchResults(semanticResults, continuation = null)
            },
            newSeed = { SEMANTIC_FIRST_SEED }
        )

        assertEquals(lexicalResults, search(repository, continuation = null, isPrefixSearch = true).results)

        isSemanticSearchAvailable = true
        assertEquals(semanticResults + lexicalResults, search(repository, continuation = null, isPrefixSearch = true).results)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testAttributionCountsComeFromTheArticle() {
        val attribution = JsonUtil.decodeFromString<Attribution>(
            "{\"trust_and_relevance\":{\"contributor_counts\":{\"total_unique\":2348},\"reference_count\":0}}"
        )!!
        val coreRestService = mockCoreRestService()
        coEvery { coreRestService.getAttribution(any()) } returns attribution

        val result = runBlocking { emptyRepository().getAttribution(searchResult("Eiffel Tower", SearchResult.SearchResultType.SEMANTIC, pageId = 1)) }

        coVerify { coreRestService.getAttribution("Eiffel_Tower") }
        assertEquals(2348, result?.trustAndRelevance?.contributorCounts?.totalUnique)
        assertEquals(0, result?.trustAndRelevance?.referenceCount)
    }

    @Test
    fun testFailedAttributionRequestReturnsNull() {
        val coreRestService = mockCoreRestService()
        coEvery { coreRestService.getAttribution(any()) } throws IOException("offline")

        val result = runBlocking { emptyRepository().getAttribution(searchResult("Paris", SearchResult.SearchResultType.PREFIX, pageId = 2)) }

        assertNull(result)
    }

    private fun mockCoreRestService(): CoreRestService {
        val coreRestService = mockk<CoreRestService>()
        mockkObject(ServiceFactory)
        every { ServiceFactory.getCoreRest(any()) } returns coreRestService
        return coreRestService
    }

    private fun emptyRepository(): InterleaveSearchRepository {
        val emptySearch = FakeSearchRepository { StandardSearchResults(emptyList(), continuation = null) }
        return InterleaveSearchRepository(emptySearch, emptySearch)
    }

    private fun search(repository: InterleaveSearchRepository, continuation: Int?, isPrefixSearch: Boolean): StandardSearchResults {
        return runBlocking {
            repository.search("eiffel", wikiSite.languageCode, Constants.InvokeSource.SEARCH, continuation, BATCH_SIZE, isPrefixSearch)
        }
    }

    private fun searchResult(title: String, type: SearchResult.SearchResultType, pageId: Int): SearchResult {
        return SearchResult(PageTitle(title, wikiSite), redirectFrom = null, type = type, pageId = pageId)
    }

    private class FakeSearchRepository(private val searchResults: () -> StandardSearchResults) : SearchRepository<StandardSearchResults> {
        var searchCount = 0
            private set

        override suspend fun search(
            searchTerm: String,
            languageCode: String,
            invokeSource: Constants.InvokeSource,
            continuation: Int?,
            batchSize: Int,
            isPrefixSearch: Boolean
        ): StandardSearchResults {
            searchCount++
            return searchResults()
        }
    }

    companion object {
        private const val SEMANTIC_FIRST_SEED = 0uL
        private const val BATCH_SIZE = 2
    }
}
