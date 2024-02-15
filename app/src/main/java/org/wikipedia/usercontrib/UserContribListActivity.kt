package org.wikipedia.usercontrib

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import org.wikipedia.databinding.ActivityUserContribBinding
import org.wikipedia.databinding.ViewEditHistoryEmptyMessagesBinding
import org.wikipedia.databinding.ViewEditHistorySearchBarBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.talk.UserTalkPopupHelper
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.SearchAndFilterActionProvider
import org.wikipedia.views.WikiErrorView

class UserContribListActivity : BaseActivity() {

    private lateinit var binding: ActivityUserContribBinding
    private lateinit var linkHandler: UserContribLinkHandler
    private val userContribListAdapter = UserContribListAdapter()
    private val userContribStatsAdapter = StatsItemAdapter()
    private val userContribSearchBarAdapter = SearchBarAdapter()
    private val userContribEmptyMessagesAdapter = EmptyMessagesAdapter()
    private val loadHeader = LoadingItemAdapter { userContribListAdapter.retry() }
    private val loadFooter = LoadingItemAdapter { userContribListAdapter.retry() }
    private val viewModel: UserContribListViewModel by viewModels { UserContribListViewModel.Factory(intent.extras!!) }
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()

    private val linkMovementMethod = LinkMovementMethodExt { url, title, linkText, x, y ->
        linkHandler.onUrlClick(url, title, linkText, x, y)
    }

    private val launchFilterActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            viewModel.langCode = Prefs.userContribFilterLangCode
            viewModel.loadStats()
            setupAdapters()
            viewModel.clearCache()
            userContribListAdapter.reload()
            userContribSearchBarAdapter.notifyItemChanged(0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserContribBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.title = getString(R.string.user_contrib_activity_title, StringUtil.fromHtml(viewModel.userName))

        linkHandler = UserContribLinkHandler(this)
        linkHandler.wikiSite = viewModel.wikiSite

        binding.refreshContainer.setOnRefreshListener {
            viewModel.clearCache()
            userContribListAdapter.reload()
        }

        binding.userContribRecycler.layoutManager = LinearLayoutManager(this)
        setupAdapters()
        binding.userContribRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                supportActionBar?.setDisplayShowTitleEnabled(binding.userContribRecycler.computeVerticalScrollOffset() > recyclerView.getChildAt(0).height)
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    userContribListAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                        .filter { it.refresh is LoadState.NotLoading }
                        .collectLatest {
                            if (binding.refreshContainer.isRefreshing) {
                                binding.refreshContainer.isRefreshing = false
                            }
                        }
                }
                launch {
                    userContribListAdapter.loadStateFlow.collectLatest {
                        loadHeader.loadState = it.refresh
                        loadFooter.loadState = it.append
                        val showEmpty = (it.append is LoadState.NotLoading && it.source.refresh is LoadState.NotLoading && userContribListAdapter.itemCount == 0)
                        if (showEmpty) {
                            (binding.userContribRecycler.adapter as ConcatAdapter).addAdapter(userContribEmptyMessagesAdapter)
                        } else {
                            (binding.userContribRecycler.adapter as ConcatAdapter).removeAdapter(userContribEmptyMessagesAdapter)
                        }
                    }
                }
                launch {
                    viewModel.userContribFlow.collectLatest {
                        userContribListAdapter.submitData(it)
                    }
                }
            }
        }

        viewModel.userContribStatsData.observe(this) {
            if (it is Resource.Success) {
                userContribStatsAdapter.notifyItemChanged(0)
                userContribSearchBarAdapter.notifyItemChanged(0)
            }
        }

        if (viewModel.actionModeActive) {
            startSearchActionMode()
        }
    }

    private fun setupAdapters() {
        if (actionMode != null) {
            binding.userContribRecycler.adapter = userContribListAdapter.withLoadStateFooter(loadFooter)
        } else {
            binding.userContribRecycler.adapter =
                    userContribListAdapter.withLoadStateHeaderAndFooter(loadHeader, loadFooter).also {
                    it.addAdapter(0, userContribStatsAdapter)
                    it.addAdapter(1, userContribSearchBarAdapter)
                }
        }
    }

    private fun startSearchActionMode() {
        actionMode = startSupportActionMode(searchActionModeCallback)
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

    private inner class UserContribDiffCallback : DiffUtil.ItemCallback<UserContribListViewModel.UserContribItemModel>() {
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

    private inner class UserContribListAdapter :
            PagingDataAdapter<UserContribListViewModel.UserContribItemModel, RecyclerView.ViewHolder>(UserContribDiffCallback()) {

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
                UserContribListItemHolder(UserContribItemView(this@UserContribListActivity))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            if (holder is SeparatorViewHolder) {
                holder.bindItem((item as UserContribListViewModel.UserContribSeparator).date)
            } else if (holder is UserContribListItemHolder) {
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
            val statsFlowValue = viewModel.userContribStatsData.value
            if (statsFlowValue is Resource.Success) {
                view.setup(viewModel.userName, statsFlowValue.data, linkMovementMethod, UriUtil.getUserPageTitle(viewModel.userName, viewModel.langCode))
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
            val statsFlowValue = viewModel.userContribStatsData.value
            if (statsFlowValue is Resource.Success) {
                binding.root.setCardBackgroundColor(
                    ResourceUtil.getThemedColor(this@UserContribListActivity, R.attr.background_color)
                )

                itemView.setOnClickListener {
                    startSearchActionMode()
                }

                binding.filterByButton.setOnClickListener {
                    launchFilterActivity.launch(UserContribFilterActivity.newIntent(this@UserContribListActivity))
                }

                FeedbackUtil.setButtonLongPressToast(binding.filterByButton)
                binding.root.isVisible = true
            }
        }

        private fun updateFilterCount() {
            val showFilterCount = viewModel.excludedFiltersCount() != 0
            val filterButtonColor = if (showFilterCount) R.attr.progressive_color else R.attr.primary_color
            binding.filterCount.isVisible = showFilterCount
            binding.filterCount.text = viewModel.excludedFiltersCount().toString()
            ImageViewCompat.setImageTintList(binding.filterByButton,
                ResourceUtil.getThemedColorStateList(this@UserContribListActivity, filterButtonColor))
        }
    }

    private inner class EmptyMessagesViewHolder constructor(val binding: ViewEditHistoryEmptyMessagesBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.emptySearchMessage.movementMethod = LinkMovementMethodExt { _ ->
                launchFilterActivity.launch(UserContribFilterActivity.newIntent(this@UserContribListActivity))
            }
        }

        fun bindItem() {
            binding.emptySearchMessage.text = StringUtil.fromHtml(getString(R.string.page_edit_history_empty_search_message))
        }
    }

    private inner class UserContribListItemHolder constructor(private val view: UserContribItemView) : RecyclerView.ViewHolder(view), UserContribItemView.Listener {
        private lateinit var contrib: UserContribution

        fun bindItem(contrib: UserContribution) {
            this.contrib = contrib
            view.setContents(contrib, viewModel.currentQuery)
            view.listener = this
        }

        override fun onClick() {
            startActivity(ArticleEditDetailsActivity.newIntent(this@UserContribListActivity,
                    PageTitle(contrib.title, viewModel.wikiSite), contrib.pageid, revisionTo = contrib.revid))
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {

        var searchAndFilterActionProvider: SearchAndFilterActionProvider? = null

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchAndFilterActionProvider = SearchAndFilterActionProvider(this@UserContribListActivity, searchHintString,
                object : SearchAndFilterActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }

                    override fun onFilterIconClick() {
                        launchFilterActivity.launch(UserContribFilterActivity.newIntent(this@UserContribListActivity))
                    }

                    override fun getExcludedFilterCount(): Int {
                        return Prefs.userContribFilterExcludedNs.size
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
            userContribListAdapter.reload()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.currentQuery = ""
            userContribListAdapter.reload()
            viewModel.actionModeActive = false
            setupAdapters()
        }

        override fun getSearchHintString(): String {
            return getString(R.string.page_edit_history_search_or_filter_edits_hint)
        }

        override fun getParentContext(): Context {
            return this@UserContribListActivity
        }
    }

    internal inner class UserContribLinkHandler internal constructor(context: Context) : LinkHandler(context) {
        private var lastX: Int = 0
        private var lastY: Int = 0

        fun onUrlClick(url: String, title: String?, linkText: String, x: Int, y: Int) {
            lastX = x
            lastY = y
            super.onUrlClick(url, title, linkText)
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            // TODO
        }

        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
            // TODO
        }

        override lateinit var wikiSite: WikiSite

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // TODO
        }

        override fun onInternalLinkClicked(title: PageTitle) {
            UserTalkPopupHelper.show(this@UserContribListActivity, title, false, lastX, lastY,
                    Constants.InvokeSource.USER_CONTRIB_ACTIVITY, HistoryEntry.SOURCE_USER_CONTRIB, showContribs = false)
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
