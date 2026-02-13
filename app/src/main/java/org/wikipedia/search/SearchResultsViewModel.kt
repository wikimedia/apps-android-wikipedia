package org.wikipedia.search

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState

class SearchResultsViewModel : ViewModel() {

    private val batchSize = 10
    private val delayMillis = 200L
    var countsPerLanguageCode = mutableListOf<Pair<String, Int>>()

    lateinit var invokeSource: Constants.InvokeSource

    private val _searchTerm = MutableStateFlow<String?>(null)
    var searchTerm = _searchTerm.asStateFlow()

    private var _languageCode = MutableStateFlow(WikipediaApp.instance.languageState.appLanguageCode)
    var languageCode = _languageCode.asStateFlow()

    private var _hybridSearchResultState = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Loading)
    val hybridSearchResultState = _hybridSearchResultState.asStateFlow()

    private var _hybridSearchPromptState = MutableStateFlow<UiState<String?>>(UiState.Loading)
    val hybridSearchPromptState = _hybridSearchPromptState.asStateFlow()

    private var _refreshSearchResults = MutableStateFlow(0)

    val getTestGroup get() = HybridSearchAbCTest().getGroupName()

    private val semanticSearchService: SemanticSearchService = ServiceFactory[WikiSite(SemanticSearchService.BASE_URL), SemanticSearchService.BASE_URL, SemanticSearchService::class.java]
    val isHybridSearchExperimentOn get() = HybridSearchAbCTest().isHybridSearchEnabled(languageCode.value)

    @OptIn(
        FlowPreview::class,
        ExperimentalCoroutinesApi::class
    ) // TODO: revisit if the debounce method changed.
    val searchResultsFlow =
        combine(_searchTerm.debounce(delayMillis), _languageCode, _refreshSearchResults) { term, lang, _ ->
            Pair(term, lang)
        }.flatMapLatest { (term, lang) ->
            val repository = StandardSearchRepository()
            Pager(PagingConfig(pageSize = batchSize)) {
                SearchResultsPagingSource(
                    searchTerm = term,
                    languageCode = lang,
                    countsPerLanguageCode = countsPerLanguageCode,
                    invokeSource = invokeSource,
                    repository = repository
                )
            }.flow
        }.cachedIn(viewModelScope)

    @OptIn(FlowPreview::class)
    fun loadHybridSearchResults() {
       viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
           _hybridSearchResultState.value = UiState.Error(throwable)
       }) {
            _hybridSearchResultState.value = UiState.Loading
            val lexicalBatchSize = 3
            val semanticBatchSize = 3

            val term = _searchTerm.value
            val lang = _languageCode.value

            if (term.isNullOrEmpty() || lang.isEmpty()) {
                _hybridSearchResultState.value = UiState.Success(emptyList())
                return@launch
            }

            val wikiSite = WikiSite.forLanguageCode(lang)

            val lexicalDeferred = async {
                runCatching {
                    val lexicalSearchResults = mutableListOf<SearchResult>()
                    // prefix + fulltext search results for at most 3 results.
                    var response = ServiceFactory.get(wikiSite).prefixSearch(term, lexicalBatchSize, 0)
                    lexicalSearchResults.addAll(buildList(response, invokeSource, wikiSite))
                    if (lexicalSearchResults.size < lexicalBatchSize) {
                        response = ServiceFactory.get(wikiSite).fullTextSearch(term, lexicalBatchSize, 0)
                        lexicalSearchResults.addAll(buildList(response, invokeSource, wikiSite))
                    }
                    lexicalSearchResults
                }
            }

            val semanticDeferred = async {
                runCatching {
                    val tableName = when (lang) {
                        "el" -> "elwiki_sections"
                        else -> ""
                    }
                    val response = semanticSearchService.search(query = term, count = semanticBatchSize, table = tableName, includeText = true)
                    val infoResponse = ServiceFactory.get(wikiSite).getInfoByPageIdsOrTitles(titles = response.results.joinToString("|") { it.title })
                    buildList(response, wikiSite, SearchResult.SearchResultType.SEMANTIC).also { list ->
                        for (result in list) {
                            val page = infoResponse.query?.pages?.find { StringUtil.addUnderscores(it.title) == result.pageTitle.prefixedText }
                            result.pageTitle.thumbUrl = page?.thumbUrl()
                            result.pageTitle.description = page?.description
                        }
                    }
                }
            }

            val lexicalResult = lexicalDeferred.await()
            val semanticResult = semanticDeferred.await()

           val biographyPromptResponse = loadBiographySearchPrompt(wikiSite, semanticResult.getOrElse { emptyList() }).filter { it.value }

           if (biographyPromptResponse.isNotEmpty()) {
               // TODO: verify if we need to get the rest of the results
               _hybridSearchPromptState.value = UiState.Success(biographyPromptResponse.firstNotNullOf { it.key })
           }

            if (lexicalResult.isFailure && semanticResult.isFailure) {
                _hybridSearchResultState.value = UiState.Error(Throwable())
                return@launch
            }

            val lexicalList = lexicalResult.getOrElse { emptyList() }
                .distinctBy { it.pageTitle.prefixedText }
            val semanticList = semanticResult.getOrElse { emptyList() }

            _hybridSearchResultState.value = UiState.Success(lexicalList + semanticList)
        }
    }

    suspend fun loadBiographySearchPrompt(wikiSite: WikiSite, searchResults: List<SearchResult>): Map<String, Boolean> {
        return withContext(Dispatchers.IO) {

            val pagePropsResponse = ServiceFactory.get(wikiSite)
                .getPageProps(searchResults.map { it.pageTitle.prefixedText }.joinToString { "|" })
            val qNumbers = pagePropsResponse.query?.pages?.mapNotNull { it.pageProps?.wikiBaseItem }
                ?: emptyList()

            val finalResponse = mutableMapOf<String, Boolean>()

            // TODO: use async
            qNumbers.forEachIndexed { index, qNumber ->
                val claimsResponse = ServiceFactory.get(Constants.wikidataWikiSite)
                    .getClaims(qNumber, "P31") // P31 = instance of
                val isBiography =
                    claimsResponse.claims["P31"]?.any { it.mainSnak?.dataValue?.value() == "Q5" }
                        ?: false // Q5 = human
                finalResponse[searchResults[index].pageTitle.prefixedText] = isBiography
            }

            finalResponse
        }
    }

    fun updateSearchTerm(term: String?) {
        _searchTerm.value = term
    }

    fun updateLanguageCode(code: String) {
        _languageCode.value = code
    }

    fun refreshSearchResults() {
        _refreshSearchResults.value += 1
    }

    fun resetHybridSearchState() {
        _hybridSearchResultState.value = UiState.Loading
    }

    class SearchResultsPagingSource(
        private val searchTerm: String?,
        private val languageCode: String?,
        private var countsPerLanguageCode: MutableList<Pair<String, Int>>,
        private var invokeSource: Constants.InvokeSource,
        private val repository: SearchRepository<StandardSearchResults>,
    ) : PagingSource<Int, SearchResult>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
            return try {
                if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                val result = repository.search(
                    searchTerm = searchTerm,
                    languageCode = languageCode,
                    invokeSource = invokeSource,
                    continuation = params.key,
                    batchSize = params.loadSize,
                    isPrefixSearch = params.key == null,
                    countsPerLanguageCode = countsPerLanguageCode
                )

                return LoadResult.Page(
                    result.results,
                    null,
                    result.continuation
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
            return null
        }
    }

    companion object {
        fun buildList(
            response: MwQueryResponse?,
            invokeSource: Constants.InvokeSource,
            wikiSite: WikiSite,
            type: SearchResult.SearchResultType = SearchResult.SearchResultType.SEARCH
        ): List<SearchResult> {
            return response?.query?.pages?.let { list ->
                (if (invokeSource == Constants.InvokeSource.PLACES)
                    list.filter { it.coordinates != null } else list).sortedBy { it.index }
                    .map { SearchResult(it, wikiSite, it.coordinates, type) }
            } ?: emptyList()
        }

        fun buildList(
            response: SemanticSearchResults,
            wikiSite: WikiSite,
            type: SearchResult.SearchResultType = SearchResult.SearchResultType.SEARCH
        ): List<SearchResult> {
            return response.results.map { result ->
                SearchResult(PageTitle.titleForUri(result.url.toUri(), wikiSite), searchResultType = type, snippet = postProcessSectionText(result.sectionText))
            }
        }

        fun postProcessSectionText(text: String): String {
            // TODO: remove this when server-side parsing is done.
            val bold = Regex("'''(.*?)'''", RegexOption.DOT_MATCHES_ALL)
            val italic = Regex("''(.*?)''", RegexOption.DOT_MATCHES_ALL)
            val emptyParens = Regex("""\([\s,.;]*\)""")
            return text
                .replace(emptyParens, "")
                .replace(bold, "<b>\$1</b>")
                .replace(italic, "<i>\$1</i>")
        }
    }
}
