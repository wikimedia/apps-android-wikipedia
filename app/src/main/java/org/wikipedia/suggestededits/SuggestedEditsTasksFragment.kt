package org.wikipedia.suggestededits

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_suggested_edits_tasks.*
import org.wikipedia.Constants
import org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE
import org.wikipedia.Constants.ACTIVITY_REQUEST_IMAGE_TAGS_ONBOARDING
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.language.LanguageSettingsInvokeSource
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder

class SuggestedEditsTasksFragment : Fragment() {
    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask
    private lateinit var addImageTagsTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val disposables = CompositeDisposable()

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

        learnMoreButton.setOnClickListener {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
        }

        contributionsStatsView.setImageDrawable(R.drawable.ic_mode_edit_white_24dp)

        editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
        editStreakStatsView.setImageDrawable(R.drawable.ic_timer_black_24dp)

        pageViewStatsView.setDescription(getString(R.string.suggested_edits_pageviews_label_text))
        pageViewStatsView.setImageDrawable(R.drawable.ic_trending_up_black_24dp)

        editQualityStatsView.setDescription(getString(R.string.suggested_edits_quality_label_text))

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
            startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), ADD_IMAGE_TAGS))
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

        progressBar.visibility = VISIBLE
        disposables.add(SuggestedEditsUserStats.getEditCountsObservable()
                .map { response ->
                    var shouldLoadPageViews = false
                    if (response.query()!!.userInfo()!!.isBlocked) {

                        setIPBlockedStatus()

                    } else if (!maybeSetPausedOrDisabled()) {
                        val editorTaskCounts = response.query()!!.editorTaskCounts()!!

                        editQualityStatsView.setGoodnessState(SuggestedEditsUserStats.getRevertSeverity())

                        if (editorTaskCounts.editStreak < 2) {
                            editStreakStatsView.setTitle(if (editorTaskCounts.lastEditDate.time > 0) DateUtil.getMDYDateString(editorTaskCounts.lastEditDate) else resources.getString(R.string.suggested_edits_last_edited_never))
                            editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_last_edited))
                        } else {
                            editStreakStatsView.setTitle(resources.getQuantityString(R.plurals.suggested_edits_edit_streak_detail_text,
                                    editorTaskCounts.editStreak, editorTaskCounts.editStreak))
                            editStreakStatsView.setDescription(resources.getString(R.string.suggested_edits_edit_streak_label_text))
                        }
                        shouldLoadPageViews = true
                    }
                    shouldLoadPageViews
                }
                .flatMap {
                    if (it) {
                        SuggestedEditsUserStats.getPageViewsObservable()
                    } else {
                        Observable.just((-1).toLong())
                    }
                }
                .subscribe({
                    if (it >= 0) {
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

        if (SuggestedEditsUserStats.totalEdits == 0) {
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
            contributionsStatsView.setTitle(SuggestedEditsUserStats.totalEdits.toString())
            contributionsStatsView.setDescription(resources.getQuantityString(R.plurals.suggested_edits_contribution, SuggestedEditsUserStats.totalEdits))
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

    private fun setupTestingButtons() {
        if (!ReleaseUtil.isPreBetaRelease()) {
            showIPBlockedMessage.visibility = GONE
            showOnboardingMessage.visibility = GONE
        }
        showIPBlockedMessage.setOnClickListener { setIPBlockedStatus() }
        showOnboardingMessage.setOnClickListener { SuggestedEditsUserStats.totalEdits = 0; setFinalUIState() }
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
