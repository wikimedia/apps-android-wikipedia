package org.wikipedia.page.edithistory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityEditHistoryBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.EditHistoryStatsView

class EditHistoryListActivity : BaseActivity() {

    private lateinit var binding: ActivityEditHistoryBinding
    private lateinit var editHistoryListAdapter: EditHistoryListAdapter
    private lateinit var pageTitle: PageTitle
    private val viewModel: EditHistoryListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
        binding = ActivityEditHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pageTitle = intent.getParcelableExtra(INTENT_EXTRA_PAGE_TITLE)!!
        viewModel.fetchData(pageTitle)
        binding.editHistoryLoadProgress.visibility = View.VISIBLE
        viewModel.editHistoryListData.observe(this) {
            if (it is Resource.Success) {
                binding.editHistoryLoadProgress.visibility = View.INVISIBLE
                setUpRecyclerView(it.data)
            }
        }
    }

    private fun setUpRecyclerView(editHistoryList: List<Any>) {
        editHistoryListAdapter = EditHistoryListAdapter(editHistoryList)
        binding.editHistoryRecycler.adapter = editHistoryListAdapter
        binding.editHistoryRecycler.layoutManager = LinearLayoutManager(this)
    }

    private inner class EditHistoryListAdapter(val editHistoryList: List<Any>) :
        Adapter<ViewHolder>(), OnClickListener {
        var listItems = mutableListOf<Any>()

        init {
            setUpList()
        }

        override fun getItemViewType(position: Int): Int {
            return when {
                listItems[position] is Revision -> VIEW_TYPE_ITEM
                listItems[position] is EditHistoryListViewModel.EditStats -> VIEW_TYPE_STATS
                else -> VIEW_TYPE_SEPARATOR
            }
        }

        override fun getItemCount(): Int {
            return listItems.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(this@EditHistoryListActivity)
            return when (viewType) {
                VIEW_TYPE_SEPARATOR -> {
                    SeparatorViewHolder(inflater.inflate(R.layout.item_edit_history_separator, parent, false))
                }
                VIEW_TYPE_STATS -> {
                    StatsViewHolder(EditHistoryStatsView(this@EditHistoryListActivity))
                }
                else -> {
                    EditHistoryListItemHolder(EditHistoryItemView(this@EditHistoryListActivity))
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            when (holder) {
                is SeparatorViewHolder -> {
                    holder.bindItem(listItems[pos] as String)
                }
                is EditHistoryListItemHolder -> {
                    holder.bindItem(listItems[pos] as Revision)
                    holder.itemView.setOnClickListener(this)
                }
                is StatsViewHolder -> {
                    holder.bindItem(listItems[pos] as EditHistoryListViewModel.EditStats)
                }
            }
            holder.itemView.tag = pos
        }

        fun setUpList() {
            editHistoryList.forEach {
                if (it is Revision) {
                    val dateStr = DateUtil.getMonthOnlyDateString(DateUtil.iso8601DateParse(it.timeStamp))
                    if (!listItems.contains(dateStr)) {
                        listItems.add(dateStr)
                    }
                }
                listItems.add(it)
            }
        }

        override fun onClick(v: View) {
            val item = listItems[v.tag as Int]
            if (item is Revision) {
                startActivity(ArticleEditDetailsActivity.newIntent(this@EditHistoryListActivity, pageTitle.prefixedText, item.revId, pageTitle.wikiSite.languageCode))
            }
        }

        override fun onViewAttachedToWindow(holder: ViewHolder) {
            super.onViewAttachedToWindow(holder)
            if (holder is StatsViewHolder) {
                supportActionBar?.title = ""
            }
        }

        override fun onViewDetachedFromWindow(holder: ViewHolder) {
            super.onViewDetachedFromWindow(holder)
            if (holder is StatsViewHolder) {
                supportActionBar?.title = title
            }
        }
    }

    private inner class StatsViewHolder constructor(itemView: View) : ViewHolder(itemView) {
        fun bindItem(editStats: EditHistoryListViewModel.EditStats) {
            (itemView as EditHistoryStatsView).setup(pageTitle.displayText, editStats)
        }
    }

    private inner class SeparatorViewHolder constructor(itemView: View) :
        ViewHolder(itemView) {
        fun bindItem(listItem: String) {
            itemView.findViewById<TextView>(R.id.date_text).text = listItem
        }
    }

    private inner class EditHistoryListItemHolder constructor(itemView: EditHistoryItemView) :
        DefaultViewHolder<EditHistoryItemView>(itemView) {
        fun bindItem(itemRevision: Revision) {
            view.setContents(itemRevision)
        }
    }

    companion object {

        private const val VIEW_TYPE_SEPARATOR = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_STATS = 2
        const val INTENT_EXTRA_PAGE_TITLE = "pageTitle"

        fun newIntent(context: Context, pageTitle: PageTitle): Intent {
            return Intent(context, EditHistoryListActivity::class.java).putExtra(FilePageActivity.INTENT_EXTRA_PAGE_TITLE, pageTitle)
        }
    }
}
