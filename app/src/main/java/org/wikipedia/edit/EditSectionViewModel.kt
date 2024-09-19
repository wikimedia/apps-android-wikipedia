package org.wikipedia.edit

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
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class EditSectionViewModel(bundle: Bundle) : ViewModel() {

    var pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE)!!
    var invokeSource = bundle.getString(Constants.INTENT_EXTRA_INVOKE_SOURCE)
    var sectionID = bundle.getInt(EditSectionActivity.EXTRA_SECTION_ID, -1)
    var sectionAnchor = bundle.getString(EditSectionActivity.EXTRA_SECTION_ANCHOR)
    var textToHighlight  = bundle.getString(EditSectionActivity.EXTRA_HIGHLIGHT_TEXT)
    var sectionWikitext: String? = null
    var sectionWikitextOriginal: String? = null
    var editingAllowed = false
    val editNotices = mutableListOf<String>()

    // Current revision of the article, to be passed back to the server to detect possible edit conflicts.
    var currentRevision: Long = 0

    private val _fetchSectionTextState = MutableStateFlow(Resource<MwServiceError?>())
    val fetchSectionTextState = _fetchSectionTextState.asStateFlow()

    init {
        fetchSectionText()
    }

    private fun fetchSectionText() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _fetchSectionTextState.value = Resource.Error(throwable)
        }) {
            _fetchSectionTextState.value = Resource.Loading()

            val infoResponse = withContext(Dispatchers.IO) {
                ServiceFactory.get(pageTitle.wikiSite).getWikiTextForSectionWithInfoSuspend(pageTitle.prefixedText, if (sectionID >= 0) sectionID else null)
            }

            infoResponse.query?.firstPage()?.let { firstPage ->
                val rev = firstPage.revisions.first()

                pageTitle = PageTitle(firstPage.title, pageTitle.wikiSite).apply {
                    this.displayText = pageTitle.displayText
                }
                sectionWikitext = rev.contentMain
                sectionWikitextOriginal = sectionWikitext
                currentRevision = rev.revId

                editNotices.clear()
                // Populate edit notices, but filter out anonymous edit warnings, since
                // we show that type of warning ourselves when previewing.
                editNotices.addAll(firstPage.getEditNotices()
                    .filterKeys { key -> (key.startsWith("editnotice") && !key.endsWith("-notext")) }
                    .values.filter { str -> StringUtil.fromHtml(str).trim().isNotEmpty() })

                val editError = firstPage.getErrorForAction("edit")
                var error: MwServiceError? = null
                if (editError.isEmpty()) {
                    editingAllowed = true
                } else {
                    error = editError[0]
                }
                
                _fetchSectionTextState.value = Resource.Success(error)
            }
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditSectionViewModel(bundle) as T
        }
    }
}
