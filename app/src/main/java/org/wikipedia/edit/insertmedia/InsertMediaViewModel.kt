package org.wikipedia.edit.insertmedia

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.FileAliasData
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class InsertMediaViewModel(bundle: Bundle) : ViewModel() {

    val invokeSource = bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource
    val wikiSite = bundle.parcelable<WikiSite>(Constants.ARG_WIKISITE)!!
    var searchQuery = StringUtil.removeHTMLTags(StringUtil.removeUnderscores(bundle.getString(InsertMediaActivity.EXTRA_SEARCH_QUERY)!!))
    val originalSearchQuery = searchQuery
    var selectedImage = bundle.parcelable<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE)
    var selectedImageSource = bundle.getString(InsertMediaActivity.EXTRA_IMAGE_SOURCE).orEmpty()
    var selectedImageSourceProjects = bundle.getString(InsertMediaActivity.EXTRA_IMAGE_SOURCE_PROJECTS).orEmpty()
    var imagePosition: String = bundle.getString(InsertMediaActivity.RESULT_IMAGE_POS, IMAGE_POSITION_RIGHT)
    var imageType: String = bundle.getString(InsertMediaActivity.RESULT_IMAGE_TYPE, IMAGE_TYPE_THUMBNAIL)
    var imageSize: String = bundle.getString(InsertMediaActivity.RESULT_IMAGE_SIZE, IMAGE_SIZE_DEFAULT)

    val insertMediaFlow = Pager(PagingConfig(pageSize = 10)) {
        InsertMediaPagingSource(searchQuery)
    }.flow.cachedIn(viewModelScope)

    init {
        loadMagicWords()
    }

    class InsertMediaPagingSource(val searchQuery: String) : PagingSource<Int, PageTitle>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PageTitle> {
            return try {
                val wikiSite = WikiSite(Service.COMMONS_URL)
                val response = ServiceFactory.get(WikiSite(Service.COMMONS_URL))
                    .fullTextSearchCommons(searchQuery, params.loadSize, params.key)

                return response.query?.pages?.let { list ->
                    val results = list.sortedBy { it.index }.filter { it.imageInfo() != null }.map {
                        val pageTitle = PageTitle(it.title, wikiSite, it.thumbUrl())
                        // since this is an imageinfo query, the thumb URL and description will
                        // come from image metadata.
                        it.imageInfo()?.let { imageInfo ->
                            pageTitle.thumbUrl = imageInfo.thumbUrl
                            pageTitle.description = imageInfo.metadata?.imageDescription()
                        }
                        pageTitle
                    }
                    LoadResult.Page(results, params.key, if (response.continuation?.gsroffset != 0)
                        response.continuation?.gsroffset else null)
                } ?: run {
                    LoadResult.Page(emptyList(), params.key, null)
                }
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, PageTitle>): Int? {
            return null
        }
    }

    @Suppress("KotlinConstantConditions")
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

    class InfoboxVars(
        val templateNameContains: String,
        val imageParamName: String,
        val imageCaptionParamName: String,
        val imageAltParamName: String,
    )

    class InfoboxVarsCollection(
        val templateNameRegex: String,
        val defaultImageParamName: String,
        val defaultImageCaptionParamName: String,
        val defaultImageAltParamName: String,
        val possibleNameParamNames: Array<String>,
        val vars: List<InfoboxVars>
    )

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

        private val infoboxVarsByLang = mapOf(
            "en" to InfoboxVarsCollection("infobox|(?:(?:automatic[ |_])?taxobox)|chembox|drugbox|speciesbox",
                "image", "caption", "alt",
                arrayOf("name", "official_name", "species", "taxon", "drug_name", "image"),
                listOf(
                    InfoboxVars("taxobox", "image", "image_caption", "image_alt"),
                    InfoboxVars("chembox", "ImageFile", "ImageCaption", "ImageAlt"),
                    InfoboxVars("speciesbox", "image", "image_caption", "image_alt"),
                    InfoboxVars("infobox.*place", "image_skyline", "image_caption", "image_alt"),
                    InfoboxVars("infobox.*settlement", "image_skyline", "image_caption", "image_alt")
                )
            ),
        )

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
                                    cursorPos: Int = 0, autoInsert: Boolean = false, attemptInfobox: Boolean = false): Triple<String, Boolean, Pair<Int, Int>> {
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
            var insertLocation = 0
            var insertLength = 0

            val infoboxVars = infoboxVarsByLang[langCode] ?: infoboxVarsByLang["en"]!!

            // initialize template parameters to the defaults for Infobox
            var imageParamName = infoboxVars.defaultImageParamName
            var imageCaptionParamName = infoboxVars.defaultImageCaptionParamName
            var imageAltParamName = infoboxVars.defaultImageAltParamName

            val infoboxMatch = """\{\{\s*((${infoboxVars.templateNameRegex})[^|]*)\|.*\}\}""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(wikiText)
            val infoBoxName = infoboxMatch?.groupValues?.get(1).orEmpty().lowercase().trim()

            infoboxVars.vars.forEach {
                if (infoBoxName.contains(it.templateNameContains.toRegex(RegexOption.IGNORE_CASE))) {
                    imageParamName = it.imageParamName
                    imageCaptionParamName = it.imageCaptionParamName
                    imageAltParamName = it.imageAltParamName
                }
            }

            if (autoInsert && attemptInfobox && infoboxMatch != null) {
                val infoboxStartIndex = infoboxMatch.range.first
                val infoboxEndIndex = infoboxMatch.range.last

                val haveNameParam = findNameParamInTemplate(infoboxVars.possibleNameParamNames, wikiText, infoboxStartIndex, infoboxEndIndex).first != -1

                if (haveNameParam) {
                    var match = """\|\s*$imageParamName\s*=([^|]*)(\|)""".toRegex()
                        .find(wikiText, infoboxStartIndex)
                    var group1 = match?.groups?.get(1)
                    var group2 = match?.groups?.get(2)

                    if (group1 != null && group2 != null && group2.range.first < infoboxEndIndex) {
                        // an 'image' parameter already exists, so try to insert the image into it,
                        // provided that it's empty.

                        var insertPos = group2.range.first
                        val valueStr = group1.value

                        if (valueStr.trim().isEmpty()) {
                            if (valueStr.contains("\n")) {
                                insertPos = group1.range.first + valueStr.indexOf("\n")
                            }
                            insertedIntoInfobox = true
                            wikiText = wikiText.substring(0, insertPos) + imageTitle + wikiText.substring(insertPos)
                        } else {
                            // image parameter already has a value, so do nothing.
                        }
                    } else {
                        // no 'image' parameter exists, so insert one at the end of the 'name' parameter.
                        val (paramInsertPos, paramNameSpaceConvention) = findNameParamInTemplate(infoboxVars.possibleNameParamNames, wikiText, infoboxStartIndex, infoboxEndIndex)
                        if (paramInsertPos > 0) {
                            insertedIntoInfobox = true
                            val insertText = "|$paramNameSpaceConvention$imageParamName = $imageTitle\n"
                            wikiText = wikiText.substring(0, paramInsertPos) + insertText + wikiText.substring(paramInsertPos)
                        }
                    }

                    if (insertedIntoInfobox && imageCaption.isNotEmpty()) {
                        // now try to insert the caption
                        match = """\|\s*$imageCaptionParamName\s*=([^|]*)(\|)""".toRegex()
                            .find(wikiText, infoboxStartIndex)
                        group1 = match?.groups?.get(1)
                        group2 = match?.groups?.get(2)
                        if (group1 != null && group2 != null && group2.range.first < infoboxEndIndex) {
                            // Caption field already exists, so insert into it.
                            var insertPos = group2.range.first
                            val valueStr = group1.value
                            if (valueStr.trim().isEmpty()) {
                                if (valueStr.contains("\n")) {
                                    insertPos = group1.range.first + valueStr.indexOf("\n")
                                }
                                wikiText = wikiText.substring(0, insertPos) + imageCaption + wikiText.substring(insertPos)
                            }
                        } else {
                            // insert a new caption field
                            val (paramInsertPos, paramNameSpaceConvention) = findNameParamInTemplate(infoboxVars.possibleNameParamNames, wikiText, infoboxStartIndex, infoboxEndIndex)
                            if (paramInsertPos > 0) {
                                val insertText = "|$paramNameSpaceConvention$imageCaptionParamName = $imageCaption\n"
                                wikiText = wikiText.substring(0, paramInsertPos) + insertText + wikiText.substring(paramInsertPos)
                            }
                        }
                    }

                    if (insertedIntoInfobox && imageAltText.isNotEmpty() && imageAltParamName.isNotEmpty()) {
                        // now try to insert alt text!
                        match = """\|\s*$imageAltParamName\s*=([^|]*)(\|)""".toRegex()
                            .find(wikiText, infoboxStartIndex)
                        group1 = match?.groups?.get(1)
                        group2 = match?.groups?.get(2)
                        if (group1 != null && group2 != null && group2.range.first < infoboxEndIndex) {
                            // Caption field already exists, so insert into it.
                            var insertPos = group2.range.first
                            val valueStr = group1.value
                            if (valueStr.trim().isEmpty()) {
                                if (valueStr.contains("\n")) {
                                    insertPos = group1.range.first + valueStr.indexOf("\n")
                                }
                                wikiText = wikiText.substring(0, insertPos) + imageAltText + wikiText.substring(insertPos)
                            }
                        } else {
                            // insert a new alt-text field
                            val (paramInsertPos, paramNameSpaceConvention) = findNameParamInTemplate(infoboxVars.possibleNameParamNames, wikiText, infoboxStartIndex, infoboxEndIndex)
                            if (paramInsertPos > 0) {
                                val insertText = "|$paramNameSpaceConvention$imageAltParamName = $imageAltText\n"
                                wikiText = wikiText.substring(0, paramInsertPos) + insertText + wikiText.substring(paramInsertPos)
                            }
                        }
                    }
                }
            }

            if (!insertedIntoInfobox) {
                // no infobox, so insert the image at the top of the page, but after any templates
                // that might be hatnotes, etc.

                var braceLevel = 0
                insertLocation = cursorPos

                if (autoInsert) {
                    for (i in wikiText.indices) {
                        if (wikiText[i] == '{') {
                            braceLevel++
                        } else if (wikiText[i] == '}') {
                            braceLevel--
                        } else if (braceLevel == 0) {
                            if (!wikiText[i].isWhitespace()) {
                                insertLocation = i
                                break
                            }
                        }
                    }
                }

                insertLocation = insertLocation.coerceIn(0, wikiText.length)
                val insertText = template + "\n"
                insertLength = insertText.length
                wikiText = wikiText.substring(0, insertLocation) + insertText + wikiText.substring(insertLocation)
            }

            return Triple(wikiText, insertedIntoInfobox, Pair(insertLocation, insertLength))
        }

        private fun findNameParamInTemplate(possibleNameParamNames: Array<String>, wikiText: String, startIndex: Int, endIndex: Int): Pair<Int, String> {
            var paramInsertPos = -1
            var paramNameSpaceConvention = ""

            possibleNameParamNames.forEach { nameParam ->
                if (paramInsertPos != -1) {
                    return@forEach
                }
                val regexName = """\|(\s*)$nameParam\s*=([^|]*)(\|)""".toRegex()
                val nameMatch = regexName.find(wikiText, startIndex)
                val group1 = nameMatch?.groups?.get(1)
                val group3 = nameMatch?.groups?.get(3)
                if (group1 != null && group3 != null && group3.range.first < endIndex) {
                    paramInsertPos = group3.range.first
                    paramNameSpaceConvention = group1.value
                }
            }
            return Pair(paramInsertPos, paramNameSpaceConvention)
        }
    }
}
