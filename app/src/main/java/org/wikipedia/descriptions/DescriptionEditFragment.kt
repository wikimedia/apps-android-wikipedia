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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.auth.AccountUtil
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.databinding.FragmentDescriptionEditBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.liftwing.DescriptionSuggestion
import org.wikipedia.dataclient.liftwing.LiftWingModelService
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.edit.EditTags
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsSurvey
import org.wikipedia.suggestededits.SuggestionsActivity
import org.wikipedia.usercontrib.UserInformationDialogViewModel
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.SuggestedArticleDescriptionsDialog
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

class DescriptionEditFragment : Fragment() {
    interface Callback {
        fun onDescriptionEditSuccess()
        fun onBottomBarContainerClicked(action: DescriptionEditActivity.Action)
    }

    private val viewModel: DescriptionEditViewModel by viewModels { DescriptionEditViewModel.Factory(requireArguments()) }
    private var _binding: FragmentDescriptionEditBinding? = null
    val binding get() = _binding!!

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
        EditAttemptStepEvent.logInit(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentDescriptionEditBinding.inflate(inflater, container, false)
        loadPageSummaryIfNeeded(savedInstanceState)

        binding.fragmentDescriptionEditView.setLoginCallback {
            val loginIntent = LoginActivity.newIntent(requireActivity(), LoginActivity.SOURCE_EDIT)
            loginLauncher.launch(loginIntent)
        }
        captchaHandler = CaptchaHandler(requireActivity() as AppCompatActivity, viewModel.pageTitle.wikiSite, binding.fragmentDescriptionEditView.getCaptchaContainer().root,
            binding.fragmentDescriptionEditView.getDescriptionEditTextView(), "", null)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.loadPageSummaryState.collect {
                        when (it) {
                            is Resource.Loading -> {
                                binding.fragmentDescriptionEditView.setEditAllowed(false)
                                binding.fragmentDescriptionEditView.showProgressBar(true)
                            }

                            is Resource.Success -> {
                                setUpEditView(savedInstanceState)
                            }

                            is Resource.Error -> {
                                FeedbackUtil.showError(requireActivity(), it.throwable, wikiSite = viewModel.pageTitle.wikiSite)
                            }
                        }
                    }
                }
                launch {
                    viewModel.requestSuggestionState.collect {
                        when (it) {
                            is Resource.Loading -> {
                                binding.fragmentDescriptionEditView.showSuggestedDescriptionsLoadingProgress()
                            }
                            is Resource.Success -> {
                                analyticsHelper.logSuggestionsReceived(requireContext(), it.data.first.blp, viewModel.pageTitle)
                                if (it.data.third.isNotEmpty() && !it.data.first.blp || it.data.second > 50) {
                                    binding.fragmentDescriptionEditView.showSuggestedDescriptionsButton(it.data.third.first(),
                                        if (it.data.third.size > 1) it.data.third.last() else null)
                                } else {
                                    binding.fragmentDescriptionEditView.isSuggestionButtonEnabled = false
                                    binding.fragmentDescriptionEditView.updateSuggestedDescriptionsButtonVisibility()
                                }
                            }
                            is Resource.Error -> {
                                binding.fragmentDescriptionEditView.isSuggestionButtonEnabled = false
                                FeedbackUtil.showError(requireActivity(), it.throwable, wikiSite = viewModel.pageTitle.wikiSite)
                            }
                        }
                    }
                }
            }
        }
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
        if ((viewModel.invokeSource == InvokeSource.PAGE_ACTIVITY || viewModel.invokeSource == InvokeSource.PAGE_EDIT_PENCIL ||
                    viewModel.invokeSource == InvokeSource.PAGE_EDIT_HIGHLIGHT) && viewModel.sourceSummary?.extractHtml.isNullOrEmpty()) {
            editingAllowed = false
            viewModel.loadPageSummary()
        } else {
            setUpEditView(savedInstanceState)
        }
    }

    private fun setUpEditView(savedInstanceState: Bundle?) {
        if (viewModel.action == DescriptionEditActivity.Action.ADD_DESCRIPTION) {
            analyticsHelper.articleDescriptionEditingStart(requireContext())
        }
        binding.fragmentDescriptionEditView.setAction(viewModel.action)
        binding.fragmentDescriptionEditView.setPageTitle(viewModel.pageTitle)
        viewModel.highlightText?.let { binding.fragmentDescriptionEditView.setHighlightText(it) }
        binding.fragmentDescriptionEditView.callback = EditViewCallback()
        viewModel.sourceSummary?.let { binding.fragmentDescriptionEditView.setSummaries(it, viewModel.targetSummary) }
        if (savedInstanceState != null) {
            binding.fragmentDescriptionEditView.description = savedInstanceState.getString(ARG_DESCRIPTION)
            binding.fragmentDescriptionEditView.loadReviewContent(savedInstanceState.getBoolean(ARG_REVIEWING))
        }
        binding.fragmentDescriptionEditView.showProgressBar(false)
        binding.fragmentDescriptionEditView.setEditAllowed(editingAllowed)
        binding.fragmentDescriptionEditView.updateInfoText()

        binding.fragmentDescriptionEditView.isSuggestionButtonEnabled = ReleaseUtil.isPreBetaRelease &&
                SuggestedArticleDescriptionsDialog.availableLanguages.contains(viewModel.pageTitle.wikiSite.languageCode) &&
                binding.fragmentDescriptionEditView.description.isNullOrEmpty()

        if (binding.fragmentDescriptionEditView.isSuggestionButtonEnabled) {
            viewModel.requestSuggestion()
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    private inner class EditViewCallback : DescriptionEditView.Callback {
        override fun onSaveClick() {
            if (!binding.fragmentDescriptionEditView.showingReviewContent()) {
                if (viewModel.action == DescriptionEditActivity.Action.ADD_DESCRIPTION) {
                    analyticsHelper.articleDescriptionEditingEnd(requireContext())
                }
                binding.fragmentDescriptionEditView.loadReviewContent(true)
            } else {
                binding.fragmentDescriptionEditView.setError(null)
                binding.fragmentDescriptionEditView.setSaveState(true)
                cancelCalls()
                analyticsHelper.logAttempt(requireContext(), viewModel.pageTitle)
                viewModel.postDescription()
                EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
            }
        }

        private fun getEditTokenThenSave() {
            if (captchaHandler.isActive) {
                captchaHandler.hideCaptcha()
            }
            val csrfSite = if (viewModel.action == DescriptionEditActivity.Action.ADD_CAPTION ||
                viewModel.action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
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

                        val automaticallyAddedEditSummary = getString(if (pageTitle.description.isNullOrEmpty()) R.string.edit_summary_added_short_description
                        else R.string.edit_summary_updated_short_description)
                        var editSummary = automaticallyAddedEditSummary
                        getEditComment()?.let {
                            editSummary += ", $it"
                        }
                        ServiceFactory.get(wikiSite).postEditSubmit(pageTitle.prefixedText, "0", null,
                            editSummary,
                            AccountUtil.assertUser, text, null, baseRevId, editToken,
                            if (captchaHandler.isActive) captchaHandler.captchaId() else null,
                            if (captchaHandler.isActive) captchaHandler.captchaWord() else null, tags = getEditTags()
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
                    getEditComment(), editToken, AccountUtil.assertUser, tags = getEditTags())
            } else {
                ServiceFactory.get(Constants.wikidataWikiSite).postDescriptionEdit(languageCode, languageCode, pageTitle.wikiSite.dbName(),
                    pageTitle.prefixedText, binding.fragmentDescriptionEditView.description.orEmpty(), getEditComment(), editToken,
                    AccountUtil.assertUser, tags = getEditTags())
            }
        }

        private fun getEditComment(): String? {
            if (viewModel.action == DescriptionEditActivity.Action.ADD_DESCRIPTION && binding.fragmentDescriptionEditView.wasSuggestionChosen) {
                return if (binding.fragmentDescriptionEditView.wasSuggestionModified) MACHINE_SUGGESTION_MODIFIED else MACHINE_SUGGESTION
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
        const val ARG_REVIEWING = "inReviewing"
        const val ARG_DESCRIPTION = "description"
        const val ARG_HIGHLIGHT_TEXT = "highlightText"
        const val ARG_ACTION = "action"
        const val ARG_SOURCE_SUMMARY = "sourceSummary"
        const val ARG_TARGET_SUMMARY = "targetSummary"
        const val MACHINE_SUGGESTION = "#machine-suggestion"
        const val MACHINE_SUGGESTION_MODIFIED = "#machine-suggestion-modified"

        val DESCRIPTION_TEMPLATES = arrayOf("Short description", "SHORTDESC")
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
                arguments = bundleOf(Constants.ARG_TITLE to title,
                        ARG_HIGHLIGHT_TEXT to highlightText,
                        ARG_SOURCE_SUMMARY to sourceSummary,
                        ARG_TARGET_SUMMARY to targetSummary,
                        ARG_ACTION to action,
                        Constants.INTENT_EXTRA_INVOKE_SOURCE to source)
            }
        }
    }
}
