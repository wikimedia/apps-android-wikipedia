package org.wikipedia.page.edithistory

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.ColorUtils
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityEditHistoryBinding
import org.wikipedia.databinding.ViewEditHistoryEmptyMessagesBinding
import org.wikipedia.databinding.ViewEditHistorySearchBarBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.talk.UserTalkPopupHelper
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.EditHistoryFilterOverflowView
import org.wikipedia.views.EditHistoryStatsView
import org.wikipedia.views.SearchAndFilterActionProvider
import org.wikipedia.views.WikiErrorView

class EditHistoryListActivity : BaseActivity() {

    private lateinit var binding: ActivityEditHistoryBinding
    private val editHistoryListAdapter = EditHistoryListAdapter()
    private val editHistoryStatsAdapter = StatsItemAdapter()
    private val editHistorySearchBarAdapter = SearchBarAdapter()
    private val editHistoryEmptyMessagesAdapter = EmptyMessagesAdapter()
    private val loadHeader = LoadingItemAdapter { editHistoryListAdapter.retry() }
    private val loadFooter = LoadingItemAdapter { editHistoryListAdapter.retry() }
    private val viewModel: EditHistoryListViewModel by viewModels { EditHistoryListViewModel.Factory(intent.extras!!) }
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.articleTitleView.isVisible = false
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

        binding.compareConfirmButton.setOnClickListener {
            if (viewModel.selectedRevisionFrom != null && viewModel.selectedRevisionTo != null) {
                startActivity(ArticleEditDetailsActivity.newIntent(this@EditHistoryListActivity,
                        viewModel.pageTitle, viewModel.selectedRevisionFrom!!.revId,
                        viewModel.selectedRevisionTo!!.revId))
            }
        }

        binding.editHistoryRefreshContainer.setOnRefreshListener {
            viewModel.clearCache()
            editHistoryListAdapter.reload()
        }

        binding.editHistoryRecycler.layoutManager = LinearLayoutManager(this)
        setupAdapters()
        binding.editHistoryRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                binding.articleTitleView.isVisible = binding.editHistoryRecycler.computeVerticalScrollOffset() > recyclerView.getChildAt(0).height
            }
        })

        lifecycleScope.launchWhenCreated {
            editHistoryListAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                    .filter { it.refresh is LoadState.NotLoading }
                    .collectLatest {
                        if (binding.editHistoryRefreshContainer.isRefreshing) {
                            binding.editHistoryRefreshContainer.isRefreshing = false
                        }
                    }
        }

        lifecycleScope.launchWhenCreated {
            editHistoryListAdapter.loadStateFlow.collectLatest {
                loadHeader.loadState = it.refresh
                loadFooter.loadState = it.append
                enableCompareButton(binding.compareButton, editHistoryListAdapter.itemCount > 2)
                val showEmpty = (it.append is LoadState.NotLoading && it.source.refresh is LoadState.NotLoading && editHistoryListAdapter.itemCount == 0)
                if (showEmpty) {
                    (binding.editHistoryRecycler.adapter as ConcatAdapter).addAdapter(editHistoryEmptyMessagesAdapter)
                } else {
                    (binding.editHistoryRecycler.adapter as ConcatAdapter).removeAdapter(editHistoryEmptyMessagesAdapter)
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.editHistoryStatsFlow.collectLatest {
                editHistoryStatsAdapter.notifyItemChanged(0)
                editHistorySearchBarAdapter.notifyItemChanged(0)

                // Submit data after showing the stats and search bar.
                lifecycleScope.launch {
                    viewModel.editHistoryFlow.collectLatest {
                        editHistoryListAdapter.submitData(it)
                    }
                }
            }
        }

        if (viewModel.actionModeActive) {
            startSearchActionMode()
        }
    }

    private fun updateCompareState() {
        binding.compareContainer.isVisible = viewModel.comparing
        binding.compareButton.text = getString(if (!viewModel.comparing) R.string.revision_compare_button else android.R.string.cancel)
        editHistoryListAdapter.notifyItemRangeChanged(0, editHistoryListAdapter.itemCount)
        setNavigationBarColor(ResourceUtil.getThemedColor(this, if (viewModel.comparing) android.R.attr.colorBackground else R.attr.paper_color))
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
        enableCompareButton(binding.compareConfirmButton, viewModel.selectedRevisionFrom != null && viewModel.selectedRevisionTo != null)
    }

    private fun enableCompareButton(button: TextView, enable: Boolean) {
        if (enable) {
            button.isEnabled = true
            button.setTextColor(ResourceUtil.getThemedColor(this, R.attr.colorAccent))
        } else {
            button.isEnabled = false
            button.setTextColor(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color))
        }
    }

    private fun setupAdapters() {
        if (actionMode != null) {
            binding.editHistoryRecycler.adapter = editHistoryListAdapter.withLoadStateFooter(loadFooter)
        } else {
            binding.editHistoryRecycler.adapter =
                editHistoryListAdapter.withLoadStateHeaderAndFooter(loadHeader, loadFooter).also {
                    it.addAdapter(0, editHistoryStatsAdapter)
                    it.addAdapter(1, editHistorySearchBarAdapter)
                }
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

    private fun startSearchActionMode() {
        actionMode = startSupportActionMode(searchActionModeCallback)
    }

    fun showFilterOverflowMenu() {
        val editCountsFlowValue = viewModel.editHistoryStatsFlow.value
        if (editCountsFlowValue is EditHistoryListViewModel.EditHistoryStats) {
            val anchorView = if (actionMode != null && searchActionModeCallback.searchAndFilterActionProvider != null)
                searchActionModeCallback.searchBarFilterIcon!! else if (editHistorySearchBarAdapter.viewHolder != null)
                    editHistorySearchBarAdapter.viewHolder!!.binding.filterByButton else binding.root
            EditHistoryFilterOverflowView(this@EditHistoryListActivity).show(anchorView, editCountsFlowValue) {
                setupAdapters()
                editHistoryListAdapter.reload()
                editHistorySearchBarAdapter.notifyItemChanged(0)
                actionMode?.let {
                    searchActionModeCallback.updateFilterIconAndText()
                }
            }
        }
    }

    private inner class SearchBarAdapter : RecyclerView.Adapter<SearchBarViewHolder>() {
        var viewHolder: SearchBarViewHolder? = null
        override fun onBindViewHolder(holder: SearchBarViewHolder, position: Int) {
            holder.bindItem()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchBarViewHolder {
            viewHolder = SearchBarViewHolder(ViewEditHistorySearchBarBinding.inflate(layoutInflater, parent, false))
            return viewHolder!!
        }

        override fun getItemCount(): Int { return 1 }
    }

    private inner class EmptyMessagesAdapter : RecyclerView.Adapter<EmptyMessagesViewHolder>() {
        override fun onBindViewHolder(holder: EmptyMessagesViewHolder, position: Int) {
            holder.bindItem()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmptyMessagesViewHolder {
            return EmptyMessagesViewHolder(ViewEditHistoryEmptyMessagesBinding.inflate(layoutInflater, parent, false))
        }

        override fun getItemCount(): Int { return 1 }
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
        override fun areContentsTheSame(oldItem: EditHistoryListViewModel.EditHistoryItemModel, newItem: EditHistoryListViewModel.EditHistoryItemModel): Boolean {
            if (oldItem is EditHistoryListViewModel.EditHistorySeparator && newItem is EditHistoryListViewModel.EditHistorySeparator) {
                return oldItem.date == newItem.date
            } else if (oldItem is EditHistoryListViewModel.EditHistoryItem && newItem is EditHistoryListViewModel.EditHistoryItem) {
                return oldItem.item.revId == newItem.item.revId
            }
            return false
        }

        override fun areItemsTheSame(oldItem: EditHistoryListViewModel.EditHistoryItemModel, newItem: EditHistoryListViewModel.EditHistoryItemModel): Boolean {
            return oldItem == newItem
        }
    }

    private inner class EditHistoryListAdapter :
            PagingDataAdapter<EditHistoryListViewModel.EditHistoryItemModel, RecyclerView.ViewHolder>(EditHistoryDiffCallback()) {

        fun reload() {
            submitData(lifecycle, PagingData.empty())
            viewModel.editHistorySource?.invalidate()
        }

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

    private inner class SeparatorViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(listItem: String) {
            val dateText = itemView.findViewById<TextView>(R.id.date_text)
            dateText.text = listItem
        }
    }

    private inner class SearchBarViewHolder constructor(val binding: ViewEditHistorySearchBarBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.isVisible = false
            updateFilterCount()
        }

        fun bindItem() {
            val editCountsFlowValue = viewModel.editHistoryStatsFlow.value
            if (editCountsFlowValue is EditHistoryListViewModel.EditHistoryStats) {
                binding.root.setCardBackgroundColor(
                    ResourceUtil.getThemedColor(this@EditHistoryListActivity, R.attr.color_group_22)
                )

                itemView.setOnClickListener {
                    startSearchActionMode()
                }

                binding.filterByButton.setOnClickListener {
                    showFilterOverflowMenu()
                }

                FeedbackUtil.setButtonLongPressToast(binding.filterByButton)
                binding.root.isVisible = true
            }
        }

        private fun updateFilterCount() {
            if (Prefs.editHistoryFilterType.isEmpty()) {
                binding.filterCount.visibility = View.GONE
                ImageViewCompat.setImageTintList(binding.filterByButton,
                    ColorStateList.valueOf(ResourceUtil.getThemedColor(this@EditHistoryListActivity, R.attr.chip_text_color)))
            } else {
                binding.filterCount.visibility = View.VISIBLE
                binding.filterCount.text = (if (Prefs.editHistoryFilterType.isNotEmpty()) 1 else 0).toString()
                ImageViewCompat.setImageTintList(binding.filterByButton,
                    ColorStateList.valueOf(ResourceUtil.getThemedColor(this@EditHistoryListActivity, R.attr.colorAccent)))
            }
        }
    }

    private inner class EmptyMessagesViewHolder constructor(val binding: ViewEditHistoryEmptyMessagesBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.emptySearchMessage.movementMethod = LinkMovementMethodExt { _ ->
                showFilterOverflowMenu()
            }
        }

        fun bindItem() {
            binding.emptySearchMessage.text = StringUtil.fromHtml(getString(R.string.page_edit_history_empty_search_message))
            RichTextUtil.removeUnderlinesFromLinks(binding.emptySearchMessage)
            binding.searchEmptyText.isVisible = actionMode != null
            binding.searchEmptyContainer.isVisible = Prefs.editHistoryFilterType.isNotEmpty()
        }
    }

    private inner class EditHistoryListItemHolder constructor(private val view: EditHistoryItemView) : RecyclerView.ViewHolder(view), EditHistoryItemView.Listener {
        private lateinit var revision: MwQueryPage.Revision

        fun bindItem(revision: MwQueryPage.Revision) {
            this.revision = revision
            view.setContents(revision, viewModel.currentQuery)
            updateSelectState()
            view.listener = this
        }

        override fun onClick() {
            if (viewModel.comparing) {
                toggleSelectState()
            } else {
                startActivity(ArticleEditDetailsActivity.newIntent(this@EditHistoryListActivity,
                        viewModel.pageTitle, revision.revId))
            }
        }

        override fun onLongClick() {
            if (!viewModel.comparing) {
                viewModel.toggleCompareState()
                updateCompareState()
            }
            toggleSelectState()
        }

        override fun onUserNameClick(v: View) {
            if (viewModel.comparing) {
                toggleSelectState()
            } else {
                UserTalkPopupHelper.show(this@EditHistoryListActivity, bottomSheetPresenter,
                        PageTitle(UserAliasData.valueFor(viewModel.pageTitle.wikiSite.languageCode),
                                revision.user, viewModel.pageTitle.wikiSite), revision.isAnon, v,
                        Constants.InvokeSource.DIFF_ACTIVITY, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS)
            }
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

    private inner class SearchCallback : SearchActionModeCallback() {

        var searchAndFilterActionProvider: SearchAndFilterActionProvider? = null
        val searchBarFilterIcon get() = searchAndFilterActionProvider?.filterIcon

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchAndFilterActionProvider = SearchAndFilterActionProvider(this@EditHistoryListActivity, searchHintString,
                object : SearchAndFilterActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }

                    override fun onFilterIconClick() {
                        showFilterOverflowMenu()
                    }

                    override fun getExcludedFilterCount(): Int {
                        return if (Prefs.editHistoryFilterType.isNotEmpty()) 1 else 0
                    }

                    override fun getFilterIconContentDescription(): Int {
                        return R.string.page_edit_history_filter_by
                    }
                })

            val menuItem = menu.add(searchHintString)

            MenuItemCompat.setActionProvider(menuItem, searchAndFilterActionProvider)

            actionMode = mode
            searchAndFilterActionProvider?.setQueryText(viewModel.currentQuery)
            setupAdapters()
            viewModel.actionModeActive = true
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.currentQuery = s
            setupAdapters()
            editHistoryListAdapter.reload()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.currentQuery = ""
            editHistoryListAdapter.reload()
            viewModel.actionModeActive = false
            setupAdapters()
        }

        override fun getSearchHintString(): String {
            return getString(R.string.page_edit_history_search_or_filter_edits_hint)
        }

        override fun getParentContext(): Context {
            return this@EditHistoryListActivity
        }

        fun updateFilterIconAndText() {
            searchAndFilterActionProvider?.updateFilterIconAndText()
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
