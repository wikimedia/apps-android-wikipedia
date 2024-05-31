package org.wikipedia.suggestededits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.language.LanguageUtil
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.Resource

class SuggestedEditsImageTagsViewModel() : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    private val _uiState = MutableStateFlow(Resource<Pair<MwQueryPage, String?>>())
    val uiState = _uiState.asStateFlow()

    fun findNextSuggestedEditsItem(languageCode: String) {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(handler) {
            val mwQueryPage = EditingSuggestionsProvider.getNextImageWithMissingTags()
            val caption = ServiceFactory.get(Constants.commonsWikiSite)
                .getWikidataEntityTerms(mwQueryPage.title, LanguageUtil.convertToUselangIfNeeded(languageCode))
                .query?.firstPage()?.entityTerms?.label?.firstOrNull()
            _uiState.value = Resource.Success(mwQueryPage to caption)
        }
    }
}
