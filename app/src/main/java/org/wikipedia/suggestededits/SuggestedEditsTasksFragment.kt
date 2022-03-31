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
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.analytics.UserContributionFunnel
import org.wikipedia.analytics.eventplatform.UserContributionEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentSuggestedEditsTasksBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.login.LoginActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.userprofile.ContributionsActivity
import org.wikipedia.userprofile.UserContributionsStats
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import java.util.*
import java.util.concurrent.TimeUnit

class SuggestedEditsTasksFragment : Fragment() {
    private var _binding: FragmentSuggestedEditsTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask
    private lateinit var addImageTagsTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val disposables = CompositeDisposable()
    private var blockMessage: String? = null
    private var isPausedOrDisabled = false
    private var totalPageviews = 0L
    private var totalContributions = 0
    private var latestEditDate = Date()
    private var latestEditStreak = 0
    private var revertSeverity = 0

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
            startActivity(ContributionsActivity.newIntent(requireActivity(), totalContributions, totalPageviews))
        }

        binding.learnMoreCard.setOnClickListener {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
        }
        binding.learnMoreButton.setOnClickListener {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
        }

        binding.swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        binding.swipeRefreshLayout.setOnRefreshListener { refreshContents() }

        binding.errorView.retryClickListener = View.OnClickListener { refreshContents() }

        binding.suggestedEditsScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            (requireActivity() as MainActivity).updateToolbarElevation(scrollY > 0)
        })
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)

        clearContents()
    }

    private fun Group.addOnClickListener(listener: View.OnClickListener) {
        referencedIds.forEach { id ->
            binding.userStatsClickTarget.findViewById<View>(id).setOnClickListener(listener)
        }
        binding.userStatsClickTarget.setOnClickListener(listener)
    }

    override fun onPause() {
        super.onPause()
        SuggestedEditsFunnel.get().pause()
    }

    override fun onResume() {
        super.onResume()
        setUpTasks()
        refreshContents()
        SuggestedEditsFunnel.get().resume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
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
        disposables.clear()
        binding.suggestedEditsScrollView.removeCallbacks(sequentialTooltipRunnable)
        SuggestedEditsFunnel.get().log()
        SuggestedEditsFunnel.reset()
        _binding = null
        super.onDestroyView()
    }

    private fun fetchUserContributions() {
        if (!AccountUtil.isLoggedIn) {
            setRequiredLoginStatus()
            return
        }

        disposables.clear()
        blockMessage = null
        isPausedOrDisabled = false
        totalContributions = 0
        latestEditStreak = 0
        revertSeverity = 0
        binding.progressBar.visibility = VISIBLE

        disposables.add(Observable.zip(ServiceFactory.get(WikipediaApp.getInstance().wikiSite).getUserContributions(AccountUtil.userName!!, 10, null).subscribeOn(Schedulers.io()),
                ServiceFactory.get(Constants.commonsWikiSite).getUserContributions(AccountUtil.userName!!, 10, null).subscribeOn(Schedulers.io()),
                ServiceFactory.get(Constants.wikidataWikiSite).getUserContributions(AccountUtil.userName!!, 10, null).subscribeOn(Schedulers.io()),
                UserContributionsStats.getEditCountsObservable()) { homeSiteResponse, commonsResponse, wikidataResponse, _ ->
                    var blockInfo: MwServiceError.BlockInfo? = null
                    when {
                        wikidataResponse.query?.userInfo!!.isBlocked -> blockInfo =
                            wikidataResponse.query?.userInfo!!
                        commonsResponse.query?.userInfo!!.isBlocked -> blockInfo =
                            commonsResponse.query?.userInfo!!
                        homeSiteResponse.query?.userInfo!!.isBlocked -> blockInfo =
                            homeSiteResponse.query?.userInfo!!
                    }
                    if (blockInfo != null) {
                        blockMessage = ThrowableUtil.getBlockMessageHtml(blockInfo)
                    }

                    totalContributions += wikidataResponse.query?.userInfo!!.editCount
                    totalContributions += commonsResponse.query?.userInfo!!.editCount
                    totalContributions += homeSiteResponse.query?.userInfo!!.editCount

                    latestEditDate = wikidataResponse.query?.userInfo!!.latestContribution

                    if (commonsResponse.query?.userInfo!!.latestContribution.after(latestEditDate)) {
                        latestEditDate = commonsResponse.query?.userInfo!!.latestContribution
                    }

                    if (homeSiteResponse.query?.userInfo!!.latestContribution.after(latestEditDate)) {
                        latestEditDate = homeSiteResponse.query?.userInfo!!.latestContribution
                    }

                    val contributions = (wikidataResponse.query!!.userContributions +
                            commonsResponse.query!!.userContributions +
                            homeSiteResponse.query!!.userContributions).sortedByDescending { it.date() }
                    latestEditStreak = getEditStreak(contributions)
                    revertSeverity = UserContributionsStats.getRevertSeverity()
                    wikidataResponse
                }
                .flatMap { response ->
                    UserContributionsStats.getPageViewsObservable(response)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    if (!blockMessage.isNullOrEmpty()) {
                        setIPBlockedStatus()
                    }
                }
                .subscribe({
                    if (maybeSetPausedOrDisabled()) {
                        isPausedOrDisabled = true
                    }

                    if (!isPausedOrDisabled && blockMessage.isNullOrEmpty()) {
                        binding.pageViewStatsView.setTitle(it.toString())
                        totalPageviews = it
                        setFinalUIState()
                    }
                }, { t ->
                    L.e(t)
                    showError(t)
                }))
    }

    fun refreshContents() {
        requireActivity().invalidateOptionsMenu()
        fetchUserContributions()
    }

    private fun clearContents(shouldScrollToTop: Boolean = true) {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.progressBar.visibility = GONE
        binding.tasksContainer.visibility = GONE
        binding.errorView.visibility = GONE
        binding.disabledStatesView.visibility = GONE
        if (shouldScrollToTop) {
            binding.suggestedEditsScrollView.scrollTo(0, 0)
        }
        binding.swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
    }

    private fun showError(t: Throwable) {
        clearContents()
        binding.errorView.setError(t)
        binding.errorView.visibility = VISIBLE
    }

    private fun setFinalUIState() {
        clearContents(false)

        binding.tasksRecyclerView.adapter!!.notifyDataSetChanged()

        setUserStatsViewsAndTooltips()

        if (latestEditStreak < 2) {
            binding.editStreakStatsView.setTitle(if (latestEditDate.time > 0) DateUtil.getMDYDateString(latestEditDate) else resources.getString(R.string.suggested_edits_last_edited_never))
            binding.editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_last_edited))
        } else {
            binding.editStreakStatsView.setTitle(resources.getQuantityString(R.plurals.suggested_edits_edit_streak_detail_text,
                    latestEditStreak, latestEditStreak))
            binding.editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        }

        if (totalContributions == 0) {
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
            binding.contributionsStatsView.setTitle(totalContributions.toString())
            binding.contributionsStatsView.setDescription(resources.getQuantityString(R.plurals.suggested_edits_contribution, totalContributions))
            if (Prefs.showOneTimeSequentialUserStatsTooltip) {
                showOneTimeSequentialUserStatsTooltips()
            }
        }

        binding.swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        binding.tasksContainer.visibility = VISIBLE
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

       binding.editQualityStatsView.setGoodnessState(revertSeverity)
       binding.editQualityStatsView.setDescription(getString(R.string.suggested_edits_quality_label_text))
       binding.editQualityStatsView.tooltipText = getString(R.string.suggested_edits_edit_quality_stat_tooltip, UserContributionsStats.totalReverts)
    }

    private fun showOneTimeSequentialUserStatsTooltips() {
        binding.suggestedEditsScrollView.fullScroll(View.FOCUS_UP)
        binding.suggestedEditsScrollView.removeCallbacks(sequentialTooltipRunnable)
        binding.suggestedEditsScrollView.postDelayed(sequentialTooltipRunnable, 500)
    }

    private fun setIPBlockedStatus() {
        clearContents()
        binding.disabledStatesView.setIPBlocked(blockMessage)
        binding.disabledStatesView.visibility = VISIBLE
        UserContributionFunnel.get().logIpBlock()
        UserContributionEvent.logIpBlock()
    }

    private fun setRequiredLoginStatus() {
        clearContents()
        binding.disabledStatesView.setRequiredLogin(this)
        binding.disabledStatesView.visibility = VISIBLE
    }

    private fun maybeSetPausedOrDisabled(): Boolean {
        val pauseEndDate = UserContributionsStats.maybePauseAndGetEndDate()

        if (totalContributions < MIN_CONTRIBUTIONS_FOR_SUGGESTED_EDITS && WikipediaApp.getInstance().appOrSystemLanguageCode == "en") {
            clearContents()
            binding.disabledStatesView.setDisabled(getString(R.string.suggested_edits_gate_message, AccountUtil.userName))
            binding.disabledStatesView.setPositiveButton(R.string.suggested_edits_learn_more, {
                UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(MIN_CONTRIBUTIONS_GATE_URL))
            }, true)
            binding.disabledStatesView.visibility = VISIBLE
            return true
        } else if (UserContributionsStats.isDisabled()) {
            // Disable the whole feature.
            clearContents()
            binding.disabledStatesView.setDisabled(getString(R.string.suggested_edits_disabled_message, AccountUtil.userName))
            binding.disabledStatesView.visibility = VISIBLE
            UserContributionFunnel.get().logDisabled()
            UserContributionEvent.logDisabled()
            return true
        } else if (pauseEndDate != null) {
            clearContents()
            binding.disabledStatesView.setPaused(getString(R.string.suggested_edits_paused_message, DateUtil.getShortDateString(pauseEndDate), AccountUtil.userName))
            binding.disabledStatesView.visibility = VISIBLE
            UserContributionFunnel.get().logPaused()
            UserContributionEvent.logPaused()
            return true
        }

        binding.disabledStatesView.visibility = GONE
        return false
    }

    private fun getEditStreak(contributions: List<UserContribution>): Int {
        if (contributions.isEmpty()) {
            return 0
        }
        // TODO: This is a bit naive, and should be updated once we switch to java.time.*
        val calendar = GregorianCalendar()
        calendar.time = Date()
        // Start with a calendar that is fixed at the beginning of today's date
        val baseCal = GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        val dayMillis = TimeUnit.DAYS.toMillis(1)
        var streak = 0
        for (c in contributions) {
            if (c.date().time >= baseCal.timeInMillis) {
                // this contribution was on the same day.
                continue
            } else if (c.date().time < (baseCal.timeInMillis - dayMillis)) {
                // this contribution is more than one day apart, so the streak is broken.
                break
            }
            streak++
            baseCal.timeInMillis = baseCal.timeInMillis - dayMillis
        }
        return streak
    }

    private fun setupTestingButtons() {
        if (!ReleaseUtil.isPreBetaRelease) {
            binding.showIPBlockedMessage.visibility = GONE
            binding.showOnboardingMessage.visibility = GONE
        }
        binding.showIPBlockedMessage.setOnClickListener { setIPBlockedStatus() }
        binding.showOnboardingMessage.setOnClickListener { totalContributions = 0; setFinalUIState() }
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
        addDescriptionsTask.imageDrawable = R.drawable.ic_article_description
        addDescriptionsTask.primaryAction = getString(R.string.suggested_edits_task_action_text_add)
        addDescriptionsTask.secondaryAction = getString(R.string.suggested_edits_task_action_text_translate)

        displayedTasks.add(addDescriptionsTask)
        displayedTasks.add(addImageCaptionsTask)
        displayedTasks.add(addImageTagsTask)
    }

    private inner class TaskViewCallback : SuggestedEditsTaskView.Callback {
        override fun onViewClick(task: SuggestedEditsTask, secondary: Boolean) {
            if (WikipediaApp.getInstance().language().appLanguageCodes.size < Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION && secondary) {
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
