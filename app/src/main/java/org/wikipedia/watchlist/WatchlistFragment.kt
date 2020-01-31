package org.wikipedia.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.fragment_watchlist.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.PageItemView

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
                    val watchlistItems = response.query()!!.watchlist

                    watchlistRecyclerView.adapter = (RecyclerAdapter(watchlistItems))
                    onSuccess()
                }, { t ->
                    L.e(t)
                    onError(t)
                }))

    }

    private fun onSuccess() {
        watchlistProgressBar.visibility = View.GONE
        watchlistRecyclerView.visibility = View.VISIBLE
    }

    private fun onError(t: Throwable) {
        watchlistProgressBar.visibility = View.GONE
    }

    internal inner class RecyclerAdapter(items: List<MwQueryResult.WatchlistItem>) : DefaultRecyclerAdapter<MwQueryResult.WatchlistItem, View>(items) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<View> {
            val view = PageItemView<MwQueryResult.WatchlistItem>(requireContext())
            return DefaultViewHolder(view)
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<View>, i: Int) {
            val view = holder.view as PageItemView<*>

            view.setTitle(items()[i].title)
            view.setDescription(StringUtil.fromHtml(items()[i].parsedComment))
        }
    }

    companion object {
        fun newInstance(): WatchlistFragment {
            return WatchlistFragment()
        }
    }
}