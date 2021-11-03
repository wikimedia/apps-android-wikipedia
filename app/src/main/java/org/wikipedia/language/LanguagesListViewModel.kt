package org.wikipedia.language

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.util.Resource
import java.util.*

class LanguagesListViewModel : ViewModel() {

    val siteListFlow = MutableStateFlow(Resource<List<SiteMatrix.SiteInfo>>())
    private val suggestedLanguageCodes = WikipediaApp.getInstance().language().remainingAvailableLanguageCodes
    private val nonSuggestedLanguageCodes = WikipediaApp.getInstance().language()
        .appMruLanguageCodes.toMutableList().also { it.removeAll(suggestedLanguageCodes) }

    private val coroutineHandler = CoroutineExceptionHandler { _, throwable ->
        siteListFlow.value = Resource.Error(throwable)
    }

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch(coroutineHandler) {
            withContext(Dispatchers.IO) {
                val siteMatrix = ServiceFactory.get(WikipediaApp.getInstance().wikiSite).getSiteMatrix()
                val sites = SiteMatrix.getSites(siteMatrix)
                siteListFlow.value = Resource.Success(sites)
            }
        }
    }

    fun getListBySearchTerm(context: Context, searchTerm: String?): List<LanguageListItem> {
        val results = mutableListOf<LanguageListItem>()
        val filter = StringUtils.stripAccents(searchTerm.orEmpty()).lowercase(Locale.getDefault())
        var first = true
        for (code in suggestedLanguageCodes) {
            val localizedName = StringUtils.stripAccents(WikipediaApp.getInstance().language().getAppLanguageLocalizedName(code).orEmpty())
            val canonicalName = StringUtils.stripAccents(getCanonicalName(code).orEmpty())
            if (filter.isEmpty() || code.contains(filter) ||
                localizedName.lowercase(Locale.getDefault()).contains(filter) ||
                canonicalName.lowercase(Locale.getDefault()).contains(filter)) {
                if (first) {
                    results.add(LanguageListItem(context.getString(R.string.languages_list_suggested_text), true))
                    first = false
                }
                results.add(LanguageListItem(code))
            }
        }
        first = true
        for (code in nonSuggestedLanguageCodes) {
            val localizedName = StringUtils.stripAccents(WikipediaApp.getInstance().language().getAppLanguageLocalizedName(code).orEmpty())
            val canonicalName = StringUtils.stripAccents(getCanonicalName(code).orEmpty())
            if (filter.isEmpty() || code.contains(filter) ||
                localizedName.lowercase(Locale.getDefault()).contains(filter) ||
                canonicalName.lowercase(Locale.getDefault()).contains(filter)) {
                if (first) {
                    results.add(LanguageListItem(context.getString(R.string.languages_list_all_text), true))
                    first = false
                }
                results.add(LanguageListItem(code))
            }
        }
        return results
    }

    fun getCanonicalName(code: String): String? {
        var canonicalName = siteListFlow.value.data?.find { it.code == code }?.localname
        if (canonicalName.isNullOrEmpty()) {
            canonicalName = WikipediaApp.getInstance().language().getAppLanguageCanonicalName(code)
        }
        return canonicalName
    }

    class LanguageListItem(val code: String, val isHeader: Boolean = false)
}
