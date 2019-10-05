package org.wikipedia.suggestededits

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_tasks.*
import org.apache.commons.lang3.StringUtils
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
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.FooterMarginItemDecoration
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class SuggestedEditsTasksFragment : Fragment() {
    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val disposables = CompositeDisposable()
    private val PADDING_16 = DimenUtil.roundedDpToPx(16.0f)
    private val ELEVATION_4 = DimenUtil.dpToPx(4.0f)
    private var totalEdits = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.elevation = 0f
        dummyButtons()
        contributionsStatsView.setDescription("Contributions")
        contributionsStatsView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_mode_edit_white_24dp)!!)
        contributionsStatsView.setImageBackground(null)
        contributionsStatsView.setOnClickListener { onUserStatClicked(contributionsStatsView) }

        editStreakStatsView.setTitle("99")
        editStreakStatsView.setDescription("Edit streak")
        editStreakStatsView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_timer_black_24dp)!!)
        editStreakStatsView.setImageBackground(null)
        editStreakStatsView.setOnClickListener { onUserStatClicked(editStreakStatsView) }


        pageViewStatsView.setDescription("Pageviews")
        pageViewStatsView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_trending_up_black_24dp)!!)
        pageViewStatsView.setImageBackground(null)
        pageViewStatsView.setOnClickListener { onUserStatClicked(pageViewStatsView) }


        editQualityStatsView.setTitle("Excellent")
        editQualityStatsView.setDescription("Edit quality")
        editQualityStatsView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_check_black_24dp)!!)
        editQualityStatsView.showCircularProgressBar(true)
        editQualityStatsView.setImageBackgroundTint(R.color.green90)
        editQualityStatsView.setImageParams(DimenUtil.roundedDpToPx(16.0f), DimenUtil.roundedDpToPx(16.0f))
        editQualityStatsView.setImageTint(ResourceUtil.getThemedAttributeId(context!!, R.attr.action_mode_green_background))
        editQualityStatsView.setOnClickListener { onUserStatClicked(editQualityStatsView) }

        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        swipeRefreshLayout.setOnRefreshListener { this.updateUI() }

        tasksRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val topDecorationDp = 16
        tasksRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, topDecorationDp))
        setUpTasks()
        tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)
    }

    private fun onUserStatClicked(view: View) {
        when (view) {
            contributionsStatsView -> showContributionsStatsViewTooltip()
            editStreakStatsView -> showEditStreakStatsViewTooltip()
            pageViewStatsView -> showPageViewStatsViewTooltip()
            else -> showEditQualityStatsViewTooltip()
        }
    }

    private fun showContributionsStatsViewTooltip() {
        tooltipLayout.visibility = VISIBLE
        val param = topTooltipArrow.layoutParams as LinearLayout.LayoutParams
        param.gravity = Gravity.START
        topTooltipArrow.layoutParams = param
        executeAfterTimer(true)
        tooltipTextView.text = getString(R.string.suggested_edits_contributions_stat_tooltip)
    }

    private fun showEditStreakStatsViewTooltip() {
        tooltipLayout.visibility = VISIBLE
        val param = topTooltipArrow.layoutParams as LinearLayout.LayoutParams
        param.gravity = Gravity.END
        topTooltipArrow.layoutParams = param
        executeAfterTimer(true)
        tooltipTextView.text = getString(R.string.suggested_edits_edit_streak_stat_tooltip)
    }

    private fun showPageViewStatsViewTooltip() {
        bottomTooltipArrow.visibility = VISIBLE
        val param = bottomTooltipArrow.layoutParams as LinearLayout.LayoutParams
        param.gravity = Gravity.START
        bottomTooltipArrow.layoutParams = param
        textViewForMessage.setBackgroundColor(ResourceUtil.getThemedColor(context!!, R.attr.paper_color))
        textViewForMessage.elevation = ELEVATION_4
        textViewForMessage.setPadding(PADDING_16, PADDING_16, PADDING_16, PADDING_16)
        textViewForMessage.text = getString(R.string.suggested_edits_page_views_stat_tooltip)
        executeAfterTimer(false)
    }

    private fun showEditQualityStatsViewTooltip() {
        bottomTooltipArrow.visibility = VISIBLE
        val param = bottomTooltipArrow.layoutParams as LinearLayout.LayoutParams
        param.gravity = Gravity.END
        bottomTooltipArrow.layoutParams = param
        textViewForMessage.setBackgroundColor(ResourceUtil.getThemedColor(context!!, R.attr.paper_color))
        textViewForMessage.elevation = ELEVATION_4
        textViewForMessage.setPadding(PADDING_16, PADDING_16, PADDING_16, PADDING_16)
        textViewForMessage.text = getString(R.string.suggested_edits_edit_quality_stat_tooltip, 3)
        executeAfterTimer(false)
    }

    private fun executeAfterTimer(isTopTooltip: Boolean) {
        Timer("TooltipTimer", false).schedule(5000) {
            requireActivity().runOnUiThread(java.lang.Runnable {
                if (isTopTooltip) {
                    tooltipLayout.visibility = GONE
                } else {
                    bottomTooltipArrow.visibility = GONE
                    textViewForMessage.background = null
                    textViewForMessage.setPadding(0, 0, 0, 0)
                    textViewForMessage.elevation = 0.0f
                    updateUserMessageTextView()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
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
    }

    private fun fetchUserContributions() {
        if (!AccountUtil.isLoggedIn()) {
            return
        }

        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).editorTaskCounts
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    swipeRefreshLayout.isRefreshing = false
                    checkForDisabledStatus(100)
                    getPageViews(listofRequiredWikiSites)
                }
                .subscribe({ response ->
                    if (response.query()!!.userInfo()!!.isBlocked) showDisabledView(-1, R.string.suggested_edits_paused_message)

                    val editorTaskCounts = response.query()!!.editorTaskCounts()!!

                    //Adding all the languages that the user has contributed to, to help us calculate pageviews of all pages that the user has contributed to in all the wikis
                    response.query()!!.editorTaskCounts()!!.captionEditsPerLanguage.forEach { (key, _) -> listofRequiredWikiSites.add(WikiSite.forLanguageCode(key)) }

                    totalEdits = 0
                    for (count in editorTaskCounts.descriptionEditsPerLanguage.values) {
                        totalEdits += count
                    }
                    for (count in editorTaskCounts.captionEditsPerLanguage.values) {
                        totalEdits += count
                    }
                    updateUserMessageTextView()
                }, { throwable ->
                    L.e(throwable)
                    FeedbackUtil.showError(requireActivity(), throwable)
                }))

    }

    private fun getPageViews(listofSites: ArrayList<WikiSite>) {
        listofSites.add(WikiSite(Service.COMMONS_URL))
        listofSites.add(WikiSite(Service.WIKIDATA_URL))
        val observableList = ArrayList<io.reactivex.Observable<MwQueryResponse>>()
        for (wikiSite in listofSites) {
            observableList.add(getPageViewsPerWiki(wikiSite))
        }
        disposables.add(io.reactivex.Observable.zip(observableList) { args -> createResult(args) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pageViewsCount ->
                    pageViewStatsView.setTitle(pageViewsCount.toString())
                }, L::e))
    }

    private fun createResult(pageViewsResultsList: Array<Any>): Int {
        var totalPageViews = 0

        for (result in pageViewsResultsList) {
            if (result is MwQueryResponse && result.query() != null) {
                if (result.query()!!.pages() != null) {
                    for (page in result.query()!!.pages()!!) {
                        if (page.pageViewsMap != null) {
                            page.pageViewsMap!!.forEach { (_, value) -> if (value != null) totalPageViews = totalPageViews + value.toInt() }
                        }
                    }
                }
            }
        }
        return totalPageViews
    }

    fun getPageViewsPerWiki(wikiSite: WikiSite): io.reactivex.Observable<MwQueryResponse> {
        return ServiceFactory.get(wikiSite).getUserContributedPages(AccountUtil.getUserName()!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    val titles = ArrayList<String>()
                    for (usercont in response.query()!!.userContribs()!!) {
                        titles.add(usercont.title!!)
                    }
                    ServiceFactory.get(wikiSite).getPageViewsForTitles(StringUtils.join(titles, "|"))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }
    }

    private fun updateUI() {
        requireActivity().invalidateOptionsMenu()
        fetchUserContributions()
    }

    private fun updateUserMessageTextView() {
        if (totalEdits == 0) {
            contributionsStatsView.visibility = GONE
            editQualityStatsView.visibility = GONE
            editStreakStatsView.visibility = GONE
            pageViewStatsView.visibility = GONE
            onboardingImageView.visibility = VISIBLE
            textViewForMessage.text = getString(R.string.suggested_edits_onboarding_message, AccountUtil.getUserName())
        } else {
            contributionsStatsView.visibility = VISIBLE
            editQualityStatsView.visibility = VISIBLE
            editStreakStatsView.visibility = VISIBLE
            pageViewStatsView.visibility = VISIBLE
            onboardingImageView.visibility = GONE
            contributionsStatsView.setTitle(totalEdits.toString())
            textViewForMessage.text = getString(R.string.suggested_edits_encouragement_message, AccountUtil.getUserName())
        }
    }

    private fun checkForDisabledStatus(editQuality: Int) {
        when (editQuality) {
            in 0..10 -> showDisabledView(R.drawable.ic_suggested_edits_paused, R.string.suggested_edits_paused_message)
            in 11..50 -> showDisabledView(R.drawable.ic_suggested_edits_disabled, R.string.suggested_edits_disabled_message)
            -1 -> showDisabledView(-1, R.string.suggested_edits_ip_blocked_message)
            else -> disabledStatesView.visibility = GONE
        }

    }

    private fun showDisabledView(@DrawableRes drawableRes: Int, @StringRes stringRes: Int) {
        if (drawableRes == -1) {
            disabledStatesView.hideImage()
            disabledStatesView.hideHelpLink()
        } else {
            disabledStatesView.unhideImage()
            disabledStatesView.unhideHelpLink()
            disabledStatesView.setImage(drawableRes)
        }
        disabledStatesView.visibility = VISIBLE
        disabledStatesView.setMessageText(stringRes)
    }

    fun dummyButtons() {
        paused.setOnClickListener { checkForDisabledStatus(8) }
        disabled.setOnClickListener { checkForDisabledStatus(45) }
        ipBlocked.setOnClickListener { checkForDisabledStatus(-1) }
    }

    private fun setUpTasks() {
        displayedTasks.clear()
        addImageCaptionsTask = SuggestedEditsTask()
        addImageCaptionsTask.title = getString(R.string.suggested_edits_image_captions)
        addImageCaptionsTask.description = getString(R.string.suggested_edits_image_captions_task_detail)
        addImageCaptionsTask.imageDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_icon_caption_images)
        displayedTasks.add(addImageCaptionsTask)

        addDescriptionsTask = SuggestedEditsTask()
        addDescriptionsTask.title = getString(R.string.description_edit_tutorial_title_descriptions)
        addDescriptionsTask.description = getString(R.string.suggested_edits_add_descriptions_task_detail)
        addDescriptionsTask.imageDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_article_description)
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
