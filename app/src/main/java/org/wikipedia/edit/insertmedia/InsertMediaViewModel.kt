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

    var imagePosition: String = bundle.getString(InsertMediaActivity.RESULT_IMAGE_POS, IMAGE_POSITION_RIGHT)
    var imageType: String = bundle.getString(InsertMediaActivity.RESULT_IMAGE_TYPE, IMAGE_TYPE_THUMBNAIL)
    var imageSize: String = bundle.getString(InsertMediaActivity.RESULT_IMAGE_SIZE, IMAGE_SIZE_DEFAULT)

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
                    .fullTextSearchCommons(searchQuery, params.key?.gsroffset?.toString(), params.loadSize, params.key?.continuation)

                return response.query?.pages?.let { list ->
                    val results = list.sortedBy { it.index }.map {
                        val pageTitle = PageTitle(it.title, wikiSite, it.thumbUrl())
                        // since this is an imageinfo query, the thumb URL and description will
                        // come from image metadata.
                        it.imageInfo()?.let { imageInfo ->
                            pageTitle.thumbUrl = imageInfo.thumbUrl
                            pageTitle.description = imageInfo.metadata?.imageDescription()
                        }
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

        initMagicWords()

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

        fun initMagicWords() {
            magicWords[IMAGE_POSITION_NONE] = "none"
            magicWords[IMAGE_POSITION_CENTER] = "center"
            magicWords[IMAGE_POSITION_LEFT] = "left"
            magicWords[IMAGE_POSITION_RIGHT] = "right"
            magicWords[IMAGE_TYPE_THUMBNAIL] = "thumb"
            magicWords[IMAGE_TYPE_FRAMELESS] = "frameless"
            magicWords[IMAGE_TYPE_FRAME] = "frame"
            magicWords[IMAGE_ALT_TEXT] = "alt=$1"
        }

        fun insertImageIntoWikiText(langCode: String, oldWikiText: String, imageTitle: String, imageCaption: String,
                                    imageAltText: String, imageSize: String, imageType: String, imagePos: String,
                                    cursorPos: Int = 0, attemptInfobox: Boolean = false): String {
            var wikiText = oldWikiText
            val namespaceName = FileAliasData.valueFor(langCode)

            var template = "[[$namespaceName:$imageTitle"
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

            var insertedIntoInfobox = false

            // initialize template parameters to the defaults for Infobox
            var imageParamName = "image"
            var imageCaptionParamName = "caption"
            var imageAltParamName = "alt"

            val infoboxNames = listOf("infobox", "(?:(?:automatic[ |_])?taxobox)", "chembox", "drugbox", "speciesbox").joinToString("|")
            val infoboxMatch = """\{\{\s*($infoboxNames).*\}\}""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(wikiText)
            val infoBoxName = infoboxMatch?.groupValues?.get(1).orEmpty().lowercase()

            when  {
                infoBoxName.contains("taxobox") -> {
                    imageCaptionParamName = "image_caption"
                    imageAltParamName = "image_alt"
                }
                infoBoxName.contains("speciesbox") -> {
                    imageCaptionParamName = "image_caption"
                    imageAltParamName = "image_alt"
                }
                infoBoxName.contains("chembox") -> {
                    imageParamName = "ImageFile"
                    imageCaptionParamName = "ImageCaption"
                    imageAltParamName = "ImageAlt"
                }
            }

            if (attemptInfobox && infoboxMatch != null) {
                val infoboxStartIndex = infoboxMatch.range.first
                val infoboxEndIndex = infoboxMatch.range.last

                var haveNameParam = false
                var paramInsertPos = -1
                var paramNameSpaceConvention = ""

                listOf("name", "species", "taxon", "drug_name").forEach { nameParam ->
                    if (haveNameParam) {
                        return@forEach
                    }
                    val regexName = """\|(\s*)$nameParam\s*=([^|]*)(\|)""".toRegex()
                    val nameMatch = regexName.find(wikiText, infoboxStartIndex)
                    if (nameMatch != null && nameMatch.groups[3] != null && nameMatch.groups[3]!!.range.first < infoboxEndIndex) {
                        haveNameParam = true
                        paramInsertPos = nameMatch.groups[3]!!.range.first
                        paramNameSpaceConvention = nameMatch.groups[1]!!.value
                    }
                }

                if (haveNameParam) {
                    var match = """\|\s*$imageParamName\s*=([^|]*)(\|)""".toRegex()
                        .find(wikiText, infoboxStartIndex)

                    if (match != null && match.groups[2] != null && match.groups[2]!!.range.first < infoboxEndIndex) {
                        // an 'image' parameter already exists, so try to insert the image into it,
                        // provided that it's empty.

                        var insertPos = match.groups[2]!!.range.first
                        val valueStr = match.groups[1]!!.value

                        if (valueStr.trim().isEmpty()) {
                            if (valueStr.contains("\n")) {
                                insertPos = match.groups[1]!!.range.first + valueStr.indexOf("\n")
                            }
                            insertedIntoInfobox = true
                            wikiText = wikiText.substring(0, insertPos) + imageTitle + wikiText.substring(insertPos)
                        } else {
                            // image parameter already has a value, so do nothing.
                        }
                    } else {
                        // no 'image' parameter exists, so insert one at the end of the 'name' parameter.
                        insertedIntoInfobox = true
                        val insertText = "|$paramNameSpaceConvention$imageParamName = $imageTitle\n"
                        wikiText = wikiText.substring(0, paramInsertPos) + insertText + wikiText.substring(paramInsertPos)
                        paramInsertPos += insertText.length
                    }

                    if (insertedIntoInfobox && imageCaption.isNotEmpty()) {
                        // now try to insert the caption
                        match = """\|\s*$imageCaptionParamName\s*=([^|]*)(\|)""".toRegex()
                            .find(wikiText, infoboxStartIndex)
                        if (match != null && match.groups[2] != null && match.groups[2]!!.range.first < infoboxEndIndex) {
                            // Caption field already exists, so insert into it.
                            var insertPos = match.groups[2]!!.range.first
                            val valueStr = match.groups[1]!!.value
                            if (valueStr.trim().isEmpty()) {
                                if (valueStr.contains("\n")) {
                                    insertPos = match.groups[1]!!.range.first + valueStr.indexOf("\n")
                                }
                                wikiText = wikiText.substring(0, insertPos) + imageCaption + wikiText.substring(insertPos)
                            }
                        } else {
                            // insert a new caption field
                            val insertText = "|$paramNameSpaceConvention$imageCaptionParamName = $imageCaption\n"
                            wikiText = wikiText.substring(0, paramInsertPos) + insertText + wikiText.substring(paramInsertPos)
                            paramInsertPos += insertText.length
                        }
                    }

                    if (insertedIntoInfobox && imageAltText.isNotEmpty() && imageAltParamName.isNotEmpty()) {
                        // now try to insert the caption
                        match = """\|\s*$imageAltParamName\s*=([^|]*)(\|)""".toRegex()
                            .find(wikiText, infoboxStartIndex)
                        if (match != null && match.groups[2] != null && match.groups[2]!!.range.first < infoboxEndIndex) {
                            // Caption field already exists, so insert into it.
                            var insertPos = match.groups[2]!!.range.first
                            val valueStr = match.groups[1]!!.value
                            if (valueStr.trim().isEmpty()) {
                                if (valueStr.contains("\n")) {
                                    insertPos = match.groups[1]!!.range.first + valueStr.indexOf("\n")
                                }
                                wikiText = wikiText.substring(0, insertPos) + imageCaption + wikiText.substring(insertPos)
                            }
                        } else {
                            // insert a new alt-text field
                            val insertText = "|$paramNameSpaceConvention$imageAltParamName = $imageAltText\n"
                            wikiText = wikiText.substring(0, paramInsertPos) + insertText + wikiText.substring(paramInsertPos)
                            paramInsertPos += insertText.length
                        }
                    }
                }
            }

            if (!insertedIntoInfobox) {
                val pos = cursorPos.coerceIn(0, wikiText.length)
                wikiText = wikiText.substring(0, pos) + template + "\n" + wikiText.substring(pos)
            }

            return wikiText
        }
    }
}
