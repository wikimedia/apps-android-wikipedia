package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_contributions_suggested_edits.*
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateUtils
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.suggestededits.Contribution.Companion.ALL_EDIT_TYPES
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_ARTICLE_DESCRIPTION
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_TAG
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultViewHolder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class SuggestedEditsContributionsFragment : Fragment(), SuggestedEditsTypeItem.Callback {
    private val adapter: ContributionsEntryItemAdapter = ContributionsEntryItemAdapter()
    private var articleContributions = ArrayList<Contribution>()
    private var imageContributions = HashSet<Contribution>()
    private var articleAndImageContributions = ArrayList<Contribution>()
    private var consolidatedContributionsWithDates: MutableList<Any> = ArrayList()
    private var continuedArticlesContributions = ArrayList<Contribution>()
    private val continuedImageContributions = HashSet<Contribution>()
    val disposables = CompositeDisposable()
    private var articleContributionsContinuation: String? = null
    private var imageContributionsContinuation: String? = null
    private var loadingMore = false
    private var editFilterType = ALL_EDIT_TYPES
    private var filterViews = ArrayList<SuggestedEditsTypeItem>()
    private val pageViewsMap = HashMap<String, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadContentOfEditFilterType()
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_contributions_suggested_edits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMoreProgressView.visibility = VISIBLE
        contributionsRecyclerView.layoutManager = LinearLayoutManager(context)
        contributionsRecyclerView.adapter = adapter
        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        swipeRefreshLayout.setOnRefreshListener {
            if (!loadingMore) {
                loadContentOfEditFilterType()
            }
        }
        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager?
                if (!loadingMore && !imageContributionsContinuation.isNullOrEmpty() && !articleContributionsContinuation.isNullOrEmpty()) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == consolidatedContributionsWithDates.size - 1) {
                        loadContentOfEditFilterType()
                        loadingMore = true
                        loadMoreProgressView.visibility = VISIBLE
                    }
                }
            }
        }
        contributionsRecyclerView.addOnScrollListener(scrollListener)
        filterViews.add(allTypesView)
        filterViews.add(articleDescriptionView)
        filterViews.add(imageCaptionsView)
        filterViews.add(imageTagsView)
        allTypesView.setAttributes(getString(R.string.suggested_edits_spinner_item_text, SuggestedEditsUserStats.totalEdits, resources.getQuantityString(R.plurals.suggested_edits_contribution, 25)), R.drawable.ic_mode_edit_themed_24dp, ALL_EDIT_TYPES, this)
        articleDescriptionView.setAttributes(getString(R.string.suggested_edits_spinner_item_text, SuggestedEditsUserStats.totalDescriptionEdits, getString(R.string.description_edit_tutorial_title_descriptions)), R.drawable.ic_article_description, EDIT_TYPE_ARTICLE_DESCRIPTION, this)
        imageCaptionsView.setAttributes(getString(R.string.suggested_edits_spinner_item_text, SuggestedEditsUserStats.totalImageCaptionEdits, getString(R.string.suggested_edits_image_captions)), R.drawable.ic_image_caption, EDIT_TYPE_IMAGE_CAPTION, this)
        imageTagsView.setAttributes(getString(R.string.suggested_edits_spinner_item_text, SuggestedEditsUserStats.totalImageTagEdits, getString(R.string.suggested_edits_image_tags)), R.drawable.ic_image_tag, EDIT_TYPE_IMAGE_TAG, this)
        disposables.add(SuggestedEditsUserStats.getPageViewsObservable().subscribe {
            contributionsSeenText.text = getString(R.string.suggested_edits_contribution_seen_text, it.toString())
        })
    }

    private fun setFilterAndUIState(view: SuggestedEditsTypeItem) {
        editFilterType = view.editType
        createConsolidatedList()
    }

    companion object {
        fun newInstance(): SuggestedEditsContributionsFragment {
            return SuggestedEditsContributionsFragment()
        }
    }

    private fun loadContentOfEditFilterType() {
        loadingMore = true
        if (editFilterType == ALL_EDIT_TYPES || editFilterType == EDIT_TYPE_ARTICLE_DESCRIPTION) {
            getArticleContributions()
        } else {
            getImageContributions()
        }
    }

    private fun getArticleContributions() {
        val qLangMap = HashMap<String, HashSet<String>>()
        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributions(AccountUtil.getUserName()!!, 50, articleContributionsContinuation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    if (!response.continuation().isNullOrEmpty()) {
                        articleContributionsContinuation = response.continuation()!!["uccontinue"]
                    }
                    continuedArticlesContributions.clear()
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
                        continuedArticlesContributions.add(Contribution(userContribution.title, "", "", EDIT_TYPE_ARTICLE_DESCRIPTION,
                                "", DateUtil.iso8601DateParse(userContribution.timestamp), WikiSite.forLanguageCode(descLang), 0, 0))

                        qLangMap[userContribution.title]!!.add(descLang)
                    }
                    ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }.subscribe({
                    for (entityKey in it.entities().keys) {
                        val entity = it.entities()[entityKey]!!
                        for (contribution in continuedArticlesContributions) {
                            if (contribution.qNumber == entityKey) {
                                contribution.title = entity.labels()[contribution.wikiSite.languageCode()]!!.value()
                                contribution.description = entity.descriptions()[contribution.wikiSite.languageCode()]!!.value()
                            }
                        }
                    }
                    getArticleContributionDetails()
                }, { t ->
                    L.e(t)
                    showError(t)
                }))
    }

    private fun getArticleContributionDetails() {
        var count = 0
        for (contributionObject in continuedArticlesContributions) {
            disposables.add(ServiceFactory.getRest(contributionObject.wikiSite).getSummary(null, contributionObject.title)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally {
                        if (++count == continuedArticlesContributions.size) {
                            articleContributions.addAll(continuedArticlesContributions)
                            if (editFilterType == EDIT_TYPE_ARTICLE_DESCRIPTION) {
                                createConsolidatedList()
                            } else {
                                getImageContributions()
                            }
                        }
                    }
                    .subscribe({ summary ->
                        contributionObject.description = StringUtils.defaultString(summary.description)
                        contributionObject.imageUrl = summary.thumbnailUrl.toString()
                        contributionObject.pageViews = pageViewsMap[contributionObject.title] ?: 0
                    }) { t: Throwable? ->
                        L.e(t)
                    })
        }
    }

    private fun getImageContributions() {
        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserContributions(AccountUtil.getUserName()!!, 50, imageContributionsContinuation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mwQueryResponse ->
                    continuedImageContributions.clear()
                    var imageCount = 0
                    imageContributionsContinuation = if (mwQueryResponse.continuation().isNullOrEmpty()) "" else mwQueryResponse.continuation()!!["uccontinue"]
                    for (userContribution in mwQueryResponse.query()!!.userContributions()) {
                        val strArr = userContribution.comment.split(" ")
                        var contributionLanguage = ""
                        var editType: Int = -1

                        for (str in strArr) {
                            if (str.contains("wbsetlabel")) {
                                val descArr = str.split("|")
                                if (descArr.size > 1) {
                                    contributionLanguage = descArr[1]
                                }
                                editType = EDIT_TYPE_IMAGE_CAPTION
                            } else if (str.contains("wbsetclaim")) {
                                editType = EDIT_TYPE_IMAGE_TAG
                            }
                        }
                        val con = Contribution("", userContribution.title, "", editType, "", DateUtil.iso8601DateParse(userContribution.timestamp),
                                WikiSite.forLanguageCode(contributionLanguage), 0, if (editType == EDIT_TYPE_IMAGE_TAG) 1 else 0)
                        if (!continuedImageContributions.add(con)) {
                            continuedImageContributions.elementAt(continuedImageContributions.indexOf(con)).tagCount++
                        }
                    }
                    for (contribution in continuedImageContributions) {
                        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(contribution.title, contribution.wikiSite.languageCode())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doAfterTerminate {
                                    if (++imageCount == continuedImageContributions.size) {
                                        for (continuedImageContribution in continuedImageContributions) {
                                            if (!imageContributions.add(continuedImageContribution)) {
                                                continuedImageContributions.elementAt(continuedImageContributions.indexOf(continuedImageContribution)).tagCount++
                                            }
                                        }
                                        createConsolidatedList()
                                    }
                                }
                                .subscribe({ response ->
                                    val page = response.query()!!.pages()!![0]
                                    if (page.imageInfo() != null) {
                                        val imageInfo = page.imageInfo()!!
                                        contribution.description = imageInfo.metadata!!.imageDescription()
                                        contribution.imageUrl = imageInfo.originalUrl
                                        contribution.title = contribution.title.replace("File:", "")
                                        if (contribution.editType == EDIT_TYPE_IMAGE_TAG) {
                                            if (!page.imageLabels.isNullOrEmpty()) {
                                                var labelsString = ""
                                                for (imageLabel in page.imageLabels) {
                                                    labelsString = imageLabel.label + "," + labelsString
                                                }
                                                contribution.description = labelsString
                                            }
                                        }
                                    }
                                }, { caught ->
                                    L.e(caught)
                                }))
                    }
                }) { t: Throwable? ->
                    L.e(t)
                    showError(t!!)
                })
    }

    private fun createConsolidatedList() {
        articleAndImageContributions.clear()
        loadDataBasedOnFilter()
        articleAndImageContributions.sortWith(Comparator { o2, o1 -> (o1.date.compareTo(o2.date)) })
        consolidatedContributionsWithDates.clear()
        adapter.clearList()
        adapter.notifyDataSetChanged()
        if (!articleAndImageContributions.isNullOrEmpty()) {
            var currentDate = articleAndImageContributions[0].date
            var nextDate: Date
            consolidatedContributionsWithDates.add(getCorrectDateString(currentDate))
            for (position in 0 until articleAndImageContributions.size) {
                nextDate = articleAndImageContributions[position].date
                if (!DateUtils.isSameDay(nextDate, currentDate)) {
                    consolidatedContributionsWithDates.add(getCorrectDateString(nextDate))
                    currentDate = nextDate
                }
                consolidatedContributionsWithDates.add(articleAndImageContributions[position])
            }
            adapter.setList(consolidatedContributionsWithDates)
        }
        loadingMore = false
        swipeRefreshLayout.isRefreshing = false
        loadMoreProgressView.visibility = GONE
        contentContainer.visibility = VISIBLE
        updateFilterViewUI()
    }

    private fun getCorrectDateString(date: Date): String {
        val yesterday: Calendar = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return when {
            DateUtils.isSameDay(Calendar.getInstance().time, date) -> getString(R.string.view_continue_reading_card_subtitle_today).capitalize()
            DateUtils.isSameDay(yesterday.time, date) -> getString(R.string.suggested_edits_date_string_yesterday)
            else -> DateUtil.getFeedCardDateString(date)
        }
    }

    private fun showError(t: Throwable) {
        swipeRefreshLayout.isRefreshing = false
        contentContainer.visibility = GONE
        errorView.setError(t)
        errorView.visibility = VISIBLE
    }

    private fun updateFilterViewUI() {
        val view: SuggestedEditsTypeItem
        val count: Int
        when (editFilterType) {
            EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                view = articleDescriptionView
                count = SuggestedEditsUserStats.totalDescriptionEdits
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                view = imageCaptionsView
                count = SuggestedEditsUserStats.totalImageCaptionEdits
            }
            EDIT_TYPE_IMAGE_TAG -> {
                view = imageTagsView
                count = SuggestedEditsUserStats.totalImageTagEdits
            }
            else -> {
                view = allTypesView
                count = SuggestedEditsUserStats.totalEdits
            }
        }

        contributionsCountText.text = getString(R.string.suggested_edits_spinner_item_text, count, resources.getQuantityString(R.plurals.suggested_edits_contribution, count))
        for (filterView in filterViews) {
            if (filterView == view) {
                filterView.setEnabledStateUI()
            } else {
                filterView.setDisabledStateUI()
            }
        }
    }

    private fun loadDataBasedOnFilter() {
        when (editFilterType) {
            EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                articleAndImageContributions.addAll(articleContributions)
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                for (imageContribution in imageContributions) {
                    if (imageContribution.editType == EDIT_TYPE_IMAGE_CAPTION) {
                        articleAndImageContributions.add(imageContribution)
                    }
                }
            }
            EDIT_TYPE_IMAGE_TAG -> {
                for (imageContribution in imageContributions) {
                    if (imageContribution.editType == EDIT_TYPE_IMAGE_TAG) {
                        articleAndImageContributions.add(imageContribution)
                    }
                }
            }
            else -> {
                articleAndImageContributions.addAll(articleContributions)
                articleAndImageContributions.addAll(imageContributions)
            }
        }
    }

    private class HeaderViewHolder internal constructor(itemView: View) : DefaultViewHolder<View?>(itemView) {
        var headerText: TextView = itemView.findViewById(R.id.section_header_text)
        fun bindItem(date: String) {
            headerText.text = date
            headerText.setTextColor(ResourceUtil.getThemedColor(headerText.context, R.attr.colorAccent))
        }
    }

    private class ContributionItemHolder internal constructor(itemView: SuggestedEditsContributionsItemView) : DefaultViewHolder<SuggestedEditsContributionsItemView?>(itemView) {
        fun bindItem(contribution: Contribution) {
            view.setTitle(contribution.description)
            view.setDescription(contribution.title)
            view.setImageUrl(contribution.imageUrl)
            view.setIcon(contribution.editType)
            view.setPageViewCountText(contribution.pageViews)
            getPageView(view, contribution)
        }

        private fun getPageView(view: SuggestedEditsContributionsItemView, contribution: Contribution) {
            val disposables = CompositeDisposable()

            disposables.add(ServiceFactory.get(contribution.wikiSite).getPageViewsForTitles(contribution.title)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        if (response is MwQueryResponse) {
                            var pageviews = 0L
                            for (page in response.query()!!.pages()!!) {
                                for (day in page.pageViewsMap.values) {
                                    pageviews += day ?: 0
                                }
                            }
                            contribution.pageViews = pageviews
                            view.setPageViewCountText(contribution.pageViews)
                        }
                    }) { t: Throwable? -> L.e(t) })
        }
    }

    private class ContributionsEntryItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        private var mutableList: MutableList<Any> = ArrayList()
        override fun getItemCount(): Int {
            return mutableList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (mutableList[position] is String) {
                VIEW_TYPE_HEADER
            } else {
                VIEW_TYPE_ITEM
            }
        }

        fun clearList() {
            mutableList.clear()
        }

        fun setList(list: MutableList<Any>) {
            mutableList.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return if (viewType == VIEW_TYPE_HEADER) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.view_section_header, parent, false)
                HeaderViewHolder(view)
            } else {
                ContributionItemHolder(SuggestedEditsContributionsItemView(parent.context))
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            if (holder is ContributionItemHolder) {
                holder.bindItem((mutableList[pos] as Contribution))
            } else {
                (holder as HeaderViewHolder).bindItem((mutableList[pos] as String))
            }
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            if (holder is ContributionItemHolder) {
                holder.view.callback = ItemCallback()
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is ContributionItemHolder) {
                holder.view.callback = null
            }
            super.onViewDetachedFromWindow(holder)
        }

        companion object {
            private const val VIEW_TYPE_HEADER = 0
            private const val VIEW_TYPE_ITEM = 1
        }
    }

    override fun onDestroy() {
        if (contributionsRecyclerView != null) {
            contributionsRecyclerView.adapter = null
        }
        adapter.clearList()
        articleAndImageContributions.clear()
        consolidatedContributionsWithDates.clear()
        articleContributions.clear()
        imageContributions.clear()
        disposables.clear()
        super.onDestroy()
    }

    private class ItemCallback : SuggestedEditsContributionsItemView.Callback {
        override fun onClick() {
        }
    }

    override fun onClick(view: SuggestedEditsTypeItem) {
        setFilterAndUIState(view)
    }
}
