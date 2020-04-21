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
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.suggestededits.SuggestedEditsContributionsActivity.Companion.ARG_CONTRIBUTIONS_CONTINUE
import org.wikipedia.suggestededits.SuggestedEditsContributionsActivity.Companion.ARG_CONTRIBUTIONS_LIST
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultViewHolder
import java.util.*
import kotlin.collections.ArrayList

private val disposables = CompositeDisposable()


class SuggestedEditsContributionsFragment : Fragment() {
    private val adapter: ContributionsEntryItemAdapter = ContributionsEntryItemAdapter()
    private var contributionsWithDatesList: MutableList<Any> = ArrayList()
    private var contributionsList = ArrayList<ContributionObject>()
    var imageContributionTitles = HashMap<String, String>()
    private var userContributionsContinuation: String? = null
    private var userImageContributionsContinuation: String? = null
    var loadingMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contributionsList = GsonUnmarshaller.unmarshal(object : TypeToken<java.util.ArrayList<ContributionObject>>() {}, requireActivity().intent.getStringExtra(ARG_CONTRIBUTIONS_LIST))
        userContributionsContinuation = requireActivity().intent.getStringExtra(ARG_CONTRIBUTIONS_CONTINUE)!!
        getArticleAndImageInfoForContributions()
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
                if (!loadingMore && !userImageContributionsContinuation.isNullOrEmpty() && !userContributionsContinuation.isNullOrEmpty()) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == contributionsWithDatesList.size - 1) {
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

    private fun getArticleAndImageInfoForContributions() {
        for (contributionObject in contributionsList) {
            disposables.add(ServiceFactory.getRest(contributionObject.wikiSite).getSummary(null, contributionObject.title)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ summary -> createArticleContributionObjects(contributionObject, summary) }) { t: Throwable? -> L.e(t) })
        }
        if (userImageContributionsContinuation == null) {
            disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserImageContributions(AccountUtil.getUserName()!!, 10)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        getImageInfo(response)
                    }) { t: Throwable? -> L.e(t) })
        } else {
            disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserImageContributionsWithContinuation(AccountUtil.getUserName()!!, 10, userImageContributionsContinuation!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        getImageInfo(response)
                    }) { t: Throwable? -> L.e(t) })
        }
    }

    private fun getImageInfo(mwQueryResponse: MwQueryResponse) {
        var imageCount = 0
        userImageContributionsContinuation = if (mwQueryResponse.continuation().isNullOrEmpty()) "" else mwQueryResponse.continuation()!!["uccontinue"]
        for (userContribution in mwQueryResponse.query()!!.userContributions()) {
            val strArr = userContribution.comment.split(" ")
            var contributionLanguage = "en"
            for (str in strArr) {
                if (str.contains("wbsetlabel")) {
                    val descArr = str.split("|")
                    if (descArr.size > 1) {
                        contributionLanguage = descArr[1]
                    }
                }
            }
            imageContributionTitles[userContribution.title] = contributionLanguage
        }
        for (title in imageContributionTitles.keys) {
            disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(title, imageContributionTitles[title]!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->

                        val page = response.query()!!.pages()!![0]
                        if (page.imageInfo() != null) {
                            val imageInfo = page.imageInfo()!!
                            val contributionObject = ContributionObject(title, imageInfo.metadata!!.imageDescription(), getString(R.string.suggested_edits_contributions_type, getString(R.string.description_edit_add_caption_hint), "en")
                                    , imageInfo.originalUrl, DateUtil.iso8601DateParse(imageInfo.timestamp), WikiSite.forLanguageCode(imageContributionTitles[title]!!))
                            contributionsList.add(contributionObject)

                            if (++imageCount == imageContributionTitles.size) {
                                contributionsList.sortWith(Comparator { o2, o1 -> (o1.date.compareTo(o2.date)) })
                                contributionsWithDatesList.clear()
                                var oldDate = contributionsList[0].date
                                var newDate: Date
                                contributionsWithDatesList.add(if (DateUtils.isSameDay(Calendar.getInstance().time, oldDate)) getString(R.string.view_continue_reading_card_subtitle_today) else DateUtil.getFeedCardDateString(oldDate))
                                for (position in 0 until contributionsList.size) {
                                    newDate = contributionsList[position].date
                                    if (!DateUtils.isSameDay(newDate, oldDate)) {
                                        contributionsWithDatesList.add(DateUtil.getFeedCardDateString(newDate))
                                        oldDate = newDate
                                    }
                                    contributionsWithDatesList.add(contributionsList[position])
                                }
                                adapter.setList(contributionsWithDatesList)
                                loadingMore = false
                                loadMoreProgressView.visibility = GONE
                            }
                        }
                    }, { caught ->
                        L.e(caught)
                    }))

        }
    }

    private fun loadMoreContributions() {
        val qLangMap = HashMap<String, HashSet<String>>()
        val timestamps = ArrayList<String>()

        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributionsWithContinuation(AccountUtil.getUserName()!!, 10, userContributionsContinuation!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    userContributionsContinuation = response.continuation()!!["uccontinue"]
                    for (userContribution in response.query()!!.userContributions()) {
                        timestamps.add(userContribution.timestamp)
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
                .subscribe({

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

                    var i = 0
                    for (lang in langArticleMap.keys) {
                        val site = WikiSite.forLanguageCode(lang)
                        for (title in langArticleMap[lang]!!) {
                            val contributionObject = ContributionObject(title, "", getString(R.string.suggested_edits_contributions_type, getString(R.string.description_edit_text_hint), lang), "", DateUtil.iso8601DateParse(timestamps[i++]), site)
                            contributionsList.add(contributionObject)
                        }
                    }
                    getArticleAndImageInfoForContributions()
                }, { t ->
                    L.e(t)
                }))
    }

    private fun createArticleContributionObjects(contributionObject: ContributionObject, summary: PageSummary) {
        contributionObject.description = StringUtils.defaultString(summary.description)
        contributionObject.imageUrl = summary.thumbnailUrl.toString()
    }

    private class HeaderViewHolder internal constructor(itemView: View) : DefaultViewHolder<View?>(itemView) {
        var headerText: TextView = itemView.findViewById(R.id.section_header_text)
        fun bindItem(date: String) {
            headerText.text = date
            headerText.setTextColor(ResourceUtil.getThemedColor(headerText.context, R.attr.colorAccent))
        }
    }

    private class ContributionItemHolder internal constructor(itemView: SuggestedEditsContributionsItemView<ContributionObject>) : DefaultViewHolder<SuggestedEditsContributionsItemView<ContributionObject>?>(itemView) {
        fun bindItem(contributionObject: ContributionObject) {
            view.setItem(contributionObject)
            view.setTime(DateUtil.get24HrFormatTimeOnlyString(contributionObject.date))
            view.setTitle(contributionObject.title)
            view.setDescription(contributionObject.description)
            view.setImageUrl(contributionObject.imageUrl)
            view.setTagType(contributionObject.editTypeText)
        }

    }

    private class ContributionsEntryItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        private var contributionsList: MutableList<Any> = ArrayList()
        override fun getItemCount(): Int {
            return contributionsList.size
        }

        val isEmpty: Boolean
            get() = itemCount == 0

        override fun getItemViewType(position: Int): Int {
            return if (contributionsList[position] is String) {
                VIEW_TYPE_HEADER
            } else {
                VIEW_TYPE_ITEM
            }
        }

        fun setList(list: MutableList<Any>) {
            contributionsList = list
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
                holder.bindItem((contributionsList[pos] as ContributionObject))
            } else {
                (holder as HeaderViewHolder).bindItem((contributionsList[pos] as String))
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

    class ContributionObject internal constructor(val title: String, var description: String, val editTypeText: String, var imageUrl: String, val date: Date, val wikiSite: WikiSite)

    private class ItemCallback : SuggestedEditsContributionsItemView.Callback<ContributionObject> {

        override fun onClick() {
        }
    }
}
