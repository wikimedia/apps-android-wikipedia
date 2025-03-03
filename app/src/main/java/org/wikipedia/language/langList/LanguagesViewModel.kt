package org.wikipedia.language.langList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.util.log.L

class LanguagesViewModel : ViewModel() {

    data class LanguageListItem(
        val code: String,
        val localizedName: String = "",
        val canonicalName: String = "",
        val headerText: String = "",
        val isHeader: Boolean = false
    )

    data class LanguageListUiState(
        val searchTerm: String = "",
        val languagesItems: List<LanguageListItem> = emptyList(),
        val error: String? = null,
        val isSiteInfoLoaded: Boolean = false
    )

    private val suggestedLanguageCodes = WikipediaApp.instance.languageState.remainingSuggestedLanguageCodes
    private val nonSuggestedLanguageCodes = WikipediaApp.instance.languageState.appMruLanguageCodes.filterNot {
        suggestedLanguageCodes.contains(it) || WikipediaApp.instance.languageState.appLanguageCodes.contains(it)
    }

    private val _siteInfoList = MutableStateFlow<List<SiteMatrix.SiteInfo>>(emptyList())

    // UI state exposed to Compose
    private val _uiState = MutableStateFlow(LanguageListUiState())
    val uiState: StateFlow<LanguageListUiState> = _uiState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiState.update {
            it.copy(
                error = throwable.localizedMessage ?: "An error occurred"
            )
        }
    }

    init {
        // immediately populate with locally available data
        populateInitialLanguageList()

        // fetch site matrix
        fetchSiteMatrix()
    }

    private fun populateInitialLanguageList() {
        updateSearchTerm("", false)
    }

    private fun fetchSiteMatrix() {
        viewModelScope.launch(handler) {
            try {
                val siteMatrix = ServiceFactory.get(WikipediaApp.instance.wikiSite).getSiteMatrix()
                val sites = SiteMatrix.getSites(siteMatrix)
                _siteInfoList.value = sites

                // site is loaded so marking it
                _uiState.update { it.copy(isSiteInfoLoaded = true) }

                // update the list
                updateSearchTerm(_uiState.value.searchTerm, siteInfoAvailable = true)
            } catch (e: Exception) { }
        }
    }

    fun updateSearchTerm(term: String, siteInfoAvailable: Boolean = _uiState.value.isSiteInfoLoaded) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    searchTerm = term,
                    languagesItems = getFilteredLanguageList(term, siteInfoAvailable),
                )
            }
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
