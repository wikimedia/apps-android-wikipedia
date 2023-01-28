package org.wikipedia.watchlist

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentWatchlistBinding
import org.wikipedia.databinding.ViewWatchlistSearchBarBinding
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.talk.UserTalkPopupHelper
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.NotificationButtonView
import org.wikipedia.views.WikiCardView
import java.util.*

class WatchlistFragment : Fragment(), WatchlistItemView.Callback, MenuProvider {
    private var _binding: FragmentWatchlistBinding? = null

    private lateinit var notificationButtonView: NotificationButtonView
    private val viewModel: WatchlistViewModel by viewModels()
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        updateDisplayLanguages()
        viewModel.fetchWatchlist()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.title = getString(R.string.watchlist_title)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.watchlistRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        binding.watchlistRefreshView.setOnRefreshListener { viewModel.fetchWatchlist() }
        binding.watchlistErrorView.retryClickListener = View.OnClickListener { viewModel.fetchWatchlist() }

        binding.watchlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        notificationButtonView = NotificationButtonView(requireActivity())
        updateDisplayLanguages()

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect {
                when (it) {
                    is WatchlistViewModel.UiState.Loading -> onLoading()
                    is WatchlistViewModel.UiState.Success -> onSuccess()
                    is WatchlistViewModel.UiState.Error -> onError(it.throwable)
                }
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

    private fun updateDisplayLanguages() {
        viewModel.displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistDisabledLanguages.contains(it) }
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
        onUpdateList()
    }

    private fun onError(t: Throwable) {
        binding.watchlistRefreshView.isRefreshing = false
        binding.watchlistProgressBar.visibility = View.GONE
        binding.watchlistErrorView.setError(t)
        binding.watchlistErrorView.visibility = View.VISIBLE
    }

    private fun onUpdateList() {
        viewModel.updateList()
        if (viewModel.filterMode == FILTER_MODE_ALL && viewModel.finalList.size < 2) {
            binding.watchlistRecyclerView.visibility = View.GONE
            binding.watchlistEmptyContainer.visibility = View.VISIBLE
        } else {
            binding.watchlistEmptyContainer.visibility = View.GONE
            binding.watchlistRecyclerView.adapter = RecyclerAdapter(viewModel.finalList)
            binding.watchlistRecyclerView.visibility = View.VISIBLE
        }
    }

    internal inner class WatchlistItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(item: MwQueryResult.WatchlistItem) {
            val view = itemView as WatchlistItemView
            view.setItem(item)
            view.callback = this@WatchlistFragment
        }
    }

    internal inner class WatchlistDateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(date: Date) {
            val textView = itemView.findViewById<TextView>(R.id.dateText)
            textView.text = DateUtil.getShortDateString(date)
        }
    }

    inner class WatchlistSearchBarHolder constructor(private val itemBinding: ViewWatchlistSearchBarBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        init {
            (itemBinding.root as WikiCardView).setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_22))

            itemBinding.root.setOnClickListener {
//                if (actionMode == null) {
//                    actionMode = startSupportActionMode(searchActionModeCallback)
//                    postprocessAndDisplay()
//                }
            }

            itemBinding.filterButton.setOnClickListener {
                resultLauncher.launch(WatchlistFilterActivity.newIntent(it.context))
            }

            FeedbackUtil.setButtonLongPressToast(itemBinding.filterButton)
        }

        fun updateFilterIconAndCount() {
//            val excludedFilters = viewModel.excludedFiltersCount()
//            if (excludedFilters == 0) {
//                itemBinding.filterCount.visibility = View.GONE
//                ImageViewCompat.setImageTintList(itemBinding.filterButton, ResourceUtil.getThemedColorStateList(requireContext(), R.attr.color_group_9))
//            } else {
//                itemBinding.filterCount.visibility = View.VISIBLE
//                itemBinding.filterCount.text = excludedFilters.toString()
//                ImageViewCompat.setImageTintList(itemBinding.filterButton, ResourceUtil.getThemedColorStateList(requireContext(), R.attr.colorAccent))
//            }
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
            if (position == 0) {
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
                is WatchlistSearchBarHolder -> { }
                is WatchlistDateViewHolder -> {
                    holder.bindItem((items[position] as Date))
                }
                else -> {
                    (holder as WatchlistItemViewHolder).bindItem((items[position] as MwQueryResult.WatchlistItem))
                }
            }
        }
    }

    override fun onItemClick(item: MwQueryResult.WatchlistItem) {
        if (item.logtype.isNotEmpty()) {
            return
        }
        startActivity(ArticleEditDetailsActivity.newIntent(requireContext(),
                PageTitle(item.title, item.wiki!!), item.revid))
    }

    override fun onUserClick(item: MwQueryResult.WatchlistItem, view: View) {
        UserTalkPopupHelper.show(requireActivity() as AppCompatActivity,
                PageTitle(UserAliasData.valueFor(item.wiki!!.languageCode), item.user, item.wiki!!),
            item.isAnon, view, Constants.InvokeSource.WATCHLIST_ACTIVITY, HistoryEntry.SOURCE_WATCHLIST, revisionId = item.revid, pageId = item.pageId)
    }

    companion object {
        const val FILTER_MODE_ALL = 0
        const val FILTER_MODE_TALK = 1
        const val FILTER_MODE_PAGES = 2
        const val FILTER_MODE_OTHER = 3

        const val VIEW_TYPE_SEARCH_BAR = 0
        const val VIEW_TYPE_DATE = 1
        const val VIEW_TYPE_ITEM = 2

        fun newInstance(): WatchlistFragment {
            return WatchlistFragment()
        }
    }
}
