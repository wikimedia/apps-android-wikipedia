package org.wikipedia.watchlist

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentWatchlistBinding
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
import java.util.*

class WatchlistFragment : Fragment(), WatchlistHeaderView.Callback, WatchlistItemView.Callback,
        WatchlistLanguagePopupView.Callback, MenuProvider {
    private var _binding: FragmentWatchlistBinding? = null

    private lateinit var notificationButtonView: NotificationButtonView
    private val viewModel: WatchlistViewModel by viewModels()
    private val binding get() = _binding!!
    private val disposables = CompositeDisposable()

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
        binding.watchlistRefreshView.setOnRefreshListener { fetchWatchlist(true) }

        binding.watchlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        notificationButtonView = NotificationButtonView(requireActivity())
        updateDisplayLanguages()

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect {
                when (it) {
                    is WatchlistViewModel.UiState.Success -> onSuccess(it.watchlistItem)
                    is WatchlistViewModel.UiState.Error -> onError(it.throwable)
                }
            }
        }

        fetchWatchlist(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_watchlist, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_change_language).isVisible = WikipediaApp.instance.languageState.appLanguageCodes.size > 1

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

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_change_language -> {
                val overflowView = WatchlistLanguagePopupView(requireContext())
                overflowView.show(ActivityCompat.requireViewById(requireActivity(), R.id.menu_change_language), this)
                true
            }
            else -> false
        }
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

    private fun fetchWatchlist(refreshing: Boolean) {
        disposables.clear()
        binding.watchlistEmptyContainer.visibility = View.GONE
        binding.watchlistRecyclerView.visibility = View.GONE
        binding.watchlistErrorView.visibility = View.GONE

        if (!AccountUtil.isLoggedIn) {
            return
        }

        if (viewModel.displayLanguages.isEmpty()) {
            binding.watchlistEmptyContainer.visibility = View.VISIBLE
            binding.watchlistProgressBar.visibility = View.GONE
            return
        }

        if (!refreshing) {
            binding.watchlistProgressBar.visibility = View.VISIBLE
        }

        viewModel.fetchWatchlist()
    }

    private fun onSuccess(watchlistItems: List<Any>) {
        binding.watchlistRefreshView.isRefreshing = false
        binding.watchlistProgressBar.visibility = View.GONE
        onUpdateList(watchlistItems)
    }

    private fun onError(t: Throwable) {
        binding.watchlistErrorView.setError(t)
        binding.watchlistErrorView.visibility = View.VISIBLE
    }

    private fun onUpdateList(watchlistItems: List<Any>) {
        if (viewModel.filterMode == FILTER_MODE_ALL && watchlistItems.size < 2) {
            binding.watchlistRecyclerView.visibility = View.GONE
            binding.watchlistEmptyContainer.visibility = View.VISIBLE
        } else {
            binding.watchlistEmptyContainer.visibility = View.GONE
            binding.watchlistRecyclerView.adapter = RecyclerAdapter(watchlistItems)
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

    internal inner class WatchlistHeaderViewHolder(view: WatchlistHeaderView) : RecyclerView.ViewHolder(view) {
        fun bindItem() {
            (itemView as WatchlistHeaderView).callback = this@WatchlistFragment
            itemView.enableByFilterMode(viewModel.filterMode)
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
                return VIEW_TYPE_HEADER
            }
            return if (items[position] is Date) {
                VIEW_TYPE_DATE
            } else {
                VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    WatchlistHeaderViewHolder(WatchlistHeaderView(requireContext()))
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
                is WatchlistHeaderViewHolder -> {
                    holder.bindItem()
                }
                is WatchlistDateViewHolder -> {
                    holder.bindItem((items[position] as Date))
                }
                else -> {
                    (holder as WatchlistItemViewHolder).bindItem((items[position] as MwQueryResult.WatchlistItem))
                }
            }
        }
    }

    override fun onSelectFilterAll() {
        viewModel.filterMode = FILTER_MODE_ALL
    }

    override fun onSelectFilterTalk() {
        viewModel.filterMode = FILTER_MODE_TALK
    }

    override fun onSelectFilterPages() {
        viewModel.filterMode = FILTER_MODE_PAGES
    }

    override fun onSelectFilterOther() {
        viewModel.filterMode = FILTER_MODE_OTHER
    }

    override fun onLanguageChanged() {
        updateDisplayLanguages()
        fetchWatchlist(false)
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

        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_DATE = 1
        const val VIEW_TYPE_ITEM = 2

        fun newInstance(): WatchlistFragment {
            return WatchlistFragment()
        }
    }
}
