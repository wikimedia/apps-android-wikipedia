package org.wikipedia.search

import kotlinx.coroutines.delay
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite

class SemanticSearchRepository: SearchRepository<SemanticResult> {
    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean,
        countsPerLanguageCode: MutableList<Pair<String, Int>>?
    ): SemanticResult {
        return SemanticResult(results = listOf(
            "apple",
            "rose",
            "dance",
            "flower"
        ), type = SearchResultType.SEMANTIC_SEARCH)
    }
}