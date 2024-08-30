package org.wikipedia.suggestededits

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.wikipedia.Constants
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthImageSuggestion
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.edit.insertmedia.InsertMediaViewModel
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.FileAliasData
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import java.io.IOException

class SuggestedEditsImageRecsFragmentViewModel(bundle: Bundle) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    lateinit var recommendation: GrowthImageSuggestion
    lateinit var pageTitle: PageTitle
    lateinit var summary: PageSummary
    lateinit var recommendedImageTitle: PageTitle
    var attemptInsertInfobox = false

    val langCode = bundle.getString(SuggestedEditsImageRecsFragment.ARG_LANG)!!
    private val _uiState = MutableStateFlow(Resource<Unit>())
    val uiState = _uiState.asStateFlow()

    init {
        fetchRecommendation()
    }

    fun fetchRecommendation() {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(handler) {
            var page: MwQueryPage?
            var tries = 0
            do {
                page = EditingSuggestionsProvider.getNextArticleWithImageRecommendation(langCode)
            } while (tries++ < 10 && page?.growthimagesuggestiondata.isNullOrEmpty())

            if (page?.growthimagesuggestiondata.isNullOrEmpty()) {
                _uiState.value = Depleted()
                return@launch
            }

            recommendation = page?.growthimagesuggestiondata?.first()!!
            val wikiSite = WikiSite.forLanguageCode(langCode)
            summary = ServiceFactory.getRest(wikiSite).getPageSummary(null, page.title)
            pageTitle = summary.getPageTitle(wikiSite)

            val thumbUrl = UriUtil.resolveProtocolRelativeUrl(ImageUrlUtil.getUrlForPreferredSize(recommendation.images[0].metadata!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE))

            recommendedImageTitle = PageTitle(FileAliasData.valueFor(langCode), recommendation.images[0].image,
                null, thumbUrl, Constants.commonsWikiSite)
            recommendedImageTitle.description = recommendation.images[0].metadata!!.description

            // In advance, attempt to insert the image into the wikitext with example parameters, then get a preview,
            // and check whether the preview contains errors, in which case don't insert into the infobox.

            val insertResult = InsertMediaViewModel.insertImageIntoWikiText(langCode, page.revisions.first().contentMain, recommendation.images[0].image,
                "caption", "alt", "200px", "thumb", "right", 0, autoInsert = true, attemptInfobox = true)

            if (insertResult.second) {
                withContext(Dispatchers.IO) {
                    try {
                        val body = FormBody.Builder()
                            .add("wikitext", insertResult.first)
                            .build()

                        L.d("Requesting preview with image inserted into infobox...")
                        val request = Request.Builder().url(ServiceFactory.getRestBasePath(wikiSite) +
                                RestService.PAGE_HTML_PREVIEW_ENDPOINT + UriUtil.encodeURL(pageTitle.prefixedText))
                            .post(body)
                            .build()
                        OkHttpConnectionFactory.client.newCall(request).execute().use { response ->
                            val previewHtml = response.body?.string().orEmpty()
                            attemptInsertInfobox = true

                            if (previewHtml.contains("with unknown parameter", true)) {
                                L.d("Preview contains error, so no longer inserting into infobox.")
                                attemptInsertInfobox = false
                            }
                        }
                    } catch (e: IOException) {
                        L.e(e)
                    }
                }
            }

            _uiState.value = Resource.Success(Unit)
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
                    reasonCodes?.mapNotNull { ImageRecommendationsEvent.reasons.getOrNull(it) }.orEmpty(),
                    recommendation.images[0].image, null, null
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

    class Depleted : Resource<Unit>()
}
