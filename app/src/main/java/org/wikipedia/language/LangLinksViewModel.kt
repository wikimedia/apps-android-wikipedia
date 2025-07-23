package org.wikipedia.language

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState

class LangLinksViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!
    private val app = WikipediaApp.instance

    private var siteInfoList = listOf<SiteMatrix.SiteInfo>()
    private var originalLanguageEntries = listOf<PageTitle>()
    private var appLanguageEntries = listOf<PageTitle>()
    private var variantLangToUpdate = mutableSetOf<String>()

    private val _uiState = MutableStateFlow<UiState<List<LangLinksItem>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    val historyEntryId = savedStateHandle.get<Long>(Constants.ARG_NUMBER) ?: -1

    init {
        fetchAllData()
    }

    fun fetchAllData() {
        _uiState.value = UiState.Loading
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = UiState.Error(throwable)
        } + Dispatchers.IO) {
            val langLinksDeferred = async {
                val response = ServiceFactory.get(pageTitle.wikiSite).getLangLinks(pageTitle.prefixedText)
                response.query!!.langLinks().toMutableList()
            }
            val siteInfoDeferred = async {
                val siteMatrix = ServiceFactory.get(WikipediaApp.instance.wikiSite).getSiteMatrix()
                SiteMatrix.getSites(siteMatrix)
            }

            val langLinks = langLinksDeferred.await()
            siteInfoList = siteInfoDeferred.await()

            processLanguageEntries(langLinks)
            originalLanguageEntries = langLinks
            appLanguageEntries = filterAppLanguages(langLinks)

            // create variant languages to update
            variantLangToUpdate = langLinks
                .mapNotNull { app.languageState.getDefaultLanguageCode(it.wikiSite.languageCode) }
                .toMutableSet()

            val variantFetchJobs = mutableListOf<Deferred<Unit>>()
            val variantsToFetch = variantLangToUpdate.toList()
            for (langCode in variantsToFetch) {
                variantFetchJobs.add(async {
                    fetchLangVariantLinks(langCode, pageTitle.prefixedText)
                })
            }
            variantFetchJobs.awaitAll()
            updateLanguageItems()
        }
    }

    fun onSearchQueryChange(searchQuery: String) {
        updateLanguageItems(searchQuery)
    }

    private suspend fun fetchLangVariantLinks(langCode: String, title: String) {
        val response = ServiceFactory.get(WikiSite.forLanguageCode(langCode)).getInfoByPageIdsOrTitles(titles = title)
        response.query?.firstPage()?.varianttitles?.let { variantMap ->
            val currentItems = originalLanguageEntries.toMutableList()
            currentItems.forEach { item ->
                variantMap[item.wikiSite.languageCode]?.let { text ->
                    item.displayText = text
                }
            }
            originalLanguageEntries = currentItems
        }
    }

    private fun processLanguageEntries(langLinks: MutableList<PageTitle>) {
        updateLanguageEntriesSupported(langLinks)
        sortLanguageEntriesByMru(langLinks)
    }

    private fun filterAppLanguages(entries: List<PageTitle>): List<PageTitle> {
        return entries.filter {
            (it.wikiSite.languageCode == AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE &&
                    app.languageState.appLanguageCodes.contains(AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE)) ||
                    (it.wikiSite.languageCode == AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE &&
                            app.languageState.appLanguageCodes.contains(AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE)) ||
                    app.languageState.appLanguageCodes.contains(it.wikiSite.languageCode)
        }
    }

    private fun getCanonicalName(code: String): String? {
        return siteInfoList.find { it.code == code }?.localname.orEmpty()
            .ifEmpty { WikipediaApp.instance.languageState.getAppLanguageCanonicalName(code) }
    }

    private fun updateLanguageItems(searchTerm: String = "") {
        val appLangEntries = appLanguageEntries
        val originalEntries = originalLanguageEntries

        val nonDuplicateEntries = originalEntries.toMutableList().apply {
            removeAll(appLangEntries)
        }

        val items = mutableListOf<LangLinksItem>()

        if (searchTerm.isNotEmpty()) {
            val filteredItems = originalEntries.filter { entry ->
                val languageCode = entry.wikiSite.languageCode
                val canonicalName = getCanonicalName(languageCode) ?: app.languageState.getAppLanguageCanonicalName(languageCode).orEmpty()
                val localizedName = app.languageState.getAppLanguageLocalizedName(languageCode).orEmpty()
                canonicalName.contains(searchTerm, true) || localizedName.contains(searchTerm, true)
            }
            items.addAll(filteredItems.map { createLangLinksItem(it) })
        } else {
            // not searching
            // Add app languages section if available, usually after user select article in different
            // language
            if (appLangEntries.isNotEmpty()) {
                items.add(
                    LangLinksItem(
                        headerText = app.getString(R.string.langlinks_your_wikipedia_languages)
                    )
                )
                items.addAll(appLangEntries.map { createLangLinksItem(it) })
            }

            // Add all languages section if available
            if (nonDuplicateEntries.isNotEmpty()) {
                items.add(
                    LangLinksItem(
                        headerText = app.getString(R.string.languages_list_all_text)
                    )
                )
                items.addAll(nonDuplicateEntries.map { createLangLinksItem(it) })
            }
        }
        _uiState.value = UiState.Success(items)
    }

    private fun createLangLinksItem(pageTitle: PageTitle): LangLinksItem {
        val languageCode = pageTitle.wikiSite.languageCode
        val localizedName =
            StringUtil.capitalize(app.languageState.getAppLanguageLocalizedName(languageCode)) ?: languageCode
        val canonicalName = getCanonicalName(languageCode)
        val articleName = pageTitle.displayText

        return LangLinksItem(
            pageTitle = pageTitle,
            articleName = articleName,
            languageCode = languageCode,
            localizedName = localizedName,
            canonicalName = canonicalName,
        )
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

data class LangLinksItem(
    val pageTitle: PageTitle? = null,
    val articleName: String = "",
    val languageCode: String = "",
    val localizedName: String = "",
    var canonicalName: String? = null,
    val subtitle: String = "",
    val headerText: String = ""
)
