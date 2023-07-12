package org.wikipedia.edit.insertmedia

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.FileAliasData
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class InsertMediaViewModel(bundle: Bundle) : ViewModel() {

    val invokeSource = bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource
    val wikiSite = bundle.getParcelable<WikiSite>(Constants.ARG_WIKISITE)!!
    var searchQuery = StringUtil.removeHTMLTags(StringUtil.removeUnderscores(bundle.getString(InsertMediaActivity.EXTRA_SEARCH_QUERY)!!))
    val originalSearchQuery = searchQuery
    var selectedImage = bundle.getParcelable<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE)

    var imagePosition = IMAGE_POSITION_RIGHT
    var imageType = IMAGE_TYPE_THUMBNAIL
    var imageSize = IMAGE_SIZE_DEFAULT

    val insertMediaFlow = Pager(PagingConfig(pageSize = 10)) {
        InsertMediaPagingSource(searchQuery)
    }.flow.cachedIn(viewModelScope)

    init {
        loadMagicWords()
    }

    class InsertMediaPagingSource(
        val searchQuery: String,
    ) : PagingSource<MwQueryResponse.Continuation, PageTitle>() {
        override suspend fun load(params: LoadParams<MwQueryResponse.Continuation>): LoadResult<MwQueryResponse.Continuation, PageTitle> {
            return try {
                val wikiSite = WikiSite(Service.COMMONS_URL)
                val response = ServiceFactory.get(WikiSite(Service.COMMONS_URL))
                    .fullTextSearch("File: $searchQuery", params.key?.gsroffset?.toString(), params.loadSize, params.key?.continuation)

                return response.query?.pages?.let { list ->
                    val results = list.sortedBy { it.index }.map {
                        val pageTitle = PageTitle(it.title, wikiSite, it.thumbUrl())
                        pageTitle.description = it.description
                        pageTitle
                    }
                    LoadResult.Page(results, null, response.continuation)
                } ?: run {
                    LoadResult.Page(emptyList(), null, null)
                }
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<MwQueryResponse.Continuation, PageTitle>): MwQueryResponse.Continuation? {
            return null
        }
    }

    private fun loadMagicWords() {
        if (magicWordsLang == wikiSite.languageCode) {
            return
        }

        magicWords[IMAGE_POSITION_NONE] = "none"
        magicWords[IMAGE_POSITION_CENTER] = "center"
        magicWords[IMAGE_POSITION_LEFT] = "left"
        magicWords[IMAGE_POSITION_RIGHT] = "right"
        magicWords[IMAGE_TYPE_THUMBNAIL] = "thumb"
        magicWords[IMAGE_TYPE_FRAMELESS] = "frameless"
        magicWords[IMAGE_TYPE_FRAME] = "frame"
        magicWords[IMAGE_ALT_TEXT] = "alt=$1"

        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            ServiceFactory.get(wikiSite).getSiteInfoWithMagicWords()
                .query?.magicwords?.let { it ->
                    it.find { it.name == IMAGE_POSITION_NONE }?.aliases?.first()?.let { magicWords[IMAGE_POSITION_NONE] = it }
                    it.find { it.name == IMAGE_POSITION_CENTER }?.aliases?.first()?.let { magicWords[IMAGE_POSITION_CENTER] = it }
                    it.find { it.name == IMAGE_POSITION_LEFT }?.aliases?.first()?.let { magicWords[IMAGE_POSITION_LEFT] = it }
                    it.find { it.name == IMAGE_POSITION_RIGHT }?.aliases?.first()?.let { magicWords[IMAGE_POSITION_RIGHT] = it }
                    it.find { it.name == IMAGE_TYPE_THUMBNAIL }?.aliases?.first()?.let { magicWords[IMAGE_TYPE_THUMBNAIL] = it }
                    it.find { it.name == IMAGE_TYPE_FRAMELESS }?.aliases?.first()?.let { magicWords[IMAGE_TYPE_FRAMELESS] = it }
                    it.find { it.name == IMAGE_TYPE_FRAME }?.aliases?.first()?.let { magicWords[IMAGE_TYPE_FRAME] = it }
                    it.find { it.name == IMAGE_ALT_TEXT }?.aliases?.first()?.let { magicWords[IMAGE_ALT_TEXT] = it }
                    it.find { it.name == IMAGE_POSITION_NONE }?.aliases?.first()?.let { magicWords[IMAGE_POSITION_NONE] = it }
                }
            magicWordsLang = wikiSite.languageCode
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InsertMediaViewModel(bundle) as T
        }
    }

    companion object {
        const val IMAGE_POSITION_NONE = "img_none"
        const val IMAGE_POSITION_CENTER = "img_center"
        const val IMAGE_POSITION_LEFT = "img_left"
        const val IMAGE_POSITION_RIGHT = "img_right"
        const val IMAGE_TYPE_THUMBNAIL = "img_thumbnail"
        const val IMAGE_TYPE_FRAMELESS = "img_frameless"
        const val IMAGE_TYPE_FRAME = "img_framed"
        const val IMAGE_TYPE_BASIC = "basic"
        const val IMAGE_ALT_TEXT = "img_alt"
        const val IMAGE_SIZE_DEFAULT = "220x124"

        private var magicWordsLang = ""
        private val magicWords = mutableMapOf<String, String>()

        fun insertImageIntoWikiText(langCode: String, oldWikiText: String, imageTitle: String, imageCaption: String,
                                    imageAltText: String, imageSize: String, imageType: String, imagePos: String,
                                    cursorPos: Int = 0, attemptInfobox: Boolean = false): String {
            var wikiText = oldWikiText
            val namespaceName = FileAliasData.valueFor(langCode)

            var template = "[[" + FileAliasData.valueFor(langCode) + ":" + imageTitle
            if (imageSize != IMAGE_SIZE_DEFAULT) {
                template += "|${imageSize}px"
            }
            magicWords[imageType]?.let { type ->
                template += "|$type"
            }
            magicWords[imagePos]?.let { pos ->
                template += "|$pos"
            }
            if (imageAltText.isNotEmpty()) {
                template += "|" + magicWords[IMAGE_ALT_TEXT].orEmpty().replace("$1", imageAltText)
            }
            if (imageCaption.isNotEmpty()) {
                template += "|$imageCaption"
            }
            template += "]]"

            // But before we resort to inserting the image at the top of the wikitext, let's see if
            // the article has an infobox, and if so, see if we can inject the image right in there.

            var insertAtTop = true

            var infoboxStartIndex = -1
            var infoboxEndIndex = -1
            var i = 0
            while (true) {
                i = wikiText.indexOf("{{", i)
                if (i == -1) {
                    break
                }
                i += 2
                val pipePos = wikiText.indexOf("|", i)
                if (pipePos > i) {
                    val templateName = wikiText.substring(i, pipePos).trim()
                    if (templateName.contains("{{") || templateName.contains("}}")) {
                        // template doesn't contain pipe symbol, not what we're looking for.
                        continue
                    }

                    if (templateName.endsWith("box") || templateName.contains("box ")) {
                        infoboxStartIndex = i
                        infoboxEndIndex = wikiText.indexOf("}}", infoboxStartIndex)
                    }
                }
            }

            if (attemptInfobox && (infoboxStartIndex in 0 until infoboxEndIndex)) {
                val regexImage = """\|\s*image(\d+)?\s*(=)\s*(\|)""".toRegex()
                var match = regexImage.find(wikiText, infoboxStartIndex)

                if (match != null && match.groups[3] != null &&
                    match.groups[3]!!.range.first < infoboxEndIndex) {
                    insertAtTop = false

                    var insertPos = match.groups[3]!!.range.first
                    val curImageStr = wikiText.substring(match.groups[2]!!.range.first + 1, match.groups[3]!!.range.first)
                    if (curImageStr.contains("\n")) {
                        insertPos = wikiText.indexOf("\n", match.groups[2]!!.range.first)
                    }

                    wikiText = wikiText.substring(0, insertPos) + namespaceName + ":" + imageTitle + wikiText.substring(insertPos)
                }

                val regexCaption = """\|\s*caption(\d+)?\s*(=)\s*(\|)""".toRegex()
                match = regexCaption.find(wikiText, infoboxStartIndex)
                if (match != null && match.groups[3] != null &&
                    match.groups[3]!!.range.first < infoboxEndIndex) {

                    var insertPos = match.groups[3]!!.range.first
                    val curImageStr = wikiText.substring(match.groups[2]!!.range.first + 1, match.groups[3]!!.range.first)
                    if (curImageStr.contains("\n")) {
                        insertPos = wikiText.indexOf("\n", match.groups[2]!!.range.first)
                    }

                    wikiText = wikiText.substring(0, insertPos) + imageCaption + wikiText.substring(insertPos)
                }

                // TODO: insert image alt text.
            }

            if (insertAtTop) {
                val pos = cursorPos.coerceIn(0, wikiText.length)
                wikiText = wikiText.substring(0, pos) + template + "\n" + wikiText.substring(pos)
            }

            // Save the new wikitext to the article.

            L.d(">>> $wikiText")
            return wikiText
        }
    }
}
