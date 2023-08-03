package org.wikipedia.suggestededits

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.Constants
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthImageSuggestion
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.FileAliasData
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.log.L
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
            recommendedImageTitle = PageTitle(FileAliasData.valueFor(langCode), recommendation.images[0].image,
                null, thumbUrl, Constants.commonsWikiSite)
            recommendedImageTitle.description = recommendation.images[0].metadata!!.description

            _uiState.value = UiState.Success()
        }
    }

    fun acceptRecommendation(token: String?, revId: Long) {
        viewModelScope.launch(handler) {
            invalidateRecommendation(token, true, revId, null)
        }
    }

    fun rejectRecommendation(token: String?, reasonCodes: List<Int>) {
        viewModelScope.launch(handler) {
            invalidateRecommendation(token, false, 0, reasonCodes)
        }
    }

    private suspend fun invalidateRecommendation(token: String?, accepted: Boolean, revId: Long, reasonCodes: List<Int>?) {
        val reasons = listOf("notrelevant", "noinfo", "offensive", "lowquality", "unfamiliar", "other")

        withContext(Dispatchers.IO) {
            val csrfToken = token ?: CsrfTokenClient.getToken(pageTitle.wikiSite).blockingSingle()

            // Attempt to call the AddImageFeedback API first, and if it fails, try the
            // growthinvalidateimagerecommendation API instead.
            try {
                val body = GrowthImageSuggestion.AddImageFeedbackBody(
                    csrfToken,
                    revId,
                    recommendation.images[0].image,
                    accepted,
                    reasonCodes?.mapNotNull { reasons.getOrNull(it) }.orEmpty(),
                    null, null, null
                )
                ServiceFactory.getCoreRest(pageTitle.wikiSite).addImageFeedback(pageTitle.prefixedText, body)
            } catch (e: Exception) {
                L.e(e)
                ServiceFactory.get(pageTitle.wikiSite).invalidateImageRecommendation("image-recommendation",
                    pageTitle.prefixedText, recommendation.images[0].image, csrfToken)
            }
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
