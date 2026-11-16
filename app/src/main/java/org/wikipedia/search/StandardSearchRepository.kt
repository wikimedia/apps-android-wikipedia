package org.wikipedia.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.util.StringUtil

class StandardSearchRepository : SearchRepository<StandardSearchResults> {
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
        // Local results are kept out of the lexical list so that it is built by the shared lexical list spec, like on iOS.
        val localResults = mutableListOf<SearchResult>()
        val lexicalResults = mutableListOf<SearchResult>()
        var response: MwQueryResponse? = null
        var currentContinuation = continuation
        var lastXSearchIdPrefix: String? = null
        var lastXSearchIdFullText: String? = null

        if (isPrefixSearch) {
            if (searchTerm.length >= 2 && invokeSource != Constants.InvokeSource.PLACES) {
                withContext(Dispatchers.IO) {
                    listOf(async {
                        getSearchResultsFromTabs(wikiSite, searchTerm)
                    }, async {
                        AppDatabase.instance.historyEntryWithImageDao().findHistoryItem(wikiSite, searchTerm)
                    }, async {
                        AppDatabase.instance.readingListPageDao().findPageForSearchQueryInAnyList(wikiSite, searchTerm)
                    }).awaitAll().forEach {
                        localResults.addAll(it.results.take(1))
                    }
                }
            }
            val prefixResponse = ServiceFactory.get(wikiSite).prefixSearchResponse(searchTerm, batchSize, 0)
            lastXSearchIdPrefix = prefixResponse.headers()["x-search-id"]
            response = prefixResponse.body()
            currentContinuation = 0
        }

        lexicalResults.addAll(SearchResultsViewModel.buildList(response, invokeSource, wikiSite, SearchResult.SearchResultType.PREFIX))

        if (lexicalResults.size < batchSize) {
            val fullTextResponse = ServiceFactory.get(wikiSite)
                .fullTextSearchResponse(searchTerm, batchSize, currentContinuation)
            lastXSearchIdFullText = fullTextResponse.headers()["x-search-id"]
            response = fullTextResponse.body()
            currentContinuation = response?.continuation?.gsroffset
            lexicalResults.addAll(SearchResultsViewModel.buildList(response, invokeSource, wikiSite, SearchResult.SearchResultType.FULL_TEXT))
        }

        if (localResults.isEmpty() && lexicalResults.isEmpty() && response?.continuation == null) {
            countsPerLanguageCode.clear()
            WikipediaApp.instance.languageState.appLanguageCodes.forEach { langCode ->
                var countResultSize = 0
                if (langCode != languageCode) {
                    val prefixSearchResponse = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                        .prefixSearch(searchTerm, batchSize, 0)
                    prefixSearchResponse.query?.pages?.let {
                        countResultSize = it.size
                    }
                    if (countResultSize == 0) {
                        val fullTextSearchResponse = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                            .fullTextSearch(searchTerm, batchSize, null)
                        countResultSize = fullTextSearchResponse.query?.pages?.size ?: 0
                    }
                }
                countsPerLanguageCode.add(langCode to countResultSize)
            }
        }

        return StandardSearchResults(
            results = lexicalResults.distinctBy { it.pageId },
            continuation = currentContinuation,
            xSearchIdPrefix = lastXSearchIdPrefix,
            xSearchIdFullText = lastXSearchIdFullText,
            localResults = localResults.distinctBy { it.pageTitle.prefixedText }
        )
    }

    private fun getSearchResultsFromTabs(wikiSite: WikiSite, searchTerm: String): SearchResults {
        WikipediaApp.instance.tabList.forEach { tab ->
            tab.backStackPositionTitle?.let {
                if (wikiSite == it.wikiSite && StringUtil.fromHtml(it.displayText).contains(searchTerm, true)) {
                    return SearchResults(mutableListOf(SearchResult(it, SearchResult.SearchResultType.TAB_LIST)))
                }
            }
        }
        return SearchResults()
    }
}

data class StandardSearchResults(
    var results: List<SearchResult>,
    val continuation: Int?,
    val xSearchIdPrefix: String? = null,
    val xSearchIdFullText: String? = null,
    val xSearchIdSemantic: String? = null,
    // Tab, history and reading list matches, shown above the results and never merged with them.
    val localResults: List<SearchResult> = emptyList()
)
