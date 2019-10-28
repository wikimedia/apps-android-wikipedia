package org.wikipedia.suggestededits

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import androidx.core.graphics.drawable.DrawableCompat
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
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.language.LanguageSettingsInvokeSource
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class SuggestedEditsTasksFragment : Fragment() {
    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val disposables = CompositeDisposable()
    private val toolTipDisposable = CompositeDisposable()
    private var totalEdits = 0

    // TODO: remove when ready
    private var editQualityState = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Todo: remove after review
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
        tasksRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable, false, false))
        tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)

        clearContents()
    }

    private fun onUserStatClicked(view: View) {
        dismissTooltips()
        when (view) {
            contributionsStatsView -> showContributionsStatsViewTooltip()
            editStreakStatsView -> showEditStreakStatsViewTooltip()
            pageViewStatsView -> showPageViewStatsViewTooltip()
            else -> showEditQualityStatsViewTooltip()
        }
    }

    private fun showContributionsStatsViewTooltip() {
        val param = toolTipArrowFirstRow.layoutParams as LinearLayout.LayoutParams
        param.gravity = Gravity.START
        toolTipArrowFirstRow.layoutParams = param
        toolTipTextFirstRow.text = getString(R.string.suggested_edits_contributions_stat_tooltip)
        toolTipFirstRow.visibility = VISIBLE
        dismissTooltipsAfterTimeout()
    }

    private fun showEditStreakStatsViewTooltip() {
        val param = toolTipArrowFirstRow.layoutParams as LinearLayout.LayoutParams
        param.gravity = Gravity.END
        toolTipArrowFirstRow.layoutParams = param
        toolTipTextFirstRow.text = getString(R.string.suggested_edits_edit_streak_stat_tooltip)
        toolTipFirstRow.visibility = VISIBLE
        dismissTooltipsAfterTimeout()
    }

    private fun showPageViewStatsViewTooltip() {
        val param = toolTipArrowSecondRow.layoutParams as LinearLayout.LayoutParams
        param.gravity = Gravity.START
        toolTipArrowSecondRow.layoutParams = param
        toolTipTextSecondRow.text = getString(R.string.suggested_edits_page_views_stat_tooltip)
        toolTipSecondRow.visibility = VISIBLE
        dismissTooltipsAfterTimeout()
    }

    private fun showEditQualityStatsViewTooltip() {
        val param = toolTipArrowSecondRow.layoutParams as LinearLayout.LayoutParams
        param.gravity = Gravity.END
        toolTipArrowSecondRow.layoutParams = param
        toolTipTextSecondRow.text = getString(R.string.suggested_edits_edit_quality_stat_tooltip, 3)
        toolTipSecondRow.visibility = VISIBLE
        dismissTooltipsAfterTimeout()
    }

    private fun dismissTooltipsAfterTimeout() {
        toolTipDisposable.clear()
        toolTipDisposable.add(Observable.timer(5000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { dismissTooltips() })
    }

    private fun dismissTooltips() {
        toolTipFirstRow.visibility = GONE
        toolTipSecondRow.visibility = GONE
    }

    override fun onResume() {
        super.onResume()
        refreshContents()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_suggested_edits_tasks, menu)
        var drawable: Drawable = menu.findItem(R.id.menu_help).icon
        drawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(drawable, ResourceUtil.getThemedColor(context!!, R.attr.colorAccent))
        menu.findItem(R.id.menu_help).icon = drawable
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE) {
            tasksRecyclerView.adapter!!.notifyDataSetChanged()
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
        toolTipDisposable.clear()
    }

    private fun fetchUserContributions() {
        if (!AccountUtil.isLoggedIn()) {
            return
        }

        progressBar.visibility = VISIBLE
        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).editorTaskCounts
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    val editorTaskCounts = response.query()!!.editorTaskCounts()!!
                    if (response.query()!!.userInfo()!!.isBlocked) {

                        setIPBlockedStatus()

                    } else if (!maybeSetDisabledStatus(80)) { // TODO: use edit quality metric from API response

                        editQualityStatsView.setGoodnessState((editQualityState++) % 7) // TODO: use edit quality metric from API response

                        editStreakStatsView.setTitle(resources.getQuantityString(R.plurals.suggested_edits_edit_streak_detail_text,
                                editorTaskCounts.editStreak, editorTaskCounts.editStreak))

                        totalEdits = 0
                        for (count in editorTaskCounts.descriptionEditsPerLanguage.values) {
                            totalEdits += count
                        }
                        for (count in editorTaskCounts.captionEditsPerLanguage.values) {
                            totalEdits += count
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
                .flatMap { entities ->
                    val langArticleMap = HashMap<String, ArrayList<String>>()
                    for (entityKey in entities.entities()!!.keys) {
                        val entity = entities.entities()!![entityKey]!!
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
    }

    private fun showError(t: Throwable) {
        clearContents()
        errorView.setError(t)
        errorView.visibility = VISIBLE
    }

    private fun setFinalUIState() {
        clearContents()

        if (totalEdits == 0) {
            contributionsStatsView.visibility = GONE
            editQualityStatsView.visibility = GONE
            editStreakStatsView.visibility = GONE
            pageViewStatsView.visibility = GONE
            onboardingImageView.visibility = VISIBLE
            textViewForMessage.text = StringUtil.fromHtml(getString(R.string.suggested_edits_onboarding_message, AccountUtil.getUserName()))
        } else {
            contributionsStatsView.visibility = VISIBLE
            editQualityStatsView.visibility = VISIBLE
            editStreakStatsView.visibility = VISIBLE
            pageViewStatsView.visibility = VISIBLE
            onboardingImageView.visibility = GONE
            contributionsStatsView.setTitle(totalEdits.toString())
            contributionsStatsView.setDescription(resources.getQuantityString(R.plurals.suggested_edits_contribution, totalEdits))
            textViewForMessage.text = getString(R.string.suggested_edits_encouragement_message, AccountUtil.getUserName())
        }

        tasksContainer.visibility = VISIBLE
    }

    private fun setIPBlockedStatus() {
        clearContents()
        disabledStatesView.setIPBlocked()
        disabledStatesView.visibility = VISIBLE
    }

    private fun maybeSetDisabledStatus(editQuality: Int): Boolean {
        when (editQuality) {
            // TODO: use correct quality ranges:
            in 0..10 -> {
                clearContents()
                disabledStatesView.setDisabled(getString(R.string.suggested_edits_disabled_message, AccountUtil.getUserName()))
                disabledStatesView.visibility = VISIBLE
                return true
            }
            in 11..50 -> {
                clearContents()
                // TODO: correctly populate the date here:
                disabledStatesView.setPaused(getString(R.string.suggested_edits_paused_message, DateUtil.getShortDateString(Date()), AccountUtil.getUserName()))
                disabledStatesView.visibility = VISIBLE
                return true
            }
        }
        disabledStatesView.visibility = GONE
        return false
    }

    private fun setupTestingButtons() {
        paused.setOnClickListener { maybeSetDisabledStatus(25) }
        disabled.setOnClickListener { maybeSetDisabledStatus(5) }
        ipBlocked.setOnClickListener { setIPBlockedStatus() }
        onboarding1.setOnClickListener { totalEdits = 0; setFinalUIState() }
    }

    private fun setUpTasks() {
        displayedTasks.clear()
        addImageCaptionsTask = SuggestedEditsTask()
        addImageCaptionsTask.title = getString(R.string.suggested_edits_image_captions)
        addImageCaptionsTask.description = getString(R.string.suggested_edits_image_captions_task_detail)
        addImageCaptionsTask.imageDrawable = R.drawable.ic_icon_caption_images
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
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), if (isTranslate) SUGGESTED_EDITS_TRANSLATE_DESC else SUGGESTED_EDITS_ADD_DESC))
            } else if (task == addImageCaptionsTask) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), if (isTranslate) SUGGESTED_EDITS_TRANSLATE_CAPTION else SUGGESTED_EDITS_ADD_CAPTION))
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
