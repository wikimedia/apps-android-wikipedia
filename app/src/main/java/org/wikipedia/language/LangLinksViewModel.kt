package org.wikipedia.language

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData
import org.wikipedia.util.log.L

class LangLinksViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    var pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!

    val languageEntries = MutableLiveData<Resource<List<PageTitle>>>()
    val languageEntryVariantUpdate = SingleLiveData<Resource<Unit>>()
    val siteListData = SingleLiveData<Resource<List<SiteMatrix.SiteInfo>>>()

    init {
        fetchLangLinks()
        fetchSiteInfo()
    }

    fun fetchLangLinks() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            languageEntries.postValue(Resource.Error(throwable))
        }) {
            val response = ServiceFactory.get(pageTitle.wikiSite).getLangLinks(pageTitle.prefixedText)
            val langLinks = response.query!!.langLinks().toMutableList()
            updateLanguageEntriesSupported(langLinks)
            sortLanguageEntriesByMru(langLinks)
            languageEntries.postValue(Resource.Success(langLinks))
        }
    }

    fun fetchLangVariantLinks(langCode: String, title: String, titles: List<PageTitle>) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            languageEntries.postValue(Resource.Error(throwable))
        }) {
            val response = ServiceFactory.get(WikiSite.forLanguageCode(langCode)).getInfoByPageIdsOrTitles(null, title)
            response.query?.firstPage()?.varianttitles?.let { variantMap ->
                titles.forEach {
                    variantMap[it.wikiSite.languageCode]?.let { text ->
                        it.displayText = text
                    }
                }
            }
            languageEntryVariantUpdate.postValue(Resource.Success(Unit))
        }
    }

    private fun fetchSiteInfo() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            val siteMatrix = ServiceFactory.get(WikipediaApp.instance.wikiSite).getSiteMatrix()
            val sites = SiteMatrix.getSites(siteMatrix)
            siteListData.postValue(Resource.Success(sites))
        }
    }

    private fun updateLanguageEntriesSupported(languageEntries: MutableList<PageTitle>) {
        val it = languageEntries.listIterator()
        while (it.hasNext()) {
            val link = it.next()
            val languageCode = link.wikiSite.languageCode
            val languageVariants = WikipediaApp.instance.languageState.getLanguageVariants(languageCode)
            if (AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE == languageCode) {
                // Replace legacy name of тарашкевіца language with the correct name.
                // TODO: Can probably be removed when T111853 is resolved.
                it.remove()
                it.add(PageTitle(link.text, WikiSite.forLanguageCode(AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE)))
            } else if (languageVariants != null) {
                // remove the language code and replace it with its variants
                it.remove()
                for (variant in languageVariants) {
                    it.add(PageTitle(if (pageTitle.isMainPage) MainPageNameData.valueFor(variant) else link.prefixedText,
                            WikiSite.forLanguageCode(variant)))
                }
            }
        }
        addVariantEntriesIfNeeded(WikipediaApp.instance.languageState, pageTitle, languageEntries)
    }

    private fun sortLanguageEntriesByMru(entries: MutableList<PageTitle>) {
        var addIndex = 0
        for (language in WikipediaApp.instance.languageState.mruLanguageCodes) {
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
                .ifEmpty { WikipediaApp.instance.languageState.getAppLanguageCanonicalName(code) }
    }

    companion object {
        fun addVariantEntriesIfNeeded(language: AppLanguageState, title: PageTitle, languageEntries: MutableList<PageTitle>) {
            val parentLanguageCode = language.getDefaultLanguageCode(title.wikiSite.languageCode)
            if (parentLanguageCode != null) {
                val languageVariants = language.getLanguageVariants(parentLanguageCode)
                if (languageVariants != null) {
                    for (languageCode in languageVariants) {
                        // Do not add zh-hant and zh-hans to the list
                        if (listOf(AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
                                AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE).contains(languageCode)) {
                            continue
                        }
                        if (!title.wikiSite.languageCode.contains(languageCode)) {
                            val pageTitle = PageTitle(if (title.isMainPage) MainPageNameData.valueFor(languageCode) else title.prefixedText, WikiSite.forLanguageCode(languageCode))
                            pageTitle.displayText = title.displayText
                            languageEntries.add(pageTitle)
                        }
                    }
                }
            }
        }
    }
}
