package org.wikipedia.page

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityEditHistoryBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.page.EditHistoryListViewModel.EditSizeDetails
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L

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
            if (it is Resource.Success) {
                binding.editHistoryLoadProgress.visibility = View.INVISIBLE
                setUpRecyclerView(viewModel.editHistoryListData.value?.data!!)
            }
        }
    }

    private fun setUpRecyclerView(editHistoryList: List<Revision>) {
        editHistoryListAdapter = EditHistoryListAdapter(editHistoryList)
        binding.editHistoryRecycler.adapter = editHistoryListAdapter
        binding.editHistoryRecycler.layoutManager = LinearLayoutManager(this)
    }

    private inner class EditHistoryListAdapter(val editHistoryList: List<Revision>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>(), View.OnClickListener {
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(this@EditHistoryListActivity)
            return if (viewType == VIEW_TYPE_HEADER) {
                HeaderViewHolder(inflater.inflate(R.layout.edit_history_section_header, parent, false))
            } else {
                EditHistoryListItemHolder(inflater.inflate(R.layout.item_edit_history, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            if (holder is HeaderViewHolder) {
                holder.bindItem(listItems[pos] as String)
            } else if (holder is EditHistoryListItemHolder) {
                holder.bindItem(getOldRevision(pos), listItems[pos] as Revision)
                holder.itemView.setOnClickListener(this)
            }
            holder.itemView.tag = pos
        }

        private fun getOldRevision(position: Int): Revision? {
            return if (position < listItems.size - 1) {
                if (listItems[position + 1] is Revision) listItems[position + 1] as Revision
                else listItems[position + 2] as Revision
            } else null
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
                startActivity(ArticleEditDetailsActivity.newIntent(this@EditHistoryListActivity,
                    pageTitle.prefixedText, item.revId, pageTitle.wikiSite.languageCode))
            }
        }
    }

    private inner class HeaderViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        fun bindItem(listItem: String) {
            itemView.findViewById<TextView>(R.id.section_header_text).text = listItem
        }
    }

    private inner class EditHistoryListItemHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        fun bindItem(oldRevision: Revision?, listItem: Revision) {
            CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, msg -> run { L.e(msg) } }) {
                val editSizeDetails: EditSizeDetails = viewModel.fetchEditDetails(pageTitle.wikiSite.languageCode, oldRevision?.revId ?: 0, listItem.revId)
                runOnUiThread {
                    val diffTextView: MaterialButton = itemView.findViewById(R.id.diffText)
                    itemView.findViewById<TextView>(R.id.editHistoryTitle).text = listItem.comment.ifEmpty { editSizeDetails.text }
                    diffTextView.text = String.format(if (editSizeDetails.diffSize != 0) "%+d" else "%d", editSizeDetails.diffSize)
                    if (editSizeDetails.diffSize >= 0) {
                        diffTextView.setTextColor(if (editSizeDetails.diffSize > 0) ContextCompat.getColor(this@EditHistoryListActivity, R.color.green50) else ResourceUtil.getThemedColor(this@EditHistoryListActivity, R.attr.material_theme_secondary_color))
                    } else {
                        diffTextView.setTextColor(ContextCompat.getColor(this@EditHistoryListActivity, R.color.red50))
                    }
                }

            }
            itemView.findViewById<MaterialButton>(R.id.userNameText).text = listItem.user
            itemView.findViewById<TextView>(R.id.editHistoryTimeText).text = DateUtil.getTimeString(DateUtil.iso8601DateParse(listItem.timeStamp))
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
