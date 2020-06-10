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
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_contributions_suggested_edits.*
import kotlinx.android.synthetic.main.fragment_contributions_suggested_edits.swipeRefreshLayout
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

class SuggestedEditsContributionsFragment : Fragment(), SuggestedEditsContributionsHeaderView.Callback {
    private val adapter: ContributionsEntryItemAdapter = ContributionsEntryItemAdapter()

    private var allContributions = ArrayList<Contribution>()
    private var displayedContributions: MutableList<Any> = ArrayList()

    private val disposables = CompositeDisposable()
    private var articleContributionsContinuation: String? = null
    private var imageContributionsContinuation: String? = null

    private var loadingMore = false

    private var editFilterType = ALL_EDIT_TYPES
    private var totalPageViews = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_contributions_suggested_edits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contributionsRecyclerView.layoutManager = LinearLayoutManager(context)
        contributionsRecyclerView.adapter = adapter
        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))

        swipeRefreshLayout.setOnRefreshListener {
            fetchContributions()
        }

        contributionsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager?
                if (!loadingMore && !imageContributionsContinuation.isNullOrEmpty() && !articleContributionsContinuation.isNullOrEmpty()) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == displayedContributions.size - 1) {
                        fetchContributions()
                    }
                }
            }
        })

        disposables.add(SuggestedEditsUserStats.getPageViewsObservable().subscribe {
            totalPageViews = it
            adapter.notifyDataSetChanged()
        })

        fetchContributions()
    }

    override fun onDestroyView() {
        contributionsRecyclerView.adapter = null
        disposables.clear()
        super.onDestroyView()
    }

    override fun onTypeItemClick(editType: Int) {
        editFilterType = editType
        displayedContributions.clear()
        createConsolidatedList()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_DATE = 1
        private const val VIEW_TYPE_ITEM = 2

        fun newInstance(): SuggestedEditsContributionsFragment {
            return SuggestedEditsContributionsFragment()
        }
    }

    private fun fetchContributions() {
        loadingMore = true
        loadMoreProgressView.visibility = VISIBLE

        disposables.add(Observable.zip(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributions(AccountUtil.getUserName()!!, 50, articleContributionsContinuation)
                .subscribeOn(Schedulers.io())
                .flatMap { response ->
                    val wikidataContributions = ArrayList<Contribution>()
                    val qLangMap = HashMap<String, HashSet<String>>()
                    if (!response.continuation().isNullOrEmpty()) {
                        articleContributionsContinuation = response.continuation()!!["uccontinue"]
                    }
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
                        wikidataContributions.add(Contribution(userContribution.title, "", "", EDIT_TYPE_ARTICLE_DESCRIPTION,
                                null, DateUtil.iso8601DateParse(userContribution.timestamp), WikiSite.forLanguageCode(descLang), 0))

                        qLangMap[userContribution.title]!!.add(descLang)
                    }
                    ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
                            .subscribeOn(Schedulers.io())
                            .flatMap { entities ->
                                for (entityKey in entities.entities().keys) {
                                    val entity = entities.entities()[entityKey]!!
                                    for (contribution in wikidataContributions) {
                                        if (contribution.qNumber == entityKey) {
                                            contribution.title = entity.labels()[contribution.wikiSite.languageCode()]!!.value()
                                            contribution.description = entity.descriptions()[contribution.wikiSite.languageCode()]!!.value()
                                        }
                                    }
                                }
                                Observable.just(wikidataContributions)
                            }
                },
                ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserContributions(AccountUtil.getUserName()!!, 50, imageContributionsContinuation)
                        .subscribeOn(Schedulers.io()),
                BiFunction<List<Contribution>, MwQueryResponse, List<Contribution>> { wikidataContributions, commonsResponse ->
                    val contributions = ArrayList<Contribution>()
                    contributions.addAll(wikidataContributions)

                    imageContributionsContinuation = if (commonsResponse.continuation().isNullOrEmpty()) "" else commonsResponse.continuation()!!["uccontinue"]
                    for (userContribution in commonsResponse.query()!!.userContributions()) {
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
                        contributions.add(Contribution("", userContribution.title, "", editType, null,
                                DateUtil.iso8601DateParse(userContribution.timestamp), WikiSite.forLanguageCode(contributionLanguage), 0))
                    }
                    contributions
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    loadingMore = false
                    swipeRefreshLayout.isRefreshing = false
                    loadMoreProgressView.visibility = GONE
                }
                .subscribe({
                    allContributions.addAll(it)
                    createConsolidatedList()
                }, { caught ->
                    L.e(caught)
                    showError(caught)
                }))
    }

    private fun createConsolidatedList() {
        val sortedContributions = ArrayList<Contribution>()
        when (editFilterType) {
            EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                sortedContributions.addAll(allContributions.filter { it.editType == EDIT_TYPE_ARTICLE_DESCRIPTION })
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                sortedContributions.addAll(allContributions.filter { it.editType == EDIT_TYPE_IMAGE_CAPTION })
            }
            EDIT_TYPE_IMAGE_TAG -> {
                sortedContributions.addAll(allContributions.filter { it.editType == EDIT_TYPE_IMAGE_TAG })
            }
            else -> {
                sortedContributions.addAll(allContributions)
            }
        }
        sortedContributions.sortWith(Comparator { o2, o1 -> (o1.date.compareTo(o2.date)) })

        if (!sortedContributions.isNullOrEmpty()) {
            var currentDate = sortedContributions[0].date
            var nextDate: Date
            displayedContributions.add(getCorrectDateString(currentDate))
            for (position in 0 until sortedContributions.size) {
                nextDate = sortedContributions[position].date
                if (!DateUtils.isSameDay(nextDate, currentDate)) {
                    displayedContributions.add(getCorrectDateString(nextDate))
                    currentDate = nextDate
                }
                displayedContributions.add(sortedContributions[position])
            }
        }
        adapter.notifyDataSetChanged()
        contributionsRecyclerView.visibility = VISIBLE
    }

    private fun getCorrectDateString(date: Date): String {
        val yesterday: Calendar = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return when {
            DateUtils.isSameDay(Calendar.getInstance().time, date) -> StringUtils.capitalize(getString(R.string.view_continue_reading_card_subtitle_today))
            DateUtils.isSameDay(yesterday.time, date) -> getString(R.string.suggested_edits_contribution_date_yesterday_text)
            else -> DateUtil.getFeedCardDateString(date)
        }
    }

    private fun showError(t: Throwable) {
        swipeRefreshLayout.isRefreshing = false
        contributionsRecyclerView.visibility = GONE
        errorView.setError(t)
        errorView.visibility = VISIBLE
    }

    private inner class HeaderViewHolder internal constructor(itemView: SuggestedEditsContributionsHeaderView) : DefaultViewHolder<SuggestedEditsContributionsHeaderView?>(itemView) {
        fun bindItem() {
            view.callback = this@SuggestedEditsContributionsFragment
            view.updateFilterViewUI(editFilterType)
            view.updateTotalPageViews(totalPageViews)
        }
    }

    private class DateViewHolder internal constructor(itemView: View) : DefaultViewHolder<View?>(itemView) {
        var headerText: TextView = itemView.findViewById(R.id.section_header_text)
        fun bindItem(date: String) {
            headerText.text = date
            headerText.setTextColor(ResourceUtil.getThemedColor(headerText.context, R.attr.colorAccent))
        }
    }

    private class ContributionItemHolder internal constructor(itemView: SuggestedEditsContributionsItemView) : DefaultViewHolder<SuggestedEditsContributionsItemView?>(itemView) {
        val disposables = CompositeDisposable()
        fun bindItem(contribution: Contribution) {
            view.setTitle(contribution.description)
            view.setDescription(contribution.title)
            view.setIcon(contribution.editType)
            view.setImageUrl(contribution.imageUrl)
            view.setPageViewCountText(contribution.pageViews)
            if (contribution.pageViews == 0L && contribution.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) {
                getPageViews(view, contribution)
            }
            if (contribution.imageUrl == null) {
                getContributionDetails(view, contribution)
            }
        }

        fun clearDisposables() {
            disposables.clear()
        }

        private fun getContributionDetails(itemView: SuggestedEditsContributionsItemView, contribution: Contribution) {
            if (contribution.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) {
                disposables.add(ServiceFactory.getRest(contribution.wikiSite).getSummary(null, contribution.title)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ summary ->
                            contribution.imageUrl = summary.thumbnailUrl.toString()
                            itemView.setImageUrl(contribution.imageUrl)
                        }, { t ->
                            L.e(t)
                        }))
            } else {
                disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(contribution.title, contribution.wikiSite.languageCode())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ response ->
                            val page = response.query()!!.pages()!![0]
                            if (page.imageInfo() != null) {
                                val imageInfo = page.imageInfo()!!
                                contribution.description = imageInfo.metadata!!.imageDescription()
                                contribution.imageUrl = imageInfo.thumbUrl
                                if (contribution.editType == EDIT_TYPE_IMAGE_TAG) {
                                    //
                                }
                                itemView.setImageUrl(contribution.imageUrl)
                                itemView.setTitle(contribution.description)
                            }
                        }, { t ->
                            L.e(t)
                        }))
            }
        }

        private fun getPageViews(view: SuggestedEditsContributionsItemView, contribution: Contribution) {
            if (contribution.editType != EDIT_TYPE_ARTICLE_DESCRIPTION) {
                view.setPageViewCountText(0)
                return
            }
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

    inner class ContributionsEntryItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        override fun getItemCount(): Int {
            return displayedContributions.size + 1
        }

        override fun getItemViewType(position: Int): Int {
            return when {
                position == 0 -> {
                    VIEW_TYPE_HEADER
                }
                displayedContributions[position + 1] is String -> {
                    VIEW_TYPE_DATE
                }
                else -> {
                    VIEW_TYPE_ITEM
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    HeaderViewHolder(SuggestedEditsContributionsHeaderView(parent.context))
                }
                VIEW_TYPE_DATE -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.view_section_header, parent, false)
                    DateViewHolder(view)
                }
                else -> {
                    ContributionItemHolder(SuggestedEditsContributionsItemView(parent.context))
                }
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            when (holder) {
                is HeaderViewHolder -> {
                    holder.bindItem()
                }
                is ContributionItemHolder -> {
                    holder.bindItem((displayedContributions[pos + 1] as Contribution))
                }
                else -> {
                    (holder as DateViewHolder).bindItem((displayedContributions[pos + 1] as String))
                }
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
                holder.clearDisposables()
            }
            super.onViewDetachedFromWindow(holder)
        }
    }

    private class ItemCallback : SuggestedEditsContributionsItemView.Callback {
        override fun onClick() {
        }
    }

}
