package org.wikipedia.suggestededits

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Function3
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_tasks.*
import org.wikipedia.Constants
import org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE
import org.wikipedia.Constants.ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.language.LanguageSettingsInvokeSource
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
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
    private var currentTooltip: Toast? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTestingButtons()

        userStatsClickTarget.setOnClickListener {
            startActivity(SuggestedEditsContributionsActivity.newIntent(requireActivity()))
        }

        contributionsStatsView.setOnClickListener {
            startActivity(SuggestedEditsContributionsActivity.newIntent(requireActivity()))
        }
        contributionsStatsView.setImageDrawable(R.drawable.ic_mode_edit_white_24dp)

        editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        editStreakStatsView.setImageDrawable(R.drawable.ic_timer_black_24dp)
        editStreakStatsView.setOnClickListener { onUserStatClicked(editStreakStatsView) }

        pageViewStatsView.setDescription(getString(R.string.suggested_edits_pageviews_label_text))
        pageViewStatsView.setImageDrawable(R.drawable.ic_trending_up_black_24dp)
        pageViewStatsView.setOnClickListener { onUserStatClicked(pageViewStatsView) }

        editQualityStatsView.setDescription(getString(R.string.suggested_edits_quality_label_text))
        editQualityStatsView.setOnClickListener { onUserStatClicked(editQualityStatsView) }

        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        swipeRefreshLayout.setOnRefreshListener { this.refreshContents() }

        errorView.setRetryClickListener { refreshContents() }

        suggestedEditsScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            (requireActivity() as MainActivity).updateToolbarElevation(scrollY > 0)
        })
        setUpTasks()
        tasksRecyclerView.layoutManager = LinearLayoutManager(context)
        tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)

        clearContents()
    }

    private fun onUserStatClicked(view: View) {
        when (view) {
            editStreakStatsView -> showEditStreakStatsViewTooltip()
            pageViewStatsView -> showPageViewStatsViewTooltip()
            else -> showEditQualityStatsViewTooltip()
        }
    }

    private fun hideCurrentTooltip() {
        if (currentTooltip != null) {
            currentTooltip!!.cancel()
            currentTooltip = null
        }
    }

    private fun showEditStreakStatsViewTooltip() {
        hideCurrentTooltip()
        currentTooltip = FeedbackUtil.showToastOverView(editStreakStatsView, getString(R.string.suggested_edits_edit_streak_stat_tooltip), FeedbackUtil.LENGTH_LONG)
    }

    private fun showPageViewStatsViewTooltip() {
        hideCurrentTooltip()
        currentTooltip = FeedbackUtil.showToastOverView(pageViewStatsView, getString(R.string.suggested_edits_page_views_stat_tooltip), Toast.LENGTH_LONG)
    }

    private fun showEditQualityStatsViewTooltip() {
        hideCurrentTooltip()
        currentTooltip = FeedbackUtil.showToastOverView(editQualityStatsView, getString(R.string.suggested_edits_edit_quality_stat_tooltip, SuggestedEditsUserStats.totalReverts), FeedbackUtil.LENGTH_LONG)
    }

    override fun onPause() {
        super.onPause()
        hideCurrentTooltip()
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_suggested_edits_tasks, menu)
        ResourceUtil.setMenuItemTint(requireContext(), menu.findItem(R.id.menu_help), R.attr.colorAccent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE) {
            tasksRecyclerView.adapter!!.notifyDataSetChanged()
        } else if (requestCode == ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING && resultCode == Activity.RESULT_OK) {
            Prefs.setShowImageTagsOnboarding(false)
            startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        SuggestedEditsFunnel.get().log()
        SuggestedEditsFunnel.reset()
    }

    private fun fetchUserContributions() {
        if (!AccountUtil.isLoggedIn()) {
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
                SuggestedEditsUserStats.getEditCountsObservable(),
                Function3<MwQueryResponse, MwQueryResponse, MwQueryResponse, MwQueryResponse> { commonsResponse, wikidataResponse, suggestedStatsResponse ->
                    if (wikidataResponse.query()!!.userInfo()!!.isBlocked || commonsResponse.query()!!.userInfo()!!.isBlocked) {
                        isIpBlocked = true
                    }

                    totalContributions += wikidataResponse.query()!!.userInfo()!!.editCount
                    totalContributions += commonsResponse.query()!!.userInfo()!!.editCount

                    latestEditDate = wikidataResponse.query()!!.userInfo()!!.latestContrib
                    if (commonsResponse.query()!!.userInfo()!!.latestContrib.after(latestEditDate)) {
                        latestEditDate = commonsResponse.query()!!.userInfo()!!.latestContrib
                    }

                    if (maybeSetPausedOrDisabled()) {
                        isPausedOrDisabled = true
                    }

                    val contributions = ArrayList<UserContribution>()
                    contributions.addAll(wikidataResponse.query()!!.userContributions())
                    contributions.addAll(commonsResponse.query()!!.userContributions())
                    contributions.sortWith(Comparator { o2, o1 -> ( o1.date().compareTo(o2.date())) })

                    latestEditStreak = getEditStreak(contributions)

                    revertSeverity = SuggestedEditsUserStats.getRevertSeverity()
                    wikidataResponse
                })
                .flatMap { response ->
                    SuggestedEditsUserStats.getPageViewsObservable(response)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    if (isIpBlocked) {
                        setIPBlockedStatus()
                    }
                }
                .subscribe({
                    if (!isPausedOrDisabled && !isIpBlocked) {
                        pageViewStatsView.setTitle(it.toString())
                        setFinalUIState()
                    }
                }, { t ->
                    L.e(t)
                    showError(t)
                }))
    }

    private fun refreshContents() {
        requireActivity().invalidateOptionsMenu()
        fetchUserContributions()
    }

    private fun clearContents() {
        swipeRefreshLayout.isRefreshing = false
        progressBar.visibility = GONE
        tasksContainer.visibility = GONE
        errorView.visibility = GONE
        disabledStatesView.visibility = GONE
        suggestedEditsScrollView.scrollTo(0, 0)
        swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
    }

    private fun showError(t: Throwable) {
        clearContents()
        errorView.setError(t)
        errorView.visibility = VISIBLE
    }

    private fun setFinalUIState() {
        clearContents()

        addImageTagsTask.new = Prefs.isSuggestedEditsImageTagsNew()
        tasksRecyclerView.adapter!!.notifyDataSetChanged()
        editQualityStatsView.setGoodnessState(revertSeverity)

        if (latestEditStreak < 2) {
            editStreakStatsView.setTitle(if (latestEditDate.time > 0) DateUtil.getMDYDateString(latestEditDate) else resources.getString(R.string.suggested_edits_last_edited_never))
            editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_last_edited))
        } else {
            editStreakStatsView.setTitle(resources.getQuantityString(R.plurals.suggested_edits_edit_streak_detail_text,
                    latestEditStreak, latestEditStreak))
            editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        }

        if (totalContributions == 0) {
            userNameView.visibility = GONE
            contributionsStatsView.visibility = GONE
            editQualityStatsView.visibility = GONE
            editStreakStatsView.visibility = GONE
            pageViewStatsView.visibility = GONE
            onboardingImageView.visibility = VISIBLE
            onboardingTextView.visibility = VISIBLE
            onboardingTextView.text = StringUtil.fromHtml(getString(R.string.suggested_edits_onboarding_message, AccountUtil.getUserName()))
        } else {
            userNameView.text = AccountUtil.getUserName()
            userNameView.visibility = VISIBLE
            contributionsStatsView.visibility = VISIBLE
            editQualityStatsView.visibility = VISIBLE
            editStreakStatsView.visibility = VISIBLE
            pageViewStatsView.visibility = VISIBLE
            onboardingImageView.visibility = GONE
            onboardingTextView.visibility = GONE
            contributionsStatsView.setTitle(totalContributions.toString())
            contributionsStatsView.setDescription(resources.getQuantityString(R.plurals.suggested_edits_contribution, totalContributions))
        }

        swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        tasksContainer.visibility = VISIBLE
    }

    private fun setIPBlockedStatus() {
        clearContents()
        disabledStatesView.setIPBlocked()
        disabledStatesView.visibility = VISIBLE
    }

    private fun maybeSetPausedOrDisabled(): Boolean {
        val pauseEndDate = SuggestedEditsUserStats.maybePauseAndGetEndDate()

        if (SuggestedEditsUserStats.isDisabled()) {
            // Disable the whole feature.
            clearContents()
            disabledStatesView.setDisabled(getString(R.string.suggested_edits_disabled_message, AccountUtil.getUserName()))
            disabledStatesView.visibility = VISIBLE
            return true
        } else if (pauseEndDate != null) {
            clearContents()
            disabledStatesView.setPaused(getString(R.string.suggested_edits_paused_message, DateUtil.getShortDateString(pauseEndDate), AccountUtil.getUserName()))
            disabledStatesView.visibility = VISIBLE
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

        displayedTasks.add(addImageTagsTask)
        displayedTasks.add(addDescriptionsTask)
        displayedTasks.add(addImageCaptionsTask)
    }


    private inner class TaskViewCallback : SuggestedEditsTaskView.Callback {
        override fun onViewClick(task: SuggestedEditsTask, isTranslate: Boolean) {
            if (WikipediaApp.getInstance().language().appLanguageCodes.size < Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION && isTranslate) {
                showLanguagesActivity(LanguageSettingsInvokeSource.SUGGESTED_EDITS.text())
                return
            }
            if (task == addDescriptionsTask) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), if (isTranslate) TRANSLATE_DESCRIPTION else ADD_DESCRIPTION))
            } else if (task == addImageCaptionsTask) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), if (isTranslate) TRANSLATE_CAPTION else ADD_CAPTION))
            } else if (task == addImageTagsTask) {
                if (Prefs.shouldShowImageTagsOnboarding()) {
                    startActivityForResult(SuggestedEditsImageTagsOnboardingActivity.newIntent(requireContext()), ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING)
                } else {
                    startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS))
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
