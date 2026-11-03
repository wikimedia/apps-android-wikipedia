package org.wikipedia.search.semantic

import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.search.SearchRepository
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResultsViewModel
import retrofit2.HttpException

class SemanticSearchRepository : SearchRepository<SemanticSearchResults> {
    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean,
        countsPerLanguageCode: MutableList<Pair<String, Int>>
    ): SemanticSearchResults {
        val wikiSite = WikiSite.forLanguageCode(languageCode)
        val response = ServiceFactory.get(wikiSite).fullTextSearchResponse(searchTerm, batchSize, continuation ?: 0, semanticSearchType = "hl")
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        return SemanticSearchResults(
            results = SearchResultsViewModel.buildList(response.body(), invokeSource, wikiSite, SearchResult.SearchResultType.SEMANTIC),
            xSearchId = response.headers()["x-search-id"]
        )
    }
}

data class SemanticSearchResults(
    val results: List<SearchResult>,
    val xSearchId: String? = null
)
