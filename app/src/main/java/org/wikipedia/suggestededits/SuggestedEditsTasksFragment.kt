package org.wikipedia.suggestededits

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.analytics.eventplatform.UserContributionEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.databinding.FragmentSuggestedEditsTasksBinding
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS
import org.wikipedia.descriptions.DescriptionEditActivity.Action.IMAGE_RECOMMENDATIONS
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditUtil
import org.wikipedia.donate.DonorHistoryActivity
import org.wikipedia.donate.DonorStatus
import org.wikipedia.events.LoggedOutEvent
import org.wikipedia.login.LoginActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.usercontrib.ContributionsDashboardHelper
import org.wikipedia.usercontrib.UserContribListActivity
import org.wikipedia.usercontrib.UserContribStats
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

class SuggestedEditsTasksFragment : Fragment() {
    private var _binding: FragmentSuggestedEditsTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SuggestedEditsTasksFragmentViewModel by viewModels()

    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask
    private lateinit var addImageTagsTask: SuggestedEditsTask
    private lateinit var imageRecommendationsTask: SuggestedEditsTask
    private lateinit var vandalismPatrolTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val sequentialTooltipRunnable = Runnable {
        if (!isAdded) {
            return@Runnable
        }
        val balloon = FeedbackUtil.getTooltip(requireContext(), binding.editsCountStatsView.tooltipText, autoDismiss = true, showDismissButton = true)
        balloon.showAlignBottom(binding.editsCountStatsView.getTitleView())
        balloon.relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), binding.editStreakStatsView.tooltipText, autoDismiss = true, showDismissButton = true), binding.editStreakStatsView.getTitleView())
            .relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), binding.pageViewStatsView.tooltipText, autoDismiss = true, showDismissButton = true), binding.pageViewStatsView.getTitleView())
            .relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), binding.editQualityStatsView.tooltipText, autoDismiss = true, showDismissButton = true), binding.editQualityStatsView.getTitleView())
        Prefs.showOneTimeSequentialUserStatsTooltip = false
        BreadCrumbLogEvent.logTooltipShown(requireActivity(), binding.editsCountStatsView)
    }

    private val requestAddLanguage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        binding.tasksRecyclerView.adapter?.notifyDataSetChanged()
    }

    private val requestAddImageTags = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            Prefs.showImageTagsOnboarding = false
            startActivity(SuggestionsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS))
        }
    }

    private val requestLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            clearContents()
        }
    }

    private val requestUpdateDonorHistory = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        maybeShowDonorHistoryUpdatedSnackbar()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTestingButtons()
        maybeShowDonorHistoryUpdatedSnackbar()
        binding.contributionsContainer.setOnClickListener {
            startActivity(UserContribListActivity.newIntent(requireActivity(), AccountUtil.userName))
        }

        binding.donorHistoryContainer.setOnClickListener {
            requestUpdateDonorHistory.launch(DonorHistoryActivity.newIntent(requireContext()))
        }

        binding.learnMoreCard.setOnClickListener {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
        }
        binding.learnMoreButton.setOnClickListener {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
        }

        binding.swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.progressive_color))
        binding.swipeRefreshLayout.setOnRefreshListener { refreshContents() }

        binding.errorView.retryClickListener = View.OnClickListener { refreshContents() }
        binding.errorView.loginClickListener = View.OnClickListener { requestLogin.launch(LoginActivity.newIntent(requireContext(), LoginActivity.SOURCE_SUGGESTED_EDITS)) }

        binding.suggestedEditsScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            (requireActivity() as MainActivity).updateToolbarElevation(scrollY > 0)
        })
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)
        binding.tasksContainer.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> onLoading()
                            is Resource.Success -> setFinalUIState()
                            is SuggestedEditsTasksFragmentViewModel.RequireLogin -> onRequireLogin()
                            is Resource.Error -> showError(it.throwable)
                        }
                    }
                }

                launch {
                    FlowEventBus.events.collectLatest { event ->
                        if (event is LoggedOutEvent &&
                            (requireActivity() as MainActivity).isCurrentFragmentSelected(this@SuggestedEditsTasksFragment)) {
                            refreshContents()
                        }
                    }
                }
            }
        }
    }

    fun refreshContents() {
        (requireActivity() as MainActivity).onTabChanged(NavTab.EDITS)
        requireActivity().invalidateOptionsMenu()
        viewModel.fetchData()
    }

    override fun onResume() {
        super.onResume()
        refreshContents()
    }

    override fun onDestroyView() {
        binding.tasksRecyclerView.adapter = null
        binding.suggestedEditsScrollView.removeCallbacks(sequentialTooltipRunnable)
        _binding = null
        super.onDestroyView()
    }

    private fun onLoading() {
        binding.progressBar.isVisible = true
        binding.suggestedEditsScrollView.isVisible = false
    }

    private fun onRequireLogin() {
        clearContents()
        binding.messageCard.setRequiredLogin {
            requestLogin.launch(LoginActivity.newIntent(requireContext(), LoginActivity.SOURCE_SUGGESTED_EDITS))
        }
        binding.messageCard.isVisible = true
        binding.contributionsContainer.isVisible = false
        binding.statsDivider.isVisible = false
    }

    private fun clearContents(shouldScrollToTop: Boolean = true) {
        binding.suggestedEditsScrollView.isVisible = true
        binding.swipeRefreshLayout.isRefreshing = false
        binding.progressBar.isVisible = false
        binding.tasksContainer.isVisible = false
        binding.errorView.isVisible = false
        binding.messageCard.isVisible = false
        binding.contributionsContainer.isVisible = false
        binding.statsDivider.isVisible = false
        if (shouldScrollToTop) {
            binding.suggestedEditsScrollView.scrollTo(0, 0)
        }
        binding.swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))

        setUpDonorHistoryStatus()
    }

    private fun showError(t: Throwable) {
        clearContents()
        binding.errorView.setError(t)
        binding.errorView.isVisible = true
    }

    private fun setFinalUIState() {
        clearContents(false)

        if (maybeSetPausedOrDisabled()) {
            return
        }

        setUpTasks()

        if (displayedTasks.isEmpty() && !viewModel.blockMessageWikipedia.isNullOrEmpty()) {
            clearContents()
            setIPBlockedStatus()
            return
        }

        binding.tasksRecyclerView.adapter!!.notifyDataSetChanged()
        setUserStatsViewsAndTooltips()

        binding.pageViewStatsView.setDescription(viewModel.totalPageviews.toString())

        if (viewModel.latestEditStreak < 2) {
            binding.editStreakStatsView.setTitle(resources.getString(R.string.suggested_edits_last_edited))
            binding.editStreakStatsView.setDescription(if (viewModel.latestEditDate.time > 0) DateUtil.getMDYDateString(viewModel.latestEditDate) else resources.getString(R.string.suggested_edits_last_edited_never))
        } else {
            binding.editStreakStatsView.setTitle(resources.getString(R.string.suggested_edits_edit_streak_label_text))
            binding.editStreakStatsView.setDescription(resources.getQuantityString(R.plurals.suggested_edits_edit_streak_detail_text,
                viewModel.latestEditStreak, viewModel.latestEditStreak))
        }

        if (viewModel.totalContributions == 0) {
            binding.contributionsContainer.isVisible = false
            binding.statsDivider.isVisible = false
            binding.messageCard.isVisible = true
            binding.messageCard.setOnboarding(getString(R.string.suggested_edits_onboarding_message, AccountUtil.userName))
        } else {
            binding.contributionsContainer.isVisible = true
            binding.statsDivider.isVisible = true
            val contributionsStatsViewPluralRes = if (ContributionsDashboardHelper.contributionsDashboardEnabled)
                R.plurals.suggested_edits_edit_frequency else R.plurals.suggested_edits_contribution
            binding.editsCountStatsView.setTitle(resources.getQuantityString(contributionsStatsViewPluralRes, viewModel.totalContributions))
            binding.editsCountStatsView.setDescription(viewModel.totalContributions.toString())
            // TODO: add the sequential tooltips back after the experiment code is removed.
//            if (Prefs.showOneTimeSequentialUserStatsTooltip) {
//                 showOneTimeSequentialUserStatsTooltips()
//            }
        }

        binding.donorHistoryContainer.isVisible = true
        if (!ContributionsDashboardHelper.contributionsDashboardEnabled) {
            binding.donorHistoryContainer.isVisible = false
            binding.statsDivider.isVisible = false
        }

        binding.swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        binding.tasksContainer.isVisible = true
    }

    private fun setUserStatsViewsAndTooltips() {
        binding.editsCountStatsView.setImageDrawable(R.drawable.ic_mode_edit_white_24dp)
        binding.editsCountStatsView.tooltipText = getString(R.string.suggested_edits_contributions_stat_tooltip)

        binding.editStreakStatsView.setTitle(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        binding.editStreakStatsView.setImageDrawable(R.drawable.ic_icon_revision_history_apps)
        binding.editStreakStatsView.tooltipText = getString(R.string.suggested_edits_edit_streak_stat_tooltip)

        binding.pageViewStatsView.setTitle(getString(R.string.suggested_edits_views_label_text))
        binding.pageViewStatsView.setImageDrawable(R.drawable.ic_trending_up_black_24dp)
        binding.pageViewStatsView.tooltipText = getString(R.string.suggested_edits_page_views_stat_tooltip)

       binding.editQualityStatsView.setGoodnessState(viewModel.revertSeverity)
       binding.editQualityStatsView.setTitle(getString(R.string.suggested_edits_quality_label_text))
       binding.editQualityStatsView.tooltipText = getString(R.string.suggested_edits_edit_quality_stat_tooltip, UserContribStats.totalReverts)
    }

    private fun showOneTimeSequentialUserStatsTooltips() {
        binding.suggestedEditsScrollView.fullScroll(View.FOCUS_UP)
        binding.suggestedEditsScrollView.removeCallbacks(sequentialTooltipRunnable)
        binding.suggestedEditsScrollView.postDelayed(sequentialTooltipRunnable, 500)
    }

    private fun setIPBlockedStatus() {
        clearContents()
        binding.messageCard.setIPBlocked(viewModel.blockMessageWikipedia)
        binding.messageCard.isVisible = true
        UserContributionEvent.logIpBlock()
    }

    private fun maybeSetPausedOrDisabled(): Boolean {
        val pauseEndDate = UserContribStats.maybePauseAndGetEndDate()

        if (viewModel.totalContributions < MIN_CONTRIBUTIONS_FOR_SUGGESTED_EDITS && WikipediaApp.instance.appOrSystemLanguageCode == "en") {
            clearContents()
            binding.messageCard.setDisabled(getString(R.string.suggested_edits_gate_message, AccountUtil.userName))
            binding.messageCard.setPositiveButton(R.string.suggested_edits_learn_more, {
                UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(MIN_CONTRIBUTIONS_GATE_URL))
            }, true)
            binding.messageCard.isVisible = true
            return true
        } else if (UserContribStats.isDisabled()) {
            // Disable the whole feature.
            clearContents()
            binding.messageCard.setDisabled(getString(R.string.suggested_edits_disabled_message, AccountUtil.userName))
            binding.messageCard.isVisible = true
            UserContributionEvent.logDisabled()
            return true
        } else if (pauseEndDate != null) {
            clearContents()
            val localDateTime = LocalDateTime.ofInstant(pauseEndDate.toInstant(), ZoneId.systemDefault()).toLocalDate()
            binding.messageCard.setPaused(getString(R.string.suggested_edits_paused_message, DateUtil.getShortDateString(localDateTime), AccountUtil.userName))
            binding.messageCard.isVisible = true
            UserContributionEvent.logPaused()
            return true
        }

        binding.messageCard.isVisible = false
        return false
    }

    private fun setupTestingButtons() {
        if (!ReleaseUtil.isPreBetaRelease) {
            binding.showIPBlockedMessage.isVisible = false
            binding.showOnboardingMessage.isVisible = false
        }
        binding.showIPBlockedMessage.setOnClickListener { setIPBlockedStatus() }
        binding.showOnboardingMessage.setOnClickListener { viewModel.totalContributions = 0; setFinalUIState() }
    }

    private fun setUpDonorHistoryStatus() {
        if (!ContributionsDashboardHelper.contributionsDashboardEnabled) {
            return
        }
        when (DonorStatus.donorStatus()) {
            DonorStatus.DONOR -> {
                Prefs.donationResults.lastOrNull()?.dateTime?.let {
                    val lastDonateMilli =
                        LocalDateTime.parse(it).atZone(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli()
                    var relativeTimeSpan = DateUtils.getRelativeTimeSpanString(
                        lastDonateMilli,
                        System.currentTimeMillis(),
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_NUMERIC_DATE
                    )
                    // Replace with the original dateTime string
                    if (relativeTimeSpan.contains("/")) {
                        relativeTimeSpan = DateUtil.getMDYDateString(Date(lastDonateMilli))
                    }
                    binding.donorHistoryStatus.text = relativeTimeSpan
                    binding.donorHistoryStatus.isVisible = true
                    binding.lastDonatedChevron.isVisible = true
                    binding.donorHistoryUpdateButton.isVisible = false
                } ?: run {
                    binding.donorHistoryStatus.isVisible = false
                    binding.lastDonatedChevron.isVisible = false
                    binding.donorHistoryUpdateButton.isVisible = true
                }
            }

            DonorStatus.NON_DONOR -> {
                binding.donorHistoryStatus.text = getString(R.string.donor_history_last_donated_never)
                binding.donorHistoryStatus.isVisible = true
                binding.lastDonatedChevron.isVisible = true
                binding.donorHistoryUpdateButton.isVisible = false
            }

            DonorStatus.UNKNOWN -> {
                binding.donorHistoryUpdateButton.setOnClickListener {
                    requestUpdateDonorHistory.launch(DonorHistoryActivity.newIntent(requireContext()))
                }
                binding.donorHistoryStatus.isVisible = false
                binding.lastDonatedChevron.isVisible = false
                binding.donorHistoryUpdateButton.isVisible = true
            }
        }
    }

    private fun setUpTasks() {
        displayedTasks.clear()

        addImageTagsTask = SuggestedEditsTask()
        addImageTagsTask.title = getString(R.string.suggested_edits_image_tags)
        addImageTagsTask.description = getString(R.string.suggested_edits_image_tags_task_detail)
        addImageTagsTask.imageDrawable = R.drawable.ic_image_tag
        addImageTagsTask.primaryAction = getString(R.string.suggested_edits_task_action_text_add)

        addImageCaptionsTask = SuggestedEditsTask()
        addImageCaptionsTask.title = getString(R.string.suggested_edits_image_captions)
        addImageCaptionsTask.description = getString(R.string.suggested_edits_image_captions_task_detail)
        addImageCaptionsTask.imageDrawable = R.drawable.ic_image_caption
        addImageCaptionsTask.primaryAction = getString(R.string.suggested_edits_task_action_text_add)
        addImageCaptionsTask.secondaryAction = getString(R.string.suggested_edits_task_action_text_translate)

        addDescriptionsTask = SuggestedEditsTask()
        addDescriptionsTask.title = getString(R.string.description_edit_tutorial_title_descriptions)
        addDescriptionsTask.description = getString(R.string.suggested_edits_add_descriptions_task_detail)
        addDescriptionsTask.imageDrawable = R.drawable.ic_article_ltr_ooui
        addDescriptionsTask.primaryAction = getString(R.string.suggested_edits_task_action_text_add)
        addDescriptionsTask.secondaryAction = getString(R.string.suggested_edits_task_action_text_translate)

        imageRecommendationsTask = SuggestedEditsTask()
        imageRecommendationsTask.title = getString(R.string.suggested_edits_image_recommendations_task_title)
        imageRecommendationsTask.description = getString(R.string.suggested_edits_image_recommendations_task_detail)
        imageRecommendationsTask.imageDrawable = R.drawable.ic_add_image
        imageRecommendationsTask.primaryAction = getString(R.string.suggested_edits_task_action_text_add)

        vandalismPatrolTask = SuggestedEditsTask()
        vandalismPatrolTask.title = getString(R.string.suggested_edits_edit_patrol)
        vandalismPatrolTask.description = getString(R.string.suggested_edits_edit_patrol_hint)
        vandalismPatrolTask.primaryAction = getString(R.string.suggested_edits_edit_patrol_review)
        vandalismPatrolTask.imageDrawable = R.drawable.ic_patrol_24
        vandalismPatrolTask.primaryActionIcon = R.drawable.ic_check_black_24dp
        vandalismPatrolTask.new = !Prefs.recentEditsOnboardingShown

        if (viewModel.allowToPatrolEdits && viewModel.blockMessageWikipedia.isNullOrEmpty()) {
            Prefs.recentEditsWikiCode = WikipediaApp.instance.appOrSystemLanguageCode
            displayedTasks.add(vandalismPatrolTask)
        }

        if (DescriptionEditUtil.wikiUsesLocalDescriptions(WikipediaApp.instance.wikiSite.languageCode) && viewModel.blockMessageWikipedia.isNullOrEmpty() ||
            !DescriptionEditUtil.wikiUsesLocalDescriptions(WikipediaApp.instance.wikiSite.languageCode) && viewModel.blockMessageWikidata.isNullOrEmpty()) {
            displayedTasks.add(addDescriptionsTask)
        }

        // If app language is `de`, the local edits need to be > 50 edits. See https://phabricator.wikimedia.org/T351275
        if (((WikipediaApp.instance.wikiSite.languageCode == "de" && viewModel.homeContributions > 50) ||
            (WikipediaApp.instance.wikiSite.languageCode != "de" && viewModel.totalContributions > 50)) &&
            viewModel.wikiSupportsImageRecommendations && viewModel.blockMessageWikipedia.isNullOrEmpty()) {
            displayedTasks.add(imageRecommendationsTask)
        }

        if (viewModel.blockMessageCommons.isNullOrEmpty()) {
            displayedTasks.add(addImageCaptionsTask)
            displayedTasks.add(addImageTagsTask)
        }
    }

    private fun maybeShowDonorHistoryUpdatedSnackbar() {
        if (ContributionsDashboardHelper.contributionsDashboardEnabled && ContributionsDashboardHelper.shouldShowDonorHistorySnackbar) {
            FeedbackUtil.showMessage(this, R.string.donor_history_updated_message_snackbar)
            ContributionsDashboardHelper.shouldShowDonorHistorySnackbar = false
            if (!Prefs.contributionsDashboardSurveyDialogShown && Prefs.hasDonorHistorySaved) {
                binding.tasksContainer.postDelayed({
                    if (!isAdded) {
                        return@postDelayed
                    }
                    ContributionsDashboardHelper.showSurveyDialog(requireContext())
                    Prefs.contributionsDashboardSurveyDialogShown = true
                }, TimeUnit.SECONDS.toMillis(10))
            }
        }
    }

    private inner class TaskViewCallback : SuggestedEditsTaskView.Callback {
        override fun onViewClick(task: SuggestedEditsTask, secondary: Boolean) {
            if (WikipediaApp.instance.languageState.appLanguageCodes.size < Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION && secondary) {
                requestAddLanguage.launch(WikipediaLanguagesActivity.newIntent(requireActivity(), Constants.InvokeSource.SUGGESTED_EDITS))
            } else if (task == addDescriptionsTask) {
                ImageRecommendationsEvent.logAction(if (secondary) "add_desc_translate_start" else "add_desc_start", "suggested_edits_dialog")
                startActivity(SuggestionsActivity.newIntent(requireActivity(), if (secondary) TRANSLATE_DESCRIPTION else ADD_DESCRIPTION))
            } else if (task == addImageCaptionsTask) {
                ImageRecommendationsEvent.logAction(if (secondary) "add_caption_translate_start" else "add_caption_start", "suggested_edits_dialog")
                startActivity(SuggestionsActivity.newIntent(requireActivity(), if (secondary) TRANSLATE_CAPTION else ADD_CAPTION))
            } else if (task == addImageTagsTask) {
                ImageRecommendationsEvent.logAction("add_tag_start", "suggested_edits_dialog")
                if (Prefs.showImageTagsOnboarding) {
                    requestAddImageTags.launch(SuggestedEditsImageTagsOnboardingActivity.newIntent(requireContext()))
                } else {
                    startActivity(SuggestionsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS))
                }
            } else if (task == imageRecommendationsTask) {
                ImageRecommendationsEvent.logAction("add_image_start", "suggested_edits_dialog")
                startActivity(SuggestionsActivity.newIntent(requireActivity(), IMAGE_RECOMMENDATIONS))
            } else if (task == vandalismPatrolTask) {
                PatrollerExperienceEvent.logAction("pt_init", "suggested_edits_dialog")
                startActivity(SuggestedEditsRecentEditsActivity.newIntent(requireContext()))
            }
        }
    }

    internal inner class RecyclerAdapter(tasks: List<SuggestedEditsTask>) : DefaultRecyclerAdapter<SuggestedEditsTask, SuggestedEditsTaskView>(tasks) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<SuggestedEditsTaskView> {
            return DefaultViewHolder(SuggestedEditsTaskView(parent.context))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<SuggestedEditsTaskView>, i: Int) {
            holder.view.setUpViews(items[i], callback)
        }
    }

    companion object {
        private const val MIN_CONTRIBUTIONS_FOR_SUGGESTED_EDITS = 3
        private const val MIN_CONTRIBUTIONS_GATE_URL = "https://en.wikipedia.org/wiki/Help:Introduction_to_editing_with_Wiki_Markup/1"

        fun newInstance(): SuggestedEditsTasksFragment {
            return SuggestedEditsTasksFragment()
        }
    }
}
