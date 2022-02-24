package org.wikipedia.page.edithistory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityEditHistoryBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.page.PageTitle

class EditHistoryListActivity : BaseActivity() {

    private lateinit var binding: ActivityEditHistoryBinding
    private val editHistoryListAdapter = EditHistoryListAdapter()
    private val viewModel: EditHistoryListViewModel by viewModels { EditHistoryListViewModel.Factory(intent.extras!!) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editHistoryRecycler.layoutManager = LinearLayoutManager(this)
        binding.editHistoryRecycler.adapter = editHistoryListAdapter

        lifecycleScope.launch {
            viewModel.editHistoryFlow.collectLatest {
                editHistoryListAdapter.submitData(it)
            }
        }
    }

    private inner class EditHistoryDiffCallback : DiffUtil.ItemCallback<MwQueryPage.Revision>() {
        override fun areItemsTheSame(oldItem: MwQueryPage.Revision, newItem: MwQueryPage.Revision): Boolean {
            return oldItem.revId == oldItem.revId
        }

        override fun areContentsTheSame(oldItem: MwQueryPage.Revision, newItem: MwQueryPage.Revision): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
    }

    private inner class EditHistoryListAdapter :
            PagingDataAdapter<MwQueryPage.Revision, RecyclerView.ViewHolder>(EditHistoryDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return EditHistoryListItemHolder(EditHistoryItemView(this@EditHistoryListActivity))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            (holder as EditHistoryListItemHolder).bindItem(item as MwQueryPage.Revision)
        }
    }

    private inner class SeparatorViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        fun bindItem(listItem: String) {
            itemView.findViewById<TextView>(R.id.date_text).text = listItem
        }
    }

    private inner class EditHistoryListItemHolder constructor(itemView: EditHistoryItemView) :
            RecyclerView.ViewHolder(itemView), OnClickListener {
        private lateinit var revision: MwQueryPage.Revision

        fun bindItem(revision: MwQueryPage.Revision) {
            (itemView as EditHistoryItemView).setContents(revision)
            itemView.setOnClickListener(this)
            this.revision = revision
        }

        override fun onClick(v: View?) {
            startActivity(ArticleEditDetailsActivity.newIntent(this@EditHistoryListActivity,
                    viewModel.pageTitle.prefixedText, revision.revId, viewModel.pageTitle.wikiSite.languageCode))
        }
    }

    companion object {

        private const val VIEW_TYPE_SEPARATOR = 0
        private const val VIEW_TYPE_ITEM = 1
        const val INTENT_EXTRA_PAGE_TITLE = "pageTitle"

        fun newIntent(context: Context, pageTitle: PageTitle): Intent {
            return Intent(context, EditHistoryListActivity::class.java).putExtra(FilePageActivity.INTENT_EXTRA_PAGE_TITLE, pageTitle)
        }
    }
}
