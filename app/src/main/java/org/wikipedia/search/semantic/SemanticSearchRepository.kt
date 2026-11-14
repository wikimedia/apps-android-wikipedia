package org.wikipedia.search.semantic

import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.search.SearchRepository
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResultsViewModel
import org.wikipedia.search.StandardSearchResults
import retrofit2.HttpException

class SemanticSearchRepository : SearchRepository<StandardSearchResults> {
    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean,
        countsPerLanguageCode: MutableList<Pair<String, Int>>
    ): StandardSearchResults {
        val wikiSite = WikiSite.forLanguageCode(languageCode)
        val response = ServiceFactory.get(wikiSite).fullTextSearchResponse(searchTerm, batchSize, continuation ?: 0, semanticSearchType = "hl")
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val body = response.body()
        return StandardSearchResults(
            results = SearchResultsViewModel.buildList(body, invokeSource, wikiSite, SearchResult.SearchResultType.SEMANTIC),
            continuation = body?.continuation?.gsroffset,
            xSearchIdSemantic = response.headers()["x-search-id"]
        )
    }
}
