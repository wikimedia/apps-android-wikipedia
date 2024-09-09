package org.wikipedia.descriptions

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.liftwing.DescriptionSuggestion
import org.wikipedia.dataclient.liftwing.LiftWingModelService
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.edit.EditTags
import org.wikipedia.extensions.parcelable
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.io.IOException
import java.util.concurrent.TimeUnit

class DescriptionEditViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE)!!
    val highlightText = bundle.getString(DescriptionEditFragment.ARG_HIGHLIGHT_TEXT)
    val action = bundle.getSerializable(DescriptionEditFragment.ARG_ACTION) as DescriptionEditActivity.Action
    val invokeSource = bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource
    val sourceSummary = bundle.parcelable<PageSummaryForEdit>(DescriptionEditFragment.ARG_SOURCE_SUMMARY)
    val targetSummary = bundle.parcelable<PageSummaryForEdit>(DescriptionEditFragment.ARG_TARGET_SUMMARY)
    var editingAllowed = false

    private val _loadPageSummaryState = MutableStateFlow(Resource<Boolean>())
    val loadPageSummaryState = _loadPageSummaryState.asStateFlow()

    private val _requestSuggestionState = MutableStateFlow(Resource<Triple<DescriptionSuggestion.Response, Int, List<String>>>())
    val requestSuggestionState = _requestSuggestionState.asStateFlow()

    private val _postDescriptionState = MutableStateFlow(Resource<Boolean>())
    val postDescriptionState = _postDescriptionState.asStateFlow()

    fun loadPageSummary() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            _loadPageSummaryState.value = Resource.Loading()
            val summaryResponse = async {ServiceFactory.getRest(pageTitle.wikiSite).getPageSummary(null, pageTitle.prefixedText) }
            val infoResponse = async { ServiceFactory.get(pageTitle.wikiSite).getWikiTextForSectionWithInfoSuspend(pageTitle.prefixedText, 0) }

            val editError = infoResponse.await().query?.firstPage()?.getErrorForAction("edit")
            if (editError.isNullOrEmpty()) {
                editingAllowed = true
            } else {
                val error = editError[0]
                _loadPageSummaryState.value = Resource.Error(MwException(error))
            }
            sourceSummary?.extractHtml = summaryResponse.await().extractHtml
            _loadPageSummaryState.value = Resource.Success(true)
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
            val userInfo = userInfoCall.await()
            val userTotalEdits = userInfo.query?.globalUserInfo?.editCount ?: 0

            // Perform some post-processing on the predictions.
            // 1) Capitalize them, if we're dealing with enwiki.
            // 2) Remove duplicates.
            val list = (if (pageTitle.wikiSite.languageCode == "en") {
                response.prediction.map { StringUtil.capitalize(it)!! }
            } else response.prediction).distinct()

            _requestSuggestionState.value = Resource.Success(Triple(response, userTotalEdits, list))
        }
    }

    fun postDescription(captchaHandler: CaptchaHandler, currentDescription: String, editComment: String?) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            _postDescriptionState.value = Resource.Loading()

            val csrfSite = if (action == DescriptionEditActivity.Action.ADD_CAPTION ||
                action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                Constants.commonsWikiSite
            } else {
                if (shouldWriteToLocalWiki()) pageTitle.wikiSite else Constants.wikidataWikiSite
            }

            val csrfToken = withContext(Dispatchers.IO) { CsrfTokenClient.getToken(csrfSite).blockingSingle() }

            if (shouldWriteToLocalWiki()) {
                // If the description is being applied to an article on English Wikipedia, it
                // should be written directly to the article instead of Wikidata.
                postDescriptionToArticle(csrfToken, captchaHandler, currentDescription, editComment)
            } else {
                postDescriptionToWikidata(csrfToken)
            }

            _postDescriptionState.value = Resource.Success(response)
        }
    }

    private suspend fun postDescriptionToArticle(csrfToken: String, captchaHandler: CaptchaHandler, currentDescription: String, editComment: String?) {
        val wikiSectionInfoResponse = ServiceFactory.get(pageTitle.wikiSite).getWikiTextForSectionWithInfoSuspend(pageTitle.prefixedText, 0)
        val errorForAction = wikiSectionInfoResponse.query?.firstPage()?.getErrorForAction("edit")
        if (!errorForAction.isNullOrEmpty()) {
            val error = errorForAction.first()
            throw MwException(error)
        }
        val firstRevision = wikiSectionInfoResponse.query?.firstPage()?.revisions?.firstOrNull()
        var text = firstRevision?.contentMain.orEmpty()
        val baseRevId = firstRevision?.revId ?: 0
        text = updateDescriptionInArticle(text, currentDescription)
        val automaticallyAddedEditSummary = WikipediaApp.instance.getString(
            if (pageTitle.description.isNullOrEmpty()) R.string.edit_summary_added_short_description
            else R.string.edit_summary_updated_short_description
        )
        var editSummary = automaticallyAddedEditSummary
        editComment?.let {
            editSummary += ", $it"
        }

        val result = ServiceFactory.get(pageTitle.wikiSite).postEditSubmit(
            pageTitle.prefixedText, "0", null, editSummary,
            AccountUtil.assertUser, text, null, baseRevId, csrfToken,
            if (captchaHandler.isActive) captchaHandler.captchaId() else null,
            if (captchaHandler.isActive) captchaHandler.captchaWord() else null, tags = getEditTags()
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                result.edit?.run {
                    when {
                        editSucceeded -> {
                            AnonymousNotificationHelper.onEditSubmitted()
                            waitForUpdatedRevision(newRevId)
                            EditAttemptStepEvent.logSaveSuccess(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                            analyticsHelper.logSuccess(requireContext(), pageTitle, newRevId)
                            ImageRecommendationsEvent.logEditSuccess(action, pageTitle.wikiSite.languageCode, newRevId)
                        }
                        hasEditErrorCode -> {
                            editFailed(MwException(MwServiceError(code, spamblacklist)), false)
                        }
                        hasCaptchaResponse -> {
                            binding.fragmentDescriptionEditView.showProgressBar(false)
                            binding.fragmentDescriptionEditView.setSaveState(false)
                            captchaHandler.handleCaptcha(null, CaptchaResult(result.edit.captchaId))
                        }
                        hasSpamBlacklistResponse -> {
                            editFailed(MwException(MwServiceError(code, info)), false)
                        }
                        else -> {
                            editFailed(IOException("Received unrecognized edit response"), true)
                        }
                    }
                } ?: run {
                    editFailed(IOException("An unknown error occurred."), true)
                }
            }) { caught -> editFailed(caught, true) })
    }

    private fun postDescriptionToWikidata(editToken: String) {
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode)).getWikiTextForSectionWithInfo(pageTitle.prefixedText, 0)
            .subscribeOn(Schedulers.io())
            .flatMap { response ->
                if (response.query?.firstPage()!!.getErrorForAction("edit").isNotEmpty()) {
                    val error = response.query?.firstPage()!!.getErrorForAction("edit")[0]
                    throw MwException(error)
                }
                ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode)).siteInfo
            }
            .flatMap { response ->
                val languageCode = if (response.query?.siteInfo?.lang != null &&
                    response.query?.siteInfo?.lang != AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE) response.query?.siteInfo?.lang
                else pageTitle.wikiSite.languageCode
                getPostObservable(editToken, languageCode.orEmpty())
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                AnonymousNotificationHelper.onEditSubmitted()
                if (response.success > 0) {
                    requireView().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(4))
                    analyticsHelper.logSuccess(requireContext(), pageTitle, response.entity?.lastRevId ?: 0)
                    ImageRecommendationsEvent.logEditSuccess(action, pageTitle.wikiSite.languageCode, response.entity?.lastRevId ?: 0)
                    EditAttemptStepEvent.logSaveSuccess(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                } else {
                    editFailed(RuntimeException("Received unrecognized description edit response"), true)
                }
            }) { caught ->
                if (caught is MwException) {
                    val error = caught.error
                    if (error.badLoginState() || error.badToken()) {
                        getEditTokenThenSave()
                    } else {
                        editFailed(caught, true)
                    }
                } else {
                    editFailed(caught, true)
                }
            })
    }

    private fun shouldWriteToLocalWiki(): Boolean {
        return (action == DescriptionEditActivity.Action.ADD_DESCRIPTION ||
                action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION) &&
                DescriptionEditUtil.wikiUsesLocalDescriptions(pageTitle.wikiSite.languageCode)
    }

    private fun updateDescriptionInArticle(articleText: String, newDescription: String): String {
        return if (articleText.contains(DescriptionEditFragment.TEMPLATE_PARSE_REGEX.toRegex())) {
            // update existing description template
            articleText.replaceFirst(DescriptionEditFragment.TEMPLATE_PARSE_REGEX.toRegex(), "$1$newDescription$3")
        } else {
            // add new description template
            "{{${DescriptionEditFragment.DESCRIPTION_TEMPLATES[0]}|$newDescription}}\n$articleText".trimIndent()
        }
    }

    private fun getEditTags(): String? {
        val tags = mutableListOf<String>()

        if (invokeSource == InvokeSource.SUGGESTED_EDITS) {
            tags.add(EditTags.APP_SUGGESTED_EDIT)
        }

        when (action) {
            DescriptionEditActivity.Action.ADD_DESCRIPTION -> {
                if (binding.fragmentDescriptionEditView.wasSuggestionChosen) {
                    tags.add(EditTags.APP_DESCRIPTION_ADD)
                    tags.add(EditTags.APP_AI_ASSIST)
                } else if (pageTitle.description.isNullOrEmpty()) {
                    tags.add(EditTags.APP_DESCRIPTION_ADD)
                } else {
                    tags.add(EditTags.APP_DESCRIPTION_CHANGE)
                }
            }
            DescriptionEditActivity.Action.ADD_CAPTION -> {
                tags.add(EditTags.APP_IMAGE_CAPTION_ADD)
            }
            DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> {
                tags.add(EditTags.APP_DESCRIPTION_TRANSLATE)
            }
            DescriptionEditActivity.Action.TRANSLATE_CAPTION -> {
                tags.add(EditTags.APP_IMAGE_CAPTION_TRANSLATE)
            }
            else -> { }
        }

        return if (tags.isEmpty()) null else tags.joinToString(",")
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DescriptionEditViewModel(bundle) as T
        }
    }
}
