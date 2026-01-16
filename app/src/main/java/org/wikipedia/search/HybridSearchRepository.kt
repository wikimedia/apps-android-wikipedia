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
        countsPerLanguageCode: MutableList<Pair<String, Int>>
    ): HybridSearchResults {

        val wikiSite = WikiSite.forLanguageCode(languageCode)
        val standardResults = mutableListOf<SearchResult>()
        val semanticResults = mutableListOf<SearchResult>()

        // prefix + fulltext search results for at most 3 results.
        var response = ServiceFactory.get(wikiSite).prefixSearch(searchTerm, batchSize, 0)
        standardResults.addAll(buildList(response, invokeSource, wikiSite))

        if (standardResults.size < batchSize) {
            response = ServiceFactory.get(wikiSite).fullTextSearch(searchTerm, batchSize, 0)
            standardResults.addAll(buildList(response, invokeSource, wikiSite))
        }

        // semantic search results
        response = ServiceFactory.get(wikiSite).semanticSearch(searchTerm, 10) // TODO: check PM with the default size
        semanticResults.addAll(buildList(response, invokeSource, wikiSite, SearchResult.SearchResultType.SEMANTIC))

        val finalList = standardResults.distinctBy { it.pageTitle.prefixedText }.toMutableList() + semanticResults

        return HybridSearchResults(finalList)
    }

    private fun buildList(
        response: MwQueryResponse,
        invokeSource: Constants.InvokeSource,
        wikiSite: WikiSite,
        type: SearchResult.SearchResultType = SearchResult.SearchResultType.SEARCH
    ): List<SearchResult> {
        return response.query?.pages?.let { list ->
            (if (invokeSource == Constants.InvokeSource.PLACES)
                list.filter { it.coordinates != null } else list).sortedBy { it.index }
                .map { SearchResult(it, wikiSite, it.coordinates, type) }
        } ?: emptyList()
    }
}

data class HybridSearchResults(
    var results: List<SearchResult>
)
