package org.wikipedia.descriptions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.eventplatform.ABTest.Companion.GROUP_1
import org.wikipedia.analytics.eventplatform.ABTest.Companion.GROUP_3
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.auth.AccountUtil
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.databinding.FragmentDescriptionEditBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsSurvey
import org.wikipedia.suggestededits.SuggestionsActivity
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import java.io.IOException
import java.lang.Runnable
import java.util.*
import java.util.concurrent.TimeUnit

class DescriptionEditFragment : Fragment() {
    interface Callback {
        fun onDescriptionEditSuccess()
        fun onBottomBarContainerClicked(action: DescriptionEditActivity.Action)
    }

    private var _binding: FragmentDescriptionEditBinding? = null
    val binding get() = _binding!!
    private lateinit var invokeSource: InvokeSource
    private lateinit var pageTitle: PageTitle
    lateinit var action: DescriptionEditActivity.Action
    private var sourceSummary: PageSummaryForEdit? = null
    private var targetSummary: PageSummaryForEdit? = null
    private var highlightText: String? = null
    private var editingAllowed = true
    private lateinit var captchaHandler: CaptchaHandler

    private val analyticsHelper = MachineGeneratedArticleDescriptionsAnalyticsHelper()

    private val disposables = CompositeDisposable()

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            binding.fragmentDescriptionEditView.loadReviewContent(binding.fragmentDescriptionEditView.showingReviewContent())
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
        }
    }

    private val editSuccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        callback()?.onDescriptionEditSuccess()
    }

    private val voiceSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val voiceSearchResult = it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (it.resultCode == Activity.RESULT_OK && voiceSearchResult != null) {
            val text = voiceSearchResult.first()
            binding.fragmentDescriptionEditView.description = text
        }
    }

    private val successRunnable = Runnable {
        if (!isAdded) {
            return@Runnable
        }
        if (!AccountUtil.isLoggedIn) {
            Prefs.incrementTotalAnonDescriptionsEdited()
        }
        if (invokeSource == InvokeSource.SUGGESTED_EDITS) {
            SuggestedEditsSurvey.onEditSuccess()
        }
        Prefs.lastDescriptionEditTime = Date().time
        Prefs.isSuggestedEditsReactivationPassStageOne = false
        binding.fragmentDescriptionEditView.setSaveState(false)
        if (Prefs.showDescriptionEditSuccessPrompt && invokeSource != InvokeSource.SUGGESTED_EDITS) {
            editSuccessLauncher.launch(DescriptionEditSuccessActivity.newIntent(requireContext(), invokeSource))
            Prefs.showDescriptionEditSuccessPrompt = false
        } else {
            val intent = Intent()
            intent.putExtra(SuggestionsActivity.EXTRA_SOURCE_ADDED_CONTRIBUTION, binding.fragmentDescriptionEditView.description)
            intent.putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            intent.putExtra(Constants.INTENT_EXTRA_ACTION, action)
            requireActivity().setResult(Activity.RESULT_OK, intent)
            DeviceUtil.hideSoftKeyboard(requireActivity())
            requireActivity().finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageTitle = requireArguments().getParcelable(ARG_TITLE)!!
        highlightText = requireArguments().getString(ARG_HIGHLIGHT_TEXT)
        action = requireArguments().getSerializable(ARG_ACTION) as DescriptionEditActivity.Action
        invokeSource = requireArguments().getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
        requireArguments().getParcelable<PageSummaryForEdit>(ARG_SOURCE_SUMMARY)?.let {
            sourceSummary = it
        }
        requireArguments().getParcelable<PageSummaryForEdit>(ARG_TARGET_SUMMARY)?.let {
            targetSummary = it
        }
        EditAttemptStepEvent.logInit(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentDescriptionEditBinding.inflate(inflater, container, false)
        loadPageSummaryIfNeeded(savedInstanceState)

        binding.fragmentDescriptionEditView.setLoginCallback {
            val loginIntent = LoginActivity.newIntent(requireActivity(), LoginActivity.SOURCE_EDIT)
            loginLauncher.launch(loginIntent)
        }
        captchaHandler = CaptchaHandler(requireActivity(), pageTitle.wikiSite, binding.fragmentDescriptionEditView.getCaptchaContainer().root,
            binding.fragmentDescriptionEditView.getDescriptionEditTextView(), "", null)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        analyticsHelper.timer.pause()
    }

    override fun onResume() {
        super.onResume()
        analyticsHelper.timer.resume()
    }

    override fun onDestroyView() {
        captchaHandler.dispose()
        binding.fragmentDescriptionEditView.callback = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        cancelCalls()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_DESCRIPTION, binding.fragmentDescriptionEditView.description)
        outState.putBoolean(ARG_REVIEWING, binding.fragmentDescriptionEditView.showingReviewContent())
    }

    private fun cancelCalls() {
        disposables.clear()
    }

    private fun loadPageSummaryIfNeeded(savedInstanceState: Bundle?) {
        binding.fragmentDescriptionEditView.showProgressBar(true)
        if ((invokeSource == InvokeSource.PAGE_ACTIVITY || invokeSource == InvokeSource.PAGE_EDIT_PENCIL ||
                    invokeSource == InvokeSource.PAGE_EDIT_HIGHLIGHT) && sourceSummary?.extractHtml.isNullOrEmpty()) {
            editingAllowed = false
            binding.fragmentDescriptionEditView.setEditAllowed(false)
            binding.fragmentDescriptionEditView.showProgressBar(true)
            disposables.add(Observable.zip(ServiceFactory.getRest(pageTitle.wikiSite).getSummary(null, pageTitle.prefixedText),
                    ServiceFactory.get(pageTitle.wikiSite).getWikiTextForSectionWithInfo(pageTitle.prefixedText, 0)) { summaryResponse, infoResponse ->
                Pair(summaryResponse, infoResponse)
            }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate { setUpEditView(savedInstanceState) }
                    .subscribe({ response ->
                        val editError = response.second.query?.firstPage()!!.getErrorForAction("edit")
                        if (editError.isEmpty()) {
                            editingAllowed = true
                        } else {
                            val error = editError[0]
                            FeedbackUtil.showError(requireActivity(), MwException(error), wikiSite = pageTitle.wikiSite)
                        }
                        sourceSummary?.extractHtml = response.first.extractHtml
                    }, { L.e(it) })
            )
        } else {
            setUpEditView(savedInstanceState)
        }
    }

    private fun setUpEditView(savedInstanceState: Bundle?) {
        if (action == DescriptionEditActivity.Action.ADD_DESCRIPTION) {
            analyticsHelper.articleDescriptionEditingStart(requireContext())
        }
        binding.fragmentDescriptionEditView.setAction(action)
        binding.fragmentDescriptionEditView.setPageTitle(pageTitle)
        highlightText?.let { binding.fragmentDescriptionEditView.setHighlightText(it) }
        binding.fragmentDescriptionEditView.callback = EditViewCallback()
        sourceSummary?.let { binding.fragmentDescriptionEditView.setSummaries(it, targetSummary) }
        if (savedInstanceState != null) {
            binding.fragmentDescriptionEditView.description = savedInstanceState.getString(ARG_DESCRIPTION)
            binding.fragmentDescriptionEditView.loadReviewContent(savedInstanceState.getBoolean(ARG_REVIEWING))
        }
        binding.fragmentDescriptionEditView.showProgressBar(false)
        binding.fragmentDescriptionEditView.setEditAllowed(editingAllowed)
        binding.fragmentDescriptionEditView.updateInfoText()

        binding.fragmentDescriptionEditView.isSuggestionButtonEnabled =
                ReleaseUtil.isPreBetaRelease && MachineGeneratedArticleDescriptionsAnalyticsHelper.isUserInExperiment &&
                MachineGeneratedArticleDescriptionsAnalyticsHelper.abcTest.group != GROUP_1

        if (binding.fragmentDescriptionEditView.isSuggestionButtonEnabled) {
            binding.fragmentDescriptionEditView.showSuggestedDescriptionsLoadingProgress()
            requestSuggestion()
        }
    }

    private fun requestSuggestion() {
        lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            binding.fragmentDescriptionEditView.isSuggestionButtonEnabled = false
            L.e(throwable)
            analyticsHelper.logApiFailed(requireContext(), throwable, pageTitle)
        }) {
            val response = ServiceFactory[pageTitle.wikiSite, DescriptionSuggestionService.API_URL, DescriptionSuggestionService::class.java]
                .getSuggestion(pageTitle.wikiSite.languageCode, pageTitle.prefixedText, 2)

            // Perform some post-processing on the predictions.
            // 1) Capitalize them, if we're dealing with enwiki.
            // 2) Remove duplicates.
            val list = (if (pageTitle.wikiSite.languageCode == "en") {
                response.prediction.map { StringUtil.capitalize(it)!! }
            } else response.prediction).distinct()
            analyticsHelper.apiOrderList = list
            analyticsHelper.logSuggestionsReceived(requireContext(), response.blp, pageTitle)
            L.d("Received suggestion: " + list.first())
            L.d("And is it a BLP? " + response.blp)

            // Randomize the display order
            if (!response.blp || MachineGeneratedArticleDescriptionsAnalyticsHelper.abcTest.group == GROUP_3) {
                val randomizedListIndex = (0 until 2).random()
                val firstSuggestion = if (list.size == 2) list[randomizedListIndex] else list.first()
                val secondSuggestion = if (list.size == 2) { if (randomizedListIndex == 0) list.last() else list.first() } else null
                analyticsHelper.displayOrderList = listOfNotNull(firstSuggestion, secondSuggestion)
                binding.fragmentDescriptionEditView.showSuggestedDescriptionsButton(firstSuggestion, secondSuggestion)
                analyticsHelper.logSuggestionsShown(requireContext(), pageTitle)
            } else {
                binding.fragmentDescriptionEditView.isSuggestionButtonEnabled = false
                binding.fragmentDescriptionEditView.updateSuggestedDescriptionsButtonVisibility()
            }
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    private fun shouldWriteToLocalWiki(): Boolean {
        return (action == DescriptionEditActivity.Action.ADD_DESCRIPTION ||
                action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION) &&
                DescriptionEditUtil.wikiUsesLocalDescriptions(pageTitle.wikiSite.languageCode)
    }

    private inner class EditViewCallback : DescriptionEditView.Callback {
        override fun onSaveClick() {
            if (!binding.fragmentDescriptionEditView.showingReviewContent()) {
                if (action == DescriptionEditActivity.Action.ADD_DESCRIPTION) {
                    analyticsHelper.articleDescriptionEditingEnd(requireContext())
                }
                binding.fragmentDescriptionEditView.loadReviewContent(true)
                analyticsHelper.timer.pause()
            } else {
                binding.fragmentDescriptionEditView.setError(null)
                binding.fragmentDescriptionEditView.setSaveState(true)
                cancelCalls()
                analyticsHelper.logAttempt(requireContext(),
                    binding.fragmentDescriptionEditView.description.orEmpty(), binding.fragmentDescriptionEditView.wasSuggestionChosen,
                    binding.fragmentDescriptionEditView.wasSuggestionModified, pageTitle
                )
                getEditTokenThenSave()
                EditAttemptStepEvent.logSaveAttempt(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
            }
        }

        private fun getEditTokenThenSave() {
            if (captchaHandler.isActive) {
                captchaHandler.hideCaptcha()
            }
            val csrfSite = if (action == DescriptionEditActivity.Action.ADD_CAPTION ||
                    action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                Constants.commonsWikiSite
            } else {
                if (shouldWriteToLocalWiki()) pageTitle.wikiSite else Constants.wikidataWikiSite
            }

            disposables.add(CsrfTokenClient.getToken(csrfSite).subscribe({ token ->
                if (shouldWriteToLocalWiki()) {
                    // If the description is being applied to an article on English Wikipedia, it
                    // should be written directly to the article instead of Wikidata.
                    postDescriptionToArticle(token)
                } else {
                    postDescriptionToWikidata(token)
                }
            }, {
                editFailed(it, false)
            }))
        }

        private fun postDescriptionToArticle(editToken: String) {
            val wikiSite = WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode)
            disposables.add(ServiceFactory.get(wikiSite).getWikiTextForSectionWithInfo(pageTitle.prefixedText, 0)
                    .subscribeOn(Schedulers.io())
                    .flatMap { mwQueryResponse ->
                        if (mwQueryResponse.query?.firstPage()!!.getErrorForAction("edit").isNotEmpty()) {
                            val error = mwQueryResponse.query?.firstPage()!!.getErrorForAction("edit")[0]
                            throw MwException(error)
                        }
                        var text = mwQueryResponse.query?.firstPage()!!.revisions[0].contentMain
                        val baseRevId = mwQueryResponse.query?.firstPage()!!.revisions[0].revId
                        text = updateDescriptionInArticle(text, binding.fragmentDescriptionEditView.description.orEmpty())

                        ServiceFactory.get(wikiSite).postEditSubmit(pageTitle.prefixedText, "0", null,
                            getEditComment().orEmpty(),
                            if (AccountUtil.isLoggedIn) "user"
                            else null, text, null, baseRevId, editToken,
                            if (captchaHandler.isActive) captchaHandler.captchaId() else null,
                            if (captchaHandler.isActive) captchaHandler.captchaWord() else null
                        )
                            .subscribeOn(Schedulers.io())
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ result ->
                        result.edit?.run {
                            when {
                                editSucceeded -> {
                                    AnonymousNotificationHelper.onEditSubmitted()
                                    waitForUpdatedRevision(newRevId)
                                    EditAttemptStepEvent.logSaveSuccess(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                                    analyticsHelper.logSuccess(requireContext(),
                                        binding.fragmentDescriptionEditView.description.orEmpty(),
                                        binding.fragmentDescriptionEditView.wasSuggestionChosen,
                                        binding.fragmentDescriptionEditView.wasSuggestionModified,
                                        pageTitle, newRevId
                                    )
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
                            analyticsHelper.logSuccess(requireContext(),
                                binding.fragmentDescriptionEditView.description.orEmpty(),
                                binding.fragmentDescriptionEditView.wasSuggestionChosen,
                                binding.fragmentDescriptionEditView.wasSuggestionModified,
                                pageTitle, response.entity?.lastRevId ?: 0
                            )
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

        @Suppress("SameParameterValue")
        private fun waitForUpdatedRevision(newRevision: Long) {
            disposables.add(ServiceFactory.getRest(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                .getSummaryResponse(pageTitle.prefixedText, null, OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(), null, null, null)
                .delay(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .map { response ->
                    if (response.body()!!.revision < newRevision) {
                        throw IllegalStateException()
                    }
                    response.body()!!.revision
                }
                .retry(10) { it is IllegalStateException }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    requireView().post(successRunnable)
                }
                .subscribe()
            )
        }

        private fun getPostObservable(editToken: String, languageCode: String): Observable<EntityPostResponse> {
            return if (action == DescriptionEditActivity.Action.ADD_CAPTION ||
                    action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                ServiceFactory.get(Constants.commonsWikiSite).postLabelEdit(languageCode, languageCode, Constants.COMMONS_DB_NAME,
                        pageTitle.prefixedText, binding.fragmentDescriptionEditView.description.orEmpty(),
                        getEditComment(), editToken, if (AccountUtil.isLoggedIn) "user" else null)
            } else {
                ServiceFactory.get(Constants.wikidataWikiSite).postDescriptionEdit(languageCode, languageCode, pageTitle.wikiSite.dbName(),
                        pageTitle.prefixedText, binding.fragmentDescriptionEditView.description.orEmpty(), getEditComment(), editToken,
                        if (AccountUtil.isLoggedIn) "user" else null)
            }
        }

        private fun getEditComment(): String? {
            if (action == DescriptionEditActivity.Action.ADD_DESCRIPTION && binding.fragmentDescriptionEditView.wasSuggestionChosen) {
                return if (binding.fragmentDescriptionEditView.wasSuggestionModified) MACHINE_SUGGESTION_MODIFIED else MACHINE_SUGGESTION
            } else if (invokeSource == InvokeSource.SUGGESTED_EDITS || invokeSource == InvokeSource.FEED) {
                return when (action) {
                    DescriptionEditActivity.Action.ADD_DESCRIPTION -> SUGGESTED_EDITS_ADD_COMMENT
                    DescriptionEditActivity.Action.ADD_CAPTION -> SUGGESTED_EDITS_ADD_COMMENT
                    DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> SUGGESTED_EDITS_TRANSLATE_COMMENT
                    DescriptionEditActivity.Action.TRANSLATE_CAPTION -> SUGGESTED_EDITS_TRANSLATE_COMMENT
                    else -> null
                }
            }
            return null
        }

        private fun editFailed(caught: Throwable, logError: Boolean) {
            binding.fragmentDescriptionEditView.setSaveState(false)
            FeedbackUtil.showError(requireActivity(), caught)
            L.e(caught)
            if (logError) {
                EditAttemptStepEvent.logSaveFailure(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
            }
        }

        override fun onCancelClick() {
            if (captchaHandler.isActive) {
                captchaHandler.cancelCaptcha()
            } else if (binding.fragmentDescriptionEditView.showingReviewContent()) {
                binding.fragmentDescriptionEditView.loadReviewContent(false)
                analyticsHelper.timer.resume()
            } else {
                DeviceUtil.hideSoftKeyboard(requireActivity())
                requireActivity().onBackPressed()
            }
        }

        override fun onBottomBarClick() {
            callback()?.onBottomBarContainerClicked(action)
        }

        override fun onVoiceInputClick() {
            try {
                voiceSearchLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM))
            } catch (a: ActivityNotFoundException) {
                FeedbackUtil.showMessage(requireActivity(), R.string.error_voice_search_not_available)
            }
        }

        override fun getAnalyticsHelper(): MachineGeneratedArticleDescriptionsAnalyticsHelper {
            return analyticsHelper
        }
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
        private const val ARG_TITLE = "title"
        private const val ARG_REVIEWING = "inReviewing"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_HIGHLIGHT_TEXT = "highlightText"
        private const val ARG_ACTION = "action"
        private const val ARG_SOURCE_SUMMARY = "sourceSummary"
        private const val ARG_TARGET_SUMMARY = "targetSummary"
        private const val SUGGESTED_EDITS_UI_VERSION = "1.0"
        const val MACHINE_SUGGESTION = "#machine-suggestion"
        const val MACHINE_SUGGESTION_MODIFIED = "#machine-suggestion-modified"
        const val SUGGESTED_EDITS_ADD_COMMENT = "#suggestededit-add $SUGGESTED_EDITS_UI_VERSION"
        const val SUGGESTED_EDITS_TRANSLATE_COMMENT = "#suggestededit-translate $SUGGESTED_EDITS_UI_VERSION"
        const val SUGGESTED_EDITS_IMAGE_TAG_AUTO_COMMENT = "#suggestededit-imgtag-auto $SUGGESTED_EDITS_UI_VERSION"
        const val SUGGESTED_EDITS_IMAGE_TAG_CUSTOM_COMMENT = "#suggestededit-imgtag-custom $SUGGESTED_EDITS_UI_VERSION"
        private val DESCRIPTION_TEMPLATES = arrayOf("Short description", "SHORTDESC")
        // Don't remove the ending escaped `\\}`
        @Suppress("RegExpRedundantEscape")
        const val TEMPLATE_PARSE_REGEX = "(\\{\\{[Ss]hort description\\|(?:1=)?)([^}|]+)([^}]*\\}\\})"

        fun newInstance(title: PageTitle,
                        highlightText: String?,
                        sourceSummary: PageSummaryForEdit?,
                        targetSummary: PageSummaryForEdit?,
                        action: DescriptionEditActivity.Action,
                        source: InvokeSource): DescriptionEditFragment {
            return DescriptionEditFragment().apply {
                arguments = bundleOf(ARG_TITLE to title,
                        ARG_HIGHLIGHT_TEXT to highlightText,
                        ARG_SOURCE_SUMMARY to sourceSummary,
                        ARG_TARGET_SUMMARY to targetSummary,
                        ARG_ACTION to action,
                        Constants.INTENT_EXTRA_INVOKE_SOURCE to source)
            }
        }
    }
}
