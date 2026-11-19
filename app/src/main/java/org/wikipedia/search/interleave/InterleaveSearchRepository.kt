package org.wikipedia.search.interleave

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.Attribution
import org.wikipedia.search.SearchRepository
import org.wikipedia.search.SearchResult
import org.wikipedia.search.StandardSearchRepository
import org.wikipedia.search.StandardSearchResults
import org.wikipedia.search.semantic.SemanticSearchRepository
import org.wikipedia.util.log.L

/**
 * Fetches lexical and semantic results on every first-page load, merges them with [SearchResultsInterleaver],
 * and serves later pages from that merged list, so paging never changes the list that was merged.
 */
class InterleaveSearchRepository(
    private val lexicalRepository: SearchRepository<StandardSearchResults> = StandardSearchRepository(),
    private val semanticRepository: SearchRepository<StandardSearchResults> = SemanticSearchRepository(),
    private val newSeed: () -> ULong = SearchResultsInterleaver::newSeed,
    private val fetchAttribution: suspend (WikiSite, String) -> Attribution = { wikiSite, title ->
        ServiceFactory.getCoreRest(wikiSite).getAttribution(title)
    },
    private val fetchLeads: suspend (WikiSite, List<Int>) -> Map<Int, String> = { wikiSite, pageIds ->
        ServiceFactory.get(wikiSite).getLeadExtractsByPageIds(pageIds.joinToString("|"))
            .query?.pages.orEmpty().mapNotNull { page -> page.extract?.let { page.pageId to it } }.toMap()
    }
) : SearchRepository<StandardSearchResults> {
    private var mergedSearchResults: MergedSearchResults? = null

    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean
    ): StandardSearchResults {
        val merged = mergedSearchResults?.takeIf { continuation != null && it.searchTerm == searchTerm && it.languageCode == languageCode }
            ?: mergeSearchResults(searchTerm, languageCode, invokeSource).also { mergedSearchResults = it }
        val slots = merged.interleaveResult.slots
        val offset = continuation ?: 0
        val pageResults = slots.drop(offset).take(batchSize).map { it.item }
        return StandardSearchResults(
            results = pageResults,
            continuation = (offset + batchSize).takeIf { it < slots.size },
            xSearchIdPrefix = merged.xSearchIdPrefix,
            xSearchIdFullText = merged.xSearchIdFullText,
            xSearchIdSemantic = merged.xSearchIdSemantic,
            localResults = if (isPrefixSearch) merged.localResults else emptyList()
        )
    }

    private suspend fun mergeSearchResults(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource
    ): MergedSearchResults = coroutineScope {
        val semanticSearch = async { searchSemanticOrEmpty(searchTerm, languageCode, invokeSource) }
        val lexicalSearch = lexicalRepository.search(
            searchTerm = searchTerm,
            languageCode = languageCode,
            invokeSource = invokeSource,
            batchSize = RESULTS_PER_SEARCH
        )
        val semanticSearchResults = semanticSearch.await()

        // Local results are not merge inputs, so the merged list matches the other platforms for the same lexical and semantic lists.
        MergedSearchResults(
            searchTerm = searchTerm,
            languageCode = languageCode,
            localResults = lexicalSearch.localResults,
            interleaveResult = withLeads(SearchResultsInterleaver.merge(lexicalSearch.results, semanticSearchResults.results, LIST_LENGTH, newSeed()) { it.pageId.toLong() }),
            xSearchIdPrefix = lexicalSearch.xSearchIdPrefix,
            xSearchIdFullText = lexicalSearch.xSearchIdFullText,
            xSearchIdSemantic = semanticSearchResults.xSearchIdSemantic
        )
    }

    // Lexical results arrive without the article's own words, so their leads are fetched once per merged list.
    private suspend fun withLeads(interleaveResult: InterleaveResult<SearchResult>): InterleaveResult<SearchResult> {
        val lexicalResults = interleaveResult.slots.map { it.item }.filter { it.type != SearchResult.SearchResultType.SEMANTIC }
        if (lexicalResults.isEmpty()) {
            return interleaveResult
        }
        val leads = try {
            val wikiSite = lexicalResults.first().pageTitle.wikiSite
            lexicalResults.map { it.pageId }.distinct().chunked(LEADS_PER_REQUEST).fold(emptyMap<Int, String>()) { leads, pageIds ->
                leads + fetchLeads(wikiSite, pageIds)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            L.w(e)
            emptyMap()
        }
        return interleaveResult.copy(slots = interleaveResult.slots.map { slot ->
            val lead = leads[slot.item.pageId].takeIf { slot.item.type != SearchResult.SearchResultType.SEMANTIC }
            if (lead == null) slot else slot.copy(item = slot.item.copy(leadText = lead))
        })
    }

    // A failed request must not break the card, so it gets no counts and hides them.
    suspend fun getAttribution(result: SearchResult): SearchResultAttribution {
        return try {
            val trustAndRelevance = fetchAttribution(result.pageTitle.wikiSite, result.pageTitle.prefixedText).trustAndRelevance
            SearchResultAttribution(
                contributorCount = trustAndRelevance?.contributorCounts?.totalUnique,
                referenceCount = trustAndRelevance?.referenceCount
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            L.w(e)
            SearchResultAttribution.NONE
        }
    }

    // A failed semantic search must not break search, so the reader gets the lexical results instead.
    private suspend fun searchSemanticOrEmpty(searchTerm: String, languageCode: String, invokeSource: Constants.InvokeSource): StandardSearchResults {
        return try {
            semanticRepository.search(searchTerm, languageCode, invokeSource, batchSize = RESULTS_PER_SEARCH)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            L.w(e)
            StandardSearchResults(emptyList(), continuation = null)
        }
    }

    private class MergedSearchResults(
        val searchTerm: String,
        val languageCode: String,
        val localResults: List<SearchResult>,
        val interleaveResult: InterleaveResult<SearchResult>,
        val xSearchIdPrefix: String?,
        val xSearchIdFullText: String?,
        val xSearchIdSemantic: String?
    )

    companion object {
        private const val LIST_LENGTH = 10
        private const val RESULTS_PER_SEARCH = 10
        private const val LEADS_PER_REQUEST = 20
    }
}
