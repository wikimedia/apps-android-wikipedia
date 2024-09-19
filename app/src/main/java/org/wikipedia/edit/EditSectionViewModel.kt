package org.wikipedia.edit

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class EditSectionViewModel(bundle: Bundle) : ViewModel() {

    var pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE)!!
    var invokeSource = bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource
    var sectionID = bundle.getInt(EditSectionActivity.EXTRA_SECTION_ID, -1)
    var sectionAnchor = bundle.getString(EditSectionActivity.EXTRA_SECTION_ANCHOR)
    var textToHighlight = bundle.getString(EditSectionActivity.EXTRA_HIGHLIGHT_TEXT)
    var sectionWikitext: String? = null
    var sectionWikitextOriginal: String? = null
    var editingAllowed = false
    val editNotices = mutableListOf<String>()

    // Current revision of the article, to be passed back to the server to detect possible edit conflicts.
    private var currentRevision: Long = 0

    private var clientJob: Job? = null

    private val _fetchSectionTextState = MutableStateFlow(Resource<MwServiceError?>())
    val fetchSectionTextState = _fetchSectionTextState.asStateFlow()

    private val _postEditState = MutableStateFlow(Resource<Edit>())
    val postEditState = _postEditState.asStateFlow()

    private val _waitForRevisionState = MutableStateFlow(Resource<Long>())
    val waitForRevisionState = _waitForRevisionState.asStateFlow()

    init {
        fetchSectionText()
    }

    fun fetchSectionText() {
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

    fun postEdit(isMinorEdit: Boolean?,
                 watchThisPage: String,
                 summaryText: String,
                 editSectionText: String,
                 editTags: String,
                 captchaId: String?,
                 captchaWord: String?) {
        clientJob?.cancel()
        clientJob = viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _postEditState.value = Resource.Error(throwable)
        }) {
            _postEditState.value = Resource.Loading()
            val csrfToken = withContext(Dispatchers.IO) { CsrfTokenClient.getToken(pageTitle.wikiSite).blockingSingle() }
            val result = ServiceFactory.get(pageTitle.wikiSite).postEditSubmitSuspend(
                title = pageTitle.prefixedText,
                section = if (sectionID >= 0) sectionID.toString() else null,
                newSectionTitle = null,
                summary = summaryText,
                user = AccountUtil.assertUser,
                text = editSectionText,
                appendText = null,
                baseRevId = currentRevision,
                token = csrfToken,
                captchaId = captchaId,
                captchaWord = captchaWord,
                minor = isMinorEdit,
                watchlist = watchThisPage,
                tags = editTags
            )
            _postEditState.value = Resource.Success(result)
        }
    }

    fun waitForRevisionUpdate(newRevision: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _waitForRevisionState.value = Resource.Success(newRevision)
        }) {
            // Implement a retry mechanism to wait for the revision to be available.
            var retry = 0
            var revision = -1L
            while (revision < newRevision && retry < 10) {
                delay(2000)
                val pageSummaryResponse = ServiceFactory.getRest(pageTitle.wikiSite)
                    .getSummaryResponseSuspend(pageTitle.prefixedText, cacheControl = OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString())
                revision = pageSummaryResponse.body()?.revision ?: -1L
                retry++
            }
            _waitForRevisionState.value = Resource.Success(revision)
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditSectionViewModel(bundle) as T
        }
    }
}
