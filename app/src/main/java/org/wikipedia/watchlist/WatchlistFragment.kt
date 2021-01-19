package org.wikipedia.watchlist

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_watchlist.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import java.util.*
import kotlin.collections.ArrayList

class WatchlistFragment : Fragment(), WatchlistHeaderView.Callback, WatchlistItemView.Callback, WatchlistLanguagePopupView.Callback {
    private val disposables = CompositeDisposable()
    private val totalItems = ArrayList<MwQueryResult.WatchlistItem>()
    private var filterMode = FILTER_MODE_ALL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.title = getString(R.string.watchlist_title)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        watchlistRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        watchlistRefreshView.setOnRefreshListener { fetchWatchlist(true) }

        watchlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchWatchlist(false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_watchlist, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_change_language).isVisible = WikipediaApp.getInstance().language().appLanguageCodes.size > 1
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_change_language -> {
                val overflowView = WatchlistLanguagePopupView(requireContext())
                overflowView.show(requireActivity().window.decorView.findViewById(R.id.menu_change_language), this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchWatchlist(refreshing: Boolean) {
        disposables.clear()
        watchlistEmptyContainer.visibility = View.GONE
        watchlistRecyclerView.visibility = View.GONE
        watchlistErrorView.visibility = View.GONE

        if (!AccountUtil.isLoggedIn()) {
            return
        }

        if (!refreshing) {
            watchlistProgressBar.visibility = View.VISIBLE
        }

        val disabledLangCodes = Prefs.getWatchlistDisabledLanguages()
        val calls = ArrayList<Observable<MwQueryResponse>>()
        for (lang in WikipediaApp.getInstance().language().appLanguageCodes) {
            if (disabledLangCodes.contains(lang)) {
                continue
            }
            calls.add(ServiceFactory.get(WikiSite.forLanguageCode(lang)).getWatchlist(lang)
                    .subscribeOn(Schedulers.io()))
        }

        disposables.add(Observable.zip(calls) { resultList ->
            val items = ArrayList<MwQueryResult.WatchlistItem>()
            for (result in resultList) {
                val wiki = WikiSite.forLanguageCode((result as MwQueryResponse).query()!!.allMessages[0]!!.name)
                for (item in result.query()!!.watchlist) {
                    item.wiki = wiki
                    items.add(item)
                }
            }
            items
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    watchlistRefreshView.isRefreshing = false
                    watchlistProgressBar.visibility = View.GONE
                }
                .subscribe({ items ->
                    onSuccess(items)
                }, { t ->
                    L.e(t)
                    onError(t)
                }))
    }

    private fun onSuccess(watchlistItems: List<MwQueryResult.WatchlistItem>) {
        totalItems.clear()
        totalItems.addAll(watchlistItems)
        totalItems.sortByDescending { it.date }
        onUpdateList(totalItems)
    }

    private fun onError(t: Throwable) {
        watchlistErrorView.setError(t)
        watchlistErrorView.visibility = View.VISIBLE
    }

    private fun onUpdateList(watchlistItems: List<MwQueryResult.WatchlistItem>) {
        val items = ArrayList<Any>()
        items.add("") // placeholder for header

        val calendar = Calendar.getInstance()
        var curDay = -1

        for (item in watchlistItems) {
            if ((filterMode == FILTER_MODE_ALL) ||
                    (filterMode == FILTER_MODE_PAGES && Namespace.of(item.ns).main()) ||
                    (filterMode == FILTER_MODE_TALK && Namespace.of(item.ns).talk()) ||
                    (filterMode == FILTER_MODE_OTHER && !Namespace.of(item.ns).main() && !Namespace.of(item.ns).talk())) {

                calendar.time = item.date
                if (calendar.get(Calendar.DAY_OF_YEAR) != curDay) {
                    curDay = calendar.get(Calendar.DAY_OF_YEAR)
                    items.add(item.date)
                }

                items.add(item)
            }
        }

        if (filterMode == FILTER_MODE_ALL && items.size < 2) {
            watchlistRecyclerView.visibility = View.GONE
            watchlistEmptyContainer.visibility = View.VISIBLE
        } else {
            watchlistEmptyContainer.visibility = View.GONE
            watchlistRecyclerView.adapter = RecyclerAdapter(items)
            watchlistRecyclerView.visibility = View.VISIBLE
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
            textView.text = DateUtil.getMonthOnlyDateString(date)
        }
    }

    internal inner class WatchlistHeaderViewHolder(view: WatchlistHeaderView) : RecyclerView.ViewHolder(view) {
        fun bindItem() {
            (itemView as WatchlistHeaderView).callback = this@WatchlistFragment
            itemView.enableByFilterMode(filterMode)
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
        filterMode = FILTER_MODE_ALL
        onUpdateList(totalItems)
    }

    override fun onSelectFilterTalk() {
        filterMode = FILTER_MODE_TALK
        onUpdateList(totalItems)
    }

    override fun onSelectFilterPages() {
        filterMode = FILTER_MODE_PAGES
        onUpdateList(totalItems)
    }

    override fun onSelectFilterOther() {
        filterMode = FILTER_MODE_OTHER
        onUpdateList(totalItems)
    }

    override fun onLanguageChanged() {
        fetchWatchlist(false)
    }

    override fun onItemClick(item: MwQueryResult.WatchlistItem) {
        startActivity(ArticleEditDetailsActivity.newIntent(requireContext(), item.title,
                item.revid, item.wiki.languageCode(), item.newlen - item.oldlen))
    }

    override fun onUserClick(item: MwQueryResult.WatchlistItem) {
        startActivity(TalkTopicsActivity.newIntent(requireContext(),
                PageTitle(UserTalkAliasData.valueFor(AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE) + ":" + item.user, item.wiki)))
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
