package org.wikipedia.search

import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse

class HybridSearchRepository : SearchRepository<HybridSearchResults> {
    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean,
        countsPerLanguageCode: MutableList<Pair<String, Int>>,
        searchInLanguages: Boolean,
        isHybridSearch: Boolean
    ): HybridSearchResults {

        val wikiSite = WikiSite.forLanguageCode(languageCode)
        val semanticResults = mutableListOf<SearchResult>()

        val response = ServiceFactory.get(wikiSite).semanticSearch(searchTerm, batchSize)
        semanticResults.addAll(buildList(response, invokeSource, wikiSite))

        return HybridSearchResults(semanticResults)
    }

    private fun buildList(
        response: MwQueryResponse,
        invokeSource: Constants.InvokeSource,
        wikiSite: WikiSite,
    ): List<SearchResult> {
        return response.query?.pages?.let { list ->
            (if (invokeSource == Constants.InvokeSource.PLACES)
                list.filter { it.coordinates != null } else list).sortedBy { it.index }
                .map { SearchResult(it, wikiSite, it.coordinates, SearchResult.SearchResultType.SEARCH) }
        } ?: emptyList()
    }
}

data class HybridSearchResults(
    var results: List<SearchResult>
)
