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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentSuggestedEditsRecentEditsBinding
import org.wikipedia.databinding.ViewSuggestedEditsRecentEditsSearchBarBinding
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.NotificationButtonView
import org.wikipedia.views.SearchAndFilterActionProvider
import java.util.Date

class SuggestedEditsRecentEditsFragment : Fragment(), SuggestedEditsRecentEditsItemView.Callback, MenuProvider {
    private var _binding: FragmentSuggestedEditsRecentEditsBinding? = null

    private lateinit var notificationButtonView: NotificationButtonView
    private var actionMode: ActionMode? = null
    private val viewModel: SuggestedEditsRecentEditsViewModel by viewModels()
    private val searchActionModeCallback = SearchCallback()
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateDisplayLanguage()
        viewModel.fetchRecentEdits(actionMode == null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsRecentEditsBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar!!.title = getString(R.string.watchlist_title)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.recentEditsRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.progressive_color))
        binding.recentEditsRefreshView.setOnRefreshListener { viewModel.fetchRecentEdits(actionMode == null) }
        binding.recentEditsErrorView.retryClickListener = View.OnClickListener { viewModel.fetchRecentEdits(actionMode == null) }

        binding.recentEditsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        notificationButtonView = NotificationButtonView(requireActivity())
        updateDisplayLanguage()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is SuggestedEditsRecentEditsViewModel.UiState.Loading -> onLoading()
                        is SuggestedEditsRecentEditsViewModel.UiState.Success -> onSuccess()
                        is SuggestedEditsRecentEditsViewModel.UiState.Error -> onError(it.throwable)
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

    private fun updateDisplayLanguage() {
        // TODO: implement this
        viewModel.displayLanguage = "en"
    }

    private fun onLoading() {
        binding.recentEditsEmptyContainer.visibility = View.GONE
        binding.recentEditsRecyclerView.visibility = View.GONE
        binding.recentEditsErrorView.visibility = View.GONE
        binding.recentEditsProgressBar.isVisible = !binding.recentEditsRefreshView.isRefreshing
    }

    private fun onSuccess() {
        binding.recentEditsErrorView.visibility = View.GONE
        binding.recentEditsRefreshView.isRefreshing = false
        binding.recentEditsProgressBar.visibility = View.GONE
        binding.recentEditsRecyclerView.adapter = RecyclerAdapter(viewModel.finalList)
        binding.recentEditsRecyclerView.visibility = View.VISIBLE

        if (viewModel.finalList.filterNot { it == "" }.isEmpty()) {
            binding.recentEditsEmptyContainer.visibility = if (actionMode == null && viewModel.filtersCount() == 0) View.VISIBLE else View.GONE
            binding.recentEditsSearchEmptyContainer.visibility = if (viewModel.filtersCount() != 0) View.VISIBLE else View.GONE
            binding.recentEditsSearchEmptyText.visibility = if (actionMode != null) View.VISIBLE else View.GONE
            setUpEmptySearchMessage()
        } else {
            binding.recentEditsEmptyContainer.visibility = View.GONE
            binding.recentEditsSearchEmptyContainer.visibility = View.GONE
            binding.recentEditsSearchEmptyText.visibility = View.GONE
        }
    }

    private fun onError(t: Throwable) {
        binding.recentEditsRefreshView.isRefreshing = false
        binding.recentEditsProgressBar.visibility = View.GONE
        binding.recentEditsErrorView.setError(t)
        binding.recentEditsErrorView.visibility = View.VISIBLE
    }

    private fun setUpEmptySearchMessage() {
        val filtersStr = resources.getQuantityString(R.plurals.watchlist_number_of_filters, viewModel.filtersCount(), viewModel.filtersCount())
        binding.recentEditsEmptySearchMessage.text = StringUtil.fromHtml(getString(R.string.watchlist_empty_search_message, "<a href=\"#\">$filtersStr</a>"))
        binding.recentEditsEmptySearchMessage.movementMethod = LinkMovementMethodExt { _ ->
            // TODO: launch filter activity
            // resultLauncher.launch(WatchlistFilterActivity.newIntent(requireContext()))
        }
    }

    internal inner class RecentEditsItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(item: MwQueryResult.RecentChange) {
            val view = itemView as SuggestedEditsRecentEditsItemView
            view.setItem(item)
            view.callback = this@SuggestedEditsRecentEditsFragment
        }
    }

    internal inner class RecentEditsDateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(date: Date) {
            val textView = itemView.findViewById<TextView>(R.id.dateText)
            textView.text = DateUtil.getShortDateString(date)
        }
    }

    inner class RecentEditsSearchBarHolder constructor(private val itemBinding: ViewSuggestedEditsRecentEditsSearchBarBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        init {
            itemBinding.root.setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.background_color))

            itemBinding.root.setOnClickListener {
                if (actionMode == null) {
                    actionMode = (requireActivity() as MainActivity).startSupportActionMode(searchActionModeCallback)
                    viewModel.updateList(false)
                }
            }

            itemBinding.filterButton.setOnClickListener {
                // TODO: launch filter activity
                // resultLauncher.launch(WatchlistFilterActivity.newIntent(it.context))
            }

            FeedbackUtil.setButtonLongPressToast(itemBinding.filterButton)
        }

        fun updateFilterIconAndCount() {
            val filterCount = viewModel.filtersCount()
            if (filterCount == 0) {
                itemBinding.filterCount.visibility = View.GONE
                ImageViewCompat.setImageTintList(itemBinding.filterButton, ResourceUtil.getThemedColorStateList(requireContext(), R.attr.primary_color))
            } else {
                itemBinding.filterCount.visibility = View.VISIBLE
                itemBinding.filterCount.text = filterCount.toString()
                ImageViewCompat.setImageTintList(itemBinding.filterButton, ResourceUtil.getThemedColorStateList(requireContext(), R.attr.progressive_color))
            }
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
                    RecentEditsSearchBarHolder(ViewSuggestedEditsRecentEditsSearchBarBinding.inflate(layoutInflater, parent, false))
                }
                VIEW_TYPE_DATE -> {
                    RecentEditsDateViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.item_suggested_edits_recent_edits_date, parent, false))
                }
                else -> {
                    RecentEditsItemViewHolder(SuggestedEditsRecentEditsItemView(requireContext()))
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is RecentEditsSearchBarHolder -> holder.updateFilterIconAndCount()
                is RecentEditsDateViewHolder -> holder.bindItem(items[position] as Date)
                else -> (holder as RecentEditsItemViewHolder).bindItem((items[position] as MwQueryResult.RecentChange))
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
                        // TODO: launch filter activity
                        // startActivity(WatchlistFilterActivity.newIntent(requireContext()))
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

    override fun onItemClick(item: MwQueryResult.RecentChange) {
        // TODO: implement this
//        startActivity(ArticleEditDetailsActivity.newIntent(requireContext(),
//                PageTitle(item.title, item.wiki!!), item.pageId, item.revid))
    }

    override fun onUserClick(item: MwQueryResult.RecentChange, view: View) {
        // TODO: implement this
    }

    companion object {
        const val VIEW_TYPE_SEARCH_BAR = 0
        const val VIEW_TYPE_DATE = 1
        const val VIEW_TYPE_ITEM = 2

        fun newInstance(): SuggestedEditsRecentEditsFragment {
            return SuggestedEditsRecentEditsFragment()
        }
    }
}
