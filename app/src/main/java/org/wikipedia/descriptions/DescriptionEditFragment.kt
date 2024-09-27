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
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.auth.AccountUtil
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.databinding.FragmentDescriptionEditBinding
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.edit.Edit
import org.wikipedia.edit.EditTags
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsSurvey
import org.wikipedia.suggestededits.SuggestionsActivity
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.Resource
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

    private lateinit var captchaHandler: CaptchaHandler

    private val analyticsHelper = MachineGeneratedArticleDescriptionsAnalyticsHelper()

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
        if (viewModel.invokeSource == InvokeSource.SUGGESTED_EDITS) {
            SuggestedEditsSurvey.onEditSuccess()
        }
        Prefs.lastDescriptionEditTime = Date().time
        Prefs.isSuggestedEditsReactivationPassStageOne = false
        binding.fragmentDescriptionEditView.setSaveState(false)
        if (Prefs.showDescriptionEditSuccessPrompt && viewModel.invokeSource != InvokeSource.SUGGESTED_EDITS) {
            editSuccessLauncher.launch(DescriptionEditSuccessActivity.newIntent(requireContext(), viewModel.invokeSource))
            Prefs.showDescriptionEditSuccessPrompt = false
        } else {
            val intent = Intent()
            intent.putExtra(SuggestionsActivity.EXTRA_SOURCE_ADDED_CONTRIBUTION, binding.fragmentDescriptionEditView.description)
            intent.putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, viewModel.invokeSource)
            intent.putExtra(Constants.INTENT_EXTRA_ACTION, viewModel.action)
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
                                it.data?.let { error ->
                                    FeedbackUtil.showError(requireActivity(), MwException(error), wikiSite = viewModel.pageTitle.wikiSite)
                                }
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
                launch {
                    viewModel.postDescriptionState.collect {
                        when (it) {
                            is Resource.Loading -> {
                                binding.fragmentDescriptionEditView.showProgressBar(true)
                                binding.fragmentDescriptionEditView.setError(null)
                                binding.fragmentDescriptionEditView.setSaveState(true)
                            }
                            is Resource.Success -> {
                                if (viewModel.shouldWriteToLocalWiki()) {
                                    (it.data as Edit).edit?.run {
                                        when {
                                            editSucceeded -> {
                                                AnonymousNotificationHelper.onEditSubmitted()
                                                viewModel.waitForRevisionUpdate(newRevId)
                                                EditAttemptStepEvent.logSaveSuccess(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                                                analyticsHelper.logSuccess(requireContext(), viewModel.pageTitle, newRevId)
                                                ImageRecommendationsEvent.logEditSuccess(viewModel.action, viewModel.pageTitle.wikiSite.languageCode, newRevId)
                                            }
                                            hasEditErrorCode -> {
                                                editFailed(MwException(MwServiceError(code, spamblacklist)), false)
                                            }
                                            hasCaptchaResponse -> {
                                                binding.fragmentDescriptionEditView.showProgressBar(false)
                                                binding.fragmentDescriptionEditView.setSaveState(false)
                                                captchaHandler.handleCaptcha(null, CaptchaResult(captchaId))
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
                                } else {
                                    (it.data as EntityPostResponse).run {
                                        AnonymousNotificationHelper.onEditSubmitted()
                                        if (success > 0) {
                                            val revId = entity?.lastRevId ?: 0
                                            requireView().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(4))
                                            analyticsHelper.logSuccess(requireContext(), viewModel.pageTitle, revId)
                                            ImageRecommendationsEvent.logEditSuccess(viewModel.action, viewModel.pageTitle.wikiSite.languageCode, revId)
                                            EditAttemptStepEvent.logSaveSuccess(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                                        } else {
                                            editFailed(RuntimeException("Received unrecognized description edit response"), true)
                                        }
                                    }
                                }
                            }
                            is Resource.Error -> {
                                if (viewModel.shouldWriteToLocalWiki()) {
                                    editFailed(it.throwable, true)
                                } else {
                                    if (it.throwable is MwException) {
                                        val error = it.throwable.error
                                        if (error.badLoginState() || error.badToken()) {
                                            viewModel.postDescription(
                                                currentDescription = binding.fragmentDescriptionEditView.description.orEmpty(),
                                                editComment = getEditComment(),
                                                editTags = getEditTags(),
                                                captchaId = if (captchaHandler.isActive) captchaHandler.captchaId() else null,
                                                captchaWord = if (captchaHandler.isActive) captchaHandler.captchaWord() else null
                                            )
                                        } else {
                                            editFailed(it.throwable, true)
                                        }
                                    } else {
                                        editFailed(it.throwable, true)
                                    }
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.waitForRevisionState.collect {
                        when (it) {
                            is Resource.Loading -> {
                                binding.fragmentDescriptionEditView.showProgressBar(true)
                            }
                            is Resource.Success -> {
                                requireView().post(successRunnable)
                            }
                            is Resource.Error -> {
                                editFailed(it.throwable, true)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_DESCRIPTION, binding.fragmentDescriptionEditView.description)
        outState.putBoolean(ARG_REVIEWING, binding.fragmentDescriptionEditView.showingReviewContent())
    }

    private fun loadPageSummaryIfNeeded(savedInstanceState: Bundle?) {
        binding.fragmentDescriptionEditView.showProgressBar(true)
        if ((viewModel.invokeSource == InvokeSource.PAGE_ACTIVITY || viewModel.invokeSource == InvokeSource.PAGE_EDIT_PENCIL ||
                    viewModel.invokeSource == InvokeSource.PAGE_EDIT_HIGHLIGHT) && viewModel.sourceSummary?.extractHtml.isNullOrEmpty()) {
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
        binding.fragmentDescriptionEditView.setEditAllowed(viewModel.editingAllowed)
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
                analyticsHelper.logAttempt(requireContext(), viewModel.pageTitle)
                EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                viewModel.postDescription(
                    currentDescription = binding.fragmentDescriptionEditView.description.orEmpty(),
                    editComment = getEditComment(),
                    editTags = getEditTags(),
                    captchaId = if (captchaHandler.isActive) captchaHandler.captchaId() else null,
                    captchaWord = if (captchaHandler.isActive) captchaHandler.captchaWord() else null
                )
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
            callback()?.onBottomBarContainerClicked(viewModel.action)
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

    private fun getEditComment(): String? {
        if (viewModel.action == DescriptionEditActivity.Action.ADD_DESCRIPTION && binding.fragmentDescriptionEditView.wasSuggestionChosen) {
            return if (binding.fragmentDescriptionEditView.wasSuggestionModified) MACHINE_SUGGESTION_MODIFIED else MACHINE_SUGGESTION
        }
        return null
    }

    private fun getEditTags(): String? {
        val tags = mutableListOf<String>()

        if (viewModel.invokeSource == InvokeSource.SUGGESTED_EDITS) {
            tags.add(EditTags.APP_SUGGESTED_EDIT)
        }

        when (viewModel.action) {
            DescriptionEditActivity.Action.ADD_DESCRIPTION -> {
                if (binding.fragmentDescriptionEditView.wasSuggestionChosen) {
                    tags.add(EditTags.APP_DESCRIPTION_ADD)
                    tags.add(EditTags.APP_AI_ASSIST)
                } else if (viewModel.pageTitle.description.isNullOrEmpty()) {
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

    private fun editFailed(caught: Throwable, logError: Boolean) {
        binding.fragmentDescriptionEditView.setSaveState(false)
        FeedbackUtil.showError(requireActivity(), caught)
        L.e(caught)
        if (logError) {
            EditAttemptStepEvent.logSaveFailure(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
        }
    }

    companion object {
        const val ARG_REVIEWING = "inReviewing"
        const val ARG_DESCRIPTION = "description"
        const val ARG_HIGHLIGHT_TEXT = "highlightText"
        const val ARG_ACTION = "action"
        const val ARG_SOURCE_SUMMARY = "sourceSummary"
        const val ARG_TARGET_SUMMARY = "targetSummary"
        private const val MACHINE_SUGGESTION = "#machine-suggestion"
        private const val MACHINE_SUGGESTION_MODIFIED = "#machine-suggestion-modified"

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
