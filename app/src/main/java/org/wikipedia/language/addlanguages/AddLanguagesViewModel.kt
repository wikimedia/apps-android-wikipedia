package org.wikipedia.language.addlanguages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L

class AddLanguagesViewModel : ViewModel() {
    private val suggestedLanguageCodes = WikipediaApp.instance.languageState.remainingSuggestedLanguageCodes
    private val nonSuggestedLanguageCodes = WikipediaApp.instance.languageState.appMruLanguageCodes.filterNot {
        suggestedLanguageCodes.contains(it) || WikipediaApp.instance.languageState.appLanguageCodes.contains(it)
    }

    private val _siteInfoList = MutableStateFlow<List<SiteMatrix.SiteInfo>>(emptyList())

    // UI state exposed to Compose
    private val _uiState = MutableStateFlow<UiState<List<LanguageListItem>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiState.value = UiState.Error(throwable)
    }

    private var fetchJob: Job? = null

    init {
      fetchAllData()
    }

    fun fetchAllData() {
        fetchJob?.cancel()

        // Set to loading state
        _uiState.value = UiState.Loading

        // fetch site matrix
        fetchJob = viewModelScope.launch(handler) {
            _uiState.value = UiState.Loading
            val siteMatrix = ServiceFactory.get(WikipediaApp.instance.wikiSite).getSiteMatrix()
            val sites = SiteMatrix.getSites(siteMatrix)
            _siteInfoList.value = sites

            // isActive checks if the job is still active
            // does not update the list if the coroutine has been cancelled
            if (isActive) {
                updateSearchTerm("")
            }
        }
    }

    fun updateSearchTerm(term: String) {
        viewModelScope.launch(handler) {
            _uiState.value = UiState.Success(getFilteredLanguageList(term))
        }
    }

    private fun getFilteredLanguageList(
        searchTerm: String,
    ): List<LanguageListItem> {
        val results = mutableListOf<LanguageListItem>()
        val filter = StringUtils.stripAccents(searchTerm)

        addFilteredLanguageListItems(
            filter,
            suggestedLanguageCodes,
            WikipediaApp.instance.getString(R.string.languages_list_suggested_text),
            results
        )

        addFilteredLanguageListItems(
            filter,
            nonSuggestedLanguageCodes,
            WikipediaApp.instance.getString(R.string.languages_list_all_text),
            results
        )

        return results
    }

    private fun addFilteredLanguageListItems(
        filter: String,
        codes: List<String>,
        headerText: String,
        results: MutableList<LanguageListItem>,
    ) {
        var first = true
        for (code in codes) {
            val localizedName = WikipediaApp.instance.languageState.getAppLanguageLocalizedName(code).orEmpty()
            val canonicalName = getCanonicalName(code)

            if (filter.isEmpty() || code.contains(filter, true) ||
                StringUtils.stripAccents(localizedName).contains(filter, true) ||
                StringUtils.stripAccents(canonicalName).contains(filter, true)) {

                if (first) {
                    results.add(
                        LanguageListItem(
                            code = "",
                            headerText = headerText,
                        )
                    )
                    first = false
                }
                results.add(
                    LanguageListItem(
                        code = code,
                        canonicalName = canonicalName,
                        localizedName = localizedName
                    )
                )
            }
        }
    }

    private fun getCanonicalName(code: String): String {
        // Only attempt to get canonical name if the site is available
        return _siteInfoList.value.find { it.code == code }?.localname.orEmpty()
            .ifEmpty { WikipediaApp.instance.languageState.getAppLanguageCanonicalName(code).orEmpty() }
    }
}

data class LanguageListItem(
    val code: String,
    val localizedName: String = "",
    val canonicalName: String = "",
    val headerText: String = "",
)
