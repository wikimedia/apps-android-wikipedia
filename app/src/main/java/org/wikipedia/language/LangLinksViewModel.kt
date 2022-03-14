package org.wikipedia.language

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData
import org.wikipedia.util.log.L

class LangLinksViewModel(bundle: Bundle) : ViewModel() {

    var pageTitle: PageTitle = bundle.getParcelable(LangLinksActivity.EXTRA_PAGETITLE)!!

    val languageEntries = MutableLiveData<Resource<List<PageTitle>>>()
    val siteListData = SingleLiveData<Resource<List<SiteMatrix.SiteInfo>>>()

    init {
        fetchLangLinks()
        fetchSiteInfo()
    }

    fun fetchLangLinks() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            languageEntries.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                val response = ServiceFactory.get(pageTitle.wikiSite).getLangLinks(pageTitle.prefixedText)
                val langLinks = response.query!!.langLinks()
                updateLanguageEntriesSupported(langLinks)
                sortLanguageEntriesByMru(langLinks)
                languageEntries.postValue(Resource.Success(langLinks))

                val variantLangList = langLinks.filter { !WikipediaApp.getInstance().language().getDefaultLanguageCode(it.wikiSite.languageCode).isNullOrEmpty() }
                if (variantLangList.isNotEmpty()) {
                    val pairs = mutableListOf<Pair<PageTitle, Deferred<PageSummary>>>()
                    variantLangList.forEach {
                        pairs.add(Pair(it, async { ServiceFactory.getRest(it.wikiSite).getPageSummary(null, it.prefixedText) }))
                    }
                    pairs.forEach { pair ->
                        langLinks.find { pair.first == it }?.displayText = pair.second.await().displayTitle
                    }
                    languageEntries.postValue(Resource.Success(langLinks))
                }
            }
        }
    }

    private fun fetchSiteInfo() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            withContext(Dispatchers.IO) {
                val siteMatrix = ServiceFactory.get(WikipediaApp.getInstance().wikiSite).getSiteMatrix()
                val sites = SiteMatrix.getSites(siteMatrix)
                siteListData.postValue(Resource.Success(sites))
            }
        }
    }

    private fun updateLanguageEntriesSupported(languageEntries: MutableList<PageTitle>) {
        val it = languageEntries.listIterator()
        while (it.hasNext()) {
            val link = it.next()
            val languageCode = link.wikiSite.languageCode
            val languageVariants = WikipediaApp.getInstance().language().getLanguageVariants(languageCode)
            if (AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE == languageCode) {
                // Replace legacy name of тарашкевіца language with the correct name.
                // TODO: Can probably be removed when T111853 is resolved.
                it.remove()
                it.add(PageTitle(link.text, WikiSite.forLanguageCode(AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE)))
            } else if (languageVariants != null) {
                // remove the language code and replace it with its variants
                it.remove()
                for (variant in languageVariants) {
                    it.add(PageTitle(if (pageTitle.isMainPage) SiteInfoClient.getMainPageForLang(variant) else link.prefixedText,
                            WikiSite.forLanguageCode(variant)))
                }
            }
        }
        LangLinksActivity.addVariantEntriesIfNeeded(WikipediaApp.getInstance().language(), pageTitle, languageEntries)
    }

    private fun sortLanguageEntriesByMru(entries: MutableList<PageTitle>) {
        var addIndex = 0
        for (language in WikipediaApp.getInstance().language().mruLanguageCodes) {
            for (i in entries.indices) {
                if (entries[i].wikiSite.languageCode == language) {
                    val entry = entries.removeAt(i)
                    entries.add(addIndex++, entry)
                    break
                }
            }
        }
    }

    fun getCanonicalName(code: String): String? {
        val value = siteListData.value
        if (value !is Resource.Success) {
            return null
        }
        return value.data.find { it.code == code }?.localname.orEmpty()
                .ifEmpty { WikipediaApp.getInstance().language().getAppLanguageCanonicalName(code) }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LangLinksViewModel(bundle) as T
        }
    }
}
