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
import org.wikipedia.util.log.L
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

    fun insertImageIntoArticle(imageTitle: String, imageCaption: String, imageAltText: String) {
        viewModelScope.launch(handler) {

            // Prepare a few things.

            // First, get the collection of magicWords from the target wiki, for composing the template
            // for the image insertion.
            val magicWords = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                .getSiteInfoWithMagicWords().query?.magicwords.orEmpty()

            val thumbParamName = magicWords.find { it.name == "img_thumbnail" }?.aliases?.firstOrNull() ?: "thumb"
            val altParamName = magicWords.find { it.name == "img_alt" }?.aliases?.firstOrNull() ?: "alt=$1"


            val pageTitle = "Alma Valencia" //summary.apiTitle


            // And of course get the wikitext of the first section of the article.
            var wikitext = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                .getWikiTextForSection(pageTitle, 0).query?.pages?.firstOrNull()?.revisions?.firstOrNull()?.contentMain.orEmpty()

            val namespaceName = FileAliasData.valueFor(langCode)

            // Now, compose the template for the image insertion.
            val template = "[[$namespaceName:$imageTitle|$thumbParamName|" +
                    (if (imageAltText.isEmpty()) "" else "${altParamName.replace("$1", imageAltText)}|") +
                    "$imageCaption]]"

            // But before we resort to inserting the image at the top of the wikitext, let's see if
            // the article has an infobox, and if so, see if we can inject the image right in there.

            var insertAtTop = true

            var infoboxStartIndex = -1
            var infoboxEndIndex = -1
            var i = 0
            while (true) {
                i = wikitext.indexOf("{{", i)
                if (i == -1) {
                    break
                }
                i += 2
                val pipePos = wikitext.indexOf("|", i)
                if (pipePos > i) {
                    val templateName = wikitext.substring(i, pipePos).trim()
                    if (templateName.contains("{{") || templateName.contains("}}")) {
                        // template doesn't contain pipe symbol, not what we're looking for.
                        continue
                    }

                    if (templateName.endsWith("box") || templateName.contains("box ")) {
                        infoboxStartIndex = i
                        infoboxEndIndex = wikitext.indexOf("}}", infoboxStartIndex)
                    }
                }
            }

            if (infoboxStartIndex in 0 until infoboxEndIndex) {
                val regexImage = """\|\s*image(\d+)?\s*(=)\s*(\|)""".toRegex()
                var match = regexImage.find(wikitext, infoboxStartIndex)

                if (match != null && match.groups[3] != null &&
                    match.groups[3]!!.range.first < infoboxEndIndex) {
                    insertAtTop = false

                    var insertPos = match.groups[3]!!.range.first
                    val curImageStr = wikitext.substring(match.groups[2]!!.range.first + 1, match.groups[3]!!.range.first)
                    if (curImageStr.contains("\n")) {
                        insertPos = wikitext.indexOf("\n", match.groups[2]!!.range.first)
                    }

                    wikitext = wikitext.substring(0, insertPos) + namespaceName + ":" + imageTitle + wikitext.substring(insertPos)
                }

                val regexCaption = """\|\s*caption(\d+)?\s*(=)\s*(\|)""".toRegex()
                match = regexCaption.find(wikitext, infoboxStartIndex)
                if (match != null && match.groups[3] != null &&
                    match.groups[3]!!.range.first < infoboxEndIndex) {

                    var insertPos = match.groups[3]!!.range.first
                    val curImageStr = wikitext.substring(match.groups[2]!!.range.first + 1, match.groups[3]!!.range.first)
                    if (curImageStr.contains("\n")) {
                        insertPos = wikitext.indexOf("\n", match.groups[2]!!.range.first)
                    }

                    wikitext = wikitext.substring(0, insertPos) + imageCaption + wikitext.substring(insertPos)
                }

                // TODO: insert image alt text.
            }

            if (insertAtTop) {
                wikitext = template + "\n" + wikitext
            }

            // Save the new wikitext to the article.

            L.d(">>> " + wikitext)
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
