package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_contributions_suggested_edits.*
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.DefaultViewHolder
import java.util.*

class SuggestedEditsContributionsFragment : Fragment() {
    private val adapter: ContributionsEntryItemAdapter = ContributionsEntryItemAdapter()
    private var list: MutableList<Any> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        list.add(IndexedHistoryEntry("Prismatic Surface", "surface by all the lines that are parallel to a given line", "Article description (en)", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Dorothy-wordsworth.jpg/320px-Dorothy-wordsworth.jpg"))
        list.add(IndexedHistoryEntry("Prismatic Surface", "surface by all the lines that are parallel to a given line", "Article description (en)", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Dorothy-wordsworth.jpg/320px-Dorothy-wordsworth.jpg"))
        adapter.setList(list)
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

    private class ContributionItemHolder internal constructor(itemView: SuggestedEditsContributionsItemView<IndexedHistoryEntry>) : DefaultViewHolder<SuggestedEditsContributionsItemView<IndexedHistoryEntry>?>(itemView!!) {
        fun bindItem(indexedEntry: IndexedHistoryEntry) {
            view.setItem(indexedEntry)
            view.setTime("16:02")
            view.setTitle(indexedEntry.title)
            view.setDescription(indexedEntry.description)
            view.setImageUrl(indexedEntry.imageUrl)
            view.setTagType(indexedEntry.editTypeText)
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
                holder.bindItem((contributionsList[pos] as IndexedHistoryEntry))
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

    private class IndexedHistoryEntry internal constructor(val title: String, val description: String, val editTypeText: String, val imageUrl: String) {
    }

    private class ItemCallback : SuggestedEditsContributionsItemView.Callback<IndexedHistoryEntry> {

        override fun onClick() {
        }
    }
}
