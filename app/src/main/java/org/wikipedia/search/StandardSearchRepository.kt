package org.wikipedia.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.search.interleave.EmptyLeadResolver
import org.wikipedia.util.StringUtil

class StandardSearchRepository(
    private val includesLeads: Boolean = false
) : SearchRepository<StandardSearchResults> {
    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean
    ): StandardSearchResults {
        val wikiSite = WikiSite.forLanguageCode(languageCode)
        // Local results are kept out of the lexical list so that it is built by the shared lexical list spec, like on iOS.
        val localResults = mutableListOf<SearchResult>()
        val lexicalResults = mutableListOf<SearchResult>()
        var response: MwQueryResponse? = null
        var currentContinuation = continuation
        var lastXSearchIdPrefix: String? = null
        var lastXSearchIdFullText: String? = null
        // Leads come with the search as HTML intros, so superscripts and italics survive. Null values are left out of the request.
        val leadProp = if (includesLeads) "|extracts" else ""
        val exIntro = true.takeIf { includesLeads }
        val exChars = LEAD_EXTRACT_CHARS.takeIf { includesLeads }
        // Full-text props already include pageprops. Prefix results need them too, so EmptyLeadResolver can skip disambiguation pages.
        val prefixLeadProp = if (includesLeads) "$leadProp|pageprops" else ""

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
            val prefixResponse = ServiceFactory.get(wikiSite).prefixSearchResponse(
                searchTerm, batchSize, 0,
                prop = Service.PREFIX_SEARCH_PROPS + prefixLeadProp,
                exIntro = exIntro,
                exChars = exChars
            )
            lastXSearchIdPrefix = prefixResponse.headers()["x-search-id"]
            response = prefixResponse.body()
            currentContinuation = 0
        }

        lexicalResults.addAll(SearchResultsViewModel.buildList(response, invokeSource, wikiSite, SearchResult.SearchResultType.PREFIX))

        if (lexicalResults.size < batchSize) {
            val fullTextResponse = ServiceFactory.get(wikiSite).fullTextSearchResponse(
                searchTerm, batchSize, currentContinuation,
                prop = Service.FULL_TEXT_SEARCH_PROPS + leadProp,
                exIntro = exIntro,
                exChars = exChars
            )
            lastXSearchIdFullText = fullTextResponse.headers()["x-search-id"]
            response = fullTextResponse.body()
            currentContinuation = response?.continuation?.gsroffset
            lexicalResults.addAll(SearchResultsViewModel.buildList(response, invokeSource, wikiSite, SearchResult.SearchResultType.FULL_TEXT))
        }

        val results = lexicalResults.distinctBy { it.pageId }
            .let { if (includesLeads) EmptyLeadResolver.resolve(wikiSite, it) else it }

        return StandardSearchResults(
            results = results,
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

    companion object {
        // fetch enough chars to fill 8 lines on the card for different devices and orientations
        private const val LEAD_EXTRACT_CHARS = 800
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
