package org.wikipedia.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.restbase.Attribution
import org.wikipedia.search.interleave.AttributionKey
import org.wikipedia.search.interleave.InterleaveSearchAbTest
import org.wikipedia.search.interleave.InterleaveSearchRepository
import org.wikipedia.search.interleave.attributionKey
import org.wikipedia.settings.Prefs
import kotlin.time.Duration.Companion.milliseconds

class SearchResultsViewModel : ViewModel() {
    private val interleaveSearchAbTest = InterleaveSearchAbTest()
    private val interleaveSearchEnabledPrefUpdates = Prefs.observeKeys(
        R.string.preference_key_interleave_search_enabled,
        R.string.preference_key_interleave_search_is_test_active,
        R.string.preference_key_interleave_search_override_supported_language
    )
    private val standardSearchRepository = StandardSearchRepository()
    private val interleaveSearchRepository = InterleaveSearchRepository(lexicalRepository = standardSearchRepository)
    private val batchSize = 10
    private val delayMillis = 200L
    var countsPerLanguageCode = mutableListOf<Pair<String, Int>>()
    var isSemanticSearchFeedbackShown = false
    var isFromRecentSearch = false

    lateinit var invokeSource: Constants.InvokeSource

    private val _searchTerm = MutableStateFlow<String?>(null)
    var searchTerm = _searchTerm.asStateFlow()

    private val _searchTermForLogging = MutableSharedFlow<String?>()
    var searchTermForLogging = _searchTermForLogging.asSharedFlow()
    private val _lexicalResultsForLogging = MutableStateFlow<List<SearchResult>?>(null)
    var lexicalResultsForLogging = _lexicalResultsForLogging.asStateFlow()

    private var _languageCode = MutableStateFlow(WikipediaApp.instance.languageState.appLanguageCode)
    var languageCode = _languageCode.asStateFlow()

    private var _refreshSearchResults = MutableStateFlow(0)

    private var lastXSearchIdPrefix = ""
    private var lastXSearchIdFullText = ""
    var lastXSearchIdSemantic = ""
    private var isInterleaveSearchExposureSent = false

    private val _attributions = MutableStateFlow<Map<AttributionKey, Attribution>>(emptyMap())
    val attributions = _attributions.asStateFlow()
    private val requestedAttributions = mutableSetOf<AttributionKey>()

    private val languageCodeWithInterleaveSearchEnabled = combine(
        _languageCode,
        interleaveSearchEnabledPrefUpdates
    ) { currentLanguageCode, _ ->
        currentLanguageCode to interleaveSearchAbTest.isInterleaveSearchEnabled(currentLanguageCode)
    }.distinctUntilChanged()

    val isInterleaveSearchEnabled = languageCodeWithInterleaveSearchEnabled
        .map { (_, isEnabled) -> isEnabled }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            interleaveSearchAbTest.isInterleaveSearchEnabled(_languageCode.value)
        )

    @OptIn(
        FlowPreview::class,
        ExperimentalCoroutinesApi::class
    ) // TODO: revisit if the debounce method changed.
    val searchResultsFlow =
        combine(_searchTerm.debounce(delayMillis.milliseconds), languageCodeWithInterleaveSearchEnabled, _refreshSearchResults) { term, (lang, isInterleaveEnabled), _ ->
            Triple(term, lang, isInterleaveEnabled)
        }.flatMapLatest { (term, lang, isInterleaveEnabled) ->
            // A language change or refresh can re-emit the previous debounced term before the new one settles.
            if (term == _searchTerm.value) {
                _searchTermForLogging.emit(term)
                if (!term.isNullOrBlank()) {
                    maybeSendInterleaveSearchExposure(lang)
                }
            }
            val repository = if (isInterleaveEnabled && invokeSource != Constants.InvokeSource.PLACES) interleaveSearchRepository else standardSearchRepository
            Pager(PagingConfig(pageSize = batchSize)) {
                SearchResultsPagingSource(
                    searchTerm = term,
                    languageCode = lang,
                    countsPerLanguageCode = countsPerLanguageCode,
                    invokeSource = invokeSource,
                    repository = repository,
                    onFirstPageLoaded = { result ->
                        lastXSearchIdPrefix = result.xSearchIdPrefix.orEmpty()
                        lastXSearchIdFullText = result.xSearchIdFullText.orEmpty()
                        result.xSearchIdSemantic?.let { lastXSearchIdSemantic = it }
                        _lexicalResultsForLogging.value = result.results
                    }
                )
            }.flow
        }.cachedIn(viewModelScope)

    fun updateSearchTerm(term: String?) {
        if (_searchTerm.value != term) {
            lastXSearchIdSemantic = ""
        }
        _searchTerm.value = term
    }

    fun updateLanguageCode(code: String) {
        if (_languageCode.value != code) {
            lastXSearchIdSemantic = ""
        }
        _languageCode.value = code
    }

    fun refreshSearchResults() {
        _refreshSearchResults.value += 1
    }

    fun getStandardEventActionContext(result: SearchResult? = null): Map<String, Any> {
        return buildMap {
            put("search_id_pre", lastXSearchIdPrefix)
            put("search_id_ful", lastXSearchIdFullText)
            if (result != null) {
                put("position", result.indexInApiCall)
                put("type", result.type)
            }
        }
    }

    fun getSemanticEventActionContext(result: SearchResult? = null): Map<String, Any> {
        return buildMap {
            put("search_id_pre", lastXSearchIdPrefix)
            put("search_id_ful", lastXSearchIdFullText)
            put("search_id_sem", lastXSearchIdSemantic)
            if (result != null) {
                put("position", result.indexInApiCall)
                put("type", result.type)
            }
        }
    }

    // Sent for both groups at the point where the semantic search entry card can appear.
    private fun maybeSendInterleaveSearchExposure(languageCode: String) {
        if (isInterleaveSearchExposureSent || invokeSource == Constants.InvokeSource.PLACES ||
            !Prefs.isInterleaveSearchEnabled || !interleaveSearchAbTest.isInterleaveSearchEligible(languageCode)) {
            return
        }
        isInterleaveSearchExposureSent = true
        interleaveSearchAbTest.maybeSendExposureEvent()
    }

    fun loadAttribution(result: SearchResult) {
        val key = result.attributionKey
        if (!requestedAttributions.add(key)) {
            return
        }
        viewModelScope.launch {
            val attribution = interleaveSearchRepository.getAttribution(result)
            _attributions.update { it + (key to attribution) }
        }
    }

    fun disableSemanticSearch() {
        // TODO: validate this in other ticket or remove before releasing to production
        Prefs.isInterleaveSearchEnabled = false
    }

    class SearchResultsPagingSource(
        private val searchTerm: String?,
        private val languageCode: String?,
        private var countsPerLanguageCode: MutableList<Pair<String, Int>>,
        private var invokeSource: Constants.InvokeSource,
        private val repository: SearchRepository<StandardSearchResults>,
        private val onFirstPageLoaded: (StandardSearchResults) -> Unit
    ) : PagingSource<Int, SearchResult>() {
        private var localResultTitles = emptySet<String>()

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
                    isPrefixSearch = params.key == null
                )

                if (params.key == null) {
                    localResultTitles = result.localResults.map { it.pageTitle.prefixedText }.toSet()
                    onFirstPageLoaded(result)
                }

                // Local results have no page ID, so their duplicates are matched by title and hidden on every page.
                val pageResults = result.localResults + result.results.filterNot { it.pageTitle.prefixedText in localResultTitles }

                if (params.key == null && pageResults.isEmpty() && result.continuation == null) {
                    countsPerLanguageCode.clear()
                    countsPerLanguageCode.addAll(loadCountsPerLanguageCode(searchTerm, languageCode, params.loadSize))
                }

                return LoadResult.Page(pageResults, null, result.continuation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        private suspend fun loadCountsPerLanguageCode(searchTerm: String, languageCode: String, batchSize: Int): List<Pair<String, Int>> {
            return WikipediaApp.instance.languageState.appLanguageCodes.map { langCode ->
                if (langCode == languageCode) {
                    return@map langCode to 0
                }
                val service = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                var count = service.prefixSearch(searchTerm, batchSize, 0).query?.pages?.size ?: 0
                if (count == 0) {
                    count = service.fullTextSearch(searchTerm, batchSize, null).query?.pages?.size ?: 0
                }
                langCode to count
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
            type: SearchResult.SearchResultType
        ): List<SearchResult> {
            return response?.query?.pages?.let { list ->
                (if (invokeSource == Constants.InvokeSource.PLACES)
                    list.filter { it.coordinates != null } else list).sortedBy { it.index }
                    .map { SearchResult(it, wikiSite, it.coordinates, type, indexInApiCall = it.index) }
            } ?: emptyList()
        }
    }
}
