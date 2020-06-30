package org.wikipedia.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_watchlist.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.PageItemView
import java.util.*
import java.util.concurrent.TimeUnit

class WatchlistFragment : Fragment() {
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val disposables = CompositeDisposable()

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
        watchlistRefreshView.setOnRefreshListener { fetchWatchlist() }

        watchlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        watchlistRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable))

        fetchWatchlist()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun fetchWatchlist() {
        if (!AccountUtil.isLoggedIn()) {
            return
        }

        watchlistProgressBar.visibility = View.VISIBLE
        disposables.add(ServiceFactory.get(WikipediaApp.getInstance().wikiSite).watchlist
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    watchlistRefreshView.isRefreshing = false
                }
                .subscribe({ response ->
                    onSuccess(response.query()!!.watchlist)
                }, { t ->
                    L.e(t)
                    onError(t)
                }))

    }

    private fun onSuccess(watchlistItems: List<MwQueryResult.WatchlistItem>) {
        val items = ArrayList<Any>()
        val curDate = Date(Date().time + TimeUnit.DAYS.toMillis(1))

        for (item in watchlistItems) {
            if (curDate.time - item.date.time > TimeUnit.DAYS.toMillis(1)) {
                items.add(item.date)
                curDate.time = item.date.time
            }
            items.add(item)
        }

        watchlistRecyclerView.adapter = RecyclerAdapter(items)

        watchlistProgressBar.visibility = View.GONE
        watchlistRecyclerView.visibility = View.VISIBLE
    }

    private fun onError(t: Throwable) {
        watchlistProgressBar.visibility = View.GONE
    }

    class WatchlistItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(item: MwQueryResult.WatchlistItem) {
            val view = itemView as PageItemView<*>
            view.setTitle(item.title)
            view.setDescription(StringUtil.fromHtml(item.parsedComment))
        }
    }

    class WatchlistHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(date: Date) {
            val textView = itemView.findViewById<TextView>(R.id.section_header_text)
            textView.text = DateUtil.getDaysAgoString(date)
        }
    }

    internal inner class RecyclerAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        constructor(items: List<Any>) : this() {
            this.items = items
        }

        private var items: List<Any> = ArrayList()
        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_ITEM = 1

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (items[position] is Date) {
                VIEW_TYPE_HEADER
            } else {
                VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_HEADER) {
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_section_header, parent, false)
                WatchlistHeaderViewHolder(view)
            } else {
                WatchlistItemViewHolder(PageItemView<MwQueryResult.WatchlistItem>(requireContext()))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is WatchlistItemViewHolder) {
                holder.bindItem((items[position] as MwQueryResult.WatchlistItem))
            } else {
                (holder as WatchlistHeaderViewHolder).bindItem((items[position] as Date))
            }
        }
    }

    companion object {
        fun newInstance(): WatchlistFragment {
            return WatchlistFragment()
        }
    }
}