package org.wikipedia.suggestededits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.wikidata.Entities
import org.wikipedia.descriptions.DescriptionEditFragment
import org.wikipedia.language.LanguageUtil
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.Resource
import java.util.UUID

class SuggestedEditsImageTagsViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    private val _uiState = MutableStateFlow(Resource<Pair<MwQueryPage, String?>>())
    val uiState = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow(Resource<Entities.Entity?>())
    val actionState = _actionState.asStateFlow()

    fun findNextSuggestedEditsItem(languageCode: String, page: MwQueryPage?) {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(handler) {
            val mwQueryPage = page ?: EditingSuggestionsProvider.getNextImageWithMissingTags()
            val caption = ServiceFactory.get(Constants.commonsWikiSite)
                .getWikidataEntityTerms(mwQueryPage.title, LanguageUtil.convertToUselangIfNeeded(languageCode))
                .query?.firstPage()?.entityTerms?.label?.firstOrNull()
            _uiState.value = Resource.Success(mwQueryPage to caption)
        }
    }

    fun publishImageTags(page: MwQueryPage, acceptedLabels: List<ImageTag>) {
        viewModelScope.launch(handler) {
            val csrfToken = CsrfTokenClient.getToken(Constants.commonsWikiSite).blockingSingle()
            val mId = "M" + page.pageId
            var claimStr = "{\"claims\":["
            var commentStr = "/* add-depicts: "
            var first = true
            for (label in acceptedLabels) {
                if (!first) {
                    claimStr += ","
                }
                if (!first) {
                    commentStr += ","
                }
                first = false
                claimStr += "{\"mainsnak\":" +
                        "{\"snaktype\":\"value\",\"property\":\"P180\"," +
                        "\"datavalue\":{\"value\":" +
                        "{\"entity-type\":\"item\",\"id\":\"${label.wikidataId}\"}," +
                        "\"type\":\"wikibase-entityid\"},\"datatype\":\"wikibase-item\"}," +
                        "\"type\":\"statement\"," +
                        "\"id\":\"${mId}\$${UUID.randomUUID()}\"," +
                        "\"rank\":\"normal\"}"
                commentStr += label.wikidataId + "|" + label.label.replace("|", "").replace(",", "")
            }
            claimStr += "]}"
            commentStr += " */" + DescriptionEditFragment.SUGGESTED_EDITS_IMAGE_TAGS_COMMENT
            val postResult = ServiceFactory.get(Constants.commonsWikiSite).postEditEntity(mId, csrfToken, claimStr, commentStr, null)
            _actionState.value = Resource.Success(postResult.entity)
        }
    }
}
