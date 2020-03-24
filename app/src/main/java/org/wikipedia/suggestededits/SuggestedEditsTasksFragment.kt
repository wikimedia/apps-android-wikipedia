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
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.language.LanguageSettingsInvokeSource
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration

class SuggestedEditsTasksFragment : Fragment() {
    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask
    private lateinit var addImageTagsTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val disposables = CompositeDisposable()
    private var currentTooltip: Toast? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTestingButtons()

        contributionsStatsView.setImageDrawable(R.drawable.ic_mode_edit_white_24dp)
        contributionsStatsView.setOnClickListener { onUserStatClicked(contributionsStatsView) }

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
        tasksRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable, drawStart = false, drawEnd = false,
                horizontalPadding = resources.getDimension(R.dimen.activity_horizontal_margin).toInt()))
        tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)

        clearContents()
    }

    private fun onUserStatClicked(view: View) {
        when (view) {
            contributionsStatsView -> showContributionsStatsViewTooltip()
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

    private fun showContributionsStatsViewTooltip() {
        hideCurrentTooltip()
        currentTooltip = FeedbackUtil.showToastOverView(contributionsStatsView, getString(R.string.suggested_edits_contributions_stat_tooltip), Toast.LENGTH_LONG)
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

        progressBar.visibility = VISIBLE
        disposables.add(SuggestedEditsUserStats.getEditCountsObservable()
                .subscribe({ response ->
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

                        getPageViews()
                    }
                }, { t ->
                    L.e(t)
                    showError(t)
                }))

    }

    private fun getPageViews() {
        val qLangMap = HashMap<String, HashSet<String>>()

        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributions(AccountUtil.getUserName()!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    for (userContribution in response.query()!!.userContributions()) {
                        var descLang = ""
                        val strArr = userContribution.comment.split(" ")
                        for (str in strArr) {
                            if (str.contains("wbsetdescription")) {
                                val descArr = str.split("|")
                                if (descArr.size > 1) {
                                    descLang = descArr[1]
                                    break
                                }
                            }
                        }
                        if (descLang.isEmpty()) {
                            continue
                        }

                        if (!qLangMap.containsKey(userContribution.title)) {
                            qLangMap[userContribution.title] = HashSet()
                        }
                        qLangMap[userContribution.title]!!.add(descLang)
                    }
                    ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }
                .flatMap {
                    if (it.entities().isEmpty()) {
                        return@flatMap Observable.just(0L)
                    }
                    val langArticleMap = HashMap<String, ArrayList<String>>()
                    for (entityKey in it.entities().keys) {
                        val entity = it.entities()[entityKey]!!
                        for (qKey in qLangMap.keys) {
                            if (qKey == entityKey) {
                                for (lang in qLangMap[qKey]!!) {
                                    val dbName = WikiSite.forLanguageCode(lang).dbName()
                                    if (entity.sitelinks().containsKey(dbName)) {
                                        if (!langArticleMap.containsKey(lang)) {
                                            langArticleMap[lang] = ArrayList()
                                        }
                                        langArticleMap[lang]!!.add(entity.sitelinks()[dbName]!!.title)
                                    }
                                }
                                break
                            }
                        }
                    }

                    val observableList = ArrayList<Observable<MwQueryResponse>>()

                    for (lang in langArticleMap.keys) {
                        val site = WikiSite.forLanguageCode(lang)
                        observableList.add(ServiceFactory.get(site).getPageViewsForTitles(langArticleMap[lang]!!.joinToString("|"))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread()))
                    }

                    Observable.zip(observableList) { resultList ->
                        var totalPageViews = 0L
                        for (result in resultList) {
                            if (result is MwQueryResponse && result.query() != null) {
                                for (page in result.query()!!.pages()!!) {
                                    for (day in page.pageViewsMap.values) {
                                        totalPageViews += day ?: 0
                                    }
                                }
                            }
                        }
                        totalPageViews
                    }
                }
                .subscribe({ pageViewsCount ->

                    pageViewStatsView.setTitle(pageViewsCount.toString())
                    setFinalUIState()

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
        swipeRefreshLayout.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.main_toolbar_color))
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
            contributionsStatsView.visibility = GONE
            editQualityStatsView.visibility = GONE
            editStreakStatsView.visibility = GONE
            pageViewStatsView.visibility = GONE
            onboardingImageView.visibility = VISIBLE
            textViewForMessage.text = StringUtil.fromHtml(getString(R.string.suggested_edits_onboarding_message, AccountUtil.getUserName()))
            textViewForMessage.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_primary_color))
        } else {
            contributionsStatsView.visibility = VISIBLE
            editQualityStatsView.visibility = VISIBLE
            editStreakStatsView.visibility = VISIBLE
            pageViewStatsView.visibility = VISIBLE
            onboardingImageView.visibility = GONE
            contributionsStatsView.setTitle(SuggestedEditsUserStats.totalEdits.toString())
            contributionsStatsView.setDescription(resources.getQuantityString(R.plurals.suggested_edits_contribution, SuggestedEditsUserStats.totalEdits))
            textViewForMessage.text = getString(R.string.suggested_edits_encouragement_message, AccountUtil.getUserName())
            textViewForMessage.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_secondary_color))
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
            ipBlocked.visibility = GONE
            onboarding1.visibility = GONE
        }
        ipBlocked.setOnClickListener { setIPBlockedStatus() }
        onboarding1.setOnClickListener { SuggestedEditsUserStats.totalEdits = 0; setFinalUIState() }
    }

    private fun setUpTasks() {
        displayedTasks.clear()

        addImageTagsTask = SuggestedEditsTask()
        addImageTagsTask.title = getString(R.string.suggested_edits_image_tags)
        addImageTagsTask.description = getString(R.string.suggested_edits_image_tags_task_detail)
        addImageTagsTask.imageDrawable = R.drawable.ic_image_tag
        addImageTagsTask.translatable = false

        // TODO: remove condition when ready
        if (ReleaseUtil.isPreBetaRelease()) {
            displayedTasks.add(addImageTagsTask)
        }

        addImageCaptionsTask = SuggestedEditsTask()
        addImageCaptionsTask.title = getString(R.string.suggested_edits_image_captions)
        addImageCaptionsTask.description = getString(R.string.suggested_edits_image_captions_task_detail)
        addImageCaptionsTask.imageDrawable = R.drawable.ic_image_caption
        displayedTasks.add(addImageCaptionsTask)

        addDescriptionsTask = SuggestedEditsTask()
        addDescriptionsTask.title = getString(R.string.description_edit_tutorial_title_descriptions)
        addDescriptionsTask.description = getString(R.string.suggested_edits_add_descriptions_task_detail)
        addDescriptionsTask.imageDrawable = R.drawable.ic_article_description
        displayedTasks.add(addDescriptionsTask)
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
