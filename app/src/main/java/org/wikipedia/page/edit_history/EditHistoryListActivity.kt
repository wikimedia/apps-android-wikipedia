package org.wikipedia.page.edit_history

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
import org.wikipedia.page.EditHistoryListViewModel
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource.Success
import org.wikipedia.views.DefaultViewHolder

class EditHistoryListActivity : BaseActivity() {

    private lateinit var binding: ActivityEditHistoryBinding
    private lateinit var editHistoryListAdapter: EditHistoryListAdapter
    private lateinit var pageTitle: PageTitle
    private val viewModel: EditHistoryListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pageTitle = intent.getParcelableExtra(INTENT_EXTRA_PAGE_TITLE)!!
        viewModel.fetchData(pageTitle)
        binding.editHistoryLoadProgress.visibility = View.VISIBLE
        viewModel.editHistoryListData.observe(this) {
            if (it is Success) {
                binding.editHistoryLoadProgress.visibility = View.INVISIBLE
                setUpRecyclerView(it.data)
            }
        }
    }

    private fun setUpRecyclerView(editHistoryList: List<Revision>) {
        editHistoryListAdapter = EditHistoryListAdapter(editHistoryList)
        binding.editHistoryRecycler.adapter = editHistoryListAdapter
        binding.editHistoryRecycler.layoutManager = LinearLayoutManager(this)
    }

    private inner class EditHistoryListAdapter(val editHistoryList: List<Revision>) :
        Adapter<ViewHolder>(), OnClickListener {
        var listItems = mutableListOf<Any>()

        init {
            setUpList()
        }

        override fun getItemViewType(position: Int): Int {
            return if (listItems[position] is Revision) VIEW_TYPE_ITEM else VIEW_TYPE_HEADER
        }

        override fun getItemCount(): Int {
            return listItems.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(this@EditHistoryListActivity)
            return if (viewType == VIEW_TYPE_HEADER) {
                HeaderViewHolder(inflater.inflate(R.layout.edit_history_section_header, parent, false))
            } else {
                EditHistoryListItemHolder(EditHistoryItemView(this@EditHistoryListActivity))
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            if (holder is HeaderViewHolder) {
                holder.bindItem(listItems[pos] as String)
            } else if (holder is EditHistoryListItemHolder) {
                holder.bindItem(listItems[pos] as Revision)
                holder.itemView.setOnClickListener(this)
            }
            holder.itemView.tag = pos
        }

        fun setUpList() {
            editHistoryList.forEach {
                val dateStr = DateUtil.getMonthOnlyDateString(DateUtil.iso8601DateParse(it.timeStamp))
                if (!listItems.contains(dateStr)) {
                    listItems.add(dateStr)
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
    }

    private inner class HeaderViewHolder constructor(itemView: View) :
        ViewHolder(itemView) {
        fun bindItem(listItem: String) {
            itemView.findViewById<TextView>(R.id.section_header_text).text = listItem
        }
    }

    private inner class EditHistoryListItemHolder constructor(itemView: EditHistoryItemView) :
        DefaultViewHolder<EditHistoryItemView>(itemView) {
        fun bindItem(itemRevision: Revision) {
            view.setContents(itemRevision)
        }
    }

    companion object {

        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        const val INTENT_EXTRA_PAGE_TITLE = "pageTitle"

        fun newIntent(context: Context, pageTitle: PageTitle): Intent {
            return Intent(context, EditHistoryListActivity::class.java).putExtra(FilePageActivity.INTENT_EXTRA_PAGE_TITLE, pageTitle)
        }
    }
}
