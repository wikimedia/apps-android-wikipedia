package org.wikipedia.suggestededits

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class SuggestedEditsCardsItemViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    private val _uiState = MutableStateFlow(Resource<Pair<PageSummaryForEdit?, PageSummaryForEdit?>>())
    val uiState = _uiState.asStateFlow()

    fun findNextSuggestedEditsItem(action: DescriptionEditActivity.Action, fromLangCode: String, toLangCode: String) {
        var sourceSummaryForEdit: PageSummaryForEdit? = null
        var targetSummaryForEdit: PageSummaryForEdit? = null
        _uiState.value = Resource.Loading()
        viewModelScope.launch(handler) {
            when (action) {
                DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> {
                    val pair = EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(fromLangCode), toLangCode)
                    val source = pair.first
                    val target = pair.second

                    sourceSummaryForEdit = PageSummaryForEdit(
                        source.apiTitle,
                        source.lang,
                        source.getPageTitle(WikiSite.forLanguageCode(fromLangCode)),
                        source.displayTitle,
                        source.description,
                        source.thumbnailUrl,
                        source.extract,
                        source.extractHtml
                    )

                    targetSummaryForEdit = PageSummaryForEdit(
                        target.apiTitle,
                        target.lang,
                        target.getPageTitle(WikiSite.forLanguageCode(toLangCode)),
                        target.displayTitle,
                        target.description,
                        target.thumbnailUrl,
                        target.extract,
                        target.extractHtml
                    )
                }

                DescriptionEditActivity.Action.ADD_CAPTION -> {
                    val response = EditingSuggestionsProvider.getNextImageWithMissingCaption(fromLangCode)
                    val imageInfoResponse = ServiceFactory.get(Constants.commonsWikiSite).getImageInfoSuspend(response, fromLangCode)
                    val page = imageInfoResponse.query?.firstPage()
                    if (page?.imageInfo() != null) {
                        val imageInfo = page.imageInfo()!!
                        val title = if (imageInfo.commonsUrl.isEmpty()) {
                            page.title
                        } else {
                            PageTitle.titleForUri(Uri.parse(imageInfo.commonsUrl), Constants.commonsWikiSite).prefixedText
                        }

                        sourceSummaryForEdit = PageSummaryForEdit(
                            title,
                            fromLangCode,
                            PageTitle(
                                Namespace.FILE.name,
                                StringUtil.removeNamespace(title),
                                null,
                                imageInfo.thumbUrl,
                                WikiSite.forLanguageCode(fromLangCode)
                            ),
                            StringUtil.removeHTMLTags(title),
                            imageInfo.metadata!!.imageDescription(),
                            imageInfo.thumbUrl,
                            null,
                            null,
                            imageInfo.timestamp,
                            imageInfo.user,
                            imageInfo.metadata
                        )
                    }
                }

                DescriptionEditActivity.Action.TRANSLATE_CAPTION -> {
                    val pair = EditingSuggestionsProvider.getNextImageWithMissingCaption(fromLangCode, toLangCode)
                    val fileCaption = pair.first
                    val imageInfoResponse = ServiceFactory.get(Constants.commonsWikiSite).getImageInfoSuspend(pair.second, fromLangCode)
                    val page = imageInfoResponse.query?.firstPage()
                    if (page?.imageInfo() != null) {
                        val imageInfo = page.imageInfo()!!
                        val title = if (imageInfo.commonsUrl.isEmpty()) {
                            page.title
                        } else {
                            PageTitle.titleForUri(Uri.parse(imageInfo.commonsUrl), Constants.commonsWikiSite).prefixedText
                        }

                        sourceSummaryForEdit = PageSummaryForEdit(
                            title,
                            fromLangCode,
                            PageTitle(
                                Namespace.FILE.name,
                                StringUtil.removeNamespace(title),
                                null,
                                imageInfo.thumbUrl,
                                WikiSite.forLanguageCode(fromLangCode)
                            ),
                            StringUtil.removeHTMLTags(title),
                            fileCaption,
                            imageInfo.thumbUrl,
                            null,
                            null,
                            imageInfo.timestamp,
                            imageInfo.user,
                            imageInfo.metadata
                        ).also {
                            targetSummaryForEdit = it.copy(
                                lang = toLangCode,
                                pageTitle = PageTitle(
                                    Namespace.FILE.name,
                                    StringUtil.removeNamespace(title),
                                    null,
                                    imageInfo.thumbUrl,
                                    WikiSite.forLanguageCode(toLangCode)
                                )
                            )
                        }
                    }
                }

                else -> {
                    val pageSummary = EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikiSite.forLanguageCode(fromLangCode))
                    sourceSummaryForEdit = PageSummaryForEdit(
                        pageSummary.apiTitle,
                        fromLangCode,
                        pageSummary.getPageTitle(WikiSite.forLanguageCode(fromLangCode)),
                        pageSummary.displayTitle,
                        pageSummary.description,
                        pageSummary.thumbnailUrl,
                        pageSummary.extract,
                        pageSummary.extractHtml
                    )
                }
            }
            _uiState.value = Resource.Success(sourceSummaryForEdit to targetSummaryForEdit)
        }
    }
}
