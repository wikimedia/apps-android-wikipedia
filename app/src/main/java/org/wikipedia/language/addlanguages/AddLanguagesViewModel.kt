package org.wikipedia.language.addlanguages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.util.log.L

class AddLanguagesViewModel : ViewModel() {
    private val suggestedLanguageCodes = WikipediaApp.instance.languageState.remainingSuggestedLanguageCodes
    private val nonSuggestedLanguageCodes = WikipediaApp.instance.languageState.appMruLanguageCodes.filterNot {
        suggestedLanguageCodes.contains(it) || WikipediaApp.instance.languageState.appLanguageCodes.contains(it)
    }

    private val _siteInfoList = MutableStateFlow<List<SiteMatrix.SiteInfo>>(emptyList())

    // UI state exposed to Compose
    private val _uiState = MutableStateFlow<LanguageListUiState>(LanguageListUiState.Loading)
    val uiState: StateFlow<LanguageListUiState> = _uiState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiState.value = LanguageListUiState.Error(throwable)
    }

    private var fetchJob: Job? = null

    init {
      fetchAllData()
    }

    fun fetchAllData() {
        fetchJob?.cancel()

        // Set to loading state
        _uiState.value = LanguageListUiState.Loading

        // fetch site matrix
        fetchJob = viewModelScope.launch(handler) {
            _uiState.value = LanguageListUiState.Loading
            val siteMatrix = ServiceFactory.get(WikipediaApp.instance.wikiSite).getSiteMatrix()
            val sites = SiteMatrix.getSites(siteMatrix)
            _siteInfoList.value = sites

            // update the list
            if (isActive) {
                updateSearchTerm(getCurrentSearchTerm(), siteInfoAvailable = true)
            }
        }
    }

    fun updateSearchTerm(term: String, siteInfoAvailable: Boolean = isSiteInfoLoaded()) {
        if (_uiState.value is LanguageListUiState.Loading) {
            return
        }

        viewModelScope.launch {
            _uiState.value = LanguageListUiState.Success(
                searchTerm = term,
                languagesItems = getFilteredLanguageList(term, siteInfoAvailable),
                isSiteInfoLoaded = siteInfoAvailable
            )
        }
    }

    private fun isSiteInfoLoaded(): Boolean {
        return when (val state = _uiState.value) {
            is LanguageListUiState.Success -> state.isSiteInfoLoaded
            else -> false
        }
    }

    private fun getCurrentSearchTerm(): String {
        return when (val state = _uiState.value) {
            is LanguageListUiState.Success -> state.searchTerm
            else -> ""
        }
    }

    private fun getFilteredLanguageList(
        searchTerm: String,
        siteInfoAvailable: Boolean
    ): List<LanguageListItem> {
        val results = mutableListOf<LanguageListItem>()
        val filter = StringUtils.stripAccents(searchTerm)

        addFilteredLanguageListItems(
            filter,
            suggestedLanguageCodes,
            WikipediaApp.instance.getString(R.string.languages_list_suggested_text),
            results,
            siteInfoAvailable
        )

        addFilteredLanguageListItems(
            filter,
            nonSuggestedLanguageCodes,
            WikipediaApp.instance.getString(R.string.languages_list_all_text),
            results,
            siteInfoAvailable
        )

        return results
    }

    private fun addFilteredLanguageListItems(
        filter: String,
        codes: List<String>,
        headerText: String,
        results: MutableList<LanguageListItem>,
        siteInfoAvailable: Boolean
    ) {
        var first = true
        for (code in codes) {
            val localizedName = StringUtils.stripAccents(
                WikipediaApp.instance.languageState.getAppLanguageLocalizedName(code).orEmpty()
            )

            // Only attempt to get canonical name if the site is available
            val canonicalName = if (siteInfoAvailable) {
                StringUtils.stripAccents(getCanonicalName(code))
            } else ""

            if (filter.isEmpty() || code.contains(filter, true) ||
                localizedName.contains(filter, true) ||
                canonicalName.contains(filter, true)) {

                if (first) {
                    results.add(
                        LanguageListItem(
                        code = "",
                        headerText = headerText,
                        isHeader = true
                    )
                    )
                    first = false
                }
                results.add(
                    LanguageListItem(
                        code = code,
                        canonicalName = canonicalName
                    )
                )
            }
        }
    }

    private fun getCanonicalName(code: String): String {
        return _siteInfoList.value.find { it.code == code }?.localname.orEmpty()
            .ifEmpty { WikipediaApp.instance.languageState.getAppLanguageCanonicalName(code).orEmpty() }
    }
}

data class LanguageListItem(
    val code: String,
    val localizedName: String = "",
    val canonicalName: String = "",
    val headerText: String = "",
    val isHeader: Boolean = false
)

sealed interface LanguageListUiState {
    data object Loading : LanguageListUiState
    data class Error(val error: Throwable) : LanguageListUiState
    data class Success(
        val searchTerm: String,
        val languagesItems: List<LanguageListItem> = emptyList(),
        val isSiteInfoLoaded: Boolean = false
    ) : LanguageListUiState
}
