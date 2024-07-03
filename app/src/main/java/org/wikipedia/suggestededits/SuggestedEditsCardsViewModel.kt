package org.wikipedia.suggestededits

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.util.Resource

class SuggestedEditsCardsViewModel(bundle: Bundle) : ViewModel() {

    var langFromCode = WikipediaApp.instance.languageState.appLanguageCode
    var langToCode = WikipediaApp.instance.languageState.appLanguageCodes.getOrElse(1) { "" }
    var action = bundle.getSerializable(Constants.INTENT_EXTRA_ACTION) as DescriptionEditActivity.Action
    var invokeSource = bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource

    private val _uiState = MutableStateFlow(Resource<List<String>>())
    val uiState = _uiState.asStateFlow()

    init {
        fetchLanguageList()
    }

    private fun fetchLanguageList() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            _uiState.value = Resource.Loading()
            val app = WikipediaApp.instance
            val siteMatrix = ServiceFactory.get(app.wikiSite).getSiteMatrix()
            val list = mutableListOf<String>()
            app.languageState.appLanguageCodes.forEach { code ->
                var name = SiteMatrix.getSites(siteMatrix).find { it.code == code }?.name
                if (name.isNullOrEmpty()) {
                    name = app.languageState.getAppLanguageLocalizedName(code)
                }
                list.add(name ?: code)
            }
            _uiState.value = Resource.Success(list)
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SuggestedEditsCardsViewModel(bundle) as T
        }
    }
}
