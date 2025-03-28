package org.wikipedia.wiktionary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.RbDefinition
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.util.Locale

class WiktionaryViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update { it.copy(isLoading = false, error = throwable) }
    }

    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!
    var selectedText = savedStateHandle.get<String>(Constants.ARG_TEXT)

    private val _uiState = MutableStateFlow(WiktionaryDialogState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDefinitions()
    }

    private fun loadDefinitions() {
        viewModelScope.launch(handler) {
            if (selectedText.isNullOrEmpty()) {
                definitionsNotFound()
                return@launch
            }
            val query = StringUtil.addUnderscores(selectedText)
            val response = try {
                ServiceFactory.getRest(WikiSite(pageTitle.wikiSite.subdomain() + WiktionaryDialog.WIKTIONARY_DOMAIN))
                    .getDefinition(query)
            } catch (e: Exception) {
                L.w("Cannot find the definition. Try to use lowercase text.")
                ServiceFactory.getRest(WikiSite(pageTitle.wikiSite.subdomain() + WiktionaryDialog.WIKTIONARY_DOMAIN))
                    .getDefinition(query.lowercase(Locale.getDefault()))
            }

            response[pageTitle.wikiSite.languageCode]?.let { usageList ->
                if (usageList.isEmpty()) {
                    definitionsNotFound()
                } else {
                    _uiState.update { it.copy(isLoading = false, list = usageList, error = null) }
                }
            } ?: run {
                definitionsNotFound()
            }
        }
    }

    private fun definitionsNotFound() {
        _uiState.update { it.copy(isLoading = false, error = Throwable("Definitions not found.")) }
    }

    fun getTermFromWikiLink(url: String): String {
        val splitUrl = url.substringAfterLast('/').split('#')
        // Remove link fragment
        return if (splitUrl[0].endsWith(GLOSSARY_OF_TERMS) && splitUrl.size > 1) splitUrl[1] else splitUrl[0]
    }

    companion object {
        // Try to get the correct definition from glossary terms: https://en.wiktionary.org/wiki/Appendix:Glossary
        private const val GLOSSARY_OF_TERMS = ":Glossary"
    }
}

data class WiktionaryDialogState(
    val isLoading: Boolean = true,
    val list: List<RbDefinition.Usage> = emptyList(),
    val error: Throwable? = null
)
