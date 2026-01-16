package org.wikipedia.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L

class HybridSearchResultsViewModel : ViewModel() {

    private val batchSize = 3
    private val delayMillis = 200L

    lateinit var invokeSource: Constants.InvokeSource

    private val _searchTerm = MutableStateFlow<String?>(null)
    var searchTerm = _searchTerm.asStateFlow()

    private var _languageCode = MutableStateFlow<String?>(null)
    var languageCode = _languageCode.asStateFlow()

    private var _searchResultsState = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Loading)
    val searchResultsState = _searchResultsState.asStateFlow()

    private var _refreshSearchResults = MutableStateFlow(0)

    @OptIn(FlowPreview::class)
    fun fetchHybridSearch() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _searchResultsState.value = UiState.Error(throwable)
        }) {
            combine(_searchTerm.debounce(delayMillis), _languageCode, _refreshSearchResults) {
                term: String?, lang: String?, _ -> term to lang
            }.collectLatest { (term, lang) ->
                if (term.isNullOrEmpty() || lang.isNullOrEmpty()) {
                    _searchResultsState.value = UiState.Error(IllegalArgumentException("Search term or language code is null or empty"))
                    return@collectLatest
                }

                _searchResultsState.value = UiState.Loading
                val repository = HybridSearchRepository()
                val result = repository.search(
                    searchTerm = term,
                    languageCode = lang,
                    invokeSource = invokeSource,
                    batchSize = batchSize
                )

                _searchResultsState.value = UiState.Success(result.results)
            }
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
}
