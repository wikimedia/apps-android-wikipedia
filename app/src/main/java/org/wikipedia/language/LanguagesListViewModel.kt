package org.wikipedia.language

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.util.Resource

class LanguagesListViewModel : ViewModel() {
    val siteListData = MutableLiveData<Resource<List<SiteMatrix.SiteInfo>>>()

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val siteMatrix = ServiceFactory.get(WikipediaApp.getInstance().wikiSite).siteMatrixCall.execute().body()!!
                val sites = SiteMatrix.getSites(siteMatrix)
                siteListData.postValue(Resource.Success(sites))
            }
        }
    }

    fun getCanonicalName(code: String): String? {
        var canonicalName = siteListData.value?.data?.find { it.code == code }?.localname
        if (canonicalName.isNullOrEmpty()) {
            canonicalName = WikipediaApp.getInstance().language().getAppLanguageCanonicalName(code)
        }
        return canonicalName
    }
}
