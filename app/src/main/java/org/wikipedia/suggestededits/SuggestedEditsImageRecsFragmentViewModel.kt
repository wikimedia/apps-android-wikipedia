package org.wikipedia.suggestededits

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthImageSuggestion
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.FileAliasData
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.ImageUrlUtil
import java.util.*

class SuggestedEditsImageRecsFragmentViewModel(bundle: Bundle) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    lateinit var recommendation: GrowthImageSuggestion
    lateinit var pageTitle: PageTitle
    lateinit var summary: PageSummary
    lateinit var recommendedImageTitle: PageTitle

    val langCode = bundle.getString(SuggestedEditsImageRecsFragment.ARG_LANG)!!
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchRecommendation()
    }

    fun fetchRecommendation() {
        _uiState.value = UiState.Loading()
        viewModelScope.launch(handler) {
            var page: MwQueryPage?
            var tries = 0
            do {
                page = EditingSuggestionsProvider.getNextArticleWithImageRecommendation(langCode)
            } while (tries++ < 10 && page?.growthimagesuggestiondata.isNullOrEmpty())

            recommendation = page?.growthimagesuggestiondata?.first()!!
            val wikiSite = WikiSite.forLanguageCode(langCode)
            summary = ServiceFactory.getRest(wikiSite).getPageSummary(null, page.title)
            pageTitle = summary.getPageTitle(wikiSite)

            var thumbUrl = ImageUrlUtil.getUrlForPreferredSize(recommendation.images[0].metadata!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)
            if (thumbUrl.startsWith("//")) {
                thumbUrl = "https:$thumbUrl"
            }
            recommendedImageTitle = PageTitle(FileAliasData.valueFor(langCode), recommendation.images[0].displayFilename,
                null, thumbUrl, Constants.commonsWikiSite)

            _uiState.value = UiState.Success()
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SuggestedEditsImageRecsFragmentViewModel(bundle) as T
        }
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
