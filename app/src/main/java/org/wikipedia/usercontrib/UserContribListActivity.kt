package org.wikipedia.usercontrib

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
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
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityUserContribBinding
import org.wikipedia.databinding.ViewEditHistoryEmptyMessagesBinding
import org.wikipedia.databinding.ViewEditHistorySearchBarBinding
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.views.SearchAndFilterActionProvider
import org.wikipedia.views.WikiErrorView

class UserContribListActivity : BaseActivity() {

    private lateinit var binding: ActivityUserContribBinding
    private val editHistoryListAdapter = EditHistoryListAdapter()
    private val editHistoryStatsAdapter = StatsItemAdapter()
    private val editHistorySearchBarAdapter = SearchBarAdapter()
    private val editHistoryEmptyMessagesAdapter = EmptyMessagesAdapter()
    private val loadHeader = LoadingItemAdapter { editHistoryListAdapter.retry() }
    private val loadFooter = LoadingItemAdapter { editHistoryListAdapter.retry() }
    private val viewModel: UserContribListViewModel by viewModels { UserContribListViewModel.Factory(intent.extras!!) }
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserContribBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        binding.titleView.isVisible = false
        binding.titleView.text = getString(R.string.user_contrib_activity_title, StringUtil.fromHtml(viewModel.userName))

        binding.editHistoryRefreshContainer.setOnRefreshListener {
            viewModel.clearCache()
            editHistoryListAdapter.reload()
        }

        binding.editHistoryRecycler.layoutManager = LinearLayoutManager(this)
        setupAdapters()
        binding.editHistoryRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                binding.titleView.isVisible = binding.editHistoryRecycler.computeVerticalScrollOffset() > recyclerView.getChildAt(0).height
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
                val showEmpty = (it.append is LoadState.NotLoading && it.source.refresh is LoadState.NotLoading && editHistoryListAdapter.itemCount == 0)
                if (showEmpty) {
                    (binding.editHistoryRecycler.adapter as ConcatAdapter).addAdapter(editHistoryEmptyMessagesAdapter)
                } else {
                    (binding.editHistoryRecycler.adapter as ConcatAdapter).removeAdapter(editHistoryEmptyMessagesAdapter)
                }
            }
        }

        viewModel.editHistoryStatsData.observe(this) {
            if (it is Resource.Success) {
                editHistoryStatsAdapter.notifyItemChanged(0)
                editHistorySearchBarAdapter.notifyItemChanged(0)
            }
        }

        lifecycleScope.launch {
            viewModel.userContribFlow.collectLatest {
                editHistoryListAdapter.submitData(it)
            }
        }

        if (viewModel.actionModeActive) {
            startSearchActionMode()
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

    private fun startSearchActionMode() {
        actionMode = startSupportActionMode(searchActionModeCallback)
    }

    fun showFilterOverflowMenu() {
        val editCountsValue = viewModel.editHistoryStatsData.value
        if (editCountsValue is Resource.Success) {
            val anchorView = if (actionMode != null && searchActionModeCallback.searchAndFilterActionProvider != null)
                searchActionModeCallback.searchBarFilterIcon!! else if (editHistorySearchBarAdapter.viewHolder != null)
                    editHistorySearchBarAdapter.viewHolder!!.binding.filterByButton else binding.root
            UserContribFilterOverflowView(this@UserContribListActivity).show(anchorView, editCountsValue.data) {
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
            return StatsViewHolder(UserContribStatsView(this@UserContribListActivity))
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

    private inner class EditHistoryDiffCallback : DiffUtil.ItemCallback<UserContribListViewModel.UserContribItemModel>() {
        override fun areContentsTheSame(oldItem: UserContribListViewModel.UserContribItemModel, newItem: UserContribListViewModel.UserContribItemModel): Boolean {
            if (oldItem is UserContribListViewModel.UserContribSeparator && newItem is UserContribListViewModel.UserContribSeparator) {
                return oldItem.date == newItem.date
            } else if (oldItem is UserContribListViewModel.UserContribItem && newItem is UserContribListViewModel.UserContribItem) {
                return oldItem.item.revid == newItem.item.revid
            }
            return false
        }

        override fun areItemsTheSame(oldItem: UserContribListViewModel.UserContribItemModel, newItem: UserContribListViewModel.UserContribItemModel): Boolean {
            return oldItem == newItem
        }
    }

    private inner class EditHistoryListAdapter :
            PagingDataAdapter<UserContribListViewModel.UserContribItemModel, RecyclerView.ViewHolder>(EditHistoryDiffCallback()) {

        fun reload() {
            submitData(lifecycle, PagingData.empty())
            viewModel.userContribSource?.invalidate()
        }

        override fun getItemViewType(position: Int): Int {
            return if (getItem(position) is UserContribListViewModel.UserContribSeparator) {
                VIEW_TYPE_SEPARATOR
            } else {
                VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_SEPARATOR) {
                SeparatorViewHolder(layoutInflater.inflate(R.layout.item_edit_history_separator, parent, false))
            } else {
                EditHistoryListItemHolder(UserContribItemView(this@UserContribListActivity))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            if (holder is SeparatorViewHolder) {
                holder.bindItem((item as UserContribListViewModel.UserContribSeparator).date)
            } else if (holder is EditHistoryListItemHolder) {
                holder.bindItem((item as UserContribListViewModel.UserContribItem).item)
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
                errorView.setError(loadState.error)
            }
        }
    }

    private inner class StatsViewHolder constructor(private val view: UserContribStatsView) : RecyclerView.ViewHolder(view) {
        fun bindItem() {
            val statsFlowValue = viewModel.editHistoryStatsData.value
            if (statsFlowValue is Resource.Success) {
                view.setup(viewModel.userName, statsFlowValue.data)
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
            val statsFlowValue = viewModel.editHistoryStatsData.value
            if (statsFlowValue is Resource.Success) {
                binding.root.setCardBackgroundColor(
                    ResourceUtil.getThemedColor(this@UserContribListActivity, R.attr.color_group_22)
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
                    ResourceUtil.getThemedColorStateList(this@UserContribListActivity, R.attr.color_group_9))
            } else {
                binding.filterCount.visibility = View.VISIBLE
                binding.filterCount.text = (if (Prefs.editHistoryFilterType.isNotEmpty()) 1 else 0).toString()
                ImageViewCompat.setImageTintList(binding.filterByButton,
                    ResourceUtil.getThemedColorStateList(this@UserContribListActivity, R.attr.colorAccent))
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

    private inner class EditHistoryListItemHolder constructor(private val view: UserContribItemView) : RecyclerView.ViewHolder(view), UserContribItemView.Listener {
        private lateinit var contrib: UserContribution

        fun bindItem(contrib: UserContribution) {
            this.contrib = contrib
            view.setContents(contrib, viewModel.currentQuery)
            view.listener = this
        }

        override fun onClick() {
            startActivity(ArticleEditDetailsActivity.newIntent(this@UserContribListActivity,
                    PageTitle(contrib.title, viewModel.wikiSite), contrib.revid))
        }

        override fun onLongClick() { }
    }

    private inner class SearchCallback : SearchActionModeCallback() {

        var searchAndFilterActionProvider: SearchAndFilterActionProvider? = null
        val searchBarFilterIcon get() = searchAndFilterActionProvider?.filterIcon

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchAndFilterActionProvider = SearchAndFilterActionProvider(this@UserContribListActivity, searchHintString,
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
            return this@UserContribListActivity
        }

        fun updateFilterIconAndText() {
            searchAndFilterActionProvider?.updateFilterIconAndText()
        }
    }

    companion object {

        private const val VIEW_TYPE_SEPARATOR = 0
        private const val VIEW_TYPE_ITEM = 1
        const val INTENT_EXTRA_USER_NAME = "userName"

        fun newIntent(context: Context, userName: String): Intent {
            return Intent(context, UserContribListActivity::class.java)
                .putExtra(INTENT_EXTRA_USER_NAME, userName)
        }
    }
}
