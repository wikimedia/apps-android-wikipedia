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

class StandardSearchRepository: SearchRepository<StandardSearchResults> {
    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean,
        countsPerLanguageCode: MutableList<Pair<String, Int>>?
    ): StandardSearchResults {
        val wikiSite = WikiSite.forLanguageCode(languageCode)
        val resultList = mutableListOf<SearchResultPage>()
        var response: MwQueryResponse? = null
        var currentContinuation = continuation

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
                        resultList.addAll(it.results.take(1))
                    }
                }
            }
            response = ServiceFactory.get(wikiSite).prefixSearch(searchTerm, batchSize, 0)
        }

        resultList.addAll(response?.query?.pages?.let { list ->
            (if (invokeSource == Constants.InvokeSource.PLACES)
                list.filter { it.coordinates != null } else list).sortedBy { it.index }
                .map { SearchResultPage(it, wikiSite, it.coordinates) }
        } ?: emptyList())

        if (resultList.size < batchSize) {
            response = ServiceFactory.get(wikiSite)
                .fullTextSearch(searchTerm, batchSize, currentContinuation)
            currentContinuation = response.continuation?.gsroffset

            resultList.addAll(response.query?.pages?.let { list ->
                (if (invokeSource == Constants.InvokeSource.PLACES)
                    list.filter { it.coordinates != null } else list).sortedBy { it.index }
                    .map { SearchResultPage(it, wikiSite, it.coordinates) }
            } ?: emptyList())
        }

        if (resultList.isEmpty() && response?.continuation == null) {
            countsPerLanguageCode?.clear()
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
                countsPerLanguageCode?.add(langCode to countResultSize)
            }
        }

        return StandardSearchResults(
            results = resultList.distinctBy { it.pageTitle.prefixedText }.toMutableList(),
            continuation = currentContinuation
        )
    }

    private fun getSearchResultsFromTabs(wikiSite: WikiSite, searchTerm: String): SearchResults {
        WikipediaApp.instance.tabList.forEach { tab ->
            tab.backStackPositionTitle?.let {
                if (wikiSite == it.wikiSite && StringUtil.fromHtml(it.displayText).contains(searchTerm, true)) {
                    return SearchResults(mutableListOf(SearchResultPage(it, SearchResultType.TAB_LIST)))
                }
            }
        }
        return SearchResults()
    }
}

data class StandardSearchResults(
    var results: List<SearchResultPage>,
    val continuation: Int?
)