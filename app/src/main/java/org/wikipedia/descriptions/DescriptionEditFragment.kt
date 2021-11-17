package org.wikipedia.descriptions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.DescriptionEditFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.databinding.FragmentDescriptionEditBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsSurvey
import org.wikipedia.suggestededits.SuggestionsActivity
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.log.L
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class DescriptionEditFragment : Fragment() {
    interface Callback {
        fun onDescriptionEditSuccess()
        fun onBottomBarContainerClicked(action: DescriptionEditActivity.Action)
    }

    private var _binding: FragmentDescriptionEditBinding? = null
    val binding get() = _binding!!
    private lateinit var funnel: DescriptionEditFunnel
    private lateinit var invokeSource: InvokeSource
    private lateinit var pageTitle: PageTitle
    lateinit var action: DescriptionEditActivity.Action
    private var sourceSummary: PageSummaryForEdit? = null
    private var targetSummary: PageSummaryForEdit? = null
    private var highlightText: String? = null

    private val disposables = CompositeDisposable()

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
        SuggestedEditsFunnel.get().success(action)
        binding.fragmentDescriptionEditView.setSaveState(false)
        if (Prefs.showDescriptionEditSuccessPrompt && invokeSource == InvokeSource.PAGE_ACTIVITY) {
            startActivityForResult(DescriptionEditSuccessActivity.newIntent(requireContext(), invokeSource),
                    Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS)
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
        val type = if (pageTitle.description == null) DescriptionEditFunnel.Type.NEW else DescriptionEditFunnel.Type.EXISTING
        funnel = DescriptionEditFunnel(WikipediaApp.instance, pageTitle, type, invokeSource)
        funnel.logStart()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentDescriptionEditBinding.inflate(inflater, container, false)
        loadPageSummaryIfNeeded(savedInstanceState)
        funnel.logReady()
        return binding.root
    }

    override fun onDestroyView() {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS) {
            callback()?.onDescriptionEditSuccess()
        } else if (requestCode == Constants.ACTIVITY_REQUEST_VOICE_SEARCH &&
                resultCode == Activity.RESULT_OK && data != null &&
                data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) != null) {
            val text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!![0]
            binding.fragmentDescriptionEditView.description = text
        }
    }

    private fun cancelCalls() {
        disposables.clear()
    }

    private fun loadPageSummaryIfNeeded(savedInstanceState: Bundle?) {
        binding.fragmentDescriptionEditView.showProgressBar(true)
        if (invokeSource == InvokeSource.PAGE_ACTIVITY && sourceSummary?.extractHtml.isNullOrEmpty()) {
            disposables.add(ServiceFactory.getRest(pageTitle.wikiSite).getSummary(null, pageTitle.prefixedText)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate { setUpEditView(savedInstanceState) }
                    .subscribe({ summary -> sourceSummary?.extractHtml = summary.extractHtml },
                            { L.e(it) }))
        } else {
            setUpEditView(savedInstanceState)
        }
    }

    private fun setUpEditView(savedInstanceState: Bundle?) {
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
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    private fun shouldWriteToLocalWiki(): Boolean {
        return (action == DescriptionEditActivity.Action.ADD_DESCRIPTION ||
                action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION) &&
                wikiUsesLocalDescriptions(pageTitle.wikiSite.languageCode)
    }

    private inner class EditViewCallback : DescriptionEditView.Callback {
        private val wikiData = WikiSite(Service.WIKIDATA_URL, "")
        private val wikiCommons = WikiSite(Service.COMMONS_URL)
        private val commonsDbName = "commonswiki"
        override fun onSaveClick() {
            if (!binding.fragmentDescriptionEditView.showingReviewContent()) {
                binding.fragmentDescriptionEditView.loadReviewContent(true)
            } else {
                binding.fragmentDescriptionEditView.setError(null)
                binding.fragmentDescriptionEditView.setSaveState(true)
                cancelCalls()
                getEditTokenThenSave()
                funnel.logSaveAttempt()
            }
        }

        private fun getEditTokenThenSave() {
            val csrfClient = if (action == DescriptionEditActivity.Action.ADD_CAPTION ||
                    action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                CsrfTokenClient(wikiCommons)
            } else {
                CsrfTokenClient(if (shouldWriteToLocalWiki()) pageTitle.wikiSite else wikiData, pageTitle.wikiSite)
            }

            disposables.add(csrfClient.token.subscribe({ token ->
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
                        var text = mwQueryResponse.query?.firstPage()!!.revisions[0].content
                        val baseRevId = mwQueryResponse.query?.firstPage()!!.revisions[0].revId
                        text = updateDescriptionInArticle(text, binding.fragmentDescriptionEditView.description.orEmpty())

                        ServiceFactory.get(wikiSite).postEditSubmit(pageTitle.prefixedText, "0", null,
                                getEditComment().orEmpty(),
                                if (AccountUtil.isLoggedIn) "user"
                                else null, text, null, baseRevId, editToken, null, null)
                                .subscribeOn(Schedulers.io())
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ result ->
                        result.edit?.run {
                            when {
                                editSucceeded -> {
                                    waitForUpdatedRevision(newRevId)
                                    funnel.logSaved(newRevId)
                                }
                                hasCaptchaResponse -> {
                                    // TODO: handle captcha.
                                    // new CaptchaResult(result.edit().captchaId());
                                    funnel.logCaptchaShown()
                                }
                                hasEditErrorCode -> {
                                    editFailed(MwException(MwServiceError(code, spamblacklist)), false)
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
                        if (response.success > 0) {
                            requireView().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(4))
                            funnel.logSaved(response.entity?.run { lastRevId } ?: 0)
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
                ServiceFactory.get(wikiCommons).postLabelEdit(languageCode, languageCode, commonsDbName,
                        pageTitle.prefixedText, binding.fragmentDescriptionEditView.description.orEmpty(),
                        getEditComment(), editToken, if (AccountUtil.isLoggedIn) "user" else null)
            } else {
                ServiceFactory.get(wikiData).postDescriptionEdit(languageCode, languageCode, pageTitle.wikiSite.dbName(),
                        pageTitle.prefixedText, binding.fragmentDescriptionEditView.description.orEmpty(), getEditComment(), editToken,
                        if (AccountUtil.isLoggedIn) "user" else null)
            }
        }

        private fun getEditComment(): String? {
            if (invokeSource == InvokeSource.SUGGESTED_EDITS || invokeSource == InvokeSource.FEED) {
                return when (action) {
                    DescriptionEditActivity.Action.ADD_DESCRIPTION -> SuggestedEditsFunnel.SUGGESTED_EDITS_ADD_COMMENT
                    DescriptionEditActivity.Action.ADD_CAPTION -> SuggestedEditsFunnel.SUGGESTED_EDITS_ADD_COMMENT
                    DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> SuggestedEditsFunnel.SUGGESTED_EDITS_TRANSLATE_COMMENT
                    DescriptionEditActivity.Action.TRANSLATE_CAPTION -> SuggestedEditsFunnel.SUGGESTED_EDITS_TRANSLATE_COMMENT
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
                funnel.logError(caught.message)
            }
            SuggestedEditsFunnel.get().failure(action)
        }

        override fun onHelpClick() {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
        }

        override fun onCancelClick() {
            if (binding.fragmentDescriptionEditView.showingReviewContent()) {
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
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            try {
                startActivityForResult(intent, Constants.ACTIVITY_REQUEST_VOICE_SEARCH)
            } catch (a: ActivityNotFoundException) {
                FeedbackUtil.showMessage(requireActivity(), R.string.error_voice_search_not_available)
            }
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

        fun wikiUsesLocalDescriptions(lang: String): Boolean {
            return lang == "en"
        }
    }
}
