package org.wikipedia.descriptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.liftwing.DescriptionSuggestion
import org.wikipedia.dataclient.liftwing.LiftWingModelService
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.edit.Edit
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class DescriptionEditViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!
    val highlightText = savedStateHandle.get<String>(DescriptionEditActivity.EXTRA_HIGHLIGHT_TEXT)
    val action = savedStateHandle.get<DescriptionEditActivity.Action>(Constants.INTENT_EXTRA_ACTION)!!
    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE)!!
    val sourceSummary = savedStateHandle.get<PageSummaryForEdit>(DescriptionEditActivity.EXTRA_SOURCE_SUMMARY)
    val targetSummary = savedStateHandle.get<PageSummaryForEdit>(DescriptionEditActivity.EXTRA_TARGET_SUMMARY)
    var editingAllowed = true

    private var clientJob: Job? = null

    private val _loadPageSummaryState = MutableStateFlow(Resource<MwServiceError?>())
    val loadPageSummaryState = _loadPageSummaryState.asStateFlow()

    private val _requestSuggestionState = MutableStateFlow(Resource<Triple<DescriptionSuggestion.Response, Int, List<String>>>())
    val requestSuggestionState = _requestSuggestionState.asStateFlow()

    private val _postDescriptionState = MutableStateFlow(Resource<Any>())
    val postDescriptionState = _postDescriptionState.asStateFlow()

    private val _waitForRevisionState = MutableStateFlow(Resource<Boolean>())
    val waitForRevisionState = _waitForRevisionState.asStateFlow()

    fun loadPageSummary() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            _loadPageSummaryState.value = Resource.Loading()
            editingAllowed = false
            val summaryResponse = async { ServiceFactory.getRest(pageTitle.wikiSite).getPageSummary(null, pageTitle.prefixedText) }
            val infoResponse = async { ServiceFactory.get(pageTitle.wikiSite).getWikiTextForSectionWithInfo(pageTitle.prefixedText, 0) }

            val editError = infoResponse.await().query?.firstPage()?.getErrorForAction("edit")
            var error: MwServiceError? = null
            if (editError.isNullOrEmpty()) {
                editingAllowed = true
            } else {
                error = editError[0]
            }
            sourceSummary?.extractHtml = summaryResponse.await().extractHtml
            _loadPageSummaryState.value = Resource.Success(error)
        }
    }

    fun requestSuggestion() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _requestSuggestionState.value = Resource.Error(throwable)
        }) {
            _requestSuggestionState.value = Resource.Loading()
            val responseCall = async { ServiceFactory[pageTitle.wikiSite, LiftWingModelService.API_URL, LiftWingModelService::class.java]
                .getDescriptionSuggestion(DescriptionSuggestion.Request(pageTitle.wikiSite.languageCode, pageTitle.prefixedText, 2)) }
            val userInfoCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite)
                .globalUserInfo(AccountUtil.userName) }

            val response = responseCall.await()
            val userTotalEdits = userInfoCall.await().query?.globalUserInfo?.editCount ?: 0

            // Perform some post-processing on the predictions.
            // 1) Capitalize them, if we're dealing with enwiki.
            // 2) Remove duplicates.
            val list = (if (pageTitle.wikiSite.languageCode == "en") {
                response.prediction.map { StringUtil.capitalize(it)!! }
            } else response.prediction).distinct()

            _requestSuggestionState.value = Resource.Success(Triple(response, userTotalEdits, list))
        }
    }

    fun postDescription(currentDescription: String,
                        editComment: String?,
                        editTags: String?,
                        captchaId: String?,
                        captchaWord: String?) {
        clientJob?.cancel()
        clientJob = viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _postDescriptionState.value = Resource.Error(throwable)
        }) {
            _postDescriptionState.value = Resource.Loading()

            val csrfSite = if (action == DescriptionEditActivity.Action.ADD_CAPTION ||
                action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                Constants.commonsWikiSite
            } else {
                if (shouldWriteToLocalWiki()) pageTitle.wikiSite else Constants.wikidataWikiSite
            }

            val csrfToken = CsrfTokenClient.getToken(csrfSite)

            val response = if (shouldWriteToLocalWiki()) {
                // If the description is being applied to an article on English Wikipedia, it
                // should be written directly to the article instead of Wikidata.
                postDescriptionToArticle(csrfToken, currentDescription, editComment, editTags, captchaId, captchaWord)
            } else {
                postDescriptionToWikidata(csrfToken, currentDescription, editComment, editTags)
            }

            _postDescriptionState.value = Resource.Success(response)
        }
    }

    fun waitForRevisionUpdate(newRevision: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _waitForRevisionState.value = Resource.Error(throwable)
        }) {
            _waitForRevisionState.value = Resource.Loading()
            // Implement a retry mechanism to wait for the revision to be available.
            var retry = 0
            var revision = -1L
            while (revision < newRevision && retry < 10) {
                delay(2000)
                val pageSummaryResponse = ServiceFactory.getRest(pageTitle.wikiSite).getSummaryResponse(pageTitle.prefixedText, cacheControl = OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString())
                revision = pageSummaryResponse.body()?.revision ?: -1L
                retry++
            }
            _waitForRevisionState.value = Resource.Success(true)
        }
    }

    private suspend fun postDescriptionToArticle(csrfToken: String,
                                                 currentDescription: String,
                                                 editComment: String?,
                                                 editTags: String?,
                                                 captchaId: String?,
                                                 captchaWord: String?): Edit {
        val wikiSectionInfoResponse = ServiceFactory.get(pageTitle.wikiSite)
            .getWikiTextForSectionWithInfo(pageTitle.prefixedText, 0)
        val errorForAction = wikiSectionInfoResponse.query?.firstPage()?.getErrorForAction("edit")
        if (!errorForAction.isNullOrEmpty()) {
            val error = errorForAction.first()
            throw MwException(error)
        }
        val firstRevision = wikiSectionInfoResponse.query?.firstPage()?.revisions?.firstOrNull()
        var text = firstRevision?.contentMain.orEmpty()
        val baseRevId = firstRevision?.revId ?: 0
        text = updateDescriptionInArticle(text, currentDescription)
        val automaticallyAddedEditSummary = L10nUtil.getStringForArticleLanguage(pageTitle,
            if (pageTitle.description.isNullOrEmpty()) R.string.edit_summary_added_short_description
            else R.string.edit_summary_updated_short_description)
        var editSummary = automaticallyAddedEditSummary
        editComment?.let {
            editSummary += ", $it"
        }

        return ServiceFactory.get(pageTitle.wikiSite).postEditSubmit(
            title = pageTitle.prefixedText,
            section = "0",
            newSectionTitle = null,
            summary = editSummary,
            user = AccountUtil.assertUser,
            text = text,
            appendText = null,
            baseRevId = baseRevId,
            token = csrfToken,
            captchaId = captchaId,
            captchaWord = captchaWord,
            tags = editTags
        )
    }

    private suspend fun postDescriptionToWikidata(csrfToken: String,
                                                  currentDescription: String,
                                                  editComment: String?,
                                                  editTags: String?): EntityPostResponse {
        val wikiSectionInfoResponse = ServiceFactory.get(pageTitle.wikiSite).getWikiTextForSectionWithInfo(pageTitle.prefixedText, 0)
        val errorForAction = wikiSectionInfoResponse.query?.firstPage()?.getErrorForAction("edit")
        if (!errorForAction.isNullOrEmpty()) {
            val error = errorForAction.first()
            throw MwException(error)
        }
        val siteInfoResponse = ServiceFactory.get(pageTitle.wikiSite).getSiteInfo()

        // TODO: need to revisit this logic
        val languageCode = if (siteInfoResponse.query?.siteInfo?.lang != null &&
            siteInfoResponse.query?.siteInfo?.lang != AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE) siteInfoResponse.query?.siteInfo?.lang.orEmpty()
        else pageTitle.wikiSite.languageCode

        return if (action == DescriptionEditActivity.Action.ADD_CAPTION ||
            action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
            ServiceFactory.get(Constants.commonsWikiSite).postLabelEdit(
                language = languageCode,
                useLang = languageCode,
                site = Constants.COMMONS_DB_NAME,
                title = pageTitle.prefixedText,
                newDescription = currentDescription,
                summary = editComment,
                token = csrfToken,
                user = AccountUtil.assertUser,
                tags = editTags
            )
        } else {
            ServiceFactory.get(Constants.wikidataWikiSite).postDescriptionEdit(
                language = languageCode,
                useLang = languageCode,
                site = pageTitle.wikiSite.dbName(),
                title = pageTitle.prefixedText,
                newDescription = currentDescription,
                summary = editComment,
                token = csrfToken,
                user = AccountUtil.assertUser,
                tags = editTags
            )
        }
    }

    fun shouldWriteToLocalWiki(): Boolean {
        return (action == DescriptionEditActivity.Action.ADD_DESCRIPTION ||
                action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION) &&
                DescriptionEditUtil.wikiUsesLocalDescriptions(pageTitle.wikiSite.languageCode)
    }

    private fun updateDescriptionInArticle(articleText: String, newDescription: String): String {
        return if (articleText.contains(TEMPLATE_PARSE_REGEX.toRegex())) {
            // update existing description template
            articleText.replaceFirst(TEMPLATE_PARSE_REGEX.toRegex(), "$1$newDescription$3")
        } else {
            // add new description template
            "{{${DESCRIPTION_TEMPLATES[0]}|$newDescription}}\n$articleText".trimIndent()
        }
    }

    companion object {
        val DESCRIPTION_TEMPLATES = arrayOf("Short description", "SHORTDESC")
        // Don't remove the ending escaped `\\}`
        @Suppress("RegExpRedundantEscape")
        const val TEMPLATE_PARSE_REGEX = "(\\{\\{[Ss]hort description\\|(?:1=)?)([^}|]+)([^}]*\\}\\})"
    }
}
