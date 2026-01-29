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
                .map { SearchResult(it.apply {
                    // TODO: remove when the real snippet is available from the API
                    snippet = "Some jurisdictions require \"fixing\" copyrighted works in a tangible form. It is often shared among multiple authors, each of whom holds a set of rights to use or <a href='#'>license the work</a>, and who are commonly referred to as rights holders."
                }, wikiSite, it.coordinates, SearchResult.SearchResultType.SEARCH) }
        } ?: emptyList()
    }
}

data class HybridSearchResults(
    var results: List<SearchResult>
)
