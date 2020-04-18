package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.suggestededits.SuggestedEditsContributionsActivity.Companion.ARG_CONTRIBUTIONS_LIST
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultViewHolder

private val disposables = CompositeDisposable()


class SuggestedEditsContributionsFragment : Fragment() {
    private val adapter: ContributionsEntryItemAdapter = ContributionsEntryItemAdapter()
    private var list: MutableList<Any> = ArrayList()
    var contributionObjects = ArrayList<ContributionObject>()
    var imageContributionTitles = ArrayList<String>()
    var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contributionObjects = GsonUnmarshaller.unmarshal(object : TypeToken<java.util.ArrayList<ContributionObject>>() {}, requireActivity().intent.getStringExtra(ARG_CONTRIBUTIONS_LIST))

        for (contributionObject in contributionObjects) {

            disposables.add(ServiceFactory.getRest(contributionObject.wikiSite).getSummary(null, contributionObject.title)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ summary -> createArticleContributionObjects(contributionObject, summary) }) { t: Throwable? -> L.e(t) })
        }
        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserImageContributions(AccountUtil.getUserName()!!, 10)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    for (userContribution in response.query()!!.userContributions()) {
                        imageContributionTitles.add(userContribution.title)
                    }
                    getImageInfo()
                }) { t: Throwable? -> L.e(t) })
    }

    private fun getImageInfo() {
        var imageCount = 0
        for (title in imageContributionTitles) {
            disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(title, "en")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->

                        val page = response.query()!!.pages()!![0]
                        if (page.imageInfo() != null) {
                            val imageInfo = page.imageInfo()!!
                            val contributionObject = ContributionObject(title, imageInfo.metadata!!.imageDescription(), getString(R.string.suggested_edits_contributions_type, getString(R.string.description_edit_add_caption_hint), "en")
                                    , imageInfo.originalUrl, imageInfo.timestamp, WikiSite.forLanguageCode("en"))
                            list.add(contributionObject)
                            if (++imageCount == imageContributionTitles.size) {
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }, { caught ->
                        L.e(caught)
                    }))

        }
    }

    private fun createArticleContributionObjects(contributionObject: ContributionObject, summary: PageSummary) {
        contributionObject.description = StringUtils.defaultString(summary.description)
        contributionObject.imageUrl = summary.thumbnailUrl.toString()
        list.add(contributionObject)
        if (++count == contributionObjects.size) {
            adapter.notifyDataSetChanged()
            count = 0
        }
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
        list.add("Today")
        adapter.setList(list)
        addImageContributions()
    }

    private fun addImageContributions() {

    }

    companion object {
        fun newInstance(): SuggestedEditsContributionsFragment {
            return SuggestedEditsContributionsFragment()
        }
    }

    private class HeaderViewHolder internal constructor(itemView: View) : DefaultViewHolder<View?>(itemView) {
        var headerText: TextView = itemView.findViewById(R.id.section_header_text)
        fun bindItem(date: String) {
            headerText.text = date
            headerText.setTextColor(ResourceUtil.getThemedColor(headerText.context, R.attr.colorAccent))
        }
    }

    private class ContributionItemHolder internal constructor(itemView: SuggestedEditsContributionsItemView<ContributionObject>) : DefaultViewHolder<SuggestedEditsContributionsItemView<ContributionObject>?>(itemView!!) {
        fun bindItem(contributionObject: ContributionObject) {
            view.setItem(contributionObject)
            view.setTime(contributionObject.timeStamp)
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

        fun clearList() {
            contributionsList.clear()
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
                holder.getView().setCallback(ItemCallback())
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is ContributionItemHolder) {
                holder.getView().setCallback(null)
            }
            super.onViewDetachedFromWindow(holder)
        }

        companion object {
            private const val VIEW_TYPE_HEADER = 0
            private const val VIEW_TYPE_ITEM = 1
        }
    }

    class ContributionObject internal constructor(val title: String, var description: String, val editTypeText: String, var imageUrl: String, val timeStamp: String, val wikiSite: WikiSite)

    private class ItemCallback : SuggestedEditsContributionsItemView.Callback<ContributionObject> {

        override fun onClick() {
        }
    }
}
