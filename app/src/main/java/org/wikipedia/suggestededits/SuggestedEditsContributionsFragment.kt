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
import com.google.gson.reflect.TypeToken
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
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.suggestededits.SuggestedEditsContributionsActivity.Companion.ARG_CONTRIBUTIONS_CONTINUE
import org.wikipedia.suggestededits.SuggestedEditsContributionsActivity.Companion.ARG_CONTRIBUTIONS_LIST
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_ARTICLE_DESCRIPTION
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_IMAGE_TAG
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultViewHolder
import java.util.*
import kotlin.collections.ArrayList


class SuggestedEditsContributionsFragment : Fragment() {
    private val adapter: ContributionsEntryItemAdapter = ContributionsEntryItemAdapter()
    private var articleContributions = ArrayList<Contribution>()
    private var imageContributions = HashSet<Contribution>()
    private var articleAndImageContributions = ArrayList<Contribution>()
    private var consolidatedContributionsWithDates: MutableList<Any> = ArrayList()

    private var continuedArticlesContributions = ArrayList<Contribution>()
    private val continuedImageContributions = HashSet<Contribution>()
    private val disposables = CompositeDisposable()

    private var articleContributionsContinuation: String? = null
    private var imageContributionsContinuation: String? = null
    var loadingMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        continuedArticlesContributions = GsonUnmarshaller.unmarshal(object : TypeToken<java.util.ArrayList<Contribution>>() {}, requireActivity().intent.getStringExtra(ARG_CONTRIBUTIONS_LIST))
        articleContributionsContinuation = requireActivity().intent.getStringExtra(ARG_CONTRIBUTIONS_CONTINUE)!!
        getArticleContributionDetails()
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_contributions_suggested_edits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contributionsRecyclerView.setLayoutManager(LinearLayoutManager(context))
        contributionsRecyclerView.setAdapter(adapter)
        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager?
                if (!loadingMore && !imageContributionsContinuation.isNullOrEmpty() && !articleContributionsContinuation.isNullOrEmpty()) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == consolidatedContributionsWithDates.size - 1) {
                        loadMoreContributions()
                        loadingMore = true
                        loadMoreProgressView.visibility = VISIBLE
                    }
                }
            }

        }
        contributionsRecyclerView.addOnScrollListener(scrollListener)
    }


    companion object {
        fun newInstance(): SuggestedEditsContributionsFragment {
            return SuggestedEditsContributionsFragment()
        }
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
                            getImageContributionDetails()
                        }
                    }
                    .subscribe({ summary ->
                        contributionObject.description = StringUtils.defaultString(summary.description)
                        contributionObject.imageUrl = summary.thumbnailUrl.toString()

                    }) { t: Throwable? -> L.e(t) })
        }
    }

    private fun getImageContributionDetails() {
        val imageContributionsObservable = if (imageContributionsContinuation.isNullOrEmpty()) ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserImageContributions(AccountUtil.getUserName()!!, 10)
        else ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserImageContributionsWithContinuation(AccountUtil.getUserName()!!, 10, imageContributionsContinuation!!)
        disposables.add(imageContributionsObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mwQueryResponse ->
                    continuedImageContributions.clear()
                    var imageCount = 0
                    imageContributionsContinuation = if (mwQueryResponse.continuation().isNullOrEmpty()) "" else mwQueryResponse.continuation()!!["uccontinue"]
                    for (userContribution in mwQueryResponse.query()!!.userContributions()) {
                        val strArr = userContribution.comment.split(" ")
                        var contributionLanguage = "en"
                        var editType: Int = -1

                        for (str in strArr) {
                            if (str.contains("wbsetlabel")) {
                                val descArr = str.split("|")
                                if (descArr.size > 1) {
                                    contributionLanguage = descArr[1]
                                }
                                editType = EDIT_TYPE_IMAGE_CAPTION
                            }
                        }
                        if (editType == -1) {
                            editType = EDIT_TYPE_IMAGE_TAG
                        }
                        continuedImageContributions.add(Contribution("", userContribution.title, "", editType, "", DateUtil.iso8601DateParse(userContribution.timestamp), WikiSite.forLanguageCode(contributionLanguage)))
                    }
                    for (contribution in continuedImageContributions) {
                        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(contribution.title, contribution.wikiSite.languageCode())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doAfterTerminate {
                                    if (++imageCount == continuedImageContributions.size) {
                                        imageContributions.addAll(continuedImageContributions)
                                        createConsolidatedList()
                                    }
                                }
                                .subscribe({ response ->
                                    val page = response.query()!!.pages()!![0]
                                    if (page.imageInfo() != null) {
                                        val imageInfo = page.imageInfo()!!
                                        contribution.description = imageInfo.metadata!!.imageDescription()
                                        contribution.imageUrl = imageInfo.originalUrl
                                    }
                                }, { caught ->
                                    L.e(caught)
                                }))
                    }
                }) { t: Throwable? -> L.e(t) })

    }

    private fun createConsolidatedList() {
        articleAndImageContributions.clear()
        articleAndImageContributions.addAll(articleContributions)
        articleAndImageContributions.addAll(imageContributions)
        articleAndImageContributions.sortWith(Comparator { o2, o1 -> (o1.date.compareTo(o2.date)) })
        consolidatedContributionsWithDates.clear()
        adapter.clearList()
        adapter.notifyDataSetChanged()
        var currentDate = articleAndImageContributions[0].date
        var nextDate: Date
        consolidatedContributionsWithDates.add(if (DateUtils.isSameDay(Calendar.getInstance().time, currentDate)) getString(R.string.view_continue_reading_card_subtitle_today) else DateUtil.getFeedCardDateString(currentDate))
        for (position in 0 until articleAndImageContributions.size) {
            nextDate = articleAndImageContributions[position].date
            if (!DateUtils.isSameDay(nextDate, currentDate)) {
                consolidatedContributionsWithDates.add(DateUtil.getFeedCardDateString(nextDate))
                currentDate = nextDate
            }
            consolidatedContributionsWithDates.add(articleAndImageContributions[position])
        }
        adapter.setList(consolidatedContributionsWithDates)
        loadingMore = false
        loadMoreProgressView.visibility = GONE
    }

    private fun loadMoreContributions() {
        val qLangMap = HashMap<String, HashSet<String>>()

        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributionsWithContinuation(AccountUtil.getUserName()!!, 10, articleContributionsContinuation!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    articleContributionsContinuation = response.continuation()!!["uccontinue"]
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
                        continuedArticlesContributions.add(Contribution(userContribution.title, "", "", EDIT_TYPE_ARTICLE_DESCRIPTION, "", DateUtil.iso8601DateParse(userContribution.timestamp), WikiSite.forLanguageCode(descLang)))

                        qLangMap[userContribution.title]!!.add(descLang)
                    }
                    ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                }
                .subscribe({

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
                }))
    }

    private class HeaderViewHolder internal constructor(itemView: View) : DefaultViewHolder<View?>(itemView) {
        var headerText: TextView = itemView.findViewById(R.id.section_header_text)
        fun bindItem(date: String) {
            headerText.text = date
            headerText.setTextColor(ResourceUtil.getThemedColor(headerText.context, R.attr.colorAccent))
        }
    }

    private class ContributionItemHolder internal constructor(itemView: SuggestedEditsContributionsItemView<Contribution>) : DefaultViewHolder<SuggestedEditsContributionsItemView<Contribution>?>(itemView) {
        fun bindItem(contribution: Contribution) {
            view.setItem(contribution)
            view.setTime(DateUtil.get24HrFormatTimeOnlyString(contribution.date))
            view.setTitle(contribution.title)
            view.setDescription(contribution.description)
            view.setImageUrl(contribution.imageUrl)
            view.setTagType(contribution.editType, contribution.wikiSite.languageCode())
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
                holder.view.setCallback(ItemCallback())
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is ContributionItemHolder) {
                holder.view.setCallback(null)
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

    class Contribution internal constructor(val qNumber: String, var title: String, var description: String, val editType: Int, var imageUrl: String, val date: Date, val wikiSite: WikiSite) {

        override fun hashCode(): Int {
            return title.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other is Contribution) {
                return this.title.equals(other.title)
            }
            return false
        }

        companion object {
            const val EDIT_TYPE_ARTICLE_DESCRIPTION = 0
            const val EDIT_TYPE_IMAGE_CAPTION = 1
            const val EDIT_TYPE_IMAGE_TAG = 2
        }
    }

    private class ItemCallback : SuggestedEditsContributionsItemView.Callback<Contribution> {

        override fun onClick() {
        }
    }
}
