package org.wikipedia.suggestededits

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.UserContributionEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentSuggestedEditsTasksBinding
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.descriptions.DescriptionEditUtil
import org.wikipedia.login.LoginActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.usercontrib.UserContribListActivity
import org.wikipedia.usercontrib.UserContribStats
import org.wikipedia.util.*
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import java.util.*

class SuggestedEditsTasksFragment : Fragment() {
    private var _binding: FragmentSuggestedEditsTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SuggestedEditsTasksFragmentViewModel by viewModels()

    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask
    private lateinit var addImageTagsTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val sequentialTooltipRunnable = Runnable {
        if (!isAdded) {
            return@Runnable
        }
        val balloon = FeedbackUtil.getTooltip(requireContext(), binding.contributionsStatsView.tooltipText, autoDismiss = true, showDismissButton = true)
        balloon.showAlignBottom(binding.contributionsStatsView.getDescriptionView())
        balloon.relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), binding.editStreakStatsView.tooltipText, autoDismiss = true, showDismissButton = true), binding.editStreakStatsView.getDescriptionView())
            .relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), binding.pageViewStatsView.tooltipText, autoDismiss = true, showDismissButton = true), binding.pageViewStatsView.getDescriptionView())
            .relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), binding.editQualityStatsView.tooltipText, autoDismiss = true, showDismissButton = true), binding.editQualityStatsView.getDescriptionView())
        Prefs.showOneTimeSequentialUserStatsTooltip = false
        BreadCrumbLogEvent.logTooltipShown(requireActivity(), binding.contributionsStatsView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTestingButtons()

        binding.userStatsViewsGroup.addOnClickListener {
            startActivity(UserContribListActivity.newIntent(requireActivity(), AccountUtil.userName.orEmpty()))
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

        binding.suggestedEditsScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            (requireActivity() as MainActivity).updateToolbarElevation(scrollY > 0)
        })
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)
        binding.tasksContainer.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is SuggestedEditsTasksFragmentViewModel.UiState.Loading -> onLoading()
                        is SuggestedEditsTasksFragmentViewModel.UiState.Success -> setFinalUIState()
                        is SuggestedEditsTasksFragmentViewModel.UiState.RequireLogin -> onRequireLogin()
                        is SuggestedEditsTasksFragmentViewModel.UiState.Error -> showError(it.throwable)
                    }
                }
            }
        }
    }

    private fun Group.addOnClickListener(listener: View.OnClickListener) {
        referencedIds.forEach { id ->
            binding.userStatsClickTarget.findViewById<View>(id).setOnClickListener(listener)
        }
        binding.userStatsClickTarget.setOnClickListener(listener)
    }

    fun refreshContents() {
        requireActivity().invalidateOptionsMenu()
        viewModel.fetchData()
    }

    override fun onResume() {
        super.onResume()
        refreshContents()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE) {
            binding.tasksRecyclerView.adapter!!.notifyDataSetChanged()
        } else if (requestCode == Constants.ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING && resultCode == Activity.RESULT_OK) {
            Prefs.showImageTagsOnboarding = false
            startActivity(SuggestionsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS, Constants.InvokeSource.SUGGESTED_EDITS))
        } else if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN && resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            clearContents()
        }
    }

    override fun onDestroyView() {
        binding.tasksRecyclerView.adapter = null
        binding.suggestedEditsScrollView.removeCallbacks(sequentialTooltipRunnable)
        _binding = null
        super.onDestroyView()
    }

    private fun onLoading() {
        binding.progressBar.isVisible = true
    }

    private fun onRequireLogin() {
        clearContents()
        binding.disabledStatesView.setRequiredLogin(this)
        binding.disabledStatesView.isVisible = true
    }

    private fun clearContents(shouldScrollToTop: Boolean = true) {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.progressBar.isVisible = false
        binding.tasksContainer.isVisible = false
        binding.errorView.isVisible = false
        binding.disabledStatesView.isVisible = false
        if (shouldScrollToTop) {
            binding.suggestedEditsScrollView.scrollTo(0, 0)
        }
        binding.swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
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

        binding.tasksRecyclerView.adapter!!.notifyDataSetChanged()
        setUserStatsViewsAndTooltips()

        binding.pageViewStatsView.setTitle(viewModel.totalPageviews.toString())

        if (viewModel.latestEditStreak < 2) {
            binding.editStreakStatsView.setTitle(if (viewModel.latestEditDate.time > 0) DateUtil.getMDYDateString(viewModel.latestEditDate) else resources.getString(R.string.suggested_edits_last_edited_never))
            binding.editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_last_edited))
        } else {
            binding.editStreakStatsView.setTitle(resources.getQuantityString(R.plurals.suggested_edits_edit_streak_detail_text,
                viewModel.latestEditStreak, viewModel.latestEditStreak))
            binding.editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        }

        if (viewModel.totalContributions == 0) {
            binding.userStatsClickTarget.isEnabled = false
            binding.userStatsViewsGroup.visibility = GONE
            binding.onboardingImageView.visibility = VISIBLE
            binding.onboardingTextView.visibility = VISIBLE
            binding.onboardingTextView.text = StringUtil.fromHtml(getString(R.string.suggested_edits_onboarding_message, AccountUtil.userName))
        } else {
            binding.userStatsViewsGroup.visibility = VISIBLE
            binding.onboardingImageView.visibility = GONE
            binding.onboardingTextView.visibility = GONE
            binding.userStatsClickTarget.isEnabled = true
            binding.userNameView.text = AccountUtil.userName
            binding.contributionsStatsView.setTitle(viewModel.totalContributions.toString())
            binding.contributionsStatsView.setDescription(resources.getQuantityString(R.plurals.suggested_edits_contribution, viewModel.totalContributions))
            if (Prefs.showOneTimeSequentialUserStatsTooltip) {
                showOneTimeSequentialUserStatsTooltips()
            }
        }

        binding.swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        binding.tasksContainer.isVisible = true
    }

    private fun setUserStatsViewsAndTooltips() {
        binding.contributionsStatsView.setImageDrawable(R.drawable.ic_mode_edit_white_24dp)
        binding.contributionsStatsView.tooltipText = getString(R.string.suggested_edits_contributions_stat_tooltip)

        binding.editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        binding.editStreakStatsView.setImageDrawable(R.drawable.ic_timer_black_24dp)
        binding.editStreakStatsView.tooltipText = getString(R.string.suggested_edits_edit_streak_stat_tooltip)

        binding.pageViewStatsView.setDescription(getString(R.string.suggested_edits_views_label_text))
        binding.pageViewStatsView.setImageDrawable(R.drawable.ic_trending_up_black_24dp)
        binding.pageViewStatsView.tooltipText = getString(R.string.suggested_edits_page_views_stat_tooltip)

       binding.editQualityStatsView.setGoodnessState(viewModel.revertSeverity)
       binding.editQualityStatsView.setDescription(getString(R.string.suggested_edits_quality_label_text))
       binding.editQualityStatsView.tooltipText = getString(R.string.suggested_edits_edit_quality_stat_tooltip, UserContribStats.totalReverts)
    }

    private fun showOneTimeSequentialUserStatsTooltips() {
        binding.suggestedEditsScrollView.fullScroll(View.FOCUS_UP)
        binding.suggestedEditsScrollView.removeCallbacks(sequentialTooltipRunnable)
        binding.suggestedEditsScrollView.postDelayed(sequentialTooltipRunnable, 500)
    }

    private fun setIPBlockedStatus() {
        clearContents()
        binding.disabledStatesView.setIPBlocked(viewModel.blockMessageWikipedia)
        binding.disabledStatesView.visibility = VISIBLE
        UserContributionEvent.logIpBlock()
    }

    private fun maybeSetPausedOrDisabled(): Boolean {
        val pauseEndDate = UserContribStats.maybePauseAndGetEndDate()

        if (viewModel.totalContributions < MIN_CONTRIBUTIONS_FOR_SUGGESTED_EDITS && WikipediaApp.instance.appOrSystemLanguageCode == "en") {
            clearContents()
            binding.disabledStatesView.setDisabled(getString(R.string.suggested_edits_gate_message, AccountUtil.userName))
            binding.disabledStatesView.setPositiveButton(R.string.suggested_edits_learn_more, {
                UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(MIN_CONTRIBUTIONS_GATE_URL))
            }, true)
            binding.disabledStatesView.visibility = VISIBLE
            return true
        } else if (UserContribStats.isDisabled()) {
            // Disable the whole feature.
            clearContents()
            binding.disabledStatesView.setDisabled(getString(R.string.suggested_edits_disabled_message, AccountUtil.userName))
            binding.disabledStatesView.visibility = VISIBLE
            UserContributionEvent.logDisabled()
            return true
        } else if (pauseEndDate != null) {
            clearContents()
            binding.disabledStatesView.setPaused(getString(R.string.suggested_edits_paused_message, DateUtil.getShortDateString(pauseEndDate), AccountUtil.userName))
            binding.disabledStatesView.visibility = VISIBLE
            UserContributionEvent.logPaused()
            return true
        }

        binding.disabledStatesView.visibility = GONE
        return false
    }

    private fun setupTestingButtons() {
        if (!ReleaseUtil.isPreBetaRelease) {
            binding.showIPBlockedMessage.visibility = GONE
            binding.showOnboardingMessage.visibility = GONE
        }
        binding.showIPBlockedMessage.setOnClickListener { setIPBlockedStatus() }
        binding.showOnboardingMessage.setOnClickListener { viewModel.totalContributions = 0; setFinalUIState() }
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

        if (DescriptionEditUtil.wikiUsesLocalDescriptions(WikipediaApp.instance.wikiSite.languageCode) && viewModel.blockMessageWikipedia.isNullOrEmpty() ||
            !DescriptionEditUtil.wikiUsesLocalDescriptions(WikipediaApp.instance.wikiSite.languageCode) && viewModel.blockMessageWikidata.isNullOrEmpty()) {
            displayedTasks.add(addDescriptionsTask)
        }

        if (viewModel.blockMessageCommons.isNullOrEmpty()) {
            displayedTasks.add(addImageCaptionsTask)
            displayedTasks.add(addImageTagsTask)
        }
    }

    private inner class TaskViewCallback : SuggestedEditsTaskView.Callback {
        override fun onViewClick(task: SuggestedEditsTask, secondary: Boolean) {
            if (WikipediaApp.instance.languageState.appLanguageCodes.size < Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION && secondary) {
                startActivityForResult(WikipediaLanguagesActivity.newIntent(requireActivity(), Constants.InvokeSource.SUGGESTED_EDITS), Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE)
                return
            }
            if (task == addDescriptionsTask) {
                startActivity(SuggestionsActivity.newIntent(requireActivity(), if (secondary) TRANSLATE_DESCRIPTION else ADD_DESCRIPTION, Constants.InvokeSource.SUGGESTED_EDITS))
            } else if (task == addImageCaptionsTask) {
                startActivity(SuggestionsActivity.newIntent(requireActivity(), if (secondary) TRANSLATE_CAPTION else ADD_CAPTION, Constants.InvokeSource.SUGGESTED_EDITS))
            } else if (task == addImageTagsTask) {
                if (Prefs.showImageTagsOnboarding) {
                    startActivityForResult(SuggestedEditsImageTagsOnboardingActivity.newIntent(requireContext()), Constants.ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING)
                } else {
                    startActivity(SuggestionsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS, Constants.InvokeSource.SUGGESTED_EDITS))
                }
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
