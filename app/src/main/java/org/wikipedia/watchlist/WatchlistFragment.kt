package org.wikipedia.watchlist

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentWatchlistBinding
import org.wikipedia.databinding.ViewWatchlistSearchBarBinding
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.talk.UserTalkPopupHelper
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.NotificationButtonView
import org.wikipedia.views.SearchAndFilterActionProvider
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class WatchlistFragment : Fragment(), WatchlistItemView.Callback, MenuProvider {
    private var _binding: FragmentWatchlistBinding? = null

    private lateinit var notificationButtonView: NotificationButtonView
    private var actionMode: ActionMode? = null
    private val viewModel: WatchlistViewModel by viewModels()
    private val searchActionModeCallback = SearchCallback()
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateDisplayLanguages()
        viewModel.fetchWatchlist(actionMode == null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar!!.title = getString(R.string.watchlist_title)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.watchlistRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.progressive_color))
        binding.watchlistRefreshView.setOnRefreshListener { viewModel.fetchWatchlist(actionMode == null) }
        binding.watchlistErrorView.retryClickListener = View.OnClickListener { viewModel.fetchWatchlist(actionMode == null) }

        binding.watchlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        notificationButtonView = NotificationButtonView(requireActivity())
        updateDisplayLanguages()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is Resource.Loading -> onLoading()
                        is Resource.Success -> onSuccess()
                        is Resource.Error -> onError(it.throwable)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actionMode?.let {
            viewModel.updateList(false)
            if (SearchActionModeCallback.`is`(it)) {
                searchActionModeCallback.refreshProvider()
            }
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
            FeedbackUtil.setButtonTooltip(notificationButtonView)
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

    private fun updateDisplayLanguages() {
        viewModel.displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistExcludedWikiCodes.contains(it) }
    }

    private fun onLoading() {
        binding.watchlistEmptyContainer.visibility = View.GONE
        binding.watchlistRecyclerView.visibility = View.GONE
        binding.watchlistErrorView.visibility = View.GONE
        binding.watchlistProgressBar.isVisible = !binding.watchlistRefreshView.isRefreshing
    }

    private fun onSuccess() {
        binding.watchlistErrorView.visibility = View.GONE
        binding.watchlistRefreshView.isRefreshing = false
        binding.watchlistProgressBar.visibility = View.GONE
        binding.watchlistRecyclerView.adapter = RecyclerAdapter(viewModel.finalList)
        WatchlistAnalyticsHelper.logWatchlistItemCountOnLoad(requireContext(), viewModel.finalList.size)
        binding.watchlistRecyclerView.visibility = View.VISIBLE

        if (viewModel.finalList.filterNot { it == "" }.isEmpty()) {
            binding.watchlistEmptyContainer.visibility = if (actionMode == null && viewModel.filtersCount() == 0) View.VISIBLE else View.GONE
            binding.watchlistSearchEmptyContainer.visibility = if (viewModel.filtersCount() != 0) View.VISIBLE else View.GONE
            binding.watchlistSearchEmptyText.visibility = if (actionMode != null) View.VISIBLE else View.GONE
            setUpEmptySearchMessage()
        } else {
            binding.watchlistEmptyContainer.visibility = View.GONE
            binding.watchlistSearchEmptyContainer.visibility = View.GONE
            binding.watchlistSearchEmptyText.visibility = View.GONE
        }
    }

    private fun onError(t: Throwable) {
        binding.watchlistRefreshView.isRefreshing = false
        binding.watchlistProgressBar.visibility = View.GONE
        binding.watchlistErrorView.setError(t)
        binding.watchlistErrorView.visibility = View.VISIBLE
    }

    private fun setUpEmptySearchMessage() {
        val filtersStr = resources.getQuantityString(R.plurals.watchlist_number_of_filters, viewModel.filtersCount(), viewModel.filtersCount())
        binding.watchlistEmptySearchMessage.text = StringUtil.fromHtml(getString(R.string.watchlist_empty_search_message, "<a href=\"#\">$filtersStr</a>"))
        binding.watchlistEmptySearchMessage.movementMethod = LinkMovementMethodExt { _ ->
            resultLauncher.launch(WatchlistFilterActivity.newIntent(requireContext()))
        }
    }

    internal inner class WatchlistItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(item: MwQueryResult.WatchlistItem) {
            val view = itemView as WatchlistItemView
            view.setItem(item, viewModel.currentSearchQuery)
            view.callback = this@WatchlistFragment
        }
    }

    internal inner class WatchlistDateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(date: Date) {
            val textView = itemView.findViewById<TextView>(R.id.dateText)
            val localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate()
            textView.text = DateUtil.getShortDateString(localDateTime)
        }
    }

    inner class WatchlistSearchBarHolder constructor(private val itemBinding: ViewWatchlistSearchBarBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        init {
            itemBinding.root.setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.background_color))

            itemBinding.root.setOnClickListener {
                if (actionMode == null) {
                    actionMode = (requireActivity() as WatchlistActivity).startSupportActionMode(searchActionModeCallback)
                    viewModel.updateList(false)
                }
            }

            itemBinding.filterButton.setOnClickListener {
                resultLauncher.launch(WatchlistFilterActivity.newIntent(it.context))
            }

            FeedbackUtil.setButtonTooltip(itemBinding.filterButton)
        }

        fun updateFilterIconAndCount() {
            val showFilterCount = viewModel.filtersCount() != 0
            val filterButtonColor = if (showFilterCount) R.attr.progressive_color else R.attr.primary_color
            itemBinding.filterCount.isVisible = showFilterCount
            itemBinding.filterCount.text = viewModel.filtersCount().toString()
            ImageViewCompat.setImageTintList(itemBinding.filterButton,
                ResourceUtil.getThemedColorStateList(requireContext(), filterButtonColor))
        }
    }

    internal inner class RecyclerAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        constructor(items: List<Any>) : this() {
            this.items = items
        }

        private var items: List<Any> = ArrayList()

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0 && actionMode == null) {
                return VIEW_TYPE_SEARCH_BAR
            }
            return if (items[position] is Date) {
                VIEW_TYPE_DATE
            } else {
                VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_SEARCH_BAR -> {
                    WatchlistSearchBarHolder(ViewWatchlistSearchBarBinding.inflate(layoutInflater, parent, false))
                }
                VIEW_TYPE_DATE -> {
                    WatchlistDateViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.item_watchlist_date, parent, false))
                }
                else -> {
                    WatchlistItemViewHolder(WatchlistItemView(requireContext()))
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is WatchlistSearchBarHolder -> holder.updateFilterIconAndCount()
                is WatchlistDateViewHolder -> holder.bindItem(items[position] as Date)
                else -> (holder as WatchlistItemViewHolder).bindItem((items[position] as MwQueryResult.WatchlistItem))
            }
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
                        DeviceUtil.hideSoftKeyboard(requireActivity())
                        startActivity(WatchlistFilterActivity.newIntent(requireContext()))
                    }

                    override fun getExcludedFilterCount(): Int {
                        return viewModel.filtersCount()
                    }

                    override fun getFilterIconContentDescription(): Int {
                        return R.string.watchlist_search_bar_filter_hint
                    }
                })

            val menuItem = menu.add(searchHintString)

            MenuItemCompat.setActionProvider(menuItem, searchAndFilterActionProvider)

            actionMode = mode
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.updateSearchQuery(s.trim())
            viewModel.updateList(false)
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.updateSearchQuery(null)
            viewModel.updateList(true)
        }

        override fun getSearchHintString(): String {
            return getString(R.string.watchlist_search)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }

        fun refreshProvider() {
            searchAndFilterActionProvider?.updateFilterIconAndText()
        }
    }

    override fun onItemClick(item: MwQueryResult.WatchlistItem) {
        if (item.logtype.isNotEmpty()) {
            return
        }
        startActivity(ArticleEditDetailsActivity.newIntent(requireContext(),
                PageTitle(item.title, item.wiki!!), item.pageId, revisionTo = item.revid, source = Constants.InvokeSource.WATCHLIST_ACTIVITY))
    }

    override fun onUserClick(item: MwQueryResult.WatchlistItem, view: View) {
        UserTalkPopupHelper.show(requireActivity() as AppCompatActivity,
                PageTitle(UserAliasData.valueFor(item.wiki!!.languageCode), item.user, item.wiki!!),
            item.isAnon, view, Constants.InvokeSource.WATCHLIST_ACTIVITY, HistoryEntry.SOURCE_WATCHLIST, revisionId = item.revid, pageId = item.pageId)
    }

    companion object {
        const val VIEW_TYPE_SEARCH_BAR = 0
        const val VIEW_TYPE_DATE = 1
        const val VIEW_TYPE_ITEM = 2

        fun newInstance(): WatchlistFragment {
            return WatchlistFragment()
        }
    }
}
