package org.wikipedia.suggestededits

import android.app.Activity
import android.content.Intent
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
import kotlinx.android.synthetic.main.fragment_suggested_edits_tasks.*
import kotlinx.android.synthetic.main.view_image_title_description.view.*
import org.wikipedia.Constants.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.analytics.UserContributionFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.language.LanguageSettingsInvokeSource
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
import kotlin.collections.ArrayList

class SuggestedEditsTasksFragment : Fragment() {
    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask
    private lateinit var addImageTagsTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val disposables = CompositeDisposable()
    private var isIpBlocked = false
    private var isPausedOrDisabled = false
    private var totalContributions = 0
    private var latestEditDate = Date()
    private var latestEditStreak = 0
    private var revertSeverity = 0

    private val sequentialTooltipRunnable = Runnable {
        if (!isAdded) {
            return@Runnable
        }
        val balloon = FeedbackUtil.getTooltip(requireContext(), contributionsStatsView.tooltipText, false, true)
        balloon.showAlignBottom(contributionsStatsView.description)
        balloon.relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), editStreakStatsView.tooltipText, false, true), editStreakStatsView.description)
                .relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), pageViewStatsView.tooltipText, false, true), pageViewStatsView.description)
                .relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), editQualityStatsView.tooltipText, false, true), editQualityStatsView.description)
        Prefs.shouldShowOneTimeSequentialUserStatsTooltip(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTestingButtons()

        userStatsViewsGroup.addOnClickListener {
            startActivity(ContributionsActivity.newIntent(requireActivity()))
        }

        learnMoreCard.setOnClickListener {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
        }
        learnMoreButton.setOnClickListener {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
        }

        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        swipeRefreshLayout.setOnRefreshListener { refreshContents() }

        errorView.setRetryClickListener { refreshContents() }

        suggestedEditsScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            (requireActivity() as MainActivity).updateToolbarElevation(scrollY > 0)
        })
        setUpTasks()
        tasksRecyclerView.layoutManager = LinearLayoutManager(context)
        tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)

        clearContents()
    }

    private fun Group.addOnClickListener(listener: View.OnClickListener) {
        referencedIds.forEach { id ->
            userStatsClickTarget.findViewById<View>(id).setOnClickListener(listener)
        }
        userStatsClickTarget.setOnClickListener(listener)
    }

    override fun onPause() {
        super.onPause()
        SuggestedEditsFunnel.get().pause()
    }

    override fun onResume() {
        super.onResume()
        refreshContents()
        SuggestedEditsFunnel.get().resume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE) {
            tasksRecyclerView.adapter!!.notifyDataSetChanged()
        } else if (requestCode == ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING && resultCode == Activity.RESULT_OK) {
            Prefs.setShowImageTagsOnboarding(false)
            startActivity(SuggestionsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS, InvokeSource.SUGGESTED_EDITS))
        } else if (requestCode == ACTIVITY_REQUEST_LOGIN && resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            clearContents()
        }
    }

    override fun onDestroyView() {
        tasksRecyclerView.adapter = null
        disposables.clear()
        suggestedEditsScrollView.removeCallbacks(sequentialTooltipRunnable)
        SuggestedEditsFunnel.get().log()
        SuggestedEditsFunnel.reset()
        super.onDestroyView()
    }

    private fun fetchUserContributions() {
        if (!AccountUtil.isLoggedIn()) {
            showAccountCreationOrIPBlocked()
            return
        }

        isIpBlocked = false
        isPausedOrDisabled = false
        totalContributions = 0
        latestEditStreak = 0
        revertSeverity = 0
        progressBar.visibility = VISIBLE

        disposables.add(Observable.zip(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserContributions(AccountUtil.getUserName()!!, 10, null).subscribeOn(Schedulers.io()),
                ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributions(AccountUtil.getUserName()!!, 10, null).subscribeOn(Schedulers.io()),
                UserContributionsStats.getEditCountsObservable(), { commonsResponse, wikidataResponse, _ ->
                    if (wikidataResponse.query()!!.userInfo()!!.isBlocked || commonsResponse.query()!!.userInfo()!!.isBlocked) {
                        isIpBlocked = true
                    }

                    totalContributions += wikidataResponse.query()!!.userInfo()!!.editCount
                    totalContributions += commonsResponse.query()!!.userInfo()!!.editCount

                    latestEditDate = wikidataResponse.query()!!.userInfo()!!.latestContrib
                    if (commonsResponse.query()!!.userInfo()!!.latestContrib.after(latestEditDate)) {
                        latestEditDate = commonsResponse.query()!!.userInfo()!!.latestContrib
                    }

                    val contributions = ArrayList<UserContribution>()
                    contributions.addAll(wikidataResponse.query()!!.userContributions())
                    contributions.addAll(commonsResponse.query()!!.userContributions())
                    contributions.sortWith { o2, o1 -> (o1.date().compareTo(o2.date())) }
                    latestEditStreak = getEditStreak(contributions)
                    revertSeverity = UserContributionsStats.getRevertSeverity()
                    wikidataResponse
                })
                .flatMap { response ->
                    UserContributionsStats.getPageViewsObservable(response)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    if (isIpBlocked) {
                        setIPBlockedStatus()
                    }
                }
                .subscribe({
                    if (maybeSetPausedOrDisabled()) {
                        isPausedOrDisabled = true
                    }

                    if (!isPausedOrDisabled && !isIpBlocked) {
                        pageViewStatsView.setTitle(it.toString())
                        setFinalUIState()
                    }
                }, { t ->
                    L.e(t)
                    showError(t)
                }))
    }

    private fun showAccountCreationOrIPBlocked() {
        disposables.add(ServiceFactory.get(WikipediaApp.getInstance().wikiSite).userInfo
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    if (response.query()!!.userInfo()!!.isBlocked) {
                        setIPBlockedStatus()
                    } else {
                        setRequiredLoginStatus()
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
        swipeRefreshLayout.isRefreshing = false
        progressBar.visibility = GONE
        tasksContainer.visibility = GONE
        errorView.visibility = GONE
        disabledStatesView.visibility = GONE
        if (shouldScrollToTop) {
            suggestedEditsScrollView.scrollTo(0, 0)
        }
        swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
    }

    private fun showError(t: Throwable) {
        clearContents()
        errorView.setError(t)
        errorView.visibility = VISIBLE
    }

    private fun setFinalUIState() {
        clearContents(false)

        tasksRecyclerView.adapter!!.notifyDataSetChanged()

        setUserStatsViewsAndTooltips()

        if (latestEditStreak < 2) {
            editStreakStatsView.setTitle(if (latestEditDate.time > 0) DateUtil.getMDYDateString(latestEditDate) else resources.getString(R.string.suggested_edits_last_edited_never))
            editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_last_edited))
        } else {
            editStreakStatsView.setTitle(resources.getQuantityString(R.plurals.suggested_edits_edit_streak_detail_text,
                    latestEditStreak, latestEditStreak))
            editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        }

        if (totalContributions == 0) {
            userStatsClickTarget.isEnabled = false
            userStatsViewsGroup.visibility = GONE
            onboardingImageView.visibility = VISIBLE
            onboardingTextView.visibility = VISIBLE
            onboardingTextView.text = StringUtil.fromHtml(getString(R.string.suggested_edits_onboarding_message, AccountUtil.getUserName()))
        } else {
            userStatsViewsGroup.visibility = VISIBLE
            onboardingImageView.visibility = GONE
            onboardingTextView.visibility = GONE
            userStatsClickTarget.isEnabled = true
            userNameView.text = AccountUtil.getUserName()
            contributionsStatsView.setTitle(totalContributions.toString())
            contributionsStatsView.setDescription(resources.getQuantityString(R.plurals.suggested_edits_contribution, totalContributions))
            if (Prefs.shouldShowOneTimeSequentialUserStatsTooltip()) {
                showOneTimeSequentialUserStatsTooltips()
            }
        }

        swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        tasksContainer.visibility = VISIBLE
    }

    private fun setUserStatsViewsAndTooltips() {
        contributionsStatsView.setImageDrawable(R.drawable.ic_mode_edit_white_24dp)
        contributionsStatsView.tooltipText = getString(R.string.suggested_edits_contributions_stat_tooltip)

        editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        editStreakStatsView.setImageDrawable(R.drawable.ic_timer_black_24dp)
        editStreakStatsView.tooltipText = getString(R.string.suggested_edits_edit_streak_stat_tooltip)

        pageViewStatsView.setDescription(getString(R.string.suggested_edits_views_label_text))
        pageViewStatsView.setImageDrawable(R.drawable.ic_trending_up_black_24dp)
        pageViewStatsView.tooltipText = getString(R.string.suggested_edits_page_views_stat_tooltip)

        editQualityStatsView.setGoodnessState(revertSeverity)
        editQualityStatsView.setDescription(getString(R.string.suggested_edits_quality_label_text))
        editQualityStatsView.tooltipText = getString(R.string.suggested_edits_edit_quality_stat_tooltip, UserContributionsStats.totalReverts)
    }

    private fun showOneTimeSequentialUserStatsTooltips() {
        suggestedEditsScrollView.fullScroll(View.FOCUS_UP)
        suggestedEditsScrollView.removeCallbacks(sequentialTooltipRunnable)
        suggestedEditsScrollView.postDelayed(sequentialTooltipRunnable, 500)
    }

    private fun setIPBlockedStatus() {
        clearContents()
        disabledStatesView.setIPBlocked()
        disabledStatesView.visibility = VISIBLE
        UserContributionFunnel.get().logIpBlock()
    }

    private fun setRequiredLoginStatus() {
        clearContents()
        disabledStatesView.setRequiredLogin(this)
        disabledStatesView.visibility = VISIBLE
    }

    private fun maybeSetPausedOrDisabled(): Boolean {
        val pauseEndDate = UserContributionsStats.maybePauseAndGetEndDate()

        if (UserContributionsStats.isDisabled()) {
            // Disable the whole feature.
            clearContents()
            disabledStatesView.setDisabled(getString(R.string.suggested_edits_disabled_message, AccountUtil.getUserName()))
            disabledStatesView.visibility = VISIBLE
            UserContributionFunnel.get().logDisabled()
            return true
        } else if (pauseEndDate != null) {
            clearContents()
            disabledStatesView.setPaused(getString(R.string.suggested_edits_paused_message, DateUtil.getShortDateString(pauseEndDate), AccountUtil.getUserName()))
            disabledStatesView.visibility = VISIBLE
            UserContributionFunnel.get().logPaused()
            return true
        }

        disabledStatesView.visibility = GONE
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
        var streak = 1
        for (c in contributions) {
            if (c.date().time >= baseCal.timeInMillis) {
                // this contribution was on the same day.
                continue
            } else if (c.date().time < (baseCal.timeInMillis - dayMillis)) {
                // this contribution is more than one day apart, so the streak is broken.
                break
            }
            streak++
            calendar.timeInMillis = calendar.timeInMillis - dayMillis
        }
        return streak
    }

    private fun setupTestingButtons() {
        if (!ReleaseUtil.isPreBetaRelease()) {
            showIPBlockedMessage.visibility = GONE
            showOnboardingMessage.visibility = GONE
        }
        showIPBlockedMessage.setOnClickListener { setIPBlockedStatus() }
        showOnboardingMessage.setOnClickListener { totalContributions = 0; setFinalUIState() }
    }

    private fun setUpTasks() {
        displayedTasks.clear()

        addImageTagsTask = SuggestedEditsTask()
        addImageTagsTask.title = getString(R.string.suggested_edits_image_tags)
        addImageTagsTask.description = getString(R.string.suggested_edits_image_tags_task_detail)
        addImageTagsTask.imageDrawable = R.drawable.ic_image_tag
        addImageTagsTask.translatable = false

        addImageCaptionsTask = SuggestedEditsTask()
        addImageCaptionsTask.title = getString(R.string.suggested_edits_image_captions)
        addImageCaptionsTask.description = getString(R.string.suggested_edits_image_captions_task_detail)
        addImageCaptionsTask.imageDrawable = R.drawable.ic_image_caption

        addDescriptionsTask = SuggestedEditsTask()
        addDescriptionsTask.title = getString(R.string.description_edit_tutorial_title_descriptions)
        addDescriptionsTask.description = getString(R.string.suggested_edits_add_descriptions_task_detail)
        addDescriptionsTask.imageDrawable = R.drawable.ic_article_description

        displayedTasks.add(addDescriptionsTask)
        displayedTasks.add(addImageCaptionsTask)
        displayedTasks.add(addImageTagsTask)
    }

    private inner class TaskViewCallback : SuggestedEditsTaskView.Callback {
        override fun onViewClick(task: SuggestedEditsTask, isTranslate: Boolean) {
            if (WikipediaApp.getInstance().language().appLanguageCodes.size < MIN_LANGUAGES_TO_UNLOCK_TRANSLATION && isTranslate) {
                showLanguagesActivity(LanguageSettingsInvokeSource.SUGGESTED_EDITS.text())
                return
            }
            if (task == addDescriptionsTask) {
                startActivity(SuggestionsActivity.newIntent(requireActivity(), if (isTranslate) TRANSLATE_DESCRIPTION else ADD_DESCRIPTION, InvokeSource.SUGGESTED_EDITS))
            } else if (task == addImageCaptionsTask) {
                startActivity(SuggestionsActivity.newIntent(requireActivity(), if (isTranslate) TRANSLATE_CAPTION else ADD_CAPTION, InvokeSource.SUGGESTED_EDITS))
            } else if (task == addImageTagsTask) {
                if (Prefs.shouldShowImageTagsOnboarding()) {
                    startActivityForResult(SuggestedEditsImageTagsOnboardingActivity.newIntent(requireContext()), ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING)
                } else {
                    startActivity(SuggestionsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS, InvokeSource.SUGGESTED_EDITS))
                }
            }
        }
    }

    private fun showLanguagesActivity(invokeSource: String) {
        val intent = WikipediaLanguagesActivity.newIntent(requireActivity(), invokeSource)
        startActivityForResult(intent, ACTIVITY_REQUEST_ADD_A_LANGUAGE)
    }

    internal inner class RecyclerAdapter(tasks: List<SuggestedEditsTask>) : DefaultRecyclerAdapter<SuggestedEditsTask, SuggestedEditsTaskView>(tasks) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<SuggestedEditsTaskView> {
            return DefaultViewHolder(SuggestedEditsTaskView(parent.context))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<SuggestedEditsTaskView>, i: Int) {
            holder.view.setUpViews(items()[i], callback)
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsTasksFragment {
            return SuggestedEditsTasksFragment()
        }
    }
}
