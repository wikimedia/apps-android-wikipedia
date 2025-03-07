package org.wikipedia.language.composelanglinks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.language.LangLinksViewModel.Companion.addVariantEntriesIfNeeded
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.StringUtil

class ComposeLangLinksViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!
    private val app = WikipediaApp.instance

    data class LangLinksItem(
        val pageTitle: PageTitle? = null,
        val articleName: String = "",
        val languageCode: String = "",
        val localizedName: String = "",
        var canonicalName: String? = null,
        val subtitle: String = "",
        val headerText: String = "",
        val isHeader: Boolean = false
    )

    data class LangLinksUiState(
        val searchTerm: String = "",
        val langLinksItems: List<LangLinksItem> = emptyList(),
        val isLoading: Boolean = false,
        val isSiteInfoLoaded: Boolean = false,
        val error: Throwable? = null,
    )

    private val _siteInfoList = MutableStateFlow<List<SiteMatrix.SiteInfo>>(emptyList())
    private val _originalLanguageEntries = MutableStateFlow<List<PageTitle>>(emptyList())
    private val _appLanguageEntries = MutableStateFlow<List<PageTitle>>(emptyList())

    private val _uiState = MutableStateFlow(LangLinksUiState())
    val uiState: StateFlow<LangLinksUiState> = _uiState.asStateFlow()

    init {
        fetchLangLinks()
        fetchSiteInfo()
    }

    private fun fetchLangLinks() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
           _uiState.update {
               it.copy(
                    error = throwable
               )
           }
        }) {
            val response = ServiceFactory.get(pageTitle.wikiSite).getLangLinks(pageTitle.prefixedText)
            val langLinks = response.query!!.langLinks().toMutableList()
            updateLanguageEntriesSupported(langLinks)
            sortLanguageEntriesByMru(langLinks)
            _originalLanguageEntries.value = langLinks

            val appLangEntries = langLinks.filter {
                (it.wikiSite.languageCode == AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE &&
                        app.languageState.appLanguageCodes.contains(AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE)) ||
                        (it.wikiSite.languageCode == AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE &&
                                app.languageState.appLanguageCodes.contains(AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE)) ||
                        app.languageState.appLanguageCodes.contains(it.wikiSite.languageCode)
            }
            _appLanguageEntries.value = appLangEntries

            updateLanguageItems()
        }
    }

    private fun fetchSiteInfo() {
        _uiState.update { it.copy(isSiteInfoLoaded = true) }
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
           _uiState.update { it.copy(error = throwable, isSiteInfoLoaded = false) }
        }) {
            delay(5000)
            val siteMatrix = ServiceFactory.get(WikipediaApp.instance.wikiSite).getSiteMatrix()
            val sites = SiteMatrix.getSites(siteMatrix)
            _siteInfoList.value = sites
            _uiState.update { it.copy(isSiteInfoLoaded = false) }
            updateLanguageItems()
        }
    }

    private fun getCanonicalName(code: String): String? {
        println("orange --> ${_siteInfoList.value.size}")
        return _siteInfoList.value.find { it.code == code }?.localname.orEmpty()
            .ifEmpty { WikipediaApp.instance.languageState.getAppLanguageCanonicalName(code) }
    }

    private fun updateLanguageItems() {
        val appLangEntries = _appLanguageEntries.value
        val originalEntries = _originalLanguageEntries.value

        val nonDuplicateEntries = originalEntries.toMutableList().apply {
            removeAll(appLangEntries)
        }

        val items = mutableListOf<LangLinksItem>()
        // not searching

        // Add app languages section if available, usually after user select article in different
        // language
        if (appLangEntries.isNotEmpty()) {
            items.add(
                LangLinksItem(
                    isHeader = true,
                    headerText = app.getString(R.string.langlinks_your_wikipedia_languages)
                )
            )
            items.addAll(appLangEntries.map { createLangLinksItem(it, true) })
        }

        // Add all languages section if available
        if (nonDuplicateEntries.isNotEmpty()) {
            items.add(
                LangLinksItem(
                    isHeader = true,
                    headerText = app.getString(R.string.languages_list_all_text)
                )
            )
            items.addAll(nonDuplicateEntries.map { createLangLinksItem(it, false) })
        }

        _uiState.update {
            it.copy(
                langLinksItems = items,
                isLoading = false
            )
        }
    }

    private fun createLangLinksItem(pageTitle: PageTitle, isAppLanguage: Boolean): LangLinksItem {
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
            canonicalName = canonicalName
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
}
