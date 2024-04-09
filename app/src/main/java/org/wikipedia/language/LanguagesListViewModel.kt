package org.wikipedia.language

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L

class LanguagesListViewModel : ViewModel() {

    private val suggestedLanguageCodes = WikipediaApp.instance.languageState.remainingAvailableLanguageCodes
    private val nonSuggestedLanguageCodes = WikipediaApp.instance.languageState.appMruLanguageCodes.filterNot {
            suggestedLanguageCodes.contains(it) || WikipediaApp.instance.languageState.appLanguageCodes.contains(it)
        }

    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
    }

    val siteListData = MutableLiveData<Resource<List<SiteMatrix.SiteInfo>>>()

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch(handler) {
            val siteMatrix = ServiceFactory.get(WikipediaApp.instance.wikiSite).getSiteMatrix()
            val sites = SiteMatrix.getSites(siteMatrix)
            siteListData.postValue(Resource.Success(sites))
        }
    }

    fun getListBySearchTerm(context: Context, searchTerm: String?): List<LanguageListItem> {
        val results = mutableListOf<LanguageListItem>()
        val filter = StringUtils.stripAccents(searchTerm.orEmpty())

        addFilteredLanguageListItems(filter, suggestedLanguageCodes,
                context.getString(R.string.languages_list_suggested_text), results)

        addFilteredLanguageListItems(filter, nonSuggestedLanguageCodes,
                context.getString(R.string.languages_list_all_text), results)

        return results
    }

    private fun addFilteredLanguageListItems(filter: String, codes: List<String>, headerText: String,
                                             results: MutableList<LanguageListItem>) {
        var first = true
        for (code in codes) {
            val localizedName = StringUtils.stripAccents(WikipediaApp.instance.languageState.getAppLanguageLocalizedName(code).orEmpty())
            val canonicalName = StringUtils.stripAccents(getCanonicalName(code).orEmpty())
            if (filter.isEmpty() || code.contains(filter, true) ||
                    localizedName.contains(filter, true) ||
                    canonicalName.contains(filter, true)) {
                if (first) {
                    results.add(LanguageListItem(headerText, true))
                    first = false
                }
                results.add(LanguageListItem(code))
            }
        }
    }

    fun getCanonicalName(code: String): String? {
        val value = siteListData.value
        if (value !is Resource.Success) {
            return null
        }
        return value.data.find { it.code == code }?.localname.orEmpty()
                .ifEmpty { WikipediaApp.instance.languageState.getAppLanguageCanonicalName(code) }
    }

    class LanguageListItem(val code: String, val isHeader: Boolean = false)
}
