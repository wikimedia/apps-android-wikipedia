package org.wikipedia.page.edithistory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityEditHistoryBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.EditHistoryStatsView
import org.wikipedia.views.WikiErrorView

class EditHistoryListActivity : BaseActivity() {

    private lateinit var binding: ActivityEditHistoryBinding
    private val editHistoryListAdapter = EditHistoryListAdapter()
    private val editHistoryStatsAdapter = StatsItemAdapter()
    private val loadHeader = LoadingItemAdapter { editHistoryListAdapter.retry() }
    private val loadFooter = LoadingItemAdapter { editHistoryListAdapter.retry() }
    private val viewModel: EditHistoryListViewModel by viewModels { EditHistoryListViewModel.Factory(intent.extras!!) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.articleTitleView.visibility = View.GONE
        binding.articleTitleView.text = getString(R.string.page_edit_history_activity_title, StringUtil.fromHtml(viewModel.pageTitle.displayText))

        val colorCompareBackground = ResourceUtil.getThemedColor(this, android.R.attr.colorBackground)
        binding.compareFromCard.setCardBackgroundColor(ColorUtils.blendARGB(colorCompareBackground,
                ResourceUtil.getThemedColor(this, R.attr.colorAccent), 0.05f))
        binding.compareToCard.setCardBackgroundColor(ColorUtils.blendARGB(colorCompareBackground,
                ResourceUtil.getThemedColor(this, R.attr.color_group_68), 0.05f))
        updateCompareState()

        binding.compareButton.setOnClickListener {
            viewModel.toggleCompareState()
            updateCompareState()
        }

        binding.editHistoryRefreshContainer.setOnRefreshListener {
            editHistoryListAdapter.refresh()
        }

        binding.editHistoryRecycler.layoutManager = LinearLayoutManager(this)
        binding.editHistoryRecycler.adapter = editHistoryListAdapter
                .withLoadStateHeaderAndFooter(loadHeader, loadFooter).also { it.addAdapter(0, editHistoryStatsAdapter) }
        binding.editHistoryRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val isVisible = if (binding.editHistoryRecycler.computeVerticalScrollOffset() > recyclerView.getChildAt(0).height) View.VISIBLE else View.INVISIBLE
                binding.articleTitleView.visibility = isVisible
            }
        })

        lifecycleScope.launch {
            viewModel.editHistoryFlow.collectLatest {
                editHistoryListAdapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenCreated {
            editHistoryListAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                    .filter { it.refresh is LoadState.NotLoading }
                    .collect {
                        if (binding.editHistoryRefreshContainer.isRefreshing) {
                            binding.editHistoryRefreshContainer.isRefreshing = false
                        }
                    }
        }

        lifecycleScope.launchWhenCreated {
            editHistoryListAdapter.loadStateFlow.collect {
                loadHeader.loadState = it.refresh
                loadFooter.loadState = it.append
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.editHistoryStatsFlow.collectLatest {
                editHistoryStatsAdapter.notifyItemChanged(0)
            }
        }
    }

    private fun updateCompareState() {
        binding.compareContainer.isVisible = viewModel.comparing
        binding.compareButton.text = getString(if (!viewModel.comparing) R.string.revision_compare_button else android.R.string.cancel)
        editHistoryListAdapter.notifyItemRangeChanged(0, editHistoryListAdapter.itemCount)
        updateCompareStateItems()
    }

    private fun updateCompareStateItems() {
        binding.compareFromCard.isVisible = viewModel.selectedRevisionFrom != null
        if (viewModel.selectedRevisionFrom != null) {
            binding.compareFromText.text = DateUtil.getShortDayWithTimeString(DateUtil.iso8601DateParse(viewModel.selectedRevisionFrom!!.timeStamp))
        }
        binding.compareToCard.isVisible = viewModel.selectedRevisionTo != null
        if (viewModel.selectedRevisionTo != null) {
            binding.compareToText.text = DateUtil.getShortDayWithTimeString(DateUtil.iso8601DateParse(viewModel.selectedRevisionTo!!.timeStamp))
        }
        if (viewModel.selectedRevisionFrom != null && viewModel.selectedRevisionTo != null) {
            binding.compareConfirmButton.isEnabled = true
            binding.compareConfirmButton.setTextColor(ResourceUtil.getThemedColor(this, R.attr.colorAccent))
        } else {
            binding.compareConfirmButton.isEnabled = false
            binding.compareConfirmButton.setTextColor(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color))
        }
    }

    override fun onBackPressed() {
        if (viewModel.comparing) {
            viewModel.toggleCompareState()
            updateCompareState()
            return
        }
        super.onBackPressed()
    }

    private inner class StatsItemAdapter : RecyclerView.Adapter<StatsViewHolder>() {
        override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
            holder.bindItem()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
            return StatsViewHolder(EditHistoryStatsView(this@EditHistoryListActivity))
        }

        override fun getItemCount(): Int { return 1 }
    }

    private inner class LoadingItemAdapter(private val retry: () -> Unit) : LoadStateAdapter<LoadingViewHolder>() {
        override fun onBindViewHolder(holder: LoadingViewHolder, loadState: LoadState) {
            holder.bindItem(loadState, retry)
        }

        override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadingViewHolder {
            return LoadingViewHolder(layoutInflater.inflate(R.layout.item_list_progress, parent, false))
        }
    }

    private inner class EditHistoryDiffCallback : DiffUtil.ItemCallback<EditHistoryListViewModel.EditHistoryItemModel>() {
        override fun areItemsTheSame(oldItem: EditHistoryListViewModel.EditHistoryItemModel, newItem: EditHistoryListViewModel.EditHistoryItemModel): Boolean {
            if (oldItem is EditHistoryListViewModel.EditHistorySeparator && newItem is EditHistoryListViewModel.EditHistorySeparator) {
                return oldItem.date == newItem.date
            } else if (oldItem is EditHistoryListViewModel.EditHistoryItem && newItem is EditHistoryListViewModel.EditHistoryItem) {
                return oldItem.item.revId == newItem.item.revId
            }
            return false
        }

        override fun areContentsTheSame(oldItem: EditHistoryListViewModel.EditHistoryItemModel, newItem: EditHistoryListViewModel.EditHistoryItemModel): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
    }

    private inner class EditHistoryListAdapter :
            PagingDataAdapter<EditHistoryListViewModel.EditHistoryItemModel, RecyclerView.ViewHolder>(EditHistoryDiffCallback()) {

        override fun getItemViewType(position: Int): Int {
            return if (getItem(position) is EditHistoryListViewModel.EditHistorySeparator) {
                VIEW_TYPE_SEPARATOR
            } else {
                VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_SEPARATOR) {
                SeparatorViewHolder(layoutInflater.inflate(R.layout.item_edit_history_separator, parent, false))
            } else {
                EditHistoryListItemHolder(EditHistoryItemView(this@EditHistoryListActivity))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            if (holder is SeparatorViewHolder) {
                holder.bindItem((item as EditHistoryListViewModel.EditHistorySeparator).date)
            } else if (holder is EditHistoryListItemHolder) {
                holder.bindItem((item as EditHistoryListViewModel.EditHistoryItem).item)
            }
        }
    }

    private inner class LoadingViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(loadState: LoadState, retry: () -> Unit) {
            val errorView = itemView.findViewById<WikiErrorView>(R.id.errorView)
            val progressBar = itemView.findViewById<View>(R.id.progressBar)
            progressBar.isVisible = loadState is LoadState.Loading
            errorView.isVisible = loadState is LoadState.Error
            errorView.retryClickListener = OnClickListener { retry() }
            if (loadState is LoadState.Error) {
                errorView.setError(loadState.error, viewModel.pageTitle)
            }
        }
    }

    private inner class StatsViewHolder constructor(private val view: EditHistoryStatsView) : RecyclerView.ViewHolder(view) {
        fun bindItem() {
            val statsFlowValue = viewModel.editHistoryStatsFlow.value
            if (statsFlowValue is EditHistoryListViewModel.EditHistoryStats) {
                view.setup(viewModel.pageTitle, statsFlowValue)
            }
        }
    }

    private inner class SeparatorViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        fun bindItem(listItem: String) {
            itemView.findViewById<TextView>(R.id.date_text).text = listItem
        }
    }

    private inner class EditHistoryListItemHolder constructor(private val view: EditHistoryItemView) : RecyclerView.ViewHolder(view), EditHistoryItemView.Listener {
        private lateinit var revision: MwQueryPage.Revision

        fun bindItem(revision: MwQueryPage.Revision) {
            this.revision = revision
            view.setContents(revision)
            updateSelectState()
            view.listener = this
        }

        override fun onClick() {
            startActivity(ArticleEditDetailsActivity.newIntent(this@EditHistoryListActivity,
                    viewModel.pageTitle.prefixedText, revision.revId, viewModel.pageTitle.wikiSite.languageCode))
        }

        override fun onLongClick() {
            if (!viewModel.comparing) {
                viewModel.toggleCompareState()
                updateCompareState()
            }
            toggleSelectState()
        }

        override fun onToggleSelect() {
            toggleSelectState()
        }

        private fun toggleSelectState() {
            if (!viewModel.toggleSelectRevision(revision)) {
                FeedbackUtil.showMessage(this@EditHistoryListActivity, R.string.revision_compare_two_only)
                return
            }
            updateSelectState()
            updateCompareStateItems()
        }

        private fun updateSelectState() {
            view.setSelectedState(viewModel.getSelectedState(revision))
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
