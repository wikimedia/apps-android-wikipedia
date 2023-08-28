package org.wikipedia.suggestededits

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuItemCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentSuggestedEditsRecentEditsBinding
import org.wikipedia.databinding.ViewEditHistoryEmptyMessagesBinding
import org.wikipedia.databinding.ViewEditHistorySearchBarBinding
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.NotificationButtonView
import org.wikipedia.views.SearchAndFilterActionProvider
import org.wikipedia.views.WikiErrorView

class SuggestedEditsRecentEditsFragment : Fragment(), MenuProvider {
    private var _binding: FragmentSuggestedEditsRecentEditsBinding? = null

    private lateinit var notificationButtonView: NotificationButtonView
    private val recentEditsListAdapter = RecentEditsListAdapter()
    private val recentEditsSearchBarAdapter = SearchBarAdapter()
    private val recentEditsEmptyMessagesAdapter = EmptyMessagesAdapter()
    private val loadHeader = LoadingItemAdapter { recentEditsListAdapter.retry() }
    private val loadFooter = LoadingItemAdapter { recentEditsListAdapter.retry() }
    private var actionMode: ActionMode? = null
    private val viewModel: SuggestedEditsRecentEditsViewModel by viewModels()
    private val searchActionModeCallback = SearchCallback()
    private val binding get() = _binding!!

    private val launchFilterActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            viewModel.langCode = Prefs.userContribFilterLangCode
            setupAdapters()
            viewModel.clearCache()
            recentEditsListAdapter.reload()
            recentEditsSearchBarAdapter.notifyItemChanged(0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsRecentEditsBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar!!.title = getString(R.string.patroller_tasks_edits_list_title)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.refreshContainer.setOnRefreshListener {
            viewModel.clearCache()
            recentEditsListAdapter.reload()
        }

        binding.refreshContainer.setOnRefreshListener {
            viewModel.clearCache()
            recentEditsListAdapter.reload()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setupAdapters()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    recentEditsListAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                        .filter { it.refresh is LoadState.NotLoading }
                        .collectLatest {
                            if (binding.refreshContainer.isRefreshing) {
                                binding.refreshContainer.isRefreshing = false
                            }
                        }
                }
                launch {
                    recentEditsListAdapter.loadStateFlow.collectLatest {
                        loadHeader.loadState = it.refresh
                        loadFooter.loadState = it.append
                        val showEmpty = (it.append is LoadState.NotLoading && it.source.refresh is LoadState.NotLoading && recentEditsListAdapter.itemCount == 0)
                        if (showEmpty) {
                            (binding.recyclerView.adapter as ConcatAdapter).addAdapter(recentEditsEmptyMessagesAdapter)
                        } else {
                            (binding.recyclerView.adapter as ConcatAdapter).removeAdapter(recentEditsEmptyMessagesAdapter)
                        }
                    }
                }
                launch {
                    viewModel.recentEditsFlow.collectLatest {
                        recentEditsListAdapter.submitData(it)
                    }
                }
            }
        }

        if (viewModel.actionModeActive) {
            startSearchActionMode()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_watchlist, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onPrepareMenu(menu: Menu) {
        val notificationMenuItem = menu.findItem(R.id.menu_notifications)
        if (AccountUtil.isLoggedIn) {
            notificationMenuItem.isVisible = true
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            notificationButtonView.setOnClickListener {
                if (AccountUtil.isLoggedIn) {
                    startActivity(NotificationActivity.newIntent(requireActivity()))
                }
            }
            notificationButtonView.contentDescription = getString(R.string.notifications_activity_title)
            notificationMenuItem.actionView = notificationButtonView
            notificationMenuItem.expandActionView()
            FeedbackUtil.setButtonLongPressToast(notificationButtonView)
        } else {
            notificationMenuItem.isVisible = false
        }
        updateNotificationDot(false)
    }

    fun updateNotificationDot(animate: Boolean) {
        if (AccountUtil.isLoggedIn && Prefs.notificationUnreadCount > 0) {
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            if (animate) {
                notificationButtonView.runAnimation()
            }
        } else {
            notificationButtonView.setUnreadCount(0)
        }
    }

    private fun setupAdapters() {
        if (actionMode != null) {
            binding.recyclerView.adapter = recentEditsListAdapter.withLoadStateFooter(loadFooter)
        } else {
            binding.recyclerView.adapter =
                recentEditsListAdapter.withLoadStateHeaderAndFooter(loadHeader, loadFooter).also {
                    it.addAdapter(0, recentEditsSearchBarAdapter)
                }
        }
    }

    private fun startSearchActionMode() {
        actionMode = (requireActivity() as MainActivity).startSupportActionMode(searchActionModeCallback)
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

    private inner class LoadingItemAdapter(private val retry: () -> Unit) : LoadStateAdapter<LoadingViewHolder>() {
        override fun onBindViewHolder(holder: LoadingViewHolder, loadState: LoadState) {
            holder.bindItem(loadState, retry)
        }

        override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadingViewHolder {
            return LoadingViewHolder(layoutInflater.inflate(R.layout.item_list_progress, parent, false))
        }
    }

    private inner class RecentEditsDiffCallback : DiffUtil.ItemCallback<SuggestedEditsRecentEditsViewModel.RecentEditsItemModel>() {
        override fun areContentsTheSame(oldItem: SuggestedEditsRecentEditsViewModel.RecentEditsItemModel, newItem: SuggestedEditsRecentEditsViewModel.RecentEditsItemModel): Boolean {
            if (oldItem is SuggestedEditsRecentEditsViewModel.RecentEditsSeparator && newItem is SuggestedEditsRecentEditsViewModel.RecentEditsSeparator) {
                return oldItem.date == newItem.date
            } else if (oldItem is SuggestedEditsRecentEditsViewModel.RecentEditsItem && newItem is SuggestedEditsRecentEditsViewModel.RecentEditsItem) {
                return oldItem.item.rcid == newItem.item.rcid
            }
            return false
        }

        override fun areItemsTheSame(oldItem: SuggestedEditsRecentEditsViewModel.RecentEditsItemModel, newItem: SuggestedEditsRecentEditsViewModel.RecentEditsItemModel): Boolean {
            return oldItem == newItem
        }
    }

    private inner class RecentEditsListAdapter :
        PagingDataAdapter<SuggestedEditsRecentEditsViewModel.RecentEditsItemModel, RecyclerView.ViewHolder>(RecentEditsDiffCallback()) {

        fun reload() {
            submitData(lifecycle, PagingData.empty())
            viewModel.recentEditsSource?.invalidate()
        }

        override fun getItemViewType(position: Int): Int {
            return if (getItem(position) is SuggestedEditsRecentEditsViewModel.RecentEditsSeparator) {
                VIEW_TYPE_SEPARATOR
            } else {
                VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_SEPARATOR) {
                SeparatorViewHolder(layoutInflater.inflate(R.layout.item_edit_history_separator, parent, false))
            } else {
                RecentEditsItemHolder(SuggestedEditsRecentEditsItemView(requireContext()))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            if (holder is SeparatorViewHolder) {
                holder.bindItem((item as SuggestedEditsRecentEditsViewModel.RecentEditsSeparator).date)
            } else if (holder is RecentEditsItemHolder) {
                holder.bindItem((item as SuggestedEditsRecentEditsViewModel.RecentEditsItem).item)
            }
        }
    }

    private inner class LoadingViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(loadState: LoadState, retry: () -> Unit) {
            val errorView = itemView.findViewById<WikiErrorView>(R.id.errorView)
            val progressBar = itemView.findViewById<View>(R.id.progressBar)
            progressBar.isVisible = loadState is LoadState.Loading
            errorView.isVisible = loadState is LoadState.Error
            errorView.retryClickListener = View.OnClickListener { retry() }
            if (loadState is LoadState.Error) {
                errorView.setError(loadState.error)
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
            binding.root.setCardBackgroundColor(
                ResourceUtil.getThemedColor(requireContext(), R.attr.background_color)
            )

            itemView.setOnClickListener {
                startSearchActionMode()
            }

            binding.filterByButton.setOnClickListener {
                // TODO: implement this
            }

            FeedbackUtil.setButtonLongPressToast(binding.filterByButton)
            binding.root.isVisible = true
        }

        private fun updateFilterCount() {
            val filtersCount = viewModel.filtersCount()
            if (filtersCount == 0) {
                binding.filterCount.visibility = View.GONE
                ImageViewCompat.setImageTintList(binding.filterByButton,
                    ResourceUtil.getThemedColorStateList(requireContext(), R.attr.primary_color))
            } else {
                binding.filterCount.visibility = View.VISIBLE
                binding.filterCount.text = filtersCount.toString()
                ImageViewCompat.setImageTintList(binding.filterByButton,
                    ResourceUtil.getThemedColorStateList(requireContext(), R.attr.progressive_color))
            }
        }
    }

    private inner class EmptyMessagesViewHolder constructor(val binding: ViewEditHistoryEmptyMessagesBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.emptySearchMessage.movementMethod = LinkMovementMethodExt { _ ->
                // TODO: implement this
            }
        }

        fun bindItem() {
            binding.emptySearchMessage.text = StringUtil.fromHtml(getString(R.string.page_edit_history_empty_search_message))
        }
    }

    private inner class RecentEditsItemHolder constructor(private val view: SuggestedEditsRecentEditsItemView) : RecyclerView.ViewHolder(view), SuggestedEditsRecentEditsItemView.Callback {
        private lateinit var recentChange: MwQueryResult.RecentChange

        fun bindItem(recentChange: MwQueryResult.RecentChange) {
            this.recentChange = recentChange
            view.setItem(recentChange)
            view.callback = this
        }

        override fun onItemClick(item: MwQueryResult.RecentChange) {
            // TODO: implement this
        }

        override fun onUserClick(item: MwQueryResult.RecentChange, view: View) {
            // TODO: implement this
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {

        var searchAndFilterActionProvider: SearchAndFilterActionProvider? = null

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchAndFilterActionProvider = SearchAndFilterActionProvider(requireContext(), searchHintString,
                object : SearchAndFilterActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }

                    override fun onFilterIconClick() {
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
            recentEditsListAdapter.reload()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.currentQuery = ""
            recentEditsListAdapter.reload()
            viewModel.actionModeActive = false
            setupAdapters()
        }

        override fun getSearchHintString(): String {
            return getString(R.string.patroller_tasks_edits_list_search_hint)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }
    }

    companion object {
        private const val VIEW_TYPE_SEPARATOR = 0
        private const val VIEW_TYPE_ITEM = 1

        fun newInstance(): SuggestedEditsRecentEditsFragment {
            return SuggestedEditsRecentEditsFragment()
        }
    }
}
