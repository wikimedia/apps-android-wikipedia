package org.wikipedia.suggestededits

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthImageSuggestion
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.FileAliasData
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.log.L
import java.util.*

class SuggestedEditsImageRecsFragmentViewModel(bundle: Bundle) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    lateinit var recommendation: GrowthImageSuggestion
    lateinit var summary: PageSummary

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
            summary = ServiceFactory.getRest(WikiSite.forLanguageCode(langCode)).getPageSummary(null, page.title)

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
            val wikitext = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                .getWikiTextForSection(pageTitle, 0).query?.pages?.firstOrNull()?.revisions?.firstOrNull()?.contentMain.orEmpty()

            val namespaceName = FileAliasData.valueFor(langCode)

            // Now, compose the template for the image insertion.
            val template = "[[$namespaceName:$imageTitle|$thumbParamName|" +
                    (if (imageAltText.isEmpty()) "" else "${altParamName.replace("$1", imageAltText)}|") +
                    "$imageCaption]]"

            // But before we resort to inserting the image at the top of the wikitext, let's see if
            // the article has an infobox, and if so, see if we can inject the image right in there.

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
                val regex = """\|\s*image(\d+)?\s*(=)\s*(\|)""".toRegex()
                val match = regex.find(wikitext, infoboxStartIndex)

                if (match != null && match.groups[3] != null &&
                    match.groups[3]!!.range.first < infoboxEndIndex) {

                    var curImageStr = wikitext.substring(match.groups[2]!!.range.first, match.groups[3]!!.range.first)

                    L.d(">>> " + curImageStr)

                    val trimmedStr = curImageStr.trim()

                    L.d(">>> " + trimmedStr)
                }

                var imagePos = wikitext.indexOf("|image", infoboxStartIndex, true)
                if (imagePos in (infoboxStartIndex + 1) until infoboxEndIndex) {
                    val nextPipePos = wikitext.indexOf("|", imagePos + 1)
                    if (nextPipePos in (imagePos + 1) until infoboxEndIndex) {
                        val equalsPos = wikitext.indexOf("=", imagePos + 1)
                        if (equalsPos in (imagePos + 1) until nextPipePos) {
                            val imageTitleInInfobox = wikitext.substring(equalsPos + 1, nextPipePos).trim()
                            if (imageTitleInInfobox.isNotEmpty()) {
                                // There's already an image in the infobox. Let's just replace it.
                                val newWikitext = wikitext.substring(0, equalsPos + 1) + " $imageTitle" +
                                        wikitext.substring(nextPipePos)
                                L.d(">>> " + newWikitext)
                                return@launch
                            } else {
                                // insert image
                            }
                            L.d(">>>> " + imageTitleInInfobox)
                        }

                    }
                }

            }

            L.d(">>> " + template)
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
